/*******************************************************************************
 * Copyright (c) 2010, 2018 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Yatta Solutions - bug 432803: public API, bug 413871: performance
 *******************************************************************************/

package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiResources;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalogSource;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.userstorage.util.ConflictException;

/**
 * @author Steffen Pingel
 * @author David Green
 * @author Carsten Reckord
 */
public class DiscoveryItem<T extends CatalogItem> extends AbstractMarketplaceDiscoveryItem<T> {

	public static final String STYLING_CLASS = "DiscoveryItem"; //$NON-NLS-1$

	private static final String FAVORITED_BUTTON_STATE_DATA = "favorited"; //$NON-NLS-1$

	public static final String WIDGET_ID_INSTALLS = "installs"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAGS = "tags"; //$NON-NLS-1$

	public static final String WIDGET_ID_RATING = "rating"; //$NON-NLS-1$

	public static final String WIDGET_ID_SHARE = "share"; //$NON-NLS-1$

	public static final String WIDGET_ID_LEARNMORE = "learn-more"; //$NON-NLS-1$

	public static final String WIDGET_ID_OVERVIEW = "overview"; //$NON-NLS-1$

	public static final String WIDGET_ID_ALREADY_INSTALLED = "already-installed"; //$NON-NLS-1$

	public static final String WIDGET_ID_ACTION = "action"; //$NON-NLS-1$

	public static final String WIDGET_ID_SCREENSHOT = "screenshot"; //$NON-NLS-1$

	private ItemButtonController buttonController;

	private StyledText installInfoLink;

	private ShareSolutionLink shareSolutionLink;

	private Button favoriteButton;

	private SelectionListener toggleFavoritesListener;

	public DiscoveryItem(Composite parent, int style, MarketplaceDiscoveryResources resources,
			IMarketplaceWebBrowser browser,
			final T connector, MarketplaceViewer viewer) {
		super(parent, style, resources, browser, connector, viewer);
	}

	@Override
	protected String getItemClass() {
		return STYLING_CLASS;
	}

	@Override
	protected String getItemId() {
		return WIDGET_ID_CSS_PREFIX + connector.getId();
	}

	@Override
	protected void createContent() {
		toggleFavoritesListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleFavorite();
			}
		};
		super.createContent();
	}

	@Override
	protected void createInstallButtons(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE); // prevent the button from changing the layout of the title
		GridDataFactory.fillDefaults().indent(0, BUTTONBAR_MARGIN_TOP).align(SWT.TRAIL, SWT.FILL).applyTo(composite);

		int numColumns = 1;
		boolean installed = connector.isInstalled();
		final String url = getCatalogItemNode().getUrl();
		if (installed && getViewer().getContentType() != ContentType.INSTALLED
				&& getViewer().getContentType() != ContentType.SELECTION) {
			Button alreadyInstalledButton = new Button(composite, SWT.PUSH | SWT.BOLD);
			setWidgetId(alreadyInstalledButton, WIDGET_ID_ALREADY_INSTALLED);
			new StyleHelper().on(alreadyInstalledButton).addClass("install-action");
			alreadyInstalledButton.setText(Messages.DiscoveryItem_AlreadyInstalled);
			alreadyInstalledButton.setFont(JFaceResources.getFontRegistry().getItalic("")); //$NON-NLS-1$
			Point preferredSize = alreadyInstalledButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int preferredWidth = preferredSize.x + 10;//Give a bit of extra padding for italic font
			GridDataFactory.swtDefaults()
			.align(SWT.TRAIL, SWT.CENTER)
			.minSize(preferredWidth, SWT.DEFAULT)
			.hint(preferredWidth, SWT.DEFAULT)
			.grab(false, true)
			.applyTo(alreadyInstalledButton);
			alreadyInstalledButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					//show installed tab
					getViewer().setContentType(ContentType.INSTALLED);
					//then scroll to item
					getViewer().reveal(DiscoveryItem.this);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		} else if (hasInstallMetadata()) {
			DropDownButton dropDown = new DropDownButton(composite, SWT.PUSH);
			Button button = dropDown.getButton();
			setWidgetId(button, WIDGET_ID_ACTION);
			new StyleHelper().on(button).addClass("install-action");
			Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int preferredWidth = preferredSize.x + 10;//Give a bit of extra padding for bold or italic font

			GridDataFactory.swtDefaults()
			.align(SWT.TRAIL, SWT.CENTER)
			.minSize(preferredWidth, SWT.DEFAULT)
			.grab(false, true)
			.applyTo(button);

			buttonController = new ItemButtonController(getViewer(), this, dropDown);
		} else if (browser != null && url != null && !"".equals(url.trim())) { //$NON-NLS-1$
			installInfoLink = StyledTextHelper.createStyledTextLabel(composite);
			setWidgetId(installInfoLink, WIDGET_ID_LEARNMORE);
			installInfoLink.setToolTipText(Messages.DiscoveryItem_installInstructionsTooltip);
			StyledTextHelper.appendLink(installInfoLink, Messages.DiscoveryItem_installInstructions,
					Messages.DiscoveryItem_installInstructions, SWT.BOLD);
			new LinkListener() {
				@Override
				protected void selected(Object href, TypedEvent e) {
					browser.openUrl(url);
				}
			}.register(installInfoLink);
			GridDataFactory.swtDefaults().align(SWT.TRAIL, SWT.CENTER).grab(false, true).applyTo(installInfoLink);
		} else {
			Label placeholder = new Label(composite, SWT.NONE);
			placeholder.setText(" "); //$NON-NLS-1$
			GridDataFactory.swtDefaults().align(SWT.TRAIL, SWT.CENTER).grab(false, true).applyTo(placeholder);
		}
		GridLayoutFactory.fillDefaults()
		.numColumns(numColumns)
		.margins(0, 0)
		.extendedMargins(0, 5, 0, 0)
		.spacing(5, 0)
		.applyTo(composite);
	}

	@Override
	protected void createInstallInfo(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL); // prevent the button from changing the layout of the title
		GridDataFactory.fillDefaults()
		.indent(DESCRIPTION_MARGIN_LEFT, BUTTONBAR_MARGIN_TOP)
		.grab(true, false)
		.align(SWT.BEGINNING, SWT.CENTER)
		.applyTo(composite);
		RowLayoutFactory.fillDefaults().type(SWT.HORIZONTAL).pack(true).applyTo(composite);

		Integer installsTotal = null;
		Integer installsRecent = null;
		if (connector.getData() instanceof INode) {
			INode node = (INode) connector.getData();
			installsTotal = node.getInstallsTotal();
			installsRecent = node.getInstallsRecent();
		}

		if (installsTotal != null || installsRecent != null) {
			StyledText installInfo = new StyledText(composite, SWT.READ_ONLY | SWT.SINGLE);
			setWidgetId(installInfo, WIDGET_ID_INSTALLS);

			String totalText = installsTotal == null ? Messages.DiscoveryItem_Unknown_Installs : MessageFormat.format(
					Messages.DiscoveryItem_Compact_Number, installsTotal.intValue(), installsTotal * 0.001,
					installsTotal * 0.000001);
			String recentText = installsRecent == null ? Messages.DiscoveryItem_Unknown_Installs
					: MessageFormat.format("{0, number}", //$NON-NLS-1$
							installsRecent.intValue());
			String installInfoText = NLS.bind(Messages.DiscoveryItem_Installs, totalText, recentText);
			int formatTotalsStart = installInfoText.indexOf(totalText);
			if (formatTotalsStart == -1) {
				installInfo.append(installInfoText);
			} else {
				if (formatTotalsStart > 0) {
					installInfo.append(installInfoText.substring(0, formatTotalsStart));
				}
				StyledTextHelper.appendStyled(installInfo, totalText, new StyleRange(0, 0, null, null, SWT.BOLD));
				installInfo.append(installInfoText.substring(formatTotalsStart + totalText.length()));
			}
		} else {
			if (shareSolutionLink != null) {
				shareSolutionLink.setShowText(true);
			}
		}
	}

	@Override
	protected void createSocialButtons(Composite parent) {
		Integer favorited = getFavoriteCount();
		if (favorited == null || getCatalogItemUrl() == null) {
			createButtonBarSpacer(parent);

		} else {
			createFavoriteButton(parent);
		}

		if (getCatalogItemUrl() != null) {
			shareSolutionLink = new ShareSolutionLink(parent, connector);
			Control shareControl = shareSolutionLink.getControl();
			GridDataFactory.fillDefaults()
			.indent(DESCRIPTION_MARGIN_LEFT, BUTTONBAR_MARGIN_TOP)
			.align(SWT.BEGINNING, SWT.FILL)
			.applyTo(shareControl);
		} else {
			createButtonBarSpacer(parent);
		}
	}

	private Integer getFavoriteCount() {
		if (connector.getData() instanceof INode) {
			INode node = (INode) connector.getData();
			IUserFavoritesService userFavoritesService = getUserFavoritesService();
			if (userFavoritesService != null) {
				return userFavoritesService.getFavoriteCount(node);
			}
			return node.getFavorited();
		}
		return null;
	}

	private void createFavoriteButton(Composite parent) {
		favoriteButton = new Button(parent, SWT.PUSH);
		setWidgetId(favoriteButton, WIDGET_ID_RATING);
		refreshFavoriteButton();

		//Make width more or less fixed
		int width = SWT.DEFAULT;
		{
			favoriteButton.setText("999"); //$NON-NLS-1$
			Point pSize = favoriteButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			width = pSize.x;
		}
		refreshFavoriteCount();
		Point pSize = favoriteButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		width = Math.max(width, pSize.x);

		final String ratingDescription = NLS.bind(Messages.DiscoveryItem_Favorited_Times, favoriteButton.getText());
		favoriteButton.setToolTipText(ratingDescription);
		favoriteButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = ratingDescription;
			}
		});

		GridDataFactory.fillDefaults()
		.indent(0, BUTTONBAR_MARGIN_TOP)
		.hint(Math.min(width, MAX_IMAGE_WIDTH), SWT.DEFAULT)
		.align(SWT.CENTER, SWT.FILL)
		.applyTo(favoriteButton);
	}

	private void refreshFavoriteButton() {
		if (favoriteButton == null || this.isDisposed() || favoriteButton.isDisposed()) {
			return;
		}
		if (Display.getCurrent() != this.getDisplay()) {
			this.getDisplay().asyncExec(() -> refreshFavoriteButton());
			return;
		}
		boolean favorited = isFavorited();
		Object lastFavorited = favoriteButton.getData(FAVORITED_BUTTON_STATE_DATA);
		if (lastFavorited == null || (favorited != Boolean.TRUE.equals(lastFavorited))) {
			favoriteButton.setData(FAVORITED_BUTTON_STATE_DATA, lastFavorited);
			String imageId = favorited ? MarketplaceClientUiResources.ITEM_ICON_STAR_SELECTED
					: MarketplaceClientUiResources.ITEM_ICON_STAR;
			favoriteButton.setImage(MarketplaceClientUiResources.getInstance().getImageRegistry().get(imageId));

			IUserFavoritesService userFavoritesService = getUserFavoritesService();
			favoriteButton.setEnabled(userFavoritesService != null);
			favoriteButton.removeSelectionListener(toggleFavoritesListener);
			if (userFavoritesService != null) {
				favoriteButton.addSelectionListener(toggleFavoritesListener);
			}
		}
		refreshFavoriteCount();
	}

	private void refreshFavoriteCount() {
		Integer favoriteCount = getFavoriteCount();
		String favoriteCountText;
		if (favoriteCount == null) {
			favoriteCountText = ""; //$NON-NLS-1$
		} else {
			favoriteCountText = favoriteCount.toString();
		}
		favoriteButton.setText(favoriteCountText);
	}

	private boolean isFavorited() {
		MarketplaceNodeCatalogItem nodeConnector = (MarketplaceNodeCatalogItem) connector;
		Boolean favorited = nodeConnector.getUserFavorite();
		return Boolean.TRUE.equals(favorited);
	}

	private void setFavorited(boolean newFavorited) {
		boolean oldFavorited = isFavorited();
		if (oldFavorited != newFavorited) {
			//FIXME we should type the connector to MarketplaceNodeCatalogItem
			MarketplaceNodeCatalogItem nodeConnector = (MarketplaceNodeCatalogItem) connector;
			nodeConnector.setUserFavorite(newFavorited);
			if (!newFavorited && getViewer().getContentType() == ContentType.FAVORITES) {
				getViewer().getCatalog().removeItem(connector);
				getViewer().refresh();
			} else {
				refreshFavoriteButton();
			}
		}
	}

	private IUserFavoritesService getUserFavoritesService() {
		MarketplaceCatalogSource source = (MarketplaceCatalogSource) this.getData().getSource();
		IUserFavoritesService userFavoritesService = source.getMarketplaceService().getUserFavoritesService();
		return userFavoritesService;
	}

	private void toggleFavorite() {
		final INode node = this.getCatalogItemNode();
		final IUserFavoritesService userFavoritesService = getUserFavoritesService();
		if (node != null && userFavoritesService != null) {
			final boolean newFavorited = !isFavorited();
			final Throwable[] error = new Throwable[] { null };
			BusyIndicator.showWhile(getDisplay(), () -> {
				try {
					ModalContext.run(monitor -> {
						try {
							userFavoritesService.getStorageService().runWithLogin(() -> {
								userFavoritesService.setFavorite(node, newFavorited, monitor);
								return null;
							});
						} catch (Exception e) {
							error[0] = e;
						}
					}, true, new NullProgressMonitor(), getDisplay());
				} catch (InvocationTargetException e1) {
					error[0] = e1.getCause();
				} catch (InterruptedException e2) {
					error[0] = e2;
				}
			});
			Throwable e = error[0];
			if (e != null) {
				if (e instanceof NotAuthorizedException) {
					// authentication was cancelled
					return;
				} else if (e instanceof ConflictException) {
					// silently ignored - service already tried to resolve this
					return;
				} else {
					IStatus status = MarketplaceClientCore.computeStatus(e,
							NLS.bind(Messages.DiscoveryItem_FavoriteActionFailed, this.getNameLabelText()));
					MarketplaceClientUi.handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
					return;
				}
			}
			setFavorited(newFavorited);
		}
	}

	private INode getCatalogItemNode() {
		Object data = connector.getData();
		if (data instanceof INode) {
			INode node = (INode) data;
			return node;
		}
		return null;
	}

	private String getCatalogItemUrl() {
		INode node = getCatalogItemNode();
		return node == null ? null : node.getUrl();
	}

	private boolean hasInstallMetadata() {
		if (!connector.getInstallableUnits().isEmpty() && connector.getSiteUrl() != null) {
			try {
				URLUtil.toURI(connector.getSiteUrl());
				return true;
			} catch (Exception ex) {
				//ignore
			}
		}
		return false;
	}

	/**
	 * @deprecated use {@link #maybeModifySelection(Operation)}
	 */
	@Deprecated
	protected boolean maybeModifySelection(org.eclipse.epp.internal.mpc.ui.wizards.Operation operation) {
		return maybeModifySelection(operation.getOperation());
	}

	protected boolean maybeModifySelection(Operation operation) {
		getViewer().modifySelection(connector, operation);
		return true;
	}

	@Override
	public boolean isSelected() {
		return getData().isSelected();
	}

	/**
	 * @deprecated use {@link #getSelectedOperation()} instead
	 */
	@Deprecated
	public org.eclipse.epp.internal.mpc.ui.wizards.Operation getOperation() {
		return org.eclipse.epp.internal.mpc.ui.wizards.Operation.map(getSelectedOperation());
	}

	public Operation getSelectedOperation() {
		return getViewer().getSelectionModel().getSelectedOperation(getData());
	}

	@Override
	protected void refresh(boolean updateState) {
		super.refresh(updateState);
		refreshFavoriteButton();
	}

	@Override
	protected void refreshState() {
		if (buttonController != null) {
			buttonController.refresh();
		}
	}

	@Override
	protected MarketplaceViewer getViewer() {
		return (MarketplaceViewer) super.getViewer();
	}

	@Override
	protected void searchForProvider(String searchTerm) {
		getViewer().search(searchTerm);
	}

	@Override
	protected void searchForTag(String tag) {
		getViewer().doQueryForTag(tag);
	}
}

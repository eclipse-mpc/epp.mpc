/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Yatta Solutions - bug 432803: public API, bug 413871: performance
 *******************************************************************************/

package org.eclipse.epp.internal.mpc.ui.wizards;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.internal.mpc.core.util.TextUtil;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalogSource;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceDiscoveryResources.ImageReceiver;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.model.ITags;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.AbstractDiscoveryItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.userstorage.util.ConflictException;

/**
 * @author Steffen Pingel
 * @author David Green
 * @author Carsten Reckord
 */
public class DiscoveryItem<T extends CatalogItem> extends AbstractDiscoveryItem<T>
implements PropertyChangeListener {

	private static final String INFO_HREF = "info"; //$NON-NLS-1$

	private static final String PROVIDER_PLACEHOLDER = "@PROVIDER@"; //$NON-NLS-1$

	private static final int DESCRIPTION_MARGIN_LEFT = 8;

	private static final int DESCRIPTION_MARGIN_TOP = 8;

	private static final int TAGS_MARGIN_TOP = 2;

	private static final int BUTTONBAR_MARGIN_TOP = 8;

	private static final int SEPARATOR_MARGIN_TOP = 8;

	private static final int MAX_IMAGE_HEIGHT = 86;

	private static final int MIN_IMAGE_HEIGHT = 64;

	private static final int MAX_IMAGE_WIDTH = 75;

	public static final String WIDGET_ID_KEY = DiscoveryItem.class.getName() + "::part"; //$NON-NLS-1$

	public static final String WIDGET_ID_NAME = "name"; //$NON-NLS-1$

	public static final String WIDGET_ID_DESCRIPTION = "description"; //$NON-NLS-1$

	public static final String WIDGET_ID_ICON = "description"; //$NON-NLS-1$

	public static final String WIDGET_ID_PROVIDER = "provider"; //$NON-NLS-1$

	public static final String WIDGET_ID_INSTALLS = "installs"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAGS = "tags"; //$NON-NLS-1$

	public static final String WIDGET_ID_RATING = "rating"; //$NON-NLS-1$

	public static final String WIDGET_ID_SHARE = "share"; //$NON-NLS-1$

	public static final String WIDGET_ID_LEARNMORE = "learn more"; //$NON-NLS-1$

	public static final String WIDGET_ID_OVERVIEW = "overview"; //$NON-NLS-1$

	public static final String WIDGET_ID_ALREADY_INSTALLED = "already installed"; //$NON-NLS-1$

	public static final String WIDGET_ID_ACTION = "action"; //$NON-NLS-1$

	private Composite checkboxContainer;

	private final CatalogItem connector;

	private StyledText description;

	private Label iconLabel;

	private Label nameLabel;

	private final MarketplaceViewer viewer;

	private ItemButtonController buttonController;

	private StyledText installInfoLink;

	private final IMarketplaceWebBrowser browser;

	private StyledText tagsLink;

	private static Boolean browserAvailable;

	private ShareSolutionLink shareSolutionLink;

	private Button favoriteButton;

	private final SelectionListener toggleFavoritesListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			toggleFavorite();
		}
	};

	public DiscoveryItem(Composite parent, int style, MarketplaceDiscoveryResources resources,
			IMarketplaceWebBrowser browser,
			final T connector, MarketplaceViewer viewer) {
		super(parent, style, resources, connector);
		this.browser = browser;
		this.connector = connector;
		this.viewer = viewer;
		createContent();
		connector.addPropertyChangeListener(this);
		this.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				connector.removePropertyChangeListener(DiscoveryItem.this);
			}
		});
	}

	private void createContent() {
		GridLayoutFactory.swtDefaults()
		.numColumns(4)
		.equalWidth(false)
		.extendedMargins(0, 0, 2, 0)
		.spacing(0, 0)
		.applyTo(this);

		new Label(this, SWT.NONE).setText(" "); // spacer //$NON-NLS-1$

		createNameLabel(this);
		createIconControl(this);

		createDescription(this);

		createProviderLabel(this);
		createTagsLabel(this);

		createSocialButtons(this);
		createInstallInfo(this);

		createInstallButtons(this);

		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults()
		.indent(0, SEPARATOR_MARGIN_TOP)
		.grab(true, false)
		.span(4, 1)
		.align(SWT.FILL, SWT.BEGINNING)
		.applyTo(separator);
	}

	static void setWidgetId(Widget widget, String id) {
		widget.setData(WIDGET_ID_KEY, id);
	}

	private void createDescription(Composite parent) {
		description = StyledTextHelper.createStyledTextLabel(parent);
		setWidgetId(description, WIDGET_ID_DESCRIPTION);
		GridDataFactory.fillDefaults()
		.grab(true, false)
		.indent(DESCRIPTION_MARGIN_LEFT, DESCRIPTION_MARGIN_TOP)
		.span(3, 1)
		.hint(100, SWT.DEFAULT)
		.applyTo(description);
		String descriptionText = connector.getDescription();
		int maxDescriptionLength = 162;
		if (descriptionText == null) {
			descriptionText = ""; //$NON-NLS-1$
		} else {
			descriptionText = TextUtil.stripHtmlMarkup(descriptionText).trim();
		}
		if (descriptionText.length() > maxDescriptionLength) {
			int truncationIndex = maxDescriptionLength;
			for (int x = truncationIndex; x > 0; --x) {
				if (Character.isWhitespace(descriptionText.charAt(x))) {
					truncationIndex = x;
					break;
				}
			}
			descriptionText = descriptionText.substring(0, truncationIndex)
					+ Messages.DiscoveryItem_truncatedTextSuffix;
		}
		descriptionText = descriptionText.replaceAll("(\\r\\n)|\\n|\\r|\\s{2,}", " "); //$NON-NLS-1$ //$NON-NLS-2$
		description.setText(descriptionText + "  "); //$NON-NLS-1$
		if (descriptionText.startsWith(Messages.DiscoveryItem_Promotion_Marker)) {
			description.replaceTextRange(0, Messages.DiscoveryItem_Promotion_Marker.length(),
					Messages.DiscoveryItem_Promotion_Display + "  - "); //$NON-NLS-1$
			StyleRange style = new StyleRange(0, Messages.DiscoveryItem_Promotion_Display.length(), null, null,
					SWT.ITALIC | SWT.BOLD);
			description.setStyleRange(style);
		}

		createInfoLink(description);
	}

	private void createNameLabel(Composite parent) {
		nameLabel = new Label(parent, SWT.WRAP);
		setWidgetId(nameLabel, WIDGET_ID_NAME);

		GridDataFactory.fillDefaults()
		.indent(DESCRIPTION_MARGIN_LEFT, 0)
		.span(3, 1)
		.grab(true, false)
		.align(SWT.BEGINNING, SWT.CENTER)
		.applyTo(nameLabel);
		nameLabel.setFont(resources.getSmallHeaderFont());
		String name = connector.getName();
		if (name == null) {
			name = NLS.bind(Messages.DiscoveryItem_UnnamedSolution, connector.getId());
		}
		nameLabel.setText(TextUtil.escapeText(connector.getName()));
	}

	private void createInstallButtons(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE); // prevent the button from changing the layout of the title
		GridDataFactory.fillDefaults().indent(0, BUTTONBAR_MARGIN_TOP).align(SWT.TRAIL, SWT.FILL).applyTo(composite);

		int numColumns = 1;
		boolean installed = connector.isInstalled();
		if (installed && viewer.getContentType() != ContentType.INSTALLED
				&& viewer.getContentType() != ContentType.SELECTION) {
			Button alreadyInstalledButton = new Button(composite, SWT.PUSH | SWT.BOLD);
			setWidgetId(alreadyInstalledButton, WIDGET_ID_ALREADY_INSTALLED);
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
				public void widgetSelected(SelectionEvent e) {
					//show installed tab
					viewer.setContentType(ContentType.INSTALLED);
					//then scroll to item
					viewer.reveal(DiscoveryItem.this);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		} else if (hasInstallMetadata()) {
			DropDownButton dropDown = new DropDownButton(composite, SWT.PUSH);
			Button button = dropDown.getButton();
			setWidgetId(button, WIDGET_ID_ACTION);
			Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int preferredWidth = preferredSize.x + 10;//Give a bit of extra padding for bold or italic font

			GridDataFactory.swtDefaults()
			.align(SWT.TRAIL, SWT.CENTER)
			.minSize(preferredWidth, SWT.DEFAULT)
			.grab(false, true)
			.applyTo(button);

			buttonController = new ItemButtonController(viewer, this, dropDown);
		} else {
			installInfoLink = StyledTextHelper.createStyledTextLabel(composite);
			setWidgetId(installInfoLink, WIDGET_ID_LEARNMORE);
			installInfoLink.setToolTipText(Messages.DiscoveryItem_installInstructionsTooltip);
			StyledTextHelper.appendLink(installInfoLink, Messages.DiscoveryItem_installInstructions,
					Messages.DiscoveryItem_installInstructions, SWT.BOLD);
			new LinkListener() {
				@Override
				protected void selected(Object href, TypedEvent e) {
					browser.openUrl(getCatalogItemNode().getUrl());
				}
			}.register(installInfoLink);
			GridDataFactory.swtDefaults().align(SWT.TRAIL, SWT.CENTER).grab(false, true).applyTo(installInfoLink);
		}
		GridLayoutFactory.fillDefaults()
		.numColumns(numColumns)
		.margins(0, 0)
		.extendedMargins(0, 5, 0, 0)
		.spacing(5, 0)
		.applyTo(composite);
	}

	private void createInstallInfo(Composite parent) {
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

	private void createSocialButtons(Composite parent) {
		Integer favorited = getFavoriteCount();
		if (favorited == null || getCatalogItemUrl() == null) {
			Label spacer = new Label(this, SWT.NONE);
			spacer.setText(" ");//$NON-NLS-1$

			GridDataFactory.fillDefaults().indent(0, BUTTONBAR_MARGIN_TOP).align(SWT.CENTER, SWT.FILL).applyTo(spacer);

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
			Label spacer = new Label(this, SWT.NONE);
			spacer.setText(" ");//$NON-NLS-1$
			GridDataFactory.fillDefaults().indent(0, BUTTONBAR_MARGIN_TOP).align(SWT.CENTER, SWT.FILL).applyTo(spacer);
		}
	}

	private Integer getFavoriteCount() {
		if (connector.getData() instanceof INode) {
			INode node = (INode) connector.getData();
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
		if (this.isDisposed() || favoriteButton.isDisposed()) {
			return;
		}
		if (Display.getCurrent() != this.getDisplay()) {
			this.getDisplay().asyncExec(new Runnable() {

				public void run() {
					refreshFavoriteButton();
				}
			});
			return;
		}
		boolean favorited = isFavorited();
		Object lastFavorited = favoriteButton.getData("favorited");
		if (lastFavorited == null || (favorited != Boolean.TRUE.equals(lastFavorited))) {
			favoriteButton.setData("favorited", lastFavorited);
			String imageId = favorited ? MarketplaceClientUiPlugin.ITEM_ICON_STAR_SELECTED
					: MarketplaceClientUiPlugin.ITEM_ICON_STAR;
			favoriteButton.setImage(MarketplaceClientUiPlugin.getInstance().getImageRegistry().get(imageId));

			UserFavoritesService userFavoritesService = getUserFavoritesService();
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
		//FIXME we should type the connector to MarketplaceNodeCatalogItem
		MarketplaceNodeCatalogItem nodeConnector = (MarketplaceNodeCatalogItem) connector;
		nodeConnector.setUserFavorite(newFavorited);
		refreshFavoriteButton();
	}

	private UserFavoritesService getUserFavoritesService() {
		MarketplaceCatalogSource source = (MarketplaceCatalogSource) this.getData().getSource();
		UserFavoritesService userFavoritesService = source.getMarketplaceService().getUserFavoritesService();
		return userFavoritesService;
	}

	private void toggleFavorite() {
		final INode node = this.getCatalogItemNode();
		final UserFavoritesService userFavoritesService = getUserFavoritesService();
		if (node != null && userFavoritesService != null) {
			final boolean newFavorited = !isFavorited();
//			String itemName = nameLabel.getText();
//			new Job("Setting favorite " + itemName) {
//				{
//					setUser(false);
//					setSystem(true);
//					setPriority(Job.INTERACTIVE);
//				}
//
//				@Override
//				protected IStatus run(IProgressMonitor monitor) {
			try {
				userFavoritesService.getStorageService().runWithLogin(new Callable<Void>() {
					public Void call() throws Exception {
						userFavoritesService.setFavorite(node, newFavorited);
						return null;
					}
				});
				setFavorited(newFavorited);
			} catch (NotAuthorizedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConflictException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//					return Status.OK_STATUS;
//				}
//			}.schedule();
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

	private void createInfoLink(StyledText description) {
		// bug 323257: don't display if there's no internal browser
		boolean internalBrowserAvailable = computeBrowserAvailable(description);
		if (internalBrowserAvailable && (hasTooltip(connector) || connector.isInstalled())) {
			if (hasTooltip(connector)) {
				String descriptionLink = Messages.DiscoveryItem_More_Info;
				StyledTextHelper.appendLink(description, descriptionLink, INFO_HREF, SWT.BOLD);
				hookTooltip(description.getParent(), description, description, description, connector.getSource(),
						connector.getOverview(), null);
			}
		} else if (!internalBrowserAvailable && hasOverviewUrl(connector)) {
			String descriptionLink = Messages.DiscoveryItem_More_Info;
			StyledTextHelper.appendLink(description, descriptionLink, INFO_HREF, SWT.BOLD);
			new LinkListener() {
				@Override
				protected void selected(Object href, TypedEvent e) {
					if (INFO_HREF.equals(href)) {
						WorkbenchUtil.openUrl(connector.getOverview().getUrl().trim(),
								IWorkbenchBrowserSupport.AS_EXTERNAL);
					}
				}
			}.register(description);
		}
	}

	private void createIconControl(Composite parent) {
		checkboxContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults()
		.indent(0, DESCRIPTION_MARGIN_TOP)
		.align(SWT.CENTER, SWT.BEGINNING)
		.hint(MAX_IMAGE_WIDTH, SWT.DEFAULT)
		.grab(false, true)
		.minSize(MAX_IMAGE_WIDTH, MIN_IMAGE_HEIGHT)
		.span(1, 3)
		.applyTo(checkboxContainer);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(checkboxContainer);

		iconLabel = new Label(checkboxContainer, SWT.NONE);
		setWidgetId(iconLabel, WIDGET_ID_ICON);
		GridDataFactory.swtDefaults()
		.align(SWT.CENTER, SWT.BEGINNING).grab(true, true)
		.applyTo(iconLabel);
		if (connector.getIcon() != null) {
			provideIconImage(iconLabel, connector.getSource(), connector.getIcon(), 64, true);
		} else {
			iconLabel.setImage(MarketplaceClientUiPlugin.getInstance()
					.getImageRegistry()
					.get(MarketplaceClientUiPlugin.NO_ICON_PROVIDED));
		}
	}

	private void provideIconImage(final Label iconLabel, AbstractCatalogSource source, Icon icon, int size,
			boolean fallback) {
		if (iconLabel == null) {
			return;
		}
		String iconPath = getResources().getIconPath(icon, size, fallback);
		getResources().setImage(
				new ImageReceiver() {

					public void setImage(Image image) {
						if (image == null || image.isDisposed() || iconLabel.isDisposed()) {
							return;
						}
						try {
							Rectangle bounds = image.getBounds();
							if (bounds.width < 0.8 * MAX_IMAGE_WIDTH || bounds.width > MAX_IMAGE_WIDTH
									|| bounds.height > MAX_IMAGE_HEIGHT) {
								final Image scaledImage = Util.scaleImage(image, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
								image = scaledImage;
								iconLabel.addDisposeListener(new DisposeListener() {
									public void widgetDisposed(DisposeEvent e) {
										scaledImage.dispose();
									}
								});
							}
							iconLabel.setImage(image);
						} catch (SWTException e) {
							// ignore, probably a bad image format
//							MarketplaceClientUi.error(NLS.bind(Messages.DiscoveryItem_cannotRenderImage_reason, connector.getIcon()
//									.getImage32(), e.getMessage()), e);
						}
					}
				},
				source,
				iconPath,
				MarketplaceClientUiPlugin.getInstance()
				.getImageRegistry()
				.get(MarketplaceClientUiPlugin.NO_ICON_PROVIDED));
	}

	public MarketplaceDiscoveryResources getResources() {
		return (MarketplaceDiscoveryResources) resources;
	}

	private boolean hasOverviewUrl(CatalogItem connector) {
		return connector.getOverview() != null && connector.getOverview().getUrl() != null
				&& connector.getOverview().getUrl().trim().length() > 0;
	}

	private synchronized boolean computeBrowserAvailable(Composite composite) {
		if (browserAvailable == null) {
			// SWT Snippet148: detect if a browser is available by attempting to create one
			// SWTError is thrown if not available.
			try {
				Browser browser = new Browser(composite, SWT.NULL);
				browser.dispose();
				browserAvailable = true;
			} catch (SWTError e) {
				browserAvailable = false;
			}
		}
		return browserAvailable;
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

	protected void createProviderLabel(Composite parent) {
		StyledText providerLink = StyledTextHelper.createStyledTextLabel(parent);
		//Link providerLink = new Link(parent, SWT.NONE);
		setWidgetId(providerLink, WIDGET_ID_PROVIDER);

		providerLink.setEditable(false);
		GridDataFactory.fillDefaults()
		.indent(DESCRIPTION_MARGIN_LEFT, DESCRIPTION_MARGIN_TOP)
		.span(3, 1)
		.align(SWT.BEGINNING, SWT.CENTER)
		.grab(true, false)
		.applyTo(providerLink);
		// always disabled color to make it less prominent
		providerLink.setForeground(resources.getColorDisabled());

		providerLink.setText(NLS.bind(Messages.DiscoveryItem_byProviderLicense, PROVIDER_PLACEHOLDER,
				connector.getLicense()));
		int providerPos = providerLink.getText().indexOf(PROVIDER_PLACEHOLDER);
		if (providerPos != -1) {
			String providerName = connector.getProvider();
			StyleRange range = new StyleRange(0, 0, providerLink.getForeground(), null, SWT.NONE);
			if (providerName == null) {
				providerName = Messages.DiscoveryItem_UnknownProvider;
				range.fontStyle = range.fontStyle | SWT.ITALIC;
			} else {
				range.underline = true;
				range.underlineStyle = SWT.UNDERLINE_LINK;
				LinkListener listener = new LinkListener() {

					@Override
					protected void selected(Object href, TypedEvent e) {
						String searchTerm = href.toString();
						if (searchTerm.contains(" ")) { //$NON-NLS-1$
							searchTerm = "\"" + searchTerm + "\""; //$NON-NLS-1$//$NON-NLS-2$
						}
						viewer.search(searchTerm);
					}
				};
				listener.register(providerLink);
			}
			range.start = providerPos;
			range.length = providerName.length();
			range.data = providerName;
			providerLink.replaceTextRange(providerPos, PROVIDER_PLACEHOLDER.length(), providerName);
			providerLink.replaceStyleRanges(providerPos, range.length, new StyleRange[] { range });
		}
	}

	protected void createTagsLabel(Composite parent) {
		tagsLink = StyledTextHelper.createStyledTextLabel(parent);
		setWidgetId(tagsLink, WIDGET_ID_TAGS);

		tagsLink.setEditable(false);
		GridDataFactory.fillDefaults()
		.indent(DESCRIPTION_MARGIN_LEFT, TAGS_MARGIN_TOP)
		.span(3, 1)
		.align(SWT.BEGINNING, SWT.BEGINNING)
		.grab(true, false)
		.applyTo(tagsLink);

		ITags tags = getCatalogItemNode().getTags();
		if (tags == null) {
			return;
		}
		for (ITag tag : tags.getTags()) {
			String tagName = tag.getName();
			StyledTextHelper.appendLink(tagsLink, tagName, tagName, SWT.NORMAL);
			tagsLink.append(" "); //$NON-NLS-1$
		}
		new LinkListener() {
			@Override
			protected void selected(Object href, TypedEvent e) {
				viewer.doQueryForTag(href.toString());
			}
		}.register(tagsLink);
	}

	protected boolean hasTooltip(CatalogItem connector) {
		return connector.getOverview() != null && connector.getOverview().getSummary() != null
				&& connector.getOverview().getSummary().length() > 0;
	}

	/**
	 * @deprecated use {@link #maybeModifySelection(Operation)}
	 */
	@Deprecated
	protected boolean maybeModifySelection(org.eclipse.epp.internal.mpc.ui.wizards.Operation operation) {
		return maybeModifySelection(operation.getOperation());
	}

	protected boolean maybeModifySelection(Operation operation) {
		viewer.modifySelection(connector, operation);
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
		return viewer.getSelectionModel().getSelectedOperation(getData());
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (!isDisposed()) {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!isDisposed()) {
						refresh(true);
					}
				}
			});
		}
	}

	@Override
	protected void refresh() {
		refresh(false);
	}

	protected void refresh(boolean updateState) {
		Color foreground = getForeground();

		nameLabel.setForeground(foreground);
		description.setForeground(foreground);
		if (installInfoLink != null) {
			installInfoLink.setForeground(foreground);
		}
		if (updateState && buttonController != null) {
			buttonController.refresh();
		}
		refreshFavoriteButton();
	}

	private void hookRecursively(Control control, Listener listener) {
		control.addListener(SWT.Dispose, listener);
		control.addListener(SWT.MouseHover, listener);
		control.addListener(SWT.MouseMove, listener);
		control.addListener(SWT.MouseExit, listener);
		control.addListener(SWT.MouseDown, listener);
		control.addListener(SWT.MouseWheel, listener);
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				hookRecursively(child, listener);
			}
		}
	}

	@Override
	protected void hookTooltip(final Control parent, final Widget tipActivator, final Control exitControl,
			final Control titleControl, AbstractCatalogSource source, Overview overview, Image image) {
		final OverviewToolTip toolTip = new OverviewToolTip(parent, browser, (MarketplaceCatalogSource) source,
				overview, image);
		hookTooltip(toolTip, tipActivator, exitControl);

		if (image != null) {
			Listener listener = new Listener() {
				public void handleEvent(Event event) {
					toolTip.show(titleControl);
				}
			};
			tipActivator.addListener(SWT.MouseHover, listener);
		}

		if (tipActivator instanceof StyledText) {
			StyledText link = (StyledText) tipActivator;

			new LinkListener() {
				@Override
				protected void selected(Object href, TypedEvent e) {
					toolTip.show(titleControl);
				}
			}.register(link);
		} else {
			Listener selectionListener = new Listener() {
				public void handleEvent(Event event) {
					toolTip.show(titleControl);
				}
			};
			tipActivator.addListener(SWT.Selection, selectionListener);
		}
	}

	private void hookTooltip(final ToolTip toolTip, Widget tipActivator, final Control exitControl) {
		if (toolTip == null) {
			return;
		}
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				toolTip.hide();
			}
		};
		tipActivator.addListener(SWT.Dispose, listener);
		tipActivator.addListener(SWT.MouseWheel, listener);
		Listener exitListener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.MouseWheel:
					toolTip.hide();
					break;
				case SWT.MouseExit:
					/*
					 * Check if the mouse exit happened because we move over the tooltip
					 */
					Rectangle containerBounds = exitControl.getBounds();
					Point displayLocation = exitControl.getParent().toDisplay(containerBounds.x, containerBounds.y);
					containerBounds.x = displayLocation.x;
					containerBounds.y = displayLocation.y;
					//be a bit relaxed about this - there's a small gap between control and tooltip
					containerBounds.height += 3;
					Point cursorLocation = Display.getCurrent().getCursorLocation();
					if (containerBounds.contains(cursorLocation)) {
						break;
					}
					Shell tipShell = (Shell) toolTip.getData(Shell.class.getName());
					if (tipShell != null && !tipShell.isDisposed()) {
						Rectangle tipBounds = tipShell.getBounds();
						if (tipBounds.contains(cursorLocation)) {
							break;
						}
					}
					toolTip.hide();
					break;
				}
			}
		};
		hookRecursively(exitControl, exitListener);
	}
}

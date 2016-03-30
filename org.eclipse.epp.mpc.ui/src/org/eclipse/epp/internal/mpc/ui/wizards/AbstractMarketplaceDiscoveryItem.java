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

import org.eclipse.epp.internal.mpc.core.util.TextUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalogSource;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceDiscoveryResources.ImageReceiver;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.model.ITags;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.AbstractDiscoveryItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * @author Steffen Pingel
 * @author David Green
 * @author Carsten Reckord
 */
public abstract class AbstractMarketplaceDiscoveryItem<T extends CatalogItem> extends AbstractDiscoveryItem<T> {

	protected static final String INFO_HREF = "info"; //$NON-NLS-1$

	private static final String PROVIDER_PLACEHOLDER = "@PROVIDER@"; //$NON-NLS-1$

	protected static final int DESCRIPTION_MARGIN_LEFT = 8;

	protected static final int DESCRIPTION_MARGIN_TOP = 8;

	protected static final int TAGS_MARGIN_TOP = 2;

	protected static final int SEPARATOR_MARGIN_TOP = 8;

	protected static final int MAX_IMAGE_HEIGHT = 86;

	protected static final int MIN_IMAGE_HEIGHT = 64;

	protected static final int MAX_IMAGE_WIDTH = 75;

	public static final String WIDGET_ID_KEY = AbstractMarketplaceDiscoveryItem.class.getName() + "::part"; //$NON-NLS-1$

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

	protected final CatalogItem connector;

	private StyledText description;

	private Label iconLabel;

	private Label nameLabel;

	private final CatalogViewer viewer;

	private StyledText installInfoLink;

	protected final IMarketplaceWebBrowser browser;

	private StyledText tagsLink;

	private static Boolean browserAvailable;

	public AbstractMarketplaceDiscoveryItem(Composite parent, int style, MarketplaceDiscoveryResources resources,
			IMarketplaceWebBrowser browser, final T connector, CatalogViewer viewer) {
		super(parent, style, resources, connector);
		this.browser = browser;
		this.connector = connector;
		this.viewer = viewer;
		createContent();
		this.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = getNameLabelText();
			}
		});
	}

	protected void createContent() {
		createContent(this);
		createSeparator(this);
	}

	protected boolean alignIconWithName() {
		return false;
	}

	protected void createContent(Composite parent) {
		GridLayoutFactory.swtDefaults()
		.numColumns(4)
		.equalWidth(false)
		.extendedMargins(0, 0, 2, 0)
		.spacing(0, 0)
		.applyTo(this);

		if (alignIconWithName()) {
			createIconContainer(parent);
		} else {
			new Label(parent, SWT.NONE).setText(" "); // spacer //$NON-NLS-1$
		}

		createNameLabel(parent);

		if (!alignIconWithName()) {
			createIconContainer(parent);
		}
		createIconControl(checkboxContainer);

		createDescription(parent);

		createProviderLabel(parent);
		createTagsLabel(parent);

		createSocialButtons(parent);
		createInstallInfo(parent);

		createInstallButtons(parent);
	}

	private void createIconContainer(Composite parent) {
		checkboxContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults()
		.indent(0, DESCRIPTION_MARGIN_TOP)
		.align(SWT.CENTER, SWT.BEGINNING)
		.hint(MAX_IMAGE_WIDTH, SWT.DEFAULT)
		.grab(false, true)
		.minSize(MAX_IMAGE_WIDTH, MIN_IMAGE_HEIGHT)
		.span(1, alignIconWithName() ? 4 : 3)
		.applyTo(checkboxContainer);
	}

	protected void createSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
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

	protected void createDescription(Composite parent) {
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

	protected void createNameLabel(Composite parent) {
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
		nameLabel.setText(getNameLabelText());
	}

	protected String getNameLabelText() {
		return TextUtil.escapeText(connector.getName());
	}

	protected abstract void createInstallButtons(Composite parent);

	protected abstract void createInstallInfo(Composite parent);

	protected abstract void createSocialButtons(Composite parent);

	private INode getCatalogItemNode() {
		Object data = connector.getData();
		if (data instanceof INode) {
			INode node = (INode) data;
			return node;
		}
		return null;
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

	protected void createIconControl(Composite checkboxContainer) {
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
						searchForProvider(searchTerm);
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

	protected abstract void searchForProvider(String searchTerm);

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
				if (href != null) {
					searchForTag(href.toString());
				}
			}
		}.register(tagsLink);
	}

	protected abstract void searchForTag(String tag);

	protected boolean hasTooltip(CatalogItem connector) {
		return connector.getOverview() != null && connector.getOverview().getSummary() != null
				&& connector.getOverview().getSummary().length() > 0;
	}

	@Override
	public boolean isSelected() {
		return getData().isSelected();
	}

	@Override
	protected void refresh() {
		Color foreground = getForeground();

		nameLabel.setForeground(foreground);
		description.setForeground(foreground);
		if (installInfoLink != null) {
			installInfoLink.setForeground(foreground);
		}
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

	protected void hookTooltip(final ToolTip toolTip, Widget tipActivator, final Control exitControl) {
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

	protected CatalogViewer getViewer() {
		return viewer;
	}
}

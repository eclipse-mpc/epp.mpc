/*******************************************************************************
 * Copyright (c) 2010-2017 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - error handling (bug 374105)
 *     Mickael Istria (Red Hat Inc.) - Discovery
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.ui.messages"; //$NON-NLS-1$

	public static String ProjectNatures;

	public static String PreferencePage_linkToEditorSettings;

	public static String CatalogExtensionPointReader_cannotFindResource;

	public static String CatalogExtensionPointReader_cannotRegisterCatalog_bundle_reason;

	public static String CatalogExtensionPointReader_labelRequired;

	public static String CatalogExtensionPointReader_urlRequired;

	public static String AskMarketPlaceForFileSupportStrategy_jobName;

	public static String AskMerketplaceForFileSupportStrategy_dialogJobName;

	/**
	 * Only kept here to give translations time to move the message to the new bundle without causing warnings.
	 *
	 * @deprecated moved to {@link org.eclipse.epp.internal.mpc.core.Messages#MarketplaceClientCore_message_message2}
	 */
	@Deprecated
	public static String MarketplaceClientUi_message_message2;

	public static String MarketplaceClientUi_unexpectedException_reason;

	/**
	 * Only kept here to give translations time to move the message to the new bundle without causing warnings.
	 *
	 * @deprecated moved to {@link org.eclipse.epp.internal.mpc.core.Messages#MarketplaceClientCore_notFound}
	 */
	@Deprecated
	public static String MarketplaceClientUi_notFound;

	/**
	 * Only kept here to give translations time to move the message to the new bundle without causing warnings.
	 *
	 * @deprecated moved to {@link org.eclipse.epp.internal.mpc.core.Messages#MarketplaceClientCore_unknownHost}
	 */
	@Deprecated
	public static String MarketplaceClientUi_unknownHost;

	/**
	 * Only kept here to give translations time to move the message to the new bundle without causing warnings.
	 *
	 * @deprecated moved to {@link org.eclipse.epp.internal.mpc.core.Messages#MarketplaceClientCore_connectionProblem}
	 */
	@Deprecated
	public static String MarketplaceClientUi_connectionProblem;

	public static String MarketplaceOrAssociateDialog_title;

	public static String MarketplaceOrAssociateDialog_linkToPreferences;

	public static String MarketplaceOrAssociateDialog_message;

	public static String MarketplaceOrAssociateDialog_showProposals;

	public static String MarketplaceOrAssociateDialog_associate;

	public static String MarketplaceOrAssociateDialog_descriptionEmbeddedSystemEditor;

	public static String MarketplaceOrAssociateDialog_descriptionExternalSystemEditor;

	public static String MarketplaceOrAssociateDialog_descriptionSimpleTextEditor;

	public static String MissingNatureDetector_Desc;
	public static String MissingNatureDetector_Message;
	public static String MissingNatureDetector_ShowSolutions;
	public static String MissingNatureDetector_Title;
	public static String MissingNatureDetector_jobName;
	public static String MissingNatureDetector_enable;
	public static String MissingNatureDetector_linkToPreferences;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

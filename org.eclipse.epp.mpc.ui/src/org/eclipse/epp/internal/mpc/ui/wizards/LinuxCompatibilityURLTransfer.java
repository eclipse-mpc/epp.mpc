/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TransferData;

final class LinuxCompatibilityURLTransfer extends ByteArrayTransfer {
	private static final String TEXT_URILIST;

	private static final String TEXT_HTML;

	private static final int TEXT_URILIST_ID;

	private static final int TEXT_HTML_ID;

	private static final Field TRANSFERDATA__TYPE;

	private static LinuxCompatibilityURLTransfer _instance;

	static {
		TEXT_URILIST = "text/uri-list"; //$NON-NLS-1$
		TEXT_HTML = "text/html"; //$NON-NLS-1$

		int uriListId = -1;
		int htmlId = -1;
		_instance = null;

		Field transferDataType = null;
		if (Platform.OS_LINUX.equals(Platform.getOS()) && Util.isGtk()) {
			try {
				transferDataType = TransferData.class.getDeclaredField("type"); //$NON-NLS-1$
			} catch (Exception e) {
				//just skip all other initialization and disable this transfer
			}
			if (transferDataType != null) {
				uriListId = registerType(TEXT_URILIST);
				htmlId = registerType(TEXT_HTML);
				_instance = new LinuxCompatibilityURLTransfer();
			}
		}
		TEXT_URILIST_ID = uriListId;
		TEXT_HTML_ID = htmlId;
		TRANSFERDATA__TYPE = transferDataType;
	}

	static LinuxCompatibilityURLTransfer getInstance() {
		return _instance;
	}

	private LinuxCompatibilityURLTransfer() {
	}

	@Override
	public Object nativeToJava(TransferData transferData) {
		if (!isSupportedType(transferData)) {
			return null;
		}
		try {
			//Both HTMLTransfer and FileTransfer support decoding text/uri-list.
			//However, FileTransfer proceeds with processing the result as a file,
			//so use HTMLTransfer as a delegate
			setTransferType(transferData, TEXT_HTML_ID);
			Object data = HTMLTransfer.getInstance().nativeToJava(transferData);
			if (validate(data)) {
				return data;
			}
		} finally {
			setTransferType(transferData, TEXT_URILIST_ID);
		}
		return null;
	}

	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if (!checkURL(object) || !isSupportedType(transferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}
		try {
			setTransferType(transferData, TEXT_HTML_ID);
			HTMLTransfer.getInstance().javaToNative(object, transferData);
		} finally {
			setTransferType(transferData, TEXT_URILIST_ID);
		}
	}

	private void setTransferType(TransferData transferData, int type) {
		//use reflection because type field can be either int or long depending on host architecture
		//also, avoid compile-time dependency to platform-specific field
		try {
			TRANSFERDATA__TYPE.set(transferData, type);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			//ignore - this will just treat the transferData as if it had an incompatible type
		}
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TEXT_URILIST_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TEXT_URILIST };
	}

	boolean checkURL(Object object) {
		return object != null && (object instanceof String) && ((String) object).length() > 0;
	}

	@Override
	protected boolean validate(Object object) {
		return checkURL(object);
	}
}
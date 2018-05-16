/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.osgi.framework.Version;

public class Util {
	private static final String GTK_VERSION_PROPERTY = "org.eclipse.swt.internal.gtk.version"; //$NON-NLS-1$

	private static Boolean isGtk3;

	/**
	 * Scale an image to a size that conforms to the given maximums while maintaining its original aspect ratio.
	 *
	 * @param image
	 *            the image to scale
	 * @param maxWidth
	 *            the maximum width of the new image
	 * @param maxHeight
	 *            the maximum height of the new image
	 * @return a new image, which must be disposed by the caller.
	 */
	public static Image scaleImage(Image image, int maxWidth, int maxHeight) {
		return scaleImage(image, maxWidth, maxHeight, null);
	}

	/**
	 * Scale an image to a size that conforms to the given maximums while maintaining its original aspect ratio.
	 *
	 * @param image
	 *            the image to scale
	 * @param maxWidth
	 *            the maximum width of the new image
	 * @param maxHeight
	 *            the maximum height of the new image
	 * @param background
	 *            background color used in place of transparency
	 * @return a new image, which must be disposed by the caller.
	 */
	public static Image scaleImage(Image image, int maxWidth, int maxHeight, Color background) {
		// scale the image using native scaling
		// and maintain aspect ratio
		Rectangle bounds = image.getBounds();
		int newHeight;
		int newWidth;
		float widthRatio = ((float) bounds.width) / maxWidth;
		float heightRatio = ((float) bounds.height) / maxHeight;
		if (widthRatio > heightRatio) {
			newWidth = maxWidth;
			newHeight = -1;
		} else {
			newWidth = -1;
			newHeight = maxHeight;
		}

		if (newHeight == -1) {
			newHeight = Math.min(maxHeight, (int) (bounds.height / widthRatio));
		} else {
			newWidth = Math.min(maxWidth, (int) (bounds.width / heightRatio));
		}

		Image scaledImage = new Image(image.getDevice(), newWidth, newHeight);
		GC gc = new GC(scaledImage);
		try {
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			if (background != null) {
				gc.setBackground(background);
				gc.fillRectangle(0, 0, newWidth, newHeight);
			}
			gc.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, newWidth, newHeight);
		} finally {
			gc.dispose();
		}
		return scaledImage;
	}

	/**
	 * Compute the message type of the given status.
	 *
	 * @see IMessageProvider
	 */
	public static int computeMessageType(IStatus status) {
		int messageType;
		switch (status.getSeverity()) {
		case IStatus.OK:
		case IStatus.INFO:
			messageType = IMessageProvider.INFORMATION;
			break;
		case IStatus.WARNING:
			messageType = IMessageProvider.WARNING;
			break;
		default:
			messageType = IMessageProvider.ERROR;
			break;
		}
		return messageType;
	}

	public static boolean isGtk3() {
		if (isGtk3 == null) {
			if (Platform.WS_GTK.equals(Platform.getWS())) {
				String gtkVersionStr = System.getProperty(GTK_VERSION_PROPERTY);
				if (gtkVersionStr != null) {
					Version gtkVersion = new Version(gtkVersionStr);
					isGtk3 = gtkVersion.getMajor() >= 3;
				} else {
					isGtk3 = false;
				}
			} else {
				isGtk3 = false;
			}
		}
		return isGtk3;
	}
}

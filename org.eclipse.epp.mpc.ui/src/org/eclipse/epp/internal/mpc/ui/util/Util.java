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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

public class Util {
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
		// scale the image using native scaling
		// and maintain aspect ratio
		Rectangle bounds = image.getBounds();
		int newHeight;
		int newWidth;
		float widthRatio = ((float) bounds.width) / maxWidth;
		float heightRatio = ((float) bounds.height) / maxHeight;
		if (widthRatio > heightRatio) {
			newWidth = maxWidth;
			newHeight = Math.min(maxHeight, (int) (bounds.height / widthRatio));
		} else {
			newWidth = Math.min(maxWidth, (int) (bounds.width / heightRatio));
			newHeight = maxHeight;
		}
		final Image scaledImage = new Image(image.getDevice(), newWidth, newHeight);
		GC gc = new GC(scaledImage);
		try {
			gc.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, newWidth, newHeight);
		} finally {
			gc.dispose();
		}
		return scaledImage;
	}

}

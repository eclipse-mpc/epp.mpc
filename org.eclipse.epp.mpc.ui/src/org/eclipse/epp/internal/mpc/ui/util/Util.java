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

import java.awt.AWTError;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.osgi.framework.Version;

public class Util {
	private static final String GTK_VERSION_PROPERTY = "org.eclipse.swt.internal.gtk.version"; //$NON-NLS-1$
	private static Boolean useAwt;

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

		Image scaledImage = null;
		// try high-quality scaling using AWT
		if (useAwt()) {
			scaledImage = scaleImageAwt(image, newWidth, newHeight);
		}
		//fall back to SWT method
		if (scaledImage == null)
		{
			if (newHeight == -1) {
				newHeight = Math.min(maxHeight, (int) (bounds.height / widthRatio));
			} else {
				newWidth = Math.min(maxWidth, (int) (bounds.width / heightRatio));
			}
			scaledImage = scaleImageSwt(image, newWidth, newHeight);
		}
		return scaledImage;
	}

	private static boolean useAwt() {
		if (useAwt == null) {
			useAwt = !isGtk3();
		}
		return useAwt;
	}

	private static Image scaleImageSwt(Image image, int newWidth, int newHeight) {
		Rectangle bounds = image.getBounds();
		Image scaledImage = new Image(image.getDevice(), newWidth, newHeight);
		GC gc = new GC(scaledImage);
		try {
			gc.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, newWidth, newHeight);
		} finally {
			gc.dispose();
		}
		return scaledImage;
	}

	private static Image scaleImageAwt(Image image, int newWidth, int newHeight) {
		try {
			// convert to awt
			BufferedImage img = convertToAWT(image.getImageData());

			// scale using best scaling filter; currently AreaAveragingScaleFilter, see BufferedImage.getScaledInstance() hint for SCALE_SMOOTH.
			// we could just call BufferedImage.getScaledInstance(), but that does a full AWT initialization.
			ImageFilter filter;
			filter = new AreaAveragingScaleFilter(newWidth, newHeight);
			ImageProducer prod;
			prod = new FilteredImageSource(img.getSource(), filter);
			final PixelGrabber pixelGrabber = new PixelGrabber(prod, 0, 0, newWidth, newHeight, null, 0, newWidth) {
				@Override
				public void setDimensions(int width, int height) {
					super.setDimensions(width, height);
				}
			};
			if (pixelGrabber.grabPixels()) {
				final Image scaledImage = new Image(image.getDevice(), convertToSWT(pixelGrabber));
				return scaledImage;
			}
			// else it didn't work on this image - no cause to completely disable AWT...
		} catch (Exception e) {
			//something went wrong with AWT - disable it
			useAwt = false;
		} catch (AWTError e) {
			useAwt = false;
		}
		return null;
	}

	/**
	 * Convert SWT image to AWT using <a href=
	 * "http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java"
	 * >SWT code snippet</a>.
	 *
	 * @param data
	 *            the SWT image to convert
	 * @return the converted AWT image
	 */
	private static BufferedImage convertToAWT(ImageData data) {
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			// To avoid transparency issues we just use the default ARGB color model here always.
			// It's used by setRGB below anyway...
			BufferedImage bufferedImage = new BufferedImage(data.width, data.height, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					bufferedImage.setRGB(x, y, data.getAlpha(x, y) << 24 | rgb.red << 16 | rgb.green << 8 | rgb.blue);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			ColorModel colorModel;
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(
					data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

	/**
	 * Convert SWT image to AWT. This is based on the <a href=
	 * "http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java"
	 * >SWT code snippet</a>, but rewritten to work on a PixelGrabber so we can avoid a full AWT initialization.
	 *
	 * @param bufferedImage
	 *            the AWT image to convert
	 * @return the converted SWT image
	 */
	private static ImageData convertToSWT(PixelGrabber pixels) {
		final byte[] byteData;
		final int[] intData;
		{
			Object data = pixels.getPixels();
			assert data != null;
			if (data instanceof int[]) {
				intData = (int[]) data;
				byteData = null;
			} else {
				byteData = (byte[]) data;
				intData = null;
			}
		}
		final int width = pixels.getWidth();
		final int height = pixels.getHeight();
		if (pixels.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) pixels.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
					colorModel.getBlueMask());
			final boolean hasAlpha = colorModel.hasAlpha();
			ImageData imageData = new ImageData(width, height, colorModel.getPixelSize(), palette);
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					// either intData or byteData are always != null
					@SuppressWarnings("null")
					int rgb = intData != null ? intData[width * y + x]
							: colorModel.getRGB(byteData[width * y + x] & 0xff);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					imageData.setPixel(x, y, pixel);
					if (hasAlpha) {
						imageData.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return imageData;
		} else if (pixels.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) pixels.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(width, height, colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					// either intData or byteData are always != null
					@SuppressWarnings("null")
					int pixel = intData != null ? intData[y * width + x] : byteData[y * width + x] & 0xff;
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		}
		return null;
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

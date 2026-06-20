package com.arceuustimers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class IconUtil {
	private IconUtil() {
	}

	public static BufferedImage smoothDownscale(BufferedImage source, int targetWidth, int targetHeight) {
		BufferedImage image = source;
		int width = image.getWidth();
		int height = image.getHeight();

		while (width / 2 >= targetWidth && height / 2 >= targetHeight) {
			width /= 2;
			height /= 2;
			image = scaleStep(image, width, height);
		}

		if (width != targetWidth || height != targetHeight) image = scaleStep(image, targetWidth, targetHeight);
		return image;
	}

	private static BufferedImage scaleStep(BufferedImage source, int width, int height) {
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.drawImage(source, 0, 0, width, height, null);
		graphics.dispose();
		return scaled;
	}
}

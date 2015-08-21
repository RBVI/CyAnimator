package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.AlphaComposite;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.TexturePaint;

import java.io.File;

import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;


public class CrossfadeCustomGraphics implements CyCustomGraphics<ImageCustomGraphicLayer> {
	BufferedImage imageOne;
	BufferedImage imageTwo;
	BufferedImage dest = null;
	float step;
	Long id = null;
	float fitRatio = 1.0f;
	int width;
	int height;

	public CrossfadeCustomGraphics(BufferedImage imageOne, BufferedImage imageTwo, float step) {
		this.imageOne = imageOne;
		this.imageTwo = imageTwo;
		this.step = step;
		width = getSize(imageOne.getWidth(), imageTwo.getWidth());
		height = getSize(imageOne.getHeight(), imageTwo.getHeight());
		/*
		if (imageOne != null) {
			try {
			ImageIO.write(imageOne, "png", new File("imageOne"+this+".png"));
			} catch(Exception e){e.printStackTrace();}
		}
		if (imageTwo != null) {
			try {
			ImageIO.write(imageTwo, "png", new File("imageTwo"+this+".png"));
			} catch(Exception e){e.printStackTrace();}
		}
		*/
		this.dest = convolve();
	}

	@Override
	public String getDisplayName() {return "Crossfade image";}

	@Override
	public void setDisplayName(String name) {}

	@Override
	public float getFitRatio() {return fitRatio;}

	@Override
	public void setFitRatio(float fitRatio) {this.fitRatio = fitRatio;}

	@Override
	public int getHeight() {return height;}

	@Override
	public void setHeight(int height) {this.height = height;}

	@Override
	public int getWidth() {return width;}

	@Override
	public void setWidth(int width) {this.width = width;}

	@Override
	public Long getIdentifier() {return id;}

	@Override
	public void setIdentifier(Long id) {this.id = id;}

	@Override
	public List<ImageCustomGraphicLayer> getLayers(CyNetworkView networkView, 
	                                          View<? extends CyIdentifiable> grView) {
		// System.out.println("CrossfadeCustomGraphics: "+this+" getLayers");
		// Thread.dumpStack();

		Rectangle2D bounds = new Rectangle2D.Double(0.0, 0.0, width, height);
		ImageCustomGraphicLayer l = new ImageCustomGraphicLayerImpl(bounds, dest);

		return Collections.singletonList(l);
	}

	@Override
	public Image getRenderedImage() {
		// System.out.println("CrossfadeCustomGraphics: "+this+" getRenderedImage");
		// Thread.dumpStack();
		return dest;
	}

	@Override
	public String toSerializableString() {
		return null;
	}

	BufferedImage convolve() {
		// return imageOne;
		dest = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB);
		Graphics2D destG = dest.createGraphics();
		destG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f-step));
		destG.drawImage(imageOne, 0, 0, null);
		destG.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR, step));
		destG.drawImage(imageTwo, 0, 0, null);
		destG.dispose();
		return dest;
	}

	int getSize(int sizeOne, int sizeTwo) {
		if (sizeOne == sizeTwo) return sizeOne;
		int delta = (int)((float)(sizeTwo-sizeOne) * step);
		return sizeOne + delta;
	}

	class ImageCustomGraphicLayerImpl implements ImageCustomGraphicLayer {
		final Rectangle2D bounds;
		final BufferedImage dest;

		public ImageCustomGraphicLayerImpl(final Rectangle2D bounds, final BufferedImage dest) {
			this.bounds = bounds;
			if (bounds.getWidth() != dest.getWidth() || 
					bounds.getHeight() != dest.getHeight()) {
				// Resize our image
				this.dest = resize(dest, bounds);
			} else {
				this.dest = dest;
			}
			System.out.println("ImageCustomGraphicsLayerImpl size: "+bounds);
			// Thread.dumpStack();
		}

		public TexturePaint getPaint(Rectangle2D bounds) {
			return new TexturePaint(dest, bounds);
		}

		public Rectangle2D getBounds2D() {
			return bounds;
		}

		public ImageCustomGraphicLayer transform(AffineTransform xform) {
			System.out.println("ImageCustomGraphicLayer transform: "+xform);
			// Thread.dumpStack();
			Shape s = xform.createTransformedShape(bounds);
			return new ImageCustomGraphicLayerImpl(s.getBounds2D(), dest);
		}

		private BufferedImage resize(BufferedImage img, Rectangle2D bounds) {
			BufferedImage newImage = 
							new BufferedImage((int)bounds.getWidth(), (int)bounds.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = newImage.createGraphics();
			g2.drawImage(img, 0, 0, (int)bounds.getWidth(), (int)bounds.getHeight(), null);
			g2.dispose();
			return newImage;
		}
	}

}

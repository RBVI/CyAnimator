package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.image.BufferedImage;
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


public class BaseCustomGraphicsLayer implements CustomGraphicLayer {
	float step;
	Boolean fadeIn;
	Long id = null;
	float fitRatio = 1.0f;
	int width;
	int height;
	CustomGraphicLayer layerOne;
	CustomGraphicLayer layerTwo;
	Rectangle2D bounds1;
	Rectangle2D bounds2;
	protected Rectangle2D rectangle;

	public BaseCustomGraphicsLayer(CustomGraphicLayer layerOne, 
	                               CustomGraphicLayer layerTwo, float step, Boolean fadeIn) {
		this.layerOne = layerOne;
		this.layerTwo = layerTwo;
		this.step = step;
		this.fadeIn = fadeIn;
		rectangle = new Rectangle2D.Double(0, 0, 1000, 1000);
	}

	@Override
	public Rectangle2D getBounds2D() {
		return rectangle;
	}

	@Override
	public Paint getPaint(Rectangle2D bounds) {
		BufferedImage blend = new BufferedImage((int)bounds.getWidth(), (int)bounds.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Rectangle2D shape = new Rectangle2D.Double(0.0, 0.0, bounds.getWidth(), bounds.getHeight());
		Paint paintOne = layerOne.getPaint(bounds);
		Paint paintTwo = layerTwo.getPaint(bounds);
		Graphics2D g2 = blend.createGraphics();

		if (fadeIn != null) {
			if (fadeIn) {
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, step));
			} else {
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1-step));
			}
			g2.setPaint(paintTwo);
			g2.fill(shape);
		} else {
			if (step <= 0.5) {
				g2.setPaint(paintOne);
				g2.fill(shape);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, step));
				g2.setPaint(paintTwo);
				g2.fill(shape);
			} else {
				g2.setPaint(paintTwo);
				g2.fill(shape);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1-step));
				g2.setPaint(paintOne);
				g2.fill(shape);
			}
		}

		TexturePaint paint = new TexturePaint(blend, bounds);
		return paint;
	}

	@Override
	public BaseCustomGraphicsLayer transform(AffineTransform xform) {
		layerOne = layerOne.transform(xform);
		layerTwo = layerTwo.transform(xform);
		BaseCustomGraphicsLayer newLayer = new BaseCustomGraphicsLayer(layerOne, layerTwo, step, fadeIn);
		newLayer.rectangle = xform.createTransformedShape(rectangle).getBounds2D();
		return newLayer;
	}
}

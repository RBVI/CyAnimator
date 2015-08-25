package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.TexturePaint;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.Cy2DGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;


public class Cy2DLayer implements Cy2DGraphicLayer {
	float step;
	Long id = null;
	float fitRatio = 1.0f;
	int width;
	int height;
	List<?> layersOne;
	List<?> layersTwo;
	protected Rectangle2D rectangle = null;

	public Cy2DLayer(List<?> layersOne, List<?> layersTwo, float step) {
		this.layersOne = layersOne;
		this.layersTwo = layersTwo;
		this.step = step;
		rectangle = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);
		// System.out.println("LayerOne has "+layersOne.size()+" layers and LayerTwo has "+layersTwo.size()+" layers");
		for (Object l: layersOne) {
			if (l instanceof PaintedShape) {
				rectangle = rectangle.createUnion(((PaintedShape)l).getBounds2D());
			} else if (l instanceof Cy2DGraphicLayer) {
				rectangle = rectangle.createUnion(((Cy2DGraphicLayer)l).getBounds2D());
			}
		}

		for (Object l: layersTwo) {
			if (l instanceof PaintedShape) {
				rectangle = rectangle.createUnion(((PaintedShape)l).getBounds2D());
			} else if (l instanceof Cy2DGraphicLayer) {
				rectangle = rectangle.createUnion(((Cy2DGraphicLayer)l).getBounds2D());
			}
		}
		// System.out.println("Bounds = "+rectangle);

	}

	public Cy2DLayer(List<?> layersOne, List<?> layersTwo, float step, Rectangle2D rectangle) {
		this.layersOne = layersOne;
		this.layersTwo = layersTwo;
		this.step = step;
		this.rectangle = rectangle;
	}

	@Override
	public Rectangle2D getBounds2D() {
		return rectangle;
	}

	@Override
	public Paint getPaint(Rectangle2D bounds) {
		return Color.BLACK;
	}

	@Override
	public void draw(Graphics2D g2, Shape shape, CyNetworkView networkView, View<? extends CyIdentifiable> view) {
		Composite original = g2.getComposite();
		if (step <= 0.5)  {
			renderShapes(layersOne, g2, shape, networkView, view);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, step));
			renderShapes(layersTwo, g2, shape, networkView, view);
		} else {
			renderShapes(layersTwo, g2, shape, networkView, view);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1-step));
			renderShapes(layersOne, g2, shape, networkView, view);
		}
		g2.setComposite(original);
	}

	@Override
	public Cy2DLayer transform(AffineTransform xform) {
		List<CustomGraphicLayer> newLayersOne = new ArrayList<>();
		for (Object obj: layersOne) {
			CustomGraphicLayer layer = (CustomGraphicLayer)obj;
			newLayersOne.add((CustomGraphicLayer)layer.transform(xform));
		}
		List<CustomGraphicLayer> newLayersTwo = new ArrayList<>();
		for (Object obj: layersTwo) {
			CustomGraphicLayer layer = (CustomGraphicLayer)obj;
			newLayersTwo.add((CustomGraphicLayer)layer.transform(xform));
		}
		// Rectangle2D rect = xform.createTransformedShape(rectangle).getBounds2D();
		Cy2DLayer newLayer = new Cy2DLayer(layersOne, layersTwo, step);
		return newLayer;
	}

	private void renderShapes(List<?> layers, Graphics2D g2, 
	                          Shape shape, CyNetworkView networkView, View<? extends CyIdentifiable> view) {
		for (Object obj: layers) {
			if (obj instanceof PaintedShape) {
				PaintedShape ps = (PaintedShape)obj;
				Shape s = ps.getShape();
				// System.out.println("s = "+s);
				// System.out.println("Paint = "+ps.getPaint());
				// System.out.println("Bounds = "+ps.getBounds2D());
				if (s == null) continue;

				if (ps.getStroke() != null) {
					Paint strokePaint = ps.getStrokePaint();
					if (strokePaint == null) strokePaint = Color.BLACK;
					g2.setPaint(strokePaint);
					g2.setStroke(ps.getStroke());
					g2.draw(s);
				}
				g2.setPaint(ps.getPaint());
				g2.fill(s);
			} else if (obj instanceof Cy2DGraphicLayer) {
				Cy2DGraphicLayer l = (Cy2DGraphicLayer)obj;
				l.draw(g2, shape, networkView, view); 
			}
		}
	}
}

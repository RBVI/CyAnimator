package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.AlphaComposite;
import java.awt.Color;
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
import org.cytoscape.view.presentation.customgraphics.PaintedShape;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;


public class PaintedShapeLayer implements PaintedShape {
	float step;
	Long id = null;
	float fitRatio = 1.0f;
	int width;
	int height;
	List<?> layersOne;
	List<?> layersTwo;
	protected Rectangle2D rectangle;

	public PaintedShapeLayer(List<?> layersOne, List<?> layersTwo, float step) {
		this.layersOne = layersOne;
		this.layersTwo = layersTwo;
		this.step = step;
		Area totalArea = new Area();
		for (Object obj: layersOne) {
			PaintedShape ps = (PaintedShape) obj;
			totalArea.add(new Area(ps.getShape()));
		}
		for (Object obj: layersOne) {
			PaintedShape ps = (PaintedShape) obj;
			totalArea.add(new Area(ps.getShape()));
		}
		rectangle = totalArea.getBounds2D();
	}

	@Override
	public Rectangle2D getBounds2D() {
		return rectangle;
	}

	@Override
	public Shape getShape() {return rectangle;}

	@Override
	public Stroke getStroke() {return null;}

	@Override
	public Paint getStrokePaint() {return null;}

	@Override
	public Paint getPaint(Rectangle2D bounds) {
		if (bounds.getWidth() == 0.0 || bounds.getHeight() == 0.0)  {
			return Color.BLACK;
		}

		BufferedImage blend = new BufferedImage((int)bounds.getWidth(), (int)bounds.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = blend.createGraphics();

		if (step <= 0.5) {
			renderShapes(layersOne, g2);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, step));
			renderShapes(layersTwo, g2);
		} else {
			renderShapes(layersTwo, g2);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1-step));
			renderShapes(layersOne, g2);
		}
		g2.dispose();

		TexturePaint paint = new TexturePaint(blend, new Rectangle2D.Double(0.0, 0.0, bounds.getWidth(), bounds.getHeight()));
		return paint;
	}

	@Override
	public Paint getPaint() {
		return getPaint(rectangle);
	}

	@Override
	public PaintedShapeLayer transform(AffineTransform xform) {
		List<PaintedShape> newLayersOne = new ArrayList<>();
		for (Object obj: layersOne) {
			PaintedShape layer = (PaintedShape)obj;
			newLayersOne.add((PaintedShape)layer.transform(xform));
		}
		List<PaintedShape> newLayersTwo = new ArrayList<>();
		for (Object obj: layersTwo) {
			PaintedShape layer = (PaintedShape)obj;
			newLayersTwo.add((PaintedShape)layer.transform(xform));
		}
		PaintedShapeLayer newLayer = new PaintedShapeLayer(newLayersOne, newLayersTwo, step);
		return newLayer;
	}

	private void renderShapes(List<?> layers, Graphics2D g2) {
		for (Object obj: layers) {
			PaintedShape ps = (PaintedShape)obj;
			Shape shape = ps.getShape();

			if (ps.getStroke() != null) {
				Paint strokePaint = ps.getStrokePaint();
				if (strokePaint == null) strokePaint = Color.BLACK;
				g2.setPaint(strokePaint);
				g2.setStroke(ps.getStroke());
				g2.draw(shape);
			}
			g2.setPaint(ps.getPaint());
			g2.fill(shape);
		}
	}
}

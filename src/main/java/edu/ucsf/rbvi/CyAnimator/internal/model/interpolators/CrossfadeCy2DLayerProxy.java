package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.AlphaComposite;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Paint;
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
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;


public class CrossfadeCy2DLayerProxy implements CyCustomGraphics<Cy2DLayer> {
	float step;
	Boolean fadeIn;
	Long id = null;
	float fitRatio = 1.0f;
	int width;
	int height;
	CyCustomGraphics<?> cgOne;
	CyCustomGraphics<?> cgTwo;

	public CrossfadeCy2DLayerProxy(CyCustomGraphics<?> cgOne, 
	                               CyCustomGraphics<?> cgTwo, float step, Boolean fadeIn) {
		this.cgOne = cgOne;
		this.cgTwo = cgTwo;
		this.step = step;
		this.fadeIn = fadeIn;
	}

	@Override
	public String getDisplayName() {return "Crossfade proxy";}

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
	public List<Cy2DLayer> getLayers(CyNetworkView networkView, 
	                                         View<? extends CyIdentifiable> grView) {
		List<?> layersOne = cgOne.getLayers(networkView, grView);
		List<?> layersTwo = cgTwo.getLayers(networkView, grView);

		if (layersOne == null || layersOne.size() == 0 || layersTwo == null || layersTwo.size() == 0)
			return null;

		return Collections.singletonList(new Cy2DLayer(layersOne, layersTwo, step, fadeIn));
	}

	@Override
	public Image getRenderedImage() {
		// System.out.println("CrossfadeCustomGraphics: "+this+" getRenderedImage");
		// Thread.dumpStack();
		return null;
	}

	@Override
	public String toSerializableString() {
		return null;
	}

}

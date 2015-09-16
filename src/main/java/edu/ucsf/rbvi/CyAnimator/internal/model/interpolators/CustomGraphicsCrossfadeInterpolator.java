/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.Color;
import java.awt.Image;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;

import org.cytoscape.view.presentation.customgraphics.Cy2DGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

/*
 * This isn't a real crossfade implementation since there is really
 * no way to do that through the visual property interface.  We
 * actually just fadeout/fadein, which is why we need the appropriate
 * transparency property...
 */
public class CustomGraphicsCrossfadeInterpolator implements FrameInterpolator {

	public CustomGraphicsCrossfadeInterpolator(){
	}

	public int passNumber() { return 1; }

	/*
	 * Interpolate size
	 */
	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, 
	                             CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int stop, CyFrame[] cyFrameArray){

		int framenum = stop-start;

		for(View<? extends CyIdentifiable> id: idList){
			Object valueOne = frameOne.getValue(id,property);
			Object valueTwo = frameTwo.getValue(id,property);

			// In fact, the values pretty much can't be null because
			// of the way custom graphics works.  Fix it up now to make
			// things consistent with our other interpolators
			
			if ((valueOne == null || valueOne.toString().equals("None")) &&
			    (valueTwo == null || valueTwo.toString().equals("None")))
				continue;

			// If valueOne and valueTwo are the same, we need to
			// tell the underlying route whether we're fadeing in or fadeing out
			Boolean fadeIn = null;

			// We've separately taken care of the case where they are both null or None
			if (valueOne == null || valueOne.toString().equals("None")) {
				valueOne = valueTwo;
				fadeIn = Boolean.TRUE;
			} else if (valueTwo == null || valueTwo.toString().equals("None")) {
				valueTwo = valueOne;
				fadeIn = Boolean.FALSE;
			}

			if (!CyCustomGraphics.class.isAssignableFrom(valueOne.getClass()) ||
					!CyCustomGraphics.class.isAssignableFrom(valueTwo.getClass())) {
				continue;
			}

			CyCustomGraphics<?> cgOne = (CyCustomGraphics)valueOne;
			CyCustomGraphics<?> cgTwo = (CyCustomGraphics)valueTwo;

			/*
			if ((cgOne.getDisplayName() != null && cgOne.getDisplayName().equals("[ Remove Graphics ]")) ||
			    (cgTwo.getDisplayName() != null && cgTwo.getDisplayName().equals("[ Remove Graphics ]")))
				continue;
			*/

			Type type = cgOne.getClass().getGenericSuperclass();

			if (type instanceof ParameterizedType) {
				Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
				if (actualType instanceof Class) {
					Class layerClass = (Class)actualType;
					if (ImageCustomGraphicLayer.class.isAssignableFrom(layerClass)) {
						for (int k = 1; k < framenum; k++) {
							float step = (float)k/(float)framenum;
							cyFrameArray[start+k].putValue(id, property, new CrossfadeCy2DLayerProxy(cgOne, cgTwo, step, fadeIn));
						}
					} else if (PaintedShape.class.isAssignableFrom(layerClass)) {
						for (int k = 1; k < framenum; k++) {
							float step = (float)k/(float)framenum;
							cyFrameArray[start+k].putValue(id, property, new CrossfadeCy2DLayerProxy(cgOne, cgTwo, step, fadeIn));
						}
					} else if (Cy2DGraphicLayer.class.isAssignableFrom(layerClass)) {
						for (int k = 1; k < framenum; k++) {
							float step = (float)k/(float)framenum;
							cyFrameArray[start+k].putValue(id, property, new CrossfadeCy2DLayerProxy(cgOne, cgTwo, step, fadeIn));
						}
					} else {
						for (int k = 1; k < framenum; k++) {
							float step = (float)k/(float)framenum;
							cyFrameArray[start+k].putValue(id, property, new CrossfadeCustomGraphicsProxy(cgOne, cgTwo, step, fadeIn));
						}
					}
				}
			} else { 
				// Might be an internal type.  Just do an image crossfade
				for (int k = 1; k < framenum; k++) {
					float step = (float)k/(float)framenum;
					cyFrameArray[start+k].putValue(id, property, new CrossfadeCy2DLayerProxy(cgOne, cgTwo, step, fadeIn));
				}
			}

		}
		return cyFrameArray;
	}
}

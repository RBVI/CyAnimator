/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.Color;
import java.awt.Paint;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

/*
 * This isn't a real crossfade implementation since there is really
 * no way to do that through the visual property interface.  We
 * actually just fadeout/fadein, which is why we need the appropriate
 * transparency property...
 */
public class CrossfadeInterpolator implements FrameInterpolator {
	final VisualProperty<?>[] transparencyProperties;

	public CrossfadeInterpolator(VisualProperty<?>... trans){
		transparencyProperties = trans;
	}

	// This interpolator must be pass 2 so that the Transparency
	// interpolators can run first
	public int passNumber() { return 2; }

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
			if (valueOne == null && valueTwo == null)
				continue;

			float transOne = 1.0f;
			float transTwo = 1.0f;
			if (valueOne == null && valueTwo != null) {
				valueOne = valueTwo;
				transOne = 0f;
			} else if (valueOne != null && valueTwo == null) {
				valueTwo = valueOne;
				transTwo = 0f;
			}

			if (valueOne.equals(valueTwo)) {
				if (transOne == transTwo) {
					for (int k=1; k < framenum+1; k++) {
						cyFrameArray[start+k].putValue(id, property, valueOne);
					}
				} else {
					// We're doing a fade-in/fade-out
					float step = (transTwo-transOne)/(float)framenum;
					float transFactor = transOne;
					for (int k=1; k< framenum; k++) {
						updateTransparencies(start+k, id, cyFrameArray, transFactor);
						transFactor += step;

						cyFrameArray[start+k].putValue(id, property, valueOne);
					}
				}
				continue;
			}

			// Crossfade
			int crossFadeDuration = 3;
			int crossFadeMid = framenum/2;
			int crossFadeStart = framenum/2-crossFadeDuration;
			int crossFadeEnd = framenum/2+crossFadeDuration;
			float transFull = 1.0f;
			float transHalf = 0.05f;
			for (int k=1; k < crossFadeStart; k++) {
				cyFrameArray[start+k].putValue(id, property, valueOne);
				updateTransparencies(start+k, id, cyFrameArray, transFull);
			}

			// Fade out
			float step = (transFull-transHalf)/(float)crossFadeDuration;
			float transFactor = transFull;
			for (int k = crossFadeStart; k < crossFadeMid; k++) {
				cyFrameArray[start+k].putValue(id, property, valueOne);
				updateTransparencies(start+k, id, cyFrameArray, transFactor);
				transFactor -= step;
			}

			// Fade in
			transFactor = transHalf;
			for (int k = crossFadeMid; k < crossFadeEnd; k++) {
				cyFrameArray[start+k].putValue(id, property, valueTwo);
				updateTransparencies(start+k, id, cyFrameArray, transFactor);
				transFactor += step;
			}

			for (int k=crossFadeEnd; k < framenum+1; k++) {
				cyFrameArray[start+k].putValue(id, property, valueTwo);
				updateTransparencies(start+k, id, cyFrameArray, transFull);
			}

		}
		return cyFrameArray;
	}

	void updateTransparencies(int index, View<? extends CyIdentifiable> id, CyFrame[] cyFrameArray, float transFactor) {
		for (VisualProperty<?> transProp: transparencyProperties) {
			Object trans = cyFrameArray[index].getValue(id, transProp);
			if (trans instanceof Integer || trans instanceof Float || trans instanceof Double) {
				Number transNumber = (Number) trans;
				float newTrans = transNumber.floatValue()*transFactor;
				if (transProp.getRange().getType().equals(Integer.class))
					cyFrameArray[index].putValue(id, transProp, (int)newTrans);
				else
					cyFrameArray[index].putValue(id, transProp, newTrans);
			} else if (trans instanceof Color || trans instanceof Paint) {
				Color transColor = (Color)trans;
				float alpha = ((float)transColor.getAlpha())*transFactor;
				if (alpha < 0.0f) alpha = 0.0f;
				if (alpha > 255.0f) alpha = 255.0f;
				Color clr = new Color(transColor.getRed(), transColor.getGreen(), transColor.getBlue(), (int) alpha);
				cyFrameArray[index].putValue(id, transProp, clr);
			}
		}
	}
}

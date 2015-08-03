/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

public class VisibleInterpolator implements FrameInterpolator {
	final VisualProperty<?>[] transparencyProperties;

	public VisibleInterpolator(VisualProperty<?>... trans){
		transparencyProperties = trans;
	}

	public int passNumber() { return 2; }

	/*
	 * TODO: implement a fade-in/fade-out?
	 */
	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, 
	                             CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int stop, CyFrame[] cyFrameArray){
		int framenum = (stop-start) - 1;	

		for(View<? extends CyIdentifiable> id: idList){
			Boolean valueOne = (Boolean)frameOne.getValue(id,property);
			Boolean valueTwo = (Boolean)frameTwo.getValue(id,property);
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

			if ((valueOne == valueTwo) && valueOne) {
				// the values are equal and set to True
				if (transOne == transTwo)
					continue; // nothing to do...

				float step = (transTwo-transOne)/(float)framenum;
				float transFactor = transOne;
				for (int k=1; k < framenum+1; k++) {
					updateTransparencies(start+k, id, cyFrameArray, transFactor);
					transFactor += step;

					cyFrameArray[start+k].putValue(id, property, valueOne);
				}
				continue;
			} else if (valueOne == valueTwo) {
				continue; // both are hidden
			}

			if (valueOne) {
				transOne = 1.0f;
				transTwo = 0.0f;
			} else {
				transOne = 0.0f;
				transTwo = 1.0f;
			}
			float step = (transTwo-transOne)/(float)framenum;
			float transFactor = transOne;
			for (int k=1; k < framenum+1; k++) {
				updateTransparencies(start+k, id, cyFrameArray, transFactor);
				transFactor += step;
				cyFrameArray[start+k].putValue(id, property, true);
			}
			cyFrameArray[start+framenum+1].putValue(id, property, valueTwo);
		}
		return cyFrameArray;
	}

	void updateTransparencies(int index, View<? extends CyIdentifiable> id, CyFrame[] cyFrameArray, float transFactor) {
		for (VisualProperty<?> transProp: transparencyProperties) {
			Number trans = (Number)cyFrameArray[index].getValue(id, transProp);
			float newTrans = trans.floatValue()*transFactor;
			if (transProp.getRange().getType().equals(Integer.class))
				cyFrameArray[index].putValue(id, transProp, (int)newTrans);
			else
				cyFrameArray[index].putValue(id, transProp, newTrans);
		}
	}
}

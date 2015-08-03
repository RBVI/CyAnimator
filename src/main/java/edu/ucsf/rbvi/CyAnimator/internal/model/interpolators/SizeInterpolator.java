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

public class SizeInterpolator implements FrameInterpolator {
	boolean growIn = false;

	public SizeInterpolator(boolean grow){
		growIn = grow;
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
			// Size can be either Integer or Double
			Number nSizeOne = (Number) frameOne.getValue(id,property);
			Number nSizeTwo = (Number) frameTwo.getValue(id,property);
			if (nSizeOne == null && nSizeTwo == null)
				continue;

			double sizeOne;
			double sizeTwo;

			// Change this if we want to fade in
			if (nSizeOne == null && nSizeTwo != null) {
				sizeTwo = nSizeTwo.doubleValue();
				if (growIn)
					sizeOne = sizeTwo/100.0;
				else
					sizeOne = sizeTwo;
			} else if (nSizeOne != null && nSizeTwo == null) {
				sizeOne = nSizeOne.doubleValue()/100.0;
				if (growIn)
					sizeTwo = sizeOne/100.0;
				else
					sizeTwo = sizeOne;
			} else {
				sizeOne = nSizeOne.doubleValue();
				sizeTwo = nSizeTwo.doubleValue();
			}

			if (sizeOne == sizeTwo) {
				Number value = new Double(sizeOne);
				if (property.getRange().getType().equals(Integer.class))
					value = new Integer(value.intValue());

				for (int k=1; k< framenum; k++) {
					cyFrameArray[start+k].putValue(id, property, value);
				}
				continue;
			}

			double sizeIncrement = (sizeTwo - sizeOne)/(double)framenum;
			double size = sizeOne;
			for (int k=1; k < framenum; k++) {
				Number value = new Double(size);
				if (property.getRange().getType().equals(Integer.class))
					value = new Integer(value.intValue());
				cyFrameArray[start+k].putValue(id, property, value);
				size += sizeIncrement;
			}
		}
		return cyFrameArray;
	}
}

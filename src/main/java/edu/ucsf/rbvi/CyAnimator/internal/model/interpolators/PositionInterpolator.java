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

public class PositionInterpolator implements FrameInterpolator {

	public PositionInterpolator(){
	}

	public int passNumber() { return 1; }

	/*
	 * Interpolate position
	 */
	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, 
	                             CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int stop, CyFrame[] cyFrameArray){

		int framenum = stop-start;

		for(View<? extends CyIdentifiable> id: idList){
			Double positionOne = (Double) frameOne.getValue(id,property);
			Double positionTwo = (Double) frameTwo.getValue(id,property);
			// System.out.println(id.getModel()+"[1] = "+positionOne+", [2] = "+positionTwo);
			if (positionOne == null && positionTwo == null)
				continue;

			if (positionOne == null && positionTwo != null) {
				positionOne = positionTwo;
			} else if (positionOne != null && positionTwo == null) {
				positionTwo = positionOne;
			}

			if (positionOne == positionTwo) {
				for (int k=1; k < framenum+1; k++) {
					cyFrameArray[start+k].putValue(id, property, positionOne);
				}
				continue;
			}

			double positionIncrement = (positionTwo - positionOne)/(double)framenum;
			double position = positionOne;
			for (int k=1; k < framenum+1; k++) {
				// System.out.println("Position["+k+"] = "+position);
				cyFrameArray[start+k].putValue(id, property, position);
				position += positionIncrement;
			}
		}
		return cyFrameArray;
	}
}

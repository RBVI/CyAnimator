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

public class RotationInterpolator implements FrameInterpolator {

	public RotationInterpolator(){
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
			Double rotationOne = (Double) frameOne.getValue(id,property);
			Double rotationTwo = (Double) frameTwo.getValue(id,property);
			// System.out.println(id.getModel()+"[1] = "+rotationOne+", [2] = "+rotationTwo);
			if (rotationOne == null && rotationTwo == null)
				continue;

			if (rotationOne == null && rotationTwo != null) {
				rotationOne = rotationTwo;
			} else if (rotationOne != null && rotationTwo == null) {
				rotationTwo = rotationOne;
			}

			if (rotationOne == rotationTwo) {
				for (int k=1; k < framenum+1; k++) {
					cyFrameArray[start+k].putValue(id, property, rotationOne);
				}
				continue;
			}

			double rotationIncrement = (rotationTwo - rotationOne)/(double)framenum;
			if (rotationOne != 0.0 || rotationTwo != 0.0) {
				System.out.println("Rotation one = "+rotationOne);
				System.out.println("Rotation two = "+rotationTwo);
				System.out.println("Rotation increment = "+rotationIncrement);
			}
			double rotation = rotationOne;
			for (int k=1; k < framenum+1; k++) {
				if (rotationOne != 0.0 || rotationTwo != 0.0) {
					System.out.println("Rotation["+k+"] = "+rotation);
				}
				cyFrameArray[start+k].putValue(id, property, rotation);
				rotation += rotationIncrement;
			}
		}
		return cyFrameArray;
	}
}

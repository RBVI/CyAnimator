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

public class TransparencyInterpolator implements FrameInterpolator {

	public TransparencyInterpolator(){
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
			// System.out.println("id: "+id.toString());

			Number transOne = (Number) frameOne.getValue(id,property);
			Number transTwo = (Number) frameTwo.getValue(id,property);
			Class<?> clazz = null;
			if (transOne == null && transTwo == null)
				continue;

			if (transOne == null && transTwo != null) {
				transOne = Float.valueOf(0);
				clazz = transTwo.getClass();
			} else if (transOne != null && transTwo == null) {
				transTwo = Float.valueOf(0);
				clazz = transOne.getClass();
			} else {
				clazz = transOne.getClass();
			}

			if (transOne.equals(transTwo)) {
				for (int k=1; k< framenum; k++) {
					cyFrameArray[start+k].putValue(id, property, transOne);
				}
				continue;
			}

			// System.out.println("transOne = "+transOne);
			// System.out.println("transTwo = "+transTwo);

			double sizeIncrement = (transTwo.doubleValue() - transOne.doubleValue())/(double)framenum;
			double size = transOne.doubleValue();
			// System.out.println("increment = "+sizeIncrement);
			// System.out.println("size = "+size);
			for (int k=1; k < framenum; k++) {
				if (clazz.equals(Double.class))
					cyFrameArray[start+k].putValue(id, property, size);
				else if (clazz.equals(Integer.class))
					cyFrameArray[start+k].putValue(id, property, (int)size);
				else if (clazz.equals(Float.class))
					cyFrameArray[start+k].putValue(id, property, (float)size);
				size += sizeIncrement;
			}
		}
		return cyFrameArray;
	}
}

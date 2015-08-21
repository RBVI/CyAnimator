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
public class ObjectPositionInterpolator implements FrameInterpolator {

	public ObjectPositionInterpolator(){
	}

	// This interpolator must be pass 2 so that the Transparency
	// interpolators can run first
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
			if (valueOne == null && valueTwo == null)
				continue;

			if (valueOne == null && valueTwo != null) {
				valueOne = valueTwo;
			} else if (valueOne != null && valueTwo == null) {
				valueTwo = valueOne;
			}

			for (int k=1; k< framenum; k++) {
				if (k < framenum/2)
					cyFrameArray[start+k].putValue(id, property, valueOne);
				else
					cyFrameArray[start+k].putValue(id, property, valueTwo);
			}
		}
		return cyFrameArray;
	}
}

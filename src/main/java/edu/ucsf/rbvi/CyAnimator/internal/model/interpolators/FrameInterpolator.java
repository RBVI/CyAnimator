package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

public interface FrameInterpolator {
	
	public int passNumber();
	
	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int end, CyFrame[] cyFrameArray);
	
}

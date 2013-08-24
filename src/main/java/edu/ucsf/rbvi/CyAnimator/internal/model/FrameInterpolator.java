package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.util.List;


public interface FrameInterpolator {
	
	public CyFrame[] interpolate(List<String> idList, CyFrame frameOne, CyFrame frameTwo, int start, int end, CyFrame[] cyFrameArray);
	
}

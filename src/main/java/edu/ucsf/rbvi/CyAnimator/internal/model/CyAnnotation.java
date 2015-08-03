package edu.ucsf.rbvi.CyAnimator.internal.model;  

import java.util.Map;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.SUIDFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.Annotation;

/**
 * A wrapper class that allows Annotations to be
 * CyIdentifiables
 */
public interface CyAnnotation extends CyIdentifiable {
	public Annotation getAnnotation();

	@Override
	public Long getSUID();
}

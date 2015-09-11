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
class CyAnnotationImpl implements CyAnnotation {
	final Annotation annotation;
	final long suid;


	public CyAnnotationImpl(final Annotation annotation) {
		this.annotation = annotation;
		suid = SUIDFactory.getNextSUID();
	}

	public Annotation getAnnotation() { return annotation; }

	@Override
	public Long getSUID() {
		return suid;
	}

	public String toString() {
		return "CyAnnotation "+suid+": "+annotation.toString();
	}
}

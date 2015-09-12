package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class CaptureTaskFactory extends AbstractTaskFactory {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager = null;
	
	public CaptureTaskFactory(CyServiceRegistrar registrar) {
		this.registrar = registrar;
	}
	
	public TaskIterator createTaskIterator() {
		appManager = registrar.getService(CyApplicationManager.class);
		return new TaskIterator(new CaptureTask(registrar, appManager));
	}

	public boolean isReady() {
		appManager = registrar.getService(CyApplicationManager.class);
		if (appManager.getCurrentNetworkView() == null)
			return false;
		return true;
	}
}

package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

public class ListTaskFactory extends AbstractTaskFactory {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager = null;
	
	public ListTaskFactory(CyServiceRegistrar registrar) {
		this.registrar = registrar;
	}
	
	public TaskIterator createTaskIterator() {
		appManager = registrar.getService(CyApplicationManager.class);
		return new TaskIterator(new ListTask(registrar, appManager));
	}

	public boolean isReady() {
		appManager = registrar.getService(CyApplicationManager.class);
		if (appManager.getCurrentNetworkView() == null)
			return false;
		if (FrameManager.getAllFrameManagers() == null ||
		    FrameManager.getAllFrameManagers().size() == 0)
			return false;
		return true;
	}
}

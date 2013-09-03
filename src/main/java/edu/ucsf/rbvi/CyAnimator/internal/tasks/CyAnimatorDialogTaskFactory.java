package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class CyAnimatorDialogTaskFactory extends AbstractNetworkViewTaskFactory {

	private CyServiceRegistrar bc;
	
	public CyAnimatorDialogTaskFactory(CyServiceRegistrar bundleContext) {
		bc = bundleContext;
	}
	
/*	@Override
	public boolean isReady() {
		if (bc != null) {
			CyApplicationManager appManager = bc.getService(CyApplicationManager.class);
			if (appManager != null && appManager.getCurrentNetworkView() != null) return true;
		}
		return false;
	}
	
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new CyAnimatorDialogTask(bc));
	} */

	public TaskIterator createTaskIterator(CyNetworkView arg0) {
		// TODO Auto-generated method stub
		return new TaskIterator(new CyAnimatorDialogTask(bc));
	}
}

package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class CyAnimatorDialogTaskFactory extends AbstractNetworkViewTaskFactory {

	private CyServiceRegistrar bc;
	private String iconURL;
	
	public CyAnimatorDialogTaskFactory(CyServiceRegistrar bundleContext, String iconURL) {
		bc = bundleContext;
		this.iconURL = iconURL;
	}
	
	public TaskIterator createTaskIterator(CyNetworkView arg0) {
		// TODO Auto-generated method stub
		return new TaskIterator(new CyAnimatorDialogTask(bc, arg0, iconURL));
	}
}

package edu.ucsf.rbvi.CyAnimator.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.osgi.framework.BundleContext;

public class CyAnimatorDialogTaskFactory extends AbstractTaskFactory {

	private BundleContext bc;
	
	public CyAnimatorDialogTaskFactory(BundleContext bundleContext) {
		bc = bundleContext;
	}
	
	@Override
	public boolean isReady() {
		if (bc != null) {
			CyApplicationManager appManager = (CyApplicationManager) getService(CyApplicationManager.class);
			if (appManager != null && appManager.getCurrentNetworkView() != null) return true;
		}
		return false;
	}
	
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new CyAnimatorDialogTask(bc));
	}
	
	private Object getService(Class<?> serviceClass) {
		return bc.getService(bc.getServiceReference(serviceClass.getName()));
	}
}

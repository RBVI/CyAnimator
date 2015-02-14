package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.CyAnimator.internal.ui.CyAnimatorDialog;

public class CyAnimatorDialogTask extends AbstractTask {

	private CyServiceRegistrar bc;
	
	public CyAnimatorDialogTask(CyServiceRegistrar bundleContext) {
		bc = bundleContext;
	}
	
	@Override
	public void run(TaskMonitor arg0) throws Exception {
		CySwingApplication swingApplication = bc.getService(CySwingApplication.class);
		CyAnimatorDialog dialog = new CyAnimatorDialog(bc, swingApplication.getJFrame());
		dialog.actionPerformed(null);
	}

}

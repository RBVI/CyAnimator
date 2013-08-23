package edu.ucsf.rbvi.CyAnimator.tasks;

import java.awt.event.ActionEvent;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;

import edu.ucsf.rbvi.CyAnimator.ui.CyAnimatorDialog;

public class CyAnimatorDialogTask extends AbstractTask {

	private BundleContext bc;
	
	public CyAnimatorDialogTask(BundleContext bundleContext) {
		bc = bundleContext;
	}
	
	@Override
	public void run(TaskMonitor arg0) throws Exception {
		CyAnimatorDialog dialog = new CyAnimatorDialog(bc);
		dialog.actionPerformed(new ActionEvent(null, 0, null));
	}

}

package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.ui.CyAnimatorDialog;

public class CyAnimatorDialogTask extends AbstractTask {

	private CyServiceRegistrar bc;
	private CyNetwork network;
	private static Map<CyRootNetwork, CyAnimatorDialog> networkMap = 
		new HashMap<CyRootNetwork, CyAnimatorDialog>();
	
	public CyAnimatorDialogTask(CyServiceRegistrar bundleContext, CyNetworkView view) {
		bc = bundleContext;
		network = view.getModel();
		FrameManager.registerDialogTask(this);
	}
	
	@Override
	public void run(TaskMonitor arg0) throws Exception {
		CyRootNetwork root = ((CySubNetwork)network).getRootNetwork();
		if (networkMap.containsKey(root)) {
			CyAnimatorDialog dialog = networkMap.get(root);
			dialog.setVisible(true);
			return;
		}

		CySwingApplication swingApplication = bc.getService(CySwingApplication.class);
		CyAnimatorDialog dialog = new CyAnimatorDialog(bc, network, swingApplication.getJFrame());
		networkMap.put(root, dialog);
		dialog.setVisible(true);
	}

	public void resetDialog(CyRootNetwork root) {
		if (networkMap.containsKey(root)) {
			CyAnimatorDialog dialog = networkMap.get(root);
			dialog.clear();
			dialog.setVisible(false);
			// dialog.dispose();
			networkMap.remove(root);
		}
	}

}

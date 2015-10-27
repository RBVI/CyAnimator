package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

public class PlayTask extends AbstractTask {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager;

	public PlayTask(CyServiceRegistrar registrar, CyApplicationManager appManager) {
		this.registrar = registrar;
		this.appManager = appManager;
	}
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CyNetwork network = appManager.getCurrentNetwork();
		FrameManager fm = FrameManager.getFrameManager(registrar, network);
		fm.play(null);
		return;
	}

	@ProvidesTitle
	public String getTitle() { return "Play Animation"; }

}

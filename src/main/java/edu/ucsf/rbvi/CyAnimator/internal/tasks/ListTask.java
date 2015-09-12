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

public class ListTask extends AbstractTask implements ObservableTask {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager;
	private int frameNumber = -1;
	private List<CyFrame> frames = null;

	@Tunable (context="nogui", description="Network to list the frames for")
	public CyNetwork network;

	public ListTask(CyServiceRegistrar registrar, CyApplicationManager appManager) {
		this.registrar = registrar;
		this.appManager = appManager;
	}
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		if (network == null)
			network = appManager.getCurrentNetwork();
		FrameManager fm = FrameManager.getFrameManager(registrar, network);
		frames = fm.getKeyFrameList();
		return;
	}

	@ProvidesTitle
	public String getTitle() { return "List Frame"; }

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			String v = "Frames: \n";
			if (frames == null)
				return (R)v;

			int count = 0;
			for (CyFrame frame: frames) {
				v += "   Frame ["+count+"]: "+frame.toString()+"\n";
				count++;
			}
			return (R)(v);
		} else if (type.equals(List.class)) {
			return (R)frames;
		}
		return null; // JSON Serialization?
	}
}

package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.util.HashMap;
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

public class CaptureTask extends AbstractTask implements ObservableTask {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager;
	private int frameNumber = -1;
	private CyFrame capturedFrame = null;

	@Tunable (context="nogui", description="Network to capture")
	public CyNetwork network;

	@Tunable (context="nogui", description="The number of frames to interpolate before this frame")
	public int interpolate = 30;


	public CaptureTask(CyServiceRegistrar registrar, CyApplicationManager appManager) {
		this.registrar = registrar;
		this.appManager = appManager;
	}
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		if (network == null)
			network = appManager.getCurrentNetwork();
		FrameManager fm = FrameManager.getFrameManager(registrar, network);
		frameNumber = fm.getFrameCount();
		capturedFrame = fm.captureCurrentFrame();
		fm.addKeyFrame(capturedFrame);
		capturedFrame.setInterCount(interpolate);
		return;
	}

	@ProvidesTitle
	public String getTitle() { return "Capture Frame"; }

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			return (R)("Added frame "+frameNumber+": "+capturedFrame.toString());
		} else if (type.equals(CyFrame.class)) {
			return (R)capturedFrame;
		} else if (type.equals(Integer.class))
			return (R)new Integer(frameNumber);
		return null; // JSON Serialization?
	}
}

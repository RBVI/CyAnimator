package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.io.File;

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
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.model.TimeBase;

public class RecordTask extends AbstractTask {

	private CyServiceRegistrar registrar;
	private CyApplicationManager appManager;

	@Tunable (context="nogui", description="Frame rate")
	ListSingleSelection<TimeBase> frameRate = new ListSingleSelection<>(TimeBase.values());

	@Tunable (context="nogui", description="Resolution")
	public int resolution = 100;

	@Tunable (context="nogui", description="Video Type")
	ListSingleSelection<String> videoType = new ListSingleSelection<>("Frames" , "GIF", "MP4/H.264"/*, "MKV/VP8"*/);

	@Tunable (context="nogui", description="Output video location")
	public File outputDir;

	public RecordTask(CyServiceRegistrar registrar, CyApplicationManager appManager) {
		this.registrar = registrar;
		this.appManager = appManager;
		String curDir = System.getProperty("user.dir");
		outputDir = new File(curDir);
	}
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CyNetwork network = appManager.getCurrentNetwork();
		FrameManager fm = FrameManager.getFrameManager(registrar, network);
		String type = videoType.getSelectedValue();
		List<String> types = videoType.getPossibleValues();
		int offset = types.indexOf(type);
		fm.updateSettings(frameRate.getSelectedValue(), offset, resolution);
		fm.recordAnimation(outputDir.getAbsolutePath());
		return;
	}

	@ProvidesTitle
	public String getTitle() { return "Record Animation"; }

}

package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.apache.commons.io.FileUtils;

import com.xuggle.xuggler.ICodec;

public class WriteTask extends AbstractTask {
	/**
	 * 
	 */
	private final FrameManager frameManager;
	TaskMonitor monitor;
	boolean canceled = false;
	String title;
	String directory;
	int videoType;
	int videoResolution;

	public WriteTask(FrameManager frameManager, String title, String directory, int videoType, int videoResolution) {
		super();
		this.frameManager = frameManager;
		this.title = title;
		this.directory = directory;
		this.videoType = videoType;
		this.videoResolution = videoResolution;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		//gets the directory from which cytoscape is running
		String curDir = System.getProperty("user.dir");
		curDir = directory;

		if(videoType != 0){
		    curDir += "/.CyAnimator";
		}

		//assigns the output directory, for now it is by default cytoscape/outputImgs
		File file = new File(curDir); //+"/outputImgs");

		//make the directory
		if(!file.exists()){
			file.mkdir();
		}

		monitor.showMessage(Level.INFO, "Writing frames");
		monitor.setProgress(0.0);

		for(int i=0; i<this.frameManager.frames.length; i++) {
			DecimalFormat frame = new DecimalFormat("#000");
			
			//assign the appropriate path and extension
			//String name = curDir+"/outputImgs/Frame_"+frame.format(i)+".png";
			String name = curDir+"/Frame_"+frame.format(i)+".png";
			if (canceled) return;
		
			try {
				BooleanWrapper finished = new BooleanWrapper(false);
				this.frameManager.frames[i].writeImage(name, videoResolution,finished);
				while (!finished.getValue())
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					};

			} catch (IOException e) {
				monitor.showMessage(Level.ERROR, "Failed to write file "+name);
				return;
			}
			monitor.setProgress(((double)i)/((double)this.frameManager.frames.length));
		}
		
		for (CyFrame frame : this.frameManager.frames) {
		    frame.clearDisplay();
		}

		if(videoType == 1){
				monitor.showMessage(Level.INFO, "Creating animated GIF");
		    GifSequenceWriter wr = new GifSequenceWriter();
		    wr.createGIF(curDir, directory, this.frameManager.fps);
		    FileUtils.deleteDirectory(file);
		}else if ( videoType == 2 ){
				monitor.showMessage(Level.INFO, "Creating MP4 Video");
		    VideoCreator vc = new VideoCreator(ICodec.ID.CODEC_ID_MPEG4, curDir, 
				                                   directory+"/video.mp4", this.frameManager.fps);
		    vc.CreateVideo();
		    FileUtils.deleteDirectory(file);
		}else if ( videoType == 3 ){
				monitor.showMessage(Level.INFO, "Creating H.264 Video");
		    VideoCreator vc = new VideoCreator(ICodec.ID.CODEC_ID_H264, curDir, 
				                                   directory+"/video.mov", this.frameManager.fps);
		    vc.CreateVideo();
		    FileUtils.deleteDirectory(file);
		}
	}
}

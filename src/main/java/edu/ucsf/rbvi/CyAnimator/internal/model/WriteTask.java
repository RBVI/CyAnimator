package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.apache.commons.io.FileUtils;

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

	public WriteTask(FrameManager frameManager, String title, String directory, int videoType) {
		super();
		this.frameManager = frameManager;
		this.title = title;
		this.directory = directory;
                this.videoType = videoType;
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
		monitor.setProgress(0.0);;

		for(int i=0; i<this.frameManager.frames.length; i++) {
			DecimalFormat frame = new DecimalFormat("#000");
			
			//assign the appropriate path and extension
			//String name = curDir+"/outputImgs/Frame_"+frame.format(i)+".png";
			String name = curDir+"/Frame_"+frame.format(i)+".png";
			if (canceled) return;
		
			try {
				BooleanWrapper finished = new BooleanWrapper(false);
				this.frameManager.frames[i].writeImage(name, finished);
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
			monitor.setProgress((i*100)/this.frameManager.frames.length);
		}
                
                for (CyFrame frame : this.frameManager.frames) {
                    frame.clearDisplay();
                }

                if(videoType == 1){
                    GifSequenceWriter wr = new GifSequenceWriter();
                    wr.createGIF(curDir, directory, 50);
                    FileUtils.deleteDirectory(file);
                }else if ( videoType == 2 ){
                    VideoCreator vc = new VideoCreator(curDir, directory, 20);
                    vc.CreateVideo();
                    FileUtils.deleteDirectory(file);
                }
	}
}
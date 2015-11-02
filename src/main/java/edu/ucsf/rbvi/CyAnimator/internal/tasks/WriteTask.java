package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.apache.commons.io.FileUtils;

import static org.jcodec.common.model.ColorSpace.RGB;
import org.jcodec.common.model.Picture;

// import edu.ucsf.rbvi.CyAnimator.internal.io.VideoCreator;
import edu.ucsf.rbvi.CyAnimator.internal.model.BooleanWrapper;
import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

import edu.ucsf.rbvi.CyAnimator.internal.video.AWTUtil;
import edu.ucsf.rbvi.CyAnimator.internal.video.GifSequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.MP4SequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.SequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.WebMSequenceEncoder;

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
		// If we're writing frames, we want
		// to write each frame as a PNG.  
		// Otherwise, we're going 
		// to create the movie directly.
		if (videoType == 0) {
			writeFrames(monitor);
			return;
		}

		// Get the right file/extension
		File movieFile = createFile();
		// TODO: Replace SequenceEncoder with a lower-level implementation
		// so we can change the fps, etc.
		SequenceEncoder enc = null;
		if (videoType == 1)
			enc = new GifSequenceEncoder(movieFile, frameManager.getTimeBase(), true);
		else if (videoType == 2)
			enc = new MP4SequenceEncoder(movieFile, frameManager.getTimeBase());
		else if (videoType == 3)
			enc = new WebMSequenceEncoder(movieFile, frameManager.getTimeBase());

		monitor.showMessage(Level.INFO, "Creating movie");
		monitor.setProgress(0.0);

		int frameCount = frameManager.getFrameCount();
		double scale = videoResolution/100.0;

		for(int i=0; i<frameCount; i++) {
			BufferedImage image = 
					frameManager.getFrame(i).getNetworkImage(scale);
			/*
			if (videoType == 1) {
				((GifSequenceEncoder)enc).encodeImage(image);
			} else {
				encodeImage(enc, image);
			}
			*/
			enc.encodeImage(image);
			monitor.setProgress(((double)i)/((double)frameCount));
		}
		enc.finish();
	}

	private void writeFrames(TaskMonitor monitor) throws Exception {
		//gets the directory from which cytoscape is running
		String curDir = System.getProperty("user.dir");
		if (directory != null) {
			curDir = directory;
		}

		if(videoType != 0){
		    curDir += "/.CyAnimator";
		}

		File file = new File(curDir); //+"/outputImgs");

		//make the directory
		if(!file.exists()){
			file.mkdir();
		}

		monitor.showMessage(Level.INFO, "Writing frames");
		monitor.setProgress(0.0);

		for(int i=0; i<this.frameManager.getFrameCount(); i++) {
			DecimalFormat frame = new DecimalFormat("#000");
			
			//assign the appropriate path and extension
			//String name = curDir+"/outputImgs/Frame_"+frame.format(i)+".png";
			String name = curDir+"/Frame_"+frame.format(i)+".png";
			if (canceled) return;
		
			try {
				BooleanWrapper finished = new BooleanWrapper(false);
				this.frameManager.getFrame(i).writeImage(name, videoResolution,finished);
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
			monitor.setProgress(((double)i)/((double)this.frameManager.getFrameCount()));
		}
		
		for (CyFrame frame : this.frameManager.getFrames()) {
			frame.clearDisplay();
		}

	}

	private File createFile() {
		String separator = System.getProperty("file.separator");
		String extension = null;
		switch(videoType) {
			case 1:
				extension = ".gif";
				break;
			case 2:
				extension = ".mp4";
				break;
			case 3:
				extension = ".webm";
				break;
		}

		if (directory == null || directory.length() == 0) {
			return new File(System.getProperty("user.dir")+separator+"video"+extension);
		}

		File dir = new File(directory);
		if (!dir.exists() || dir.isFile()) return dir;

		return new File(dir, "video"+extension);
	}

}

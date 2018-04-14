package edu.ucsf.rbvi.CyAnimator.internal.tasks;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.apache.commons.io.FileUtils;


import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rational;
import static org.jcodec.common.model.ColorSpace.RGB;
import org.jcodec.common.model.Picture;

// import edu.ucsf.rbvi.CyAnimator.internal.io.VideoCreator;
import edu.ucsf.rbvi.CyAnimator.internal.model.BooleanWrapper;
import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.model.VideoType;

import edu.ucsf.rbvi.CyAnimator.internal.video.AWTUtil;
import edu.ucsf.rbvi.CyAnimator.internal.video.GifSequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.MP4SequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.MovSequenceEncoder;
import edu.ucsf.rbvi.CyAnimator.internal.video.SequenceEncoderWrapper;
import edu.ucsf.rbvi.CyAnimator.internal.video.WebMSequenceEncoder;

public class WriteTask extends AbstractTask {
	/**
	 * 
	 */
	private final FrameManager frameManager;
	TaskMonitor monitor;
	String title;
	String directory;
	VideoType videoType;
	int videoResolution;
	int fps;

	public WriteTask(FrameManager frameManager, String title, String directory, VideoType videoType, int videoResolution, int fps) {
		super();
		this.frameManager = frameManager;
		this.title = title;
		this.directory = directory;
		this.videoType = videoType;
		this.videoResolution = videoResolution;
		this.fps = fps;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Record Animation");

		if (frameManager.getFrameCount() == 0) {
			monitor.showMessage(Level.INFO, "Interpolating Frames");
			frameManager.updateFrames();
		}

		// If we're writing frames, we want
		// to write each frame as a PNG.  
		// Otherwise, we're going 
		// to create the movie directly.
		if (videoType == VideoType.FRAMES) {
			writeFrames(monitor);
			return;
		}

		// Get the right file/extension
		File movieFile = createFile();
		// TODO: Replace SequenceEncoder with a lower-level implementation
		// so we can change the fps, etc.
		SequenceEncoderWrapper enc = null;
		switch (videoType) {
			case GIF:
				enc = new GifSequenceEncoder(movieFile, frameManager.getTimeBase(), true);
				break;
			case MP4:
				// enc = new MP4SequenceEncoder(movieFile, frameManager.getTimeBase(), MP4SequenceEncoder.MP4);
				// enc = new SequenceEncoder(NIOUtils.writableChannel(movieFile), new Rational(fps,1), Format.MOV, Codec.H264, null);
				enc = new MP4SequenceEncoder(movieFile, frameManager.getTimeBase(), fps);
				break;
			case WEBM:
				// Note: this is broken somewhere in JCodec.  Don't use!
				enc = new WebMSequenceEncoder(movieFile, frameManager.getTimeBase(), fps);
				break;
			case MOV:
				// Note: not really sure there's a value in providing MOV separate from MP4
				enc = new MovSequenceEncoder(movieFile, frameManager.getTimeBase(), fps);
				// enc = new SequenceEncoder(NIOUtils.writableChannel(movieFile), new Rational(fps,1), Format.MOV, Codec.H264, null);
				// enc = new MP4SequenceEncoder(movieFile, frameManager.getTimeBase(), MP4SequenceEncoder.MOV);
				break;
		}

		monitor.showMessage(Level.INFO, "Creating "+videoType.toString()+" movie");
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
			if (cancelled)
				break;
			// enc.encodeNativeFrame(AWTUtil.fromBufferedImage(image, ColorSpace.RGB));
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
		
			try {
				BooleanWrapper finished = new BooleanWrapper(false);
				this.frameManager.getFrame(i).writeImage(name, videoResolution,finished);
			} catch (IOException e) {
				monitor.showMessage(Level.ERROR, "Failed to write file "+name);
				return;
			}

			if (cancelled)
				break;
			monitor.setProgress(((double)i)/((double)this.frameManager.getFrameCount()));
		}
		
		for (CyFrame frame : this.frameManager.getFrames()) {
			frame.clearDisplay();
		}

	}

	private File createFile() throws Exception {
		String separator = System.getProperty("file.separator");
		String extension = videoType.getExt();

		if (directory == null || directory.length() == 0) {
			return new File(System.getProperty("user.dir")+separator+"video."+extension);
		}

		File dir = new File(directory);
		if (!dir.exists() || dir.isFile()) return dir;

		return new File(dir, "video."+extension);
	}

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.io;

/**
 *
 * @author vijay13
 */
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.io.*;


import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import java.text.DecimalFormat;

public class VideoCreator {
	private int IMAGE_WIDTH = 880, IMAGE_HEIGHT = 440;
	
	private double FRAME_RATE = 20;
	
	private String inputImgDirPath = "";

	private String outputFilename = "";

	private ICodec.ID type;
	
	public VideoCreator(ICodec.ID videoType, String inputImgDirPath, 
	                    String outputVideoPath, double frameRate){
		this.inputImgDirPath = inputImgDirPath;
		this.outputFilename = outputVideoPath;
		this.FRAME_RATE = frameRate;
		this.type = videoType;
	}

	public void CreateVideo() {
  // let's make a IMediaWriter to write the file.
		final IMediaWriter writer = ToolFactory.makeWriter(outputFilename);

		long startTime = System.nanoTime();
		
		int filesCount = new File(inputImgDirPath).list().length;
		DecimalFormat frame = new DecimalFormat("#000");

// get width and height of image for setting resolution of video - there can be better 
// solution for getting these param

		for (int index = 0; index < filesCount - 1; index++) {

			BufferedImage screen = null;
			try {
				screen = ImageIO.read(new File(inputImgDirPath +"/Frame_"+frame.format(index)+".png"));
				IMAGE_WIDTH = screen.getWidth();
				IMAGE_HEIGHT = screen.getHeight();
				break;
			} catch (IOException e) {
				continue;
			}
		}

  // We tell it we're going to add one video stream, with id 0,
  // at position 0, and that it will have a fixed frame rate of FRAME_RATE.
		IRational rate = IRational.make(FRAME_RATE);
		if (type == ICodec.ID.CODEC_ID_H264) {
			if ((double)IMAGE_WIDTH%2 != 0)
				IMAGE_WIDTH += 1;
			if ((double)IMAGE_HEIGHT%2 != 0)
				IMAGE_HEIGHT += 1;
		}
		writer.addVideoStream(0, 0, type, rate, IMAGE_WIDTH, IMAGE_HEIGHT);
		
		for (int index = 0; index < filesCount - 1; index++) {
// read image
			BufferedImage screen = null;
			try {
				screen = ImageIO.read(new File(inputImgDirPath +"/Frame_"+frame.format(index)+".png"));
			} catch (IOException e) {
				System.out.println("Could not read image");
				continue;
			}

// convert to the right image type
			BufferedImage bgrScreen = convertToType(screen,
				    BufferedImage.TYPE_3BYTE_BGR);

// encode the image to stream #0
			writer.encodeVideo(0, bgrScreen, System.nanoTime() - startTime,
				    TimeUnit.NANOSECONDS);

// sleep for frame rate milliseconds
			try {

				Thread.sleep((long) (1000 / FRAME_RATE));

			} catch (InterruptedException e) {

	// ignore
			}

		}

  // tell the writer to close and write the trailer if  needed
		writer.close();

	}

	public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {

		BufferedImage image;

  // if the source image is already the target type, return the source image
		if (sourceImage.getType() == targetType) {

			image = sourceImage;

		} // otherwise create a new image of the target type and draw the new image
		else {

			image = new BufferedImage(sourceImage.getWidth(),
			                          sourceImage.getHeight(), targetType);

			image.getGraphics().drawImage(sourceImage, 0, 0, null);

		}

		return image;

	}

}

/*
 * File: FrameManager.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
 * 
 * FrameManager is the driving class for the animations which holds the list of key frames, creates and manages the timer, and essentially
 * makes the animation. The primary function is to create a timer ( http://java.sun.com/j2se/1.4.2/docs/api/java/util/Timer.html) which
 * fires an action command at a specified interval displaying each frame in the animation in rapid succession. There is also support
 * for all of the standard, play, stop, pause, step forwards, step backwards commands along with code to set up the exporting of images.
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

public class FrameManager {

	//Holds the set of all key frames in order
	private ArrayList<CyFrame> keyFrameList = null;
	
	//Holds the set of all frames following interpolation of the key frames.
	CyFrame[] frames = null;
	
	//Timer that controls the animations, its public so that a slider etc.. can adjust the delay
	public Timer timer;
	
	//frames per second
	private int fps = 30;
	
	private BundleContext bundleContext;
	private TaskManager<?,?> taskManager;
	//keeps track of the current frame being displayed during animation
	int frameIndex = 0;
	
	
	public FrameManager(BundleContext bc){
		bundleContext = bc;
		taskManager = (TaskManager<?, ?>) getService(TaskManager.class);
		keyFrameList = new ArrayList<CyFrame>();
		
	}
	
	/**
	 * Creates a CyFrame from the current network and view.
	 * 
	 * @return CyFrame of the current CyNetworkView 
	 * @throws IOException throws exception if cannot export image.
	 */
	public CyFrame captureCurrentFrame() throws IOException{
		
	//	CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
		CyFrame frame = new CyFrame(bundleContext);
		
	/*	CyApplicationManager appManager = (CyApplicationManager) getService(CyApplicationManager.class);
		CyNetworkView networkView = appManager.getCurrentNetworkView(); */
		
		//extract view data to make the frame
		frame.populate(); 
		
		//set the interpolation count, or number of frames between this frame and the next to be interpolated
		frame.setInterCount(30);
		
		//frame.setID(networkView.getIdentifier()+"_"+frameid);
		//System.out.println("Frame ID: "+frameid);
		
		//capture an image of the frame
		frame.captureImage();
		
		//frameid++;
		
		return frame;
		
	}
	
	/**
	 * Deletes a key frame from the key frame list.
	 * 
	 * @param index in keyFrameList of frame to be deleted
	 */
	public void deleteKeyFrame(int index){
		List<CyFrame>remove = new ArrayList<CyFrame>();

		remove.add(keyFrameList.get(index));
			
		
		for (CyFrame frame: remove)
			keyFrameList.remove(frame);
			

		updateTimer();
	}
	
	
	/**
	 * Adds the current frame to the keyFrameList and creates a new timer or
	 * updates the existing timer.
	 * @throws IOException throws exception if cannot export image
	 */
	public void addKeyFrame() throws IOException{
		keyFrameList.add(captureCurrentFrame());
		
		if(keyFrameList.size() > 1 && timer != null){ 
			updateTimer();
		}else{	
			makeTimer(); 
		}
	}
	
	
	/**
	 * Creates a timer that fires continually at a delay set in milliseconds.
	 * Each firing of the timer causes the next the frame in the array to be
	 * displayed thus animating the array of CyFrames.  
	 */
	public void makeTimer(){
		
		frameIndex = 0;
		
		//Create a new interpolator
		Interpolator lint = new Interpolator();
		
		//interpolate between all of the key frames in the list to fill the array
		frames = lint.makeFrames(keyFrameList);
		
		 //timer delay is set in milliseconds, so 1000/fps gives delay per frame
        int delay = 1000/fps; 
		
		
		ActionListener taskPerformer = new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if(frameIndex == frames.length){ frameIndex = 0;}
				
				frames[frameIndex].display();
				frameIndex++;
			}
		};

		timer = new Timer(delay, taskPerformer);		
	}

	
	/**
	 * Updates the timer by making a new one while also checking to see whether
	 * or not the animation was already playing.
	 */
	public void updateTimer(){
		if(timer.isRunning()){ 
			timer.stop();
			makeTimer();
			timer.start();
		}else{
			makeTimer();
		}
	}
	
	
	/**
	 * "Records" the animation in the sense that it dumps an image of each frame to 
	 * a specified directory.  It also numbers the frames automatically so that they
	 * can be easily compressed into a standard movie format.
	 * 
	 */
	public void recordAnimation(String directory) throws IOException {
		WriteTask task = new WriteTask(this, "Writing output files", directory);
		taskManager.execute(new TaskIterator(task));
	}
	
	/**
	 * Starts the the timer and plays the animation.
	 */
	public void play(){
		if(timer == null){ return; }
		//1000ms in a second, so divided by frames per second gives ms interval 
		timer.setDelay(1000/fps);
		timer.start();
	}
	
	/**
	 * Stops the timer and the animation.
	 */
	public void stop(){
		if(timer == null){ return; }
		timer.stop();
		makeTimer();
	}
	
	/**
	 * Pauses the timer and the animation.
	 */
	public void pause(){
		if(timer == null){ return; }
		timer.stop();
	}
	
	/**
	 * Steps forward one frame in the animation.
	 */
	public void stepForward(){
		if(timer == null){ return; }
		timer.stop();
		
		//check to see if we have reached the last frame
		if(frameIndex == frames.length-1){ frameIndex = 0; }
		else{ frameIndex++; }
		
		frames[frameIndex].display();
	}
	
	/**
	 * Steps backwards one frame in the animation.
	 */
	public void stepBackward(){
		if(timer == null){ return; }
		timer.stop();
		
		//check to see if we are back to the first frame
		if(frameIndex == 0){ frameIndex = frames.length-1; }
		else{ frameIndex--; }
		
		frames[frameIndex].display();
	}
	
	/**
	 * Returns the key frame list.
	 * 
	 * @return
	 */
	public ArrayList<CyFrame> getKeyFrameList(){
		return keyFrameList;
	}
	
	/**
	 * Sets the key frame list.
	 * 
	 * @param frameList
	 */
	public void setKeyFrameList(ArrayList<CyFrame> frameList){
		keyFrameList = frameList;	
		
		if(frameList.size() > 1 && timer != null){
			updateTimer();
		}else{
			makeTimer();
		}
	}
	
	/**
	 * Returns the current timer.
	 * 
	 * @return
	 */
	public Timer getTimer(){
		return timer;
	}
	
	private Object getService(Class<?> serviceClass) {
		return bundleContext.getService(bundleContext.getServiceReference(serviceClass.getName()));
	}
}

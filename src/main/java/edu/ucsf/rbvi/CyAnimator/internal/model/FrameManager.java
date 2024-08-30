/*
 * File: FrameManager.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
 * 
 * FrameManager is the driving class for the animations which holds the 
 * list of key frames, creates and manages the timer, and essentially
 * makes the animation. The primary function is to create a timer 
 * ( http://java.sun.com/j2se/1.4.2/docs/api/java/util/Timer.html) which
 * fires an action command at a specified interval displaying each frame 
 * in the animation in rapid succession. There is also support
 * for all of the standard, play, stop, pause, step forwards, step 
 * backwards commands along with code to set up the exporting of images.
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Timer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.CySession;
import org.cytoscape.view.model.ContinuousRange;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.Range;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.DoubleVisualProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.CyAnimator.internal.model.AnnotationLexicon;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.ColorInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.CrossfadeInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.FrameInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.CustomGraphicsCrossfadeInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.NoneInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.ObjectPositionInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.PositionInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.RotationInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.SizeInterpolator;
// import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.ShapeInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.TransparencyInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.VisibleInterpolator;
import edu.ucsf.rbvi.CyAnimator.internal.tasks.CyAnimatorDialogTask;
import edu.ucsf.rbvi.CyAnimator.internal.tasks.WriteTask;

public class FrameManager implements NetworkViewAboutToBeDestroyedListener {
	static final Range<Double> ARBITRARY_DOUBLE_RANGE = 
					new ContinuousRange<>(Double.class, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, true);

	private static Map<CyRootNetwork, FrameManager> networkMap = null;

	private static CyAnimatorDialogTask dialogTask = null;

	//Holds the set of all key frames in order
	private ArrayList<CyFrame> keyFrameList = null;

	//Holds all of the properties and interpolators
	private Map<VisualProperty<?>, FrameInterpolator> interpolatorMap;

	//Holds the set of all frames following interpolation of the key frames.
	CyFrame[] frames = null;

	//Timer that controls the animations, its public so that a slider etc.. can adjust the delay
	public Timer timer;

	//frames per second
	private int fps = 30;
	private TimeBase tb = TimeBase.THIRTY;
	private VideoType videoType = VideoType.MP4;
	private int videoResolution = 100;
	private CyServiceRegistrar bundleContext;
	private TaskManager<?,?> taskManager;
	private CyRootNetwork rootNetwork;
	//keeps track of the current frame being displayed during animation
	int frameIndex = 0;

	// Whether or not to loop the animation
	private boolean loop = true;

	private static VisualLexicon dingVisualLexicon;
	private RenderingEngine<?> dingRenderingEngine;
	private RenderingEngineManager renderingEngineManager;
	private Scrubber currentScrubber = null;

	static public FrameManager getFrameManager(CyServiceRegistrar bc, CyNetwork network) {
		// Get the root network
		CyRootNetwork rootNetwork;
		if (CyRootNetwork.class.isAssignableFrom(network.getClass())) 
			rootNetwork = (CyRootNetwork) network;
		else
			rootNetwork = ((CySubNetwork)network).getRootNetwork();

		if (networkMap == null)
			networkMap = new HashMap<CyRootNetwork, FrameManager>();

		// Do we already have a frame manager?
		if (networkMap.containsKey(rootNetwork)) {
			// Yes, return it
			return networkMap.get(rootNetwork);
		}

		// Create a new frame manager for this root network
		FrameManager fm = new FrameManager(bc, rootNetwork);
		bc.registerService(fm, NetworkViewAboutToBeDestroyedListener.class, new Properties());
		networkMap.put(rootNetwork, fm);
		return fm;
	}

	static public Collection<FrameManager> getAllFrameManagers() {
		if (networkMap == null) return null;
		return networkMap.values();
	}

	static public void removeFrameManager(FrameManager manager) {
		for (CyRootNetwork rootNetwork: networkMap.keySet()) {
			if (networkMap.get(rootNetwork).equals(manager)) {
				networkMap.remove(rootNetwork);
				return;
			}
		}
	}

	static public void removeFrameManager(CyNetwork network) {
		CyRootNetwork rootNetwork;
		if (CyRootNetwork.class.isAssignableFrom(network.getClass())) 
			rootNetwork = (CyRootNetwork) network;
		else
			rootNetwork = ((CySubNetwork)network).getRootNetwork();

		if (networkMap.containsKey(rootNetwork))
			networkMap.remove(rootNetwork);
	}

	static public void reset() {
		if (dialogTask != null) {
			for (CyRootNetwork rootNetwork: networkMap.keySet()) {
				dialogTask.resetDialog(rootNetwork);
			}
		}
		if (networkMap != null)
			networkMap.clear();

		dialogTask = null;
	}

	static public void registerDialogTask(CyAnimatorDialogTask dialogTask) {
		FrameManager.dialogTask = dialogTask;
	}

	static public void restoreFramesFromSession(CyServiceRegistrar bc, CySession session, JSONObject frame) {
		// Get the RootNetwork
		Long oldSUID = (Long)frame.get("rootNetwork");
		// System.out.println("Root network suid: "+oldSUID);
		CyNetwork rootNetwork = session.getObject(oldSUID, CyNetwork.class);
		// System.out.println("Found root network: "+rootNetwork);

		// Create a FrameManager for this network
		FrameManager manager = FrameManager.getFrameManager(bc, rootNetwork);

		// Now, get each of the frames
		for (Object frameObject: (JSONArray)frame.get("frames")) {
			JSONObject jsonObject = (JSONObject) frameObject;
			JSONArray networkArray = (JSONArray) jsonObject.get("networks");
			Long networkSuid = (Long)((JSONObject)networkArray.get(0)).get("suid");
			// CyNetwork network = session.getObject(networkSuid, CyNetwork.class);
			CyNetwork network = session.getObject(networkSuid, CyNetwork.class);
			if (network == null)
				continue;

			CyNetworkView networkView = getNetworkView(bc, network);
			if (networkView == null)
				continue;

			CyFrame cyFrame = new CyFrame(bc, manager, networkView);
			cyFrame.loadFrame(session, dingVisualLexicon, jsonObject);
			manager.addKeyFrame(cyFrame);
		}

	}

	static private CyNetworkView getNetworkView(CyServiceRegistrar bc, CyNetwork network) {
		CyNetworkViewManager viewManager = bc.getService(CyNetworkViewManager.class);
		if (viewManager.viewExists(network)) {
			for (CyNetworkView v: viewManager.getNetworkViews(network)) {
				return v;
			}
		}
		return null;
	}

	public int getFrameCount() {
		if (frames == null) return 0;
		return frames.length;
	}

	public CyFrame getFrame(int i) { 
		if (frames == null) return null;
		return frames[i]; 
	}

	public CyFrame[] getFrames() { return frames; }

	public Map<VisualProperty<?>, FrameInterpolator> getInterpolatorMap() { return interpolatorMap; }

	protected FrameManager(CyServiceRegistrar bc, CyRootNetwork rootNetwork){
		bundleContext = bc;
		this.rootNetwork = rootNetwork;
		taskManager = bundleContext.getService(TaskManager.class);
		keyFrameList = new ArrayList<CyFrame>();

		renderingEngineManager = bundleContext.getService(RenderingEngineManager.class);
		// Get the Ding Visual Lexicon
		for (RenderingEngine<?> engine: renderingEngineManager.getAllRenderingEngines()) {
			if (engine.getRendererId().equals("org.cytoscape.ding")) {
				dingRenderingEngine = engine;
				if (dingVisualLexicon == null)
					dingVisualLexicon = engine.getVisualLexicon();
				break;
			}
		}

		// Do we still have a null visual lexicon?  Post a warning
		//

		interpolatorMap = initializeInterpolators();
		makeTimer();
	}

	public <S> S getService(Class<S> serviceClass) {
		return bundleContext.getService(serviceClass);
	}

	/*
	 * If we don't have the dingVisualLexicon, return false.  This
	 * will allow callers to post a warning message that ding-specific
	 * features will be ignored.
	 *
	 * @return true if we're using ding
	 */
	public boolean haveDingFeatures() {
		if (dingVisualLexicon == null) return false;
		return true;
	}

	/**
	 * If we're destroying a network view, we need to destroy
	 * our corresponding CyFrames
	 */
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent ev) {
		CyNetworkView view = ev.getNetworkView();
		CyNetwork net = view.getModel();
		CyRootNetwork rootNet = ((CySubNetwork)net).getRootNetwork();
		// Is this network part of our root?
		if (!rootNet.equals(rootNetwork))
			return;

		// Yes.  Now we need to see if we're using this network view for
		// any of our frames
		List<CyFrame> removeFrame = new ArrayList<>();

		for (CyFrame frame: keyFrameList) {
			if (frame.getNetworkView().equals(view)) {
				// Yes, remove this frame
				removeFrame.add(frame);
			}
		}

		if (removeFrame.size() == keyFrameList.size()) {
			// OK, need to do a full reset
			if (FrameManager.dialogTask != null) {
				FrameManager.dialogTask.resetDialog(rootNet);
				FrameManager.dialogTask = null;
			}
			FrameManager.removeFrameManager(this); // Remove ourselves from the list
			return;
		}

		// Remove the implicated frames
		for (CyFrame frame: removeFrame) {
			keyFrameList.remove(frame);
		}

		resetFrames();
	}

	/**
	 * Creates a CyFrame from the current network and view.
	 * 
	 * @return CyFrame of the current CyNetworkView 
	 * @throws IOException throws exception if cannot export image.
	 */
	public CyFrame captureCurrentFrame() throws IOException{
	//	CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
		CyFrame frame = new CyFrame(bundleContext, this);

		//extract view data to make the frame
		frame.populate(); 

		//set the interpolation count, or number of frames between this frame and the next to be interpolated
		frame.setInterCount(fps);

		//capture an image of the frame
		frame.captureImage(null);

		return frame;

	}

	/**
	 * Deletes a key frame from the key frame list.
	 * 
	 * @param index in keyFrameList of frame to be deleted
	 */
	public void deleteKeyFrame(int index){
		deleteKeyFrame(keyFrameList.get(index));
	}

	/**
	 * Deletes a key frame from the key frame list.
	 * 
	 * @param key frame to remove
	 */
	public void deleteKeyFrame(CyFrame frame){
		keyFrameList.remove(frame);

		// updateTimer();
		resetFrames();
	}


	/**
	 * Adds the current frame to the keyFrameList and creates a new timer or
	 * updates the existing timer.
	 * @throws IOException throws exception if cannot export image
	 */
	public void addKeyFrame() throws IOException{
		addKeyFrame(captureCurrentFrame());
	}

	/**
	 * Adds the current frame to the keyFrameList and creates a new timer or
	 * updates the existing timer.
	 */
	public void addKeyFrame(CyFrame frame) {
		keyFrameList.add(frame);
		frame.setID(""+(keyFrameList.size()-1));

		resetFrames();
	}


	/**
	 * Creates a timer that fires continually at a delay set in milliseconds.
	 * Each firing of the timer causes the next the frame in the array to be
	 * displayed thus animating the array of CyFrames.  
	 */
	public void makeTimer(){
		//timer delay is set in milliseconds, so 1000/fps gives delay per frame
		int delay = 1000/fps; 

		ActionListener taskPerformer = new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if (getFrameCount() == 0) 
					return;

				if(frameIndex == frames.length){ 
					frameIndex = 0;
					if (!loop) {
						timer.stop();
						return;
					}
				}

				frames[frameIndex].display();
				frames[frameIndex].clearDisplay();
				updateScrubber(currentScrubber, frameIndex);
				frameIndex++;
			}
		};

		timer = new Timer(delay, taskPerformer);
	}

	public void makeFrames() {
		if (frames != null) return;

		frameIndex = 0;

		//Create a new interpolator
		InterpolateFrames lint = new InterpolateFrames(this);

		//interpolate between all of the key frames in the list to fill the array
		frames = lint.makeFrames(keyFrameList);
	}


	/**
	 * Updates the timer by making a new one while also checking to see whether
	 * or not the animation was already playing.
	 */
	public void updateFrames(){
		if(timer.isRunning()){ 
			timer.stop();
			makeFrames();
			timer.start();
		}else{
			makeFrames();
		}
	}

	/**
	 * Resets the timer if it is running
	 */
	public void resetFrames() {
		if(timer != null && timer.isRunning())
			timer.stop();
		frames = null;
	}

	/**
	 * "Records" the animation in the sense that it dumps an image of each frame to 
	 * a specified directory.  It also numbers the frames automatically so that they
	 * can be easily compressed into a standard movie format.
	 * 
	 */
	public void recordAnimation(String directory) throws IOException {
		WriteTask task = new WriteTask(this, "Writing output files", directory, videoType, videoResolution, fps);
		taskManager.execute(new TaskIterator(task));
	}

	/**
	 * Starts the the timer and plays the animation.
	 */
	public void play(Scrubber scrubber, boolean loop){
		//1000ms in a second, so divided by frames per second gives ms interval 
		this.loop = loop;
		timer.setDelay(1000/fps);
		currentScrubber = scrubber;
		timer.stop();
		if (frames == null)
			updateFrames();
		timer.start();
	}

	/**
	 * Stops the timer and the animation.
	 */
	public void stop(){
		if(timer == null){ return; }
		timer.stop();
		frameIndex = 0;
		if (currentScrubber != null)
			updateScrubber(currentScrubber, frameIndex);
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
	public void stepForward(Scrubber scrubber){
		if(timer == null || frames == null){ return; }
		timer.stop();

		//check to see if we have reached the last frame
		if(frameIndex == frames.length-1){ frameIndex = 0; }
		else{ frameIndex++; }

		frames[frameIndex].display();
		frames[frameIndex].clearDisplay();
		updateScrubber(scrubber, frameIndex);
	}

	/**
	 * Steps backwards one frame in the animation.
	 */
	public void stepBackward(Scrubber scrubber){
		if(timer == null || frames == null){ return; }
		timer.stop();

		//check to see if we are back to the first frame
		if(frameIndex == 0){ frameIndex = frames.length-1; }
		else{ frameIndex--; }

		frames[frameIndex].display();
		frames[frameIndex].clearDisplay();
		updateScrubber(scrubber, frameIndex);
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
	 * Swaps two frames in the key frame list
	 *
	 * @param frame1 the first frame
	 * @param frame2 the second frame
	 */
	public void swapFrame(CyFrame frame1, CyFrame frame2) {
		int index1 = keyFrameList.indexOf(frame1);
		int index2 = keyFrameList.indexOf(frame2);
		keyFrameList.set(index1, frame2);
		keyFrameList.set(index2, frame1);
	}

	/**
	 * Sets the key frame list.
	 * 
	 * @param frameList
	 */
	public void setKeyFrameList(ArrayList<CyFrame> frameList){
		keyFrameList = frameList;

		resetFrames();
	}

	/**
	 * Returns the current timer.
	 * 
	 * @return
	 */
	public Timer getTimer(){
		return timer;
	}

	/**
	 * update frame and video related settings.
	 * @return
	 */
	public void updateSettings(TimeBase timebase, VideoType videoType, int videoResolution){
		this.tb = timebase;
		// Special handling for NTSC
		if (tb.equals(TimeBase.NTSC))
			fps = 30;
		else
			fps = timebase.getTimeBase();;
		this.videoType = videoType;
		this.videoResolution = videoResolution;
	}

	public TimeBase getTimeBase() { return this.tb; }

	/**
	 * Write out the current frame list
	 *
	 * @param file the File to write the list to
	 */
	public void writeFrames(BufferedWriter output, boolean first) throws IOException {
		if (first)
			output.write(" {\n");
		else
			output.write("\n ,{\n");
		output.write("\t\"rootNetwork\": "+rootNetwork.getSUID()+",\n");
		output.write("\t\"frames\": [\n");
		int frameNumber = 0;
		for (CyFrame frame: keyFrameList) {
			if (frameNumber > 0)
				output.write(",\n");
			frame.writeFrame(output, frameNumber);
			frameNumber++;
		}
		output.write("\t]\n");
		output.write(" }");
	}

	Map<VisualProperty<?>, FrameInterpolator> initializeInterpolators() {
		Map<VisualProperty<?>, FrameInterpolator> iMap = new HashMap<>();
		// Network properties
		iMap.put(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, new ColorInterpolator(false));
		iMap.put(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, new PositionInterpolator());
		iMap.put(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, new PositionInterpolator());
		iMap.put(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, new PositionInterpolator());
		iMap.put(BasicVisualLexicon.NETWORK_DEPTH, new SizeInterpolator(false));
		iMap.put(BasicVisualLexicon.NETWORK_HEIGHT, new SizeInterpolator(false));
		iMap.put(BasicVisualLexicon.NETWORK_SCALE_FACTOR, new SizeInterpolator(false));
		iMap.put(BasicVisualLexicon.NETWORK_SIZE, new SizeInterpolator(false));
		// iMap.put(BasicVisualLexicon.NETWORK_TITLE, new TitleInterpolator(false));
		iMap.put(BasicVisualLexicon.NETWORK_WIDTH, new SizeInterpolator(false));

		// Node properties
		iMap.put(BasicVisualLexicon.NODE_BORDER_LINE_TYPE, 
		         new CrossfadeInterpolator(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.NODE_BORDER_PAINT, new ColorInterpolator(false));
		iMap.put(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, new TransparencyInterpolator());
		iMap.put(BasicVisualLexicon.NODE_BORDER_WIDTH, new SizeInterpolator(false));
		iMap.put(BasicVisualLexicon.NODE_FILL_COLOR, new ColorInterpolator(false));
		iMap.put(BasicVisualLexicon.NODE_HEIGHT, new SizeInterpolator(true));
		iMap.put(BasicVisualLexicon.NODE_LABEL,
		         new CrossfadeInterpolator(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.NODE_LABEL_COLOR, new ColorInterpolator(true));
		iMap.put(BasicVisualLexicon.NODE_LABEL_FONT_FACE,
		         new CrossfadeInterpolator(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, new SizeInterpolator(true));
		iMap.put(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, new TransparencyInterpolator());
		if (haveDingFeatures()) {
			iMap.put(getDingProperty(CyNode.class, "NODE_LABEL_POSITION"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_LABEL_ROTATION"), new RotationInterpolator());
		}
		// iMap.put(BasicVisualLexicon.NODE_LABEL_WIDTH, new LabelWidthInterpolator());
		iMap.put(BasicVisualLexicon.NODE_NESTED_NETWORK_IMAGE_VISIBLE, new NoneInterpolator());
		// iMap.put(BasicVisualLexicon.NODE_PAINT, new PaintInterpolator());
		iMap.put(BasicVisualLexicon.NODE_SIZE, new SizeInterpolator(true));
		iMap.put(BasicVisualLexicon.NODE_SHAPE,
		         new CrossfadeInterpolator(BasicVisualLexicon.NODE_TRANSPARENCY,
		                                   BasicVisualLexicon.NODE_BORDER_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.NODE_TRANSPARENCY, new TransparencyInterpolator());
		iMap.put(BasicVisualLexicon.NODE_VISIBLE, 
		         new VisibleInterpolator(BasicVisualLexicon.NODE_TRANSPARENCY,
										                 BasicVisualLexicon.NODE_BORDER_TRANSPARENCY,
																		 BasicVisualLexicon.NODE_LABEL_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.NODE_WIDTH, new SizeInterpolator(true));
		iMap.put(BasicVisualLexicon.NODE_X_LOCATION, new PositionInterpolator());
		iMap.put(BasicVisualLexicon.NODE_Y_LOCATION, new PositionInterpolator());
		iMap.put(BasicVisualLexicon.NODE_Z_LOCATION, new PositionInterpolator());


		if (haveDingFeatures()) {
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_1"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_2"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_3"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_4"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_5"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_6"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_7"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_8"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_9"), new CustomGraphicsCrossfadeInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_1"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_2"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_3"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_4"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_5"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_6"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_7"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_8"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_SIZE_9"), new SizeInterpolator(true));
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_1"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_2"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_3"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_4"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_5"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_6"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_7"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_8"), new ObjectPositionInterpolator());
			iMap.put(getDingProperty(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_9"), new ObjectPositionInterpolator());
		}

		// Edge properties
		iMap.put(BasicVisualLexicon.EDGE_LABEL,
		         new CrossfadeInterpolator(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.EDGE_LABEL_COLOR, new ColorInterpolator(true));
		iMap.put(BasicVisualLexicon.EDGE_LABEL_FONT_FACE,
		         new CrossfadeInterpolator(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, new SizeInterpolator(true));
		iMap.put(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, new TransparencyInterpolator());
		// iMap.put(BasicVisualLexicon.EDGE_LABEL_WIDTH, new LabelWidthInterpolator());
		iMap.put(BasicVisualLexicon.EDGE_LINE_TYPE,
		         new CrossfadeInterpolator(BasicVisualLexicon.EDGE_TRANSPARENCY));
		// iMap.put(BasicVisualLexicon.EDGE_PAINT, new ColorInterpolator(false));
		// iMap.put(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, new ColorInterpolator(false));
		iMap.put(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new ColorInterpolator(false));
		// iMap.put(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, new ArrowShapeInterpolator());
		iMap.put(BasicVisualLexicon.EDGE_SOURCE_ARROW_SIZE, new SizeInterpolator(false));
		// iMap.put(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, new ArrowShapeInterpolator());
		iMap.put(BasicVisualLexicon.EDGE_TARGET_ARROW_SIZE, new SizeInterpolator(false));
		iMap.put(BasicVisualLexicon.EDGE_TRANSPARENCY, new TransparencyInterpolator());
		iMap.put(BasicVisualLexicon.EDGE_VISIBLE, new VisibleInterpolator(BasicVisualLexicon.EDGE_TRANSPARENCY));
		iMap.put(BasicVisualLexicon.EDGE_WIDTH, new SizeInterpolator(false));
		if (haveDingFeatures()) {
			iMap.put(getDingProperty(CyEdge.class, "EDGE_TARGET_ARROW_UNSELECTED_PAINT"), new ColorInterpolator(false));
			iMap.put(getDingProperty(CyEdge.class, "EDGE_SOURCE_ARROW_UNSELECTED_PAINT"), new ColorInterpolator(false));
			iMap.put(getDingProperty(CyEdge.class, "EDGE_LABEL_ROTATION"), new RotationInterpolator());
		}

		if (haveDingFeatures()) {
			// Annotation properties
			iMap.put(AnnotationLexicon.ANNOTATION_X_LOCATION, new PositionInterpolator());
			iMap.put(AnnotationLexicon.ANNOTATION_Y_LOCATION, new PositionInterpolator());
			// iMap.put(AnnotationLexicon.ANNOTATION_ZOOM, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_CANVAS, new NoneInterpolator());

			// ShapeAnnotation
			iMap.put(AnnotationLexicon.ANNOTATION_WIDTH, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_HEIGHT, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_COLOR, new ColorInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_OPACITY, new TransparencyInterpolator());

			iMap.put(AnnotationLexicon.ANNOTATION_BORDER_WIDTH, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_BORDER_COLOR, new ColorInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_BORDER_OPACITY, new TransparencyInterpolator());
			iMap.put(AnnotationLexicon.ANNOTATION_SHAPE, 
			         new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_OPACITY,AnnotationLexicon.ANNOTATION_BORDER_COLOR));

			/*
			iMap.put(AnnotationLexicon.ANNOTATION_VISIBLE, 
			         new VisibleInterpolator(AnnotationLexicon.ANNOTATION_OPACITY,
											                 AnnotationLexicon.ANNOTATION_BORDER_OPACITY,
																			 AnnotationLexicon.ANNOTATION_IMAGE_OPACITY));
			*/

			// Text visual properites
			iMap.put(AnnotationLexicon.ANNOTATION_TEXT, 
			         new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_FONT_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_FONT_SIZE, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_FONT_COLOR, new ColorInterpolator(true));
			iMap.put(AnnotationLexicon.ANNOTATION_FONT_STYLE, 
								new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_FONT_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_FONT_FAMILY, 
								new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_FONT_COLOR));

			// Image visual properties
			iMap.put(AnnotationLexicon.ANNOTATION_IMAGE_URL, 
								new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_IMAGE_OPACITY));
			iMap.put(AnnotationLexicon.ANNOTATION_IMAGE_CONTRAST, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_IMAGE_BRIGHTNESS, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_IMAGE_OPACITY, new TransparencyInterpolator());

			// Arrow visual properties
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_ANCHOR,
							 new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_TYPE,
							 new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_COLOR, new ColorInterpolator(true));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_SIZE, new SizeInterpolator(false));

			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_WIDTH, new SizeInterpolator(false));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_COLOR, new ColorInterpolator(true));

			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_TARGET_ANCHOR,
							 new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_ARROW_TARGET_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_TARGET_TYPE,
							 new CrossfadeInterpolator(AnnotationLexicon.ANNOTATION_ARROW_TARGET_COLOR));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_TARGET_COLOR, new ColorInterpolator(true));
			iMap.put(AnnotationLexicon.ANNOTATION_ARROW_TARGET_SIZE, new SizeInterpolator(false));
		}

		return iMap;
	}

	public RenderingEngine<?> getRenderingEngine(CyNetworkView view) {
		RenderingEngine<?> reReturn = dingRenderingEngine;

		for (RenderingEngine<?> re: renderingEngineManager.getRenderingEngines(view)) {
			// if it's ding, use that preferentially
			if (re == dingRenderingEngine)
				return dingRenderingEngine;
			reReturn = re;
		}
		return reReturn;
	}

	VisualProperty<?> getDingProperty(Class <?> type, String propertyName) {
		VisualProperty vp = dingVisualLexicon.lookup(type, propertyName);
		if (vp == null) {
			System.out.println("Unable to find property for "+propertyName+" of type "+type);
		}
		return vp;
	}

	private void updateScrubber(Scrubber scrubber, int frame) {
		if (scrubber != null)
			scrubber.frameDisplayed(frame);
	}

}

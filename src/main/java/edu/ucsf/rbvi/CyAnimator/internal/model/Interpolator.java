/*
 * File: AttributeSaverDialog.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
 * 
 * The Interpolator is what make the animations go smoothly. It works by taking a 
 * list of the key frames for the animation from which it determines how many 
 * frames will be in the final animation after interpolation. It then creates 
 * an array of CyFrames which gets "filled" with all of the interpolation data 
 * as it is generated. This works by creating lists of FrameInterpolators which 
 * is a generic interface (FrameInterpolator.java) that has only one method, 
 * interpolate(). There are then many inner classes in Interpolator.java which implement
 * FrameInterpolator and do the interpolation of a single visual property. For 
 * example there is currently interpolateNodeColor, interpolateNodeOpacity 
 * interpolateNodePosition, interpolateEdgeColor, interpolateNetworkColor etc... 
 * Thus the design is such that many interpolators can ultimately be made and swapped 
 * in or out at will. After the set of interpolators are decided, they are iterated 
 * through and all of the NodeView, EdgeView, and NetworkView data is interpolated 
 * appropriately for each frame in the frame array. 
 * 
 * 
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;

import java.awt.Color;
import java.util.*;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;

public class Interpolator {
	
	/*
	 * These lists of FrameInterpolators define the set of visual properties
	 * which will be interpolated for and updated in the CyNetworkView during
	 * the animation.
	 */
	List<FrameInterpolator> nodeInterpolators = new ArrayList<FrameInterpolator>();
	List<FrameInterpolator> edgeInterpolators = new ArrayList<FrameInterpolator>();	
        List<FrameInterpolator> annotationInterpolators = new ArrayList<FrameInterpolator>();
	List<FrameInterpolator> networkInterpolators = new ArrayList<FrameInterpolator>();
	
	public Interpolator(){
		
		//add any desired interpolators to their respective interpolator lists
                nodeInterpolators.add(new interpolateNodeShape());
		nodeInterpolators.add(new interpolateNodePosition());
		nodeInterpolators.add(new interpolateNodeColor());
		nodeInterpolators.add(new interpolateNodeBorderColor());
		nodeInterpolators.add(new interpolateNodeOpacity());
		nodeInterpolators.add(new interpolateNodeSize());
		nodeInterpolators.add(new interpolateNodeBorderWidth());
		nodeInterpolators.add(new interpolateNodeLabel());

		edgeInterpolators.add(new interpolateEdgeColor());
		edgeInterpolators.add(new interpolateEdgeOpacity());
		edgeInterpolators.add(new interpolateEdgeWidth());
		edgeInterpolators.add(new interpolateEdgeLabel());
                edgeInterpolators.add(new interpolateEdgeArrowShape());
                
                annotationInterpolators.add(new interpolateAnnotationsPosition());
                annotationInterpolators.add(new interpolateAnnotationsSize());
                annotationInterpolators.add(new interpolateAnnotationsColor());
                annotationInterpolators.add(new interpolateAnnotationsText());

                networkInterpolators.add(new interpolateNetworkTitle());
		networkInterpolators.add(new interpolateNetworkZoom());
		networkInterpolators.add(new interpolateNetworkColor());
		networkInterpolators.add(new interpolateNetworkCenter());
	}

	/**
	 * This is the driver method which takes a list of key frames and runs the frames
	 * in sets of two through the interpolators to generate the intermediate frames.
	 * 
	 * @param frameList is a list of CyFrames which are key frames in the animation
	 * @return an array of CyFrames which contains each of the key frames with the interpolated frames appropriately interspersed
	 */
	public CyFrame[] makeFrames(List<CyFrame> frameList) {

		if(frameList.isEmpty()){ return null; }
		
		//initialize the framecount to the number of key frames
		int framecount = frameList.size();
		
		//add on the number of frames to be interpolated
		for(int i=0; i<frameList.size()-1; i++){ 
			
			//each frame contains the number of frames which will be interpolated after it which is the interCount
			framecount = framecount + frameList.get(i).getInterCount() - 1;
		}
		
		//create the main CyFrame array which will then be run through all of the interpolators
		CyFrame[] cyFrameArray = new CyFrame[framecount]; //(frameList.size()-1)*framecount + 1];

		//initialize the CyFrame array
		for(int i=0; i<cyFrameArray.length; i++){
			cyFrameArray[i] = new CyFrame(frameList.get(0).getBundleContext());
                        cyFrameArray[i].populate();
		}

		int start = 0;
		int end = 0;

		/*
		 * Runs through the key frame list and adds to the CyFrame array by interpolating between 
		 * two frames at a time in succession. For example it might take key frame one and key frame 
		 * two and then interpolate the node visual properties from frame one to frame two, then the
		 * edge visual properties from frame one to two etc.. It then does the same thing for frame
		 * two and frame three, then frame three and frame four, etc... until the key frames are fully
		 * interpolated.
		 */
		for(int i=0; i < frameList.size()-1; i++) {
			
			//set framecount for this round of interpolation
			framecount = frameList.get(i).getInterCount();
			
			//set ending point for frames to be made
			end = start + framecount;
			
			//set the first frame to the the first key frame
			cyFrameArray[start] = frameList.get(i);
			List<CyNode> nodeList = nodeViewUnionize(frameList.get(i), frameList.get(i+1));
			List<CyEdge> edgeList = edgeViewUnionize(frameList.get(i), frameList.get(i+1));
                                                
			List<Long> nodeIdList = nodeIdUnionize(frameList.get(i), frameList.get(i+1));
			List<Long> edgeIdList = edgeIdUnionize(frameList.get(i), frameList.get(i+1));
                        List<Long> annotationIdList = annotationsIdUnionize(frameList.get(i), frameList.get(i+1));

                        //reset the nodeLists once the unionizer has updated them
			for (int k = start+1; k < end; k++) {
				cyFrameArray[k].setNodeList(nodeList);
				cyFrameArray[k].setEdgeList(edgeList);
			}
                        
                        //reset the nodeLists once the unionizer has updated them
			for (int k = start+1; k < end; k++) {
				cyFrameArray[k].setNodeIdList(nodeIdList);
				cyFrameArray[k].setEdgeIdList(edgeIdList);
			}

			/*
			 * Interpolates all of the node, edge, and network visual properties, this happens by 
			 * iterating through the respective lists of FrameInterpolators which are classes that
			 * implement FrameInterpolator.  This allows for modularization of the interpolation as
			 * you can easily change which FrameInterpolators are in the node, edge, and network 
			 * interpolation lists.
			 */
			for(FrameInterpolator interp: nodeInterpolators){
				cyFrameArray = interp.interpolate(nodeIdList, frameList.get(i), frameList.get(i+1), 
				                                  start, end, cyFrameArray);
			}

			for(FrameInterpolator interp: edgeInterpolators){
				cyFrameArray = interp.interpolate(edgeIdList, frameList.get(i), frameList.get(i+1), 
				                                  start, end, cyFrameArray);
			}

                        for(FrameInterpolator interp: annotationInterpolators){
				cyFrameArray = interp.interpolate(annotationIdList, frameList.get(i), frameList.get(i+1), 
				                                  start, end, cyFrameArray);
			}

			for(FrameInterpolator interp: networkInterpolators){
				cyFrameArray = interp.interpolate(nodeIdList, frameList.get(i), frameList.get(i+1), 
				                                  start, end, cyFrameArray);
			}

			start = end;
		}
	   		
		cyFrameArray[end] = frameList.get(frameList.size()-1);
	   	
		return cyFrameArray;
	}
	
	
	/**
	 * Takes two CyFrames and returns a list of NodeViews which is the union of the list of 
	 * NodeViews that are in each of the two frames.  This is done to accomodate the adding/deleting
	 * of nodes between frames in animation as the union provides a complete set of nodes when
	 * moving across frames.
	 * 
	 * @param frameOne is the first of two frames to be unionized
	 * @param frameTwo is the second of two frames to be unionized
	 * @return the unionized list of NodeViews
	 */
	public List<CyNode> nodeViewUnionize(CyFrame frameOne, CyFrame frameTwo){
		
		List<CyNode> list1 = frameOne.getNodeList();
		List<CyNode> list2 = frameTwo.getNodeList();
		Map<CyNode,CyNode> bigList = new HashMap<CyNode,CyNode>();	
		
		for (CyNode node: list1) {
			bigList.put(node, node);
		}

		for (CyNode node: list2) {
			bigList.put(node, node);
		}

		return new ArrayList<CyNode>(bigList.keySet());
	}
	
	
	/**
	 * Takes two CyFrames and returns the union of the EdgeView lists that are contained
	 * within each frame.  This is to ensure that when edges are added/deleted they will
	 * be able to be interpolated from one frame to the next instead of just instantly
	 * disappearing.
	 * 
	 * @param frameOne is the first frame whose edge list will be unionized
	 * @param frameTwo is the second frame whose edge list will be unionized
	 * @return the unionized list of EdgeViews
	 * 
	 */
	public List<CyEdge> edgeViewUnionize(CyFrame frameOne, CyFrame frameTwo){
		
		List<CyEdge> list1 = frameOne.getEdgeList();
		List<CyEdge> list2 = frameTwo.getEdgeList();
		Map<CyEdge,CyEdge> bigList = new HashMap<CyEdge,CyEdge>();	

		for (CyEdge edge: list1) {
			bigList.put(edge, edge);
		}

		for (CyEdge edge: list2) {
			bigList.put(edge, edge);
		}
		
		return new ArrayList<CyEdge>(bigList.keySet());
		
	}
        
        /**
	 * Takes two CyFrames and returns a list of NodeViews which is the union of the list of 
	 * NodeViews that are in each of the two frames.  This is done to accomodate the adding/deleting
	 * of nodes between frames in animation as the union provides a complete set of nodes when
	 * moving across frames.
	 * 
	 * @param frameOne is the first of two frames to be unionized
	 * @param frameTwo is the second of two frames to be unionized
	 * @return the unionized list of NodeViews
	 */
	public List<Long> nodeIdUnionize(CyFrame frameOne, CyFrame frameTwo){
		
		List<Long> list1 = frameOne.getNodeIdList();
		List<Long> list2 = frameTwo.getNodeIdList();
		Map<Long,Long> bigList = new HashMap<Long,Long>();	
		
		for (Long node: list1) {
			bigList.put(node, node);
		}

		for (Long node: list2) {
			bigList.put(node, node);
		}

		return new ArrayList<Long>(bigList.keySet());
	}
	
	
	/**
	 * Takes two CyFrames and returns the union of the EdgeView lists that are contained
	 * within each frame.  This is to ensure that when edges are added/deleted they will
	 * be able to be interpolated from one frame to the next instead of just instantly
	 * disappearing.
	 * 
	 * @param frameOne is the first frame whose edge list will be unionized
	 * @param frameTwo is the second frame whose edge list will be unionized
	 * @return the unionized list of EdgeViews
	 * 
	 */
	public List<Long> edgeIdUnionize(CyFrame frameOne, CyFrame frameTwo){
		
		List<Long> list1 = frameOne.getEdgeIdList();
		List<Long> list2 = frameTwo.getEdgeIdList();
		Map<Long,Long> bigList = new HashMap<Long,Long>();	

		for (Long edge: list1) {
			bigList.put(edge, edge);
		}

		for (Long edge: list2) {
			bigList.put(edge, edge);
		}
		
		return new ArrayList<Long>(bigList.keySet());
		
	}
        
        /**
	 * Takes two CyFrames and returns the union of the annotations lists that are contained
	 * within each frame.  This is to ensure that when annoations are added/deleted they will
	 * be able to be interpolated from one frame to the next instead of just instantly
	 * disappearing.
	 * 
	 * @param frameOne is the first frame whose annotations list will be unionized
	 * @param frameTwo is the second frame whose annotations list will be unionized
	 * @return the unionized list of annotations
	 * 
	 */
	public List<Long> annotationsIdUnionize(CyFrame frameOne, CyFrame frameTwo){
		
		List<Long> list1 = frameOne.getAnnotationIdList();
		List<Long> list2 = frameTwo.getAnnotationIdList();
		Map<Long,Long> bigList = new HashMap<Long,Long>();	

		for (Long id: list1) {
			bigList.put(id, id);
		}

		for (Long id: list2) {
			bigList.put(id, id);
		}
		
		return new ArrayList<Long>(bigList.keySet());
		
	}
	
	/**
	 * This method performs a generic color interpolation and is used by many of the interpolators
	 * to do their color interpolations.  It simply takes the absolute difference between the R, G, and B
	 * values from the two colors, divides the difference by the number of intervals which will
	 * be interpolated, and then eithers adds or subtracts the appropriate amount for each R, G, and B
	 * value and creates a new color which is placed in the color array.  The color array thus has colorOne
	 * as its first value and colorTwo as its last value with the interpolated colors filling the middle of
	 * the array.
	 * 
	 * @param colorOne is the color to be interpolated from
	 * @param colorTwo is the color to be interpolated to
	 * @param framenum is the number or frames which need to be interpolated for and thus the length of the color array
	 * @return the array of interpolated colors 
	 * 
	 */
	public static Color[] interpolateColor(Color colorOne, Color colorTwo, int framenum, boolean includeAlpha){
		Color[] paints = new Color[framenum+1];

		if (colorOne == null || colorTwo == null) {
			if (colorOne == null)
				colorOne = new Color(colorTwo.getRed(), colorTwo.getGreen(), colorTwo.getBlue(), 0);
			else
				colorTwo = new Color(colorOne.getRed(), colorOne.getGreen(), colorOne.getBlue(), 0);
		}

		float red1 = colorOne.getRed(); 
		float green1 = colorOne.getGreen(); 
		float blue1 = colorOne.getBlue();
		float alpha1 = colorOne.getAlpha();

		float red2 = colorTwo.getRed();
		float green2 = colorTwo.getGreen();
		float blue2 = colorTwo.getBlue();
		float alpha2 = colorTwo.getAlpha();

		//Set up the increment lengths for each RGB values
		float rIncLen = (Math.abs(red1 - red2))/(framenum+1);
		float gIncLen = (Math.abs(green1 - green2))/(framenum+1);
		float bIncLen = (Math.abs(blue1 - blue2))/(framenum+1);
		float aIncLen = (Math.abs(alpha1 - alpha2))/(framenum+1);

		//arrays which will hold the RGB values at each increment, these arrays are parallel to the Color[]
		float[] rArray = new float[framenum+2];
		float[] gArray = new float[framenum+2];
		float[] bArray = new float[framenum+2];
		float[] aArray = new float[framenum+2];

		rArray[0] = 0;
		gArray[0] = 0;
		bArray[0] = 0;
		aArray[0] = 0;

		/*
		 * Initialize the RGB arrays, start of the array contains the value from colorOne, 
		 * end of the arrays contain the value from colorTwo.
		 */
		rArray[1] = red1;// + rIncLen;
		rArray[framenum+1] = red2;
		gArray[1] = green1;// + gIncLen;
		gArray[framenum+1] = green2;
		bArray[1] = blue1 ;//+ bIncLen;
		bArray[framenum+1] = blue2;
		aArray[1] = alpha1 ;//+ aIncLen;
		aArray[framenum+1] = alpha2;

		
		//fill the middle of the RGB arrays
		for(int k=1; k<framenum+1; k++){

			//general strategy is if red1 is less than red2 increment, else decrement
			if(red1 < red2){	
				rArray[k+1] = rArray[k] + rIncLen;
			}else{
				if((rArray[k] - rIncLen) > 0){
					rArray[k+1] = rArray[k] - rIncLen;
				}
			}
			if(green1 < green2){	
				gArray[k+1] = gArray[k] + gIncLen;
			}else{
				if((gArray[k] - gIncLen) > 0){
					gArray[k+1] = gArray[k] - gIncLen;
				}	
			}
			if(blue1 < blue2){	
				bArray[k+1] = bArray[k] + bIncLen;
			}else{
				if((bArray[k] - bIncLen) > 0){
					bArray[k+1] = bArray[k] - bIncLen;
				}	
			}

			if(alpha1 < alpha2){	
				aArray[k+1] = aArray[k] + aIncLen;
			}else{
				if((aArray[k] - aIncLen) > 0){
					aArray[k+1] = aArray[k] - aIncLen;
				}	
			}
			
			//create the new color and put it in the Color[]
			if (includeAlpha)
				paints[k] = new Color((int) rArray[k+1], (int) gArray[k+1], (int) bArray[k+1], (int) aArray[k+1]);	
			else
				paints[k] = new Color((int) rArray[k+1], (int) gArray[k+1], (int) bArray[k+1]);	
		}

		return paints;
	}

}
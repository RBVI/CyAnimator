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
	List<FrameInterpolator> networkInterpolators = new ArrayList<FrameInterpolator>();
	
	public Interpolator(){
		
		//add any desired interpolators to their respective interpolator lists
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
	public Color[] interpolateColor(Color colorOne, Color colorTwo, int framenum, boolean includeAlpha){
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

	/**
	 * Interpolates the node position, using the standard linear interpolation formula described
	 * at http://en.wikipedia.org/wiki/Linear_interpolation. It essentially just finds the absolute
	 * difference between the position of a node in frame one, and in frame two.  It then divides
	 * this distance by the number of frames which will be interpolated and increments or decrements
	 * from the node position in the first frame to the node position in the second.  The incrementing
	 * is done on the x values, which are then plugged into the interpolation formula to generate a y-value.
	 * 
	 */
	
	class interpolateNodePosition implements FrameInterpolator {
		
		public interpolateNodePosition(){
			
		}
		
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is in this case a list of ID's
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){

			int framenum = stop-start;

			for(long nodeid: idList){
				//Get the node positions and set up the position interpolation
				double[] xyOne = frameOne.getNodePosition(nodeid);
				double[] xyTwo = frameTwo.getNodePosition(nodeid);		
				if(xyOne == null && xyTwo == null){ continue; }

				// Handle missing (or appearing) nodes
				if (xyOne == null || xyTwo == null) {
					double[] xy = new double[3];
					if (xyOne == null)
						xy = xyTwo;
					else
						xy = xyOne;

					for(int k=1; k<framenum; k++) {
						cyFrameArray[start+k].setNodePosition(nodeid, xy);
					}
					continue;
				}
				
				double incrementLength = (xyTwo[0] - xyOne[0])/framenum;
				double[] xArray = new double[framenum+1];
				xArray[1] = xyOne[0] + incrementLength;

				for(int k=1; k<framenum; k++){

					double[] xy = new double[3];
					xy[0] = 0;
					xy[1] = 0;
					xy[2] = 0;

					xArray[k+1] = xArray[k] + incrementLength;
					xy[0] = xArray[k];

					//Do the position interpolation
					if((xyTwo[0] - xyOne[0]) == 0){
						xy[1] = xyOne[1];
						xy[2] = xyOne[2];
					}else{

						xy[1] = xyOne[1] + ((xArray[k] - xyOne[0])*((xyTwo[1]-xyOne[1])/(xyTwo[0] - xyOne[0])));
						xy[2] = xyOne[2] + ((xArray[k] - xyOne[0])*((xyTwo[2]-xyOne[2])/(xyTwo[0] - xyOne[0])));
					}

					cyFrameArray[start+k].setNodePosition(nodeid, xy);
				}

			}
			return cyFrameArray;
		}
	}
	
	/**
	 * Fills in the interpolated color values for NodeViews.  Works by using the inner
	 * interpolateColor() method.
	 * 
	 */
	class interpolateNodeColor implements FrameInterpolator {

		public interpolateNodeColor(){
			
		}
		
		
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is in this case a list of NodeViews
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){

			
			int framenum = (stop-start) - 1;
			
			for(long nodeid: idList){
				
				Color colorOne = frameOne.getNodeColor(nodeid);
				Color colorTwo = frameTwo.getNodeColor(nodeid);
				Color colorFillOne = frameOne.getNodeFillColor(nodeid);
				Color colorFillTwo = frameTwo.getNodeFillColor(nodeid);
				if(colorOne != null || colorTwo != null) {
					// Handle missing (or appearing) nodes
					if (colorOne == null) 
						colorOne = colorTwo;
					else if (colorTwo == null)
						colorTwo = colorOne;
				
					if (colorOne == colorTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeColor(nodeid, colorOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeColor(nodeid, paints[k]);
						}	
					}
				}
				
				if (colorFillOne != null || colorFillTwo != null) {
					if (colorFillOne == null)
						colorFillOne = colorFillTwo;
					else if (colorFillTwo == null)
						colorFillTwo = colorFillOne;

					if (colorFillOne == colorFillTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeFillColor(nodeid, colorFillOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorFillOne, colorFillTwo, framenum, false);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeFillColor(nodeid, paints[k]);
						}	
					}
				}
			}	
			return cyFrameArray;
		}
		
	}
	
	
	/**
	 * Interpolates node opacity by linearly incrementing or decrementing the opacity value. 
	 * 
	 */
	class interpolateNodeOpacity implements FrameInterpolator {
		
		public interpolateNodeOpacity(){
			
		}
		
		
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is in this case a list of NodeViews
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = (stop-start) - 1;	
		
			for(long nodeid: idList){
				
				//Get the node transparencies and set up the transparency interpolation
				Integer transOne = frameOne.getNodeOpacity(nodeid);
				Integer transTwo = frameTwo.getNodeOpacity(nodeid);
				Integer transFillOne = frameOne.getNodeFillOpacity(nodeid);
				Integer transFillTwo = frameTwo.getNodeFillOpacity(nodeid);
				
				if (transOne == null) transOne = new Integer(0);
				if (transTwo == null) transTwo = new Integer(0);
				if (transFillOne == null) transFillOne = new Integer(0);
				if (transFillTwo == null) transFillTwo = new Integer(0);

				if (transOne.intValue() == transTwo.intValue()) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].setNodeOpacity(nodeid, transOne);
					}
				} else {
					int transIncLength = (transTwo - transOne)/framenum;
					int[] transArray = new int[framenum+2];
					transArray[1] = transOne + transIncLength;
					
					for(int k=1; k<framenum+1; k++){
						transArray[k+1] = transArray[k] + transIncLength;
						cyFrameArray[start+k].setNodeOpacity(nodeid, transArray[k]);
					}
				}
				
				if (transFillOne.intValue() == transFillTwo.intValue()) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].setNodeFillOpacity(nodeid, transFillOne);
					}
				} else {
					float transIncLength = ((float)(transFillTwo - transFillOne))/((float)framenum);
					float[] transArray = new float[framenum+2];
					transArray[1] = transFillOne + transIncLength;
					
					for(int k=1; k<framenum+1; k++){
						transArray[k+1] = transArray[k] + transIncLength;
						cyFrameArray[start+k].setNodeFillOpacity(nodeid, (int)transArray[k]);
					}
				}
			}
			return cyFrameArray;
		}
	}
	
	/**
	 * 
	 * Linearly interpolates both the height and width of a node simultaneously 
	 * to achieve the affect of interpolating the size.
	 *
	 */
	class interpolateNodeSize implements FrameInterpolator {

		public interpolateNodeSize(){

		}

		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
				int start, int stop, CyFrame[] cyFrameArray){

			int framenum = stop-start;

			for(long nodeid: idList){

				//Get the node sizes and set up the size interpolation
				double[] sizeOne = frameOne.getNodeSize(nodeid);
				double[] sizeTwo = frameTwo.getNodeSize(nodeid);

				if (sizeOne == null && sizeTwo == null) {
					continue;
				}

				if (sizeOne == null && sizeTwo != null) {
					sizeOne = new double[2];
					sizeOne[0] = sizeTwo[0];
					sizeOne[1] = sizeTwo[1];
				}

				if (sizeOne != null && sizeTwo == null) {
					sizeTwo = new double[2];
					sizeTwo[0] = sizeOne[0];
					sizeTwo[1] = sizeOne[1];
				}
				
				
				if (sizeOne[0] == sizeTwo[0] && sizeOne[1] == sizeTwo[1]) {
					for(int k=1; k<framenum; k++){
						cyFrameArray[start+k].setNodeSize(nodeid, sizeOne);
					}
					continue;
				}

				double sizeIncXlength = (sizeTwo[0] - sizeOne[0])/framenum;
				double sizeIncYlength = (sizeTwo[1] - sizeOne[1])/framenum;
				double[] sizeXArray = new double[framenum+1];
				double[] sizeYArray = new double[framenum+1];
				sizeXArray[1] = sizeOne[0] + sizeIncXlength;
				sizeYArray[1] = sizeOne[1] + sizeIncYlength;
					
				for(int k=1; k<framenum; k++){
					sizeXArray[k+1] = sizeXArray[k] + sizeIncXlength;
					sizeYArray[k+1] = sizeYArray[k] + sizeIncYlength;
					double[] temp = {sizeXArray[k], sizeYArray[k]};
					cyFrameArray[start+k].setNodeSize(nodeid, temp);
				}	

			}
			return cyFrameArray;
		}
	}
	
	/**
	 * 
	 * Linearly interpolates the node border width.
	 *
	 */
	class interpolateNodeBorderWidth implements FrameInterpolator {

		public interpolateNodeBorderWidth(){

		}

		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
				int start, int stop, CyFrame[] cyFrameArray){

			int framenum = stop-start;	

			for(long nodeid: idList){

				//get the border widths of the node from each of the two frames
				double widthOne = frameOne.getNodeBorderWidth(nodeid);
				double widthTwo = frameTwo.getNodeBorderWidth(nodeid);
				
				
				//if (widthOne == null) sizeOne = new Integer(1);
				//if (widthTwo == null) sizeTwo = new Integer(1);
				
				
				if (widthOne == widthTwo) {
					for(int k=1; k<framenum; k++){
						cyFrameArray[start+k].setNodeBorderWidth(nodeid, widthOne);
					}
					continue;
				}

				double widthInclength = (widthTwo - widthOne)/framenum;
				double[] widthArray = new double[framenum+1];
				widthArray[1] = widthOne + widthInclength;
					
				for(int k=1; k<framenum; k++){
					widthArray[k+1] = widthArray[k] + widthInclength;
					cyFrameArray[start+k].setNodeBorderWidth(nodeid, widthArray[k]);
				}	

			}
			return cyFrameArray;
		}
	}

	/**
	 * Linearly interpolate node border color
	 * @author Allan Wu
	 *
	 */
	class interpolateNodeBorderColor implements FrameInterpolator {
		
		public interpolateNodeBorderColor() {}
		
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne,
				CyFrame frameTwo, int start, int end, CyFrame[] cyFrameArray) {

			int framenum = (end-start) - 1;	
	
			for(long nodeid: idList)
			{
				Color colorOne = frameOne.getNodeBorderColor(nodeid);
				Color colorTwo = frameTwo.getNodeBorderColor(nodeid);
				if(colorOne != null || colorTwo != null) {
					if (colorOne == colorTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeBorderColor(nodeid, colorOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorOne, colorTwo, framenum, true);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeBorderColor(nodeid, paints[k]);
						}	
					}
				}
				Integer transOne = frameOne.getNodeBorderTrans(nodeid);
				Integer transTwo = frameTwo.getNodeBorderTrans(nodeid);
                                
                                if ( transOne == null) transOne = 0;
                                if ( transTwo == null) transTwo = 0;
                                
				
				if (transOne.equals(transTwo)) {
                                    for (int k = 1; k < framenum + 1; k++) {
                                        cyFrameArray[start + k].setNodeBorderTrans(nodeid, transOne);
                                    }
                                } else {
                                    double sizeInc = ((double) transTwo - (double) transOne) / ((double) framenum), sizeIncrease = sizeInc;

                                    for (int k = 1; k < framenum + 1; k++) {
                                        cyFrameArray[start + k].setNodeBorderTrans(nodeid, transOne + (int) sizeIncrease);
                                        sizeIncrease += sizeInc;
                                    }
                                }
				
			}	
			return cyFrameArray;
		}
	}

	/**
	 * 
	 * Linearly interpolates label size and color
	 *
	 */
	class interpolateNodeLabel implements FrameInterpolator {

		public interpolateNodeLabel() {

		}

		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
				                         int start, int stop, CyFrame[] cyFrameArray){

			int framenum = (stop-start) - 1;	
	
			for(long nodeid: idList)
			{
                                String labelOne = frameOne.getNodeLabel(nodeid);
                                String labelTwo = frameTwo.getNodeLabel(nodeid);

                                Integer transOne = frameOne.getNodeLabelTrans(nodeid);
				Integer transTwo = frameTwo.getNodeLabelTrans(nodeid);

                                if ( transOne == null) transOne = 0;
                                if ( transTwo == null) transTwo = 0;

                                if (labelOne == labelTwo){
                                    for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setNodeLabel(nodeid, labelTwo);
					}
                                    if (transOne.equals(transTwo)) {
                                        for(int k=1; k<framenum+1; k++){
                                                cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne);
                                        }
                                    } else {
                                            double sizeInc = ((double) transTwo - (double) transOne) / ((double) (framenum + 1)), sizeIncrease = sizeInc;

                                            for(int k=1; k<framenum+1; k++){
                                                    cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne + (int) sizeIncrease);
                                                    sizeIncrease += sizeInc;
                                            }
                                    }
                                }else if (labelTwo != null){
                                    double sizeInc = (0 - (double) transOne) / ((double) (framenum / 2)), sizeIncrease = sizeInc;
                                    for(int k=1; k<framenum/2; k++){
                                        cyFrameArray[start+k].setNodeLabel(nodeid, labelOne);
                                        cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne + (int) sizeIncrease);
                                        sizeIncrease += sizeInc;
					}
                                    sizeInc = ((double) transTwo - 0) / ((double) (framenum / 2));
                                    sizeIncrease = sizeInc;
                                    for(int k=framenum/2; k<framenum + 1; k++){
                                        cyFrameArray[start+k].setNodeLabel(nodeid, labelTwo);
                                        cyFrameArray[start+k].setNodeLabelTrans(nodeid, 0 + (int) sizeIncrease);
                                        sizeIncrease += sizeInc;
					}
                                }

				Color colorOne = frameOne.getNodeLabelColor(nodeid);
				Color colorTwo = frameTwo.getNodeLabelColor(nodeid);
				if(colorOne != null || colorTwo != null) {
					if (colorOne == colorTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeLabelColor(nodeid, colorOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorOne, colorTwo, framenum, true);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setNodeLabelColor(nodeid, paints[k]);
						}	
					}
				}
				Integer sizeOne = frameOne.getNodeLabelFontSize(nodeid);
				Integer sizeTwo = frameTwo.getNodeLabelFontSize(nodeid);
                                
                                if ( sizeOne == null) sizeOne = 0;
                                if ( sizeTwo == null) sizeTwo = 0;
                                
				
                                if (sizeOne.equals(sizeTwo)) {
                                        for(int k=1; k<framenum+1; k++){
                                                cyFrameArray[start+k].setNodeLabelFontSize(nodeid, sizeOne);
                                        }	
                                } else {
                                        double sizeInc = ((double) sizeTwo - (double) sizeOne) / ((double) (framenum + 1)), sizeIncrease = sizeInc;

                                        for(int k=1; k<framenum+1; k++){
                                                cyFrameArray[start+k].setNodeLabelFontSize(nodeid, sizeOne + (int) sizeIncrease);
                                                sizeIncrease += sizeInc;
                                        }	
                                }

			}	
			return cyFrameArray;
		}
	}

	/**
	 * Interpolates edgeColor using the interpolateColor() method.
	 */
	class interpolateEdgeColor implements FrameInterpolator {
		
		public interpolateEdgeColor(){
			
		}
	
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is in this case a list of EdgeViews
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = (stop-start) - 1;	

			for(long edgeid: idList){
				
				Color colorOne = frameOne.getEdgeColor(edgeid);
				Color colorTwo = frameTwo.getEdgeColor(edgeid);
				if(colorOne != null || colorTwo != null) {

					// Handle missing (or appearing) nodes
					if (colorOne == null) 
						colorOne = colorTwo;
					else if (colorTwo == null)
						colorTwo = colorOne;
				
					if (colorOne == colorTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeColor(edgeid, colorOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeColor(edgeid, paints[k]);
						}
					}
				}
				
				Color colorStrokeOne = frameOne.getEdgeStrokeColor(edgeid);
				Color colorStrokeTwo = frameTwo.getEdgeStrokeColor(edgeid);
				if(colorStrokeOne != null || colorStrokeTwo != null) {

					// Handle missing (or appearing) nodes
					if (colorStrokeOne == null) 
						colorStrokeOne = colorStrokeTwo;
					else if (colorStrokeTwo == null)
						colorStrokeTwo = colorStrokeOne;
				
					if (colorStrokeOne == colorStrokeTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeStrokeColor(edgeid, colorStrokeOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorStrokeOne, colorStrokeTwo, framenum, false);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeStrokeColor(edgeid, paints[k]);
						}
					}
				}
			}
			return cyFrameArray;
		}
	}
		
	
	/**
	 * Linearly interpolates the edge opacity.
	 */
	class interpolateEdgeOpacity implements FrameInterpolator {
		public interpolateEdgeOpacity(){
			
		}
		
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is in this case a list of EdgeViews
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = (stop-start) - 1;	
		
			for(long edgeid: idList){
				
				//Get the node transparencies and set up the transparency interpolation
				Double transOne;
				Double transTwo;
				
				if (frameOne.getEdgeOpacity(edgeid) == null) transOne = new Double(0);
				else transOne = new Double(frameOne.getEdgeOpacity(edgeid));
				if (frameTwo.getEdgeOpacity(edgeid) == null) transTwo = new Double(0);
				else transTwo = new Double(frameTwo.getEdgeOpacity(edgeid));

				if (transOne.intValue() == transTwo.intValue()) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].setEdgeOpacity(edgeid, transOne.intValue());
					}
				} else {
					double transIncLength = ((double)(transTwo - transOne))/((double)(framenum + 1));
					double[] transArray = new double[framenum+2];
					transArray[1] = transOne + transIncLength;
					
					for(int k=1; k<framenum+1; k++){
						transArray[k+1] = transArray[k] + transIncLength;
						cyFrameArray[start+k].setEdgeOpacity(edgeid, (int)transArray[k]);
					}
				}
				
				//Get the node transparencies and set up the transparency interpolation
				Double transStrokeOne = new Double(frameOne.getEdgeStrokeOpacity(edgeid));
				Double transStrokeTwo = new Double(frameTwo.getEdgeStrokeOpacity(edgeid));

				if (transStrokeOne.intValue() == transStrokeTwo.intValue()) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].setEdgeStrokeOpacity(edgeid, transStrokeOne.intValue());
					}
				} else {
					double transIncLength = ((double)(transStrokeTwo - transStrokeOne))/((double)(framenum + 1));
					double[] transArray = new double[framenum+2];
					transArray[1] = transStrokeOne + transIncLength;
					
					for(int k=1; k<framenum+1; k++){
						transArray[k+1] = transArray[k] + transIncLength;
						cyFrameArray[start+k].setEdgeStrokeOpacity(edgeid, (int)transArray[k]);
					}
				}
				
			}
			return cyFrameArray;
		}
	}
	
	/**
	 * 
	 * Linearly interpolates the edge line width.
	 *
	 */
	class interpolateEdgeWidth implements FrameInterpolator {

		public interpolateEdgeWidth(){

		}
		/**
		 * 
		 * 
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
				int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = (stop-start) - 1;	
		
			for(long edgeid: idList){
				
				//get the edge widths of the edge from each of the two frames
				double widthOne = frameOne.getEdgeWidth(edgeid);
				double widthTwo = frameTwo.getEdgeWidth(edgeid);
				
				if (widthOne == widthTwo) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].setEdgeWidth(edgeid, widthOne);
					}
					continue;
				}

				double widthInclength = (widthTwo - widthOne)/(framenum + 1);
				double[] widthArray = new double[framenum+2];
				widthArray[1] = widthOne + widthInclength;
					
				for(int k=1; k<framenum+1; k++){
					widthArray[k+1] = widthArray[k] + widthInclength;
					cyFrameArray[start+k].setEdgeWidth(edgeid, widthArray[k]);
				}	

			}
			return cyFrameArray;
		}
	}

	/**
	 * 
	 * Linearly interpolates label size and color
	 *
	 */
	class interpolateEdgeLabel implements FrameInterpolator {

		public interpolateEdgeLabel(){

		}

		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
				                         int start, int stop, CyFrame[] cyFrameArray){

			int framenum = (stop-start) - 1;	
	
			for(long edgeid: idList)
			{
                                String labelOne = frameOne.getEdgeLabel(edgeid);
                                String labelTwo = frameTwo.getEdgeLabel(edgeid);

                                Integer transOne = frameOne.getEdgeLabelTrans(edgeid);
				Integer transTwo = frameTwo.getEdgeLabelTrans(edgeid);

                                if ( transOne == null) transOne = 0;
                                if ( transTwo == null) transTwo = 0;

                                if ( labelOne == labelTwo){
                                    for (int k = 1; k < framenum + 1; k++) {
                                            cyFrameArray[start + k].setEdgeLabel(edgeid, labelTwo);
                                        }
                                    if ( transOne.equals(transTwo) ) {
                                        for (int k = 1; k < framenum + 1; k++) {
                                            cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne);
                                        }
                                    } else {
                                        int transInc = (transTwo - transOne) / (framenum + 1), transIncrease = transInc;
                                        for (int k = 1; k < framenum + 1; k++) {
                                            cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne + transIncrease);
                                            transIncrease += transInc;
                                        }
                                    }
                                }else if (labelTwo != null){
                                    int transInc = (0 - transOne) / (framenum / 2), transIncrease = transInc;
                                    for (int k = 1; k < framenum / 2; k++) {
                                            cyFrameArray[start + k].setEdgeLabel(edgeid, labelOne);
                                            cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne + transIncrease);
                                            transIncrease += transInc;
                                        }
                                    transInc = (transTwo - 0) / (framenum / 2);
                                    transIncrease = transInc;
                                    for (int k = framenum/2; k < framenum +1; k++) {
                                            cyFrameArray[start + k].setEdgeLabel(edgeid, labelTwo);
                                            cyFrameArray[start + k].setEdgeLabelTrans(edgeid, 0 + transIncrease);
                                            transIncrease += transInc;
                                        }
                                }

				Color colorOne = frameOne.getEdgeLabelColor(edgeid);
				Color colorTwo = frameTwo.getEdgeLabelColor(edgeid);
				if(colorOne != null || colorTwo != null) {
					if (colorOne == colorTwo) {
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeLabelColor(edgeid, colorOne);
						}	
					} else {
						Color[] paints = interpolateColor(colorOne, colorTwo, framenum, true);
	
						for(int k=1; k<framenum+1; k++){
							cyFrameArray[start+k].setEdgeLabelColor(edgeid, paints[k]);
						}	
					}
				}
				Integer sizeOne = frameOne.getEdgeLabelFontSize(edgeid);
				Integer sizeTwo = frameTwo.getEdgeLabelFontSize(edgeid);
				
                                if ( sizeOne == null ) sizeOne = 0;
                                if ( sizeTwo == null ) sizeTwo = 0;
                                
				if ( sizeOne.equals(sizeTwo) ) {
                                    for (int k = 1; k < framenum + 1; k++) {
                                        cyFrameArray[start + k].setEdgeLabelFontSize(edgeid, sizeOne);
                                    }
                                } else {
                                    double sizeInc = ((double) sizeTwo - (double) sizeOne) / ((double) framenum + 1), sizeIncrease = sizeInc;

                                    for (int k = 1; k < framenum + 1; k++) {
                                        cyFrameArray[start + k].setEdgeLabelFontSize(edgeid, sizeOne + (int) sizeIncrease);
                                        sizeIncrease += sizeInc;
                                    }
                                }

			}	
			return cyFrameArray;
		}
	}

	class interpolateNetworkTitle implements FrameInterpolator {

		public interpolateNetworkTitle(){

		}

		/**
		 * Performs the interpolation.
		 *
		 * @param idList is not used in this case
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo,
		                             int start, int stop, CyFrame[] cyFrameArray){

			int framenum = (stop-start) - 1;

			String titleOne = frameOne.getTitle();
			String titleTwo = frameTwo.getTitle();

			for(int k=1; k<framenum/2; k++){
				cyFrameArray[start+k].setTitle(titleOne);
			}
                        for(int k=framenum/2; k<framenum+1; k++){
				cyFrameArray[start+k].setTitle(titleTwo);
			}
			return cyFrameArray;
		}
	}
	
	/**
	 * Linearly interpolates the network zoom.
	 * 
	 */
	class interpolateNetworkZoom implements FrameInterpolator {
		
		public interpolateNetworkZoom(){
			
		}
	
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is not used in this case 
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = stop-start;
			
			double[] zoomValues = new double[framenum+1];
		//	zoomValues[0] = 0;
			zoomValues[0] = frameOne.getZoom();
			zoomValues[framenum] = frameTwo.getZoom();
			double zoomInc = Math.abs(frameOne.getZoom() - frameTwo.getZoom())/framenum;
			
			for(int k=1; k<framenum; k++){
				
				
				if(frameOne.getZoom() < frameTwo.getZoom()){
					zoomValues[k] = zoomValues[k-1] + zoomInc;
				}else{
					zoomValues[k] = zoomValues[k-1] - zoomInc;
				}
				
				cyFrameArray[start+k].setZoom(zoomValues[k]);
			}
			return cyFrameArray;
		}
	}
	
	
	
	class interpolateNetworkColor implements FrameInterpolator {
		
		public interpolateNetworkColor(){
			
		}
	
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is not used in this case
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
		                             int start, int stop, CyFrame[] cyFrameArray){
	
			int framenum = (stop-start) - 1;
			
			Color colorOne = (Color)frameOne.getBackgroundPaint();
			Color colorTwo = (Color)frameTwo.getBackgroundPaint();
			Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);
			
			for(int k=1; k<framenum+1; k++){
				cyFrameArray[start+k].setBackgroundPaint(paints[k]);
			}
			return cyFrameArray;
		}
	}
	
	class interpolateNetworkCenter implements FrameInterpolator {
	
		public interpolateNetworkCenter(){}
		
		/**
		 * Performs the interpolation.
		 *  
		 * @param idList is not used in this case
		 * @param frameOne is the frame to be interpolated from
		 * @param frameTwo is the frame to be interpolated to
		 * @param start is the starting position of the frame in the CyFrame array
		 * @param end is the ending positiong of the interpolation in the CyFrame array
		 * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
		 * @return the array of CyFrames filled with interpolated node position data
		 */
		public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                int start, int stop, CyFrame[] cyFrameArray){
			
			int framenum = stop-start;
			
			double xone = frameOne.getCenterPoint().getX();
			double yone = frameOne.getCenterPoint().getY();
			double zone = frameOne.getCenterPoint().getZ();
			
			double xtwo = frameTwo.getCenterPoint().getX();
			double ytwo = frameTwo.getCenterPoint().getY();
			double ztwo = frameTwo.getCenterPoint().getZ();
			
			double incrementLength = (xtwo - xone)/framenum;
			double[] xArray = new double[framenum+1];
			xArray[0] = xone;

			for(int k=1; k<framenum; k++){

				Point3D xy = new Point3D(0, 0, 0);
				
				xArray[k] = xArray[k-1] + incrementLength;
				//xy.setLocation(xArray[k], arg1)[0] = xArray[k];

				//Do the position interpolation
				if((xtwo - xone) == 0){
					xy.setLocation(xArray[k], yone, zone);
				}else{

					double y = yone + ((xArray[k] - xone)*((ytwo-yone)/(xtwo -xone)));
					double z = zone + ((xArray[k] - xone)*((ztwo-zone)/(xtwo -xone)));
					xy.setLocation(xArray[k], y, z);
				}

				cyFrameArray[start+k].setCenterPoint(xy);
			}
			
			return cyFrameArray;
		}
		
	}
}	

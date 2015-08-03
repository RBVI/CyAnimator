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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;

import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.FrameInterpolator;

public class InterpolateFrames {
	FrameManager frameManager;

	public InterpolateFrames(FrameManager frameManager){
		this.frameManager = frameManager;
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
		for(int i=1; i<frameList.size(); i++){ 
	
			//each frame contains the number of frames which will be interpolated after it which is the interCount
			framecount = framecount + frameList.get(i).getInterCount() - 1;
		}

		//create the main CyFrame array which will then be run through all of the interpolators
		CyFrame[] cyFrameArray = new CyFrame[framecount];

		//initialize the CyFrame array
		for(int i=0; i<cyFrameArray.length; i++){
			cyFrameArray[i] = new CyFrame(frameList.get(0).getBundleContext(), frameManager);
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
			framecount = frameList.get(i+1).getInterCount();
	
			//set ending point for frames to be made
			end = start + framecount;
	
			//set the first frame to the the first key frame
			cyFrameArray[start] = frameList.get(i);
			Set<CyNode> nodeSet = new HashSet<CyNode>();
			Set<CyEdge> edgeSet = new HashSet<CyEdge>();
			Set<Annotation> annotationList = new HashSet<Annotation>();
			Set<View<? extends CyIdentifiable>> nodeList = nodeViewUnionize(frameList.get(i), frameList.get(i+1), nodeSet);
			Set<View<? extends CyIdentifiable>> edgeList = edgeViewUnionize(frameList.get(i), frameList.get(i+1), edgeSet);
			Set<CyAnnotationView> annotationViewList = annotationViewUnionize(frameList.get(i), frameList.get(i+1), annotationList);

      //reset the nodeLists once the unionizer has updated them
			for (int k = start+1; k < end; k++) {
				cyFrameArray[k].setNodeViewList(nodeList);
				cyFrameArray[k].setNodeList(nodeSet);
				cyFrameArray[k].setEdgeViewList(edgeList);
				cyFrameArray[k].setEdgeList(edgeSet);
				cyFrameArray[k].setAnnotationViewList(annotationViewList);
				cyFrameArray[k].setAnnotationList(new ArrayList<Annotation>(annotationList));
			}

			/*
			 * Interpolates all of the node, edge, and network visual properties, this happens by 
			 * iterating through the respective lists of FrameInterpolators which are classes that
			 * implement FrameInterpolator.  This allows for modularization of the interpolation as
			 * you can easily change which FrameInterpolators are in the node, edge, and network 
			 * interpolation lists.
			 */
			Map<VisualProperty<?>, FrameInterpolator> iMap = frameManager.getInterpolatorMap();
			interpolate(1, iMap, i, nodeList, edgeList, annotationViewList, frameList, start, end, cyFrameArray);
			interpolate(2, iMap, i, nodeList, edgeList, annotationViewList, frameList, start, end, cyFrameArray);

			start = end;
		}
	   
		cyFrameArray[end] = frameList.get(frameList.size()-1);
	   
		return cyFrameArray;
	}

	CyFrame[] interpolate(int pass, Map<VisualProperty<?>, FrameInterpolator> iMap, int i,
	                      Set<View<? extends CyIdentifiable>> nodeList,
												Set<View<? extends CyIdentifiable>> edgeList,
												Set<CyAnnotationView> annotationList,
												List<CyFrame> frameList,
												int start, int end, CyFrame[] cyFrameArray) {

		for (VisualProperty<?> vp: iMap.keySet()) {
			FrameInterpolator fi = iMap.get(vp);
			if (fi.passNumber() != pass)
				continue;

			Set<View<? extends CyIdentifiable>> viewSet = null;
			if (vp.getTargetDataType().equals(CyNode.class))
				viewSet =  nodeList;
			else if (vp.getTargetDataType().equals(CyEdge.class))
				//viewSet = (Set<View<? extends CyIdentifiable>>) edgeList;
				viewSet =  edgeList;
			else if (vp.getTargetDataType().equals(CyNetwork.class)) {
				View<? extends CyIdentifiable> networkView = frameList.get(i).getNetworkView();
				viewSet = new HashSet<View<? extends CyIdentifiable>>();
				viewSet.add(networkView);
			} else if (vp.getTargetDataType().equals(CyAnnotation.class)) {
				viewSet = new HashSet<View<? extends CyIdentifiable>>();

				for (CyAnnotationView view: annotationList) {
					viewSet.add((View<CyAnnotation>)view);
				}
			}

			if (viewSet == null) continue;
			fi.interpolate(viewSet, frameList.get(i), frameList.get(i+1), vp, start, end, cyFrameArray);
		}
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
	public Set<View<? extends CyIdentifiable>> nodeViewUnionize(CyFrame frameOne, CyFrame frameTwo,
	                                                            Set<CyNode>nodeSet){

		Set<View<? extends CyIdentifiable>> list1 = frameOne.getNodeViewList();
		Set<View<? extends CyIdentifiable>> list2 = frameTwo.getNodeViewList();
		Set<View<? extends CyIdentifiable>> bigList = new HashSet<>();

		for (View<? extends CyIdentifiable> node: list1) {
			bigList.add(node);
			nodeSet.add((CyNode)node.getModel());
		}

		for (View<? extends CyIdentifiable> node: list2) {
			bigList.add(node);
			nodeSet.add((CyNode)node.getModel());
		}

		return bigList;
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
	public Set<View<? extends CyIdentifiable>> edgeViewUnionize(CyFrame frameOne, CyFrame frameTwo,
	                                                            Set<CyEdge>edgeSet){

		Set<View<? extends CyIdentifiable>> list1 = frameOne.getEdgeViewList();
		Set<View<? extends CyIdentifiable>> list2 = frameTwo.getEdgeViewList();
		Set<View<? extends CyIdentifiable>> bigList = new HashSet<>();

		for (View<? extends CyIdentifiable> edge: list1) {
			bigList.add(edge);
			edgeSet.add((CyEdge)edge.getModel());
		}

		for (View<? extends CyIdentifiable> edge: list2) {
			bigList.add(edge);
			edgeSet.add((CyEdge)edge.getModel());
		}

		return bigList;

	}

	/**
	 * Takes two CyFrames and returns the union of the AnnotationView lists that are contained
	 * within each frame.  This is to ensure that when edges are added/deleted they will
	 * be able to be interpolated from one frame to the next instead of just instantly
	 * disappearing.
	 * 
	 * @param frameOne is the first frame whose edge list will be unionized
	 * @param frameTwo is the second frame whose edge list will be unionized
	 * @return the unionized list of EdgeViews
	 * 
	 */
	public Set<CyAnnotationView> annotationViewUnionize(CyFrame frameOne, CyFrame frameTwo,
	                                                    Set<Annotation>annotationSet){
		Set<CyAnnotationView> list1 = frameOne.getAnnotationViewList();
		Set<CyAnnotationView> list2 = frameTwo.getAnnotationViewList();
		Set<CyAnnotationView> bigList = new HashSet<>();

		for (CyAnnotationView annotationView: list1) {
			bigList.add(annotationView);
			annotationSet.add(annotationView.getModel().getAnnotation());
		}

		for (CyAnnotationView annotationView: list2) {
			bigList.add(annotationView);
			annotationSet.add(annotationView.getModel().getAnnotation());
		}

		return bigList;
	}

}

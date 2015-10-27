/*
 * File: CyFrame.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
 *
 * The CyFrame class is essentially a wrapper on a CyNetworkView. It works by having a
 * populate() method which essentially extracts the necessary view data from the current
 * CyNetworkView and stores it in the CyFrame. Each CyFrame also contains a display()
 * method which updates the current network view based upon the visual data stored in
 * that particular CyFrame. It also can hold an image of the network and contains a facility
 * for writing this image to a file.
 *
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;  

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.*;
import java.awt.Paint;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySession;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.BoundedTextAnnotation;
import org.cytoscape.view.presentation.annotations.ImageAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.CyAnimator.internal.model.interpolators.FrameInterpolator;  

public class CyFrame {

	private String frameid = "";
	private static final String PNG = "png";

	// New approach
	private Map<CyNode, Map<VisualProperty<?>, Object>> nodePropertyMap;
	private Map<CyEdge, Map<VisualProperty<?>, Object>> edgePropertyMap;
	private Map<CyNetwork, Map<VisualProperty<?>, Object>> networkPropertyMap;
	private Map<Annotation, Map<VisualProperty<?>, Object>> annotationPropertyMap;

	private Map<VisualProperty<?>, FrameInterpolator> interpolatorMap;

	private HashMap<CyNode, CyNode> recordNode;
	private HashMap<CyEdge, CyEdge> recordEdge;
	private HashMap<Annotation, Annotation> recordAnnotation;

	private CyServiceRegistrar bundleContext;
	private CyApplicationManager appManager;
	private CyEventHelper eventHelper;
	private CyNetworkView networkView = null;
	private CyNetwork currentNetwork = null;
	private BufferedImage networkImage = null;
	private Set<View<? extends CyIdentifiable>> nodeViewList = null;
	private Set<View<? extends CyIdentifiable>> edgeViewList = null;
	private Set<CyAnnotationView> annotationViewList = null;
	private Set<CyNode> nodeList = null;
	private Set<CyEdge> edgeList = null;
	private Set<Annotation> annotationList = null;
	private VisualStyle vizStyle = null;
	private int intercount = 0;
	private SynchronousTaskManager<?> taskManager;
	private FrameManager frameManager;
	private AnnotationManager annotationManager;
	private AnnotationLexicon annotationLexicon;
	private boolean keyFrame = false;

	private static final int IMAGE_WIDTH = 100, IMAGE_HEIGHT = 100;

	/**
	 * Creates this CyFrame by initializing and populating all of the fields.
	 *
	 * @param currentNetwork
	 */
	public CyFrame(CyServiceRegistrar bc, FrameManager frameManager){
		this(bc, frameManager, null);
	}

	public CyFrame(CyServiceRegistrar bc, FrameManager frameManager, CyNetworkView view) {
		bundleContext = bc;
		this.frameManager = frameManager;
		networkView = view;

		appManager = bundleContext.getService(CyApplicationManager.class);
		taskManager = bundleContext.getService(SynchronousTaskManager.class);
		annotationManager = bundleContext.getService(AnnotationManager.class);
		eventHelper = bundleContext.getService(CyEventHelper.class);
		annotationLexicon = new AnnotationLexicon();

		if (networkView == null)
			networkView = appManager.getCurrentNetworkView();

		recordNode = new HashMap<CyNode, CyNode>();
		recordEdge = new HashMap<CyEdge, CyEdge>();
		recordAnnotation = new HashMap<Annotation, Annotation>();
		nodePropertyMap = new HashMap<>();
		edgePropertyMap = new HashMap<>();
		networkPropertyMap = new HashMap<>();
		annotationPropertyMap = new HashMap<>();

		edgeViewList = new HashSet<View<? extends CyIdentifiable>>(networkView.getEdgeViews());
		nodeViewList = new HashSet<View<? extends CyIdentifiable>>(networkView.getNodeViews());
		nodeList = new HashSet<CyNode>(networkView.getModel().getNodeList());
		edgeList = new HashSet<CyEdge>(networkView.getModel().getEdgeList());

		currentNetwork = networkView.getModel();

		// Now get all of our annotations
		if (annotationManager.getAnnotations(networkView) == null) {
			annotationList = new HashSet<>();
		} else {
			annotationList = new HashSet<>(annotationManager.getAnnotations(networkView));
		}
		annotationViewList = new HashSet<CyAnnotationView>(CyAnnotationView.wrapViews(networkView, annotationList));

		// Remember the visual style
		VisualMappingManager visualManager = bundleContext.getService(VisualMappingManager.class);
		vizStyle = visualManager.getCurrentVisualStyle();

		// Initialize our properties and interpolators
		interpolatorMap = frameManager.getInterpolatorMap();
	}

	/*
	 * Captures all of the current visual settings for nodes and edges from a
	 * CyNetworkView and stores them in this frame.
	 */
	public void populate() {
		networkPropertyMap.put(currentNetwork, new HashMap<VisualProperty<?>, Object>());
		for (VisualProperty<?> property: interpolatorMap.keySet()) {
			if (property.getTargetDataType().isAssignableFrom(CyNetwork.class))
				networkPropertyMap.get(currentNetwork).put(property, networkView.getVisualProperty(property));
		}

		for (CyNode node: nodeList) {
			nodePropertyMap.put(node, new HashMap<VisualProperty<?>, Object>());
			View<CyNode> view = networkView.getNodeView(node);
			if (view != null) {
				for (VisualProperty<?> property: interpolatorMap.keySet()) {
					if (property.getTargetDataType().isAssignableFrom(CyNode.class))
						nodePropertyMap.get(node).put(property, view.getVisualProperty(property));
				}
			}
		}
		for (CyEdge edge: edgeList) {
			edgePropertyMap.put(edge, new HashMap<VisualProperty<?>, Object>());
			View<CyEdge> view = networkView.getEdgeView(edge);
			if (view != null) {
				for (VisualProperty<?> property: interpolatorMap.keySet()) {
					if (property.getTargetDataType().isAssignableFrom(CyEdge.class))
						edgePropertyMap.get(edge).put(property, view.getVisualProperty(property));
				}
			}
		}

		for (Annotation annotation: annotationList) {
			annotationPropertyMap.put(annotation, new HashMap<VisualProperty<?>, Object>());
			CyAnnotationView view = CyAnnotationView.getAnnotationView(annotation, annotationViewList);
			if (view != null) {
				// System.out.println("Adding annotation: "+printArgMap(annotation.getArgMap()));
				for (VisualProperty<?> property: interpolatorMap.keySet()) {
					if (property.getTargetDataType().isAssignableFrom(CyAnnotation.class)) {
						annotationPropertyMap.get(annotation).put(property, view.getVisualProperty(property));
					}
				}
			}
		}
		keyFrame = true;
	}

	/**
	 * Inits all of our property maps without actually populating.  This is used by the
	 * frame interpolator to create all of the empty frames that we'll eventually interpolate
	 */
	public void initMaps() {
		networkPropertyMap.put(currentNetwork, new HashMap<VisualProperty<?>, Object>());

		for (CyNode node: nodeList) {
			nodePropertyMap.put(node, new HashMap<VisualProperty<?>, Object>());
		}

		for (CyEdge edge: edgeList) {
			edgePropertyMap.put(edge, new HashMap<VisualProperty<?>, Object>());
		}

		for (Annotation annotation: annotationList) {
			annotationPropertyMap.put(annotation, new HashMap<VisualProperty<?>, Object>());
		}
	}

	private String printArgMap(Map<String,String> argMap) {
		boolean first = true;
		String result = "";
		for (String key: argMap.keySet()) {
			if (!first)
				result += "|"+key+"="+argMap.get(key);
			else {
				result = key+"="+argMap.get(key);
				first = false;
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(View<? extends CyIdentifiable> view, VisualProperty<T> property) {
		if (view == null)
			return null;

		if (view.getModel() instanceof CyNode)
			return getNodeValue((CyNode)view.getModel(), property);
		else if (view.getModel() instanceof CyEdge)
			return getEdgeValue((CyEdge)view.getModel(), property);
		else if (view.getModel() instanceof CyNetwork)
			return getNetworkValue((CyNetwork)view.getModel(), property);
		else if (view.getModel() instanceof CyAnnotation)
			return getAnnotationValue((CyAnnotation)view.getModel(), property);
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	public <T> T getNodeValue(CyNode node, VisualProperty<T> property) {
		if (!nodePropertyMap.containsKey(node))
			return null;

		if (!nodePropertyMap.get(node).containsKey(property))
			return null;

		return (T)nodePropertyMap.get(node).get(property);
	}

	@SuppressWarnings("unchecked")
	public <T> T getEdgeValue(CyEdge edge, VisualProperty<T> property) {
		if (!edgePropertyMap.containsKey(edge))
			return null;

		if (!edgePropertyMap.get(edge).containsKey(property))
			return null;

		return (T)edgePropertyMap.get(edge).get(property);
	}

	@SuppressWarnings("unchecked")
	public <T> T getNetworkValue(CyNetwork network, VisualProperty<T> property) {
		if (!networkPropertyMap.containsKey(network))
			return null;

		if (!networkPropertyMap.get(network).containsKey(property))
			return null;

		return (T)networkPropertyMap.get(network).get(property);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAnnotationValue(CyAnnotation annotation, VisualProperty<T> property) {
		Annotation ann = annotation.getAnnotation();
		if (!annotationPropertyMap.containsKey(ann))
			return null;

		if (!annotationPropertyMap.get(ann).containsKey(property))
			return null;

		// System.out.println(property+" = "+(T)annotationPropertyMap.get(ann).get(property));

		return (T)annotationPropertyMap.get(ann).get(property);
	}

	public void putValue(View<? extends CyIdentifiable> view, VisualProperty<?> property, Object value) {
		if (view == null)
			return;

		if (view.getModel() instanceof CyNode) {
			CyNode node = (CyNode) view.getModel();
			if (!nodePropertyMap.containsKey(node))
				return;
			nodePropertyMap.get(node).put(property, value);
		} else if (view.getModel() instanceof CyEdge) {
			CyEdge edge = (CyEdge) view.getModel();
			if (!edgePropertyMap.containsKey(edge))
				return;
			edgePropertyMap.get(edge).put(property, value);
		} else if (view.getModel() instanceof CyNetwork) {
			CyNetwork network = (CyNetwork) view.getModel();
			if (!networkPropertyMap.containsKey(network))
				return;
			networkPropertyMap.get(network).put(property, value);
		} else if (view.getModel() instanceof CyAnnotation) {
			Annotation annotation = ((CyAnnotation) view.getModel()).getAnnotation();
			if (!annotationPropertyMap.containsKey(annotation))
				return;
			annotationPropertyMap.get(annotation).put(property, value);
		} else
			return;
	}

	/**
	 * Captures and stores a thumbnail image from the current CyNetworkView for
	 * this frame.
	 * @throws IOException throws exception if cannot create or read temporary file
	 */
	public void captureImage(CyNetworkView view) throws IOException {

	/*	double scale = .35;
		double wscale = .25; */

		if (view == null)
			view = appManager.getCurrentNetworkView();

		double width = view.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		double height = view.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);
		double scale = (double)IMAGE_WIDTH/width;
		if (width < height)
			scale = (double)IMAGE_HEIGHT/height;
		networkImage = getNetworkImage(view, scale);
	}

	/*
	 * Cycles through the list of nodes and edges and updates the node and edge views
	 * based upon the visual data stored as part of the CyFrame. 
	 */
	public void display() {
		VisualMappingManager visualManager = (VisualMappingManager) bundleContext.getService(VisualMappingManager.class);
		visualManager.setVisualStyle(vizStyle, networkView);

		// We want to use the current view in case we're interpolating
		// across views
		final CyNetworkView currentView = appManager.getCurrentNetworkView();

		// Make sure everything is on the EDT
		DisplayFrame displayFrame = new DisplayFrame(currentView, this);
		if (SwingUtilities.isEventDispatchThread()) {
			displayFrame.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( displayFrame );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

  /**
	 * Removes temporarily added nodes and edges from network.
	 *
	 *
	 */
	public void clearDisplay(){
		final Collection<CyEdge> removeAddedEdges = new ArrayList<CyEdge>();
		final Collection<Long> removeAddedEdgesKeys = new ArrayList<Long>();
		for (CyEdge e: recordEdge.values() ){
			removeAddedEdges.add(e);
			removeAddedEdgesKeys.add(e.getSUID());
		}
	
		final Collection<CyNode> removeAddedNodes = new ArrayList<CyNode>();
		final Collection<Long> removeAddedKeys = new ArrayList<Long>();
		for (CyNode n: recordNode.values() ){
			removeAddedNodes.add(n);
			removeAddedKeys.add(n.getSUID());
		}
	
		ClearDisplay clearDisplay = new ClearDisplay(appManager,
		                                             removeAddedEdges, removeAddedNodes,
		                                             removeAddedEdgesKeys, removeAddedKeys);
		if (SwingUtilities.isEventDispatchThread()) {
			clearDisplay.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( clearDisplay );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String toString() {
		return "CyFrame: '"+frameid+"' with "+
		        nodePropertyMap.size()+" nodes, "+
		        edgePropertyMap.size()+" edges, and "+
		        annotationPropertyMap.size()+" annotations.  "+
						"nodeViewList has "+nodeViewList.size()+" entries, and nodeList has "+
						nodeList.size()+" entries";
	}

	private void handleMissingNodes(final CyNetworkView currentView) {
		if (!keyFrame)
			return;

		// Initialize our edge view maps
		List<View<CyNode>> removeNodes = new ArrayList<View<CyNode>>();
		for (View<CyNode> nv : currentView.getNodeViews()) {
			if (!nodeViewList.contains(nv)) {
				removeNodes.add(nv);
			}
		}

		for (View<CyNode> nodeView : removeNodes) {
			if (nodeView == null) {
				continue;
			}
			if (nodeView.isValueLocked(BasicVisualLexicon.NODE_VISIBLE)) {
				nodeView.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
			}

			// System.out.println("Setting node "+nodeView.getModel()+" to invisible");
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_SIZE, 0.5);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_WIDTH, 0.5);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_HEIGHT, 0.5);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 0);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, 0);
			currentView.updateView();
		}
	}

	private void handleMissingEdges(final CyNetworkView currentView) {
		// First see if we have any views we need to remove
		List<View<CyEdge>> removeEdges = new ArrayList<View<CyEdge>>();
		for (View<CyEdge> ev: currentView.getEdgeViews()) {
			if (!edgeViewList.contains(ev)) {
				removeEdges.add(ev);
			}
		}

		for (View<CyEdge> edgeView : removeEdges) {
			if (edgeView == null) {
				continue;
			}
			if (edgeView.isValueLocked(BasicVisualLexicon.EDGE_VISIBLE)) {
				edgeView.clearValueLock(BasicVisualLexicon.EDGE_VISIBLE);
			}
			edgeView.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false);
			// edgeView.setVisualProperty(BasicVisualLexicon.EDGE_WIDTH, 0.0);
			currentView.updateView();
		}
	}
	
	private void handleMissingAnnotations(final CyNetworkView currentView) {
		List<Annotation> removeAnnotations = new ArrayList<>();
		List<Annotation> annotations = annotationManager.getAnnotations(currentView);
		if (annotations != null) {
			for (Annotation annotation : annotations) {
				if (!annotationList.contains(annotation)) {
					removeAnnotations.add(annotation);
				}
			}
		}

		for (Annotation ann: removeAnnotations) {
			CyAnnotationView annView = CyAnnotationView.getAnnotationView(currentView, ann);
			annView.setVisualProperty(AnnotationLexicon.ANNOTATION_VISIBLE, false);
			ann.update();
		}
	}

	private void handleNodes(final CyNetworkView currentView) {
		for (CyNode node: nodePropertyMap.keySet()) {
			Map<VisualProperty<?>, Object> propertyValues = nodePropertyMap.get(node);
			View<CyNode> view = currentView.getNodeView(node);
			if (view == null) {
				// Add temporary node to network for viewing the node which is removed from current network
				if (!recordNode.containsKey(node)) {
					((CySubNetwork)currentView.getModel()).addNode(node);
					eventHelper.flushPayloadEvents();
					// recordNode.put(node, node);
					view = currentView.getNodeView(node);
					// nodeViewList.add(view);
					eventHelper.flushPayloadEvents();
				} else {
					view = currentView.getNodeView(recordNode.get(node));
				}
			} else if (!nodeViewList.contains(view)) {
				// Make sure we know about this view
				if (!view.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)) {
					view.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, true);
				}
			}

			for (VisualProperty<?> vp: propertyValues.keySet()) {
				if (view.isValueLocked(vp))
					view.clearValueLock(vp);
				view.setVisualProperty(vp, propertyValues.get(vp));
				/*
				if (vp.getIdString().equals("NODE_CUSTOMGRAPHICS_1")) {
					System.out.println("Node "+node+" custom graphics = "+propertyValues.get(vp));
				}
				*/
			}
		}
		currentView.updateView();
	}

	private void handleEdges(final CyNetworkView currentView) {
		for (CyEdge edge: edgePropertyMap.keySet()) {
			Map<VisualProperty<?>, Object> propertyValues = edgePropertyMap.get(edge);
			View<CyEdge> view = currentView.getEdgeView(edge);
			if (view == null) {
				// Add temporary edge to network for viewing the edge which is removed from current network
				if (!recordEdge.containsKey(edge)) {
					CyNetwork net = currentView.getModel();
					if (net.containsNode(edge.getSource()) && net.containsNode(edge.getTarget())) {
						((CySubNetwork)net).addEdge(edge);
						eventHelper.flushPayloadEvents();
						view = currentView.getEdgeView(edge);
						eventHelper.flushPayloadEvents();
					}
				} else {
					view = currentView.getEdgeView(recordEdge.get(edge));
				}
			} else if (!edgeViewList.contains(view)) {
				if (!view.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
					view.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true);
				}
			}

			if (view != null) {
				for (VisualProperty<?> vp: propertyValues.keySet()) {
					if (view.isValueLocked(vp))
						view.clearValueLock(vp);
					view.setVisualProperty(vp, propertyValues.get(vp));
				}
			}
		}
		currentView.updateView();
	}

	private void handleNetwork(final CyNetworkView currentView) {
		Map<VisualProperty<?>, Object> propertyValues = networkPropertyMap.get(currentView.getModel());
		for (VisualProperty<?> vp: propertyValues.keySet()) {
			if (currentView.isValueLocked(vp))
				currentView.clearValueLock(vp);
			currentView.setVisualProperty(vp, propertyValues.get(vp));
		}
	}
	
	private void handleAnnotations(final CyNetworkView currentView) {
		for (Annotation annotation: annotationPropertyMap.keySet()) {
			// debug("looking at annotation: "+annotation);
			CyAnnotationView view = CyAnnotationView.getAnnotationView(annotation, annotationViewList);
			if (view == null || !annotationPresent(currentView, annotation)) {
				// debug("view is null");
				// Add temporary annotation to view for viewing the annotation which is removed from current network
				if (!recordAnnotation.containsKey(annotation)) {
					// debug("Making copy");
					CyAnnotation artAnnotation = copyAnnotation(currentView, 
					                                            annotation.getClass(), 
																			                annotation.getArgMap());
					// debug("new annotation: "+artAnnotation.toString());
					recordAnnotation.put(annotation, artAnnotation.getAnnotation());
					annotationManager.addAnnotation(artAnnotation.getAnnotation());
					view = CyAnnotationView.getAnnotationView(currentView, artAnnotation.getAnnotation());
					artAnnotation.getAnnotation().update();
					currentView.updateView();
				} else {
					// debug("not making copy");
					view = CyAnnotationView.getAnnotationView(currentView, recordAnnotation.get(annotation));
				}
			}
			// debug("getting property map");
			Map<VisualProperty<?>, Object> propertyValues = annotationPropertyMap.get(annotation);
			for (VisualProperty<?> vp: propertyValues.keySet()) {
				if (view.isValueLocked(vp))
					view.clearValueLock(vp);
				// if (propertyValues.get(vp) != null)
					// debug("setting visual property "+vp+" to "+propertyValues.get(vp));
				view.setVisualProperty(vp, propertyValues.get(vp));
			}
			// debug("updating annotation");
			view.getModel().getAnnotation().update();
		}
		// debug("updating view");
		currentView.updateView();
	}

	private boolean annotationPresent(CyNetworkView networkView, Annotation annotation) {
		List<Annotation> annList = annotationManager.getAnnotations(networkView);
		if (annList == null) 
			return false;
		return annList.contains(annotation);
	}

	@SuppressWarnings("unchecked")
	private CyAnnotation copyAnnotation(final CyNetworkView networkView, 
											                Class<?> annClass,
	                                    Map<String, String> argMap) { 
		argMap.remove("uuid"); // Generate a new UUID
		if (BoundedTextAnnotation.class.isAssignableFrom(annClass))  {
			AnnotationFactory<BoundedTextAnnotation> factory = 
					bundleContext.getService(AnnotationFactory.class, "(type=BoundedTextAnnotation.class)");
			BoundedTextAnnotation bt = factory.createAnnotation(BoundedTextAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(bt);
		} else if (ShapeAnnotation.class.isAssignableFrom(annClass))  {
			AnnotationFactory<ShapeAnnotation> factory = 
					bundleContext.getService(AnnotationFactory.class, "(type=ShapeAnnotation.class)");
			ShapeAnnotation sa = factory.createAnnotation(ShapeAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(sa);
		} else if (TextAnnotation.class.isAssignableFrom(annClass))  {
			AnnotationFactory<TextAnnotation> factory = 
					bundleContext.getService(AnnotationFactory.class, "(type=TextAnnotation.class)");
			TextAnnotation ta = factory.createAnnotation(TextAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(ta);
		} else if (ImageAnnotation.class.isAssignableFrom(annClass))  {
			AnnotationFactory<ImageAnnotation> factory = 
					bundleContext.getService(AnnotationFactory.class, "(type=ImageAnnotation.class)");
			ImageAnnotation ia = factory.createAnnotation(ImageAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(ia);
		} else if (ArrowAnnotation.class.isAssignableFrom(annClass))  {
			AnnotationFactory<ArrowAnnotation> factory = 
					bundleContext.getService(AnnotationFactory.class, "(type=ArrowAnnotation.class)");
			ArrowAnnotation aa = factory.createAnnotation(ArrowAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(aa);
		}
		return null;
	}

	/**
	 * Return the frame ID for this frame
	 *
	 * @return the frame ID
	 *
	 */
	public String getID() {
		return frameid;
	}

	public void setID(String ID) {
		frameid = ID;
	}

	/**
	 * Return the CyNetwork for this frame
	 *
	 * @return the CyNetwork
	 */
	public CyNetwork getCurrentNetwork() {
		return currentNetwork;
	}

	public CyNetworkView getNetworkView() {
		return networkView;
	}

	/**
	 * Return the number of frames to be interpolated between this frame and the next.
	 *
	 * @return the frame number
	 */
	public int getInterCount() {
		return intercount;
	}

	/**
	 * Set the number of frames to be interpolated between this frame and the next.
	 *
	 * @param interCount the number of frames to interpret
	 */
	public void setInterCount(int intercount) {
		this.intercount = intercount;
	}

	/**
	 * Get the list of node views in this frame
	 *
	 * @return the list of node views
	 */
	public Set<View<? extends CyIdentifiable>> getNodeViewList() {
		return nodeViewList;
	}

	/**
	 * Get the list of nodes in this frame
	 *
	 * @return the list of nodes
	 */
	public Set<CyNode> getNodeList() {
		return nodeList;
	}

	/**
	 * Get the list of edge views in this frame
	 *
	 * @return the list of edge views
	 */
	public Set<View<? extends CyIdentifiable>> getEdgeViewList() {
		return edgeViewList;
	}

	/**
	 * Get the list of edges in this frame
	 *
	 * @return the list of edges
	 */
	public Set<CyEdge> getEdgeList() {
		return edgeList;
	}

	/**
	 * Get the list of annotation views in this frame
	 *
	 * @return the list of annotation views
	 */
	public Set<CyAnnotationView> getAnnotationViewList() {
		return annotationViewList;
	}

	/**
	 * Get the list of annotations in this frame
	 *
	 * @return the list of annotations
	 */
	public List<Annotation> getAnnotationList() {
		return new ArrayList<>(annotationList);
	}

	/**
	 * Set the list of node views in this frame
	 *
	 * @param nodeList the list of node views
	 */
	public void setNodeViewList(Set<View<? extends CyIdentifiable>>nodeList) {
		this.nodeViewList = nodeList;
	}

	/**
	 * Set the list of nodes in this frame
	 *
	 * @param nodeList the list of nodes
	 */
	public void setNodeList(Set<CyNode>nodeList) {
		this.nodeList = nodeList;
	}

	/**
	 * Set the list of edge views in this frame
	 *
	 * @param edgeList the list of edge views
	 */
	public void setEdgeViewList(Set<View<? extends CyIdentifiable>>edgeList) {
		this.edgeViewList = edgeList;
	}

	/**
	 * Set the list of edges in this frame
	 *
	 * @param edgeList the list of edges
	 */
	public void setEdgeList(Set<CyEdge>edgeList) {
		this.edgeList = edgeList;
	}

	/**
	 * Set the list of annotation views in this frame
	 *
	 * @param annotationViewList the list of annotation views
	 */
	public void setAnnotationViewList(Set<CyAnnotationView> annotationViewList) {
		this.annotationViewList = annotationViewList;
	}

	/**
	 * Set the list of annotations in this frame
	 *
	 * @param annotationList the list of annotations
	 */
	public void setAnnotationList(List<Annotation> annotationList) {
		this.annotationList = new HashSet<>(annotationList);
	}

	/**
	 * Get the Image for this frame
	 *
	 * @return the image for this frame
	 */
	public BufferedImage getFrameImage() {
		return this.networkImage;
	}

	public BufferedImage getNetworkImage(double scale) {
		display();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CyNetworkView view = appManager.getCurrentNetworkView();
		return getNetworkImage(view, scale);
	}

	/**
 	 * Export a graphic image for this frame
 	 *
 	 * @param fileName the file to write the image to
 	 */
	public void writeImage(String fileName, final int videoResolution,final BooleanWrapper finished) throws IOException {
		display();
		// Make sure we let the renderer catch up
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CyNetworkView view = appManager.getCurrentNetworkView();
		BufferedImage image = getNetworkImage(view, (double)videoResolution/100.0);
		ImageIO.write(image, "PNG", new File(fileName));

		clearDisplay();
	}

	/**
	 * Returns the BundleContext of this CyFrame.
	 * @return the BundleContext of this CyFrame.
	 */
	public CyServiceRegistrar getBundleContext() {
		return bundleContext;
	}

	public void loadFrame(CySession session, VisualLexicon lex, JSONObject frameObject) {
		int frameNumber = ((Long) frameObject.get("frameNumber")).intValue();
		frameid = (String) frameObject.get("frameId");
		intercount = ((Long) frameObject.get("interCount")).intValue();

		// We need to reset to get the right nodes and node views
		nodeViewList = new HashSet<View<? extends CyIdentifiable>>();
		nodeList = new HashSet<CyNode>();

		// Create property maps
		for (Object nodeEntry: (JSONArray)frameObject.get("nodes")) {
			JSONObject jsonNode = (JSONObject) nodeEntry;
			CyNode node = session.getObject((Long)jsonNode.get("suid"), CyNode.class);
			if (networkView.getNodeView(node) == null)
				continue;
			nodeList.add(node);
			nodeViewList.add(networkView.getNodeView(node));
			nodePropertyMap.put(node, new HashMap<VisualProperty<?>, Object>());
			populatePropertyMap((JSONArray)jsonNode.get("properties"), 
			                    nodePropertyMap.get(node), lex, CyNode.class);
		}

		// We need to reset to get the right nodes and node views
		edgeViewList = new HashSet<View<? extends CyIdentifiable>>();
		edgeList = new HashSet<CyEdge>();

		for (Object edgeEntry: (JSONArray)frameObject.get("edges")) {
			JSONObject jsonEdge = (JSONObject) edgeEntry;
			CyEdge edge = session.getObject((Long)jsonEdge.get("suid"), CyEdge.class);
			if (networkView.getEdgeView(edge) == null)
				continue;
			edgeList.add(edge);
			edgeViewList.add(networkView.getEdgeView(edge));
			edgePropertyMap.put(edge, new HashMap<VisualProperty<?>, Object>());
			populatePropertyMap((JSONArray)jsonEdge.get("properties"), 
			                    edgePropertyMap.get(edge), lex, CyEdge.class);
		}

		for (Object networkEntry: (JSONArray)frameObject.get("networks")) {
			JSONObject jsonNetwork = (JSONObject) networkEntry;
			CyNetwork network = session.getObject((Long)jsonNetwork.get("suid"), CyNetwork.class);
			networkPropertyMap.put(network, new HashMap<VisualProperty<?>, Object>());
			populatePropertyMap((JSONArray)jsonNetwork.get("properties"), 
			                    networkPropertyMap.get(network), lex, CyNetwork.class);
		}

		annotationList = new HashSet<>();
		List<Annotation> allAnnotations = annotationManager.getAnnotations(networkView);
		for (Object annotationEntry: (JSONArray)frameObject.get("annotations")) {
			JSONObject jsonAnnotation = (JSONObject) annotationEntry;
			Annotation annotation = getAnnotationFromList((String)jsonAnnotation.get("uuid"), 
			                                              allAnnotations);
			if (annotation == null) {
				// OK, the annotation didn't get saved, so we need to re-created it.
				Map<String, String> annMap = 
								createArgMap((String)jsonAnnotation.get("annotation"));
				if (annMap != null && annMap.size() > 0) {
					try {
						Class<?> annClass = Class.forName(annMap.get("type"));
						CyAnnotation cyAnn = copyAnnotation(networkView, annClass, annMap);
						annotation = cyAnn.getAnnotation();
					} catch (ClassNotFoundException cnfe) {
						continue;
					}
				} else {
					System.out.println("Can't find annotation: "+(String)jsonAnnotation.get("suid"));
					continue;
				}
			}
			annotationList.add(annotation);
			annotationPropertyMap.put(annotation, new HashMap<VisualProperty<?>, Object>()); 
			populatePropertyMap((JSONArray)jsonAnnotation.get("properties"), 
			                     annotationPropertyMap.get(annotation), 
			                     annotationLexicon, Annotation.class);
		}
		annotationViewList = 
					new HashSet<CyAnnotationView>(CyAnnotationView.wrapViews(networkView, annotationList));

		try {
			// Finally, update our image
			display();
			captureImage(networkView);
		} catch (IOException ioe) {
		}

	}

	@SuppressWarnings("rawtypes")
	private void populatePropertyMap(JSONArray array, Map<VisualProperty<?>, Object> propertyMap, 
	                                 VisualLexicon visualLexicon, Class<?> type) {
		for (Object entry: array) {
			JSONObject property = (JSONObject) entry;
			for (Object key: property.keySet()) {
				try {
				VisualProperty p = visualLexicon.lookup(type, (String) key);
				Object v = p.parseSerializableString((String)property.get(key));
				propertyMap.put(p, v);
				} catch (Exception e) {
					System.out.println("Exception: "+key+"="+(String)property.get(key));
					System.out.println("Type = "+type);
					System.out.println("Property = "+visualLexicon.lookup(type, (String) key));
				}
			}
		}
	}

	private Annotation getAnnotationFromList(String uuid, 
	                                         List<Annotation> annotations) {
		UUID annotationID = UUID.fromString(uuid);
		for (Annotation a: annotations) {
			if (a.getUUID().equals(annotationID))
				return a;
		}

		return null;
	}

	public void writeFrame(BufferedWriter writer, int frameNumber) throws IOException {
		writer.write("\t\t{\n");
		writer.write("\t\t\t\"frameNumber\": "+frameNumber+",\n");
		writer.write("\t\t\t\"frameID\": \""+frameid+"\",\n");
		writer.write("\t\t\t\"interCount\": "+intercount+",\n");

		// Write out our node data
		writer.write("\t\t\t\"nodes\": [\n");
		boolean first = true;
		for (CyNode node: nodeList) {
			writeCyIdentifiable(writer, node.getSUID(), nodePropertyMap.get(node), first);
			first = false;
		}
		writer.write("\n\t\t\t],\n");

		// Write out our edge data
		writer.write("\t\t\t\"edges\": [\n");
		first = true;
		for (CyEdge edge: edgeList) {
			writeCyIdentifiable(writer, edge.getSUID(), edgePropertyMap.get(edge), first);
			first = false;
		}
		writer.write("\n\t\t\t],\n");

		// Write out our network data
		writer.write("\t\t\t\"networks\": [\n");
		first = true;
		// Only have one network
		writeCyIdentifiable(writer, currentNetwork.getSUID(), 
		                    networkPropertyMap.get(currentNetwork), first);

		writer.write("\n\t\t\t],\n");

		// Write out our annotation data
		writer.write("\t\t\t\"annotations\": [\n");
		first = true;
		for (Annotation annotation: annotationList) {
			writeAnnotation(writer, annotation, 
			                annotationPropertyMap.get(annotation), first);
			first = false;
		}
		writer.write("\n\t\t\t]\n");
		writer.write("\t\t}");
	}

	private void writeCyIdentifiable(BufferedWriter writer, Long id, 
	                                 Map<VisualProperty<?>, Object> vpMap, 
														    	 boolean first) throws IOException {
		if (vpMap == null || vpMap.size() == 0) return;

		if (!first)
			writer.write(",\n");

		writer.write("\t\t\t\t{ \"suid\": "+id+",\n");
		writeProperties(writer, vpMap);
		writer.write("\t\t\t\t}");
	}

	private void writeAnnotation(BufferedWriter writer, Annotation ann,
	                             Map<VisualProperty<?>, Object> vpMap, 
														   boolean first) throws IOException {
		if (vpMap == null || vpMap.size() == 0) return;

		if (!first)
			writer.write(",\n");

		writer.write("\t\t\t\t{ \"uuid\": \""+ann.getUUID().toString()+"\",\n");
		// We need to serialize the annotation since it may not be present 
		// in the session file, so we'll need to restore it
		writer.write("\t\t\t\t  \"annotation\": \""+serializeAnnotation(ann)+"\",\n");
		writeProperties(writer, vpMap);
		writer.write("\t\t\t\t}");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void writeProperties(BufferedWriter writer, Map<VisualProperty<?>, Object> vpMap) 
	                             throws IOException {
		writer.write("\t\t\t\t  \"properties\": [\n");
		boolean vFirst = true;
		for (VisualProperty property: vpMap.keySet()) {
			Object value = vpMap.get(property);
			if (value != null) {
				if (!vFirst)
					writer.write(",\n");
				writer.write("\t\t\t\t\t\t{ \""+property.getIdString()+"\": \""+
				             stringify(property.toSerializableString(value))+"\" }");
				vFirst = false;
			}
		}
		writer.write("\t\t\t\t  ]\n");
	}

	private String serializeAnnotation(Annotation annotation) {
		Map<String,String> argMap = annotation.getArgMap();
		String result = null;
		for (String arg: argMap.keySet()) {
			if (result == null) 
				result = arg+"="+argMap.get(arg);
			else
				result += "|"+arg+"="+argMap.get(arg);
		}
		return result;
	}

	private String stringify(String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '"')
				sb.append('\\');
			sb.append(c);
		}
		return sb.toString();
	}

	private Map<String, String> createArgMap(String annString) {
		Map<String,String> argMap = new HashMap<>();
		String[] tokens = annString.split("|");
		for (String token: tokens) {
			String[] pair = token.split("=");
			argMap.put(pair[0], pair[1]);
		}
		return argMap;
	}

	private void debug(String str) {
		if (frameid != null && frameid.length() > 0) {
			System.out.println("Annotation "+frameid+": "+str);
		}
	}

	private BufferedImage getNetworkImage(CyNetworkView view, double scale) {
		double width = view.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		double height = view.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);
		BufferedImage image = new BufferedImage((int)(width*scale), (int)(height*scale), BufferedImage.TYPE_INT_RGB);
		view.updateView();
		Graphics2D g2d = (Graphics2D) image.getGraphics();
		g2d.scale(scale, scale);
		frameManager.getRenderingEngine().printCanvas(g2d);
		g2d.dispose();
		return image;
	}


	class DisplayFrame implements Runnable {
			CyNetworkView currentView;
			CyFrame frame;

			public DisplayFrame(CyNetworkView view, CyFrame frame) {
				currentView = view;
				this.frame = frame;
			}

			public void run() {
				// System.out.println("Displaying frame: "+frame.toString());
				handleMissingEdges(currentView);
				handleMissingNodes(currentView);
				handleMissingAnnotations(currentView);

				handleNodes(currentView);
				handleEdges(currentView);
				handleNetwork(currentView);
				handleAnnotations(currentView);

				eventHelper.flushPayloadEvents();
				currentView.updateView();
			}
	}

	class ClearDisplay implements Runnable {
		final CyApplicationManager appManager;
		final Collection<CyEdge> removeAddedEdges;
		final Collection<CyNode> removeAddedNodes;
		final Collection<Long> removeAddedEdgesKeys;
		final Collection<Long> removeAddedKeys;

		public ClearDisplay(final CyApplicationManager appManager,
		                    final Collection<CyEdge> removeAddedEdges,
												final Collection<CyNode> removeAddedNodes,
												final Collection<Long> removeAddedEdgesKeys,
												final Collection<Long> removeAddedKeys) {
			this.appManager = appManager;
			this.removeAddedEdges = removeAddedEdges;
			this.removeAddedEdgesKeys = removeAddedEdgesKeys;
			this.removeAddedNodes = removeAddedNodes;
			this.removeAddedKeys = removeAddedKeys;
		}

		public void run() {
 			CyNetwork network = appManager.getCurrentNetworkView().getModel();
			network.removeEdges(removeAddedEdges);
 			network.getDefaultEdgeTable().deleteRows(removeAddedEdgesKeys);

 			network.removeNodes(removeAddedNodes);
 			network.getDefaultNodeTable().deleteRows(removeAddedKeys);
 			appManager.getCurrentNetworkView().updateView();
		}
	}

}

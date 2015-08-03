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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
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
	private CyNetworkView networkView = null;
	private CyNetwork currentNetwork = null;
	private BufferedImage networkImage = null;
	private Set<View<? extends CyIdentifiable>> nodeViewList = null;
	private Set<View<? extends CyIdentifiable>> edgeViewList = null;
	private Set<CyAnnotationView> annotationViewList = null;
	private Set<CyNode> nodeList = null;
	private Set<CyEdge> edgeList = null;
	private List<Annotation> annotationList = null;
	private VisualStyle vizStyle = null;
	private int intercount = 0;
	private SynchronousTaskManager<?> taskManager;
	private FrameManager frameManager;
	private AnnotationFactory<?> annotationFactory;
	private AnnotationManager annotationManager;

	private static final int IMAGE_WIDTH = 200, IMAGE_HEIGHT = 150;

	/**
	 * Creates this CyFrame by initializing and populating all of the fields.
	 *
	 * @param currentNetwork
	 */
	public CyFrame(CyServiceRegistrar bc, FrameManager frameManager){
		bundleContext = bc;
		this.frameManager = frameManager;
		appManager = bundleContext.getService(CyApplicationManager.class);
		taskManager = bundleContext.getService(SynchronousTaskManager.class);
		annotationManager = bundleContext.getService(AnnotationManager.class);
		annotationFactory = bundleContext.getService(AnnotationFactory.class);
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

		this.currentNetwork = appManager.getCurrentNetwork();
		networkView = appManager.getCurrentNetworkView();

		// Now get all of our annotations
		annotationList = annotationManager.getAnnotations(networkView);
		if (annotationList == null)
			annotationList = new ArrayList<>();
		annotationViewList = new HashSet<CyAnnotationView>(CyAnnotationView.wrapViews(annotationList));

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
				for (VisualProperty<?> property: interpolatorMap.keySet()) {
					if (property.getTargetDataType().isAssignableFrom(CyAnnotation.class)) {
						annotationPropertyMap.get(annotation).put(property, view.getVisualProperty(property));
					}
				}
			}
		}
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
	public void captureImage() throws IOException {

	/*	double scale = .35;
		double wscale = .25; */

		CyNetworkView view = appManager.getCurrentNetworkView();

		NetworkViewTaskFactory exportImageTaskFactory = bundleContext.getService(NetworkViewTaskFactory.class, "(&(commandNamespace=view)(command=export))");
		if (exportImageTaskFactory != null && exportImageTaskFactory.isReady(view)) {
			TunableSetter tunableSetter = bundleContext.getService(TunableSetter.class);
			Map<String, Object> tunables = new HashMap<String, Object>();
			List<String> fileTypeList = new ArrayList<String>();
			fileTypeList.add(PNG);
			ListSingleSelection<String> fileType = new ListSingleSelection<String>(fileTypeList);
			fileType.setSelectedValue(PNG);
			tunables.put("options", fileType);
			final File temporaryImageFile = File.createTempFile("temporaryCytoscapeImage", ".png");
			tunables.put("OutputFile", temporaryImageFile);
			taskManager.execute(tunableSetter.createTaskIterator(
					exportImageTaskFactory.createTaskIterator(view), tunables),
					new TaskObserver() {

						public void taskFinished(ObservableTask arg0) {
							// TODO Auto-generated method stub
						}

						public void allFinished(FinishStatus arg0) {
							BufferedImage image = null, scaledImage = null;
							try {
								image = ImageIO.read(temporaryImageFile);
								int type = image.getType() == 0? BufferedImage.TYPE_INT_ARGB : image.getType();
								scaledImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, type);
								Graphics2D g = scaledImage.createGraphics();
								g.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
								g.dispose();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							networkImage = scaledImage;
						//	temporaryImageFile.delete();
						}
					});
		}

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
		DisplayFrame displayFrame = new DisplayFrame(currentView);
		if (SwingUtilities.isEventDispatchThread()) {
			displayFrame.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( displayFrame );
			} catch (Exception e) {}
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
			} catch(Exception e) {}
		}
  }

	private void handleMissingNodes(final CyNetworkView currentView) {
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
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
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
			currentView.updateView();
		}
	}
	
	private void handleMissingAnnotations(final CyNetworkView currentView) {
		List<Annotation> removeAnnotations = new ArrayList<>();
		List<Annotation> annotations = annotationManager.getAnnotations(currentView);
		if (annotations != null) {
			for (Annotation annotation : annotations) {
				if (!annotationList.contains(annotation)) {
					CyAnnotationView view = new CyAnnotationView(annotation);
					System.out.println("Hiding annotation: "+view.getModel().getAnnotation());
					view.setVisualProperty(AnnotationLexicon.ANNOTATION_VISIBLE, false);
				} else {
					CyAnnotationView view = new CyAnnotationView(annotation);
					System.out.println("Showing annotation: "+view.getModel().getAnnotation());
					view.setVisualProperty(AnnotationLexicon.ANNOTATION_VISIBLE, true);
				}
				annotation.update();
			}
		}
	}

	private void handleNodes(final CyNetworkView currentView) {
		for (CyNode node: nodePropertyMap.keySet()) {
			Map<VisualProperty<?>, Object> propertyValues = nodePropertyMap.get(node);
			View<CyNode> view = currentView.getNodeView(node);
			if (view == null) {
				// Add temporary node to network for viewing the node which is removed from current network
				if (!recordNode.containsKey(node)) {
					CyNode artNode = currentView.getModel().addNode();
					recordNode.put(node, artNode);
					currentView.updateView();
					view = currentView.getNodeView(artNode);
				} else {
					view = currentView.getNodeView(recordNode.get(node));
				}
			}
			for (VisualProperty<?> vp: propertyValues.keySet()) {
				if (view.isValueLocked(vp))
					view.clearValueLock(vp);
				view.setVisualProperty(vp, propertyValues.get(vp));
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
					CyEdge artEdge = null;
					if (recordNode.containsKey(edge.getSource()) &&
					    nodeList.contains(edge.getTarget()) &&
							!recordNode.containsKey(edge.getTarget())) {
						artEdge = currentView.getModel().addEdge(recordNode.get(edge.getSource()), edge.getTarget(), true);
					} else if (nodeList.contains(edge.getSource()) &&
					           !recordNode.containsKey(edge.getSource()) &&
										 recordNode.containsKey(edge.getTarget())) {
						artEdge = currentView.getModel().addEdge(edge.getSource(), recordNode.get(edge.getTarget()), true);
					} else if (recordNode.containsKey(edge.getSource()) &&
					           recordNode.containsKey(edge.getTarget())) {
						artEdge = currentView.getModel().addEdge(recordNode.get(edge.getSource()), recordNode.get(edge.getTarget()), true);
					} else {
						continue;
					}
					currentView.updateView();
					view = currentView.getEdgeView(artEdge);
					recordEdge.put(edge, artEdge);
				} else {
					view = currentView.getEdgeView(recordEdge.get(edge));
				}
			}
			for (VisualProperty<?> vp: propertyValues.keySet()) {
				if (view.isValueLocked(vp))
					view.clearValueLock(vp);
				view.setVisualProperty(vp, propertyValues.get(vp));
			}
		}
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
			CyAnnotationView view = CyAnnotationView.getAnnotationView(annotation, annotationViewList);
			if (view == null) {
				// Add temporary node to view for viewing the annotation which is removed from current network
				if (!recordAnnotation.containsKey(annotation)) {
					CyAnnotation artAnnotation = copyAnnotation(currentView, annotation);
					recordAnnotation.put(annotation, artAnnotation.getAnnotation());
					annotationManager.addAnnotation(artAnnotation.getAnnotation());
					view = new CyAnnotationView(artAnnotation);
					currentView.updateView();
				} else {
					view = new CyAnnotationView(annotation);
				}
			}
			Map<VisualProperty<?>, Object> propertyValues = annotationPropertyMap.get(annotation);
			for (VisualProperty<?> vp: propertyValues.keySet()) {
				if (view.isValueLocked(vp))
					view.clearValueLock(vp);
				view.setVisualProperty(vp, propertyValues.get(vp));
			}
			annotation.update();
		}
		currentView.updateView();
	}

	@SuppressWarnings("unchecked")
	private CyAnnotation copyAnnotation(final CyNetworkView networkView, Annotation annotation) {
		Map<String, String> argMap = annotation.getArgMap();
		argMap.remove("uuid"); // Generate a new UUID
		if (annotation instanceof BoundedTextAnnotation)  {
			AnnotationFactory<BoundedTextAnnotation> factory = (AnnotationFactory<BoundedTextAnnotation>)annotationFactory;
			BoundedTextAnnotation bt = factory.createAnnotation(BoundedTextAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(bt);
		} else if (annotation instanceof ShapeAnnotation)  {
			AnnotationFactory<ShapeAnnotation> factory = (AnnotationFactory<ShapeAnnotation>)annotationFactory;
			ShapeAnnotation sa = factory.createAnnotation(ShapeAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(sa);
		} else if (annotation instanceof TextAnnotation)  {
			AnnotationFactory<TextAnnotation> factory = (AnnotationFactory<TextAnnotation>)annotationFactory;
			TextAnnotation ta = factory.createAnnotation(TextAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(ta);
		} else if (annotation instanceof ImageAnnotation)  {
			AnnotationFactory<ImageAnnotation> factory = (AnnotationFactory<ImageAnnotation>)annotationFactory;
			ImageAnnotation ia = factory.createAnnotation(ImageAnnotation.class, networkView, argMap);
			return new CyAnnotationImpl(ia);
		} else if (annotation instanceof ArrowAnnotation)  {
			AnnotationFactory<ArrowAnnotation> factory = (AnnotationFactory<ArrowAnnotation>)annotationFactory;
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
		return annotationList;
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
		this.annotationList = annotationList;
	}

	/**
	 * Get the Image for this frame
	 *
	 * @return the image for this frame
	 */
	public BufferedImage getFrameImage() {
		return this.networkImage;
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

		NetworkViewTaskFactory exportImageTaskFactory = (NetworkViewTaskFactory) bundleContext.getService(NetworkViewTaskFactory.class, "(&(commandNamespace=view)(command=export))");
		if (exportImageTaskFactory != null && exportImageTaskFactory.isReady(view)) {
			Map<String, Object> tunables = new HashMap<String, Object>();
			List<String> fileTypeList = new ArrayList<String>();
			fileTypeList.add(PNG);
			ListSingleSelection<String> fileType = new ListSingleSelection<String>(fileTypeList);
			fileType.setSelectedValue(PNG);
			tunables.put("options", fileType);
			tunables.put("OutputFile", new File(fileName));
			tunables.put("Zoom", new BoundedDouble(0.0, (double) videoResolution, (double) videoResolution,true,false));
			taskManager.setExecutionContext(tunables);
			taskManager.execute(exportImageTaskFactory.createTaskIterator(view));
			finished.setValue(true);
		}
		clearDisplay();
	}

	/**
	 * Returns the BundleContext of this CyFrame.
	 * @return the BundleContext of this CyFrame.
	 */
	public CyServiceRegistrar getBundleContext() {
		return bundleContext;
	}

	class DisplayFrame implements Runnable {
			CyNetworkView currentView;

			public DisplayFrame(CyNetworkView view) {
				currentView = view;
			}

			public void run() {
				handleMissingEdges(currentView);
				handleMissingNodes(currentView);
				handleMissingAnnotations(currentView);

				handleNodes(currentView);
				handleEdges(currentView);
				handleNetwork(currentView);
				handleAnnotations(currentView);

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

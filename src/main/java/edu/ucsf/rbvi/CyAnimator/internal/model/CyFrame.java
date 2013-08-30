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

import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.*;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.util.ListSingleSelection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class CyFrame {
	
	private String frameid = "";
	private static String PNG = "png";
	private HashMap<String, double[]> nodePosMap;
	private HashMap<String, Color> nodeColMap;
	private HashMap<String, Integer> nodeOpacityMap;
	private HashMap<String, Color> nodeFillColMap;
	private HashMap<String, Integer> nodeFillOpacityMap;
	private HashMap<String, Double> nodeBorderWidthMap;
	private HashMap<String, Color> nodeBorderColorMap;
	private HashMap<String, Integer> nodeBorderTransMap;
	private HashMap<String, double[]> nodeSizeMap;
	private HashMap<String, Color> nodeLabelColMap;
	private HashMap<String, Integer> nodeLabelFontSizeMap;
	private HashMap<String, Integer> nodeLabelTransMap;

	private HashMap<String, Integer> edgeOpacityMap;
	private HashMap<String, Integer> edgeStrokeOpacityMap;
	private HashMap<String, Color> edgeColMap;
	private HashMap<String, Color> edgeStrokeColMap;
	private HashMap<String, Double> edgeWidthMap;
	private HashMap<String, Color> edgeLabelColMap;
	private HashMap<String, Integer> edgeLabelFontSizeMap;
	private HashMap<String, Integer> edgeLabelTransMap;
	
	private Paint backgroundPaint = null;
	private double zoom = 0;
	
	private double xalign;
	private double yalign;
	
	private BundleContext bundleContext;
	private CyApplicationManager appManager;
	private CyNetworkView networkView = null;
	private CyNetwork currentNetwork = null;
	private CyTable nodeTable = null, edgeTable = null;
	private BufferedImage networkImage = null;
	private Map<String, View<CyNode>> nodeMap = null;
	private Map<String, View<CyEdge>> edgeMap = null;
	private VisualStyle vizStyle = null;
	private List<CyNode> nodeList = null;
	private List<CyEdge> edgeList = null;
	private List<String> nodeIdList = null;
	private List<String> edgeIdList = null;
	private int intercount = 0;
	private Point2D centerPoint = null;
	private TaskManager<?,?> taskManager;

	private static int IMAGE_WIDTH = 200, IMAGE_HEIGHT = 150;
//	private DGraphView dview = null; 
	
	/**
	 * Creates this CyFrame by initializing and populating all of the fields.
	 * 
	 * @param currentNetwork
	 */
	public CyFrame(BundleContext bc){
		bundleContext = bc;
		appManager = (CyApplicationManager) getService(CyApplicationManager.class);
		taskManager = (TaskManager<?, ?>) getService(TaskManager.class);
		nodePosMap = new HashMap<String, double[]>();
		nodeColMap = new HashMap<String, Color>();
		nodeFillColMap = new HashMap<String, Color>();
		nodeLabelColMap = new HashMap<String, Color>();
		nodeLabelFontSizeMap = new HashMap<String, Integer>();
		nodeLabelTransMap = new HashMap<String, Integer>();
		nodeSizeMap = new HashMap<String, double[]>();
		nodeBorderWidthMap = new HashMap<String, Double>();
		nodeBorderColorMap = new HashMap<String, Color>();
		nodeBorderTransMap = new HashMap<String, Integer>();
		edgeMap = new HashMap<String, View<CyEdge>>();
		nodeMap = new HashMap<String, View<CyNode>>();
		nodeOpacityMap = new HashMap<String, Integer>();
		nodeFillOpacityMap = new HashMap<String, Integer>();
		edgeOpacityMap = new HashMap<String, Integer>();
		edgeStrokeOpacityMap = new HashMap<String, Integer>();
		edgeColMap = new HashMap<String, Color>();
		edgeStrokeColMap = new HashMap<String, Color>();
		edgeLabelColMap = new HashMap<String, Color>();
		edgeLabelFontSizeMap = new HashMap<String, Integer>();
		edgeLabelTransMap = new HashMap<String, Integer>();
		edgeWidthMap = new HashMap<String, Double>();
		this.currentNetwork = appManager.getCurrentNetwork();
		networkView = appManager.getCurrentNetworkView();
		nodeTable = currentNetwork.getDefaultNodeTable();
		edgeTable = currentNetwork.getDefaultEdgeTable();
	//	this.dview = (DGraphView)networkView;
		this.centerPoint = new Point2D.Double(networkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION),
									networkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION));

		nodeIdList = new ArrayList<String>();
		edgeIdList = new ArrayList<String>();
		
		// Initialize our node view maps
		for (View<CyEdge> ev: networkView.getEdgeViews()) {
			if (ev.getModel() == null) continue;
			String edgeid = edgeTable.getRow(ev.getModel().getSUID()).get(CyNetwork.NAME, String.class);
			edgeMap.put(edgeid, ev);
			edgeIdList.add(edgeid);
		}

		// Initialize our edge view maps
		for (View<CyNode> nv: networkView.getNodeViews()) {
			String nodeid = nodeTable.getRow(nv.getModel().getSUID()).get(CyNetwork.NAME, String.class);
			nodeMap.put(nodeid, nv);
			nodeIdList.add(nodeid);
		}

		// Remember the visual style
		VisualMappingManager visualManager = (VisualMappingManager) getService(VisualMappingManager.class);
		vizStyle = visualManager.getCurrentVisualStyle();

		// Get our initial nodeList
		nodeList = currentNetwork.getNodeList();

		// Get our initial edgeList
		edgeList = currentNetwork.getEdgeList();
		
		
	}
	
	/*
	 * Captures all of the current visual settings for nodes and edges from a 
	 * CyNetworkView and stores them in this frame.
	 */
	public void populate() {
		backgroundPaint = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		zoom = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
		xalign = networkView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
		yalign = networkView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
		
	//	dview = (DGraphView)networkView;
		
		CyTable nodeTable = networkView.getModel().getDefaultNodeTable();
		for(CyNode node: nodeList){
		
			View<CyNode> nodeView = networkView.getNodeView(node);
			if(nodeView == null){ continue; }
			String nodeName = nodeTable.getRow(node.getSUID()).get(CyNetwork.NAME, String.class);

			//stores the x and y position of the node
			double[] xy = new double[3];
			xy[0] = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			xy[1] = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			xy[2] = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);
			nodePosMap.put(nodeName, xy);
			
			double height = nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);
			double width = nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
			double[] size = {height, width};
			nodeSizeMap.put(nodeName, size);
			
			double borderWidth = nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH);
			nodeBorderWidthMap.put(nodeName, borderWidth);
			Color borderColor = (Color) nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT);
			nodeBorderColorMap.put(nodeName, borderColor);
			Integer borderTrans = nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY);
			nodeBorderTransMap.put(nodeName, borderTrans);
			
			//grab color and opacity
			Color nodeColor = (Color)nodeView.getVisualProperty(BasicVisualLexicon.NODE_PAINT);
			Integer trans = nodeColor.getAlpha();
			//store in respective hashmap
			nodeColMap.put(nodeName, nodeColor);
			nodeOpacityMap.put(nodeName, trans);

			//grab color and opacity
			Color nodeFillColor = (Color)nodeView.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
			Integer transFill = nodeColor.getAlpha();
			//store in respective hashmap
			nodeFillColMap.put(nodeName, nodeFillColor);
			nodeFillOpacityMap.put(nodeName, transFill);

			// Grab the label information
			Color labelColor = (Color)nodeView.getVisualProperty(BasicVisualLexicon.NODE_LABEL_COLOR);
			nodeLabelColMap.put(nodeName, labelColor);
			Integer labelFontSize = nodeView.getVisualProperty(BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
			nodeLabelFontSizeMap.put(nodeName, labelFontSize);
			Integer labelTrans = nodeView.getVisualProperty(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY);
			nodeLabelTransMap.put(nodeName, labelTrans);

			centerPoint = new Point2D.Double(networkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION),
											networkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION));
			
		}

		CyTable edgeTable = networkView.getModel().getDefaultEdgeTable();
		for(CyEdge edge: edgeList){
			
			View<CyEdge> edgeView = networkView.getEdgeView(edge);
			if(edgeView == null){  continue; }
			String edgeName = edgeTable.getRow(edge.getSUID()).get(CyNetwork.NAME, String.class);

			//grab color and opacity
			Color p = (Color)edgeView.getVisualProperty(BasicVisualLexicon.EDGE_PAINT);
			Integer trans = p.getAlpha();
			//store in respective hashmap
			edgeColMap.put(edgeName, p);
			edgeOpacityMap.put(edgeName, trans);
			edgeWidthMap.put(edgeName, edgeView.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH));

			//grab color and opacity
			Color pStroke = (Color)edgeView.getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
			Integer transStroke = p.getAlpha();
			//store in respective hashmap
			edgeStrokeColMap.put(edgeName, pStroke);
			edgeStrokeOpacityMap.put(edgeName, transStroke);

			// Grab the label information
			Color labelColor = (Color)edgeView.getVisualProperty(BasicVisualLexicon.EDGE_LABEL_COLOR);
			edgeLabelColMap.put(edgeName, labelColor);
			Integer labelFontSize = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE),
					labelTransMap = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY);
			edgeLabelFontSizeMap.put(edgeName, labelFontSize);
			edgeLabelTransMap.put(edgeName, labelTransMap);
		}
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
		
		NetworkViewTaskFactory exportImageTaskFactory = (NetworkViewTaskFactory) getService(NetworkViewTaskFactory.class, "(&(commandNamespace=view)(command=export))");
		if (exportImageTaskFactory != null && exportImageTaskFactory.isReady(view)) {
			TunableSetter tunableSetter = (TunableSetter) getService(TunableSetter.class);
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
						
						public void allFinished() {
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
		VisualMappingManager visualManager = (VisualMappingManager) getService(VisualMappingManager.class);
		visualManager.setVisualStyle(vizStyle, networkView);

		// We want to use the current view in case we're interpolating
		// across views
		CyNetworkView currentView = appManager.getCurrentNetworkView();


		// First see if we have any views we need to remove
		Collection<CyEdge> removeEdges = new ArrayList<CyEdge>();
		CyTable curEdgeTable = currentView.getModel().getDefaultEdgeTable();
		for (CyEdge ev: currentView.getModel().getEdgeList()) {
			if (!edgeMap.containsKey(curEdgeTable.getRow(ev.getSUID()).get(CyNetwork.NAME, String.class)))
				removeEdges.add(ev);
		}

		currentView.getModel().removeEdges(removeEdges);

		// Initialize our edge view maps
		List<CyNode> removeNodes = new ArrayList<CyNode>();
		CyTable curNodeTable = currentView.getModel().getDefaultNodeTable();
		for (CyNode nv: currentView.getModel().getNodeList()) {
			if (!nodeMap.containsKey(curNodeTable.getRow(nv.getSUID()).get(CyNetwork.NAME, String.class)))
				removeNodes.add(nv);
		}

		currentView.getModel().removeNodes(removeNodes);


		for(CyNode node: nodeList)
		{
		
			View<CyNode> nodeView = currentView.getNodeView(node);
			if (nodeView == null) {
			//	addNodeView(currentView, nodeMap.get(node), node);
				nodeView = currentView.getNodeView(node);
			//	Cytoscape.getVisualMappingManager().vizmapNode(nodeView, currentView);
			}
			String nodeName = curNodeTable.getRow(node.getSUID()).get(CyNetwork.NAME, String.class);
			
			double[] xy = nodePosMap.get(nodeName);
			Color p = nodeColMap.get(nodeName), pFill = nodeFillColMap.get(nodeName);
			Integer trans = nodeOpacityMap.get(nodeName), transFill = nodeFillOpacityMap.get(nodeName);
			// System.out.println("DISPLAY "+node+": "+xy[0]+"  "+xy[1]+", trans = "+trans);
			//if(xy == null || nodeView == null){ continue; }
			
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, xy[0]);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, xy[1]);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, xy[2]);
			
			double[] size = nodeSizeMap.get(nodeName);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_HEIGHT, size[0]);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_WIDTH, size[1]);
			
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH, nodeBorderWidthMap.get(nodeName));
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT, nodeBorderColorMap.get(nodeName));
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, nodeBorderTransMap.get(nodeName));

			if (p != null)
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_PAINT, new Color(p.getRed(), p.getGreen(), p.getBlue(), trans));
			if (pFill != null)
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, new Color(pFill.getRed(), pFill.getGreen(), pFill.getBlue(), transFill));

			Color labelColor = nodeLabelColMap.get(nodeName);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_LABEL_COLOR,
										new Color(labelColor.getRed(), 
										labelColor.getGreen(), labelColor.getBlue(), 
										labelColor.getAlpha()));
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, nodeLabelFontSizeMap.get(nodeName));
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, nodeLabelTransMap.get(nodeName));
		}

		for(CyEdge edge: getEdgeList())
		{
			View<CyEdge> edgeView = currentView.getEdgeView(edge);
			if (edgeView == null) {
			//	addEdgeView(currentView, edgeMap.get(edge), edge);
				edgeView = currentView.getEdgeView(edge);
			}
			String edgeName = curEdgeTable.getRow(edge.getSUID()).get(CyNetwork.NAME, String.class);
			Color p = edgeColMap.get(edgeName), pStroke = edgeStrokeColMap.get(edgeName);
			if ((p == null && pStroke == null) || edgeView == null) continue;
			Integer trans = edgeOpacityMap.get(edgeName), transStroke = edgeStrokeOpacityMap.get(edgeName);
			if (p != null)
				edgeView.setVisualProperty(BasicVisualLexicon.EDGE_PAINT, new Color(p.getRed(), p.getGreen(), p.getBlue(), trans));
			if (pStroke != null)
				edgeView.setVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(pStroke.getRed(), pStroke.getGreen(), pStroke.getBlue(), transStroke));
			edgeView.setVisualProperty(BasicVisualLexicon.EDGE_WIDTH, edgeWidthMap.get(edgeName));

			Color labelColor = edgeLabelColMap.get(edgeName);
			edgeView.setVisualProperty(BasicVisualLexicon.EDGE_LABEL_COLOR,
										new Color(labelColor.getRed(), 
										labelColor.getGreen(), labelColor.getBlue(),
										labelColor.getAlpha()));
			Integer labelFontSize = edgeLabelFontSizeMap.get(edgeName),
					labelTrans = edgeLabelTransMap.get(edgeName);
			edgeView.setVisualProperty(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, labelFontSize);
			edgeView.setVisualProperty(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, labelTrans);
		}
		currentView.setVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, backgroundPaint);
		currentView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, zoom);
		//networkView.getComponent().
	//	dview = (DGraphView)currentView;
		
		//InternalFrameComponent ifc = Cytoscape.getDesktop().getNetworkViewManager().getInternalFrameComponent(networkView);
		
		networkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, centerPoint.getX());
		networkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, centerPoint.getY());
		
		//dview.setBounds(x, y, Math.round(ifc.getWidth()), Math.round(ifc.getHeight()));
		//ifc.setBounds(arg0, arg1, arg2, arg3)
		currentView.updateView();
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
	 * Return the zoom value for this frame.
	 *
	 * @return zoom
	 */
	public double getZoom() {
		return zoom;
	}

	/**
	 * Set the zoom value for this frame.
	 *
	 * @param zoom set the zoom value
	 */
	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	/**
	 * Return the background color value for this frame.
	 *
	 * @return the background color
	 */
	public Paint getBackgroundPaint() {
		return backgroundPaint;
	}

	/**
	 * Set the background color value for this frame.
	 *
	 * @param bg set the background color
	 */
	public void setBackgroundPaint(Paint bg) {
		backgroundPaint = bg;
	}

	/**
	 * Get the node position for a node in this frame
	 *
	 * @param nodeID the ID of the node whose position to retrieve
	 * @return the node position as a double array with two values
	 */
	public double[] getNodePosition(String nodeID) {
		if (nodePosMap.containsKey(nodeID))
			return nodePosMap.get(nodeID);
		return null;
	}

	/**
	 * Set the node position for a node in this frame
	 *
	 * @param nodeID the ID of the node whose position to retrieve
	 * @param pos a 2 element double array with the x,y values for this node
	 */
	public void setNodePosition(String nodeID, double[] pos) {
		nodePosMap.put(nodeID, pos);
	}

	/**
	 * Get the node color for a node in this frame
	 *
	 * @param nodeID the ID of the node whose color to retrieve
	 * @return the color 
	 */
	public Color getNodeColor(String nodeID) {
		if (nodeColMap.containsKey(nodeID))
			return nodeColMap.get(nodeID);
		return null;
	}

	/**
	 * Get the node color for a node in this frame
	 *
	 * @param nodeID the ID of the node whose color to retrieve
	 * @return the color 
	 */
	public Color getNodeFillColor(String nodeID) {
		if (nodeFillColMap.containsKey(nodeID))
			return nodeFillColMap.get(nodeID);
		return null;
	}

	/**
	 * Set the node color for a node in this frame
	 *
	 * @param nodeID the ID of the node whose color to retrieve
	 * @param color the color for this node
	 */
	public void setNodeColor(String nodeID, Color color) {
		nodeColMap.put(nodeID, color);
	}

	/**
	 * Set the node color for a node in this frame
	 *
	 * @param nodeID the ID of the node whose color to retrieve
	 * @param color the color for this node
	 */
	public void setNodeFillColor(String nodeID, Color color) {
		nodeFillColMap.put(nodeID, color);
	}

	/**
	 * Get the edge color for an edge in this frame
	 *
	 * @param edgeID the ID of the edge whose color to retrieve
	 * @return the color 
	 */
	public Color getEdgeColor(String edgeID) {
		if (edgeColMap.containsKey(edgeID))
			return edgeColMap.get(edgeID);
		return null;
	}

	/**
	 * Get the edge color for an edge in this frame
	 *
	 * @param edgeID the ID of the edge whose color to retrieve
	 * @return the color 
	 */
	public Color getEdgeStrokeColor(String edgeID) {
		if (edgeStrokeColMap.containsKey(edgeID))
			return edgeStrokeColMap.get(edgeID);
		return null;
	}

	/**
	 * Set the edge color for a edge in this frame
	 *
	 * @param edge the ID of the edge whose color to retrieve
	 * @param color the color for this edge
	 */
	public void setEdgeColor(String edgeID, Color color) {
		edgeColMap.put(edgeID, color);
	}

	/**
	 * Set the edge color for a edge in this frame
	 *
	 * @param edge the ID of the edge whose color to retrieve
	 * @param color the color for this edge
	 */
	public void setEdgeStrokeColor(String edgeID, Color color) {
		edgeStrokeColMap.put(edgeID, color);
	}

	/**
	 * Get the edge opacity for an edge in this frame
	 *
	 * @param edgeID the ID of the edge whose opacity to retrieve
	 * @return the opacity 
	 */
	public Integer getEdgeOpacity(String edgeID) {
		if (edgeOpacityMap.containsKey(edgeID))
			return edgeOpacityMap.get(edgeID);
		return new Integer(0);
	}

	/**
	 * Get the edge opacity for an edge in this frame
	 *
	 * @param edgeID the ID of the edge whose opacity to retrieve
	 * @return the opacity 
	 */
	public Integer getEdgeStrokeOpacity(String edgeID) {
		if (edgeStrokeOpacityMap.containsKey(edgeID))
			return edgeStrokeOpacityMap.get(edgeID);
		return new Integer(0);
	}

	/**
	 * Set the edge opacity for an edge in this frame
	 *
	 * @param edge the ID of the edge whose opacity to retrieve
	 * @param opacity the opacity for this edge
	 */
	public void setEdgeOpacity(String edgeID, Integer opacity) {
		edgeOpacityMap.put(edgeID, opacity);
	}

	/**
	 * Set the edge opacity for an edge in this frame
	 *
	 * @param edge the ID of the edge whose opacity to retrieve
	 * @param opacity the opacity for this edge
	 */
	public void setEdgeStrokeOpacity(String edgeID, Integer opacity) {
		edgeStrokeOpacityMap.put(edgeID, opacity);
	}

	/**
	 * Get the node opacity for a node in this frame
	 *
	 * @param nodeID the ID of the node whose opacity to retrieve
	 * @return the opacity 
	 */
	public Integer getNodeOpacity(String nodeID) {
		if (nodeOpacityMap.containsKey(nodeID))
			return nodeOpacityMap.get(nodeID);
		return new Integer(0);
	}

	/**
	 * Get the node opacity for a node in this frame
	 *
	 * @param nodeID the ID of the node whose opacity to retrieve
	 * @return the opacity 
	 */
	public Integer getNodeFillOpacity(String nodeID) {
		if (nodeFillOpacityMap.containsKey(nodeID))
			return nodeFillOpacityMap.get(nodeID);
		return new Integer(0);
	}

	/**
	 * Set the node opacity for an node in this frame
	 *
	 * @param node the ID of the node whose opacity to retrieve
	 * @param opacity the opacity for this node
	 */
	public void setNodeOpacity(String nodeID, Integer opacity) {
		nodeOpacityMap.put(nodeID, opacity);
	}

	/**
	 * Set the node opacity for an node in this frame
	 *
	 * @param node the ID of the node whose opacity to retrieve
	 * @param opacity the opacity for this node
	 */
	public void setNodeFillOpacity(String nodeID, Integer opacity) {
		nodeFillOpacityMap.put(nodeID, opacity);
	}

	/**
	 * Gets node size of an individual node
	 * 
	 * @param nodeID
	 * @return node size
	 */
	public double[] getNodeSize(String nodeID) {
		if (nodeSizeMap.containsKey(nodeID)) {
			double[] size = nodeSizeMap.get(nodeID);
			return size;
		}
		return null;
	}
	
	/**
	 * Sets node size of an individual node
	 * 
	 * @param nodeID
	 * @param size
	 */
	public void setNodeSize(String nodeID, double[] size){
		nodeSizeMap.put(nodeID, size);
	}
	
	/**
	 * 
	 * @param nodeID
	 * @return width of node border
	 */
	public double getNodeBorderWidth(String nodeID){
		if (nodeBorderWidthMap.containsKey(nodeID))
			return nodeBorderWidthMap.get(nodeID);
		return 0.0f;
	}
	
	/**
	  * 
	  * @param nodeID
	  * @param width
	  */
	public void setNodeBorderWidth(String nodeID, double width){
		nodeBorderWidthMap.put(nodeID, width);
	}

	/**
	 * 
	 * @param nodeID
	 * @return node border color
	 */
	public Color getNodeBorderColor(String nodeID) {
		if (nodeBorderColorMap.containsKey(nodeID))
			return nodeBorderColorMap.get(nodeID);
		return null;
	}

	/**
	 * 
	 * @param nodeID
	 * @param color node border color
	 */
	public void setNodeBorderColor(String nodeID, Color color) {
		nodeBorderColorMap.put(nodeID, color);
	}

	/**
	 * 
	 * @param nodeID
	 * @return node border transparency
	 */
	public Integer getNodeBorderTrans(String nodeID) {
		if (nodeBorderTransMap.containsKey(nodeID))
			return nodeBorderTransMap.get(nodeID);
		return null;
	}

	/**
	 * 
	 * @param nodeID
	 * @param trans node border transparency
	 */
	public void setNodeBorderTrans(String nodeID, Integer trans) {
		nodeBorderTransMap.put(nodeID, trans);
	}

	/**
	 * 
	 * @param nodeID
	 * @return node label font size
	 */
	public Integer getNodeLabelFontSize(String nodeID) {
		if (nodeLabelFontSizeMap.containsKey(nodeID))
			return nodeLabelFontSizeMap.get(nodeID);
		return null;
	}

	/**
	 * 
	 * @param nodeID
	 * @param size node label font size
	 */
	public void setNodeLabelFontSize(String nodeID, Integer size) {
		nodeLabelFontSizeMap.put(nodeID, size);
	}

	/**
	 * 
	 * @param nodeID
	 * @return node label transparency
	 */
	public Integer getNodeLabelTrans(String nodeID) {
		if (nodeLabelTransMap.containsKey(nodeID))
			return nodeLabelTransMap.get(nodeID);
		return null;
	}

	/**
	 * 
	 * @param nodeID
	 * @param trans node label transparency
	 */
	public void setNodeLabelTrans(String nodeID, Integer trans) {
		nodeLabelTransMap.put(nodeID, trans);
	}

	/**
	 *
	 * @param nodeID
	 * @return node label color
	 */
	public Color getNodeLabelColor(String nodeID) {
		if (nodeLabelColMap.containsKey(nodeID))
			return nodeLabelColMap.get(nodeID);
		return null;
	}

	/**
	  * 
	  * @param nodeID
	  * @param color
	  */
	public void setNodeLabelColor(String nodeID, Color color){
		nodeLabelColMap.put(nodeID, color);
	}
	
	/**
	 * 
	 * @param edgeID
	 * @return the edge width 
	 */
	public double getEdgeWidth(String edgeID){
		if (edgeWidthMap.containsKey(edgeID))
			return edgeWidthMap.get(edgeID);
		return 0.0f;
	}
	
	/**
	 * 
	 * @param edgeID
	 * @param width
	 */
	public void setEdgeWidth(String edgeID, double width){
		edgeWidthMap.put(edgeID, width);
	}

	/**
	 *
	 * @param edgeID
	 * @return edge label color
	 */
	public Color getEdgeLabelColor(String edgeID) {
		if (edgeLabelColMap.containsKey(edgeID))
			return edgeLabelColMap.get(edgeID);
		return null;
	}

	/**
	 *
	 * @param edgeID
	 * @return edge label size
	 */
	public Integer getEdgeLabelFontSize(String edgeID) {
		if (edgeLabelFontSizeMap.containsKey(edgeID))
			return edgeLabelFontSizeMap.get(edgeID);
		return null;
	}

	/**
	 *
	 * @param edgeID
	 * @return edge transparency
	 */
	public Integer getEdgeLabelTrans(String edgeID) {
		if (edgeLabelTransMap.containsKey(edgeID))
			return edgeLabelTransMap.get(edgeID);
		return null;
	}

	/**
	  * 
	  * @param edgeID
	  * @param color
	  */
	public void setEdgeLabelColor(String edgeID, Color color){
		edgeLabelColMap.put(edgeID, color);
	}

	/**
	  * 
	  * @param edgeID
	  * @param size font size
	  */
	public void setEdgeLabelFontSize(String edgeID, Integer size){
		edgeLabelFontSizeMap.put(edgeID, size);
	}

	/**
	  * 
	  * @param edgeID
	  * @param trans transparency
	  */
	public void setEdgeLabelTrans(String edgeID, Integer trans){
		edgeLabelTransMap.put(edgeID, trans);
	}

	/**
	 * Get the list of nodes in this frame
	 *
	 * @return the list of nodes
	 */
	public List<CyNode> getNodeList() {
		return nodeList;
	}

	/**
	 * Get the list of edges in this frame
	 *
	 * @return the list of edges
	 */
	public List<CyEdge> getEdgeList() {
		return edgeList;
	}

	/**
	 * Set the list of nodes in this frame
	 *
	 * @param nodeList the list of nodes
	 */
	public void setNodeList(List<CyNode>nodeList) {
		this.nodeList = nodeList;
	}

	/**
	 * Set the list of edges in this frame
	 *
	 * @param edgeList the list of edges
	 */
	public void setEdgeList(List<CyEdge>edgeList) {
		this.edgeList = edgeList;
	}

	/**
	 * Get the list of node views in this frame
	 *
	 * @return the list of node views
	 */
	public List<String> getNodeIdList() {
		return nodeIdList;
	}

	/**
	 * Get the list of edge views in this frame
	 *
	 * @return the list of edge views
	 */
	public List<String> getEdgeIdList() {
		return edgeIdList;
	}

	/**
	 * Set the list of node views in this frame
	 *
	 * @param nodeIdList the list of node views
	 */
	public void setNodeIdList(List<String>nodeIdList) {
		this.nodeIdList = nodeIdList;
	}

	/**
	 * Set the list of edge views in this frame
	 *
	 * @param edgeIdList the list of edges
	 */
	public void setEdgeIdList(List<String>edgeIdList) {
		this.edgeIdList = edgeIdList;
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
	public void writeImage(String fileName, final BooleanWrapper finished) throws IOException {
		display();
		CyNetworkView view = appManager.getCurrentNetworkView();
		
		NetworkViewTaskFactory exportImageTaskFactory = (NetworkViewTaskFactory) getService(NetworkViewTaskFactory.class, "(&(commandNamespace=view)(command=export))");
		if (exportImageTaskFactory != null && exportImageTaskFactory.isReady(view)) {
			TunableSetter tunableSetter = (TunableSetter) getService(TunableSetter.class);
			Map<String, Object> tunables = new HashMap<String, Object>();
			List<String> fileTypeList = new ArrayList<String>();
			fileTypeList.add(PNG);
			ListSingleSelection<String> fileType = new ListSingleSelection<String>(fileTypeList);
			fileType.setSelectedValue(PNG);
			tunables.put("options", fileType);
			tunables.put("OutputFile", new File(fileName));
			taskManager.execute(tunableSetter.createTaskIterator(
					exportImageTaskFactory.createTaskIterator(view), tunables),
					new TaskObserver() {
						
						public void taskFinished(ObservableTask arg0) {
							// TODO Auto-generated method stub
						}
						
						public void allFinished() {
							// TODO Auto-generated method stub
							finished.setValue(true);
						}
					});
		}
	}

	/**
	 * Get the center point for the frame
	 * 
	 * @return the center for this frame
	 */
	public Point2D getCenterPoint() {
		return this.centerPoint;
	}

	/**
	 * Set the center point of the frame
	 * 
	 * @param center point for a frame
	 */
	public void setCenterPoint(Point2D pnt) {
		this.centerPoint = pnt;
	}
	
	/**
	 * Returns the BundleContext of this CyFrame.
	 * @return the BundleContext of this CyFrame.
	 */
	public BundleContext getBundleContext() {
		return bundleContext;
	}
	// At some point, need to pull the information from nv
	// and map it to the new nv.
/*	private void addNodeView(CyNetworkView view, View<CyNode> nv, CyNode node) {
		view.addNodeView(node.getRootGraphIndex());
	} */

/*	private void addEdgeView(CyNetworkView view, View<CyEdge> ev, CyEdge edge) {
		view.addEdgeView(edge.getRootGraphIndex());
	} */

	private Object getService(Class<?> serviceClass) {
		return bundleContext.getService(bundleContext.getServiceReference(serviceClass.getName()));
	}
	
	private Object getService(Class<?> serviceClass, String filter) {
		try {
			ServiceReference[] services = bundleContext.getServiceReferences(serviceClass.getName(), filter);
			if (services != null && services.length > 0) {
				return bundleContext.getService(services[0]);
			}
		} catch (Exception ex) {
			// ignore
			// ex.printStackTrace();
		}
		return null;
	}
}

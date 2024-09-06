/*
 * File: CyAnimatorDialog.java
 * 
 * This contains all of the swing code for the GUI which is essentially just a thumbnail viewer
 * accompanied by stop, play, pause, forwards, and backwards buttons.  It also contains a slider
 * which is used to adjust the speed of the animations.  Each thumbnail is created by putting an
 * ImageIcon onto a JButton.
 * 
 */

package edu.ucsf.rbvi.CyAnimator.internal.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.IOException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;


public class CyAnimatorPanel extends JPanel implements CytoPanelComponent2 {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6650485843548244554L;
	private FramePanel framePanel;
	private TimelinePanel timeline;
	private JScrollPane timelineScroller;
	private ControlPanel controlPanel;

	private FrameManager frameManager;

	private final CyServiceRegistrar bc;
	private final String iconURL;
	private ImageIcon icon;
	private boolean docked = false;
	private CyAnimatorDialog parentDialog;

	public CyAnimatorPanel(final CyServiceRegistrar bundleContext, FrameManager frameManager, CyAnimatorDialog parentDialog,
	                       final String iconURL) {
		super();
		this.parentDialog = parentDialog;
		bc = bundleContext;
		this.iconURL = iconURL;

		this.frameManager = frameManager;

		try {
			Image img = ImageIO.read(new URL(iconURL));
			icon = new ImageIcon(img);
		} catch (IOException e) {
			icon = null;
		}


		initialize();
	}

	/**
	 * Create the control buttons, panels, and initialize the main JDialog.
	 */
	public void initialize(){
		BoxLayout mainbox = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(mainbox);	

		timeline = new TimelinePanel(frameManager, this);
		timelineScroller = new JScrollPane(timeline);
		timelineScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		// framePanel = new FramePanel(frameManager, timeline);

		controlPanel = new ControlPanel(frameManager, timeline, parentDialog);

		timeline.updateThumbnails();

		// mainPanel.add(framePanel);
		add(controlPanel);
		add(timelineScroller);

		return;

	}

	public void setSelected(boolean selected) {
		controlPanel.enableDelete(selected);
	}

	/**
	 * Clear all of our data structures and release our frame manager
	 */
	public void clear() {
		frameManager = null;
	}

	public void enableControlButtons(boolean enable) {
		controlPanel.enableButtons(enable);
	}

	public boolean loopAnimation() { return controlPanel.loopAnimation(); }
	public void stopAnimation() { controlPanel.stopAnimation(); }

	public void updateThumbnails() {
		timeline.updateThumbnails();
	}

	public void focusGained(FocusEvent e){
		timeline.updateThumbnails();
	}

	public void focusLost(FocusEvent e){

	}

	public void hideCytoPanel() {
    bc.unregisterService(this, CytoPanelComponent.class);
    // registered = false;
  }

  public String getIdentifier() {
    return "edu.ucsf.rbvi.CyAnimator.CyAnimator";
  }

  public Component getComponent() {
    return this;
  }

  public CytoPanelName getCytoPanelName() {
    return CytoPanelName.SOUTH;
  }

  public Icon getIcon() {
    return icon;
  }

  public String getTitle() {
    return "CyAnimator";
  }



}

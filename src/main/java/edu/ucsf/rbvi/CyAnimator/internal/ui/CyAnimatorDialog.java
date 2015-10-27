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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;

public class CyAnimatorDialog extends JDialog 
                              implements PropertyChangeListener, 
																	       FocusListener, WindowListener {


	/**
	 * 
	 */
	private static final long serialVersionUID = 6650485843548244554L;
	private FramePanel framePanel;
	private TimelinePanel timeline;
	private JScrollPane framePane;
	private JPanel controlPanel;

	private JPanel mainPanel;

	private FrameManager frameManager;

	private CyServiceRegistrar bc;

	public CyAnimatorDialog(CyServiceRegistrar bundleContext, CyNetwork network, JFrame frame){
		super(frame);

		this.setTitle("CyAnimator");
		bc = bundleContext;
		frameManager = FrameManager.getFrameManager(bc, network);

		addWindowListener(this);

		initialize();
		setPreferredSize(new Dimension(625,260));
		setSize(new Dimension(625,260));
		pack();
	}

	/**
	 * Create the control buttons, panels, and initialize the main JDialog.
	 */
	public void initialize(){
		mainPanel = new JPanel();
		mainPanel.addPropertyChangeListener(this);

		BoxLayout mainbox = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
		mainPanel.setLayout(mainbox);	

		timeline = new TimelinePanel(frameManager, this);
		JScrollPane timelineScroller = new JScrollPane(timeline);
		timelineScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		framePanel = new FramePanel(frameManager, timeline);

		controlPanel = new ControlPanel(frameManager, timeline);

		mainPanel.add(framePanel);
		mainPanel.add(timelineScroller);
		mainPanel.add(controlPanel);

		setContentPane(mainPanel);
	}

	public void setSelected(boolean selected) {
		framePanel.enableDelete(selected);
	}

	/**
	 * Clear all of our data structures and release our frame manager
	 */
	public void clear() {
		frameManager = null;
	}

	public void propertyChange ( PropertyChangeEvent e ) {
		if(e.getPropertyName().equals("ATTRIBUTES_CHANGED")){
			//initialize();
			setVisible(true);
		}
	}

	public void focusGained(FocusEvent e){
		timeline.updateThumbnails();
	}

	public void focusLost(FocusEvent e){

	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowClosing(WindowEvent e) {
		frameManager.stop();
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}


}

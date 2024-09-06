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


public class CyAnimatorDialog extends JDialog
	                            implements PropertyChangeListener, WindowListener {


	/**
	 * 
	 */
	private static final long serialVersionUID = 6650485843548244554L;
	private FramePanel framePanel;

	private FrameManager frameManager;

	private final CyServiceRegistrar bc;
	private CyAnimatorPanel mainPanel;
	private final String iconURL;
	private boolean docked = false;

	public CyAnimatorDialog(final CyServiceRegistrar bundleContext, 
	                        final CyNetwork network, final JFrame frame, final String iconURL) {
		super(frame);
		bc = bundleContext;
		this.iconURL = iconURL;

		setTitle("CyAnimator");
		frameManager = FrameManager.getFrameManager(bc, network);

		addWindowListener(this);

		mainPanel = new CyAnimatorPanel(bundleContext, frameManager, this, iconURL);
		setContentPane(mainPanel);
		setPreferredSize(new Dimension(800,200));
		setSize(new Dimension(800,200));
		pack();
	}

	public void setSelected(boolean selected) {
		mainPanel.setSelected(selected);
	}

	/**
	 * Clear all of our data structures and release our frame manager
	 */
	public void clear() {
		frameManager = null;
	}

	/**
	 * Dock ourselves into the table browser CytoPanel
	 */
	public void dock() {
		docked = true;
		setVisible(false);
		setContentPane(new JPanel());
    bc.registerService(mainPanel, CytoPanelComponent.class, new Properties());
	}

	/**
	 * Undock ourselves
	 */
	public void undock() {
		docked = false;
    bc.unregisterService(mainPanel, CytoPanelComponent.class);
		setContentPane(mainPanel);
		revalidate();
		mainPanel.revalidate();
		mainPanel.setVisible(true);
		System.out.println("mainPanel = "+mainPanel);
		pack();
		setVisible(true);
	}


	public void propertyChange ( PropertyChangeEvent e ) {
		if(e.getPropertyName().equals("ATTRIBUTES_CHANGED")){
			//initialize();
			setVisible(true);
		}
	}

	public void enableControlButtons(boolean enable) {
		mainPanel.enableControlButtons(enable);
	}

	public boolean loopAnimation() { return mainPanel.loopAnimation(); }
	public void stopAnimation() { mainPanel.stopAnimation(); }

	public void focusGained(FocusEvent e){
		mainPanel.updateThumbnails();
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

  public String getIdentifier() {
    return "edu.ucsf.rbvi.CyAnimator.CyAnimator";
  }

  public String getTitle() {
    return "CyAnimator";
  }



}

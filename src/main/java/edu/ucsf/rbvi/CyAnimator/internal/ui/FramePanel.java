/*
 * File: CyFrame.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
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

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.cytoscape.util.swing.IconManager;
import static org.cytoscape.util.swing.IconManager.ICON_PLUS;
import static org.cytoscape.util.swing.IconManager.ICON_TRASH_O;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

public class FramePanel extends JPanel {

	/**
	 * 
	 */
	private FrameManager frameManager;
	private TimelinePanel timeline;
	private IconManager iconManager;
	private JButton deleteButton;

	public FramePanel(FrameManager frameManager, TimelinePanel timeline) {
		super();

		this.frameManager = frameManager;
		this.timeline = timeline;
		this.iconManager = frameManager.getService(IconManager.class);
		initialize();
	}

	/**
	 * Create the control buttons, panels, and initialize the main JDialog.
	 */
	public void initialize(){
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		{
			JButton captureButton = new JButton(ICON_PLUS);
			captureButton.setFont(iconManager.getIconFont(14.0f));
			captureButton.setToolTipText("Add frame");
			captureButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					try {
						frameManager.addKeyFrame();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					timeline.updateThumbnails();
				}
			});
			captureButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
			add(captureButton);
			add(Box.createRigidArea(new Dimension(10,0)));
		}

		{
			deleteButton = new JButton(ICON_TRASH_O);
			deleteButton.setFont(iconManager.getIconFont(14.0f));
			deleteButton.setToolTipText("Delete selected frames");
			deleteButton.setEnabled(false);
			deleteButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					// Get a list of selected frames
					List<CyFrame> removeFrames = new ArrayList<>();
					int interpOffset = 0;
					for (CyFrame frame: frameManager.getKeyFrameList()) {
						// Delete the frame from our timeline
						FrameButton button = timeline.getButtonForFrame(frame);
						if (button != null && button.isSelected()) {
							removeFrames.add(frame);
							interpOffset += frame.getInterCount();
						} else {
							frame.setInterCount(frame.getInterCount()+interpOffset);
							interpOffset = 0;
						}
					}
					// Remove them
					for (CyFrame frame: removeFrames) {
						frameManager.deleteKeyFrame(frame);
					}
					timeline.updateThumbnails();
					timeline.selectionChanged();
				}
			});
			deleteButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
			add(deleteButton);
			add(Box.createHorizontalGlue());
		}
		
		{
			JButton clearButton = new JButton("Clear All Frames");
			clearButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e){
					// Get a list of selected frames
					// Remove them
					List<CyFrame> removeFrames = new ArrayList<>();
					for (CyFrame frame: frameManager.getKeyFrameList()) {
						// Delete the frame from our timeline
						removeFrames.add(frame);
					}

					for (CyFrame frame: removeFrames) {
						frameManager.deleteKeyFrame(frame);
					}

					timeline.updateThumbnails();
				}
			});
			clearButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
			add(clearButton);
		}
	}

	public void enableDelete(boolean enable) {
		deleteButton.setEnabled(enable);
	}
}

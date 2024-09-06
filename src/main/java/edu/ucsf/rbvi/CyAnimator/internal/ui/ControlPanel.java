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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.util.swing.IconManager;
import static org.cytoscape.util.swing.IconManager.ICON_CIRCLE;
import static org.cytoscape.util.swing.IconManager.ICON_PAUSE;
import static org.cytoscape.util.swing.IconManager.ICON_PLAY;
import static org.cytoscape.util.swing.IconManager.ICON_PLUS;
import static org.cytoscape.util.swing.IconManager.ICON_REPEAT;
import static org.cytoscape.util.swing.IconManager.ICON_STEP_FORWARD;
import static org.cytoscape.util.swing.IconManager.ICON_STEP_BACKWARD;
import static org.cytoscape.util.swing.IconManager.ICON_STOP;
import static org.cytoscape.util.swing.IconManager.ICON_TRASH_O;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.model.TimeBase;
import edu.ucsf.rbvi.CyAnimator.internal.model.VideoType;

public class ControlPanel extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6650485843548244554L;
	private JSlider speedSlider;
	private final FrameManager frameManager;
	private final TimelinePanel timeline;
	private RecordPanel recordPanel;
	private IconManager iconManager;
	private JButton playButton;
	private JButton stopButton;
	private JButton pauseButton;
	private JButton forwardButton;
	private JButton backwardButton;
	private JButton deleteButton;
	private JToggleButton repeatButton;
	private JButton recordButton;
	private boolean interpolationComplete = false;
	private final Logger logger;
	private CyAnimatorDialog parent;

	public ControlPanel(final FrameManager frameManager, final TimelinePanel timeline, final CyAnimatorDialog parent) {
		super();
		logger = Logger.getLogger(CyUserLog.NAME);
		this.frameManager = frameManager;
		this.timeline = timeline;

		this.iconManager = frameManager.getService(IconManager.class);

		recordPanel = new RecordPanel();

		initialize();
		if (frameManager.getKeyFrameList() != null && 
		    frameManager.getKeyFrameList().size() > 1)
			enableButtons(true);
		else
			enableButtons(false);

		this.parent = parent;
	}

	/**
	 * Create the control buttons, panels, and initialize the main JDialog.
	 */
	public void initialize(){
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

		playButton = new JButton(ICON_PLAY);
		playButton.setFont(iconManager.getIconFont(14.0f));
		playButton.setToolTipText("Show animation");
		playButton.addActionListener(this);
		playButton.setActionCommand("play");

		stopButton = new JButton(ICON_STOP);
		stopButton.setFont(iconManager.getIconFont(14.0f));
		stopButton.setToolTipText("Stop animation");
		stopButton.addActionListener(this);
		stopButton.setActionCommand("stop");
		stopButton.setEnabled(false);

		pauseButton = new JButton(ICON_PAUSE);
		pauseButton.setFont(iconManager.getIconFont(14.0f));
		pauseButton.setToolTipText("Pause animation");
		pauseButton.addActionListener(this);
		pauseButton.setActionCommand("pause");
		pauseButton.setEnabled(false);

		forwardButton = new JButton(ICON_STEP_FORWARD);
		forwardButton.setFont(iconManager.getIconFont(14.0f));
		forwardButton.setToolTipText("Step forward one frame");
		forwardButton.addActionListener(this);
		forwardButton.setActionCommand("step forward");

		backwardButton = new JButton(ICON_STEP_BACKWARD);
		backwardButton.setFont(iconManager.getIconFont(14.0f));
		backwardButton.setToolTipText("Step backward one frame");
		backwardButton.addActionListener(this);
		backwardButton.setActionCommand("step backward");

		repeatButton = new JToggleButton(ICON_REPEAT);
		repeatButton.setFont(iconManager.getIconFont(14.0f));
		repeatButton.setToolTipText("Repeat (loop) animation");
		repeatButton.setSelected(true);

		recordButton = new JButton(ICON_CIRCLE);
		recordButton.setFont(iconManager.getIconFont(14.0f));
		recordButton.setToolTipText("Record animation (make movie)");
		recordButton.setForeground(Color.RED);
		recordButton.addActionListener(this);
		recordButton.setActionCommand("record");

		JLabel sliderLabel = new JLabel("Animation Speed: ");
		sliderLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		speedSlider = new JSlider(1,60);
		speedSlider.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));

		speedSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting()) {

					//fps is frames per second
					int fps = source.getValue();
					frameManager.setFPS(fps);
					//fps = fps/60;
					if(frameManager.timer == null){ return; }
					// System.out.println("FPS: "+fps);
				
					//timer delay is set in milliseconds, so 1000/fps gives delay per frame
					frameManager.timer.setDelay(1000/fps);
				}
			}
		});

		JButton clearButton = new JButton("Clear");
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

		JButton dockButton = new JButton("Dock");

		dockButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				if (dockButton.getText() == "Dock") {
					dockButton.setText("Undock");
					parent.dock();
				} else {
					dockButton.setText("Dock");
					parent.undock();
				}
			}
		});

		BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
		setLayout(box);

		add(captureButton);
		add(Box.createRigidArea(new Dimension(10,0)));
		add(deleteButton);
		add(Box.createHorizontalGlue());
		add(Box.createRigidArea(new Dimension(10,0)));

		add(playButton);
		add(pauseButton);
		add(stopButton);
		add(backwardButton);
		add(forwardButton);
		add(repeatButton);
		add(recordButton);
		add(sliderLabel);
		add(speedSlider);

		add(Box.createHorizontalGlue());
		add(Box.createRigidArea(new Dimension(10,0)));
		add(clearButton);
		add(dockButton);
	}


	public void actionPerformed(ActionEvent e){

		String command = "";
		if (e != null)
			command = e.getActionCommand();

		if(command.equals("play")){

			interpolationComplete = false;

			final ProgressDialog dialog = new ProgressDialog(timeline, "Please wait", "Interpolating frames");
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dialog.setVisible(true);
					while (!interpolationComplete) {
						dialog.update(dialog.getGraphics());
						try {Thread.sleep(100);} catch (Exception e) {};
					}
					dialog.dispose();
					frameManager.play(timeline, loopAnimation());
				}
			});

			new Thread() {
				public void run() {
					frameManager.updateFrames();
					interpolationComplete = true;
				}
			}.start();

			stopButton.setEnabled(true);
			pauseButton.setEnabled(true);
			playButton.setEnabled(false);
			recordButton.setEnabled(false);
		}  else if(command.equals("stop")){
			stopAnimation();
		} else if(command.equals("pause")){
			frameManager.pause();
			recordButton.setEnabled(true);
			pauseButton.setEnabled(false);
			playButton.setEnabled(true);
		} else if(command.equals("step forward")){
			//move forward one frame in the animation
			frameManager.stepForward(timeline);
		} else if(command.equals("step backward")){
			//move backwards one frame in the animation
			frameManager.stepBackward(timeline);
		} else if(command.equals("record")){
			int result = JOptionPane.showConfirmDialog(this, 
											 recordPanel,
											 "Output Options",
											 JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return;

			VideoType choice = recordPanel.getOutputType();
			int resolution = recordPanel.getResolution();
			int fps = frameManager.getFPS(); // Get the current FPS so we can restore it after the record is done
			TimeBase frameCount = recordPanel.getFrameCount();
			frameManager.updateSettings(frameCount, choice, resolution);

			try {
				frameManager.recordAnimation(recordPanel.getFilePath());
			} catch (Exception excp) {
				logger.error("Record of animation failed",excp);
			}
			frameManager.setFPS(fps);
		}
	}

	public boolean loopAnimation() {
		return repeatButton.isSelected();
	}

	public void stopAnimation() {
		frameManager.stop();
		recordButton.setEnabled(true);
		stopButton.setEnabled(false);
		pauseButton.setEnabled(false);
		playButton.setEnabled(true);
	}

	public void enableButtons(boolean enable) {
		playButton.setEnabled(enable);
		forwardButton.setEnabled(enable);
		backwardButton.setEnabled(enable);
		recordButton.setEnabled(enable);
	}

	public void enableDelete(boolean enable) {
		deleteButton.setEnabled(enable);
	}
}

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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.model.Progress;
import edu.ucsf.rbvi.CyAnimator.internal.model.Scrubber;

public class TimelinePanel extends JPanel implements ComponentListener, Scrubber { 
	/**
	 * 
	 */
	private FrameManager frameManager;
	private CyAnimatorDialog parent;
	private int fps = 30; // Frames per second
	private int SCALEOFFSET = 100;
	private int MAJORTICK = 10;
	private int MINORTICK = 5;
	private int LABELOFFSET = 25;

	private int scrubberPosition = -1;

	List<CyFrame> keyFrameList;
	Map<CyFrame, FrameButton> buttonMap;
	int width = 600;

	public TimelinePanel(FrameManager frameManager, CyAnimatorDialog parentDialog) {
		super();
		this.frameManager = frameManager;
		this.parent = parentDialog;

		// This will get updated when we draw our timeline the first time
		setPreferredSize(new Dimension(width,140));

		addComponentListener(this);

		setLayout(null);

	}

	public void updateThumbnails() {
		removeAll();
		keyFrameList = frameManager.getKeyFrameList();
		if (keyFrameList.size() > 1)
			parent.enableControlButtons(true);
		else
			parent.enableControlButtons(false);

		buttonMap = new HashMap<>();

		int xOffset = 5;
		boolean first = true;
		for (CyFrame frame: keyFrameList) {
			if (first) {
				first = false;
			} else {
				xOffset = xOffset + frame.getInterCount()*5;
			}

			BufferedImage image = frame.getFrameImage();
			FrameButton button = new FrameButton(this, frame, new ImageIcon(image));
			Rectangle bounds = new Rectangle(xOffset, 90-image.getHeight(), image.getWidth()+4, image.getHeight()+4);
			button.setBounds(bounds);
			add(button);
			buttonMap.put(frame, button);
		}

		scrubberPosition = -1;

		updateWidth(xOffset);

		repaint();
	}

	public void updateWidth(int offset) {
		if ((offset+seconds2Pixels(2.0)) > width) {
			width = offset + seconds2Pixels(2.0); // Add 2 seconds
			setPreferredSize(new Dimension(width,140));
			parent.revalidate();
			parent.repaint();
			repaint();
		}
	}

	public void setWidth(int width) {
		this.width = width;
		parent.revalidate();
		parent.repaint();
		repaint();
	}

	public void selectionChanged() {
		keyFrameList = frameManager.getKeyFrameList();
		for (CyFrame frame: keyFrameList) {
			if (buttonMap.containsKey(frame) && buttonMap.get(frame).isSelected()) {
				parent.setSelected(true);
				return;
			}
		}
		parent.setSelected(false);
	}

	@Override
	public void frameDisplayed(int frameNumber) {
		scrubberPosition = frames2Pixels(frameNumber)+5;
		repaint();
		scrollRectToVisible(new Rectangle(scrubberPosition, SCALEOFFSET-2, 1, SCALEOFFSET-7));
		if (frameNumber == (frameManager.getFrameCount()-1) && !parent.loopAnimation()) {
			parent.stopAnimation();
		}
	}

	public void setScrubber(CyFrame frame) {
		// Get the button
		if (buttonMap.containsKey(frame)) {
			FrameButton button = buttonMap.get(frame);
			Rectangle bounds = button.getBounds();
			scrubberPosition = bounds.x;
			repaint();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2d.drawLine(5, SCALEOFFSET, width-5, SCALEOFFSET);
		g2d.drawString("Seconds", width/2-30, SCALEOFFSET+LABELOFFSET+20);

		// Draw major ticks every second and minor ticks every 1/2 second
		// 1 second = fps*5 units
		boolean major = true;
		int seconds = 0;
		for (int tick=5; tick <= width-5; tick = tick+fps*5/2) {
			if (major){
				g2d.drawLine(tick, SCALEOFFSET, tick, SCALEOFFSET+MAJORTICK);
				// TODO: use nicer fonts
				g2d.drawString(Integer.toString(seconds), tick-3, SCALEOFFSET+LABELOFFSET);
				major = false;
				seconds++;
			} else {
				g2d.drawLine(tick, SCALEOFFSET, tick, SCALEOFFSET+MINORTICK);
				major = true;
			}
		}

		paintChildren(g);

		if (scrubberPosition > 0) {
			g2d.drawLine(scrubberPosition, SCALEOFFSET-2, scrubberPosition, 5);
		}

	}

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentResized(ComponentEvent e) {
		// Get the new size
		setWidth(getWidth());
	}

	@Override
	public void componentShown(ComponentEvent e) {}

	public CyFrame getPreviousFrame(CyFrame frame) {
		int index = keyFrameList.indexOf(frame);
		if (index <= 0)
			return null;
		return keyFrameList.get(index-1);
	}

	public CyFrame getNextFrame(CyFrame frame) {
		int index = keyFrameList.indexOf(frame);
		if (index < 0 || index >= keyFrameList.size()-1)
			return null;
		return keyFrameList.get(index+1);
	}

	public FrameButton getButtonForFrame(CyFrame frame) {
		if (buttonMap.containsKey(frame))
			return buttonMap.get(frame);
		return null;
	}

	public void swapFrame(CyFrame frame1, CyFrame frame2) {
		frameManager.swapFrame(frame1, frame2);
		keyFrameList = frameManager.getKeyFrameList();
	}

	public void adjustFrames(CyFrame frame, int shift) {
		boolean start = false;
		int maxBounds = 0;
		for (CyFrame f: keyFrameList) {
			if (start) {
				FrameButton button = buttonMap.get(f);
				Rectangle bounds = button.getBounds();
				bounds.x = bounds.x+shift;
				maxBounds = bounds.x;
				button.setBounds(bounds);
			} else if (f.equals(frame)) {
				start = true;
			}
		}
		updateWidth(maxBounds);
	}

	public void adjustNext(CyFrame frame, int delta) {
		boolean start = false;
		for (CyFrame f: keyFrameList) {
			if (start) {
				f.setInterCount(f.getInterCount()-delta/5);
				return;
			} else if (f.equals(frame)) {
				start = true;
			}
		}
	}

	public void resetFrames() {
		frameManager.resetFrames();
	}

	private int seconds2Pixels(double seconds) {
		return (int)(seconds*fps*5);
	}

	private int frames2Pixels(int frames) {
		return frames*5;
	}

	private double pixels2Seconds(int pixels) {
		return (double)pixels/(fps*5);
	}
}

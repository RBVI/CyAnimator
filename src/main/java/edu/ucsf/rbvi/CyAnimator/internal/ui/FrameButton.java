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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.BevelBorder;


import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

public class FrameButton extends JLabel { 
	/**
	 * 
	 */
	private FrameManager frameManager;
	private TimelinePanel parentPanel;
	private int fps = 30; // Frames per second
	private boolean selected = false;
	private CyFrame frame;
	private Robot robot;
	private boolean ignoreMove = false;

	public FrameButton(TimelinePanel parent, CyFrame frame, ImageIcon icon) {
		super(icon);

		this.frame = frame;
		parentPanel = parent;

		Border inside = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border outside = BorderFactory.createEmptyBorder();
		Border border = BorderFactory.createCompoundBorder(outside, inside);
		setBorder(border);

		FrameMouseMotionListener listener = new FrameMouseMotionListener(parent, frame);
		addMouseMotionListener(listener);
		addMouseListener(listener);
		try {
			robot = new Robot();
		} catch (Exception e) {
			robot = null;
		}

	}

	public CyFrame getFrame() { return frame; }

	public boolean isSelected() { return selected; }

	public void setSelected(boolean sel) { 
		selected = sel; 
		Border outside;
		Border inside;
		if (selected) {
			inside = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
			outside = BorderFactory.createLineBorder(Color.YELLOW, 2);
		} else {
			inside = BorderFactory.createBevelBorder(BevelBorder.RAISED);
			outside = BorderFactory.createEmptyBorder();
		}
		Border border = BorderFactory.createCompoundBorder(outside, inside);
		setBorder(border);
		repaint();
		parentPanel.selectionChanged();
	}

	class FrameMouseMotionListener extends MouseAdapter implements ActionListener {
		TimelinePanel parent;
		CyFrame frame;
		int xLast = 0; // The last position of the mouse
		int xOffset = 0; // The offset in the button of the mouse press
		int deltaTotal = 0;
		boolean shiftAll = false;
		private final int clickInterval;
		MouseEvent lastEvent;
		Timer timer;
		CyFrame previousFrame;
		CyFrame nextFrame;

		public FrameMouseMotionListener(TimelinePanel parent, CyFrame frame) {
			this.parent = parent;
			this.frame = frame;
			if (Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") == null) {
				clickInterval = 200;
			} else {
				clickInterval = (Integer)Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
			}
			timer = new Timer(clickInterval, this);

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int clickCount = e.getClickCount();

			if (clickCount > 2) return;
			lastEvent = e;

			if (timer.isRunning()) {
				timer.stop();
				doubleClick( lastEvent );
			} else {
				timer.restart();
			}
		}

		public void actionPerformed(ActionEvent e) {
			timer.stop();
			singleClick( lastEvent );
		}

		void doubleClick(MouseEvent e) {
			frame.display();
			parent.setScrubber(frame);
		}

		void singleClick(MouseEvent e) {
			FrameButton button = (FrameButton)e.getSource();
			if (button.isSelected())
				button.setSelected(false);
			else
				button.setSelected(true);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (ignoreMove) return;
			int x = e.getXOnScreen();
			int y = e.getYOnScreen();
			FrameButton button = (FrameButton)e.getSource();

			int delta = x-xLast;
			deltaTotal += delta;
			xLast = x;
			Rectangle bounds = button.getBounds();
			bounds.x = bounds.x+delta;
			if (bounds.x < 0) bounds.x = 0; // Don't go past the beginning
			button.setBounds(bounds);
			if (shiftAll) {
				parent.adjustFrames(frame, delta);
			}

			if (deltaTotal < 0) {
				// See if we've gone past the previous frame
				FrameButton prev = parent.getButtonForFrame(previousFrame);
				if (prev != null) {
					if (bounds.x <= prev.getBounds().x) {
						// Need to swap frames
						parent.swapFrame(prev.getFrame(), frame);
						// Fix up everyone's interpolation count
						frame.setInterCount(prev.getFrame().getInterCount());
						prev.getFrame().setInterCount(1);
						deltaTotal = 0;

						// Get the new previous frame
						previousFrame = parent.getPreviousFrame(frame);
					}
				}
			} else {
				// See if we've gone past the next frame
				FrameButton next = parent.getButtonForFrame(nextFrame);
				if (next != null) {
					if (bounds.x > next.getBounds().x) {
						// Need to swap frames
						parent.swapFrame(next.getFrame(), frame);
						// Fix up everyone's interpolation count
						next.getFrame().setInterCount(next.getFrame().getInterCount()+frame.getInterCount());
						frame.setInterCount(1);
						deltaTotal = 5;
						// Get the new next frame
						nextFrame = parent.getNextFrame(frame);
					}
				}
			}

			parent.updateWidth(bounds.x);
			parent.scrollRectToVisible(bounds);

			if (robot != null) {
				//
				// Try to keep the mouse in the timeline window to allow the user better control 
				//
				Rectangle visibleRect = parent.getVisibleRect();
				// System.out.println("Timeline visible rect = "+visibleRect);
				Point parentScreenLocation = parent.getLocationOnScreen(); // Top-left corner
				// System.out.println("Timeline screen location = "+parent.getLocationOnScreen());

				// System.out.println("xOffset = "+xOffset);
				// Now warp the mouse, if we need to
				if (x > (parentScreenLocation.x+visibleRect.x+visibleRect.width-bounds.width+xOffset)) {
					// System.out.println("Mouse location = "+x+","+y);
					Point screenLocation = getLocationOnScreen(); // Top-left corner
					// System.out.println("Screen location = "+screenLocation);
					// System.out.println("Mouse location = "+x+","+y);
					xLast = screenLocation.x+xOffset;
					// System.out.println("Warping mouse to "+xLast);
					ignoreMove = true;
					robot.mouseMove(xLast, y);
					ignoreMove = false;
				} else if (x < (parentScreenLocation.x+visibleRect.x+xOffset)) {
					Point screenLocation = getLocationOnScreen(); // Top-left corner
					xLast = screenLocation.x+xOffset;
					ignoreMove = true;
					robot.mouseMove(xLast, y);
					ignoreMove = false;
				}
			}

		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			xLast = e.getXOnScreen();
			xOffset = xLast-getLocationOnScreen().x;
			// Check to see if we're moving everything to our right,
			// or just us
			if ((e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK)) == MouseEvent.SHIFT_DOWN_MASK) {
				shiftAll = true;
			}

			previousFrame = parent.getPreviousFrame(frame);
			nextFrame = parent.getNextFrame(frame);

			deltaTotal = 0;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int clickCount = e.getClickCount();
			if (clickCount > 0) {
				// mouseClicked(e);
				return;
			}

			int interCount = frame.getInterCount()*5+deltaTotal;
			frame.setInterCount(interCount/5);
			if (!shiftAll)
				parent.adjustNext(frame, deltaTotal);

			parent.updateThumbnails();
			parent.resetFrames();
			xLast = 0;
			deltaTotal = 0;
			shiftAll = false;
		}
	}
}

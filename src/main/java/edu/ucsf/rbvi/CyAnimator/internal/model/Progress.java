/*
 * File: Progress.java
 *
 * This interface provides a mechanism for the FrameManager to
 * inform any interested UI elements that a frame has been
 * processed.
 * 
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;

public interface Progress {
	public void progress(int frame);
	public void frames(int frameCount);
}

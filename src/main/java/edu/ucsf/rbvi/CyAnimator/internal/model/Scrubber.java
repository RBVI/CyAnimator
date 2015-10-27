/*
 * File: Scrubber.java
 *
 * This interface provides a mechanism for the FrameManager to
 * inform any interested UI elements that a frame has been
 * displayed.
 * 
 */


package edu.ucsf.rbvi.CyAnimator.internal.model;

public interface Scrubber {
	public void frameDisplayed(int frame);
}

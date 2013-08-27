package edu.ucsf.rbvi.CyAnimator.internal.model;

/**
 * Wrapper class around boolean
 * @author Allan Wu
 *
 */
public class BooleanWrapper {
	private boolean value;
	
	public BooleanWrapper(boolean b) {value = b;}
	
	public void setValue(boolean b) {value = b;}
	
	public boolean getValue() {return value;}
}

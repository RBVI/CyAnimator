package edu.ucsf.rbvi.CyAnimator.internal.model;  

/**
 * A wrapper class that allows Annotations to be
 * CyIdentifiables
 */
public enum TimeBase {
	PAL("25 (PAL)", 25, 1),
	NTSC("29.97 (NTSC)", 30000, 1001),
	THIRTY("30", 30, 1),
	SIXTY("60", 60, 1);

	private String label;
	private int timebase;
	private int duration;

	TimeBase(String label, int timebase, int duration) {
		this.label = label;
		this.timebase = timebase;
		this.duration = duration;
	}

	public String toString() { return label; }
	public int getTimeBase() { return timebase; }
	public int getFrameDuration() { return duration; }
}

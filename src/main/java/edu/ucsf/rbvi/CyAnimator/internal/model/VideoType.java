package edu.ucsf.rbvi.CyAnimator.internal.model;  

import java.util.ArrayList;
import java.util.List;

/**
 */
public enum VideoType {
	FRAMES("Frames", "", true),
	GIF("Animated GIF", "gif", true),
	MP4("MP4/H.264", "mp4", true),
	WEBM("WebM/VP8", "webm", false /* Doesn't work right (JCodec problem */),
	MOV("MOV/H.264", "mov", false /* Not sure it's usefule */);

	private String label;
	private String extension;
	private boolean supported;

	VideoType(String label, String ext, boolean supported) {
		this.label = label;
		this.extension = ext;
		this.supported = supported;
	}

	public String toString() { return label; }
	public String getExt() { return extension; }
	public boolean isSupported() { return supported; }

	public static VideoType[] supportedValues() { 
		List<VideoType> supportedList = new ArrayList<>();
		for (VideoType type: values()) {
			if (type.isSupported())
				supportedList.add(type);
		}
		return supportedList.toArray(new VideoType[supportedList.size()]);
	}
}

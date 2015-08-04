/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyAnnotation;
import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

public class ColorInterpolator implements FrameInterpolator {
	boolean includeAlpha;

	public ColorInterpolator(boolean includeAlpha){
		this.includeAlpha = includeAlpha;
	}

	public int passNumber() { return 1; }

	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, 
	                             CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int stop, CyFrame[] cyFrameArray){
		int framenum = (stop-start) - 1;	

		for(View<? extends CyIdentifiable> id: idList){
			Color colorOne = (Color) frameOne.getValue(id,property);
			Color colorTwo = (Color) frameTwo.getValue(id,property);
			if(colorOne != null || colorTwo != null) {
				// Handle missing (or appearing) nodes
				if (colorOne == null) {
					if (includeAlpha) 
						colorOne = new Color(colorTwo.getRed(), colorTwo.getBlue(), colorTwo.getGreen(), 0);
					else
						colorOne = colorTwo;
				} else if (colorTwo == null) {
					if (includeAlpha) 
						colorTwo = new Color(colorOne.getRed(), colorOne.getBlue(), colorOne.getGreen(), 0);
					else
						colorTwo = colorOne;
				}

				if (colorOne.equals(colorTwo)) {
					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].putValue(id, property, colorOne);
					}	
				} else {
					Color[] paints = interpolateColor(colorOne, colorTwo, framenum, includeAlpha);

					for(int k=1; k<framenum+1; k++){
						cyFrameArray[start+k].putValue(id, property, paints[k]);
					}
				}
			}
		}
		return cyFrameArray;
	}

	/**
	 * This method performs a generic color interpolation and is used by many of the interpolators
	 * to do their color interpolations.  It simply takes the absolute difference between the R, G, and B
	 * values from the two colors, divides the difference by the number of intervals which will
	 * be interpolated, and then eithers adds or subtracts the appropriate amount for each R, G, and B
	 * value and creates a new color which is placed in the color array.  The color array thus has colorOne
	 * as its first value and colorTwo as its last value with the interpolated colors filling the middle of
	 * the array.
	 * 
	 * @param colorOne is the color to be interpolated from
	 * @param colorTwo is the color to be interpolated to
	 * @param framenum is the number or frames which need to be interpolated for and thus the length of the color array
	 * @return the array of interpolated colors 
	 * 
	 */
	public static Color[] interpolateColor(Color colorOne, Color colorTwo, int framenum, boolean includeAlpha){
		Color[] paints = new Color[framenum+1];
		boolean useBezier = false;

		if (colorOne == null || colorTwo == null) {
			if (colorOne == null) {
				colorOne = new Color(colorTwo.getRed(), colorTwo.getGreen(), colorTwo.getBlue(), 0);
			} else {
				colorTwo = new Color(colorOne.getRed(), colorOne.getGreen(), colorOne.getBlue(), 0);
			}
			// Use Bezier interpolation for opacity -- it should face in slowly and fade out slowly
			useBezier = true;
		}

		float red1 = colorOne.getRed(); 
		float green1 = colorOne.getGreen(); 
		float blue1 = colorOne.getBlue();
		float alpha1 = colorOne.getAlpha();

		float red2 = colorTwo.getRed();
		float green2 = colorTwo.getGreen();
		float blue2 = colorTwo.getBlue();
		float alpha2 = colorTwo.getAlpha();

		//Set up the increment lengths for each RGB values
		float rIncLen = (Math.abs(red1 - red2))/(framenum+1);
		float gIncLen = (Math.abs(green1 - green2))/(framenum+1);
		float bIncLen = (Math.abs(blue1 - blue2))/(framenum+1);
		float aIncLen = (Math.abs(alpha1 - alpha2))/(framenum+1);

		//arrays which will hold the RGB values at each increment, these arrays are parallel to the Color[]
		float[] rArray = new float[framenum+2];
		float[] gArray = new float[framenum+2];
		float[] bArray = new float[framenum+2];
		float[] aArray = new float[framenum+2];

		rArray[0] = 0;
		gArray[0] = 0;
		bArray[0] = 0;
		aArray[0] = 0;

		/*
		 * Initialize the RGB arrays, start of the array contains the value from colorOne, 
		 * end of the arrays contain the value from colorTwo.
		 */
		rArray[1] = red1;// + rIncLen;
		rArray[framenum+1] = red2;
		gArray[1] = green1;// + gIncLen;
		gArray[framenum+1] = green2;
		bArray[1] = blue1 ;//+ bIncLen;
		bArray[framenum+1] = blue2;
		aArray[1] = alpha1 ;//+ aIncLen;
		aArray[framenum+1] = alpha2;

		if (includeAlpha && useBezier)
			aArray = bezier(alpha1, alpha2, framenum);
		
		//fill the middle of the RGB arrays
		for(int k=1; k<framenum+1; k++){

			//general strategy is if red1 is less than red2 increment, else decrement
			if(red1 < red2){	
				rArray[k+1] = rArray[k] + rIncLen;
			}else{
				if((rArray[k] - rIncLen) > 0){
					rArray[k+1] = rArray[k] - rIncLen;
				}
			}
			if(green1 < green2){	
				gArray[k+1] = gArray[k] + gIncLen;
			}else{
				if((gArray[k] - gIncLen) > 0){
					gArray[k+1] = gArray[k] - gIncLen;
				}	
			}
			if(blue1 < blue2){	
				bArray[k+1] = bArray[k] + bIncLen;
			}else{
				if((bArray[k] - bIncLen) > 0){
					bArray[k+1] = bArray[k] - bIncLen;
				}	
			}

			if (includeAlpha && !useBezier) {
				if(alpha1 < alpha2){	
					aArray[k+1] = aArray[k] + aIncLen;
				}else{
					if((aArray[k] - aIncLen) > 0){
						aArray[k+1] = aArray[k] - aIncLen;
					}	
				}
			}
			
			//create the new color and put it in the Color[]
			if (includeAlpha) {
				paints[k] = new Color((int) rArray[k+1], (int) gArray[k+1], (int) bArray[k+1], (int) aArray[k+1]);	
			} else {
				paints[k] = new Color((int) rArray[k+1], (int) gArray[k+1], (int) bArray[k+1]);	
			}
		}

		return paints;
	}

	/**
	 * This method does a bezier interpolation between alpha1 and alpha2.  It
	 * assumes that we're looking at a third point that's 3/4 of the way between
	 * alpha1 and alpha2 with 1/4 of the value.
	 */
	public static float[] bezier(float alpha1, float alpha2, int framenum) {

		float alpha3Y = Math.abs(alpha2-alpha1)*.25f;
		float alpha3X;
		float [] alphas = new float[framenum+2];
		Arrays.fill(alphas, -1.0f);
		alphas[0] = alpha1;
		alphas[framenum+1] = alpha2;
		if (alpha1 < alpha2) {
			alpha3X = .75f;
		} else {
			alpha3X = .25f;
		}

		float tincr = 1.0f/((framenum+1)*2);
		for (float t = tincr*2; t < 1.0; t = t + tincr) {
			// Calculate the x and y values
			float y = (1.0f-t)*(1.0f-t)*alpha1 + 2.0f*(1.0f-t)*t*alpha3Y + t*t*alpha2;
			float x = 2.0f*(1.0f-t)*t*alpha3X + t*t; // alpha1X = 0.0; alpha2X = 1.0;
			int index = (int)(x*(framenum+1));
			// System.out.println("t = "+t+" x = "+x+" index = "+index+" y = "+y);
			float alphav = alphas[index];
			if (alphav != -1.0f)
				alphas[index] = (y + alphav) / 2;
			else
				alphas[index] = y;
		}

		// Fill any missing values
		for (int i = 1; i < framenum+1; i++) {
			if (alphas[i] < 0.0f) {
				alphas[i] = (alphas[i-1]+alphas[i+1])/2;
			}
		}

		return alphas;
	}
}

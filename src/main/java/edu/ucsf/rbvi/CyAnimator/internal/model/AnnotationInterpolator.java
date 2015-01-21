/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model;

import static edu.ucsf.rbvi.CyAnimator.internal.model.Interpolator.interpolateColor;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

/**
 *
 * @author root
 */
public class AnnotationInterpolator {
    
}

class interpolateAnnotationsPosition implements FrameInterpolator {

        public interpolateAnnotationsPosition(){

        }

        /**
         * Performs the interpolation.
         *
         * @param idList is the list of ids of annotation present in either frame
         * @param frameOne is the frame to be interpolated from
         * @param frameTwo is the frame to be interpolated to
         * @param start is the starting position of the frame in the CyFrame array
         * @param end is the ending positiong of the interpolation in the CyFrame array
         * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
         * @return the array of CyFrames filled with interpolated node position data
         */
        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo,
                                     int start, int stop, CyFrame[] cyFrameArray){

                int framenum = stop - start;

                for (long annotationId : idList) {
                    //Get the annotation positions and set up the position interpolation
                    Point ptOne = frameOne.getAnnotationPos((int) annotationId);
                    Point ptTwo = frameTwo.getAnnotationPos((int) annotationId);
										// System.out.println("ptOne = "+ptOne+", ptTwo = "+ptTwo);
                    if (ptOne == null && ptTwo == null) {
                        continue;
                    }

                    // Handle missing (or appearing) annotation
                    if (ptOne == null || ptTwo == null) {
                        if (ptOne == null) {
                            ptOne = ptTwo;
                        } else {
                            ptTwo = ptOne;
                        }

                        for (int k = 1; k < framenum; k++) {
                            cyFrameArray[start + k].setAnnotationPos((int) annotationId, ptTwo);
                        }
                        continue;
                    }

                    double xIncrement = (ptTwo.getX() - ptOne.getX()) / (double)framenum;
                    double yIncrement = (ptTwo.getY() - ptOne.getY()) / (double)framenum;
										// System.out.println("xIncrement = "+xIncrement+", yIncrement = "+yIncrement);
                    for (int k = 1; k < framenum; k++) {
                        Point p = new Point();
												p.setLocation(ptOne.getX() + xIncrement*(double)k,
																			ptOne.getY() + yIncrement*(double)k);

                        //Do the position interpolation
                        // System.out.println("Frame: "+(start+k)+" pos: " + ptOne.getX() + " "+ ptOne.getY() +" new " + p.getX() +" " + p.getY());
                        cyFrameArray[start + k].setAnnotationPos((int) annotationId, p);
                    }

                }
                return cyFrameArray;
        }
}

class interpolateAnnotationsSize implements FrameInterpolator {

        public interpolateAnnotationsSize(){

        }

        /**
         * Performs the interpolation.
         *
         * @param idList is the list of ids of annotation present in either frame
         * @param frameOne is the frame to be interpolated from
         * @param frameTwo is the frame to be interpolated to
         * @param start is the starting position of the frame in the CyFrame array
         * @param end is the ending positiong of the interpolation in the CyFrame array
         * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
         * @return the array of CyFrames filled with interpolated node position data
         */
        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo,
                                     int start, int stop, CyFrame[] cyFrameArray){

                int framenum = (stop-start) - 1;

                for(long annotationId: idList){
                    double zoomOne = frameOne.getAnnotationZoom((int) annotationId);
                    double zoomTwo = frameTwo.getAnnotationZoom((int) annotationId);

                    if( zoomOne == zoomTwo){
                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationZoom((int) annotationId, zoomTwo);
                        }
                    }else{
                        double sizeInc = (zoomTwo - zoomOne) / (framenum + 1), sizeIncrease = sizeInc;

                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationZoom((int) annotationId, (zoomOne + sizeIncrease) );
                                sizeIncrease += sizeInc;
                        }
                    }
                }
                
                for(long annotationId: idList){
                    double fontSizeOne = frameOne.getAnnotationFontSize((int) annotationId);
                    double fontSizeTwo = frameTwo.getAnnotationFontSize((int) annotationId);

                    if( fontSizeOne == fontSizeTwo){
                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationFontSize((int) annotationId, fontSizeTwo);
                        }
                    }else{
                        double sizeInc = (fontSizeTwo - fontSizeOne) / (framenum + 1), sizeIncrease = sizeInc;

                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationFontSize((int) annotationId, (fontSizeOne + sizeIncrease) );
                                sizeIncrease += sizeInc;
                        }
                    }
                }
                
                for(long annotationId: idList){
                    double borderWidthOne = frameOne.getAnnotationBorderWidth((int) annotationId);
                    double borderWidthTwo = frameTwo.getAnnotationBorderWidth((int) annotationId);

                    if( borderWidthOne == borderWidthTwo){
                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationBorderWidth((int) annotationId, borderWidthTwo);
                        }
                    }else{
                        double sizeInc = (borderWidthTwo - borderWidthOne) / (framenum + 1), sizeIncrease = sizeInc;

                        for(int k=1; k<framenum + 1; k++){
                                cyFrameArray[start+k].setAnnotationBorderWidth((int) annotationId, (borderWidthOne + sizeIncrease) );
                                sizeIncrease += sizeInc;
                        }
                    }
                }
                
                return cyFrameArray;
        }
}

class interpolateAnnotationsColor implements FrameInterpolator {

        public interpolateAnnotationsColor(){

        }

        /**
         * Performs the interpolation.
         *
         * @param idList is the list of ids of annotation present in either frame
         * @param frameOne is the frame to be interpolated from
         * @param frameTwo is the frame to be interpolated to
         * @param start is the starting position of the frame in the CyFrame array
         * @param end is the ending positiong of the interpolation in the CyFrame array
         * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
         * @return the array of CyFrames filled with interpolated node position data
         */
        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo,
                                     int start, int stop, CyFrame[] cyFrameArray){

                int framenum = (stop-start) - 1;

                for(long annotationId: idList){
                    
                    Color colorFillOne = frameOne.getAnnotationFillColor((int) annotationId);
                    Color colorFillTwo = frameTwo.getAnnotationFillColor((int) annotationId);
                    Color colorBorderOne = frameOne.getAnnotationBorderColor((int) annotationId);
                    Color colorBorderTwo = frameTwo.getAnnotationBorderColor((int) annotationId);
                    Color colorTextOne = frameOne.getAnnotationTextColor((int) annotationId);
                    Color colorTextTwo = frameTwo.getAnnotationTextColor((int) annotationId);
                    
                    if (colorFillOne != null || colorFillTwo != null) {
                        if (colorFillOne == null) {
                            colorFillOne = colorFillTwo;
                        } else if (colorFillTwo == null) {
                            colorFillTwo = colorFillOne;
                        }

                        if (colorFillOne == colorFillTwo) {
                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationFillColor((int) annotationId, colorFillOne);
                            }
                        } else {
                            Color[] paints = interpolateColor(colorFillOne, colorFillTwo, framenum, false);

                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationFillColor((int) annotationId, paints[k]);
                            }
                        }
                    }
                    
                    if (colorBorderOne != null || colorBorderTwo != null) {
                        // Handle missing (or appearing) nodes
                        if (colorBorderOne == null) {
                            colorBorderOne = colorBorderTwo;
                        } else if (colorBorderTwo == null) {
                            colorBorderTwo = colorBorderOne;
                        }

                        if (colorBorderOne == colorBorderTwo) {
                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationBorderColor((int) annotationId, colorBorderOne);
                            }
                        } else {
                            Color[] paints = interpolateColor(colorBorderOne, colorBorderTwo, framenum, false);

                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationBorderColor((int) annotationId, paints[k]);
                            }
                        }
                    }
                    
                    if (colorTextOne != null || colorTextTwo != null) {
                        // Handle missing (or appearing) nodes
                        if (colorTextOne == null) {
                            colorTextOne = colorTextTwo;
                        } else if (colorTextTwo == null) {
                            colorTextTwo = colorTextOne;
                        }

                        if (colorTextOne == colorTextTwo) {
                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationTextColor((int) annotationId, colorTextOne);
                            }
                        } else {
                            Color[] paints = interpolateColor(colorTextOne, colorTextTwo, framenum, false);

                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setAnnotationTextColor((int) annotationId, paints[k]);
                            }
                        }
                    }
                }
                return cyFrameArray;
        }
}

class interpolateAnnotationsText implements FrameInterpolator {

        public interpolateAnnotationsText(){

        }

        /**
         * Performs the interpolation.
         *
         * @param idList is the list of ids of annotation present in either frame
         * @param frameOne is the frame to be interpolated from
         * @param frameTwo is the frame to be interpolated to
         * @param start is the starting position of the frame in the CyFrame array
         * @param end is the ending positiong of the interpolation in the CyFrame array
         * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
         * @return the array of CyFrames filled with interpolated node position data
         */
        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo,
                                     int start, int stop, CyFrame[] cyFrameArray){

                int framenum = (stop-start) - 1;

                for(long annotationId: idList){
                    
                    String textOne = frameOne.getAnnotationText((int) annotationId);
                    String textTwo = frameTwo.getAnnotationText((int) annotationId);
                    
                    if (textOne == textTwo) {
                        for (int k = 1; k < framenum + 1; k++) {
                            cyFrameArray[start + k].setAnnotationText((int) annotationId, textTwo);
                        }
                    } else {
                        // Find way to interpolate shapes

                        for (int k = 1; k < framenum / 2; k++) {
                            cyFrameArray[start + k].setAnnotationText((int) annotationId, textOne);
                        }
                        for (int k = framenum / 2; k < framenum + 1; k++) {
                            cyFrameArray[start + k].setAnnotationText((int) annotationId, textTwo);
                        }
                    }
                }
                return cyFrameArray;
        }
}

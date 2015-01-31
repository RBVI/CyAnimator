/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model;

import static edu.ucsf.rbvi.CyAnimator.internal.model.Interpolator.interpolateColor;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.LineType;

/**
 *
 * @author root
 */
public class EdgeInterpolator {
    
}

/**
* 
* Direct interpolates the edge arrow shape.
*
*/
class interpolateEdgeArrowShape implements FrameInterpolator {

       public interpolateEdgeArrowShape(){

       }

       public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                       int start, int stop, CyFrame[] cyFrameArray){

               int framenum = (stop-start) - 1;	

               for(long edgeid: idList){
                       // set source arrow shapes
                       ArrowShape sourceOne = frameOne.getEdgeSourceArrowShape(edgeid);
                       ArrowShape sourceTwo = frameTwo.getEdgeSourceArrowShape(edgeid);

                       // no checks implemented as there is no finite interpolation                                
                       for(int k=1; k<framenum/2; k++){
                               cyFrameArray[start+k].setEdgeSourceArrowShape(edgeid, sourceOne);
                       }
                       for(int k=framenum/2; k<framenum+1; k++){
                               cyFrameArray[start+k].setEdgeSourceArrowShape(edgeid, sourceTwo);
                       }
                       // similarly for target
                       ArrowShape targetOne = frameOne.getEdgeTargetArrowShape(edgeid);
                       ArrowShape targetTwo = frameTwo.getEdgeTargetArrowShape(edgeid);

                       for(int k=1; k<framenum/2; k++){
                               cyFrameArray[start+k].setEdgeTargetArrowShape(edgeid, targetOne);
                       }
                       for(int k=framenum/2; k<framenum+1; k++){
                               cyFrameArray[start+k].setEdgeTargetArrowShape(edgeid, targetTwo);
                       }
                       // interpolate linetype directly
                       LineType lineOne = frameOne.getEdgeLineType(edgeid);
                       LineType lineTwo = frameTwo.getEdgeLineType(edgeid);

                       for(int k=1; k<framenum/2; k++){
                               cyFrameArray[start+k].setEdgeLineType(edgeid, lineOne);
                       }
                       for(int k=framenum/2; k<framenum+1; k++){
                               cyFrameArray[start+k].setEdgeLineType(edgeid, lineTwo);
                       }
               }
               return cyFrameArray;
       }
}

/**
* Interpolates edgeColor using the interpolateColor() method.
*/
class interpolateEdgeColor implements FrameInterpolator {

       public interpolateEdgeColor(){

       }

       /**
        * Performs the interpolation.
        *  
        * @param idList is in this case a list of EdgeViews
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

               for(long edgeid: idList){

                       Color colorOne = frameOne.getEdgeColor(edgeid);
                       Color colorTwo = frameTwo.getEdgeColor(edgeid);
                       if(colorOne != null || colorTwo != null) {

                               // Handle missing (or appearing) nodes
                               if (colorOne == null) 
                                       colorOne = colorTwo;
                               else if (colorTwo == null)
                                       colorTwo = colorOne;

                               if (colorOne == colorTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setEdgeColor(edgeid, colorOne);
                                       }	
                               } else {
                                       Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setEdgeColor(edgeid, paints[k]);
                                       }
                               }
                       }

                       Color colorStrokeOne = frameOne.getEdgeStrokeColor(edgeid);
                       Color colorStrokeTwo = frameTwo.getEdgeStrokeColor(edgeid);
                       if(colorStrokeOne != null || colorStrokeTwo != null) {

                               // Handle missing (or appearing) nodes
                               if (colorStrokeOne == null) 
                                       colorStrokeOne = colorStrokeTwo;
                               else if (colorStrokeTwo == null)
                                       colorStrokeTwo = colorStrokeOne;

                               if (colorStrokeOne == colorStrokeTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setEdgeStrokeColor(edgeid, colorStrokeOne);
                                       }	
                               } else {
                                       Color[] paints = interpolateColor(colorStrokeOne, colorStrokeTwo, framenum, false);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setEdgeStrokeColor(edgeid, paints[k]);
                                       }
                               }
                       }
               }
               return cyFrameArray;
       }
}
		
	
/**
 * Linearly interpolates the edge opacity.
 */
class interpolateEdgeOpacity implements FrameInterpolator {
        public interpolateEdgeOpacity(){

        }

        /**
         * Performs the interpolation.
         *  
         * @param idList is in this case a list of EdgeViews
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

                for(long edgeid: idList){

                        //Get the node transparencies and set up the transparency interpolation
                        Double transOne;
                        Double transTwo;

                        if (frameOne.getEdgeOpacity(edgeid) == null) transOne = new Double(0);
                        else transOne = new Double(frameOne.getEdgeOpacity(edgeid));
                        if (frameTwo.getEdgeOpacity(edgeid) == null) transTwo = new Double(0);
                        else transTwo = new Double(frameTwo.getEdgeOpacity(edgeid));

                        if (transOne.intValue() == transTwo.intValue()) {
                                for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setEdgeOpacity(edgeid, transOne.intValue());
                                }
                        } else {
                                double transIncLength = ((double)(transTwo - transOne))/((double)(framenum + 1));
                                double[] transArray = new double[framenum+2];
                                transArray[1] = transOne + transIncLength;

                                for(int k=1; k<framenum+1; k++){
                                        transArray[k+1] = transArray[k] + transIncLength;
                                        cyFrameArray[start+k].setEdgeOpacity(edgeid, (int)transArray[k]);
                                }
                        }

                        //Get the node transparencies and set up the transparency interpolation
                        Double transStrokeOne = new Double(frameOne.getEdgeStrokeOpacity(edgeid));
                        Double transStrokeTwo = new Double(frameTwo.getEdgeStrokeOpacity(edgeid));

                        if (transStrokeOne.intValue() == transStrokeTwo.intValue()) {
                                for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setEdgeStrokeOpacity(edgeid, transStrokeOne.intValue());
                                }
                        } else {
                                double transIncLength = ((double)(transStrokeTwo - transStrokeOne))/((double)(framenum + 1));
                                double[] transArray = new double[framenum+2];
                                transArray[1] = transStrokeOne + transIncLength;

                                for(int k=1; k<framenum+1; k++){
                                        transArray[k+1] = transArray[k] + transIncLength;
                                        cyFrameArray[start+k].setEdgeStrokeOpacity(edgeid, (int)transArray[k]);
                                }
                        }

                }
                return cyFrameArray;
        }
}
	
/**
 * 
 * Linearly interpolates the edge line width.
 *
 */
class interpolateEdgeWidth implements FrameInterpolator {

        public interpolateEdgeWidth(){

        }
        /**
         * 
         * 
         */
        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                        int start, int stop, CyFrame[] cyFrameArray){

                int framenum = (stop-start) - 1;	

                for(long edgeid: idList){

                        //get the edge widths of the edge from each of the two frames
                        double widthOne = frameOne.getEdgeWidth(edgeid);
                        double widthTwo = frameTwo.getEdgeWidth(edgeid);

                        if (widthOne == widthTwo) {
                                for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setEdgeWidth(edgeid, widthOne);
                                }
                                continue;
                        }

                        double widthInclength = (widthTwo - widthOne)/(framenum + 1);
                        double[] widthArray = new double[framenum+2];
                        widthArray[1] = widthOne + widthInclength;

                        for(int k=1; k<framenum+1; k++){
                                widthArray[k+1] = widthArray[k] + widthInclength;
                                cyFrameArray[start+k].setEdgeWidth(edgeid, widthArray[k]);
                        }	

                }
                return cyFrameArray;
        }
}

/**
 * 
 * Linearly interpolates label size and color
 *
 */
class interpolateEdgeLabel implements FrameInterpolator {

        public interpolateEdgeLabel(){

        }

        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                                                 int start, int stop, CyFrame[] cyFrameArray){

                int framenum = (stop-start) - 1;	

                for(long edgeid: idList)
                {
                        String labelOne = frameOne.getEdgeLabel(edgeid);
                        String labelTwo = frameTwo.getEdgeLabel(edgeid);

                        Integer transOne = frameOne.getEdgeLabelTrans(edgeid);
                        Integer transTwo = frameTwo.getEdgeLabelTrans(edgeid);

                        if ( transOne == null) transOne = 0;
                        if ( transTwo == null) transTwo = 0;

                        if ( labelOne == labelTwo){
                            for (int k = 1; k < framenum + 1; k++) {
                                    cyFrameArray[start + k].setEdgeLabel(edgeid, labelTwo);
                                }
                            if ( transOne.equals(transTwo) ) {
                                for (int k = 1; k < framenum + 1; k++) {
                                    cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne);
                                }
                            } else {
                                int transInc = (transTwo - transOne) / (framenum + 1), transIncrease = transInc;
                                for (int k = 1; k < framenum + 1; k++) {
                                    cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne + transIncrease);
                                    transIncrease += transInc;
                                }
                            }
                        }else if (labelTwo != null){
                            int transInc = (0 - transOne) / (framenum / 2), transIncrease = transInc;
                            for (int k = 1; k < framenum / 2; k++) {
                                    cyFrameArray[start + k].setEdgeLabel(edgeid, labelOne);
                                    cyFrameArray[start + k].setEdgeLabelTrans(edgeid, transOne + transIncrease);
                                    transIncrease += transInc;
                                }
                            transInc = (transTwo - 0) / (framenum / 2);
                            transIncrease = transInc;
                            for (int k = framenum/2; k < framenum +1; k++) {
                                    cyFrameArray[start + k].setEdgeLabel(edgeid, labelTwo);
                                    cyFrameArray[start + k].setEdgeLabelTrans(edgeid, 0 + transIncrease);
                                    transIncrease += transInc;
                                }
                        }

                        Color colorOne = frameOne.getEdgeLabelColor(edgeid);
                        Color colorTwo = frameTwo.getEdgeLabelColor(edgeid);
                        if(colorOne != null || colorTwo != null) {
                                if (colorOne == colorTwo) {
                                        for(int k=1; k<framenum+1; k++){
                                                cyFrameArray[start+k].setEdgeLabelColor(edgeid, colorOne);
                                        }	
                                } else {
                                        Color[] paints = interpolateColor(colorOne, colorTwo, framenum, true);

                                        for(int k=1; k<framenum+1; k++){
                                                cyFrameArray[start+k].setEdgeLabelColor(edgeid, paints[k]);
                                        }	
                                }
                        }
                        Integer sizeOne = frameOne.getEdgeLabelFontSize(edgeid);
                        Integer sizeTwo = frameTwo.getEdgeLabelFontSize(edgeid);

                        if ( sizeOne == null ) sizeOne = 0;
                        if ( sizeTwo == null ) sizeTwo = 0;

                        if ( sizeOne.equals(sizeTwo) ) {
                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setEdgeLabelFontSize(edgeid, sizeOne);
                            }
                        } else {
                            double sizeInc = ((double) sizeTwo - (double) sizeOne) / ((double) framenum + 1), sizeIncrease = sizeInc;

                            for (int k = 1; k < framenum + 1; k++) {
                                cyFrameArray[start + k].setEdgeLabelFontSize(edgeid, sizeOne + (int) sizeIncrease);
                                sizeIncrease += sizeInc;
                            }
                        }

                        // updating fonts in frames
                        Font fontOne = frameOne.getEdgeLabelFont(edgeid);
                        Font fontTwo = frameTwo.getEdgeLabelFont(edgeid);

                        for (int k = 1; k < framenum/2; k++) {
                            cyFrameArray[start + k].setEdgeLabelFont(edgeid, fontOne);
                        }
                        for (int k = framenum/2; k < framenum + 1; k++) {
                            cyFrameArray[start + k].setEdgeLabelFont(edgeid, fontTwo);
                        }

                }	
                return cyFrameArray;
        }
}
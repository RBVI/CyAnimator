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
import org.cytoscape.view.presentation.property.values.NodeShape;

/**
 *
 * @author root
 */
public class NodeInterpolator {

	
}

/**
* Linearly interpolate node border color
* @author Allan Wu
*
*/
class interpolateNodeBorderColor implements FrameInterpolator {

       public interpolateNodeBorderColor() {}

       public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne,
                       CyFrame frameTwo, int start, int end, CyFrame[] cyFrameArray) {

               int framenum = (end-start) - 1;	

               for(long nodeid: idList)
               {
                       Color colorOne = frameOne.getNodeBorderColor(nodeid);
                       Color colorTwo = frameTwo.getNodeBorderColor(nodeid);
                       if(colorOne != null || colorTwo != null) {
                               if (colorOne == colorTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeBorderColor(nodeid, colorOne);
                                       }	
                               } else {
                                       Color[] paints = Interpolator.interpolateColor(colorOne, colorTwo, framenum, true);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeBorderColor(nodeid, paints[k]);
                                       }	
                               }
                       }
                       Integer transOne = frameOne.getNodeBorderTrans(nodeid);
                       Integer transTwo = frameTwo.getNodeBorderTrans(nodeid);

                       if ( transOne == null) transOne = 0;
                       if ( transTwo == null) transTwo = 0;


                       if (transOne.equals(transTwo)) {
                           for (int k = 1; k < framenum + 1; k++) {
                               cyFrameArray[start + k].setNodeBorderTrans(nodeid, transOne);
                           }
                       } else {
                           double sizeInc = ((double) transTwo - (double) transOne) / ((double) framenum), sizeIncrease = sizeInc;

                           for (int k = 1; k < framenum + 1; k++) {
                               cyFrameArray[start + k].setNodeBorderTrans(nodeid, transOne + (int) sizeIncrease);
                               sizeIncrease += sizeInc;
                           }
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
class interpolateNodeLabel implements FrameInterpolator {

       public interpolateNodeLabel() {

       }

       public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                                                int start, int stop, CyFrame[] cyFrameArray){

               int framenum = (stop-start) - 1;	

               for(long nodeid: idList)
               {
                       String labelOne = frameOne.getNodeLabel(nodeid);
                       String labelTwo = frameTwo.getNodeLabel(nodeid);

                       Integer transOne = frameOne.getNodeLabelTrans(nodeid);
                       Integer transTwo = frameTwo.getNodeLabelTrans(nodeid);

                       if ( transOne == null) transOne = 0;
                       if ( transTwo == null) transTwo = 0;

                       if (labelOne == labelTwo){
                           for(int k=1; k<framenum+1; k++){
                               cyFrameArray[start+k].setNodeLabel(nodeid, labelTwo);
                               }
                           if (transOne.equals(transTwo)) {
                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne);
                               }
                           } else {
                                   double sizeInc = ((double) transTwo - (double) transOne) / ((double) (framenum + 1)), sizeIncrease = sizeInc;

                                   for(int k=1; k<framenum+1; k++){
                                           cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne + (int) sizeIncrease);
                                           sizeIncrease += sizeInc;
                                   }
                           }
                       }else if (labelTwo != null){
                           double sizeInc = (0 - (double) transOne) / ((double) (framenum / 2)), sizeIncrease = sizeInc;
                           for(int k=1; k<framenum/2; k++){
                               cyFrameArray[start+k].setNodeLabel(nodeid, labelOne);
                               cyFrameArray[start+k].setNodeLabelTrans(nodeid, transOne + (int) sizeIncrease);
                               sizeIncrease += sizeInc;
                               }
                           sizeInc = ((double) transTwo - 0) / ((double) (framenum / 2));
                           sizeIncrease = sizeInc;
                           for(int k=framenum/2; k<framenum + 1; k++){
                               cyFrameArray[start+k].setNodeLabel(nodeid, labelTwo);
                               cyFrameArray[start+k].setNodeLabelTrans(nodeid, 0 + (int) sizeIncrease);
                               sizeIncrease += sizeInc;
                               }
                       }

                       Color colorOne = frameOne.getNodeLabelColor(nodeid);
                       Color colorTwo = frameTwo.getNodeLabelColor(nodeid);
                       if(colorOne != null || colorTwo != null) {
                               if (colorOne == colorTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeLabelColor(nodeid, colorOne);
                                       }	
                               } else {
                                       Color[] paints = Interpolator.interpolateColor(colorOne, colorTwo, framenum, true);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeLabelColor(nodeid, paints[k]);
                                       }	
                               }
                       }
                       Integer sizeOne = frameOne.getNodeLabelFontSize(nodeid);
                       Integer sizeTwo = frameTwo.getNodeLabelFontSize(nodeid);

                       if ( sizeOne == null) sizeOne = 0;
                       if ( sizeTwo == null) sizeTwo = 0;

                       if (sizeOne.equals(sizeTwo)) {
                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeLabelFontSize(nodeid, sizeOne);
                               }	
                       } else {
                               double sizeInc = ((double) sizeTwo - (double) sizeOne) / ((double) (framenum + 1)), sizeIncrease = sizeInc;

                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeLabelFontSize(nodeid, sizeOne + (int) sizeIncrease);
                                       sizeIncrease += sizeInc;
                               }	
                       }

                       Double widthOne = frameOne.getNodeLabelWidth(nodeid);
                       Double widthTwo = frameTwo.getNodeLabelWidth(nodeid);

                       if ( widthOne == null) widthOne = 0.0;
                       if ( widthTwo == null) widthTwo = 0.0;

                       if (widthOne.equals(widthTwo)) {
                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeLabelWidth(nodeid, widthTwo);
                               }	
                       } else {
                               double sizeInc = (widthTwo - widthOne) / (framenum + 1), sizeIncrease = sizeInc;

                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeLabelWidth(nodeid, widthOne + sizeIncrease);
                                       sizeIncrease += sizeInc;
                               }
                       }

                       Font fontOne = frameOne.getNodeLabelFont(nodeid);
                       Font fontTwo = frameTwo.getNodeLabelFont(nodeid);

                       for(int k=1; k<framenum/2; k++){
                               cyFrameArray[start+k].setNodeLabelFont(nodeid, fontOne);
                       }
                       for(int k=framenum/2; k<framenum+1; k++){
                               cyFrameArray[start+k].setNodeLabelFont(nodeid, fontTwo);
                       }
               }	
               return cyFrameArray;
       }
}

/**
* 
* Linearly interpolates the node border width.
*
*/
class interpolateNodeBorderWidth implements FrameInterpolator {

       public interpolateNodeBorderWidth(){

       }

       public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                       int start, int stop, CyFrame[] cyFrameArray){

               int framenum = stop-start;	

               for(long nodeid: idList){

                       //get the border widths of the node from each of the two frames
                       double widthOne = frameOne.getNodeBorderWidth(nodeid);
                       double widthTwo = frameTwo.getNodeBorderWidth(nodeid);


                       //if (widthOne == null) sizeOne = new Integer(1);
                       //if (widthTwo == null) sizeTwo = new Integer(1);


                       if (widthOne == widthTwo) {
                               for(int k=1; k<framenum; k++){
                                       cyFrameArray[start+k].setNodeBorderWidth(nodeid, widthOne);
                               }
                               continue;
                       }

                       double widthInclength = (widthTwo - widthOne)/framenum;
                       double[] widthArray = new double[framenum+1];
                       widthArray[1] = widthOne + widthInclength;

                       for(int k=1; k<framenum; k++){
                               widthArray[k+1] = widthArray[k] + widthInclength;
                               cyFrameArray[start+k].setNodeBorderWidth(nodeid, widthArray[k]);
                       }	

               }
               return cyFrameArray;
       }
}

/**
* Fills in the interpolated color values for NodeViews.  Works by using the inner
* interpolateColor() method.
* 
*/
class interpolateNodeColor implements FrameInterpolator {

       public interpolateNodeColor(){

       }


       /**
        * Performs the interpolation.
        *  
        * @param idList is in this case a list of NodeViews
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

               for(long nodeid: idList){

                       Color colorOne = frameOne.getNodeColor(nodeid);
                       Color colorTwo = frameTwo.getNodeColor(nodeid);
                       Color colorFillOne = frameOne.getNodeFillColor(nodeid);
                       Color colorFillTwo = frameTwo.getNodeFillColor(nodeid);
                       if(colorOne != null || colorTwo != null) {
                               // Handle missing (or appearing) nodes
                               if (colorOne == null) 
                                       colorOne = colorTwo;
                               else if (colorTwo == null)
                                       colorTwo = colorOne;

                               if (colorOne == colorTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeColor(nodeid, colorOne);
                                       }	
                               } else {
                                       Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeColor(nodeid, paints[k]);
                                       }	
                               }
                       }

                       if (colorFillOne != null || colorFillTwo != null) {
                               if (colorFillOne == null)
                                       colorFillOne = colorFillTwo;
                               else if (colorFillTwo == null)
                                       colorFillTwo = colorFillOne;

                               if (colorFillOne == colorFillTwo) {
                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeFillColor(nodeid, colorFillOne);
                                       }	
                               } else {
                                       Color[] paints = interpolateColor(colorFillOne, colorFillTwo, framenum, false);

                                       for(int k=1; k<framenum+1; k++){
                                               cyFrameArray[start+k].setNodeFillColor(nodeid, paints[k]);
                                       }	
                               }
                       }
               }	
               return cyFrameArray;
       }

}

/**
 * Interpolates node opacity by linearly incrementing or decrementing the opacity value. 
 * 
 */
class interpolateNodeOpacity implements FrameInterpolator {

        public interpolateNodeOpacity(){

        }


        /**
         * Performs the interpolation.
         *  
         * @param idList is in this case a list of NodeViews
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

                for(long nodeid: idList){

                        //Get the node transparencies and set up the transparency interpolation
                        Integer transOne = frameOne.getNodeOpacity(nodeid);
                        Integer transTwo = frameTwo.getNodeOpacity(nodeid);
                        Integer transFillOne = frameOne.getNodeFillOpacity(nodeid);
                        Integer transFillTwo = frameTwo.getNodeFillOpacity(nodeid);

                        if (transOne == null) transOne = new Integer(0);
                        if (transTwo == null) transTwo = new Integer(0);
                        if (transFillOne == null) transFillOne = new Integer(0);
                        if (transFillTwo == null) transFillTwo = new Integer(0);

                        if (transOne.intValue() == transTwo.intValue()) {
                                for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setNodeOpacity(nodeid, transOne);
                                }
                        } else {
                                int transIncLength = (transTwo - transOne)/framenum;
                                int[] transArray = new int[framenum+2];
                                transArray[1] = transOne + transIncLength;

                                for(int k=1; k<framenum+1; k++){
                                        transArray[k+1] = transArray[k] + transIncLength;
                                        cyFrameArray[start+k].setNodeOpacity(nodeid, transArray[k]);
                                }
                        }

                        if (transFillOne.intValue() == transFillTwo.intValue()) {
                                for(int k=1; k<framenum+1; k++){
                                        cyFrameArray[start+k].setNodeFillOpacity(nodeid, transFillOne);
                                }
                        } else {
                                float transIncLength = ((float)(transFillTwo - transFillOne))/((float)framenum);
                                float[] transArray = new float[framenum+2];
                                transArray[1] = transFillOne + transIncLength;

                                for(int k=1; k<framenum+1; k++){
                                        transArray[k+1] = transArray[k] + transIncLength;
                                        cyFrameArray[start+k].setNodeFillOpacity(nodeid, (int)transArray[k]);
                                }
                        }
                }
                return cyFrameArray;
        }
}
	
/**
 * 
 * Linearly interpolates both the height and width of a node simultaneously 
 * to achieve the affect of interpolating the size.
 *
 */
class interpolateNodeSize implements FrameInterpolator {

        public interpolateNodeSize(){

        }

        public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                        int start, int stop, CyFrame[] cyFrameArray){

                int framenum = stop-start;

                for(long nodeid: idList){

                        //Get the node sizes and set up the size interpolation
                        double[] sizeOne = frameOne.getNodeSize(nodeid);
                        double[] sizeTwo = frameTwo.getNodeSize(nodeid);

                        if (sizeOne == null && sizeTwo == null) {
                                continue;
                        }

                        if (sizeOne == null && sizeTwo != null) {
                                sizeOne = new double[2];
                                sizeOne[0] = sizeTwo[0];
                                sizeOne[1] = sizeTwo[1];
                        }

                        if (sizeOne != null && sizeTwo == null) {
                                sizeTwo = new double[2];
                                sizeTwo[0] = sizeOne[0];
                                sizeTwo[1] = sizeOne[1];
                        }


                        if (sizeOne[0] == sizeTwo[0] && sizeOne[1] == sizeTwo[1]) {
                                for(int k=1; k<framenum; k++){
                                        cyFrameArray[start+k].setNodeSize(nodeid, sizeOne);
                                }
                                continue;
                        }

                        double sizeIncXlength = (sizeTwo[0] - sizeOne[0])/framenum;
                        double sizeIncYlength = (sizeTwo[1] - sizeOne[1])/framenum;
                        double[] sizeXArray = new double[framenum+1];
                        double[] sizeYArray = new double[framenum+1];
                        sizeXArray[1] = sizeOne[0] + sizeIncXlength;
                        sizeYArray[1] = sizeOne[1] + sizeIncYlength;

                        for(int k=1; k<framenum; k++){
                                sizeXArray[k+1] = sizeXArray[k] + sizeIncXlength;
                                sizeYArray[k+1] = sizeYArray[k] + sizeIncYlength;
                                double[] temp = {sizeXArray[k], sizeYArray[k]};
                                cyFrameArray[start+k].setNodeSize(nodeid, temp);
                        }	

                }
                return cyFrameArray;
        }
}

/**
* Interpolates the node position, using the standard linear interpolation formula described
* at http://en.wikipedia.org/wiki/Linear_interpolation. It essentially just finds the absolute
* difference between the position of a node in frame one, and in frame two.  It then divides
* this distance by the number of frames which will be interpolated and increments or decrements
* from the node position in the first frame to the node position in the second.  The incrementing
* is done on the x values, which are then plugged into the interpolation formula to generate a y-value.
* 
*/

class interpolateNodePosition implements FrameInterpolator {

       public interpolateNodePosition(){

       }

       /**
        * Performs the interpolation.
        *  
        * @param idList is in this case a list of ID's
        * @param frameOne is the frame to be interpolated from
        * @param frameTwo is the frame to be interpolated to
        * @param start is the starting position of the frame in the CyFrame array
        * @param end is the ending positiong of the interpolation in the CyFrame array
        * @param cyFrameArray is the array of CyFrames which gets populated with the interpolated data
        * @return the array of CyFrames filled with interpolated node position data
        */
       public CyFrame[] interpolate(List<Long> idList, CyFrame frameOne, CyFrame frameTwo, 
                                    int start, int stop, CyFrame[] cyFrameArray){

               int framenum = stop-start;

               for(long nodeid: idList){
                       //Get the node positions and set up the position interpolation
                       double[] xyOne = frameOne.getNodePosition(nodeid);
                       double[] xyTwo = frameTwo.getNodePosition(nodeid);		
                       if(xyOne == null && xyTwo == null){ continue; }

                       // Handle missing (or appearing) nodes
                       if (xyOne == null || xyTwo == null) {
                               double[] xy = new double[3];
                               if (xyOne == null)
                                       xy = xyTwo;
                               else
                                       xy = xyOne;

                               for(int k=1; k<framenum; k++) {
                                       cyFrameArray[start+k].setNodePosition(nodeid, xy);
                               }
                               continue;
                       }

                       double incrementLength = (xyTwo[0] - xyOne[0])/framenum;
                       double[] xArray = new double[framenum+1];
                       xArray[1] = xyOne[0] + incrementLength;

                       for(int k=1; k<framenum; k++){

                               double[] xy = new double[3];
                               xy[0] = 0;
                               xy[1] = 0;
                               xy[2] = 0;

                               xArray[k+1] = xArray[k] + incrementLength;
                               xy[0] = xArray[k];

                               //Do the position interpolation
                               if((xyTwo[0] - xyOne[0]) == 0){
                                       xy[1] = xyOne[1];
                                       xy[2] = xyOne[2];
                               }else{

                                       xy[1] = xyOne[1] + ((xArray[k] - xyOne[0])*((xyTwo[1]-xyOne[1])/(xyTwo[0] - xyOne[0])));
                                       xy[2] = xyOne[2] + ((xArray[k] - xyOne[0])*((xyTwo[2]-xyOne[2])/(xyTwo[0] - xyOne[0])));
                               }

                               cyFrameArray[start+k].setNodePosition(nodeid, xy);
                       }

               }
               return cyFrameArray;
       }
}

/**
* Interpolate the shape.
* 
*/
class interpolateNodeShape implements FrameInterpolator {

       public interpolateNodeShape(){

       }

       /**
        * Performs the interpolation.
        *  
        * @param idList is in this case a list of NodeViews
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

               for(long nodeid: idList){

                       NodeShape shapeOne = frameOne.getNodeShape(nodeid);
                       NodeShape shapeTwo = frameTwo.getNodeShape(nodeid);

                       // Handle missing (or appearing) nodes
                       if (shapeOne == null) 
                               shapeOne = shapeTwo;
                       else if (shapeTwo == null)
                               shapeTwo = shapeOne;

                       if (shapeOne == shapeOne) {
                               for(int k=1; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeShape(nodeid, shapeTwo);
                               }	
                       } else {
                               // Find way to interpolate shapes

                               for(int k=1; k<framenum/2; k++){
                                           cyFrameArray[start+k].setNodeShape(nodeid, shapeOne);
                               }
                               for(int k=framenum/2; k<framenum+1; k++){
                                       cyFrameArray[start+k].setNodeShape(nodeid, shapeTwo);
                               }
                       }
               }
               return cyFrameArray;
       }

}
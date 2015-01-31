/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model;

import static edu.ucsf.rbvi.CyAnimator.internal.model.Interpolator.interpolateColor;
import java.awt.Color;
import java.util.List;

/**
 *
 * @author root
 */
public class NetworkInterpolator {
    
}

class interpolateNetworkTitle implements FrameInterpolator {

        public interpolateNetworkTitle(){

        }

        /**
         * Performs the interpolation.
         *
         * @param idList is not used in this case
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

                String titleOne = frameOne.getTitle();
                String titleTwo = frameTwo.getTitle();

                for(int k=1; k<framenum/2; k++){
                        cyFrameArray[start+k].setTitle(titleOne);
                }
                for(int k=framenum/2; k<framenum+1; k++){
                        cyFrameArray[start+k].setTitle(titleTwo);
                }
                return cyFrameArray;
        }
}
	
/**
 * Linearly interpolates the network zoom.
 * 
 */
class interpolateNetworkZoom implements FrameInterpolator {

        public interpolateNetworkZoom(){

        }

        /**
         * Performs the interpolation.
         *  
         * @param idList is not used in this case 
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

                double[] zoomValues = new double[framenum+1];
        //	zoomValues[0] = 0;
                zoomValues[0] = frameOne.getZoom();
                zoomValues[framenum] = frameTwo.getZoom();
                double zoomInc = Math.abs(frameOne.getZoom() - frameTwo.getZoom())/framenum;

                for(int k=1; k<framenum; k++){


                        if(frameOne.getZoom() < frameTwo.getZoom()){
                                zoomValues[k] = zoomValues[k-1] + zoomInc;
                        }else{
                                zoomValues[k] = zoomValues[k-1] - zoomInc;
                        }

                        cyFrameArray[start+k].setZoom(zoomValues[k]);
                }

                double sizeOne = frameOne.getNetworkSize();
                double sizeTwo = frameTwo.getNetworkSize();

                if( sizeOne == sizeTwo){
                    for(int k=1; k<framenum; k++)
                        cyFrameArray[start+k].setNetworkSize(sizeTwo);
                }else{
                    double sizeInc = (sizeTwo - sizeOne) / framenum, sizeIncrease = sizeInc;

                    for(int k=1; k<framenum; k++){
                            cyFrameArray[start+k].setNetworkSize(sizeOne + sizeIncrease);
                            sizeIncrease += sizeInc;
                    }
                }

                double widthOne = frameOne.getNetworkWidth();
                double widthTwo = frameTwo.getNetworkWidth();

                if( widthOne == widthTwo){
                    for(int k=1; k<framenum; k++)
                        cyFrameArray[start+k].setNetworkWidth(widthTwo);
                }else{
                    double sizeInc = (widthTwo - widthOne) / framenum, sizeIncrease = sizeInc;

                    for(int k=1; k<framenum; k++){
                            cyFrameArray[start+k].setNetworkWidth(widthOne + sizeIncrease);
                            sizeIncrease += sizeInc;
                    }
                }

                double heightOne = frameOne.getNetworkHeight();
                double heightTwo = frameTwo.getNetworkHeight();

                if( heightOne == heightTwo){
                    for(int k=1; k<framenum; k++)
                        cyFrameArray[start+k].setNetworkHeight(heightTwo);
                }else{
                    double sizeInc = (heightTwo - heightOne) / framenum, sizeIncrease = sizeInc;

                    for(int k=1; k<framenum; k++){
                            cyFrameArray[start+k].setNetworkHeight(heightOne + sizeIncrease);
                            sizeIncrease += sizeInc;
                    }
                }

                return cyFrameArray;
        }
}

class interpolateNetworkColor implements FrameInterpolator {

        public interpolateNetworkColor(){

        }

        /**
         * Performs the interpolation.
         *  
         * @param idList is not used in this case
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

                Color colorOne = (Color)frameOne.getBackgroundPaint();
                Color colorTwo = (Color)frameTwo.getBackgroundPaint();
                Color[] paints = interpolateColor(colorOne, colorTwo, framenum, false);

                for(int k=1; k<framenum+1; k++){
                        cyFrameArray[start+k].setBackgroundPaint(paints[k]);
                }
                return cyFrameArray;
        }
}

class interpolateNetworkCenter implements FrameInterpolator {

        public interpolateNetworkCenter(){}

        /**
         * Performs the interpolation.
         *  
         * @param idList is not used in this case
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

                double xone = frameOne.getCenterPoint().getX();
                double yone = frameOne.getCenterPoint().getY();
                double zone = frameOne.getCenterPoint().getZ();

                double xtwo = frameTwo.getCenterPoint().getX();
                double ytwo = frameTwo.getCenterPoint().getY();
                double ztwo = frameTwo.getCenterPoint().getZ();

                double incrementLength = (xtwo - xone)/framenum;
                double[] xArray = new double[framenum+1];
                xArray[0] = xone;

                for(int k=1; k<framenum; k++){

                        Point3D xy = new Point3D(0, 0, 0);

                        xArray[k] = xArray[k-1] + incrementLength;
                        //xy.setLocation(xArray[k], arg1)[0] = xArray[k];

                        //Do the position interpolation
                        if((xtwo - xone) == 0){
                                xy.setLocation(xArray[k], yone, zone);
                        }else{

                                double y = yone + ((xArray[k] - xone)*((ytwo-yone)/(xtwo -xone)));
                                double z = zone + ((xArray[k] - xone)*((ztwo-zone)/(xtwo -xone)));
                                xy.setLocation(xArray[k], y, z);
                        }

                        cyFrameArray[start+k].setCenterPoint(xy);
                }

                return cyFrameArray;
        }

}

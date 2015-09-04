/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.model.interpolators;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;

/*
 * This isn't a real crossfade implementation since there is really
 * no way to do that through the visual property interface.  We
 * actually just fadeout/fadein, which is why we need the appropriate
 * transparency property...
 */
public class ObjectPositionInterpolator implements FrameInterpolator {
	enum Anchor {
	  NW("Northwest", "NW"), 
		N("North", "N"), 
	  NE("Northeast","NE"), 
		W("West", "W"), 
	  C("Center", "C"), 
		E("East", "E"), 
	  SW("Southwest", "SW"), 
	  S("South", "S"), 
		SE("Southeast", "SE");
		
		private final String displayName;
		private final String shortName;

		private Anchor (String name, String shortName) {
			this.displayName = name;
			this.shortName = shortName;
		}

		public String toString() { return displayName; }
		public String shortName() { return shortName; }
	}



	public ObjectPositionInterpolator(){
	}

	public int passNumber() { return 1; }

	/*
	 * Interpolate size
	 */
	public CyFrame[] interpolate(Set<View<? extends CyIdentifiable>> idList, 
	                             CyFrame frameOne, CyFrame frameTwo, 
	                             VisualProperty<?> property, int start, int stop, CyFrame[] cyFrameArray){

		int framenum = stop-start;
		// System.out.println("Property: "+property);
		// System.out.println("Property hashCode is: "+property.hashCode());

		for(View<? extends CyIdentifiable> id: idList){
			Object valueOne = frameOne.getValue(id,property);
			Object valueTwo = frameTwo.getValue(id,property);
			// System.out.println("valueOne.toString = "+valueOne);
			// System.out.println("valueTwo.toString = "+valueTwo);

			// System.out.println("Current property "+property+" value is "+id.getVisualProperty(property));
			if (valueOne == null && valueTwo == null)
				continue;

			if (valueOne == null && valueTwo != null) {
				valueOne = valueTwo;
			} else if (valueOne != null && valueTwo == null) {
				valueTwo = valueOne;
			}

			// System.out.println("valueOne.string = "+((VisualProperty)property).toSerializableString(valueOne));
			// System.out.println("valueTwo.string = "+((VisualProperty)property).toSerializableString(valueTwo));

			// Format of the serialized strings is: targetAnchor,objectAnchor,justification,xOffset,yOffset
			// where the anchor strings are one of C,N,NE,E,SE,S,SW,W,NW and the justifcation
			// strings are one of C,L,R
			String str1 = ((VisualProperty)property).toSerializableString(valueOne);
			String str2 = ((VisualProperty)property).toSerializableString(valueTwo);
			ObjectPosition pos1 = new ObjectPosition(str1);
			ObjectPosition pos2 = new ObjectPosition(str2);

			// OK, here are the rules:
			//   1) Don't deal with justification changes
			//   2) Can't deal with changes in objectAnchor since we don't know the size of the object
			//   3) Can deal with changes in targetAnchor since we do know that size of the node, and
			//   4) Can deal with changes in x,y offsets
			// We populate the values by modifying the X and Y offsets to move from one position
			// to the other.  For simplicity, all locations are calculated from the node center and
			// object center;
			Point2D startPos = calculatePosition(id, pos1);
			Point2D endPos = calculatePosition(id, pos2);
			// System.out.println("startPos = "+startPos);
			// System.out.println("endPos = "+endPos);

			if (startPos == null || endPos == null) {
				for (int k=1; k< framenum; k++) {
					if (k < framenum/2)
						cyFrameArray[start+k].putValue(id, property, valueOne);
					else
						cyFrameArray[start+k].putValue(id, property, valueTwo);
				}
			} else {
				double x = startPos.getX();
				double y = startPos.getY();
				double endX = endPos.getX();
				double endY = endPos.getY();
				double xStep = (endX-x)/framenum;
				double yStep = (endY-y)/framenum;
				for (int k=1; k< framenum; k++) {
					String newPosition = "C,C,c,"+x+","+y;
					Object newValue = ((VisualProperty)property).parseSerializableString(newPosition);
					cyFrameArray[start+k].putValue(id, property, newValue);
					x = x+xStep;
					y = y+yStep;
				}
			}
		}
		return cyFrameArray;
	}

	private Point2D calculatePosition(View<? extends CyIdentifiable> nodeView, ObjectPosition position) {
		Double width = nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
		Double height = nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);

		// Start with the simple case
		if (position.getTargetAnchor().equals(Anchor.C) &&
				position.getObjectAnchor().equals(Anchor.C)) {
			return new Point2D.Double(position.getXOffset(), position.getYOffset());
		}

		return offsetFromCenter(position, width, height);
	}

	private Point2D offsetFromCenter(ObjectPosition position, double width, double height) {
		if (!position.getObjectAnchor().equals(Anchor.C))
			return null; // Can't deal with changes in object anchor

		double xOff = position.getXOffset();
		double yOff = position.getYOffset();
		switch (position.getTargetAnchor()) {
			case C:
				break;
			case N:
				yOff = yOff-height/2.0;
				break;
			case NE:
				yOff = yOff-height/2.0;
				xOff = xOff+width/2.0;
				break;
			case E:
				xOff = xOff+width/2.0;
				break;
			case SE:
				yOff = yOff+height/2.0;
				xOff = xOff+width/2.0;
				break;
			case S:
				yOff = yOff+height/2.0;
				break;
			case SW:
				yOff = yOff+height/2.0;
				xOff = xOff-width/2.0;
				break;
			case W:
				xOff = xOff-width/2.0;
				break;
			case NW:
				yOff = yOff-height/2.0;
				xOff = xOff-width/2.0;
				break;
		}
		return new Point2D.Double(xOff, yOff);
	}

	class ObjectPosition {
		Anchor targetAnchor;
		Anchor objectAnchor;
		double xOffset;
		double yOffset;
		String just;

		public ObjectPosition(String sString) {
			String[] tokens = sString.split(",");
			targetAnchor = getAnchorValue(tokens[0]);
			objectAnchor = getAnchorValue(tokens[1]);
			just = tokens[2];
			xOffset = Double.parseDouble(tokens[3]);
			yOffset = Double.parseDouble(tokens[4]);
		}

		public Anchor getTargetAnchor() { return targetAnchor; }
		public Anchor getObjectAnchor() { return objectAnchor; }
		public double getXOffset() { return xOffset; }
		public double getYOffset() { return yOffset; }

		private Anchor getAnchorValue(String str) {
			for (Anchor a: Anchor.values()) {
				if (a.shortName().equalsIgnoreCase(str)) {
					return a;
				}
			}
			return null;
		}

		String getString() {
			return ""+targetAnchor+","+objectAnchor+","+just+","+xOffset+","+yOffset;
		}
	}
}

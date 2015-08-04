package edu.ucsf.rbvi.CyAnimator.internal.model;  

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.SUIDFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.BoundedTextAnnotation;
import org.cytoscape.view.presentation.annotations.ImageAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

/**
 * A wrapper class that allows Annotations to be
 * CyIdentifiables
 */
// class CyAnnotationView<CyAnnotation extends CyIdentifiable> implements View<CyAnnotation> {
class CyAnnotationView implements View<CyAnnotation> {
	final CyAnnotation cyAnnotation;
	final Annotation annotation;
	final long suid;
	boolean visible = true;

	Paint savedTextColor = null;
	Paint savedFillColor = null;
	Paint savedBorderColor = null;
	Paint savedLineColor = null;
	Paint savedArrowTargetColor = null;
	Paint savedArrowSourceColor = null;
	Float savedImageOpacity = null;

	static public List<CyAnnotationView> wrapViews(List<Annotation> annotations) {
		List<CyAnnotationView> views = new ArrayList<>();
		if (annotations != null && annotations.size() > 0) {
			for (Annotation a: annotations) {
				CyAnnotation cyA = new CyAnnotationImpl(a);
				CyAnnotationView cyAView = new CyAnnotationView(cyA);
				views.add(cyAView);
			}
		}
		return views;
	}

	static public CyAnnotationView getAnnotationView(Annotation ann, Set<CyAnnotationView> viewList) {
		for (CyAnnotationView view: viewList) {
			if (view.getModel().getAnnotation().equals(ann))
				return view;
		}
		return null;
	}

	public CyAnnotationView(final CyAnnotation cyAnnotation) {
		this.cyAnnotation = cyAnnotation;
		this.annotation = cyAnnotation.getAnnotation();

		suid = SUIDFactory.getNextSUID();
	}

	public CyAnnotationView(final Annotation annotation) {
		this.annotation = annotation;
		this.cyAnnotation = new CyAnnotationImpl(annotation);

		suid = SUIDFactory.getNextSUID();
	}

	@Override
	public void clearValueLock(VisualProperty<?> vp) {}

	@Override
	public void clearVisualProperties() {}

	@Override
	public CyAnnotation getModel() { return cyAnnotation; }

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getVisualProperty(VisualProperty<T> vp) {
		// Global properties
		if (vp.equals(AnnotationLexicon.ANNOTATION_X_LOCATION)) {
			return (T)(new Double(annotation.getArgMap().get(Annotation.X)));
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_Y_LOCATION)) {
			return (T)(new Double(annotation.getArgMap().get(Annotation.Y)));
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_ZOOM)) {
			return (T)(new Double(annotation.getArgMap().get(Annotation.ZOOM)));
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_CANVAS)) {
			return (T)annotation.getArgMap().get(Annotation.CANVAS);
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
			// Hack!
			return (T)(new Boolean(visible));
		}

		if (BoundedTextAnnotation.class.isAssignableFrom(annotation.getClass())) {
			BoundedTextAnnotation ta = (BoundedTextAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_SIZE)) {
				return (T)(new Double(ta.getFontSize()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_COLOR)) {
				return (T)ta.getTextColor();
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_STYLE)) {
				return (T)(new Integer(ta.getFontStyle()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_FAMILY)) {
				return (T)ta.getFontFamily();
			}
		}

		if (ShapeAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ShapeAnnotation sa = (ShapeAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_HEIGHT)) {
				return (T)(new Double(sa.getArgMap().get(ShapeAnnotation.HEIGHT)));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_WIDTH)) {
				return (T)(new Double(sa.getArgMap().get(ShapeAnnotation.WIDTH)));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_COLOR)) {
				return (T)(sa.getFillColor());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_OPACITY)) {
				return (T)(new Double(sa.getFillOpacity()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_COLOR)) {
				return (T)(sa.getBorderColor());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_WIDTH)) {
				return (T)(new Double(sa.getBorderWidth()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_SHAPE)) {
				return (T)sa.getShapeType();
			/*
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_OPACITY)) {
				return (T)(new Double(sa.getBorderWidth()));
			*/
			}
		}

		if (TextAnnotation.class.isAssignableFrom(annotation.getClass())) {
			TextAnnotation ta = (TextAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_SIZE)) {
				return (T)(new Double(ta.getFontSize()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_COLOR)) {
				return (T)ta.getTextColor();
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_STYLE)) {
				return (T)(new Integer(ta.getFontStyle()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_FAMILY)) {
				return (T)ta.getFontFamily();
			}
		}

		if (ImageAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ImageAnnotation ia = (ImageAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_URL)) {
				return (T)ia.getImageURL().toString();
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_CONTRAST)) {
				return (T)(new Integer(ia.getImageContrast()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_BRIGHTNESS)) {
				return (T)(new Integer(ia.getImageBrightness()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_OPACITY)) {
				return (T)(new Double(ia.getImageOpacity()));
			}
		}

		if (ArrowAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ArrowAnnotation aa = (ArrowAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_ANCHOR)) {
				return (T)(aa.getAnchorType(ArrowAnnotation.ArrowEnd.SOURCE).toString());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_TYPE)) {
				return (T)(aa.getArrowType(ArrowAnnotation.ArrowEnd.SOURCE));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_COLOR)) {
				return (T)(aa.getArrowColor(ArrowAnnotation.ArrowEnd.SOURCE));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_SIZE)) {
				return (T)(new Double(aa.getArrowSize(ArrowAnnotation.ArrowEnd.SOURCE)));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_WIDTH)) {
				return (T)(new Double(aa.getLineWidth()));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_COLOR)) {
				return (T)aa.getLineColor();
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_ANCHOR)) {
				return (T)(aa.getAnchorType(ArrowAnnotation.ArrowEnd.TARGET).toString());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_TYPE)) {
				return (T)(aa.getArrowType(ArrowAnnotation.ArrowEnd.TARGET));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_COLOR)) {
				return (T)(aa.getArrowColor(ArrowAnnotation.ArrowEnd.TARGET));
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_SIZE)) {
				return (T)(new Double(aa.getArrowSize(ArrowAnnotation.ArrowEnd.TARGET)));
			}
		}
		return null;
	}

	@Override
	public boolean isDirectlyLocked(VisualProperty<?> vp) {return false;}

	@Override
	public boolean isSet(VisualProperty<?> vp) {
		return false;
	}

	@Override
	public boolean isValueLocked(VisualProperty<?> vp) {return false;}

	@Override
	public <T, V extends T> void setLockedValue(VisualProperty<? extends T> vp, V value) {}

	@Override
	public <T, V extends T> void setVisualProperty(VisualProperty<? extends T> vp, V value) {
		Color transparent = new Color(0,0,0,0);
		// Global properties
		if (vp.equals(AnnotationLexicon.ANNOTATION_X_LOCATION)) {
			double y = new Double(annotation.getArgMap().get(Annotation.Y));
			double x = ((Double)value).doubleValue();
			Point2D p = new Point2D.Double(x,y);
			annotation.moveAnnotation(p);
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_Y_LOCATION)) {
			double x = new Double(annotation.getArgMap().get(Annotation.X));
			double y = ((Double)value).doubleValue();
			Point2D p = new Point2D.Double(x,y);
			annotation.moveAnnotation(p);
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_ZOOM)) {
			annotation.setZoom(((Double)value).doubleValue());
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_CANVAS)) {
			annotation.setCanvas((String)value);
		} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
			// Hack!
			Boolean v = (Boolean)value;
			// Remove from canvas?
			if (v) {
				annotation.getArgMap().put("Visible", "true");
				visible = true;
			} else {
				annotation.getArgMap().put("Visible", "false");
				visible = false;
			}
		}

		if (BoundedTextAnnotation.class.isAssignableFrom(annotation.getClass())) {
			BoundedTextAnnotation bta = (BoundedTextAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_SIZE)) {
				bta.setFontSize((Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_COLOR)) {
				bta.setTextColor((Color)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_STYLE)) {
				bta.setFontStyle((Integer)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_FAMILY)) {
				bta.setFontFamily((String)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
				if (!visible) {
					savedTextColor = bta.getTextColor();
					bta.setTextColor(transparent);
				} else if (savedTextColor != null) {
					bta.setTextColor((Color)savedTextColor);
					savedTextColor = null;
				}
			}
		}

		if (ShapeAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ShapeAnnotation sa = (ShapeAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_HEIGHT)) {
				double width = new Double(sa.getArgMap().get(ShapeAnnotation.WIDTH));
				double height = ((Double)value).doubleValue();
				sa.setSize(width, height);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_WIDTH)) {
				double height = new Double(sa.getArgMap().get(ShapeAnnotation.HEIGHT));
				double width = ((Double)value).doubleValue();
				sa.setSize(width, height);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_COLOR)) {
				sa.setFillColor((Paint)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_OPACITY)) {
				sa.setFillOpacity(((Number)value).doubleValue());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_COLOR)) {
				sa.setBorderColor((Paint)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_WIDTH)) {
				sa.setBorderWidth((Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_SHAPE)) {
				sa.setShapeType((String)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
				if	(!visible) {
					savedFillColor = sa.getFillColor();
					savedBorderColor = sa.getBorderColor();
					sa.setFillColor(transparent);
					sa.setBorderColor(transparent);
				} else if (savedFillColor != null) {
					sa.setFillColor(savedFillColor);
					sa.setBorderColor(savedBorderColor);
					savedFillColor = null;
					savedBorderColor = null;
				}

			/*
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_BORDER_OPACITY)) {
				return (T)(new Double(sa.getBorderWidth()));
			*/
			}
		}

		if (TextAnnotation.class.isAssignableFrom(annotation.getClass())) {
			TextAnnotation ta = (TextAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_SIZE)) {
				ta.setFontSize((Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_COLOR)) {
				ta.setTextColor((Color)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_STYLE)) {
				ta.setFontStyle((Integer)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_FONT_FAMILY)) {
				ta.setFontFamily((String)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
				if (!visible) {
					savedTextColor = ta.getTextColor();
					ta.setTextColor(transparent);
				} else if (savedTextColor != null) {
					ta.setTextColor((Color)savedTextColor);
					savedTextColor = null;
				}
			}
		}

		if (ImageAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ImageAnnotation ia = (ImageAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_URL)) {
				try {
					ia.setImage(new URL((String)value));
				} catch (MalformedURLException e) {}
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_CONTRAST)) {
				ia.setImageContrast((Integer)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_BRIGHTNESS)) {
				ia.setImageBrightness((Integer)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_IMAGE_OPACITY)) {
				ia.setImageOpacity(((Double)value).floatValue());
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
				if (!visible) {
					savedImageOpacity = ia.getImageOpacity();
					ia.setImageOpacity(0.0f);
				} else if (savedImageOpacity != null) {
					ia.setImageOpacity(savedImageOpacity);
					savedImageOpacity = null;
				}
			}
		}

		if (ArrowAnnotation.class.isAssignableFrom(annotation.getClass())) {
			ArrowAnnotation aa = (ArrowAnnotation) annotation;
			if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_ANCHOR)) {
				ArrowAnnotation.AnchorType anchor = ArrowAnnotation.AnchorType.valueOf((String)value);
				aa.setAnchorType(ArrowAnnotation.ArrowEnd.SOURCE, anchor);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_TYPE)) {
				aa.setArrowType(ArrowAnnotation.ArrowEnd.SOURCE, (String)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_COLOR)) {
				aa.setArrowColor(ArrowAnnotation.ArrowEnd.SOURCE, (Paint)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_SOURCE_SIZE)) {
				aa.setArrowSize(ArrowAnnotation.ArrowEnd.SOURCE, (Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_WIDTH)) {
				aa.setLineWidth((Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_COLOR)) {
				aa.setLineColor((Paint)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_ANCHOR)) {
				ArrowAnnotation.AnchorType anchor = ArrowAnnotation.AnchorType.valueOf((String)value);
				aa.setAnchorType(ArrowAnnotation.ArrowEnd.TARGET, anchor);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_TYPE)) {
				aa.setArrowType(ArrowAnnotation.ArrowEnd.TARGET, (String)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_COLOR)) {
				aa.setArrowColor(ArrowAnnotation.ArrowEnd.TARGET, (Paint)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_ARROW_TARGET_SIZE)) {
				aa.setArrowSize(ArrowAnnotation.ArrowEnd.TARGET, (Double)value);
			} else if (vp.equals(AnnotationLexicon.ANNOTATION_VISIBLE)) {
				if (!visible) {
					savedLineColor = aa.getLineColor();
					savedArrowTargetColor = aa.getArrowColor(ArrowAnnotation.ArrowEnd.TARGET);
					savedArrowSourceColor = aa.getArrowColor(ArrowAnnotation.ArrowEnd.SOURCE);
					aa.setLineColor(transparent);
					aa.setArrowColor(ArrowAnnotation.ArrowEnd.TARGET, transparent);
					aa.setArrowColor(ArrowAnnotation.ArrowEnd.SOURCE, transparent);
				} else if (savedLineColor != null) {
					aa.setLineColor(savedLineColor);
					aa.setArrowColor(ArrowAnnotation.ArrowEnd.TARGET, savedArrowTargetColor);
					aa.setArrowColor(ArrowAnnotation.ArrowEnd.SOURCE, savedArrowSourceColor);
					savedLineColor = null;
					savedArrowTargetColor = null;
					savedArrowSourceColor = null;
				}
			}
		}
	}

	@Override
	public Long getSUID() {
		return suid;
	}

	public String toString() {
		return "View of "+annotation.toString();
	}
}

package edu.ucsf.rbvi.CyAnimator.internal.model;  

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.view.model.ContinuousRange;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.NullDataType;
import org.cytoscape.view.model.Range;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualLexiconNode;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.property.BooleanVisualProperty;
import org.cytoscape.view.presentation.property.DoubleVisualProperty;
import org.cytoscape.view.presentation.property.IntegerVisualProperty;
import org.cytoscape.view.presentation.property.PaintVisualProperty;
import org.cytoscape.view.presentation.property.StringVisualProperty;

/**
 * A wrapper class that allows Annotations to be
 * CyIdentifiables
 */
public class AnnotationLexicon implements VisualLexicon {
	static final Range<Double> ARBITRARY_DOUBLE_RANGE = 
					new ContinuousRange<>(Double.class, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, true);

	static final Set<String> STRING_SET = new HashSet<String>();
	static final Range<String> ARBITRARY_STRING_RANGE = new DiscreteRange<String>(String.class, STRING_SET) {
		// Takes any String as valid value.
		@Override
		public boolean inRange(String value) {
			return true;
		}
	};
	static final Color MIN_COLOR = new Color(0, 0, 0);
	static final Color MAX_COLOR = new Color(0xFF, 0xFF, 0xFF);
	static final Range<Paint> PAINT_RANGE = new ContinuousRange<>(Paint.class, MIN_COLOR, MAX_COLOR, true, true);

	private final Map<String, VisualProperty<?>> identifierLookup;
	private final Set<VisualProperty<?>> visualPropertySet;


	// Define our visual properties
	// Standard visual properites
	public static final VisualProperty<Double> ANNOTATION_X_LOCATION = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
										      "ANNOTATION_X_LOCATION", "Annotation X Location", true, CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_Y_LOCATION = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
											    "ANNOTATION_Y_LOCATION", "Annotation Y Location", true, CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_ZOOM = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
											    "ANNOTATION_ZOOM", "Annotation zoom", true, CyAnnotation.class);
	public static final VisualProperty<String> ANNOTATION_CANVAS = 
					new StringVisualProperty("", ARBITRARY_STRING_RANGE,
										      "ANNOTATION_CANVAS", "Canvas", CyAnnotation.class);
	public static final VisualProperty<Boolean> ANNOTATION_VISIBLE = 
					new BooleanVisualProperty(Boolean.TRUE, "ANNOTATION_VISIBLE", "Canvas", CyAnnotation.class);

	// Shape visual properites
	public static final VisualProperty<Double> ANNOTATION_HEIGHT = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
										      "ANNOTATION_HEIGHT", "Annotation Height", CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_WIDTH = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
										      "ANNOTATION_WIDTH", "Annotation Width", CyAnnotation.class);
	public static final VisualProperty<Paint> ANNOTATION_COLOR = 
					new PaintVisualProperty(Color.WHITE, PAINT_RANGE,
										      "ANNOTATION_COLOR", "Annotation Fill Color", CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_OPACITY = 
					new DoubleVisualProperty(1.0,
										      new ContinuousRange<Double>(Double.class, 0.0, 1.0, true, true), "ANNOTATION_OPACITY",
													      "Annotation transparency", CyAnnotation.class);

	public static final VisualProperty<Double> ANNOTATION_BORDER_WIDTH = 
					new DoubleVisualProperty(0.0, ARBITRARY_DOUBLE_RANGE,
										      "ANNOTATION_BORDER_WIDTH", "Annotation border width", CyAnnotation.class);
	public static final VisualProperty<Paint> ANNOTATION_BORDER_COLOR = 
					new PaintVisualProperty(Color.WHITE, PAINT_RANGE,
										      "ANNOTATION_BORDER_COLOR", "Annotation Border Color", CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_BORDER_OPACITY = 
					new DoubleVisualProperty(1.0,
										      new ContinuousRange<Double>(Double.class, 0.0, 1.0, true, true), 
													    "ANNOTATION_BORDER_OPACITY",
													     "Annotation border transparency", CyAnnotation.class);
	public static final VisualProperty<String> ANNOTATION_SHAPE = 
					new StringVisualProperty("", ARBITRARY_STRING_RANGE,
										      "ANNOTATION_SHAPE", "Annotation Shape", CyAnnotation.class);

	// Text visual properites
	public static final VisualProperty<String> ANNOTATION_TEXT = 
					new StringVisualProperty("", ARBITRARY_STRING_RANGE,
										      "ANNOTATION_TEXT", "Annotation Text", CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_FONT_SIZE = 
					new DoubleVisualProperty(12.0, ARBITRARY_DOUBLE_RANGE,
										      "ANNOTATION_FONT_SIZE", "Annotation Text Size", CyAnnotation.class);
	public static final VisualProperty<Paint> ANNOTATION_FONT_COLOR = 
					new PaintVisualProperty(Color.WHITE, PAINT_RANGE,
										      "ANNOTATION_FONT_COLOR", "Annotation Text Color", CyAnnotation.class);
	public static final VisualProperty<Integer> ANNOTATION_FONT_STYLE = 
					new IntegerVisualProperty(Font.PLAIN, 
										      new ContinuousRange<Integer>(Integer.class, 0, 4, true, true), 
										      "ANNOTATION_FONT_STYLE", "Annotation Text Style", CyAnnotation.class);
	public static final VisualProperty<String> ANNOTATION_FONT_FAMILY = 
					new StringVisualProperty("dialog", ARBITRARY_STRING_RANGE,
										      "ANNOTATION_FONT_FAMILY", "Annotation Text Family", CyAnnotation.class);

	// Image visual properties
  public static final VisualProperty<String> ANNOTATION_IMAGE_URL = 
					 new StringVisualProperty("", ARBITRARY_STRING_RANGE,
													"ANNOTATION_IMAGE_URL", "Annotation Immage URL", CyAnnotation.class);
  public static final VisualProperty<Integer> ANNOTATION_IMAGE_CONTRAST = 
					 new IntegerVisualProperty(0, 
										      new ContinuousRange<Integer>(Integer.class, -100, 100, true, true), 
													"ANNOTATION_IMAGE_CONTRAST", "Annotation Image Contrast", CyAnnotation.class);
  public static final VisualProperty<Integer> ANNOTATION_IMAGE_BRIGHTNESS = 
					 new IntegerVisualProperty(0, 
										      new ContinuousRange<Integer>(Integer.class, -100, 100, true, true), 
													"ANNOTATION_IMAGE_BRIGHTNESS", "Annotation Image Brightness", CyAnnotation.class);
	public static final VisualProperty<Double> ANNOTATION_IMAGE_OPACITY = 
					new DoubleVisualProperty(1.0,
										      new ContinuousRange<Double>(Double.class, 0.0, 1.0, true, true), 
													      "ANNOTATION_IMAGE_OPACITY",
													      "Annotation Image Transparency", CyAnnotation.class);


	// Arrow visual properties
  public static final VisualProperty<String> ANNOTATION_ARROW_SOURCE_ANCHOR = 
						           new StringVisualProperty("CENTER", ARBITRARY_STRING_RANGE,
																			          "ANNOTATION_ARROW_SOURCE_ANCHOR", "Annotation Source Arrow Anchor", CyAnnotation.class);
  public static final VisualProperty<String> ANNOTATION_ARROW_SOURCE_TYPE = 
						           new StringVisualProperty("NONE", ARBITRARY_STRING_RANGE,
																			          "ANNOTATION_ARROW_SOURCE_TYPE", "Annotation Source Arrow Type", CyAnnotation.class);
  public static final VisualProperty<Paint> ANNOTATION_ARROW_SOURCE_COLOR = 
						           new PaintVisualProperty(Color.BLACK, PAINT_RANGE,
																			          "ANNOTATION_ARROW_SOURCE_COLOR", "Annotation Source Arrow Color", CyAnnotation.class);
  public static final VisualProperty<Double> ANNOTATION_ARROW_SOURCE_SIZE = 
						           new DoubleVisualProperty(1.0, ARBITRARY_DOUBLE_RANGE,
																			          "ANNOTATION_ARROW_SOURCE_SIZE", "Annotation Source Arrow Size", CyAnnotation.class);

  public static final VisualProperty<Double> ANNOTATION_ARROW_WIDTH = 
						           new DoubleVisualProperty(1.0, ARBITRARY_DOUBLE_RANGE,
																			          "ANNOTATION_ARROW_WIDTH", "Annotation Arrow Line Width", CyAnnotation.class);
  public static final VisualProperty<Paint> ANNOTATION_ARROW_COLOR = 
						           new PaintVisualProperty(Color.BLACK, PAINT_RANGE,
																			          "ANNOTATION_ARROW_COLOR", "Annotation Arrow Color", CyAnnotation.class);

  public static final VisualProperty<String> ANNOTATION_ARROW_TARGET_ANCHOR = 
						           new StringVisualProperty("CENTER", ARBITRARY_STRING_RANGE,
																			          "ANNOTATION_ARROW_TARGET_ANCHOR", "Annotation Target Arrow Anchor", CyAnnotation.class);
  public static final VisualProperty<String> ANNOTATION_ARROW_TARGET_TYPE = 
						           new StringVisualProperty("NONE", ARBITRARY_STRING_RANGE,
																			          "ANNOTATION_ARROW_TARGET_TYPE", "Annotation Target Arrow Type", CyAnnotation.class);
  public static final VisualProperty<Paint> ANNOTATION_ARROW_TARGET_COLOR = 
						           new PaintVisualProperty(Color.BLACK, PAINT_RANGE,
																			          "ANNOTATION_ARROW_TARGET_COLOR", "Annotation Target Arrow Color", CyAnnotation.class);
  public static final VisualProperty<Double> ANNOTATION_ARROW_TARGET_SIZE = 
						           new DoubleVisualProperty(1.0, ARBITRARY_DOUBLE_RANGE,
																			          "ANNOTATION_ARROW_TARGET_SIZE", "Annotation Target Arrow Size", CyAnnotation.class);

	public AnnotationLexicon() { 
		visualPropertySet = new HashSet<>();
		identifierLookup = new HashMap<>();
		addVisualProperties();
	}

	private void addVisualProperties() {
		addProperty(ANNOTATION_X_LOCATION);
		addProperty(ANNOTATION_X_LOCATION);
		addProperty(ANNOTATION_Y_LOCATION);
		addProperty(ANNOTATION_ZOOM);
		addProperty(ANNOTATION_CANVAS);
		addProperty(ANNOTATION_WIDTH);
		addProperty(ANNOTATION_HEIGHT);
		addProperty(ANNOTATION_COLOR);
		addProperty(ANNOTATION_OPACITY);

		addProperty(ANNOTATION_BORDER_WIDTH);
		addProperty(ANNOTATION_BORDER_COLOR);
		addProperty(ANNOTATION_BORDER_OPACITY);
		addProperty(ANNOTATION_SHAPE); 
		addProperty(ANNOTATION_TEXT); 
		addProperty(ANNOTATION_FONT_SIZE);
		addProperty(ANNOTATION_FONT_COLOR);
		addProperty(ANNOTATION_FONT_STYLE);
		addProperty(ANNOTATION_FONT_FAMILY);
		addProperty(ANNOTATION_IMAGE_URL);
		addProperty(ANNOTATION_IMAGE_CONTRAST);
		addProperty(ANNOTATION_IMAGE_BRIGHTNESS);
		addProperty(ANNOTATION_IMAGE_OPACITY);
  	addProperty(ANNOTATION_ARROW_SOURCE_ANCHOR);
  	addProperty(ANNOTATION_ARROW_SOURCE_TYPE);
  	addProperty(ANNOTATION_ARROW_SOURCE_COLOR);
  	addProperty(ANNOTATION_ARROW_SOURCE_SIZE);

  	addProperty(ANNOTATION_ARROW_WIDTH);
  	addProperty(ANNOTATION_ARROW_COLOR);

  	addProperty(ANNOTATION_ARROW_TARGET_ANCHOR);
  	addProperty(ANNOTATION_ARROW_TARGET_TYPE);
  	addProperty(ANNOTATION_ARROW_TARGET_COLOR);
  	addProperty(ANNOTATION_ARROW_TARGET_SIZE);
	}

	@Override
	public Collection<VisualProperty<?>> getAllDescendants(VisualProperty<?> vp) {
		return visualPropertySet;
	}

	@Override
	public Set<VisualProperty<?>> getAllVisualProperties() {
		return visualPropertySet;
	}

	@Override
	public VisualProperty<NullDataType> getRootVisualProperty() {
		return null;
	}

	public <T> Set<T> getSupportedValueRange(VisualProperty<T> vp) {
		return null;
	}

	@Override
	public VisualLexiconNode getVisualLexiconNode(VisualProperty<?> vp) { return null; }

	@Override
	public boolean isSupported(VisualProperty<?> vp) {
		return visualPropertySet.contains(vp);
	}

	@Override
	public VisualProperty<?> lookup(Class<?> type, String identifier) {
		if (!type.isAssignableFrom(Annotation.class))
			return null;
		return identifierLookup.get(identifier);
	}

	private void addProperty(VisualProperty<?> prop) {
		identifierLookup.put(prop.getIdString(), prop);
		visualPropertySet.add(prop);
	}

}

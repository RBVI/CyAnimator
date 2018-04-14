package edu.ucsf.rbvi.CyAnimator.internal.video;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface SequenceEncoderWrapper {
  public void encodeImage(BufferedImage img) throws IOException;

  /**
   * Close this GifSequenceWriter object. This does not close the underlying
   * stream, just finishes off the GIF.
   */
  public void finish() throws IOException;
}

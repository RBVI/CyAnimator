package edu.ucsf.rbvi.CyAnimator.internal.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rational;
import static org.jcodec.common.model.ColorSpace.RGB;

import edu.ucsf.rbvi.CyAnimator.internal.model.TimeBase;

/**
 * This class was originally part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 *
 * It has been modified by the RBVI to support the CyAnimator project to expose the FPS
 * parameter to provide user control of the frame rate
 * 
 * @author The JCodec project
 */
public class MP4SequenceEncoder extends SequenceEncoder implements SequenceEncoderWrapper {
		private TimeBase timebase;

    public MP4SequenceEncoder(File out, TimeBase timebase, int fps) throws IOException {
				super(NIOUtils.writableChannel(out), new Rational(fps,1), Format.MOV, Codec.H264, null);
				this.timebase = timebase;
    }

		
		public void encodeImage(BufferedImage bi) throws IOException {
			encodeNativeFrame(AWTUtil.fromBufferedImage(bi, ColorSpace.RGB));
		}

}

package edu.ucsf.rbvi.CyAnimator.internal.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;

import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack;
import org.jcodec.scale.RgbToYuv420p;
import org.jcodec.scale.Transform;

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
@Deprecated
public class WebMSequenceEncoder implements SequenceEncoder {
    private SeekableByteChannel ch;
    private Picture toEncode;
		private VP8Encoder encoder;
		private MKVMuxer muxer;
		private MKVMuxerTrack outTrack;
    private ByteBuffer _out;
    private int frameNo;
    private ByteBuffer sps;
    private ByteBuffer pps;
		private TimeBase timebase;
		private Transform transform;

    public WebMSequenceEncoder(File out, TimeBase timebase) throws IOException {
        this.ch = NIOUtils.writableFileChannel(out);
				this.timebase = timebase;

				// Get the encoder
				encoder = new VP8Encoder(10 /*qp?*/);

        // Transform to convert between RGB and YUV
        transform = new RgbToYuv420p(0, 0);

        // Muxer that will store the encoded frames
        muxer = new MKVMuxer();

        outTrack = null;

        // Allocate a buffer big enough to hold output frames
        _out = ByteBuffer.allocate(1920 * 1080 * 6);

				frameNo = 0;

    }

		
		public void encodeImage(BufferedImage bi) throws IOException {
			if (outTrack == null)
      	outTrack = muxer.createVideoTrack(new Size(bi.getWidth(), bi.getHeight()), "V_VP8");

			try {
				Picture p = AWTUtil.fromBufferedImage(bi);
				encodeNativeFrame(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

    public void encodeNativeFrame(Picture pic) throws IOException {
				Picture toEncode = Picture.create(pic.getWidth(), pic.getHeight(), ColorSpace.YUV420);

				if (outTrack == null)
      		outTrack = muxer.createVideoTrack(new Size(pic.getWidth(), pic.getHeight()), "V_VP8");

        // Perform conversion
        transform.transform(pic, toEncode);

				if (_out == null)
					_out = ByteBuffer.allocate(pic.getWidth()*pic.getHeight()*3);
				else
					_out.clear();

        ByteBuffer result = encoder.encodeFrame(toEncode, _out);
				outTrack.addSampleEntry(result, frameNo);

				frameNo++;
    }

    public void finish() throws IOException {
        // Write MP4 header and finalize recording
        muxer.mux(ch);
        NIOUtils.closeQuietly(ch);
    }
}

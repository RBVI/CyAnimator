package edu.ucsf.rbvi.CyAnimator.internal.video;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AWTUtil {

    public static BufferedImage toBufferedImage(Picture src) {
        if (src.getColor() != ColorSpace.RGB) {
            Transform transform = ColorUtil.getTransform(src.getColor(), ColorSpace.RGB);
            Picture rgb = Picture.createCropped(src.getWidth(), src.getHeight(), ColorSpace.RGB, src.getCrop());
            transform.transform(src, rgb);
            src = rgb;
        }

        BufferedImage dst = new BufferedImage(src.getCroppedWidth(), src.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (src.getCrop() == null)
            toBufferedImage(src, dst);
        else
            toBufferedImageCropped(src, dst);

        return dst;
    }

    private static void toBufferedImageCropped(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        int dstStride = dst.getWidth() * 3;
        int srcStride = src.getWidth() * 3;
        for (int line = 0, srcOff = 0, dstOff = 0; line < dst.getHeight(); line++) {
            for (int id = dstOff, is = srcOff; id < dstOff + dstStride; id += 3, is += 3) {
                data[id] = (byte) srcData[is];
                data[id + 1] = (byte) srcData[is + 1];
                data[id + 2] = (byte) srcData[is + 2];
            }
            srcOff += srcStride;
            dstOff += dstStride;
        }
    }

    public static void toBufferedImage(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) srcData[i];
        }
    }

    public static Picture fromBufferedImage(BufferedImage src, ColorSpace tgtColor) {
        Picture rgb = fromBufferedImage(src);
        Transform tr = ColorUtil.getTransform(rgb.getColor(), tgtColor);
        Picture res = Picture.create(rgb.getWidth(), rgb.getHeight(), tgtColor);
        tr.transform(rgb, res);
        return res;
    }

    public static Picture fromBufferedImage(BufferedImage src) {
				// Make sure our Picture is even
				int width = (src.getWidth()/2)*2;
				int height = (src.getHeight()/2)*2;
        Picture dst = Picture.create(width, height, RGB);
        fromBufferedImage(src, dst);
        return dst;
    }

    public static void fromBufferedImage(BufferedImage src, Picture dst) {
        byte[] dstData = dst.getPlaneData(0);

        int off = 0;
        for (int i = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j++) {
                int rgb1 = src.getRGB(j, i);
                dstData[off++] = (byte)(((rgb1 >> 16) & 0xff) - 128);
                dstData[off++] = (byte)(((rgb1 >> 8) & 0xff) - 128);
                dstData[off++] = (byte)((rgb1 & 0xff) - 128);
            }
        }
    }

    public static void savePicture(Picture pic, String format, File file) throws IOException {
        ImageIO.write(toBufferedImage(pic), format, file);
    }
}

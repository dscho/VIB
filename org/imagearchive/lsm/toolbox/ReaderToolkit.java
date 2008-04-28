package org.imagearchive.lsm.toolbox;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.RandomAccessStream;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;

public class ReaderToolkit {

    public static short swap(short x) {
        return (short) ((x << 8) | ((x >> 8) & 0xff));
    }

    public static char swap(char x) {
        return (char) ((x << 8) | ((x >> 8) & 0xff));
    }

    public static int swap(int x) {
        return (int) ((swap((short) x) << 16) | (swap((short) (x >> 16)) & 0xffff));
    }

    public static long swap(long x) {
        return (long) (((long) swap((int) (x)) << 32) | ((long) swap((int) (x >> 32)) & 0xffffffffL));
    }

    public static float swap(float x) {
        return Float.intBitsToFloat(swap(Float.floatToIntBits(x)));
    }

    public static double swap(double x) {
        return Double.longBitsToDouble(swap(Double.doubleToLongBits(x)));
    }

    public static String readSizedNULLASCII(RandomAccessStream stream, long s) {
        int offset = 0;
        String tempstr = new String("");
        int in = 0;
        char ch;
        boolean addchar = true;
        try {
            while (offset < s) {
                in = stream.read();
                if (in == -1)
                    break;
                ch = (char) in;
                if (addchar == true) {
                    String achar = new Character((char) ch).toString();
                    if (ch != 0x00)
                        tempstr += achar;
                    else
                        addchar = false;
                }
                offset++;
            }
        }

        catch (IOException Read_ASCII_exception) {
            Read_ASCII_exception.printStackTrace();
        }
        return tempstr;
    }

    public static String readASCII(RandomAccessStream stream, long length) {
        String rtn = "";
        try {
            for (int i = 0; i < length; i++)
                rtn += (char) (stream.read());
        } catch (IOException Read_ASCII_exception) {
            Read_ASCII_exception.printStackTrace();
        }
        return rtn;
    }

    public static String readNULLASCII(RandomAccessStream stream) {
        String rtn = "";
        try {
            char ch;

            do {
                ch = (char) (stream.read());
                if (ch != 0) {
                    rtn += ch;
                }
            } while (ch != 0);
        } catch (IOException Read_ASCII_exception) {
            Read_ASCII_exception.printStackTrace();
        }
        return rtn;
    }

    public static String readNULLASCII2(RandomAccessStream stream, long s) {
        int offset = 0;
        String tempstr = new String("");
        int in = 0;
        char ch;
        try {
            while (offset < s) {
                in = stream.read();
                if (in == -1)
                    break;
                ch = (char) in;
                if (ch != 0)
                    tempstr += Character.toString(ch);
                else
                    return tempstr;
                offset++;
            }
        } catch (IOException Read_ASCII_exception) {
            Read_ASCII_exception.printStackTrace();
        }
        return tempstr;
    }
    
    /***************************************************************************
     * method : Apply_colors, applies color gradient; function taken out from *
     * my Lut_Panel plugin *
     **************************************************************************/

    public static void apply_colors(ImagePlus imp, Color[] gc, int i) {
        FileInfo fi = new FileInfo();
        fi.reds = new byte[256];
        fi.greens = new byte[256];
        fi.blues = new byte[256];
        fi.lutSize = 256;
        float nColorsfl = 256;
        float interval = 256;
        float iR = gc[0].getRed();
        float iG = gc[0].getGreen();
        float iB = gc[0].getBlue();
        float idR = gc[1].getRed() - gc[0].getRed();
        float idG = gc[1].getGreen() - gc[0].getGreen();
        float idB = gc[1].getBlue() - gc[0].getBlue();
        idR = (idR / interval);
        idG = (idG / interval);
        idB = (idB / interval);
        int a = 0;
        for (a = (int) (interval * 0); a < (int) (interval * (0) + interval); a++, iR += idR, iG += idG, iB += idB) {
            fi.reds[a] = (byte) (iR);
            fi.greens[a] = (byte) (iG);
            fi.blues[a] = (byte) (iB);
        }
        int b = (int) (interval * 0 + interval) - 1;
        fi.reds[b] = (byte) (gc[1].getRed());
        fi.greens[b] = (byte) (gc[1].getGreen());
        fi.blues[b] = (byte) (gc[1].getBlue());
        nColorsfl = 256;

        if (nColorsfl > 0) {
            if (nColorsfl < 256)
                interpolate(fi.reds, fi.greens, fi.blues, (int) nColorsfl);
            showLut(imp, fi, true);
            return;
        }
    }

    /***************************************************************************
     * method : interpolate taken out from the orignal ImageJ project from Wayne *
     * Rasband.No access to method so I had to add it manually. *
     **************************************************************************/

    public static void interpolate(byte[] reds, byte[] greens, byte[] blues,
            int nColors) {
        byte[] r = new byte[nColors];
        byte[] g = new byte[nColors];
        byte[] b = new byte[nColors];
        System.arraycopy(reds, 0, r, 0, nColors);
        System.arraycopy(greens, 0, g, 0, nColors);
        System.arraycopy(blues, 0, b, 0, nColors);
        double scale = nColors / 256.0;
        int i1, i2;
        double fraction;
        for (int i = 0; i < 256; i++) {
            i1 = (int) (i * scale);
            i2 = i1 + 1;
            if (i2 == nColors)
                i2 = nColors - 1;
            fraction = i * scale - i1;
            reds[i] = (byte) ((1.0 - fraction) * (r[i1] & 255) + fraction
                    * (r[i2] & 255));
            greens[i] = (byte) ((1.0 - fraction) * (g[i1] & 255) + fraction
                    * (g[i2] & 255));
            blues[i] = (byte) ((1.0 - fraction) * (b[i1] & 255) + fraction
                    * (b[i2] & 255));
        }
    }

    /***************************************************************************
     * method : showLut, applies the new Lut on the actual image *
     **************************************************************************/

    static void showLut(ImagePlus imp, FileInfo fi, boolean showImage) {
        if (imp != null) {
            if (imp.getType() == ImagePlus.COLOR_RGB)
                IJ.error("Color tables cannot be assiged to RGB Images.");
            else {
                ImageProcessor ip = imp.getProcessor();
                ColorModel cm = new IndexColorModel(8, 256, fi.reds, fi.greens,
                        fi.blues);
                ip.setColorModel(cm);
                if (imp.getStackSize() > 1)
                    imp.getStack().setColorModel(cm);
                imp.updateAndDraw();
            }
        }
        /*
         * else imp.createImage(fi, showImage);
         */
    }
}

package ij3d;

import javax.vecmath.Color3f;

import java.awt.Color;
import java.awt.image.IndexColorModel;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;



public class ColorTable {

	public static Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				return colors[i];
			}
		}
		return null;
	}

	public static String[] colorNames = new String[]{"None", "Black", 
				"White", "Red", "Green", "Blue", "Cyan", 
				"Magenta", "Yellow"};

	public static Color3f[] colors = {
				null,
				new Color3f(0,    0,    0),
				new Color3f(1f, 1f, 1f),
				new Color3f(1f, 0,    0),
				new Color3f(0,    1f, 0),
				new Color3f(0,    0,    1f),
				new Color3f(0,    1f, 1f),
				new Color3f(1f, 0,    1f),
				new Color3f(1f, 1f, 0)};

	public static boolean isRedCh(String color) {
		return color.equals("White") || color.equals("Red") || 
				color.equals("Magenta") || color.equals("Yellow");
	}

	public static boolean isGreenCh(String color) {
		return color.equals("White") || color.equals("Green") || 
				color.equals("Cyan") || color.equals("Yellow");
	}

	public static boolean isBlueCh(String color) {
		return color.equals("White") || color.equals("Blue") || 
				color.equals("Cyan") || color.equals("Magenta");
	}
	
	public static String getColorName(Color3f col) {
		for(int i = 1; i < colors.length; i++) {
			if(colors[i].equals(col))
				return colorNames[i];
		}
		return "None";
	}

	public static IndexColorModel adjustColorModel(
				IndexColorModel cmodel, String color, boolean[] ch) {

		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		byte[] a = new byte[256];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		float sum = 0;
		for(int i = 0; i < 3; i++) 
			if(ch[i])
				sum++;
		for(int i = 0; i < 256; i++) {
			if(color.equals("None")) {
				r[i] = ch[0] ? r[i] : 0;
	                  g[i] = ch[1] ? g[i] : 0;
	                  b[i] = ch[2] ? b[i] : 0;
				a[i] = (byte)((
						((int)r[i]&0xff) + 
						((int)b[i]&0xff) + 
						((int)g[i]&0xff)) / sum);
				a[i] = (byte)i;
			} else {
				int intens = 0;
				if(ch[0]) intens += ((int)r[i] & 0xff);
				if(ch[1]) intens += ((int)g[i] & 0xff);
				if(ch[2]) intens += ((int)b[i] & 0xff);
				byte val = (byte)(intens/sum);
				r[i] = isRedCh(color) ? val : 0;
				g[i] = isGreenCh(color) ? val : 0;
				b[i] = isBlueCh(color) ? val : 0;
				a[i] = val;
			}
		}
		IndexColorModel c = new IndexColorModel(8, 256, r, g, b, a);
		return c;
	}

	public static ImagePlus adjustChannels(ImagePlus imp, boolean[] ch) {

		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int[] weight = new int[3];
		IndexColorModel cmodel = 
				(IndexColorModel)imp.getProcessor().getColorModel();
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		float sum = 0;
		for(int i = 0; i < 3; i++) {
			if(ch[i]) {
				weight[i] = 1;
				sum++;
			}
		}
		
		ImageStack res = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] bytes = 
				(byte[])imp.getStack().getProcessor(z+1).getPixels();
			byte[] newB = new byte[bytes.length];
			for(int i = 0; i < w*h; i++) {
				int index = bytes[i] & 0xff;
				int value = (weight[0] * (int)(r[index]&0xff) + 
						weight[1] * (int)(g[index]&0xff) + 
						weight[2] * (int)(b[index]&0xff));
				newB[i] = (byte)(value/sum); 
					
			}
			res.addSlice("", new ByteProcessor(w, h, newB, null));
		}
		ImagePlus newImage = new ImagePlus(imp.getTitle(), res);
		newImage.setCalibration(imp.getCalibration());
		return newImage;
	}

	public static void debug(IndexColorModel cmodel) {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		for(int i = 0; i < 256; i++) {
			System.out.println((r[i] & 0xff) + "\t" + 
						 (g[i] & 0xff) + "\t" + 
						 (b[i] & 0xff));
		}
	}
}

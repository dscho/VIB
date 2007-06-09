import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.IJ;

import ij3d.ImageWindow3D;
import ij3d.Content;
import java.awt.Dimension;
import java.awt.MenuBar;
import java.awt.Menu;

import ij3d.Image3DUniverse;
import ij3d.Image3DMenubar;
import isosurface.MeshGroup;
import voltex.VoltexGroup;
import orthoslice.OrthoGroup;

public class ImageJ_3D_Viewer implements PlugInFilter {

	private ImagePlus image;
	private static Image3DUniverse univ;


	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("ImageJ 3D Viewer");
		gd.addChoice("Surface or Volume: ", 
			new String[]{"Surface", "Volume", "Orthoslice"},
			"Volume");
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		
		try {
			univ = new Image3DUniverse(512, 512);
			String type = gd.getNextChoice();
			if(type.equals("Surface"))
				MeshGroup.addContent(univ, image);
			else if(type.equals("Volume"))
				VoltexGroup.addContent(univ, image);
			else if(type.equals("Orthoslice"))
				OrthoGroup.addContent(univ, image);

			univ.show();
		} catch(Exception e) {
			StringBuffer buf = new StringBuffer();
			StackTraceElement[] st = e.getStackTrace();
			buf.append("An unexpected exception occurred. \n" + 
				"Please mail me the following lines if you \n"+
				"need help.\n" + 
				"bene.schmid@gmail.com\n   \n");
			buf.append(e.getClass().getName()  + ":" + 
						e.getMessage() + "\n");
			for(int i = 0; i < st.length; i++) {
				buf.append(
					"    at " + st[i].getClassName() + 
					"." + st[i].getMethodName() + 
					"(" + st[i].getFileName() + 
					":" + st[i].getLineNumber() + 
					")\n");
			}
			new ij.text.TextWindow("Error", buf.toString(), 500, 400);
		}
	}

	public static void startAnimate() {
		if(univ != null) univ.startAnimation();
	}

	public static void stopAnimate() {
		if(univ != null) univ.pauseAnimation();
	}

	public static void startRecord() {
		if(univ != null) univ.startRecording();
	}

	public static void stopRecord() {
		if(univ != null) univ.stopRecording();
	}

	public static void close() {
		if(univ != null) univ.close();
	}

	public static void select(String name) {
		if(univ != null) univ.select(
			(Content)univ.getContent(name));
	}

	public static void setColor(String red, String green, String blue) {
		float r = Integer.parseInt(red) / 256f;
		float g = Integer.parseInt(green) / 256f;
		float b = Integer.parseInt(blue) / 256f;
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setColor(
				new javax.vecmath.Color3f(r, g, b));
		}
	}

	public static void setTransparency(String t) {
		float tr = Float.parseFloat(t);
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransparency(tr);
		}
	}

	public static void setChannels(String red, String green, String blue) {
		boolean r = Boolean.valueOf(red).booleanValue();
		boolean g = Boolean.valueOf(green).booleanValue();
		boolean b = Boolean.valueOf(blue).booleanValue();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setChannels(new boolean[]{r, g, b});
		}
	}

	public static void resetView() {
		if(univ != null) univ.resetView();
	}

	public static void fillSelection() {
		if(univ != null && univ.getSelected() != null && 
			univ.getSelected() instanceof VoltexGroup) {

			VoltexGroup vg = (VoltexGroup)univ.getSelected();
			vg.fillRoiBlack(univ, (byte)0);
		}
	}

	public static void delete() {
		try{
		if(univ != null && univ.getSelected() != null) {
			univ.removeContent(univ.getSelected().getName());
		}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8C | DOES_8G;
	}
}

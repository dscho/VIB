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

import javax.media.j3d.Transform3D;

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

	// View menu
	public static void resetView() {
		if(univ != null) univ.resetView();
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

	// Contents menu
	public static void addVolume(String image, String color, String name,
			String r, String g, String b, String resamplingF) {

		/*
		ImagePlus grey = WindowManager.getImage(image);
		Color3f color = ColorTable.getColor(color);

		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		Vector3f tr = new Vector3f();
		if(grey != null) {
			Calibration c = grey.getCalibration();
			tr.x = (float)(-grey.getWidth() * c.pixelWidth/2);
			tr.y = (float)(-grey.getHeight() * c.pixelHeight/2);
			tr.z = (float)(-grey.getStackSize() * c.pixelDepth/2);
		}
		
		univ.addVoltex(grey, color, name, channels, factor);
		*/
	}

	public static void delete() {
		if(univ != null && univ.getSelected() != null) {
			univ.removeContent(univ.getSelected().getName());
		}
	}


	// Individual content's menu
	public static void adjustSlices(String x, String y, String z) {
		if(univ != null && univ.getSelected() != null && 
			univ.getSelected() instanceof OrthoGroup) {

			OrthoGroup vg = (OrthoGroup)univ.getSelected();
			vg.setSlices(Integer.parseInt(x),
					Integer.parseInt(y),
					Integer.parseInt(z));
		}
	}

	public static void fillSelection() {
		if(univ != null && univ.getSelected() != null && 
			univ.getSelected() instanceof VoltexGroup) {

			VoltexGroup vg = (VoltexGroup)univ.getSelected();
			vg.fillRoiBlack(univ, (byte)0);
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

	public static void applyTransform(String transform) {
		if(univ != null && univ.getSelected() != null) {
			String[] s = ij.util.Tools.split(transform);
			float[] m = new float[s.length];
			for(int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().applyTransform(new Transform3D(m));
		}
	}

	public static void resetTransform(String transform) {
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransform(new Transform3D());
		}
	}

	public static void setTransform(String transform) {
		if(univ != null && univ.getSelected() != null) {
			String[] s = ij.util.Tools.split(transform);
			float[] m = new float[s.length];
			for(int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().setTransform(new Transform3D(m));
		}
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8C | DOES_8G;
	}
}

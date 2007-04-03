import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

import ij3d.ImageWindow3D;
import java.awt.Dimension;

import vis3d.Image3DUniverse;
import vis3d.Image3DCanvasPopup;

public class Isosurface_Viewer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		Image3DUniverse univ = new Image3DUniverse();
		univ.getCanvas().setSize(new Dimension(512, 512));
		Image3DCanvasPopup.addNewImage(univ, image);
		new Image3DCanvasPopup(univ);
		new ImageWindow3D("Surface Viewer", univ.getCanvas(), univ);
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | NO_CHANGES;
	}
}

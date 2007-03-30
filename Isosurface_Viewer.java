import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

import ij3d.ImageWindow3D;
import java.awt.Dimension;

import isosurface.IsosurfaceUniverse;
import isosurface.IsosurfaceCanvasPopup;

public class Isosurface_Viewer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		IsosurfaceUniverse univ = new IsosurfaceUniverse();
		univ.getCanvas().setSize(new Dimension(512, 512));
		IsosurfaceCanvasPopup.addNewImage(univ, image);
		new IsosurfaceCanvasPopup(univ);
		new ImageWindow3D("Surface Viewer", univ.getCanvas(), univ);
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | NO_CHANGES;
	}
}

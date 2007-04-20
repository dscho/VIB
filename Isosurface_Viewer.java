import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

import ij3d.ImageWindow3D;
import java.awt.Dimension;
import java.awt.MenuBar;
import java.awt.Menu;

import ij3d.Image3DUniverse;
import ij3d.Image3DMenubar;
import isosurface.MeshGroup;

public class Isosurface_Viewer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		MeshGroup.addContent(univ, image);
		univ.show();
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8C | DOES_8G;
	}
}

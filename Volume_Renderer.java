import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

import ij3d.ImageWindow3D;
import java.awt.Dimension;
import java.awt.MenuBar;
import java.awt.Menu;

import vis3d.Image3DUniverse;
import vis3d.Image3DMenubar;
import voltex.VoltexGroup;

public class Volume_Renderer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		Image3DUniverse univ = new Image3DUniverse();
		univ.getCanvas().setSize(new Dimension(512, 512));
		VoltexGroup.addContent(univ, image);
		ImageWindow3D win = new ImageWindow3D(
				"Volume Renderer", univ.getCanvas(), univ);
		win.setMenuBar(new Image3DMenubar(univ));
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8C | DOES_8G;
	}
}

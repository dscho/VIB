import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import voltex.VolRendEdit;

public class Volume_Renderer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip){
		new VolRendEdit(image);
	}

	public int setup(String arg, ImagePlus img){
		this.image = img;
		return DOES_8G;
	}
}

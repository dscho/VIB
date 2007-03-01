import javax.media.j3d.Canvas3D;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import voltex.VolRend;
import voltex.ColorTable;
import ij3d.ImageWindow3D;

public class Volume_Renderer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip){
		VolRend volRend = new VolRend();
		volRend.initContext(image, 
				ColorTable.getColor(ColorTable.GREEN), 
				false); 
		
		Canvas3D canvas = volRend.getCanvas();
		canvas.setSize(512,512);
		
		ImageWindow3D win = new ImageWindow3D("Java3D Volume Renderer", 
				canvas, volRend.getUniverse());
	}

	public int setup(String arg, ImagePlus img){
		this.image = img;
		return DOES_8G | DOES_8C | NO_CHANGES;
	}
}

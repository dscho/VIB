
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Polygon;

/**
 * @author Benjamin Schmid
 * @date 19. July 2006
 */
public class Fill_holes implements PlugIn {

    public static final String MACRO_CMD =
		"var leftClick=16, alt=9;\n" +
        "macro 'Fill hole Tool - C111O11ffC100T6c0aF' {\n" +
        " while (true) {\n" +
        "  getCursorLoc(x, y, z, flags);\n" +
        "  if (flags&leftClick==0) exit();\n" +
        "  call('Fill_holes.fillHoles', x,y,z,flags);\n" +
        "  exit();" + 
        " }\n" +
        "}\n" +
        "\n";
	
	public void run(String arg) {
		MacroInstaller installer = new MacroInstaller();
        installer.install(MACRO_CMD);
	}
	
	public synchronized static void fillHoles(String x, String y, String z, String flags){
		fillHoles(Integer.parseInt(x),
				Integer.parseInt(y),
				Integer.parseInt(z));
	}

	public synchronized static void fillHoles(int x, int y, int z){
		
		ImagePlus imp = IJ.getImage();
		Roi roi = imp.getRoi();
        if (roi==null || roi.getType()!=Roi.COMPOSITE) {
        	IJ.showMessage("Image with composite selection required");
        	return;
        }
        if(roi.contains(x,y)){
        	IJ.showMessage("There is no hole at the specified location");
        	return;
        }
        
        if(roi instanceof ShapeRoi){
        	
        	try{
	        	Roi[] rois = ((ShapeRoi)roi).getRois();        
		        imp.killRoi();
		        ShapeRoi containingRois = null;	        
		        
		        // Rois which contain (x,y) are removed (NOT) from the original roi and 
		        // a union is build from them separately, which is afterwards added (OR)
		        // to the original roi.
		        for(int i = 0; i<rois.length; i++){
		        	if(rois[i].contains(x,y)){
		        		if(containingRois == null){
		        			containingRois = new ShapeRoi(rois[i]);
		        		} else {
		        			containingRois.or(new ShapeRoi(rois[i]));
		        		}
		        		((ShapeRoi)roi).not(new ShapeRoi(rois[i]));
		        	} else {
		        		// do nothing. Rois, which do not contain (x,y) remain
		        		// in roi.
		        	}
		        	if(containingRois != null){
		        		((ShapeRoi)roi).or(containingRois);
		        	}
		        }
		        imp.setRoi(roi);		
				imp.updateAndDraw();
        	} catch(Exception e){
        		e.printStackTrace();
        	}
        }
	}
}

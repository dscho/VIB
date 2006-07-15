
import ij.IJ;
import ij.process.ImageProcessor;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.OvalRoi;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.*;

/**
 * User: Tom Larkworthy
 * Date: 21-Jun-2006
 * Time: 00:28:54
 */
public class LabelBrush_ implements PlugIn {
    public static final String MACRO_CMD = "var brushWidth = 10;\n" +
                    "var leftClick=16, alt=9;\n" +
					"var pollDelay = 10;\n" +
                    "macro 'Label Brush Tool - C111O11ffC100T6c0aL' {\n" +
                    " while (true) {\n" +
                    "  getCursorLoc(x, y, z, flags);\n" +
                    "  if (flags&leftClick==0) exit();\n" +
                    "  if (flags&alt==0){\n" +
                    "   call('LabelBrush_.label', x,y,z,flags,brushWidth);\n" +
                    "  }else{\n" +
                    "   call('LabelBrush_.unlabel', x,y,z,flags,brushWidth);\n" +
                    "  }\n" +
                    "  wait(pollDelay);\n" +
                    " }\n" +
                    "}\n" +
                    "\n" +
                    "macro 'Label Brush Tool Options...' {\n" +
                    " brushWidth = getNumber('Label Brush Width (pixels):', brushWidth);\n"+
                    "}";


    public void run(String arg) {
        System.out.println("run of LabelBrush_ ...");
        if (IJ.versionLessThan("1.37c"))
        {
            System.err.println("Version too old");
            return;
        }else{
            System.out.println("loading LabelBrushTool");
        }

        MacroInstaller installer = new MacroInstaller();
        installer.install(MACRO_CMD);
    }

    //methods in a macro accessable format
    public synchronized static void label(String x, String y, String z, String flags, String width) {
        label(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public synchronized static void unlabel(String x, String y, String z, String flags, String width) {
        unlabel(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void label(int x, int y, int z, int flags, int width) {
        z++;
        labelROI(getBrushRoi(x,y,width), getProcessor(z), getColor());
        updateSlice(z);
    }

    public static void unlabel(int x, int y, int z, int flags, int width) {
        z++;
        labelROI(getBrushRoi(x,y,width), getProcessor(z), 0);
        updateSlice(z);
    }

    private static ImageProcessor getProcessor(int z) {
        return new SegmentatorModel(IJ.getImage()).getLabelImagePlus().getStack().getProcessor(z);
    }

    private static int getColor(){
        AmiraParameters.Material material = new SegmentatorModel(IJ.getImage()).getCurrentMaterial();
        if(material == null) return 0;
        return material.id;
    }

    private static void updateSlice(int z) {
        new SegmentatorModel(IJ.getImage()).updateSlice(z);
    }

    public static Roi getBrushRoi(int x, int y, int width){
        return new OvalRoi(x-width/2,y-width/2,width,width);
    }

    public static void labelROI(Roi roi, ImageProcessor ip, int color){
        Rectangle bounds = roi.getBoundingRect();
        for(int i=bounds.x;i<=bounds.x+bounds.width;i++){
            for(int j=bounds.y;j<=bounds.y+bounds.height;j++){
                if(roi.contains(i,j)) ip.set(i,j,color);
            }
        }
    }


}

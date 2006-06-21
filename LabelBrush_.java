
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
        String macros =
                "var brushWidth = 10;\n" +
                "var leftClick=16, alt=9;\n" +
                "\n" +
                "macro 'Label Brush Tool - C111o11ff' {\n" +
                " while (true) {\n" +
                "  getCursorLoc(x, y, z, flags);\n" +
                "  if (flags&leftClick==0) exit();\n" +
                "  if (flags&alt==0){\n" +
                "   call('LabelBrush_.label', x,y,z,flags,brushWidth);\n" +
                "  }else{\n" +
                "   call('LabelBrush_.unlabel', x,y,z,flags,brushWidth);\n" +
                "  }\n" +
                "  wait(10);\n" +
                " }\n" +
                "}\n" +
                "\n" +
                "macro 'Label Brush Tool Options...' {\n" +
                " brushWidth = getNumber('Label Brush Width (pixels):', brushWidth);\n" +
                "}";
        installer.install(macros);
    }

    //methods in a macro accessable format
    public static void label(String x, String y, String z, String flags, String width) {
        label(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void unlabel(String x, String y, String z, String flags, String width) {
        unlabel(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void label(int x, int y, int z, int flags, int width) {
        z++;
        System.out.println("label");
        fillOval(x-width / 2, y-width / 2,z,  width, width, getColor());
        updateSlice(z);
    }



    public static void unlabel(int x, int y, int z, int flags, int width) {
        System.out.println("unlabel");
        fillOval(x-width / 2, y-width / 2,z,  width, width, 0);
    }

    //had to write our own trivial implementation becuase the ImageJ one does not seem to work...
    private static void fillOval(int x, int y, int z, int width, int height, int color) {
        OvalRoi roi = new OvalRoi(x,y,width,height);

        ImageProcessor ip = getProcessor(z);
        ip.setRoi(roi);
        for(int i=x;i<=x+width;i++){
            for(int j=y;j<=y+height;j++){
                if(roi.contains(i,j)) ip.set(i,j,color);
            }
        }
        ip.resetRoi();
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


}

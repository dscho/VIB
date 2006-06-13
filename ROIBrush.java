
import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.OvalRoi;

import java.awt.*;

/**
 * User: Tom Larkworthy
 * Date: 08-Jun-2006
 * Time: 17:32:42
 */
public class ROIBrush {

    public static void init() {

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
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.or(getBrushRoi(x, y,  width));
        } else {
            roi = getBrushRoi(x, y,  width);
        }

        IJ.getImage().setRoi(roi);
    }

    public static void unlabel(int x, int y, int z, int flags, int width) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.not(getBrushRoi(x, y,  width));

            IJ.getImage().setRoi(roi);
        }
    }


    private static ShapeRoi getBrushRoi(int x, int y, int width) {
        return new ShapeRoi(new OvalRoi(x- width/2, y - width/2, width, width));
    }


}

package isosurface;

import java.util.List;
import ij.ImagePlus;

public interface Triangulator {
	/**
	 * This method must return a list of elements of class Point3f.
	 * Three subsequent points specify one triangle.
	 * @param image the ImagePlus to be displayed
	 * @param threshold the isovalue of the surface to be generated.
	 */
	public List getTriangles(ImagePlus image, int threshold);
}

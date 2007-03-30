package marchingcubes;

import ij.ImagePlus;
import java.util.List;

import isosurface.Triangulator;

public class MCTriangulator implements Triangulator {

	public List getTriangles(ImagePlus image, int threshold) {
		return MCCube.getTriangles(image, threshold);
	}
}

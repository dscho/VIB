package marchingcubes;

import ij.ImagePlus;
import java.util.List;

import vib.Resample_;

import vis3d.ColorTable;

import isosurface.Triangulator;

public class MCTriangulator implements Triangulator {

	public List getTriangles(ImagePlus image, int threshold, 
						boolean[] channels, int resamplingF) {
		image = ColorTable.adjustChannels(image, channels);
		if(resamplingF != 1)
			image = Resample_.resample(image, resamplingF);
		List l = MCCube.getTriangles(image, threshold);
		return l;
	}
}

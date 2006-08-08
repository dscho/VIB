package vib;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.text.TextPanel;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.lang.*;

public class AugmentedLandmarkWarp_ implements PlugInFilter {
	ImagePlus image;
	InterpolatedImage ii;

	InterpolatedImage model;
	InterpolatedImage labels;

	int labelCount;
	FloatMatrix[] matrix;
	int[][] center;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Transform Parameters");
		if (!AmiraParameters.addAmiraLabelsList(gd, "TemplateLabels"))
			return;
		if (!AmiraParameters.addAmiraMeshList(gd, "Model"))
			return;
		gd.addStringField("LabelTransformationList","1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ii = new InterpolatedImage(image);
		labels = new InterpolatedImage(
				WindowManager.getImage(gd.getNextChoice()));
		model = new InterpolatedImage(
				WindowManager.getImage(gd.getNextChoice()));
		matrix = FloatMatrix.parseMatrices(gd.getNextString());

		init();
		run();
	}

	long[] volumes;

	void init() {
		// find centers
		center = new int[256][3];
		volumes = new long[256];
		InterpolatedImage.Iterator iter = labels.iterator(false);
		while (iter.next() != null) {
			int value = labels.getNoInterpol(iter.i,
					iter.j, iter.k);
			center[value][0] += iter.i;
			center[value][1] += iter.j;
			center[value][2] += iter.k;
			volumes[value]++;
		}
		for (labelCount = 256; labelCount > 1 &&
				volumes[labelCount - 1] == 0; labelCount--);
		for (int i = 0; i < labelCount; i++)
			if (volumes[i] > 0)
				for (int j = 0; j < 3; j++)
					center[i][j] /= volumes[i];
			else
				matrix[i] = null;

		// adjust matrices to pixel coordinates
		FloatMatrix fromTemplate = FloatMatrix.fromCalibration(ii.image);
		FloatMatrix toModel = FloatMatrix.fromCalibration(model.image).inverse();

		for (int i = 1; i < matrix.length; i++)
			if (matrix[i] != null)
				matrix[i] = toModel.times(matrix[i].inverse().times(fromTemplate));
				//matrix[i] = fromTemplate.inverse().times(matrix[i].inverse().times(toModel.inverse()));
	}

	float x, y, z;

	void transCoord(int i, int j, int k) {
		float total = 0.0f;
		x = y = z = 0;
		for (int l = 0; l < labelCount; l++) {
			if (matrix[l] == null)
				continue;
			
			matrix[l].apply(i, j, k);
			float x = matrix[l].x;
			float y = matrix[l].y;
			float z = matrix[l].z;

			int xdiff = center[l][0] - i;
			int ydiff = center[l][1] - j;
			int zdiff = center[l][2] - k;
			
			float factor = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
			factor = 1.0f / (factor + 0.01f);
			
			this.x += x * factor;
			this.y += y * factor;
			this.z += z * factor;
			
			total += factor;
		}
		x /= total;
		y /= total;
		z /= total;
		//System.err.println("result: " + x + ", " + y + ", " + z);
	}

	void run() {

		InterpolatedImage.Iterator iter = ii.iterator(true);
		while (iter.next() != null) {
			transCoord(iter.i, iter.j, iter.k);
			ii.set(iter.i, iter.j, iter.k, (int)model.interpol.get(x, y, z));
		}
		new AmiraParameters(model.image).setParameters(ii.image);
		ii.image.updateAndDraw();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}

}


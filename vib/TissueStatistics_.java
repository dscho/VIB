package vib;

import amira.AmiraParameters;
import amira.AmiraTable;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class TissueStatistics_ implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		AmiraTable table = calculateStatistics(image);
		table.show();
	}

	public static AmiraTable calculateStatistics(ImagePlus labelfield) {
		if (!AmiraParameters.isAmiraLabelfield(labelfield)) {
			IJ.error("Need a labelfield!");
			return null;
		}
		String title = "Statistics for " + labelfield.getTitle();
		String headings = "Nr\tMaterial\tCount\tVolume\t" + 
							"CenterX\tCenterY\tCenterZ\t" + 
							"MinX\tMaxX\tMinY\tMaxY\tMinZ\tMaxZ";

		InterpolatedImage ii = new InterpolatedImage(labelfield);
		Statistics stat = new Statistics(ii);

		AmiraTable table = new AmiraTable(title, headings,
				stat.getResult(), true);
		return table;
	}

	private static class Statistics {
		Calibration cal;
		AmiraParameters parameters;
		String[] materials;
		long[] count, cX, cY, cZ;
		int[] minX, maxX, minY, maxY, minZ, maxZ;

		public Statistics(InterpolatedImage ii) {
			cal = ii.image.getCalibration();
			parameters = new AmiraParameters(ii.image);
			materials = parameters.getMaterialList();
			count = new long[materials.length];
			cX = new long[materials.length];
			cY = new long[materials.length];
			cZ = new long[materials.length];
			minX = new int[materials.length];
			maxX = new int[materials.length];
			minY = new int[materials.length];
			maxY = new int[materials.length];
			minZ = new int[materials.length];
			maxZ = new int[materials.length];

			for (int i = 0; i < materials.length; i++)
				minX[i] = minY[i] = minZ[i] = Integer.MAX_VALUE;

			doit(ii);
		}

		public void doit(InterpolatedImage ii) {
			InterpolatedImage.Iterator iter = ii.iterator(true);
			while (iter.next() != null) {
				int v = ii.getNoInterpol(iter.i,
						iter.j, iter.k);
				count[v]++;
				cX[v] += iter.i;
				cY[v] += iter.j;
				cZ[v] += iter.k;
				if (minX[v] > iter.i)
					minX[v] = iter.i;
				else if (maxX[v] < iter.i)
					maxX[v] = iter.i;
				if (minY[v] > iter.j)
					minY[v] = iter.j;
				else if (maxY[v] < iter.j)
					maxY[v] = iter.j;
				if (minZ[v] > iter.k)
					minZ[v] = iter.k;
				else if (maxZ[v] < iter.k)
					maxZ[v] = iter.k;
			}
		}

		double x(double i) {
			return cal.xOrigin + (i + 0.5) * cal.pixelWidth;
		}

		double y(double j) {
			return cal.yOrigin + (j + 0.5) * cal.pixelHeight;
		}

		double z(double k) {
			return cal.yOrigin + (k + 0.5) * cal.pixelDepth;
		}

		public String getResult() {
			double voxelVolume = cal.pixelWidth * cal.pixelHeight
				* cal.pixelDepth;
			String result = "";
			for (int i = 0; i < materials.length; i++) {
				result += (i + 1) + "\t";
				result += materials[i] + "\t";
				result += count[i] + "\t";
				if (count[i] == 0) {
					result += "0\t0\t0\t0\t"
						+ "0\t0\t0\t0\t0\t0\n";
					continue;
				}
				result += (count[i] * voxelVolume) + "\t";
				result += x(cX[i] / (double)count[i]) + "\t";
				result += y(cY[i] / (double)count[i]) + "\t";
				result += z(cZ[i] / (double)count[i]) + "\t";
				result += x(minX[i]) + "\t";
				result += x(maxX[i]) + "\t";
				result += y(minY[i]) + "\t";
				result += y(maxY[i]) + "\t";
				result += z(minZ[i]) + "\t";
				result += z(maxZ[i]) + "\n";
			}

			return result;
		}
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}
}


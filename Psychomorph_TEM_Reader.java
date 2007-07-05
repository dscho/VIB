/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * Use this plugin to read PsychoMorph .tem files
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class TEM_Reader implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus ip) {
		image = ip;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor imp) {
		OpenDialog od = new OpenDialog("TEM File", null);
		String dir = od.getDirectory();
		String arg = od.getFileName();
		if(arg==null)
			return;
		try {
			TEMFileParser parser =
				new TEMFileParser(dir + File.separator + arg);
			parser.parse(image);
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	static class TEMFileParser {
		FileReader fileReader;
		BufferedReader reader;

		float[] x, y;
		int[][] segments;

		TEMFileParser(String path) throws IOException {
			fileReader = new FileReader(path);
			reader = new BufferedReader(fileReader);
		}

		float[] readLine() throws IOException {
			String line = reader.readLine();
			if (line == null)
				return null;
			StringTokenizer t = new StringTokenizer(line);
			int count = 0;
			while (t.hasMoreTokens()) {
				t.nextToken();
				count++;
			}
			float[] result = new float[count];
			t = new StringTokenizer(line);
			int i;
			for (i = 0; i < count && t.hasMoreTokens(); i++)
				result[i] = Float.parseFloat(t.nextToken());
			return result;
		}

		void parse(ImagePlus target) throws IOException {
			float[] line = readLine();
			if (line == null)
				return;
			int count = (int)line[0];
			x = new float[count];
			y = new float[count];
			for (int i = 0; i < count; i++) {
				line = readLine();
				if (line == null || line.length < 2)
					throw new IOException("early EOF");
				x[i] = line[0];
				y[i] = line[1];
			}

			// TODO: read segments
			int[] x1 = new int[count];
			int[] y1 = new int[count];
			for (int i = 0; i < count; i++) {
				x1[i] = (int)Math.round(x[i]);
				y1[i] = (int)Math.round(y[i]);
			}
			PointRoi roi = new PointRoi(x1, y1, count);
			target.setRoi(roi);
			target.updateAndDraw();
		}
	}
}



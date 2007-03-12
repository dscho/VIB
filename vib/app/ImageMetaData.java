package vib.app;

/* This class is only a wrapper around AmiraTable for the moment */

import amira.AmiraTable;
import amira.AmiraMeshDecoder;

import ij.text.TextPanel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import vib.FastMatrix;

public class ImageMetaData {
	public static class Material {
		public String name;
		public int count;
		public double volume, centerX, centerY, centerZ;
	}
	public static class Transformation {
		public String name;
		FastMatrix matrix;
	}

	public Material[] materials;
	public Transformation[] transformations;

	public ImageMetaData(String fileName) {
		loadFrom(fileName);
	}

	public FastMatrix getMatrix(String name) {
		for (int i = 0; i < transformations.length; i++)
			if (transformations[i].name.equals(name))
				return transformations[i].matrix;
		return null;
	}

	public void loadFrom(String path) {
		AmiraMeshDecoder decoder = new AmiraMeshDecoder();
		AmiraTable table;
		if (decoder.open(path) &&
				decoder.isTable())
			table = decoder.getTable();
		else
			return;
		TextPanel panel = table.getTextPanel();
		materials = new Material[panel.getLineCount()];
		for (int i = 0; i < materials.length; i++) {
			String[] values = split(panel.getLine(i));
			materials[i] = new Material();
			materials[i].name = values[0];
			materials[i].count = Integer.parseInt(values[1]);
			materials[i].volume = Double.parseDouble(values[2]);
			materials[i].centerX = Double.parseDouble(values[3]);
			materials[i].centerY = Double.parseDouble(values[4]);
			materials[i].centerZ = Double.parseDouble(values[5]);
		}

		Properties props = table.getProperties();
		ArrayList transforms = new ArrayList();
		Enumeration keys = props.keys();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			if (!key.endsWith("Transformation"))
				continue;
			Transformation t = new Transformation();
			t.name = key;
			String matrix = (String)props.get(key);
			t.matrix = FastMatrix.parseMatrix(matrix);
		}

		transformations = new Transformation[transforms.size()];
		for (int i = 0; i < transformations.length; i++)
			transformations[i] = (Transformation)transforms.get(i);
	}

	public void saveTo(String path) {
	}

	private String[] split(String line) {
		ArrayList list = new ArrayList();
		int tab = -1;
		do {
			int lastTab = tab;
			tab = line.indexOf('\t', tab + 1);
			if (tab >= 0)
				list.add(line.substring(lastTab + 1,
						tab - lastTab - 1));
			else
				list.add(line.substring(lastTab + 1));
		} while (tab >= 0);

		String[] result = new String[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)list.get(i);
		return result;
	}
}

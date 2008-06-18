package view4d;

import ij.plugin.PlugIn;
import ij.*;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.ArrayList;

import java.util.Arrays;
import javax.vecmath.Color3f;
import ij3d.Image3DUniverse;
import ij3d.Executer;
import ij3d.Content;

public class Viewer4D {

	String directory = null;
	Image3DUniverse univ;
	Content[] contents = null;
	int current = 0;

	public Viewer4D(Image3DUniverse univ) {
		this.univ = univ;
	}

	public int size() {
		if(contents == null) return 0;
		return contents.length;
	}

	public void faster() {
		if(delay >= 100)
			delay -= 100;
	}

	public void slower() {
		delay += 100;
	}

	private boolean shouldPause = false;
	private int delay = 1000;
	public void play() {
		new Thread(new Runnable() {
			public void run() {
				while(!shouldPause) {
					if(current < contents.length - 1)
						next();
					else
						first();
					try {
						Thread.currentThread()
							.sleep(delay);
					} catch(Exception e) {
						shouldPause = false;
					}
				}
				shouldPause = false;
			}
		}).start();
	}

	public void pause() {
		shouldPause = true;
	}

	public void next() {
		if(contents == null || contents.length == 0)
			return;
		if(current == contents.length - 1)
			return;
		contents[current].setVisible(false);
		current++;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
	}

	public void previous() {
		if(contents == null || contents.length == 0)
			return;
		if(current == 0)
			return;
		contents[current].setVisible(false);
		current--;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
	}

	public void first() {
		if(contents == null || contents.length == 0)
			return;
		if(current == 0)
			return;
		contents[current].setVisible(false);
		current = 0;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
	}

	public void last() {
		if(contents == null || contents.length == 0)
			return;
		if(current == contents.length - 1)
			return;
		contents[current].setVisible(false);
		current = contents.length - 1;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
	}

	public void releaseContents() {
		pause();
		directory = null;
		// remove all except the current content
		for(int i = 0; i < contents.length; i++) {
			if(i != current)
				univ.removeContent(contents[i].getName());
		}
		contents = null;
		current = 0;
		univ.setStatus("");
	}

	public void loadContents() {

		// remove all contents from the universe
		univ.removeAllContents();
		
		GenericDialog gd = new GenericDialog("Load time lapse");

		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Load");
		p.add(b);
		gd.addPanel(p);

		gd.addStringField("Time lapse dir", "", 30);
		final TextField folder = (TextField)gd.getStringFields().get(0);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DirectoryChooser dc = new DirectoryChooser(
							"Time lapse dir");
				String dir = dc.getDirectory();
				if (null == dir) return;
				folder.setText(dir);
			}
		});

		String[] types = new String[] {
			"Volume", "Orthoslice", "Surface"};
		int threshold = 0;
		int resf = 2;
		int type = 0;

		gd.addChoice("Display as", types, types[0]);
		gd.addNumericField("Threshold", threshold, 0);
		gd.addNumericField("Resampling factor", resf, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		directory = folder.getText();
		directory = directory.replace('\\', '/');
		if (!directory.endsWith("/"))
			directory += "/";
		type = (int)gd.getNextChoiceIndex();
		threshold = (int)gd.getNextNumber();
		resf = (int)gd.getNextNumber();
		
		File dir = new File(directory);
		if(!dir.exists())
			return;

		// get the file names
		String[] names = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("tif");
			}
		});

		if (names.length == 0) {
			IJ.showMessage("No files!");
			return;
		}
		Arrays.sort(names);
		Content[] c = new Content[names.length];

		int i = 0;
		for(int j = 0; j < names.length; j++) {
			univ.setStatus("Loading " + (j+1) + "/" + names.length);
			ImagePlus image = IJ.openImage(directory + names[j]);
			if(image == null)
				continue;
			Executer.convert(image);
			IJ.showStatus("adding " + image);
			c[i] = univ.addContent(image, null, names[j],
				threshold, new boolean[] {true, true, true}, 
				resf, type);

			if(i != 0) c[i].setVisible(false);
			i++;
		}

		contents = new Content[i];
		System.arraycopy(c, 0, contents, 0, contents.length);
		univ.setStatus("");
		current = 0;
	}
}

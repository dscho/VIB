package view4d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij3d.Content;
import ij3d.Executer;
import ij3d.Image3DUniverse;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

/**
 * Implements the functionality for the 4D viewer, like loading
 * and animation.
 *
 * @author Benjamin Schmid
 */
public class Viewer4D {

	private String directory = null;
	private Image3DUniverse univ;
	private Content[] contents = null;
	private int current = 0;

	/**
	 * Initialize a new Viewer4D.
	 * @param univ
	 */
	public Viewer4D(Image3DUniverse univ) {
		this.univ = univ;
	}

	/**
	 * Returns the number of time points.
	 * @return
	 */
	public int size() {
		if(contents == null) return 0;
		return contents.length;
	}

	/**
	 * Speed up the animation.
	 */
	public void faster() {
		if(delay >= 100)
			delay -= 100;
	}

	/**
	 * Slows the animation down.
	 */
	public void slower() {
		delay += 100;
	}

	private boolean shouldPause = false;
	private int delay = 1000;

	/**
	 * Start animation.
	 */
	public synchronized void play() {
		new Thread(new Runnable() {
			public void run() {
				while(!shouldPause) {
					if(current < contents.length - 1)
						next();
					else
						first();
					try {
						Thread.sleep(delay);
					} catch(Exception e) {
						shouldPause = false;
					}
				}
				shouldPause = false;
			}
		}).start();
	}

	/**
	 * Stop/pause animation
	 */
	public synchronized void pause() {
		shouldPause = true;
	}

	/**
	 * Display next timepoint.
	 */
	public void next() {
		if(contents == null || contents.length == 0)
			return;
		if(current == contents.length - 1)
			return;
		contents[current].setVisible(false);
		current++;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
		univ.fireContentChanged(contents[current]);
	}

	/**
	 * Display previous timepoint.
	 */
	public void previous() {
		if(contents == null || contents.length == 0)
			return;
		if(current == 0)
			return;
		contents[current].setVisible(false);
		current--;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
		univ.fireContentChanged(contents[current]);
	}

	/**
	 * Display first timepoint.
	 */
	public void first() {
		if(contents == null || contents.length == 0)
			return;
		if(current == 0)
			return;
		contents[current].setVisible(false);
		current = 0;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
		univ.fireContentChanged(contents[current]);
	}

	/**
	 * Display last timepoint.
	 */
	public void last() {
		if(contents == null || contents.length == 0)
			return;
		if(current == contents.length - 1)
			return;
		contents[current].setVisible(false);
		current = contents.length - 1;
		contents[current].setVisible(true);
		univ.setStatus((current+1) + "/" + contents.length);
		univ.fireContentChanged(contents[current]);
	}

	/**
	 * Releases all loaded contents but the currently displayed one.
	 */
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

	/**
	 * Opens a dialog, which asks the user for a directory with Contents
	 * to load, and loads them
	 *
	 * @return false, if something went wrong and the Contents could
	 * not be loaded.
	 */
	public boolean loadContents() {

		// remove all contents from the universe
		univ.removeAllContents();

		GenericDialog gd = new GenericDialog("Load time lapse");

		gd.addMessage("Select either a file containing a hyperstack\n" +
				"or a directory containing your time lapse " +
				"data");

		Panel p = new Panel(new FlowLayout());
		Label l = new Label("Time lapse data");
		final TextField folder = new TextField(30);
		Button b = new Button("...");
		p.add(l);
		p.add(folder);
		p.add(b);
		gd.addPanel(p);

		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OpenDialog dc = new OpenDialog(
						"Time lapse dir", null);
				String dir = dc.getDirectory();
				String file = dc.getFileName();
				if(dir == null || file == null)
					return;
				folder.setText(new File(dir, file).
					getAbsolutePath());
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
			return false;

		directory = folder.getText();
		directory = directory.replace('\\', '/');
		if (!directory.endsWith("/"))
			directory += "/";
		type = (int)gd.getNextChoiceIndex();
		threshold = (int)gd.getNextNumber();
		resf = (int)gd.getNextNumber();

		File dir = new File(directory);
		if(!dir.exists()) {
			IJ.showMessage(directory + " does not exist");
			return false;
		}

		ImagePlus[] images = dir.isDirectory()
			? getImages(dir)
			: getImages(IJ.openImage(dir.getAbsolutePath()));
		if(images == null || images.length == 0)
			return false;
		if(images.length == 0) {
			IJ.showMessage("Could not load any of the images");
			return false;
		}

		load(images, threshold, resf, type);
		return true;
	}

	/**
	 * Loads the specified images, each as an individual Content with
	 * the specified threshold, resampling factor and type.
	 * @param images
	 * @param thresh
	 * @param resf
	 * @param type
	 */
	public void load(ImagePlus[] images, int thresh, int resf, int type) {
		Content[] c = new Content[images.length];

		for(int j = 0; j < images.length; j++) {
			univ.setStatus("Adding " + (j+1) + "/" + images.length);
			ImagePlus image = images[j];
			Executer.convert(image);
			c[j] = univ.addContent(image, null, image.getTitle(),
				thresh, new boolean[] {true, true, true},
				resf, type);
			c[j].showCoordinateSystem(false);

			if(j != 0) c[j].setVisible(false);
		}

		contents = new Content[images.length];
		System.arraycopy(c, 0, contents, 0, contents.length);
		univ.setStatus("");
		current = 0;
	}

	/**
	 * Get an array of images of the specified image, which is assumed to
	 * be a hyperstack. The hyperstack should contain of only one channes,
	 * with the different images as different frames.
	 * @param imp
	 * @return
	 */
	public ImagePlus[] getImages(ImagePlus imp) {
		int nChannels = imp.getNChannels();
		if(nChannels != 1) {
			IJ.showMessage(
				"Currently, images with one channel are\n" +
				"supported.");
			return null;
		}

		int nSlices = imp.getNSlices();
		int nFrames = imp.getNFrames();
		ImagePlus[] ret = new ImagePlus[nFrames];
		int w = imp.getWidth(), h = imp.getHeight();

		ImageStack oldStack = imp.getStack();
		String oldTitle = imp.getTitle();
		for(int i = 0, slice = 1; i < nFrames; i++) {
			ImageStack newStack = new ImageStack(w, h);
			for(int j = 0; j < nSlices; j++, slice++) {
				newStack.addSlice(
					oldStack.getSliceLabel(slice),
					oldStack.getPixels(slice));
			}
			ret[i] = new ImagePlus(oldTitle
				+ " (frame " + i + ")", newStack);
		}
		return ret;
	}

	/**
	 * First sorts all the files in the specified directory alphabetically
	 * and then tries to load each of them, failing silently if an image
	 * can not be opened by IJ.openImage().
	 * @param dir
	 * @return
	 */
	public ImagePlus[] getImages(File dir) {
		if(!dir.isDirectory())
			return null;
		// get the file names
		String[] names = dir.list();
		if (names.length == 0) {
			IJ.showMessage("No files in " + dir.getName());
			return null;
		}
		Arrays.sort(names);
		ImagePlus[] ret = new ImagePlus[names.length];
		for(int i = 0, j = 0; i < ret.length; i++) {
			univ.setStatus("Loading " + (j+1) + "/" + names.length);
			File f = new File(dir, names[i]);
			ImagePlus imp = IJ.openImage(f.getAbsolutePath());
			if(imp != null)
				ret[j++] = imp;
		}
		return ret;
	}
}

package ij3d;

import ij.gui.GenericDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;

import math3d.Transform_IO;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import vib.PointList;
import vib.BenesNamedPoint;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import isosurface.MeshGroup;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;

public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;

	private MenuItem add;
	private MenuItem color;
	private MenuItem channels;
	private MenuItem transparency;
	private MenuItem threshold;
	private MenuItem fill;
	private MenuItem slices;
	private MenuItem delete;
	private MenuItem properties;
	private MenuItem windowSize;
	private MenuItem resetView;
	private MenuItem startRecord;
	private MenuItem stopRecord;
	private MenuItem startAnimation;
	private MenuItem stopAnimation;
	private MenuItem close;
	private MenuItem setTransform;
	private MenuItem resetTransform;
	private MenuItem applyTransform;
	private MenuItem saveTransform;
	private MenuItem exportObj;
	private MenuItem exportDXF;
	private MenuItem smoothMesh;
	private MenuItem scalebar;
	private MenuItem smoothAllMeshes;
	private MenuItem displayAsVolume;
	private MenuItem displayAsOrtho;
	private MenuItem displayAsSurface;
	private MenuItem pl_load;
	private MenuItem regist;
	private MenuItem pl_save;
	private CheckboxMenuItem pl_show;
	private CheckboxMenuItem perspective;
	private CheckboxMenuItem coordinateSystem;
	private CheckboxMenuItem lock;
	private CheckboxMenuItem show;

	private Menu selectedMenu;
	private Menu selectSubMenu;
	private Menu viewMenu;
	private Menu contentsMenu;
	private Menu fileMenu;

	// These strings are the names of the stataic methods in
	// ImageJ3DViewer.
	public static final String START_ANIMATE = "startAnimate";
	public static final String STOP_ANIMATE = "stopAnimate";
	public static final String START_RECORD = "startRecord";
	public static final String STOP_RECORD = "stopRecord";
	public static final String RESET_VIEW = "resetView";
	public static final String SCALEBAR = "scalebar";
	public static final String CLOSE = "close";
	public static final String WINDOW_SIZE = "windowSize";

	public static final String SET_COLOR = "setColor"; 
	public static final String SET_TRANSPARENCY = "setTransparency";
	public static final String SET_CHANNELS = "setChannels";
	public static final String FILL_SELECTION = "fillSelection";
	public static final String SET_SLICES = "setSlices";
	public static final String LOCK = "lock";
	public static final String UNLOCK = "unlock";
	public static final String SET_THRESHOLD = "setThreshold";
	public static final String SET_CS = "setCoordinateSystem";
	public static final String SET_TRANSFORM = "setTransform";
	public static final String APPLY_TRANSFORM = "applyTransform";
	public static final String SAVE_TRANSFORM = "saveTransform";
	public static final String RESET_TRANSFORM = "resetTransform";

	// TODO
	public static final String ADD = "add";
	public static final String DELETE = "delete";

	public static final String SMOOTH = "smooth";

	private List openDialogs = new ArrayList();

	private SelectionListener selListener = new SelectionListener();

	public Image3DMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;

		univ.addUniverseListener(this);

		fileMenu = createFileMenu();
		this.add(fileMenu);

		viewMenu = createViewMenu();
		this.add(viewMenu);
		contentsMenu = createContentsMenu();
		this.add(contentsMenu);
		selectedMenu = createSelectedMenu();
	}

	public Menu createFileMenu() {
		Menu file = new Menu("File");

		exportObj = new MenuItem("Export as WaveFront");
		exportObj.addActionListener(this);
		file.add(exportObj);

		exportDXF = new MenuItem("Export as DXF");
		exportDXF.addActionListener(this);
		file.add(exportDXF);

		return file;
	}

	public Menu createViewMenu() {
		// Viewer
		Menu view = new Menu("View");

		windowSize = new MenuItem("Set window size");
		windowSize.addActionListener(this);
		view.add(windowSize);

		resetView = new MenuItem("Reset view");
		resetView.addActionListener(this);
		view.add(resetView);

		perspective = new CheckboxMenuItem(
					"Perspective Projection", false);
		perspective.addItemListener(this);
		view.add(perspective);

		view.addSeparator();

		startRecord = new MenuItem("Start recording");
		startRecord.addActionListener(this);
		view.add(startRecord);

		stopRecord = new MenuItem("Stop recording");
		stopRecord.addActionListener(this);
		view.add(stopRecord);

		view.addSeparator();

		startAnimation = new MenuItem("Start animation");
		startAnimation.addActionListener(this);
		view.add(startAnimation);

		stopAnimation = new MenuItem("Stop animation");
		stopAnimation.addActionListener(this);
		view.add(stopAnimation);

		view.addSeparator();

		scalebar = new MenuItem("Scalebar");
		scalebar.addActionListener(this);
		view.add(scalebar);

		view.addSeparator();

		close = new MenuItem("Close");
		close.addActionListener(this);
		view.add(close);

		return view;
	}

	public Menu createContentsMenu() {
		// Universe
		Menu universe = new Menu("Contents");
		add = new MenuItem("Add content");
		add.addActionListener(this);
		universe.add(add);

		delete = new MenuItem("Delete");
		delete.setEnabled(false);
		delete.addActionListener(this);
		universe.add(delete);

		universe.addSeparator();

		selectSubMenu = createSelectSubMenu();
		universe.add(selectSubMenu);

		universe.addSeparator();

		regist = new MenuItem("Register");
		regist.addActionListener(this);
		universe.add(regist);

		smoothAllMeshes = new MenuItem("Smooth all meshes");
		smoothAllMeshes.addActionListener(this);
		universe.add(smoothAllMeshes);

		return universe;
	}

	public Menu createSelectSubMenu() {
		Menu select = new Menu("Select");
		if(univ == null)
			return select;
		for(Iterator it = univ.contents(); it.hasNext();) {
			String name = ((Content)it.next()).getName();
			MenuItem mi = new MenuItem(name);
			mi.addActionListener(selListener);
		}
		return select;
	}

	public Menu createPLSubMenu() {
		Menu pl = new Menu("Point list");
		if(univ == null)
			return pl;
		pl_load = new MenuItem("Load Point List");
		pl_load.addActionListener(this);
		pl.add(pl_load);

		pl_save = new MenuItem("Save Point List");
		pl_save.addActionListener(this);
		pl.add(pl_save);

		pl_show = new CheckboxMenuItem("Show Point List");
		pl_show.addItemListener(this);
		pl.add(pl_show);

		return pl;
	}

	public Menu createTransformSubMenu() {
		Menu transform = new Menu("Transformation");

		lock = new CheckboxMenuItem("Lock");
		lock.addItemListener(this);
		transform.add(lock);
		
		setTransform = new MenuItem("Set Transform");
		setTransform.addActionListener(this);
		transform.add(setTransform);

		resetTransform = new MenuItem("Reset Transform");
		resetTransform.addActionListener(this);
		transform.add(resetTransform);

		applyTransform = new MenuItem("Apply Transform");
		applyTransform.addActionListener(this);
		transform.add(applyTransform);

		saveTransform = new MenuItem("Save Transform");
		saveTransform.addActionListener(this);
		transform.add(saveTransform);

		return transform;
	}

	public Menu createHideSubMenu() {
		Menu hide = new Menu("Hide/Show");

		show = new CheckboxMenuItem("Show content");
		show.setState(true);
		show.addItemListener(this);
		hide.add(show);

		coordinateSystem = new CheckboxMenuItem(
					"Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		hide.add(coordinateSystem);

		return hide;
	}

	public Menu createAttributesSubMenu() {
		Menu attributes = new Menu("Attributes");

		channels = new MenuItem("Change channels");
		channels.addActionListener(this);
		attributes.add(channels);

		color = new MenuItem("Change color");
		color.addActionListener(this);
		attributes.add(color);

		transparency = new MenuItem("Change transparency");
		transparency.addActionListener(this);
		attributes.add(transparency);

		threshold = new MenuItem("Adjust threshold");
		threshold.addActionListener(this);
		attributes.add(threshold);
		return attributes;
	}

	public Menu createDisplayAsSubMenu() {
		Menu display = new Menu("Display as");

		displayAsVolume = new MenuItem("Volume");
		displayAsVolume.addActionListener(this);
		display.add(displayAsVolume);

		displayAsOrtho = new MenuItem("Orthoslice");
		displayAsOrtho.addActionListener(this);
		display.add(displayAsOrtho);

		displayAsSurface = new MenuItem("Surface");
		displayAsSurface.addActionListener(this);
		display.add(displayAsSurface);

		return display;
	}

	public Menu createSelectedMenu() {
		// Contents
		Menu content = new Menu("Content");
		
		slices = new MenuItem("Adjust slices");
		slices.addActionListener(this);
		content.add(slices);
		
		fill = new MenuItem("Fill selection");
		fill.addActionListener(this);
		content.add(fill);

		smoothMesh = new MenuItem("Smooth mesh");
		smoothMesh.addActionListener(this);
		content.add(smoothMesh);

		content.addSeparator();
		
		content.add(createDisplayAsSubMenu());
		content.add(createAttributesSubMenu());
		content.add(createHideSubMenu());
		content.add(createPLSubMenu());
		content.add(createTransformSubMenu());

		content.addSeparator();

		properties = new MenuItem("Properties");
		properties.addActionListener(this);
		content.add(properties);

		return content;
	}

	private class SelectionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String name = e.getActionCommand();
			Content c = univ.getContent(name);
			if(c != null)
				univ.select(c);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == color) {
			Content selected = univ.getSelected();
			if(selected == null) {
				IJ.error("Selection required");
				return;
			}	
			changeColor(selected);
			univ.clearSelection();
		}

		if(e.getSource() == scalebar) {
			editScalebar();
		}

		if(e.getSource() == channels) {
			Content selected = univ.getSelected();
			if(selected == null) {
				IJ.error("Selection required");
				return;
			}
			changeChannels(selected);
			univ.clearSelection();
		}

		if(e.getSource() == transparency) {
			Content selected = univ.getSelected();
			if(selected == null) {
				IJ.error("Selection required");
				return;
			}	
			changeTransparency(selected);
			univ.clearSelection();
		}

		if(e.getSource() == add) {
			Content c = addContent(null, -1);
			String[] arg = new String[] {
				c.image.getTitle(), 
				ColorTable.getColorName(c.color),
				c.name, 
				Integer.toString(c.threshold),
				Boolean.toString(c.channels[0]), 
				Boolean.toString(c.channels[1]),
				Boolean.toString(c.channels[2]),
				Integer.toString(c.resamplingF),
				Integer.toString(c.type)};
			record(ADD, arg);
		}

		if(e.getSource() == delete) {
			record(DELETE);
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			univ.removeContent(c.name);
		}

		if(e.getSource() == regist) {
			regist();
		}
	
		if(e.getSource() == resetView) {
			record(RESET_VIEW);
			univ.resetView();
		}

		if(e.getSource() == windowSize) {
			setWindowSize();
		}
		
		if(e.getSource() == startRecord) {
			record(START_RECORD);
			univ.startRecording();
		}

		if(e.getSource() == stopRecord) {
			record(STOP_RECORD);
			ImagePlus movie = univ.stopRecording();
			if(movie != null) movie.show();
		}

		if(e.getSource() == startAnimation) {
			record(START_ANIMATE);
			univ.startAnimation();
		}

		if(e.getSource() == stopAnimation) {
			record(STOP_ANIMATE);
			univ.pauseAnimation();
		}

		if(e.getSource() == threshold) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			adjustThreshold(c);
			univ.clearSelection();
		}

		if(e.getSource() == displayAsVolume) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.displayAs(Content.VOLUME);
			univ.clearSelection();
		}

		if(e.getSource() == displayAsOrtho) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.displayAs(Content.ORTHO);
			univ.clearSelection();
		}

		if(e.getSource() == displayAsSurface) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.displayAs(Content.SURFACE);
			univ.clearSelection();
		}


		if(e.getSource() == slices) {
			Content c = univ.getSelected();
			if(c == null || c.getType() != Content.ORTHO) {
				IJ.error("Orthoslices must be selected");
				return;
			}
			adjustSlices(c);
			univ.clearSelection();
		}

		if(e.getSource() == fill) {
			final Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			if(c.getType() == Content.VOLUME) {
				new Thread(new Runnable() {
					public void run() {
						((VoltexGroup)c.getContent()).
						fillRoiBlack(univ, (byte)0);
						univ.fireContentChanged(c);
						record(FILL_SELECTION);
					}
				}).start();
			}
		}

		if(e.getSource() == close) {
			univ.close();
			record(CLOSE);
		}

		if(e.getSource() == resetTransform) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			if(c.isLocked()) {
				IJ.error(c.name + " is locked");
				return;
			}
			univ.fireTransformationStarted();
			c.setTransform(new Transform3D());
			univ.fireTransformationFinished();
			record(RESET_TRANSFORM);
		}

		if(e.getSource() == setTransform) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			if(c.isLocked()) {
				IJ.error(c.name + " is locked");
				return;
			}
			univ.fireTransformationStarted();
			float[] t = readTransform(c);
			if(t != null) {
				c.setTransform(new Transform3D(t));
				univ.fireTransformationFinished();
			}
			record(SET_TRANSFORM, affine2string(t));
		}

		if(e.getSource() == properties) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			TextWindow tw = new TextWindow(c.getName(), 
				" \tx\ty\tz",
				"min\t" + (float)c.getContent().min.x + "\t"
					+ (float)c.getContent().min.y + "\t"
					+ (float)c.getContent().min.z + "\n" +
				"max\t" + (float)c.getContent().max.x + "\t"
					+ (float)c.getContent().max.y + "\t"
					+ (float)c.getContent().max.z + "\n" +
				"cog\t" + (float)c.getContent().center.x + "\t"
					+ (float)c.getContent().center.y + "\t"
					+ (float)c.getContent().center.z + "\n\n" +
				"volume\t" + c.getContent().getVolume(),
				512, 512);
		}

		if(e.getSource() == applyTransform) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			if(c.isLocked()) {
				IJ.error(c.name + " is locked");
				return;
			}
			univ.fireTransformationStarted();
			float[] t = readTransform(univ.getSelected());
			if(t != null) {
				c.applyTransform(new Transform3D(t));
				univ.fireTransformationFinished();
			}
			record(APPLY_TRANSFORM, affine2string(t));
		}

		if(e.getSource() == saveTransform) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			Transform3D t1 = new Transform3D();
			c.getLocalTranslate().getTransform(t1);
			Transform3D t2 = new Transform3D();
			c.getLocalRotate().getTransform(t2);
			t1.mul(t2);
			float[] matrix = new float[16];
			t1.get(matrix);
			if(new Transform_IO().saveAffineTransform(matrix))
				record(SAVE_TRANSFORM, affine2string(matrix));
		}

		if (e.getSource() == pl_load) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.loadPointList();
		}
		if (e.getSource() == pl_save) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.savePointList();
		}
		if (e.getSource() == exportDXF) {
			MeshExporter.saveAsDXF(univ.getContents());
		}
		if (e.getSource() == exportObj) {
			MeshExporter.saveAsWaveFront(univ.getContents());
		}
		if (e.getSource() == smoothMesh) {
			Content c = univ.getSelected();
			if (null == c) {
				IJ.error("Selection required");
				return;
			}
			MeshEditor.smooth(c, 0.25f);
			// needs studying first // record(SMOOTH);
		}
		if (e.getSource() == smoothAllMeshes) {
			// process each Mesh in a separate thread
			final Collection all = univ.getContents();
			final Content[] c = new Content[all.size()];
			all.toArray(c);
			final AtomicInteger ai = new AtomicInteger(0);
			final Thread[] thread = new Thread[Runtime.getRuntime().availableProcessors()];
			for (int i = 0; i<thread.length; i++) {
				thread[i] = new Thread() {
					public void run() {
						try {
							for (int k = ai.getAndIncrement(); k < c.length; k = ai.getAndIncrement()) {
								MeshEditor.smooth(c[k], 0.25f); // will ignore non-mesh Content instances
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				thread[i].start();
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == perspective) {
			int policy = perspective.getState() 
						? View.PERSPECTIVE_PROJECTION 
						: View.PARALLEL_PROJECTION;
			univ.getViewer().getView().setProjectionPolicy(policy);
		}

		if(e.getSource() == coordinateSystem) {
			if(univ.getSelected() == null) {
				IJ.error("Selection required");
				return;
			}
			boolean b = coordinateSystem.getState();
			univ.getSelected().showCoordinateSystem(b);
			record(SET_CS, Boolean.toString(b));
		}

		if(e.getSource() == show) {
			if(univ.getSelected() == null) {
				IJ.error("Selection required");
				return;
			}
			boolean b = show.getState();
			univ.getSelected().setVisible(b);
			if(!b)
				univ.clearSelection();
		}

		if(e.getSource() == lock) {
			Content selected = univ.getSelected();
			if(selected == null) {
				IJ.error("Selection required");
				return;
			}
			selected.toggleLock();
			if(selected.isLocked())
				record(LOCK);
			else
				record(UNLOCK);
		}

		if (e.getSource() == pl_show) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			c.showPointList(pl_show.getState());
		}
	}

	public void setWindowSize() {
		final GenericDialog gd = new GenericDialog("Window size...");
		Dimension d = univ.getSize();
		if(d == null)
			return;
		gd.addNumericField("width", d.width, 0);
		gd.addNumericField("height", d.height, 0);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
		univ.setSize((int)gd.getNextNumber(), (int)gd.getNextNumber());
	}

	public void editScalebar() {
		Scalebar sc = univ.getScalebar();
		final GenericDialog gd = new GenericDialog("Edit scalebar...");
		gd.addNumericField("x position", sc.getX(), 2);
		gd.addNumericField("y position", sc.getY(), 2);
		gd.addNumericField("length", sc.getLength(), 2);
		gd.addStringField("Units", sc.getUnit(), 5);
		gd.addChoice("Color", ColorTable.colorNames, 
				ColorTable.getColorName(sc.getColor()));
		gd.addCheckbox("show", sc.isVisible());
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
		sc.setPosition((float)gd.getNextNumber(), 
				(float)gd.getNextNumber());
		sc.setLength((float)gd.getNextNumber());
		sc.setUnit(gd.getNextString());
		sc.setColor(ColorTable.getColor(gd.getNextChoice()));
		sc.setVisible(gd.getNextBoolean());
	}

	public void changeTransparency(final Content selected) {
		final GenericDialog gd = 
				new GenericDialog("Adjust transparency ...");
		final int oldTr = (int)(selected.getTransparency() * 100);
		gd.addSlider("Transparency", 0, 100, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				float newTr = (float)e.getValue() / 100f; 
				selected.setTransparency(newTr);
				univ.fireContentChanged(selected);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				openDialogs.remove(gd);
				if(gd.wasCanceled()) {
					float newTr = (float)oldTr / 100f;
					selected.setTransparency(newTr);
					univ.fireContentChanged(selected);
					return;
				} else {
					record(SET_TRANSPARENCY, Float.
					toString(((Scrollbar)gd.getSliders().
					get(0)).getValue() / 100f));
				}
			}
		});
		openDialogs.add(gd);
		gd.showDialog();
		
	}

	public void adjustThreshold(final Content selected) {
		final int oldTr = (int)(selected.getThreshold());
		if(selected.getType() == Content.SURFACE) {
			int th = (int)Math.round(
				IJ.getNumber("Threshold [0..255]", oldTr));
			th = Math.max(0, th);
			th = Math.min(th, 255);
			selected.setThreshold(th);
			univ.fireContentChanged(selected);
			record(SET_THRESHOLD, Integer.toString(th));
			return;
		}
		// in case we've not a mesh, change it interactively
		final GenericDialog gd = 
				new GenericDialog("Adjust threshold...");
		gd.addSlider("Transparency", 0, 255, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				int newTr = (int)e.getValue();
				selected.setThreshold(newTr);
				univ.fireContentChanged(selected);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				openDialogs.remove(gd);
				if(gd.wasCanceled()) {
					selected.setThreshold(oldTr);
					univ.fireContentChanged(selected);
					return;
				} else {
					record(SET_THRESHOLD, 
						Integer.toString(
							selected.threshold));
				}
			}
		});
		openDialogs.add(gd);
		gd.showDialog();
	}

	private Thread updatingThread = null;
	private boolean stopUpdating = false;

	public void adjustSlices(final Content selected) {
		final GenericDialog gd = new GenericDialog("Adjust slices...");
		final OrthoGroup os = (OrthoGroup)selected.getContent();
		final int[] oldvalues = os.getSlices();
		ImagePlus imp = selected.image;
		int w = imp.getWidth() / selected.getResamplingFactor();
		int h = imp.getHeight() / selected.getResamplingFactor();
		int d = imp.getStackSize() / selected.getResamplingFactor();

		gd.addSlider("x", 0, w-1, oldvalues[0]);
		gd.addSlider("y", 0, h-1, oldvalues[1]);
		gd.addSlider("z", 0, d-1, oldvalues[2]);

		final Scrollbar xSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar ySlider = (Scrollbar)gd.getSliders().get(1);
		final Scrollbar zSlider = (Scrollbar)gd.getSliders().get(2);

// 		AdjustmentListener listener = new AdjustmentListener() {
// 			public void adjustmentValueChanged(AdjustmentEvent e) {
// 				os.setSlices(
// 					xSlider.getValue(), 
// 					ySlider.getValue(), 
// 					zSlider.getValue());
// 				univ.fireContentChanged(
// 					selected);
// 			}
// 		};
		xSlider.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				os.setXSlice(xSlider.getValue()); 
				univ.fireContentChanged(selected);
			}
		});

		ySlider.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				os.setYSlice(ySlider.getValue()); 
				univ.fireContentChanged(selected);
			}
		});

		zSlider.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				os.setZSlice(zSlider.getValue()); 
				univ.fireContentChanged(selected);
			}
		});


		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				openDialogs.remove(gd);
				if(gd.wasCanceled()) {
					os.setSlices(
						oldvalues[0], 
						oldvalues[1], 
						oldvalues[2]);
					univ.fireContentChanged(selected);
					return;
				} else {
					record(SET_SLICES, 
					Integer.toString(xSlider.getValue()), 
					Integer.toString(ySlider.getValue()),
					Integer.toString(zSlider.getValue()));
				}
			}
		});
		openDialogs.add(gd);
		gd.showDialog();
	}

	public void changeColor(final Content selected) {
		final GenericDialog gd = new GenericDialog("Adjust color ...");
		final Color3f oldC = selected.color;

		gd.addCheckbox("Use default color", oldC == null);
		gd.addSlider("Red",0,255,oldC == null ? 255 : oldC.x*255);
		gd.addSlider("Green",0,255,oldC == null ? 0 : oldC.y*255);
		gd.addSlider("Blue",0,255,oldC == null ? 0 : oldC.z*255);

		final Scrollbar rSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar gSlider = (Scrollbar)gd.getSliders().get(1);
		final Scrollbar bSlider = (Scrollbar)gd.getSliders().get(2);
		final Checkbox cBox = (Checkbox)gd.getCheckboxes().get(0);

		rSlider.setEnabled(oldC != null);
		gSlider.setEnabled(oldC != null);
		bSlider.setEnabled(oldC != null);

		cBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				rSlider.setEnabled(!cBox.getState());
				gSlider.setEnabled(!cBox.getState());
				bSlider.setEnabled(!cBox.getState());
				selected.setColor(cBox.getState() ? null :
					new Color3f(rSlider.getValue() / 255f, 
						gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
				gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				univ.fireContentChanged(selected);
			}
		});

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				selected.setColor(new Color3f(
					rSlider.getValue() / 255f, 
					gSlider.getValue() / 255f, 
					bSlider.getValue() / 255f));
				univ.fireContentChanged(selected);
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				openDialogs.remove(gd);
				if(gd.wasCanceled()) {
					selected.setColor(oldC);
					univ.fireContentChanged(selected);
					return;
				} else if(cBox.getState()){
					record(SET_COLOR,
						"null", "null", "null");
				} else {
					record(SET_COLOR, 
					Integer.toString(rSlider.getValue()), 
					Integer.toString(gSlider.getValue()),
					Integer.toString(bSlider.getValue()));
				}
			}
		});
		openDialogs.add(gd);
		gd.showDialog();
		
	}

	public void changeChannels(Content selected) {
		GenericDialog gd = new GenericDialog("Adjust channels ...");
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
				new String[] {"red", "green", "blue"}, 
				selected.getChannels());
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
			
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		selected.setChannels(channels);
		univ.fireContentChanged(selected);
		record(SET_CHANNELS, Boolean.toString(channels[0]),
			Boolean.toString(channels[1]),
			Boolean.toString(channels[2]));
	}

	public static void record(String command) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command);
		}
	}
	
	public static void record(String command, String a1) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command, a1);
		}
	}

	public static void record(String command, String a1, String a2) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call",command,a1,a2);
		}
	}

	public static void record(String command, String a1, String a2, String a3) {
		command = "call(\"ImageJ_3D_Viewer." + command + "\", " + 
				"\"" + a1 + "\"," + 
				"\"" + a2 + "\"," + 
				"\"" + a3 + "\")"; 
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.recordString(command);
		}
	}

	public static void record(String command, String[] args) {
		command = "call(\"ImageJ_3D_Viewer." + command + "\"";
		for(int i = 0; i < args.length; i++) {
			command += ", \"" + args[i] + "\"";
		}
		command += ");";
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.recordString(command);
		}
	}

	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}

	public void universeClosed() {
		closeAllDialogs();
	}

	public void contentAdded(Content c) {
		if(c == null)
			return;
		MenuItem item = new MenuItem(c.getName());
		item.addActionListener(selListener);
		selectSubMenu.add(item);
	}

	public void contentRemoved(Content c) {
		if(c == null)
			return;
		for(int i = 0; i < selectSubMenu.getItemCount(); i++) {
			MenuItem item = selectSubMenu.getItem(i);
			if(item.getLabel().equals(c.getName())) {
				selectSubMenu.remove(i);
				return;
			}
		}
	}

	public void contentSelected(Content c) {
		delete.setEnabled(c != null);
		if(c == null) {
			remove(selectedMenu);
			return;
		}	
		selectedMenu.setLabel(c.getName());
		if(!containsSelectedMenu())
			add(selectedMenu);
		
		slices.setEnabled(c.getType() == Content.ORTHO);
		fill.setEnabled(c.getType() == Content.VOLUME);

		coordinateSystem.setState(c.hasCoord());
		lock.setState(c.isLocked());
		show.setState(c.isVisible());
		pl_show.setState(c.isPLVisible());

		displayAsVolume.setEnabled(c.getType() != Content.VOLUME);
		displayAsOrtho.setEnabled(c.getType() != Content.ORTHO);
		displayAsSurface.setEnabled(c.getType() != Content.SURFACE);
	}

	private boolean containsSelectedMenu() {
		for(int i = 0; i < getMenuCount(); i++) {
			if(getMenu(i) == selectedMenu) {
				return true;
			}
		}
		return false;
	}

	private float[] readTransform(Content selected) {
		final GenericDialog gd = new GenericDialog(
					"Read transformation", null);
		Transform3D t1 = new Transform3D();
		selected.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		selected.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		float[] matrix = new float[16];
		t1.get(matrix);
		String transform = affine2string(matrix);
		gd.addStringField("Transformation", transform, 25);
		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Open from file");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float[] m = new Transform_IO().
						openAffineTransform();
				if(m != null) {
					TextField tf = (TextField)gd.
						getStringFields().get(0);
					tf.setText(affine2string(m));
					tf.repaint();
				}
			}
		});
		p.add(b);
		gd.addPanel(p);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return null;

		transform = gd.getNextString();
		float[] m = string2affine(transform);
		return m;
	}

	private String affine2string(float[] matrix) {
		String transform = "";
		for(int i = 0; i < matrix.length; i++) {
			transform += matrix[i] + " ";
		}
		return transform;
	}

	private float[] string2affine(String transform){
		String[] s = ij.util.Tools.split(transform);
		float[] m = new float[s.length];
		for(int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		return m;
	}

	private static final int getAutoThreshold(ImagePlus imp) {
		int[] histo = new int[256];
		int d = imp.getStackSize();
		for(int z = 0; z < d; z++) {
			byte[] p = (byte[])imp.getStack().getPixels(z+1);
			for(int i = 0; i < p.length; i++) {
				histo[(int)(p[i]&0xff)]++;
			}
		}
		return imp.getProcessor().getAutoThreshold(histo);
	}

	public Content addContent(ImagePlus image, int type) {
		// setup default values
		int img_count = WindowManager.getImageCount();
		Vector windows = new Vector();
		String[] images;
		for(int i=1; i<=img_count; i++) {
			int id = WindowManager.getNthImageID(i);
			ImagePlus imp = WindowManager.getImage(id);
			if(imp != null){
				 windows.add(imp.getTitle());
			}
		}
		if(windows.size() == 0)
			IJ.error("No images open");
		images = (String[])windows.toArray(new String[]{});
		String name = image == null ? images[0] : image.getTitle();
		String[] types = new String[] {
				"Volume", "Orthoslice", "Surface"};
		type = type < 0 ? 0 : type;
		int threshold = type == Content.SURFACE ? 50 : 0;
		int resf = 2;

		// create dialog
		GenericDialog gd = new GenericDialog("Add ...");
		gd.addChoice("Image", images, name);
		gd.addStringField("Name", name, 10);
		gd.addChoice("Display as", types, types[type]);
		gd.addChoice("Color", ColorTable.colorNames, 
						ColorTable.colorNames[0]);
		gd.addNumericField("Threshold", threshold, 0);
		gd.addNumericField("Resampling factor", resf, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
				new String[] {"red", "green", "blue"}, 
				new boolean[]{true, true, true});


		// automatically set threshold if surface is selected
		final TextField tf = (TextField)gd.getNumericFields().get(0);
		final Choice ch = (Choice)gd.getChoices().get(1);
		ch.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(ch.getSelectedIndex() == Content.SURFACE)
					tf.setText(Integer.toString(50));
				else
					tf.setText(Integer.toString(0));
			}
		});
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return null;
			
		image = WindowManager.getImage(gd.getNextChoice());
		name = gd.getNextString();
		type = gd.getNextChoiceIndex();
		Color3f color = ColorTable.getColor(gd.getNextChoice());
		threshold = (int)gd.getNextNumber();
		resf = (int)gd.getNextNumber();
		boolean[] channels = new boolean[] {gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};

		if(univ.contains(name)) {
			IJ.error("Could not add new content. A content with " +
				"name \"" + name + "\" exists already.");
			return null;
		}

		return univ.addContent(image, color, 
				name, threshold, channels, resf, type);
	}

	public void regist() {
		Collection contents = univ.getContents();
		if(contents.size() < 2) {
			IJ.error("At least two bodies are required for " +
				" registration");
			return;
		}
		String[] conts = new String[contents.size()];
		int i = 0;
		for(Iterator it = contents.iterator(); it.hasNext();)
			conts[i++] = ((Content)it.next()).getName();
		GenericDialog gd = new GenericDialog("Registration");
		gd.addChoice("template", conts, conts[0]);
		gd.addChoice("model", conts, conts[1]);
		gd.addCheckbox("allow scaling", true);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
		Content templ = univ.getContent(gd.getNextChoice());
		Content model = univ.getContent(gd.getNextChoice());
		boolean scaling = gd.getNextBoolean();
		PointList tpoints = templ.getPointList();
		PointList mpoints = model.getPointList();
		if(tpoints.size() < 2 || mpoints.size() < 2) {
			IJ.error("At least two points are required in each "
				+ "of the point lists");
		}
		List sett = new ArrayList();
		List setm = new ArrayList();
		for(i = 0; i < tpoints.size(); i++) {
			BenesNamedPoint pt = tpoints.get(i);
			BenesNamedPoint pm = mpoints.get(pt.getName());
			if(pm != null) {
				sett.add(pt);
				setm.add(pm);
			}
		}
		if(sett.size() < 2) {
			IJ.error("At least two points with the same name "
				+ "must exist in both bodies");
			return;
		}
		IJ.write("Points used for registration");
		for(i = 0; i < sett.size(); i++) {
			BenesNamedPoint p = (BenesNamedPoint)sett.get(i);
			IJ.write(p.getName() + "    " + (float)p.x + "    "
				+ (float)p.y + "    " + (float)p.z);
		}
		BenesNamedPoint[] sm = new BenesNamedPoint[setm.size()];
		BenesNamedPoint[] st = new BenesNamedPoint[sett.size()];
		FastMatrix fm = FastMatrix.bestRigid(
			(BenesNamedPoint[])setm.toArray(sm),
			(BenesNamedPoint[])sett.toArray(st));

		// reset the transformation of the template
		// and set the transformation of the model.
		Transform3D t3d = new Transform3D(fm.rowwise16());
		templ.setTransform(new Transform3D());
		model.setTransform(t3d);
	}

	public void closeAllDialogs() {
		while(openDialogs.size() > 0) {
			GenericDialog gd = (GenericDialog)openDialogs.get(0);
			gd.dispose();
			openDialogs.remove(gd);
		}
	}
}


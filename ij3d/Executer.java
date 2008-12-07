package ij3d;

import ij3d.shapes.Scalebar;
import ij.gui.GenericDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;
import ij.process.StackConverter;
import ij.process.ImageConverter;

import view4d.Viewer4D;
import view4d.Viewer4DController;

import math3d.Transform_IO;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;

import vib.InterpolatedImage;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import voltex.Renderer;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4d;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

import java.util.concurrent.atomic.AtomicInteger;
import octree.FilePreparer;
import octree.OctreeDialog;
import octree.VolumeOctree;

public class Executer {

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

	private Image3DUniverse univ;


	public Executer(Image3DUniverse univ) {
		this.univ = univ;
	}


	/* **********************************************************
	 * File menu
	 * *********************************************************/
	public void addContent(final ImagePlus image, final int type) {
		new Thread() {
			@Override
			public void run() {
				addC(image, type);
			}
		}.start();
	}

	private Content addC(ImagePlus image, int type) {
		// setup default values
		int img_count = WindowManager.getImageCount();
		Vector windows = new Vector();
		String[] images;
		for(int i=1; i<=img_count; i++) {
			int id = WindowManager.getNthImageID(i);
			ImagePlus imp = WindowManager.getImage(id);
			if(imp != null && !imp.getTitle().equals("3d")){
				 windows.add(imp.getTitle());
			}
		}
		if(windows.size() == 0) {
			IJ.error("No images open");
			return null;
		}
		images = (String[])windows.toArray(new String[]{});
		String name = image == null ? images[0] : image.getTitle();
		String[] types = new String[] {
			"Volume", "Orthoslice", "Surface", "Surface Plot 2D"};
		type = type < 0 ? 0 : type;
		int threshold = type == Content.SURFACE ? 50 : 0;
		int resf = 2;

		// create dialog
		GenericDialog gd = new GenericDialog(
					"Add ...", univ.getWindow());
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
		final TextField th = (TextField)gd.getNumericFields().get(0);
		final Choice di = (Choice)gd.getChoices().get(1);
		di.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(di.getSelectedIndex() == Content.SURFACE)
					th.setText(Integer.toString(50));
				else
					th.setText(Integer.toString(0));
			}
		});
		final Choice im = (Choice)gd.getChoices().get(0);
		final TextField na = (TextField)gd.getStringFields().get(0);
		im.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				na.setText(im.getSelectedItem());
			}
		});
		gd.showDialog();
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

		int imaget = image.getType();
		if(imaget != ImagePlus.GRAY8 && imaget != ImagePlus.COLOR_RGB)
			// TODO correct message
			if(IJ.showMessageWithCancel("Convert...", 
				"8-bit image required. Convert?"))
				convert(image);

		Content c = univ.addContent(image, color, 
				name, threshold, channels, resf, type);

		if(c == null)
			return null;
		// record
		String[] arg = new String[] {
			c.image.getTitle(), ColorTable.getColorName(c.color),
			c.name, Integer.toString(c.threshold),
			Boolean.toString(c.channels[0]), 
			Boolean.toString(c.channels[1]),
			Boolean.toString(c.channels[2]),
			Integer.toString(c.resamplingF),
			Integer.toString(c.type)};
		record(ADD, arg);

		return c;
	}

	public void delete(Content c) {
		if(!checkSel(c))
			return;
		univ.removeContent(c.name);
		record(DELETE);
	}

	public void loadOctree() {
		OctreeDialog od = new OctreeDialog();
		od.showDialog();
		if(!od.checkUserInput())
			return;
		String dir = od.getImageDir();
		String name = od.getName();
		String path = od.getImagePath();
		if(od.shouldCreateData()) {
			try {
				new FilePreparer(path, VolumeOctree.SIZE, dir).createFiles();
			} catch(Exception e) {
				IJ.error(e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		univ.addOctree(dir, name);
	}

	public void removeOctree() {
		univ.removeOctree();
	}

	public void load4D() {
		if(!univ.getContents().isEmpty()) {
			// showMessage...() is false if Canceled
			if(!IJ.showMessageWithCancel(
				"Loading 4D data...",
				"All current 3D objects are removed from\n" +
				"the view! Continue?")) {
				return;
			}
		}
		Viewer4D view4d = new Viewer4D(univ);
		if(view4d.loadContents())
			new Viewer4DController(view4d);
	}

	public void saveAsDXF() {
		MeshExporter.saveAsDXF(univ.getContents());
	}

	public void saveAsWaveFront() {
		MeshExporter.saveAsWaveFront(univ.getContents());
	}

	public void close() {
		univ.close();
		record(CLOSE);
	}



	/* **********************************************************
	 * Edit menu
	 * *********************************************************/
	public void changeSlices(final Content c) {
		if(!checkSel(c))
			return;
		final GenericDialog gd = new GenericDialog(
			"Adjust slices...", univ.getWindow());
		final OrthoGroup os = (OrthoGroup)c.getContent();
		final int[] oldvalues = os.getSlices();
		final boolean[] visible = os.getVisible();
		ImagePlus imp = c.image;
		int w = imp.getWidth() / c.getResamplingFactor();
		int h = imp.getHeight() / c.getResamplingFactor();
		int d = imp.getStackSize() / c.getResamplingFactor();

		gd.addCheckbox("Show_yz plane", visible[0]);
		gd.addSlider("x coordinate", 0, w-1, oldvalues[0]);
		gd.addCheckbox("Show_xz plane", visible[1]);
		gd.addSlider("y coordinate", 0, h-1, oldvalues[1]);
		gd.addCheckbox("Show_xy plane", visible[2]);
		gd.addSlider("z coordinate", 0, d-1, oldvalues[2]);

		gd.addMessage(  "You can use the x, y and z key plus\n" +
				"the arrow keys to adjust slices in\n" +
				"x, y and z direction respectively.\n \n" +
				"x, y, z + SPACE switches planes on\n" +
				"and off");

		final int[] dirs = new int[] {Renderer.X_AXIS, 
				Renderer.Y_AXIS, Renderer.Z_AXIS};
		final Scrollbar[] sl = new Scrollbar[3]; 
		final Checkbox[] cb = new Checkbox[3];

		for(int k = 0; k < 3; k++) {
			final int i = k;
			sl[i] = (Scrollbar)gd.getSliders().get(i);
			sl[i].addAdjustmentListener(new AdjustmentListener() {
				public void adjustmentValueChanged(
							AdjustmentEvent e) {
					os.setSlice(dirs[i], sl[i].getValue());
					univ.fireContentChanged(c);
				}
			});

			cb[i] = (Checkbox)gd.getCheckboxes().get(i);
			cb[i].addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					os.setVisible(dirs[i],cb[i].getState());
				}
			});
		}

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					os.setSlices(oldvalues);
					os.setVisible(visible);
					univ.fireContentChanged(c);
					return;
				} else {
					record(SET_SLICES,
					Integer.toString(sl[0].getValue()), 
					Integer.toString(sl[1].getValue()),
					Integer.toString(sl[2].getValue()));
					return;
				}
			}
		});
		gd.showDialog();
	}

	public void fill(final Content c) {
		if(!checkSel(c))
			return;
		int type = c.getType();
		if(type != Content.VOLUME && type != Content.ORTHO) 
			return;
		new Thread() {
			@Override
			public void run() {
				((VoltexGroup)c.getContent()).
					fillRoiBlack(univ, (byte)0);
				univ.fireContentChanged(c);
				record(FILL_SELECTION);
			}
		}.start();
	}

	public void smoothMesh(Content c) {
		if(!checkSel(c))
			return;
		MeshEditor.smooth(c, 0.25f);
	}

	public void smoothAllMeshes() {
		// process each Mesh in a separate thread
		final Collection all = univ.getContents();
		final Content[] c = new Content[all.size()];
		all.toArray(c);
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] thread = new Thread[
			Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i<thread.length; i++) {
			thread[i] = new Thread() {
				@Override
				public void run() {
					try {
						for (int k=ai.getAndIncrement();
						k < c.length;
						k = ai.getAndIncrement()) {
							MeshEditor.
							smooth(c[k], 0.25f);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			thread[i].start();
		}
	}


	/* ----------------------------------------------------------
	 * Display As submenu
	 * --------------------------------------------------------*/
	public void displayAs(Content c, int type) {
		if(!checkSel(c))
			return;
		c.displayAs(type);
		univ.clearSelection();
	}


	/* ----------------------------------------------------------
	 * Attributes submenu
	 * --------------------------------------------------------*/
	public void changeColor(final Content c) {
		if(!checkSel(c))
			return;
		final GenericDialog gd = 
			new GenericDialog("Adjust color ...", univ.getWindow());
		final Color3f oldC = c.color;

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
				c.setColor(cBox.getState() ? null :
					new Color3f(rSlider.getValue() / 255f,
						gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
				gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				univ.fireContentChanged(c);
			}
		});

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				c.setColor(new Color3f(
						rSlider.getValue() / 255f,
						gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
				univ.fireContentChanged(c);
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					c.setColor(oldC);
					univ.fireContentChanged(c);
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
				univ.clearSelection();
			}
		});
		gd.showDialog();
	}

	public void changeChannels(Content c) {
		if(!checkSel(c))
			return;
		GenericDialog gd = new GenericDialog("Adjust channels ...",
							univ.getWindow());
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
				new String[] {"red", "green", "blue"}, 
				c.getChannels());
		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		c.setChannels(channels);
		univ.fireContentChanged(c);
		record(SET_CHANNELS, Boolean.toString(channels[0]),
			Boolean.toString(channels[1]),
			Boolean.toString(channels[2]));
		univ.clearSelection();
	}

	public void changeTransparency(final Content c) {
		if(!checkSel(c))
			return;
		final SliderAdjuster transp_adjuster = new SliderAdjuster() {
			public synchronized final void setValue(Content c, int v) {
				c.setTransparency(v / 100f);
			}
		};
		final GenericDialog gd = new GenericDialog(
			"Adjust transparency ...", univ.getWindow());
		final int oldTr = (int)(c.getTransparency() * 100);
		gd.addSlider("Transparency", 0, 100, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if(!transp_adjuster.go)
					transp_adjuster.start();
				transp_adjuster.exec((int)e.getValue(), c, univ);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					float newTr = (float)oldTr / 100f;
					c.setTransparency(newTr);
					univ.fireContentChanged(c);
					return;
				} else {
					record(SET_TRANSPARENCY, Float.
					toString(((Scrollbar)gd.getSliders().
					get(0)).getValue() / 100f));
				}
				if (null != transp_adjuster)
					transp_adjuster.quit();
				univ.clearSelection();
			}
		});
		gd.showDialog();
	}

	public void changeThreshold(final Content c) {
		if(!checkSel(c))
			return;
		final SliderAdjuster thresh_adjuster = new SliderAdjuster() {
			public synchronized final void setValue(Content c, int v) {
				c.setThreshold(v);
			}
		};
		final int oldTr = (int)(c.getThreshold());
		if(c.getType() == Content.SURFACE) {
			int th = (int)Math.round(
				IJ.getNumber("Threshold [0..255]", oldTr));
			th = Math.max(0, th);
			th = Math.min(th, 255);
			c.setThreshold(th);
			univ.fireContentChanged(c);
			record(SET_THRESHOLD, Integer.toString(th));
			return;
		}
		// in case we've not a mesh, change it interactively
		final GenericDialog gd = 
				new GenericDialog("Adjust threshold...");
		gd.addSlider("Threshold", 0, 255, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				// start adjuster and request an action
				if(!thresh_adjuster.go)
					thresh_adjuster.start();
				thresh_adjuster.exec((int)e.getValue(), c, univ);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					if(gd.wasCanceled()) {
						c.setThreshold(oldTr);
						univ.fireContentChanged(c);
						return;
					} else {
						record(SET_THRESHOLD, 
							Integer.toString(
								c.threshold));
					}
				} finally {
					// [ This code block executes even when
					//   calling return above ]
					//
					// clean up
					if (null != thresh_adjuster)
						thresh_adjuster.quit();
					univ.clearSelection();
				}
			}
		});
		gd.showDialog();
	}

	public void setShaded(Content c, boolean b) {
		if(!checkSel(c))
			return;
		int t = c.getType();
		if(t == Content.SURFACE || t == Content.SURFACE_PLOT2D)
			c.setShaded(b);
	}


	/* ----------------------------------------------------------
	 * Hide/Show submenu
	 * --------------------------------------------------------*/
	public void showCoordinateSystem(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.showCoordinateSystem(b);
		record(SET_CS, Boolean.toString(b));
	}

	public void showContent(Content c, boolean b) {
		if(!checkSel(c))
			return;
		univ.getSelected().setVisible(b);
		if(!b)
			univ.clearSelection();
	}

	public void showAllCoordinateSystems(boolean b) {
		for (Iterator it = univ.contents(); it.hasNext(); )
			((Content)it.next()).showCoordinateSystem(b);
	}


	/* ----------------------------------------------------------
	 * Point list submenu
	 * --------------------------------------------------------*/
	public void loadPointList(Content c) {
		if(!checkSel(c))
			return;
		c.loadPointList();
	}

	public void savePointList(Content c) {
		if(!checkSel(c))
			return;
		c.savePointList();
	}

	public void changePointSize(final Content c) {
		if(!checkSel(c))
			return;
		final GenericDialog gd = 
			new GenericDialog("Point size", univ.getWindow());
		final float oldS = (float)(c.getLandmarkPointSize());
		gd.addSlider("Size", 0, 20, oldS);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				float newS = (float)e.getValue();
				c.setLandmarkPointSize(newS);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					c.setLandmarkPointSize(oldS);
					return;
				}
			}
		});
		gd.showDialog();
	}

	public void showPointList(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.showPointList(b);
	}

	public void register() {
		// Select the contents used for registration
		Collection contents = univ.getContents();
		if(contents.size() < 2) {
			IJ.error("At least two bodies are " +
				"required for registration");
			return;
		}
		RegistrationMenubar rm = univ.getRegistrationMenubar();
		univ.setMenubar(rm);
		rm.register();
	}

	public void contentProperties(Content c) {
		if(!checkSel(c))
			return;
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



	/* **********************************************************
	 * Select menu
	 * *********************************************************/
	public void select(String name) {
		Content c = univ.getContent(name);
		if(c != null)
			univ.select(c);
	}

	/* **********************************************************
	 * Transformation menu
	 * *********************************************************/
	public void setLocked(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.setLocked(b);
		if(b) record(LOCK);
		else record(UNLOCK);
	}

	public void resetTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.name + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		c.setTransform(new Transform3D());
		univ.fireTransformationFinished();
		record(RESET_TRANSFORM);
	}

	public void setTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.name + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		float[] t = readTransform(c);
		if(t != null) {
			c.setTransform(new Transform3D(t));
			univ.fireTransformationFinished();
			record(SET_TRANSFORM, affine2string(t));
		}
	}

	public void applyTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.name + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		float[] t = readTransform(c);
		if(t != null) {
			c.applyTransform(new Transform3D(t));
			univ.fireTransformationFinished();
			record(APPLY_TRANSFORM, affine2string(t));
		}
	}

	public void saveTransform(Content c) {
		if(!checkSel(c))
			return;
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

	public void exportTransformed(final Content c) {
		if(!checkSel(c))
			return;
		new Thread(new Runnable() {
			public void run() {
				exportTr(c);
			}
		}).start();
	}

	private void exportTr(Content c) {
		ImagePlus orig = c.getImage();
		if(orig == null) {
			IJ.error("No greyscale image exists for "
				+ c.getName());
			return;
		}
		Transform3D t1 = new Transform3D();
		c.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		FastMatrix fc = FastMatrix.fromCalibration(orig);
		FastMatrix fm = fc.inverse().times(toFastMatrix(t1).inverse()).
			times(fc);
		InterpolatedImage in = new InterpolatedImage(orig);
		InterpolatedImage out = in.cloneDimensionsOnly();
		int w = orig.getWidth(), h = orig.getHeight();
		int d = orig.getStackSize();

		for (int k = 0; k < d; k++) {
			for (int j = 0; j < h; j++) {
				for(int i = 0; i < w; i++) {
					fm.apply(i, j, k);
					out.set(i, j, k, (byte)in.interpol.get(
							fm.x, fm.y, fm.z));
				}
				IJ.showProgress(k + 1, d);
			}
		}
		out.getImage().setTitle(orig.getTitle() + "_transformed");
		out.getImage().show();
	}




	/* **********************************************************
	 * View menu
	 * *********************************************************/
	public void resetView() {
		univ.resetView();
		record(RESET_VIEW);
	}

	public void centerSelected(Content c) {
		if(!checkSel(c))
			return;

		Point3f center = c.getContent().center;
		Transform3D localToVWorld = new Transform3D();
		c.getLocalToVworld(localToVWorld);
		localToVWorld.transform(center);
		univ.getViewPlatformTransformer().centerAt(center);
	}

	public void record() {
		ImagePlus movie = univ.record();
		if(movie != null)
			movie.show();
		record(START_RECORD);
	}

	public void startAnimation() {
		univ.startAnimation();
		record(START_ANIMATE);
	}

	public void stopAnimation() {
		univ.pauseAnimation();
		record(STOP_ANIMATE);
	}

	public void viewPreferences() {
		GenericDialog gd = new GenericDialog(
				"View Preferences", univ.getWindow());
		gd.addMessage("Window size:");
		Dimension d = univ.getSize();
		if(d == null)
			return;
		gd.addNumericField("width", d.width, 0);
		gd.addNumericField("height", d.height, 0);

		String[] choice = new String[] {"PARALLEL", "PERSPECTIVE"};
		int v[] = new int[] {View.PARALLEL_PROJECTION,
					View.PERSPECTIVE_PROJECTION};
		String def = univ.getViewer().getView().getProjectionPolicy()
			== v[0] ? choice[0] : choice[1];
		gd.addChoice("Projection", choice, def);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		univ.setSize((int)gd.getNextNumber(), (int)gd.getNextNumber());
		univ.getViewer().getView().setProjectionPolicy(
					v[gd.getNextChoiceIndex()]);
	}

	public void editScalebar() {
		Scalebar sc = univ.getScalebar();
		final GenericDialog gd = new GenericDialog(
				"Edit scalebar...", univ.getWindow());
		gd.addNumericField("x position", sc.getX(), 2);
		gd.addNumericField("y position", sc.getY(), 2);
		gd.addNumericField("length", sc.getLength(), 2);
		gd.addStringField("Units", sc.getUnit(), 5);
		gd.addChoice("Color", ColorTable.colorNames, 
				ColorTable.getColorName(sc.getColor()));
		gd.addCheckbox("show", sc.isVisible());
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		sc.setPosition((float)gd.getNextNumber(), 
				(float)gd.getNextNumber());
		sc.setLength((float)gd.getNextNumber());
		sc.setUnit(gd.getNextString());
		sc.setColor(ColorTable.getColor(gd.getNextChoice()));
		sc.setVisible(gd.getNextBoolean());
	}



	/* **********************************************************
	 * Help menu
	 * *********************************************************/
	public void j3dproperties() {
		TextWindow tw = new TextWindow("Java 3D Properties", 
			"Key\tValue", "", 512, 512);
		Map props = Image3DUniverse.getProperties();
		tw.append("Java 3D properties\n \n");
		for(Iterator it = props.entrySet().iterator();
						it.hasNext();) {
			Map.Entry me = (Map.Entry)it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
		props = univ.getCanvas().queryProperties();
		tw.append(" \nRendering properties\n \n");
		for(Iterator it = props.entrySet().iterator();
						it.hasNext();) {
			Map.Entry me = (Map.Entry)it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
	}



	/* **********************************************************
	 * Utility methods
	 * *********************************************************/
	public static void convert(ImagePlus image) {
		int imaget = image.getType();
		if(imaget == ImagePlus.GRAY8 || imaget == ImagePlus.COLOR_256)
			return;
		int s = image.getStackSize();
		switch(imaget) {
			case ImagePlus.COLOR_256:
				if(s == 1)
					new ImageConverter(image).
						convertToRGB();
				else
					new StackConverter(image).
						convertToRGB();
				break;
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32:
				if(s == 1)
					new ImageConverter(image).
						convertToGray8();
				else
					new StackConverter(image).
						convertToGray8();
				break;
		}
	}

	private float[] readTransform(Content selected) {
		final GenericDialog gd = new GenericDialog(
				"Read transformation", univ.getWindow());
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
		gd.showDialog();
		if(gd.wasCanceled())
			return null;

		transform = gd.getNextString();
		float[] m = string2affine(transform);
		return m;
	}

	private FastMatrix toFastMatrix(Transform3D t3d) {
		Matrix4d m = new Matrix4d();
		t3d.get(m);
		return new FastMatrix(new double[][] {
			{m.m00, m.m01, m.m02, m.m03}, 
			{m.m10, m.m11, m.m12, m.m13},
			{m.m20, m.m21, m.m22, m.m23}});
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

	private final boolean checkSel(Content c) {
		if(c == null) {
			IJ.error("Selection required");
			return false;
		}
		return true;
	}



	/* **********************************************************
	 * Recording methods
	 * *********************************************************/
	private static void record(String command) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command);
		}
	}
	
	private static void record(String command, String a1) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command, a1);
		}
	}

	private static void record(String command, String a1, String a2) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call",command,a1,a2);
		}
	}

	private static void record(String command, String a1, 
						String a2, String a3) {
		command = "call(\"ImageJ_3D_Viewer." + command + "\", " + 
				"\"" + a1 + "\"," + 
				"\"" + a2 + "\"," + 
				"\"" + a3 + "\")"; 
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.recordString(command);
		}
	}

	private static void record(String command, String[] args) {
		command = "call(\"ImageJ_3D_Viewer." + command + "\"";
		for(int i = 0; i < args.length; i++) {
			command += ", \"" + args[i] + "\"";
		}
		command += ");";
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.recordString(command);
		}
	}



	/* **********************************************************
	 * Thread which handles the updates of sliders
	 * *********************************************************/
	private abstract class SliderAdjuster extends Thread {
		boolean go = false;
		int newV;
		Content content;
		Image3DUniverse univ;
		final Object lock = new Object();

		SliderAdjuster() {
			super("VIB-SliderAdjuster");
			setPriority(Thread.NORM_PRIORITY);
			setDaemon(true);
		}

		/*
		 * Set a new event, overwritting previous if any.
		 */
		void exec(final int newV, final Content content, final Image3DUniverse univ) {
			synchronized (lock) {
				this.newV = newV;
				this.content = content;
				this.univ = univ;
			}
			synchronized (this) { notify(); }
		}

		public void quit() {
			this.go = false;
			synchronized (this) { notify(); }
		}
		
		/*
		 * This class has to be implemented by subclasses, to define
		 * the specific updating function.
		 */
		protected abstract void setValue(final Content c, final int v);

		@Override
		public void run() {
			go = true;
			while (go) {
				try {
					if (null == content) {
						synchronized (this) { wait(); }
					}
					if (!go) return;
					// 1 - cache vars, to free the lock very quickly
					Content c;
					int transp = 0;
					Image3DUniverse u;
					synchronized (lock) {
						c = this.content;
						transp = this.newV;
						u = this.univ;
					}
					// 2 - exec cached vars
					if (null != c) {
						setValue(c, transp);
						u.fireContentChanged(c);
					}
					// 3 - done: reset only if no new request was put
					synchronized (lock) {
						if (c == this.content) {
							this.content = null;
							this.univ = null;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
}

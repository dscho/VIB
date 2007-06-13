package ij3d;

import ij.gui.GenericDialog;
import ij.IJ;
import ij.ImagePlus;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import isosurface.MeshGroup;
import javax.vecmath.Color3f;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;

	private MenuItem mesh;
	private MenuItem voltex;
	private MenuItem ortho;
	private MenuItem color;
	private MenuItem channels;
	private MenuItem transparency;
	private MenuItem fill;
	private MenuItem slices;
	private MenuItem delete;
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
	private CheckboxMenuItem perspective;
	private CheckboxMenuItem coordinateSystem;

	private Menu selectedMenu;
	private Menu viewMenu;
	private Menu contentsMenu;

	public static final String START_ANIMATE = "startAnimate";
	public static final String STOP_ANIMATE = "stopAnimate";
	public static final String START_RECORD = "startRecord";
	public static final String STOP_RECORD = "stopRecord";
	public static final String RESET_VIEW = "resetView";

	public static final String SET_COLOR = "setColor"; 
	public static final String SET_TRANSPARENCY = "setTransparency";
	public static final String SET_CHANNELS = "setChannels";
	public static final String FILL_SELECTION = "fillSelection";
	public static final String DELETE = "delete";

	public Image3DMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;

		univ.addUniverseListener(this);

		viewMenu = createViewMenu();
		this.add(viewMenu);
		contentsMenu = createContentsMenu();
		this.add(contentsMenu);
		selectedMenu = createSelectedMenu();
	}

	public Menu createViewMenu() {
		// Viewer
		Menu view = new Menu("View");

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

		close = new MenuItem("Close");
		close.addActionListener(this);
		view.add(close);

		return view;
	}

	public Menu createContentsMenu() {
		// Universe
		Menu universe = new Menu("Contents");
		voltex = new MenuItem("Add volume");
		voltex.addActionListener(this);
		universe.add(voltex);

		mesh = new MenuItem("Add mesh");
		mesh.addActionListener(this);
		universe.add(mesh);

		ortho = new MenuItem("Add Orthoslice");
		ortho.addActionListener(this);
		universe.add(ortho);

		universe.addSeparator();

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		universe.add(delete);


		return universe;
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

		content.addSeparator();
		
		channels = new MenuItem("Change channels");
		channels.addActionListener(this);
		content.add(channels);

		color = new MenuItem("Change color");
		color.addActionListener(this);
		content.add(color);

		transparency = new MenuItem("Change transparency");
		transparency.addActionListener(this);
		content.add(transparency);

		coordinateSystem = new CheckboxMenuItem(
					"Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		content.add(coordinateSystem);

		content.addSeparator();

		setTransform = new MenuItem("Set Transform");
		setTransform.addActionListener(this);
		content.add(setTransform);

		resetTransform = new MenuItem("Reset Transform");
		resetTransform.addActionListener(this);
		content.add(resetTransform);

		applyTransform = new MenuItem("Apply Transform");
		applyTransform.addActionListener(this);
		content.add(applyTransform);

		saveTransform = new MenuItem("Save Transform");
		saveTransform.addActionListener(this);
		content.add(saveTransform);

		return content;
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

		if(e.getSource() == voltex) {
			VoltexGroup.addContent(univ, null);
		}
		
		if(e.getSource() == mesh) {
			MeshGroup.addContent(univ, null);
		}

		if(e.getSource() == ortho) {
			OrthoGroup.addContent(univ, null);
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
	
		if(e.getSource() == resetView) {
			record(RESET_VIEW);
			univ.resetView();
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

		if(e.getSource() == slices) {
			Content c = univ.getSelected();
			if(c == null || !(c instanceof OrthoGroup)) {
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
			if(c instanceof VoltexGroup) {
				new Thread(new Runnable() {
					public void run() {
						((VoltexGroup)c).
						fillRoiBlack(univ, (byte)0);
						univ.fireContentChanged(c);
						record(FILL_SELECTION);
					}
				}).start();
			}
		}

		if(e.getSource() == close) {
			univ.close();
		}

		if(e.getSource() == resetTransform) {
			if(univ.getSelected() == null) {
				IJ.error("Selection required");
				return;
			}
			univ.fireTransformationStarted();
			univ.getSelected().setTransform(
					new Transform3D());
			univ.fireTransformationFinished();
		}

		if(e.getSource() == setTransform) {
			if(univ.getSelected() == null) {
				IJ.error("Selection required");
				return;
			}
			univ.fireTransformationStarted();
			float[] t = readTransform(univ.getSelected());
			if(t != null) {
				univ.getSelected().setTransform(
					new Transform3D(t));
				univ.fireTransformationFinished();
			}
		}

		if(e.getSource() == applyTransform) {
			if(univ.getSelected() == null) {
				IJ.error("Selection required");
				return;
			}
			univ.fireTransformationStarted();
			float[] t = readTransform(univ.getSelected());
			if(t != null) {
				univ.getSelected().applyTransform(
					new Transform3D(t));
				univ.fireTransformationFinished();
			}
		}

		if(e.getSource() == saveTransform) {
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
			univ.getSelected().showCoordinateSystem(
				coordinateSystem.getState());
		}
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
				if(gd.wasCanceled()) {
					float newTr = (float)oldTr / 100f;
					selected.setTransparency(newTr);
					univ.fireContentChanged(selected);
					return;
				}
			}
		});
		gd.showDialog();
		
		record(SET_TRANSPARENCY,Float.toString(
			((Scrollbar)gd.getSliders().get(0)).getValue() / 100f));
	}

	public void adjustSlices(final Content selected) {
		final GenericDialog gd = new GenericDialog("Adjust slices...");
		final OrthoGroup os = (OrthoGroup)selected;
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

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				os.setSlices(
					xSlider.getValue(), 
					ySlider.getValue(), 
					zSlider.getValue());
				univ.fireContentChanged(selected);
			}
		};
		xSlider.addAdjustmentListener(listener);
		ySlider.addAdjustmentListener(listener);
		zSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					os.setSlices(
						oldvalues[0], 
						oldvalues[1], 
						oldvalues[2]);
					univ.fireContentChanged(selected);
					return;
				}
			}
		});
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
				if(gd.wasCanceled()) {
					selected.setColor(oldC);
					univ.fireContentChanged(selected);
					return;
				}
			}
		});
		gd.showDialog();
		
		record(SET_COLOR, Integer.toString(rSlider.getValue()), 
				Integer.toString(gSlider.getValue()),
				Integer.toString(bSlider.getValue()));
	}

	public void changeChannels(Content selected) {
		GenericDialog gd = new GenericDialog("Adjust channels ...");
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
				new String[] {"red", "green", "blue"}, 
				selected.getChannels());
		gd.showDialog();
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

	public void record(String command) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command);
		}
	}
	
	public void record(String command, String a1) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call", command, a1);
		}
	}

	public void record(String command, String a1, String a2) {
		command = "ImageJ_3D_Viewer." + command;
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.record("call",command,a1,a2);
		}
	}

	public void record(String command, String a1, String a2, String a3) {
		command = "call(\"ImageJ_3D_Viewer." + command + "\", " + 
				"\"" + a1 + "\"," + 
				"\"" + a2 + "\"," + 
				"\"" + a3 + "\")"; 
		if(ij.plugin.frame.Recorder.record) {
			ij.plugin.frame.Recorder.recordString(command);
		}
	}

	// Universe Listener interface
	public void transformationStarted() {}
	public void transformationFinished() {}
	public void contentAdded(Content c) {}
	public void contentRemoved(Content c) {}
	public void canvasResized() {}
	public void transformationUpdated() {}
	public void contentChanged(Content c) {}

	public void contentSelected(Content c) {
		delete.setEnabled(c != null);
		if(c == null) {
			remove(selectedMenu);
			return;
		}	
		selectedMenu.setLabel(c.getName());
		if(!containsSelectedMenu())
			add(selectedMenu);
		
		slices.setEnabled(c instanceof OrthoGroup);
		fill.setEnabled(c instanceof VoltexGroup);
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
		GenericDialog gd = new GenericDialog(
					"Read transformation", null);
		Transform3D t1 = new Transform3D();
		selected.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		selected.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		float[] matrix = new float[16];
		t1.get(matrix);
		String transform = "";
		for(int i = 0; i < matrix.length; i++) {
			transform += matrix[i] + " ";
		}
		gd.addStringField("Transformation", transform, 25);
		gd.showDialog();
		if(gd.wasCanceled())
			return null;

		transform = gd.getNextString();
		String[] s = ij.util.Tools.split(transform);
		float[] m = new float[s.length];
		for(int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		return m;
	}
}


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

public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener {

	private Image3DUniverse univ;

	private MenuItem mesh;
	private MenuItem voltex;
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
	private CheckboxMenuItem perspective;

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

	public Image3DMenubar(Image3DUniverse universe) {
		super();
		this.univ = universe;

		Menu menu = new Menu("3D Viewer");
		this.add(menu);

		voltex = new MenuItem("Add volume");
		voltex.addActionListener(this);
		menu.add(voltex);

		mesh = new MenuItem("Add mesh");
		mesh.addActionListener(this);
		menu.add(mesh);

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		menu.add(delete);

		menu.addSeparator();

		resetView = new MenuItem("Reset view");
		resetView.addActionListener(this);
		menu.add(resetView);

		menu.addSeparator();

		slices = new MenuItem("Adjust slices");
		slices.addActionListener(this);
		menu.add(slices);
		
		fill = new MenuItem("Fill selection");
		fill.addActionListener(this);
		menu.add(fill);
		
		channels = new MenuItem("Change channels");
		channels.addActionListener(this);
		menu.add(channels);

		color = new MenuItem("Change color");
		color.addActionListener(this);
		menu.add(color);

		transparency = new MenuItem("Change transparency");
		transparency.addActionListener(this);
		menu.add(transparency);

		menu.addSeparator();

		startRecord = new MenuItem("Start recording");
		startRecord.addActionListener(this);
		menu.add(startRecord);

		stopRecord = new MenuItem("Stop recording");
		stopRecord.addActionListener(this);
		menu.add(stopRecord);

		menu.addSeparator();

		startAnimation = new MenuItem("Start animation");
		startAnimation.addActionListener(this);
		menu.add(startAnimation);

		stopAnimation = new MenuItem("Stop animation");
		stopAnimation.addActionListener(this);
		menu.add(stopAnimation);

		menu.addSeparator();

		perspective = new CheckboxMenuItem(
					"Perspective Projection", true);
		perspective.addItemListener(this);
		menu.add(perspective);

		menu.addSeparator();

		close = new MenuItem("Close");
		close.addActionListener(this);
		menu.add(close);
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
					}
				}).start();
				record(FILL_SELECTION);
			}
		}

		if(e.getSource() == close) {
			univ.close();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == perspective) {
			int policy = perspective.getState() 
						? View.PERSPECTIVE_PROJECTION 
						: View.PARALLEL_PROJECTION;
			univ.getViewer().getView().setProjectionPolicy(policy);
		}
	}

	public void changeTransparency(final Content selected) {
		GenericDialog gd = new GenericDialog("Adjust transparency ...");
		int oldTr = (int)(selected.getTransparency() * 100);
		gd.addSlider("Transparency", 0, 100, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				float newTr = (float)e.getValue() / 100f; 
				selected.setTransparency(newTr);
			}
		});
		gd.showDialog();
		if(gd.wasCanceled()) {
			float newTr = (float)oldTr / 100f;
			selected.setTransparency(newTr);
			return;
		}
		
		record(SET_TRANSPARENCY,Float.toString(
			((Scrollbar)gd.getSliders().get(0)).getValue() / 100f));
	}

	public void adjustSlices(final Content selected) {
		final GenericDialog gd = new GenericDialog("Adjust slices...");
		final OrthoGroup os = (OrthoGroup)selected;
		int[] oldvalues = os.getSlices();
		ImagePlus imp = selected.image;
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();		

		gd.addSlider("x", 0, w, oldvalues[0]);
		gd.addSlider("y", 0, h, oldvalues[1]);
		gd.addSlider("z", 0, d, oldvalues[2]);

		final Scrollbar xSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar ySlider = (Scrollbar)gd.getSliders().get(1);
		final Scrollbar zSlider = (Scrollbar)gd.getSliders().get(2);

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				os.setSlices(
					xSlider.getValue(), 
					ySlider.getValue(), 
					zSlider.getValue());
			}
		};
		xSlider.addAdjustmentListener(listener);
		ySlider.addAdjustmentListener(listener);
		zSlider.addAdjustmentListener(listener);

		gd.showDialog();
		if(gd.wasCanceled()) {
			os.setSlices(oldvalues[0], oldvalues[1], oldvalues[2]);
			return;
		}
	}

	public void changeColor(final Content selected) {
		final GenericDialog gd = new GenericDialog("Adjust color ...");
		Color3f oldC = selected.color;

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
			}
		});

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				selected.setColor(new Color3f(
					rSlider.getValue() / 255f, 
					gSlider.getValue() / 255f, 
					bSlider.getValue() / 255f));
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.showDialog();
		if(gd.wasCanceled()) {
			selected.setColor(oldC);
			return;
		}
		
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
}


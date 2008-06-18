package ij3d;

import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;
import ij.gui.Toolbar;
import ij.process.StackConverter;
import ij.process.ImageConverter;

import view4d.Viewer4D;
import view4d.Viewer4DController;

import math3d.Transform_IO;

import java.text.DecimalFormat;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import vib.PointList;
import vib.BenesNamedPoint;
import vib.InterpolatedImage;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import voltex.Renderer;
import isosurface.MeshGroup;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix4d;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;

public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;
	private Executer executer;

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
	private MenuItem exportTransformed;
	private MenuItem exportObj;
	private MenuItem exportDXF;
	private MenuItem smoothMesh;
	private MenuItem scalebar;
	private MenuItem smoothAllMeshes;
	private MenuItem displayAsVolume;
	private MenuItem displayAsOrtho;
	private MenuItem displayAsSurface;
	private MenuItem displayAsSurfacePlot;
	private MenuItem pl_load;
	private MenuItem regist;
	private CheckboxMenuItem shaded;
	private MenuItem pl_save;
	private MenuItem pl_size;
	private MenuItem j3dproperties;
	private MenuItem viewer4d;
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
	private Menu helpMenu;


	private SelectionListener selListener = new SelectionListener();


	public Image3DMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;
		this.executer = univ.getExecuter();

		univ.addUniverseListener(this);

		fileMenu = createFileMenu();
		this.add(fileMenu);

		viewMenu = createViewMenu();
		this.add(viewMenu);
		contentsMenu = createContentsMenu();
		this.add(contentsMenu);
		selectedMenu = createSelectedMenu();
		helpMenu = createHelpMenu();
		this.add(helpMenu);
		this.setHelpMenu(helpMenu);
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

	public Menu createHelpMenu() {
		Menu help = new Menu("Help");
		j3dproperties = new MenuItem("Java 3D Properties");
		j3dproperties.addActionListener(this);
		help.add(j3dproperties);
		return help;
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

		viewer4d = new MenuItem("Load 4D data");
		viewer4d.addActionListener(this);
		view.add(viewer4d);

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

		pl.addSeparator();

		pl_size = new MenuItem("Point size");
		pl_size.addActionListener(this);
		pl.add(pl_size);

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

		transform.addSeparator();

		exportTransformed= new MenuItem("Export transformed image");
		exportTransformed.addActionListener(this);
		transform.add(exportTransformed);

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

		shaded = new CheckboxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		attributes.add(shaded);

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

		displayAsSurfacePlot = new MenuItem("Surface Plot 2D");
		displayAsSurfacePlot.addActionListener(this);
		display.add(displayAsSurfacePlot);

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
			executer.select(e.getActionCommand());
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if(src == color)
			executer.changeColor(univ.getSelected());
		else if(src == scalebar)
			executer.editScalebar();
		else if(src == viewer4d)
			executer.load4D();
		else if(src == channels)
			executer.changeChannels(univ.getSelected());
		else if(src == transparency)
			executer.changeTransparency(univ.getSelected());
		else if(src == add)
			executer.addContent(null, -1);
		else if(src == delete)
			executer.delete(univ.getSelected());
		else if(src == resetView)
			executer.resetView();
		else if(src == windowSize)
			executer.setWindowSize();
		else if(src == startRecord)
			executer.startRecording();
		else if(src == stopRecord)
			executer.stopRecording();
		else if(src == startAnimation)
			executer.startAnimation();
		else if(src == stopAnimation)
			executer.stopAnimation();
		else if(src == threshold)
			executer.changeThreshold(univ.getSelected());
		else if(src == displayAsVolume)
			executer.displayAs(univ.getSelected(), Content.VOLUME);
		else if(src == displayAsOrtho)
			executer.displayAs(univ.getSelected(), Content.ORTHO);
		else if(src == displayAsSurface)
			executer.displayAs(univ.getSelected(), Content.SURFACE);
		else if(src == displayAsSurfacePlot)
			executer.displayAs(
				univ.getSelected(), Content.SURFACE_PLOT2D);
		else if(src == slices)
			executer.changeSlices(univ.getSelected());
		else if(src == fill)
			executer.fill(univ.getSelected());
		else if(src == close)
			executer.close();
		else if(src == resetTransform)
			executer.resetTransform(univ.getSelected());
		else if(src == setTransform)
			executer.setTransform(univ.getSelected());
		else if(src == properties)
			executer.contentProperties(univ.getSelected());
		else if(src == applyTransform)
			executer.applyTransform(univ.getSelected());
		else if(src == saveTransform)
			executer.saveTransform(univ.getSelected());
		else if(src == exportTransformed)
			executer.exportTransformed(univ.getSelected());
		else if (src == pl_load)
			executer.loadPointList(univ.getSelected());
		else if (src == pl_save)
			executer.savePointList(univ.getSelected());
		else if (src == pl_size)
			executer.changePointSize(univ.getSelected());
		else if (src == exportDXF)
			executer.saveAsDXF();
		else if (src == exportObj)
			executer.saveAsWaveFront();
		else if (src == smoothMesh)
			executer.smoothMesh(univ.getSelected());
		else if (src == smoothAllMeshes)
			executer.smoothAllMeshes();
		else if(src == j3dproperties)
			executer.j3dproperties();
	}

	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();
		Content c = univ.getSelected();
		if(src == perspective)
			executer.perspectiveProjection(perspective.getState());
		else if(src == coordinateSystem)
			executer.showCoordinateSystem(
						c, coordinateSystem.getState());
		else if(src == show)
			executer.showContent(c, show.getState());
		else if(src == lock)
			executer.setLocked(c, lock.getState());
		else if(src == shaded)
			executer.setShaded(c, shaded.getState());
		else if (src == pl_show)
			executer.showPointList(c, pl_show.getState());
	}







	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}
	public void universeClosed() {}

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
		int t = c.getType();
		selectedMenu.setLabel(c.getName());
		if(!containsSelectedMenu())
			add(selectedMenu);
		
		slices.setEnabled(t == Content.ORTHO);
		fill.setEnabled(t == Content.VOLUME);
		shaded.setEnabled(t == Content.SURFACE_PLOT2D ||
			t == Content.SURFACE);

		coordinateSystem.setState(c.hasCoord());
		lock.setState(c.isLocked());
		show.setState(c.isVisible());
		pl_show.setState(c.isPLVisible());
		shaded.setState(c.isShaded());


		displayAsVolume.setEnabled(t != Content.VOLUME);
		displayAsOrtho.setEnabled(t != Content.ORTHO);
		displayAsSurface.setEnabled(t != Content.SURFACE);
		displayAsSurfacePlot.setEnabled(t != Content.SURFACE_PLOT2D);
	}

	private boolean containsSelectedMenu() {
		for(int i = 0; i < getMenuCount(); i++) {
			if(getMenu(i) == selectedMenu) {
				return true;
			}
		}
		return false;
	}
}


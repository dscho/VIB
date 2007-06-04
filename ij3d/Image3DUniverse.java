package ij3d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.IJ;

import ij3d.ImageWindow3D;
import ij3d.ImageCanvas3D;

import isosurface.Triangulator;
import isosurface.MeshGroup;

import voltex.VoltexGroup;

import marchingcubes.MCTriangulator;

import java.awt.Panel;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;

import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.j3d.utils.picking.behaviors.PickingCallback;
import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;
import javax.media.j3d.*;
import javax.media.j3d.Alpha;
import javax.media.j3d.RotationInterpolator;
import javax.vecmath.*;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

public class Image3DUniverse extends DefaultAnimatableUniverse {

	private Content selected;
	private Hashtable contents = new Hashtable();
	private Image3DMenubar menubar;
	private ImageCanvas3D canvas;

	public Image3DUniverse(int width, int height) {
		super(width, height);
		canvas = (ImageCanvas3D)getCanvas();

		// add mouse listeners
		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				Content c = getContentAtCanvasPosition(
						e.getX(), e.getY());
				if(c != null)
					IJ.showStatus(c.name);
				else
					IJ.showStatus("");
			}
		});
		canvas.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Content c = getContentAtCanvasPosition(
						e.getX(), e.getY());
				select(c);
			}
		});
	}

	public void select(Content c) {
		if(selected != null) {
			selected.setSelected(false);
			selected = null;
		}
		if(c != null) {
			c.setSelected(true);
			selected = c;
		}
		String st = c != null ? c.name : "none";
		IJ.showStatus("selected: " + st);
		canvas.setStatus("selected: " + st);
	}

	public void transformChanged(int type, TransformGroup tg) {
		super.transformChanged(type, tg);
		Iterator it = contents.values().iterator();
		while(it.hasNext()) {
			((Content)it.next()).eyePtChanged(
				canvas.getView());		
		}
	}

	public void show() {
		this.show(true);
	}

	public void show(boolean withMenuBar) {
		super.show();
		if(withMenuBar) {
			menubar = new Image3DMenubar(this);
			win.setMenuBar(menubar);
		}
	}

	public Image3DMenubar getMenuBar() {
		return menubar;
	}

	public Menu getMenu() {
		if(menubar != null)
			return menubar.getMenu(0);
		return null;
	}

	public void addMenuItem(String label, ActionListener al) {
		Menu menu = getMenu();
		if(menu != null) {
			MenuItem item = new MenuItem(label);
			item.addActionListener(al);
			menu.add(item);
		}
	}

	public void removeMenuItem(String label) {
		Menu menu = getMenu();
		if(menu != null) {
			for(int i = 0; i < menu.getItemCount(); i++) {
				if(label.equals(menu.getItem(i).getLabel()))
					menu.remove(i);
			}
		}
	}

	public void addVoltex(ImagePlus image, Color3f color, 
		String name, boolean[] channels, int resamplingF, Vector3f t) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		Transform3D tr = new Transform3D();
		tr.setTranslation(t);
		VoltexGroup content = new VoltexGroup(
			name, color, image, channels, resamplingF, tr);
		scene.addChild(content);
		contents.put(name, content);
		fireContentAdded(content);
	}


	private void ensureScale(ImagePlus image) {
		Transform3D scale = new Transform3D();
		scaleTG.getTransform(scale);
		float oldXRange = (float)scale.getScale();

		float xRange = image.getWidth() * 
				(float)image.getCalibration().pixelWidth;
		if(xRange > oldXRange) {
			scale.setScale(1/xRange);
			scaleTG.setTransform(scale);
		}
	}
	
	public void addMesh(ImagePlus image, Color3f color, String name, 
		int threshold, boolean[] channels, int resamplingF, Vector3f t){
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		Transform3D tr = new Transform3D();
		tr.setTranslation(t);
		MeshGroup meshG = new MeshGroup(
			name, color, image, channels,resamplingF,threshold,tr);
		scene.addChild(meshG);
		contents.put(name, meshG);
		fireContentAdded(meshG);
	}

	public void addMesh(List mesh, Color3f color, 
			String name, float scale, int threshold, Vector3f t){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleTG.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleTG.setTransform(scaletr);
		// add the mesh
		addMesh(mesh, color, name, threshold, t);
	}

	public void addMesh(List mesh, 
			Color3f color, String name, int threshold, Vector3f t) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
	
		Transform3D tr = new Transform3D();
		tr.setTranslation(t);
		MeshGroup meshG = new MeshGroup(
				name, color, mesh, threshold, tr);
		scene.addChild(meshG);
		contents.put(name, meshG);
		fireContentAdded(meshG);
	}

	public void removeContent (String name) {
		Content content = (Content)contents.get(name);
		if(content == null)
			return;
		scene.removeChild(content);
		contents.remove(name);
		if(selected == content)
			selected = null;
		fireContentRemoved(content);
	}

	public Iterator contents() {
		return contents.values().iterator();
	}

	public Content getContent(String name) {
		if (null == name) return null;
		return contents.get(name);
	}

	public void resetView() {
		fireTransformationStarted();
		Transform3D t = new Transform3D();
		for(Iterator it = contents(); it.hasNext();) {
			Content c = (Content)it.next();
			c.resetView();
		}
		getViewingPlatform().setNominalViewingTransform();
		TransformGroup tg = null;
		transformChanged(-1, tg);
		fireTransformationFinished();
	}

	public Content getSelected() {
		return selected;
	}

	public void clearSelection() {
		if(selected != null)
			selected.setSelected(false);
		selected = null;
	}

	private Content getContentAtCanvasPosition(int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, scene); 
		pickCanvas.setMode(PickCanvas.GEOMETRY);
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeLocation(x, y); 
		PickResult result = null;
		try {
			result = pickCanvas.pickClosest();
		} catch(NullPointerException e) {}
		if(result == null) 
			return null;
		Content content = 
			(Content)result.getNode(PickResult.BRANCH_GROUP);
		if(content== null)
			return null;
		return content;
	}
} 

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

import orthoslice.OrthoGroup;

import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.*;

import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;

import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

public class Image3DUniverse extends DefaultAnimatableUniverse {

	private Content selected;
	private Hashtable contents = new Hashtable();
	private Image3DMenubar menubar;
	private ImageCanvas3D canvas;

	private Point3f globalMin = new Point3f();
	private Point3f globalMax = new Point3f();
	private Point3f globalCenter = new Point3f();

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
//		canvas.setStatus("selected: " + st);

		fireContentSelected(c);

		if(c != null && ij.plugin.frame.Recorder.record)
			ij.plugin.frame.Recorder.record(
				"call", "ImageJ_3D_Viewer.select", c.name);
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

	public void close() {
		super.close();
		removeAllContents();
		contents = null;
	}

	public Image3DMenubar getMenuBar() {
		return menubar;
	}

	public void recalculateGlobalMinMax(Content c) {
		Point3f cmin = c.minPoint;
		Point3f cmax = c.maxPoint;
		if(cmin.x < globalMin.x) globalMin.x = cmin.x;
		if(cmin.y < globalMin.y) globalMin.y = cmin.y;
		if(cmin.z < globalMin.z) globalMin.z = cmin.z;
		if(cmax.x > globalMax.x) globalMax.x = cmax.x;
		if(cmax.y > globalMax.y) globalMax.y = cmax.y;
		if(cmax.z > globalMax.z) globalMax.z = cmax.z;
		globalCenter.x = (globalMax.x - globalMin.x)/2;
		globalCenter.y = (globalMax.y - globalMin.y)/2;
		globalCenter.z = (globalMax.z - globalMin.z)/2;

		Transform3D transform = new Transform3D();
		transform.setTranslation(new Vector3f(
			-globalCenter.x, -globalCenter.y, -globalCenter.z));
		centerTG.setTransform(transform);
	}

	public Point3f getGlobalCenterPoint() {
		return globalCenter;
	}

	public VoltexGroup addVoltex(ImagePlus image, Color3f color, 
		String name, boolean[] channels, int resamplingF) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
		ensureScale(image);
		VoltexGroup content = new VoltexGroup(
			name, color, image, channels, resamplingF);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		fireContentAdded(content);
		return content;
	}

	public OrthoGroup addOrthoslice(ImagePlus image, Color3f color, 
		String name, boolean[] channels, int resamplingF) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
		ensureScale(image);
		OrthoGroup content = new OrthoGroup(
			name, color, image, channels, resamplingF);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		fireContentAdded(content);
		return content;
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
	
	public MeshGroup addMesh(ImagePlus image, Color3f color, String name, 
		int threshold, boolean[] channels, int resamplingF){
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
		ensureScale(image);
		MeshGroup meshG = new MeshGroup(
			name, color, image, channels,resamplingF,threshold);
		scene.addChild(meshG);
		contents.put(name, meshG);
		recalculateGlobalMinMax(meshG);
		fireContentAdded(meshG);
		return meshG;
	}

	public MeshGroup addMesh(List mesh, Color3f color, 
			String name, float scale, int threshold){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleTG.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleTG.setTransform(scaletr);
		// add the mesh
		return addMesh(mesh, color, name, threshold);
	}

	public MeshGroup addMesh(List mesh, 
			Color3f color, String name, int threshold) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
	
		MeshGroup meshG = new MeshGroup(name, color, mesh, threshold);
		scene.addChild(meshG);
		contents.put(name, meshG);
		recalculateGlobalMinMax(meshG);
		fireContentAdded(meshG);
		return meshG;
	}

	public void removeAllContents() {
		for(Iterator it = contents.keySet().iterator(); it.hasNext();) {
			String name = (String)it.next();
			removeContent(name);
		}
	}

	public void removeContent(String name) {
		Content content = (Content)contents.get(name);
		if(content == null)
			return;
		scene.removeChild(content);
		contents.remove(name);
		if(selected == content)
			clearSelection();
		fireContentRemoved(content);
		content.flush();
		content = null;
	}

	public Iterator contents() {
		return contents.values().iterator();
	}

	public Collection getContents() {
		return contents.values();
	}

	public Content getContent(String name) {
		if (null == name) return null;
		return (Content)contents.get(name);
	}

	public void resetView() {
		fireTransformationStarted();
		Transform3D t = new Transform3D();
		getViewingPlatform().setNominalViewingTransform();
		rotationsTG.setTransform(t);
		translateTG.setTransform(t);
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
		fireContentSelected(null);
	}

	private Content getContentAtCanvasPosition(int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, scene); 
		pickCanvas.setMode(PickCanvas.GEOMETRY);
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeLocation(x, y); 
		PickResult result = null;
		try {
			result = pickCanvas.pickClosest();
			if(result == null) 
				return null;
			Content content = (Content)result.
					getNode(PickResult.BRANCH_GROUP);
			if(content== null)
				return null;
			return content;
		} catch(Exception e) {
			return null;
		}
	}
} 

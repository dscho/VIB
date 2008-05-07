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
	private RegistrationMenubar registrationMenubar;
	private ImageCanvas3D canvas;

	private Point3f globalMin = new Point3f();
	private Point3f globalMax = new Point3f();
	private Point3f globalCenter = new Point3f();

	PointListDialog pld;

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

		fireContentSelected(c);

		if(c != null && ij.plugin.frame.Recorder.record)
			ij.plugin.frame.Recorder.record(
				"call", "ImageJ_3D_Viewer.select", c.name);
	}

	public void show() {
		super.show();
		menubar = new Image3DMenubar(this);
		registrationMenubar = new RegistrationMenubar(this);
		setMenubar(menubar);
		pld = new PointListDialog(win);
	}

	public void setMenubar(MenuBar mb) {
		win.setMenuBar(mb);
	}

	public void close() {
		super.close();
		removeAllContents();
		contents = null;
	}

	public void setStatus(String text) {
		win.getStatusLabel().setText("  " + text);
	}

	public Image3DMenubar getMenuBar() {
		return menubar;
	}

	public RegistrationMenubar getRegistrationMenubar() {
		return registrationMenubar;
	}

	public void recalculateGlobalMinMax(Content c) {
		Point3f cmin = c.getContent().min;
		Point3f cmax = c.getContent().max;
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

	public Content addContent(ImagePlus image, Color3f color, String name,
		int thresh, boolean[] channels, int resf, int type) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
		ensureScale(image);
		Content content = new Content(name);
		content.image = image;
		content.color = color;
		content.threshold = thresh;
		content.channels = channels;
		content.resamplingF = resf;
		content.setPointListDialog(pld);
		content.displayAs(type);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		fireContentAdded(content);
		this.addUniverseListener(content);
		return content;
	}

	public Content addVoltex(ImagePlus image, Color3f color,
		String name, int thresh, boolean[] channels, int resamplingF) {
		return addContent(image, color, name, thresh, channels,
			resamplingF, Content.VOLUME);
	}

	public Content addOrthoslice(ImagePlus image, Color3f color,
		String name, int thresh, boolean[] channels, int resamplingF) {
		return addContent(image, color, name, thresh, channels,
			resamplingF, Content.ORTHO);
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
	
	public Content addMesh(ImagePlus image, Color3f color, String name,
		int threshold, boolean[] channels, int resamplingF){
		return addContent(image, color, name, threshold, channels,
			resamplingF, Content.SURFACE);
	}

	public Content addMesh(List mesh, Color3f color,
			String name, float scale, int threshold){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleTG.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleTG.setTransform(scaletr);
		// add the mesh
 		return addMesh(mesh, color, name, threshold);
	}

	public Content addMesh(List mesh,
			Color3f color, String name, int threshold) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return null;
		}
		Content content = new Content(name);
		content.color = color;
		content.threshold = threshold;
		content.displayMesh(mesh);
		content.setPointListDialog(pld);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		fireContentAdded(content);
		return content;
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
	}

	public Iterator contents() {
		return contents.values().iterator();
	}

	public Collection getContents() {
		return contents.values();
	}
	
	public boolean contains(String name) {
		return contents.containsKey(name);
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
		fireTransformationUpdated();
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


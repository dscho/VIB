package ij3d;

import ij3d.pointlist.PointListDialog;
import ij.ImagePlus;
import ij.IJ;

import java.awt.MenuBar;
import java.awt.event.*;

import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;

import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import isosurface.MeshGroup;
import java.io.File;
import octree.FilePreparer;
import octree.VolumeOctree;

public class Image3DUniverse extends DefaultAnimatableUniverse {

	private Content selected;
	private Hashtable contents = new Hashtable();
	private Image3DMenubar menubar;
	private RegistrationMenubar registrationMenubar;
	private ImageCanvas3D canvas;

	private Executer executer;

	private Point3f globalMin = new Point3f();
	private Point3f globalMax = new Point3f();
	private Point3f globalCenter = new Point3f();

	PointListDialog pld;

	static{
		UniverseSettings.load();
	}

	public Image3DUniverse() {
		this(UniverseSettings.startupWidth, UniverseSettings.startupHeight);
	}

	public Image3DUniverse(int width, int height) {
		super(width, height);
		canvas = (ImageCanvas3D)getCanvas();
		executer = new Executer(this);

		// add mouse listeners
		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Content c = picker.getPickedContent(
						e.getX(), e.getY());
				if(c != null)
					IJ.showStatus(c.name);
				else
					IJ.showStatus("");
			}
		});
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Content c = picker.getPickedContent(
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

	@Override
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

	@Override
	public void close() {
		super.close();
		removeAllContents();
		contents = null;
	}

	public Executer getExecuter() {
		return executer;
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
		if(contents.size() == 1) {
			globalMin.set(cmin);
			globalMax.set(cmax);
		} else {
			if(cmin.x < globalMin.x) globalMin.x = cmin.x;
			if(cmin.y < globalMin.y) globalMin.y = cmin.y;
			if(cmin.z < globalMin.z) globalMin.z = cmin.z;
			if(cmax.x > globalMax.x) globalMax.x = cmax.x;
			if(cmax.y > globalMax.y) globalMax.y = cmax.y;
			if(cmax.z > globalMax.z) globalMax.z = cmax.z;
		}
		globalCenter.x = globalMin.x + (globalMax.x - globalMin.x) / 2;
		globalCenter.y = globalMin.y + (globalMax.y - globalMin.y) / 2;
		globalCenter.z = globalMin.z + (globalMax.z - globalMin.z) / 2;

		getViewPlatformTransformer().centerAt(globalCenter);
	}

	public Point3f getGlobalCenterPoint() {
		return globalCenter;
	}

	public void getGlobalCenterPoint(Point3f p) {
		p.set(globalCenter);
	}

	public void getGlobalMinPoint(Point3f p) {
		p.set(globalMin);
	}

	public void getGlobalMaxPoint(Point3f p) {
		p.set(globalMax);
	}

	public void updateOctree() {
		if(octree != null)
			octree.update();
	}

	public void cancelOctree() {
		if(octree != null)
			octree.cancel();
	}

	private VolumeOctree octree = null;

	public void removeOctree() {
		if(octree != null) {
			this.removeUniverseListener(octree);
			scene.removeChild(octree.getRootBranchGroup());
			octree = null;
		}
	}

	public VolumeOctree addOctree(String imageDir, String name) {
		if(octree != null) {
			IJ.error("Only one large volume can be displayed a time.\n" +
				"Please remove previously displayed volumes first.");
			return null;
		}
		if(contents.containsKey(name)) {
			IJ.error("Name exists already");
			return null;
		}
		try {
			octree = new VolumeOctree(imageDir, canvas);
			octree.getRootBranchGroup().compile();
			scene.addChild(octree.getRootBranchGroup());
			octree.displayInitial();
			this.addUniverseListener(octree);
			ensureScale(octree.realWorldXDim());
		} catch(Exception e) {
			e.printStackTrace();
		}
		return octree;
	}

	/*
	 * Requires an empty directory.
	 */
	public VolumeOctree createAndAddOctree(String imagePath, String dir, String name) {
		File outdir = new File(dir);
		if(!outdir.exists())
			outdir.mkdir();
		if(!outdir.isDirectory()) {
			throw new RuntimeException("Not a directory");
		}
		try {
			new FilePreparer(imagePath, VolumeOctree.SIZE, dir).createFiles();
			return addOctree(dir, name);
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public octree.VolumeOctree createAndAddOctree(ImagePlus image, String dir, String name) {
		File outdir = new File(dir);
		if(!outdir.exists())
			outdir.mkdir();
		if(!outdir.isDirectory()) {
			throw new RuntimeException("Not a directory");
		}
		try {
			new FilePreparer(image, VolumeOctree.SIZE, dir).createFiles();
			return addOctree(dir, name);
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Content addContent(ImagePlus image, Color3f color, String name,
		int thresh, boolean[] channels, int resf, int type) {
		if(contents.containsKey(name)) {
			IJ.error("Content named '"+name+"' exists already");
			return null;
		}
		Content content = new Content(name);
		content.image = image;
		content.color = color;
		content.threshold = thresh;
		content.channels = channels;
		content.resamplingF = resf;
		content.setPointListDialog(pld);
		content.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.displayAs(type);
		content.compile();
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		ensureScale(image);
		fireContentAdded(content);
		fireTransformationFinished();
		this.addUniverseListener(content);
		return content;
	}

	public Content addVoltex(ImagePlus image) {
		return addVoltex(image, null, image.getTitle(), 0,
			new boolean[] {true, true, true}, 1);
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
		ensureScale(image.getWidth() *
				(float)image.getCalibration().pixelWidth);
	}

	private float oldRange = 2f;
	private void ensureScale(float range) {
		if(range > oldRange) {
			oldRange = range;
			double d = (range) / Math.tan(Math.PI/8);
			getViewPlatformTransformer().zoomTo(d);
			getViewer().getView().setBackClipDistance(2 * d);
			getViewer().getView().setFrontClipDistance(2 * d / 100);
		}
	}

	public void resetZoom() {
		double d = oldRange / Math.tan(Math.PI/8);
		getViewPlatformTransformer().zoomTo(new Vector3d(0, 0, -1), d);
		getViewer().getView().setBackClipDistance(2 * d);
		getViewer().getView().setFrontClipDistance(2 * d / 100);
	}
	
	public Content addMesh(ImagePlus image, Color3f color, String name,
		int threshold, boolean[] channels, int resamplingF){
		return addContent(image, color, name, threshold, channels,
			resamplingF, Content.SURFACE);
	}

	public Content addMesh(List mesh, Color3f color, String name,
			float scale, int threshold) {
		Content c = addMesh(mesh, color, name, threshold);
		ensureScale(scale);
		return c;
	}

	public Content addLineMesh(List mesh,
			Color3f color, String name, int threshold, boolean strips) {
		return addLineMesh(mesh, color, name, threshold, strips,
				new LineAttributes());
	}

	public Content addLineMesh(List mesh, Color3f color, String name,
				int threshold, boolean strips, LineAttributes attrs) {
		// check if exists already
		if(contents.containsKey(name)) {
			IJ.error("Mesh named '"+name+"' exists already");
			return null;
		}
		Content content = new Content(name);
		content.color = color;
		content.threshold = threshold;
		int mode = strips ? MeshGroup.LINE_STRIPS : MeshGroup.LINES;
		content.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.displayMesh(mesh, mode, attrs);
		content.setPointListDialog(pld);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		float range = globalMax.x - globalMin.x;
		ensureScale(range);
		fireContentAdded(content);
		return content;
	}

	public Content addMesh(List mesh,
			Color3f color, String name, int threshold) {
		// check if exists already
		if(contents.containsKey(name)) {
			IJ.error("Mesh named '"+name+"' exists already");
			return null;
		}
		Content content = new Content(name);
		content.color = color;
		content.threshold = threshold;
		content.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.displayMesh(mesh);
		content.setPointListDialog(pld);
		scene.addChild(content);
		contents.put(name, content);
		recalculateGlobalMinMax(content);
		float range = globalMax.x - globalMin.x;
		ensureScale(range);
		fireContentAdded(content);
		return content;
	}

	public void removeAllContents() {
		String[] names = new String[contents.size()];
		contents.keySet().toArray(names);
		for (int i=0; i<names.length; i++)
			removeContent(names[i]);
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
		this.removeUniverseListener(content);
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
		getRotationTG().setTransform(t);
		getTranslateTG().setTransform(t);
		getZoomTG().setTransform(t);
		getZoomTG().setTransform(t);
		getViewPlatformTransformer().centerAt(globalCenter);
		resetZoom();
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
}


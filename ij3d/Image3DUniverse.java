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

public class Image3DUniverse extends SimpleUniverse 
				implements PickingCallback {

	private BranchGroup scene;
	private Content selected;
	private Hashtable contents = new Hashtable();;
	private TransformGroup rotationsTG;
	private TransformGroup scaleTG;
	private Alpha animation;
	private Triangulator triangulator = new MCTriangulator();

	public Image3DUniverse(int width, int height) {
		super(new ImageCanvas3D(width, height));
		getViewingPlatform().setNominalViewingTransform();

		BranchGroup root = new BranchGroup();
		
		scaleTG = new TransformGroup();
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleTG);

		rotationsTG = new TransformGroup();
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		scaleTG.addChild(rotationsTG);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		rotationsTG.addChild(scene);

		BoundingSphere b = new BoundingSphere();
		b.setRadius(10.0);
		new MouseNavigation(scene, getCanvas(), scaleTG, this);

		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(b);
		root.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(b);
		root.addChild(lightD1);

		// Animation
		animation = new Alpha(-1, 4000);
		RotationInterpolator rotpol = 
			new RotationInterpolator(animation, rotationsTG) {
			public void processStimulus(java.util.Enumeration e) {
				super.processStimulus(e);
				transformChanged(0, null);
			}
		};
		rotpol.setSchedulingBounds(b);
		rotationsTG.addChild(rotpol);

		root.compile();
		addBranchGraph(root);

		// add mouse listeners
		getCanvas().addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				Content c = getContentAtCanvasPosition(
						e.getX(), e.getY());
				if(c != null)
					IJ.showStatus(c.name);
				else
					IJ.showStatus("");
			}
		});
		getCanvas().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Content c = getContentAtCanvasPosition(
						e.getX(), e.getY());
				if(selected != null){
					selected.setSelected(false);
					selected = null;
				}
				if(c != null) {
					c.setSelected(true);
					selected = c;
				}
				String st = c != null ? c.name : "none";
				IJ.showStatus("selected: " + st);
			}
		});
	}

	public void addVoltex(ImagePlus image, String color, 
			String name, boolean[] channels, int resamplingF) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		VoltexGroup content = new VoltexGroup(
						name, color, image, channels, resamplingF);
		scene.addChild(content);
		contents.put(name, content);
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
	
	public void addMesh(ImagePlus image, String color, 
		String name, int threshold, boolean[] channels, int resamplingF) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		MeshGroup meshG = new MeshGroup(
				name, color, image, channels, resamplingF, threshold);
		scene.addChild(meshG);
		contents.put(name, meshG);
	}

	public void addMesh(List mesh, String color, 
					String name, float scale, int threshold){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleTG.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleTG.setTransform(scaletr);
		// add the mesh
		addMesh(mesh, color, name, 1, threshold);
	}

	public void addMesh(List mesh, 
				String color, String name, int threshold) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
	
		MeshGroup meshG = new MeshGroup(
				name, color, mesh, threshold);
		scene.addChild(meshG);
		contents.put(name, meshG);
	}

	public void removeContent (String name) {
		Content content = (Content)contents.get(name);
		if(content == null)
			return;
		scene.removeChild(content);
		contents.remove(name);
		if(selected == content)
			selected = null;
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
		PickCanvas pickCanvas = new PickCanvas(getCanvas(), scene); 
		pickCanvas.setMode(PickCanvas.GEOMETRY);
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeLocation(x, y); 
		PickResult result = null;
		try {
			result = pickCanvas.pickClosest();
		} catch(NullPointerException e) {}
		if(result == null) 
			return null;
		Content content = (Content)result.getNode(PickResult.BRANCH_GROUP);
		if(content== null)
			return null;
		return content;
	}
		

	private ImageStack stack;
	private boolean recording = false;
	private boolean animated = true;
	private float animationValue = 0;
	private boolean doRecord = false;
	private float oldValue = 0f;

	public void transformChanged(int type, TransformGroup tg) {
		Iterator it = contents.values().iterator();
		while(it.hasNext()) {
			Content c = (Content)it.next();
			c.eyePtChanged(getViewer().getView());
		}
		if(recording && animated) {
			boolean newLoop = animation.value() < oldValue;
			oldValue = animation.value();
			if(!doRecord && newLoop) {
				doRecord = true;
			} else if(doRecord && newLoop) {
				doRecord = false;
				oldValue = 0f;
				ImagePlus mov = stopRecording();
				if(mov != null) mov.show();
			}
		} else  {
			doRecord = recording;
		}
		
		// add actual image to recording stack
		if(!doRecord) 
			return;
		ImageWindow3D win = (ImageWindow3D)getCanvas().getParent();
		ImageProcessor ip = win.getImagePlus().getProcessor();
		int w = ip.getWidth(), h = ip.getHeight();
		if(stack == null) 
			stack = new ImageStack(w, h);
		stack.addSlice("", ip);
	}

	public void startRecording() {
		recording = true;
		if(animated) {
			animationValue = animation.value();
		}
	}

	public ImagePlus stopRecording() {
		recording = false;
		if(stack == null)
			return null;
		ImagePlus imp = new ImagePlus("Movie", stack);
		stack = null;
		return imp;
	}

	public void startAnimation() {
		animation.resume();
		animated = true;
	}

	public void pauseAnimation() {
		animation.pause();
		animated = false;
	}

	public void show() {
		ImageWindow3D win = new ImageWindow3D("ImageJ 3D Viewer", this);
		win.setMenuBar(new Image3DMenubar(this));
	}
} 

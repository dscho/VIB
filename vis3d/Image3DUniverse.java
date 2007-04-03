package vis3d;

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
import javax.vecmath.*;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

public class Image3DUniverse extends SimpleUniverse 
				implements PickingCallback {

	private BranchGroup scene;
	private Hashtable contents = new Hashtable();;
	private TransformGroup scaleTG;
	private Triangulator triangulator = new MCTriangulator();

	public Image3DUniverse() {
		super(new ImageCanvas3D(getPreferredConfiguration(), 512, 512));
		getViewingPlatform().setNominalViewingTransform();

		BranchGroup root = new BranchGroup();
		
		scaleTG = new TransformGroup();
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleTG);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		scaleTG.addChild(scene);

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
	}

	public Image3DUniverse(ImagePlus image, int threshold, 
						Color3f color, String name) {
		this();
		addVoltex(image, color, name);
		//addMesh(image, threshold, color, name);
	}

	public void addVoltex(ImagePlus image, Color3f color, String name) {
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		VoltexGroup content = new VoltexGroup(name, color, image);
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
	
	public void addMesh(ImagePlus image, int threshold, 
						Color3f color, String name) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		ensureScale(image);
		List mesh = triangulator.getTriangles(image, threshold);
		addMesh(mesh, color, name);
	}

	public void addMesh(List mesh, Color3f color, String name, float scale){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleTG.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleTG.setTransform(scaletr);
		// add the mesh
		addMesh(mesh, color, name);
	}

	public void addMesh(List mesh, Color3f color, String name) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
	
		MeshGroup meshG = new MeshGroup(name, color, mesh);
		scene.addChild(meshG);
		contents.put(name, meshG);
	}

	public void removeContentAt(int x, int y) {
		Content content = getContentAtCanvasPosition(x, y);
		if(content == null)
			return;
		scene.removeChild(content);
		contents.remove(content.name);
	}

	public void removeContent (String name) {
		Content content = (Content)contents.get(name);
		if(content == null)
			return;
		scene.removeChild(content);
		contents.remove(name);
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
	public boolean recording = false;

	public void transformChanged(int type, TransformGroup tg) {
		Iterator it = contents.values().iterator();
		while(it.hasNext()) {
			Content c = (Content)it.next();
			c.eyePtChanged(getViewer().getView());
		}
		if(!recording) return;

		ImageWindow3D win = (ImageWindow3D)getCanvas().getParent();
		ImageProcessor ip = win.getImagePlus().getProcessor();
		int w = ip.getWidth(), h = ip.getHeight();
		
		if(stack == null) stack = new ImageStack(w, h);
		stack.addSlice("", ip);
	}

	public ImagePlus stopRecording() {
		recording = false;
		if(stack == null)
			return null;
		ImagePlus imp = new ImagePlus("Movie", stack);
		stack = null;
		return imp;
	}
} 

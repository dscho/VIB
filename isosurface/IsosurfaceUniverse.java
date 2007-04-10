package isosurface;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.IJ;

import ij3d.ImageWindow3D;

import marchingcubes.MCTriangulator;

import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;

import java.util.List;
import java.util.Hashtable;

import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.behaviors.picking.*;
import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;

public class IsosurfaceUniverse extends SimpleUniverse 
				implements PickingCallback {

	private BranchGroup scene;
	private Hashtable contents = new Hashtable();;
	private TransformGroup scaleGr;
	private Triangulator triangulator = new MCTriangulator();

	public IsosurfaceUniverse() {
		super(new Canvas3D(SimpleUniverse.getPreferredConfiguration()));
		getViewingPlatform().setNominalViewingTransform();

		BranchGroup root = new BranchGroup();

		scaleGr = new TransformGroup();
		scaleGr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleGr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleGr);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		scaleGr.addChild(scene);

		// Picking
		BoundingSphere b = new BoundingSphere();
		int mode = PickObject.USE_GEOMETRY;
		PickRotateBehavior rot = new PickRotateBehavior(scene, 
							getCanvas(), b, mode); 
		rot.setupCallback(this);
		scene.addChild(rot);
		
		scene.addChild(
			new PickTranslateBehavior(scene, getCanvas(), b, mode)); 
		MouseZoom myMouseZoom = new MouseZoom();
		myMouseZoom.setTransformGroup(scaleGr);
		myMouseZoom.setSchedulingBounds(b);
		scene.addChild(myMouseZoom);

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
					IJ.showStatus(c.shape.name);
				else
					IJ.showStatus("");
			}
		});
	}

	public IsosurfaceUniverse(ImagePlus image, int threshold, 
						Color3f color, String name) {
		this();
		addImage(image, threshold, color, name);
	}
	
	public void addImage(ImagePlus image, int threshold, 
						Color3f color, String name) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
		// correct global scaling transformation
		Transform3D scale= new Transform3D();
		scaleGr.getTransform(scale);
		float oldXRange = (float)scale.getScale();

		float xRange = image.getWidth() * 
				(float)image.getCalibration().pixelWidth;
		if(xRange > oldXRange) {
			scale.setScale(1/xRange);
			scaleGr.setTransform(scale);
		}
		List mesh = triangulator.getTriangles(image, threshold);
		addMesh(mesh, color, name, threshold);
	}

	public void addMesh(List mesh, Color3f color, String name, int threshold, 
										float scale){
		// correct global scaling transformation
		Transform3D scaletr = new Transform3D();
		scaleGr.getTransform(scaletr);
		scaletr.setScale(scale);
		scaleGr.setTransform(scaletr);
		// add the mesh
		addMesh(mesh, color, name, threshold);
	}

	public void addMesh(List mesh, Color3f color, String name,int threshold) {
		// check if exists already
		if(contents.contains(name)) {
			IJ.error("Name exists already");
			return;
		}
	
		// create BranchGroup for this image
		BranchGroup obj = new BranchGroup();
		obj.setCapability(BranchGroup.ALLOW_DETACH);

		// create transformation for pickeing
		TransformGroup pickTr= new TransformGroup();
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		pickTr.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		obj.addChild(pickTr);

		// create the IsoShape for this image and add it
		IsoShape shape = new IsoShape(mesh, color, name, threshold);
		pickTr.addChild(shape);

		obj.compile();
		scene.addChild(obj);
		contents.put(name, new Content(shape, obj));
	}

	public void removeImageAt(int x, int y) {
		Content content = getContentAtCanvasPosition(x, y);
		if(content == null)
			return;
		scene.removeChild(content.bg);
		contents.remove(content.shape.name);
	}

	public void removeImage(String name) {
		Content content = (Content)contents.get(name);
		if(content == null)
			return;
		BranchGroup bg = content.bg;
		scene.removeChild(bg);
		contents.remove(name);
	}

	private Content getContentAtCanvasPosition(int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(getCanvas(), scene); 
		pickCanvas.setMode(PickTool.GEOMETRY);
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeCylinderRay(new Point3d(0,0,0),
				new Vector3d(0,0,1), 20.0); 
		pickCanvas.setShapeLocation(x, y); 
		PickResult result = pickCanvas.pickClosest(); 
		if(result == null) 
			return null;
		IsoShape shape = (IsoShape)result.getNode(PickResult.SHAPE3D);
		if(shape == null)
			return null;
		String name = shape.name;
		return (Content)contents.get(name);
	}
		

	private static class Content {
		private IsoShape shape;
		private BranchGroup bg;

		public Content(IsoShape shape, BranchGroup bg) {
			this.shape = shape;
			this.bg = bg;
		}
	}

	private ImageStack stack;
	public boolean recording = false;

	public void transformChanged(int type, TransformGroup tg) {
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

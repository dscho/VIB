package isosurface;

import ij.ImagePlus;

import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;

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

public class IsosurfaceUniverse extends SimpleUniverse {

	private BranchGroup scene;
	private Hashtable contents = new Hashtable();;
	private TransformGroup scaleGr;

	public IsosurfaceUniverse() {
		super(new Canvas3D(SimpleUniverse.getPreferredConfiguration()));
		getViewingPlatform().setNominalViewingTransform();

		BranchGroup root = new BranchGroup();

		scaleGr = new TransformGroup();
		scaleGr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleGr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleGr);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_DETACH);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scaleGr.addChild(scene);

		// Picking
		BoundingSphere b = new BoundingSphere();
		int mode = PickObject.USE_GEOMETRY;
		scene.addChild(
			new PickRotateBehavior(scene, getCanvas(), b, mode)); 
		scene.addChild(
			new PickTranslateBehavior(scene, getCanvas(), b, mode)); 
		MouseZoom myMouseZoom = new MouseZoom();
		myMouseZoom.setTransformGroup(scaleGr);
		myMouseZoom.setSchedulingBounds(b);
		scene.addChild(myMouseZoom);

		root.compile();
		addBranchGraph(root);
	}

	public IsosurfaceUniverse(ImagePlus image,int threshold,Color3f color) {
		this();
		addImage(image, threshold, color);
	}
	
	public void addImage(ImagePlus image, int threshold, Color3f color) {

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

		// create BranchGroup for this image
		BranchGroup obj = new BranchGroup();

		// create transformation for pickeing
		TransformGroup pickTr= new TransformGroup();
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		pickTr.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		obj.addChild(pickTr);

		// create the IsoShape for this image and add it
		pickTr.addChild(new IsoShape(image, threshold, color));
		
		// Lightening
		BoundingSphere b = new BoundingSphere();
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(b);
		obj.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(new BoundingSphere(b));
		obj.addChild(lightD1);

		scene.addChild(obj);
		contents.put(image.getTitle(), 
				new Content(image, image.getTitle(), obj));
	} 

	private static class Content {
		private ImagePlus image;
		private String name;
		private BranchGroup bg;

		public Content(ImagePlus image, String name, BranchGroup bg) {
			this.image = image;
			this.name = name;
			this.bg = bg;
		}
	}
} 

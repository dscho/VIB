package isosurface;

import ij.ImagePlus;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;
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

	public IsosurfaceUniverse() {
		super(new Canvas3D(SimpleUniverse.getPreferredConfiguration()));
		getViewingPlatform().setNominalViewingTransform();
		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_DETACH);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.compile();
		addBranchGraph(scene);
	}

	public IsosurfaceUniverse(ImagePlus image, int threshold, Color3f color) {
		this();
		addImage(image, threshold, color);
	}
	
	// TODO scale together
	public void addImage(ImagePlus image, int threshold, Color3f color) {

		BranchGroup obj = new BranchGroup();
		// create initial transformation
		Transform3D scale = new Transform3D();
		float xRange = image.getWidth() * 
								(float)image.getCalibration().pixelWidth;
		scale.setScale(1/xRange);

		// create TransformGroup and set capabilities
        TransformGroup objTransform = new TransformGroup(scale);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTransform.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		objTransform.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		obj.addChild(objTransform);
		
		objTransform.addChild(new IsoShape(image, threshold, color));
		
		// Picking
		BoundingSphere b = new BoundingSphere();
		b.setRadius(2000.0);

		MouseRotate myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(objTransform);
		myMouseRotate.setSchedulingBounds(b);
		obj.addChild(myMouseRotate);
		MouseTranslate myMouseTranslate = new MouseTranslate();
		myMouseTranslate.setTransformGroup(objTransform);
		myMouseTranslate.setSchedulingBounds(b);
		obj.addChild(myMouseTranslate);
		MouseZoom myMouseZoom = new MouseZoom();
		myMouseZoom.setTransformGroup(objTransform);
		myMouseZoom.setSchedulingBounds(b);
		obj.addChild(myMouseZoom);

	
		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(b);
		obj.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(new BoundingSphere(b));
		obj.addChild(lightD1);

		scene.addChild(obj);
	} 
} 

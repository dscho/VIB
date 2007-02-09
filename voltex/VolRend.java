package voltex;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.util.Enumeration;
import com.sun.j3d.utils.behaviors.mouse.*;
import java.net.*;
import ij.ImageStack; 
import ij.ImagePlus;

/**
 * The base class for VolRend applets and applications.  Sets up the basic
 * scene graph structure and processes changes to the attributes
 */
public class VolRend implements MouseBehaviorCallback {

    private static final int POST_AWT_CHANGE = 1;

    Volume volume;
    Renderer renderer;
    View view; // primary view for renderers

    UpdateBehavior updateBehavior;


    TransformGroup objectGroup;
    TransformGroup centerGroup;
    Transform3D centerXform = new Transform3D();
    Vector3d centerOffset = new Vector3d(-0.5, -0.5, -0.5);

    Group dynamicAttachGroup;

    Canvas3D 		canvas;

    int			volEditId = -1;

    public VolRend() {
	    canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
    }

    Canvas3D getCanvas() {
		return canvas;
    }

    void setupScene() {

		// Setup the graphics
		// Create a simple scene and attach it to the virtual universe
		BranchGroup scene = createSceneGraph();
		scene.compile();
		SimpleUniverse u = new SimpleUniverse(canvas);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
		u.getViewingPlatform().setNominalViewingTransform();

		// get the primary view
		view = u.getViewer().getView();

		// switch to a parallel projection, which is faster for texture mapping
		//view.setProjectionPolicy(View.PARALLEL_PROJECTION);
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);

		u.addBranchGraph(scene);

		canvas.setDoubleBufferEnable(true);

		// setup the renderers
		renderer = new Axis2DRenderer(view, volume);

		// Add the volume to the scene
		clearAttach();
		renderer.attach(dynamicAttachGroup);

    }

    void update() {
		System.out.println("VolRend.update()");
		updateBehavior.postId(POST_AWT_CHANGE);
    }

    void initContext(ImagePlus imp) {
		// initialize the volume
		volume = new Volume(imp);

		// initialize the scene graph
		setupScene();
    }

    private void doUpdate() {
		System.out.println("VolRend.doUpdate()");
		canvas.setDoubleBufferEnable(true);
		//view.setProjectionPolicy(View.PARALLEL_PROJECTION);
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
		renderer.update();
		int newVolEditId;
		if ((newVolEditId = volume.update()) != volEditId) {
			updateCenter(volume.minCoord, volume.maxCoord);
			newVolEditId = volEditId;
		}
    }


    private class UpdateBehavior extends Behavior {
		WakeupCriterion criterion[] = {
			new WakeupOnBehaviorPost(null, POST_AWT_CHANGE)
		};
		
		WakeupCondition conditions = new WakeupOr( criterion );

		public void initialize() {
			wakeupOn(conditions);
		}

		public void processStimulus( Enumeration criteria) {
			// Do the update
			doUpdate();
			wakeupOn(conditions);
		}
    }

    void updateCenter(Point3d minCoord, Point3d maxCoord) {
		centerOffset.x = -(maxCoord.x - minCoord.x)/2.0;
		centerOffset.y = -(maxCoord.y - minCoord.y)/2.0;
		centerOffset.z = -(maxCoord.z - minCoord.z)/2.0;
		centerXform.setTranslation(centerOffset);
		centerGroup.setTransform(centerXform);
    }

    BranchGroup createSceneGraph() {

		Color3f lColor1 = new Color3f(0.7f, 0.7f, 0.7f);
		Vector3f lDir1  = new Vector3f(0.0f, 0.0f, 1.0f);
		Color3f alColor = new Color3f(1.0f, 1.0f, 1.0f);

		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		// Create a transform group to scale the whole scene
		TransformGroup scaleGroup = new TransformGroup();
		Transform3D scaleXform = new Transform3D();
		double scale = 1.2;
		scaleXform.setScale(scale);
		scaleGroup.setTransform(scaleXform);
		objRoot.addChild(scaleGroup);

		// Create the static ordered group
		OrderedGroup scaleOGroup = new OrderedGroup();
		scaleGroup.addChild(scaleOGroup);


		// Create a TG at the origin
		objectGroup = new TransformGroup();

		// Enable the TRANSFORM_WRITE capability so that our behavior code
		// can modify it at runtime.  Add it to the root of the subgraph.
		//
		objectGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objectGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleOGroup.addChild(objectGroup);


		// Create the transform group node and initialize it center the
		// object around the origin
		centerGroup = new TransformGroup();
		updateCenter(new Point3d(0.0, 0.0, 0.0), new Point3d(1.0, 1.0, 1.0));
		centerGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objectGroup.addChild(centerGroup);

		// Set up the annotation/volume/annotation sandwitch
		OrderedGroup centerOGroup = new OrderedGroup();
		centerGroup.addChild(centerOGroup);

		// create the dynamic attachment point
		dynamicAttachGroup = new Group();
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		centerOGroup.addChild(dynamicAttachGroup);

		BoundingSphere bounds =
				new BoundingSphere(new Point3d(0.0,0.0,0.0), 100000.0);

		MouseRotate mr = new MouseRotate();
		mr.setupCallback(this);
		mr.setTransformGroup(objectGroup);
		mr.setSchedulingBounds(bounds);
		mr.setFactor(0.007);
		objRoot.addChild(mr);
		
		MouseTranslate mt = new MouseTranslate();
		mt.setTransformGroup(objectGroup);
		mt.setSchedulingBounds(bounds);
		objRoot.addChild(mt);
		MouseZoom mz = new MouseZoom();
		mz.setTransformGroup(objectGroup);
		mz.setSchedulingBounds(bounds);
		mz.setFactor(0.1);
		objRoot.addChild(mz);
		
		updateBehavior = new UpdateBehavior();
		updateBehavior.setSchedulingBounds(bounds);
		objRoot.addChild(updateBehavior);

		return objRoot;
    }

    private void clearAttach() {
		while(dynamicAttachGroup.numChildren() > 0) {
			dynamicAttachGroup.removeChild(0);
		}
    }

    public void transformChanged(int type, Transform3D xform) {
		renderer.eyePtChanged();
    }
}

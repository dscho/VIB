package voltex;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;

import javax.swing.*;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.geometry.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.net.*;

import ij.process.PolygonFiller;
import ij.process.ImageProcessor;

import ij.IJ;
import ij.ImageStack; 
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij3d.RoiCanvas3D;

/**
 * The base class for VolRend applets and applications.  Sets up the basic
 * scene graph structure and processes changes to the attributes
 */
public class VolRend implements MouseBehaviorCallback {

    Volume volume;
	private ImagePlus image;
    Renderer renderer;
    View view; // primary view for renderers

	private MouseNavigation navigation;
	private TransformGroup objectGroup;
    TransformGroup centerGroup;
    Transform3D centerXform = new Transform3D();

	private CoordinateSystem coordBG;    
	private Vector3d centerOffset = new Vector3d(-0.5, -0.5, -0.5);

	BranchGroup scene;
    Group dynamicAttachGroup;

	SimpleUniverse simpleU;
    RoiCanvas3D canvas;

    int	volEditId = -1;

    public VolRend() {
		canvas = new RoiCanvas3D(SimpleUniverse.getPreferredConfiguration());
		CanvasPopup popup = new CanvasPopup(this);
	}

    public RoiCanvas3D getCanvas() {
		return canvas;
    }

	public SimpleUniverse getUniverse() {
		return simpleU;
	}

    void setupScene() {

		// Setup the graphics
		// Create a simple scene and attach it to the virtual universe
		scene = createSceneGraph();
		scene.compile();
		simpleU = new SimpleUniverse(canvas);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		// get the primary view
		view = simpleU.getViewer().getView();

		// switch to a parallel projection, which is faster for texture mapping
		//view.setProjectionPolicy(View.PARALLEL_PROJECTION);
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);

		simpleU.addBranchGraph(scene);

		canvas.setDoubleBufferEnable(true);

		// setup the renderers
		renderer = new Axis2DRenderer(view, volume);

		// Add the volume to the scene
		renderer.attach(dynamicAttachGroup);

    }

    public void update() {
		System.out.println("VolRend.update()");
		doUpdate();
    }

    public void initContext(ImagePlus imp) {
		this.image = imp;
		volume = new Volume(imp);
		setupScene();
    }

    private void doUpdate() {
		renderer.update();
		int newVolEditId;
		if ((newVolEditId = volume.update()) != volEditId) {
			updateCenter(volume.minCoord, volume.maxCoord);
			newVolEditId = volEditId;
		}
    }

    private void updateCenter(Point3d minCoord, Point3d maxCoord) {
		centerOffset.x = -(maxCoord.x - minCoord.x)/2.0;
		centerOffset.y = -(maxCoord.y - minCoord.y)/2.0;
		centerOffset.z = -(maxCoord.z - minCoord.z)/2.0;
		centerXform.setTranslation(centerOffset);
		centerGroup.setTransform(centerXform);
    }

    BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		// Create a TG at the origin
		objectGroup = new TransformGroup();

		// Enable the TRANSFORM_WRITE capability so that our behavior code
		// can modify it at runtime.  Add it to the root of the subgraph.
		//
		objectGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objectGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objRoot.addChild(objectGroup);


		// Create the transform group node and initialize it center the
		// object around the origin
		centerGroup = new TransformGroup();
		updateCenter(new Point3d(0.0, 0.0, 0.0), new Point3d(1.0, 1.0, 1.0));
		centerGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		centerGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		centerGroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		centerGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		centerGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		objectGroup.addChild(centerGroup);

		// create the dynamic attachment point
		dynamicAttachGroup = new Group();
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		centerGroup.addChild(dynamicAttachGroup);

		// attach the coordinate system
		coordBG = new CoordinateSystem();
		coordBG.setCapability(BranchGroup.ALLOW_DETACH);
		centerGroup.addChild(coordBG);

		navigation = new MouseNavigation(objectGroup, objRoot, this);
		return objRoot;
    }

	public void showCoordinateSystem(boolean flag) {
		if(flag)
			centerGroup.addChild(coordBG);
		else 
			centerGroup.removeChild(coordBG);
	}

	public void resetView() {
		navigation.resetView();
		renderer.eyePtChanged();		
	}

    public void transformChanged(int type, Transform3D xform) {
		renderer.eyePtChanged();
    }

	public Point2d volumePointInCanvas(int x, int y, int z) {
		double px = x * volume.xSpace;
		double py = y * volume.ySpace;
		double pz = z * volume.zSpace;
		Point3d locInImagePlate = new Point3d(px, py, pz);
		
		Transform3D toVWorld = new Transform3D();
		dynamicAttachGroup.getLocalToVworld(toVWorld);
		toVWorld.transform(locInImagePlate);

		Transform3D toImagePlate = new Transform3D();
		canvas.getImagePlateToVworld(toImagePlate);
		toImagePlate.invert();
		toImagePlate.transform(locInImagePlate);

		Point2d onCanvas = new Point2d();
		canvas.getPixelLocationFromImagePlate(locInImagePlate, onCanvas);

		return onCanvas;
	}

	public void fillRoiBlack(byte fillValue) {
		Polygon p = canvas.getPolygon();
		int w = image.getWidth(), h = image.getHeight();
		int d = image.getStackSize();
		for(int z = 0; z < d; z++) {
			byte[] data =(byte[])image.getStack().
								getProcessor(z+1).getPixels();
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int index = y * w + x;
					Point2d onCanvas = volumePointInCanvas(x, y, z);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						data[index] = fillValue;
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, d);
		}
	}
}

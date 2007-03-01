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
import ij3d.ImageCanvas3D;

/**
 * The base class for VolRend applets and applications.  Sets up the basic
 * scene graph structure and processes changes to the attributes
 */
public class VolRend implements MouseBehaviorCallback {

    private List<Renderer> renderer = new ArrayList<Renderer>();

	private MouseNavigation navigation;

	private TransformGroup objectTG;
    private TransformGroup centerTG;
    private Transform3D centerXform = new Transform3D();

	private CoordinateSystem coordBG;    

    private BranchGroup dynamicAttachGroup;

	private SimpleUniverse simpleU;
    private ImageCanvas3D canvas;

    private int volEditId = -1;

    public VolRend() {
		canvas = new ImageCanvas3D(SimpleUniverse.getPreferredConfiguration(), 
						512, 512);
		CanvasPopup popup = new CanvasPopup(this);
		setupUniverse();
	}

    public ImageCanvas3D getCanvas() {
		return canvas;
    }

	public SimpleUniverse getUniverse() {
		return simpleU;
	}

	public List<Renderer> getRenderer() {
		return renderer;
	}

	public Renderer getLastRenderer() {
		return renderer.get(renderer.size()-1);
	}

    private void setupUniverse() {
		// Setup the graphics
		// Create a simple scene and attach it to the virtual universe
		BranchGroup scene = createSceneGraph();
		scene.compile();
		simpleU = new SimpleUniverse(canvas);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		// get the primary view
		View view = simpleU.getViewer().getView();

		// switch to a parallel projection, which is faster for texture mapping
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
		simpleU.addBranchGraph(scene);
		canvas.setDoubleBufferEnable(true);
    }

    public void update() {
		for(Renderer rend : renderer) {
			int newVolEditId;
			Volume volume = rend.volume;
			if ((newVolEditId = volume.update()) != volEditId) {
				updateCenter(volume.minCoord, volume.maxCoord);
				newVolEditId = volEditId;
			}
			rend.update();
		}
    }

    public void initContext(ImagePlus imp, Color3f color, boolean replace) {
		if(renderer != null && renderer.size() != 0 && replace) {
			Renderer rend = getLastRenderer();
			dynamicAttachGroup.removeChild(rend.getVolumeNode());
		}
		View view = simpleU.getViewer().getView();
		Renderer rend = new Axis2DRenderer(view, imp, color);
		renderer.add(rend);
		dynamicAttachGroup.addChild(rend.getVolumeNode());
		update();
    }

	public void reload() {
		update();
	}

    private void updateCenter(Point3d minCoord, Point3d maxCoord) {
		Vector3d centerOffset = new Vector3d();
		centerOffset.x = -(maxCoord.x - minCoord.x)/2.0;
		centerOffset.y = -(maxCoord.y - minCoord.y)/2.0;
		centerOffset.z = -(maxCoord.z - minCoord.z)/2.0;
		centerXform.setTranslation(centerOffset);
		centerTG.setTransform(centerXform);
    }

	private BranchGroup objRoot;

    private BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		objRoot = new BranchGroup();

		// Create a TG at the origin
		objectTG = new TransformGroup();

		// Enable the TRANSFORM_WRITE capability so that our behavior code
		// can modify it at runtime.  Add it to the root of the subgraph.
		//
		objectTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objectTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objRoot.addChild(objectTG);


		// Create the transform group node and initialize it center the
		// object around the origin
		centerTG = new TransformGroup();
		updateCenter(new Point3d(0.0, 0.0, 0.0), new Point3d(1.0, 1.0, 1.0));
		centerTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		centerTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		centerTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		centerTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		centerTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		centerTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		objectTG.addChild(centerTG);

		// create the dynamic attachment point
		dynamicAttachGroup = new BranchGroup();
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		dynamicAttachGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		centerTG.addChild(dynamicAttachGroup);

		// attach the coordinate system
		coordBG = new CoordinateSystem();
		coordBG.setCapability(BranchGroup.ALLOW_DETACH);
		showCoordinateSystem(true);

		// attach the Mouse tools
		navigation = new MouseNavigation(objectTG, objRoot, this);

		return objRoot;
    }

	public void showCoordinateSystem(boolean flag) {
		if(flag)
			centerTG.addChild(coordBG);
		else 
			centerTG.removeChild(coordBG);
	}

	public void setPerspectiveProjection(boolean flag) {
		int policy = flag ? View.PERSPECTIVE_PROJECTION 
							: View.PARALLEL_PROJECTION;
		simpleU.getViewer().getView().setProjectionPolicy(policy);
	}

	public void resetView() {
		navigation.resetView();
		for(Renderer rend : renderer) {
			rend.eyePtChanged();		
		}
	}

    public void transformChanged(int type, Transform3D xform) {
		for(Renderer rend : renderer) {
			rend.eyePtChanged();
		}
    }

	public Transform3D volumeToImagePlate() {
		Transform3D toVWorld = new Transform3D();
		dynamicAttachGroup.getLocalToVworld(toVWorld);
		
		Transform3D toImagePlate = new Transform3D();
		canvas.getImagePlateToVworld(toImagePlate);
		toImagePlate.invert();

		toImagePlate.mul(toVWorld);
		return toImagePlate;
	}

	public Point2d volumePointInCanvas(Renderer rend, Transform3D volToIP,
														int x,int y,int z) {
		
		Volume volume = rend.volume;
		double px = x * volume.xSpace;
		double py = y * volume.ySpace;
		double pz = z * volume.zSpace;
		Point3d locInImagePlate = new Point3d(px, py, pz);
		
		volToIP.transform(locInImagePlate);

		Point2d onCanvas = new Point2d();
		canvas.getPixelLocationFromImagePlate(locInImagePlate, onCanvas);

		return onCanvas;
	}

	public void fillRoiBlack(byte fillValue) {
		Polygon p = canvas.getRoi().getPolygon();
		Transform3D volToIP = volumeToImagePlate();
		for(Renderer rend : renderer) {
			ImagePlus image = rend.image;
			int w = image.getWidth(), h = image.getHeight();
			int d = image.getStackSize();
			for(int z = 0; z < d; z++) {
				byte[] data =(byte[])image.getStack().
									getProcessor(z+1).getPixels();
				for(int y = 0; y < h; y++) {
					for(int x = 0; x < w; x++) {
						int index = y * w + x;
						Point2d onCanvas = 
								volumePointInCanvas(rend,volToIP,x,y,z);
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
}

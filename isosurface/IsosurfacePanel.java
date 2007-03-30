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

public class IsosurfacePanel extends Panel {

	public SimpleUniverse simpleU;
	public Canvas3D canvas;
	private BranchGroup scene;

	private TransformGroup objTransform;

	public BranchGroup createSceneGraph(ImagePlus image, int seekValue) {

		BranchGroup objRoot = new BranchGroup();
		
		// create initial transformation
		Transform3D scale = new Transform3D();
		float xRange = image.getWidth() * 
								(float)image.getCalibration().pixelWidth;
		scale.setScale(1/xRange);

		// create TransformGroup and set capabilities
        objTransform = new TransformGroup(scale);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTransform.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		objTransform.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		objRoot.addChild(objTransform);
		
		objTransform.addChild(new IsoShape(image, seekValue));
		
		// Picking
		BoundingSphere b = new BoundingSphere();
		b.setRadius(2000.0);

		MouseRotate myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(objTransform);
		myMouseRotate.setSchedulingBounds(b);
		objRoot.addChild(myMouseRotate);
		MouseTranslate myMouseTranslate = new MouseTranslate();
		myMouseTranslate.setTransformGroup(objTransform);
		myMouseTranslate.setSchedulingBounds(b);
		objRoot.addChild(myMouseTranslate);
		MouseZoom myMouseZoom = new MouseZoom();
		myMouseZoom.setTransformGroup(objTransform);
		myMouseZoom.setSchedulingBounds(b);
		objRoot.addChild(myMouseZoom);

	
		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(b);
		objRoot.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(new BoundingSphere(b));
		objRoot.addChild(lightD1);
		
		objRoot.setCapability(BranchGroup.ALLOW_DETACH);
		return objRoot;
	} 

	// Create a simple scene and attach it to the virtual universe

	public IsosurfacePanel(ImagePlus image, int seekValue) {
		setLayout(new BorderLayout());
	
		GraphicsConfiguration config =
		   SimpleUniverse.getPreferredConfiguration();

		canvas = new Canvas3D(config);
		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Point3d location = getPointForMouseEvent(e.getX(), e.getY());
				
				if(location != null) {
					Transform3D transl = new Transform3D();
					transl.setTranslation(new Vector3f(location));
					TransformGroup tg = new TransformGroup(transl);
					Sphere sphere = new Sphere(1.0f);
					Appearance papp = new Appearance();
					ColoringAttributes cattr = new ColoringAttributes(
							1.0f, 1.0f, 0.0f, ColoringAttributes.FASTEST);
					papp.setColoringAttributes(cattr);
					sphere.setAppearance(papp);
					tg.addChild(sphere);
					BranchGroup bg = new BranchGroup();
					bg.addChild(tg);
					bg.compile();
					objTransform.addChild(bg);
				}
			}
		});
		add("Center", canvas);

		scene = createSceneGraph(image, seekValue);

		// SimpleUniverse is a Convenience Utility class
		simpleU = new SimpleUniverse(canvas);

		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		
		scene.compile();

		simpleU.addBranchGraph(scene);
	}

	public Point3d getPointForMouseEvent(int x, int y) {
	    PickCanvas pickCanvas = new PickCanvas(canvas, scene); 
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO); 
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeCylinderRay(new Point3d(0,0,0),
				new Vector3d(0,0,1), 20.0); 
		pickCanvas.setShapeLocation(x, y); 
		PickResult result = pickCanvas.pickClosest(); 
		if(result == null) 
			return null;
		PickIntersection intersection = result.getIntersection(0);
		Point3d point = intersection.getPointCoordinates();
		/*
		int intersectionCount = result.numIntersections();
		for(int j=0; j<intersectionCount; j++) {
			PickIntersection intersection = result.getIntersection(j); 
			Point3d point = intersection.getPointCoordinates(); 
			GeometryArray geometryArr = intersection.getGeometryArray();
			int vertexCount = geometryArr.getVertexCount();
			Point3d closestVertexCoord = 
				intersection.getClosestVertexCoordinates();
			Point3d coord = new Point3d();
			Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
			for(int i=0; i<vertexCount; i++){
				geometryArr.getCoordinate(i, coord);
				if(coord.equals(closestVertexCoord)){
					geometryArr.setColor(i,red);
				}
			}
		}*/
		return point; 
	}

	public void updateShape(ImagePlus image, int seekValue){
		BranchGroup old = scene;
		scene = createSceneGraph(image, seekValue);
		scene.compile();
		simpleU.getLocale().replaceBranchGraph(old, scene);
	}
} 

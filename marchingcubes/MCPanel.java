package marchingcubes;

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

public class MCPanel extends Panel {

	public SimpleUniverse simpleU;
	public Canvas3D canvas;
	private BranchGroup scene;

	private TransformGroup objTransform;

	public BranchGroup createSceneGraph(byte[][][] voxData,
			int xRange, int seekValue) {

		BranchGroup objRoot = new BranchGroup();
		
		// create initial transformation
		Transform3D scale = new Transform3D();
//		scale.setScale(1.0/xRange);
		scale.setScale(1/8.0f);

		// create TransformGroup and set capabilities
        objTransform = new TransformGroup(scale);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTransform.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		objTransform.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		objRoot.addChild(objTransform);
		
		objTransform.addChild(new MCShape(voxData, seekValue));
		
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

	public MCPanel(byte[][][] voxData, int xRange, int seekValue) {
		setLayout(new BorderLayout());
	
		GraphicsConfiguration config =
		   SimpleUniverse.getPreferredConfiguration();

		canvas = new Canvas3D(config);
		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Point3d location = getPointForMouseEvent(e);
				if(location != null) {
					Transform3D transl = new Transform3D();
					transl.setTranslation(new Vector3f(location));
					TransformGroup tg = new TransformGroup(transl);
					tg.addChild(new Sphere(1.0f));
					BranchGroup bg = new BranchGroup();
					bg.addChild(tg);
					bg.compile();
					objTransform.addChild(bg);
				}
			}
		});
		add("Center", canvas);

		scene = createSceneGraph(voxData, xRange, seekValue);

		// SimpleUniverse is a Convenience Utility class
		simpleU = new SimpleUniverse(canvas);

		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		TransformGroup tg = simpleU.getViewingPlatform().
								getViewPlatformTransform();
		Transform3D transl = new Transform3D();
		transl.setTranslation(new Vector3f(0, 0, 10.0f));
		tg.setTransform(transl);
//		simpleU.getViewingPlatform().setNominalViewingTransform();

		
		scene.compile();

		simpleU.addBranchGraph(scene);
	}

	public Point3d getPointForMouseEvent(MouseEvent mouseEvent) {
	    PickCanvas pickCanvas = new PickCanvas(canvas, scene); 
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO); 
		pickCanvas.setTolerance(4.0f); 
		pickCanvas.setShapeLocation(mouseEvent); 
		PickResult result = pickCanvas.pickClosest(); 
		if(result == null) 
			return null;
		PickIntersection intersection = result.getIntersection(0); 
		Point3d point = intersection.getPointCoordinates(); 
		return point; 
	}

	public void updateShape(byte[][][] voxData, int xRange, int seekValue){
		BranchGroup old = scene;
		scene = createSceneGraph(voxData, xRange, seekValue);
		scene.compile();
		simpleU.getLocale().replaceBranchGraph(old, scene);
	}
} 

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


public class MCPanel extends Panel {

	private SimpleUniverse simpleU;
	private Canvas3D canvas;
	private BranchGroup scene;

	public BranchGroup createSceneGraph(byte[][][] voxData,
			int xRange, int seekValue) {

		BranchGroup objRoot = new BranchGroup();
		
		// create initial transformation
		Transform3D scale = new Transform3D();
		scale.setScale(1.0/xRange);
		
		// create TransformGroup and set capabilities
        TransformGroup objTransform = new TransformGroup(scale);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTransform.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		objRoot.addChild(objTransform);
		
		objTransform.addChild(new MCShape(voxData, seekValue));
		//objTransform.addChild(new Sphere(30.0f, Sphere.GENERATE_NORMALS,
		//			MCShape.createAppearance()));
		
		// Picking
		BoundingSphere b = new BoundingSphere();
		PickRotateBehavior pickR = new PickRotateBehavior(objRoot,canvas,b);
		objRoot.addChild(pickR);
		PickTranslateBehavior pickT = new PickTranslateBehavior(objRoot,canvas,b);
		objRoot.addChild(pickT);
		PickZoomBehavior pickZ = new PickZoomBehavior(objRoot,canvas,b);
		objRoot.addChild(pickZ);
	
		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(new BoundingSphere());
		objRoot.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(new BoundingSphere());
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
		add("Center", canvas);

		scene = createSceneGraph(voxData, xRange, seekValue);

		// SimpleUniverse is a Convenience Utility class
		simpleU = new SimpleUniverse(canvas);

		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		TransformGroup vpTrans = simpleU.getViewingPlatform().
										getViewPlatformTransform();
//		KeyNavigatorBehavior keyNavBeh = new KeyNavigatorBehavior(vpTrans);
//		keyNavBeh.setSchedulingBounds(new BoundingSphere(new Point3d(),1000.0));
//		scene.addChild(keyNavBeh);
		
		scene.compile();

		simpleU.addBranchGraph(scene);
	} 

	public void updateShape(byte[][][] voxData, int xRange, int seekValue){
		BranchGroup old = scene;
		scene = createSceneGraph(voxData, xRange, seekValue);
		scene.compile();
		simpleU.getLocale().replaceBranchGraph(old, scene);
	}
} 

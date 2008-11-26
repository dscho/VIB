package octree;

import ij.IJ;
import ij.ImagePlus;

import ij3d.UniverseListener;
import ij3d.Content;

import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import voltex.VolRendConstants;
import voltex.Renderer;

public class VolumeOctree implements UniverseListener, VolRendConstants {

	public static final int SIZE = 128;

	static final int DETAIL_AXIS = 6;

	private static final int[][] axisIndex = new int[3][2];

	final Switch axisSwitch;

	private String imageDir;

	private Cube rootCube;
	private BranchGroup rootBranchGroup;

	private int curAxis = Z_AXIS;
	private int curDir = FRONT;

	private final int xdim, ydim, zdim;
	final float pw, ph, pd;
	private final Point3d refPt;
	private OctreeBehavior behavior;

	private boolean cancelUpdating = false;
	private boolean updateFinished = true;
	private CubeUpdater updater = new CubeUpdater();

	public VolumeOctree(String imageDir, Canvas3D canvas) throws RuntimeException {
		this.imageDir = imageDir;

		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK]  = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK]  = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK]  = 5;

		axisSwitch = new Switch();
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		for(int i = 0; i < 7; i++) {
			axisSwitch.addChild(getBranchGroup());
		}

		rootBranchGroup = new BranchGroup();
		rootBranchGroup.addChild(axisSwitch);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_LOCAL_TO_VWORLD_READ);

		behavior = new OctreeBehavior(canvas, this);
		behavior.setSchedulingBounds(new BoundingSphere());
		behavior.setEnable(true);
		rootBranchGroup.addChild(behavior);

		Properties props = new Properties();
		int maxLevel = 1;

		try {
			props.load(new FileReader(new File(imageDir, "props.txt")));

			xdim = Integer.parseInt(props.getProperty("width"));
			ydim = Integer.parseInt(props.getProperty("height"));
			zdim = Integer.parseInt(props.getProperty("depth"));
			maxLevel = Integer.parseInt(props.getProperty("level"));

			pw = Float.parseFloat(props.getProperty("pixelWidth"));
			ph = Float.parseFloat(props.getProperty("pixelHeight"));
			pd = Float.parseFloat(props.getProperty("pixelDepth"));

			rootCube = new Cube(this, imageDir, 0, 0, 0, maxLevel);
			rootCube.createChildren();

			refPt = new Point3d(xdim*pw / 2, ydim*ph / 2, zdim*pd / 2);
		} catch(Exception e) {
			throw new RuntimeException("Error in property file.", e);
		}
	}

	public void update() {
		behavior.postId(OctreeBehavior.TRIGGER_ID);
	}

	public void cancel() {
		if(!isUpdateFinished()) {
			setCancelUpdating(true);
			updater.cancel();
		}
	}

	public BranchGroup getRootBranchGroup() {
		return rootBranchGroup;
	}

	public Cube getRootCube() {
		return rootCube;
	}

	public float realWorldXDim() {
		return xdim * pw;
	}

	public float realWorldYDim() {
		return ydim * ph;
	}

	public float realWorldZDim() {
		return zdim * pd;
	}

	public void displayInitial() {
		int[] axis = new int[] {X_AXIS, Y_AXIS, Z_AXIS};

		for(int ai = 0; ai < 3; ai++) {
			CubeData cdata = new CubeData(rootCube);
			cdata.prepareForAxis(axis[ai]);
			try {
				cdata.createData();
			} catch(Exception e) {
				e.printStackTrace();
			}
			Arrays.sort(cdata.shapes);
			OrderedGroup fg = getOrderedGroup();
			OrderedGroup bg = getOrderedGroup();
			for(int i = 0; i < SIZE; i++) {
				fg.addChild(new ShapeGroup(cdata.shapes[i]).shape);
				bg.insertChild(cdata.shapes[i].shape, 0);
			}
			BranchGroup bgf = getBranchGroup(), bgb = getBranchGroup();
			bgf.addChild(fg);
			bgb.addChild(bg);

			axisSwitch.setChild(bgf, axisIndex[axis[ai]][FRONT]);
			axisSwitch.setChild(bgb, axisIndex[axis[ai]][BACK]);
		}
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
		System.out.println("# shapes: " + countInitialShapes());
	}

	/*
	 * This method should only be called from OctreeBehavior
	 */
	private Transform3D volumeToImagePlate = new Transform3D();
	private Transform3D tmp = new Transform3D();
	void display(Canvas3D canvas) {
		setUpdateFinished(false);

		// calculate the current view parameters
		canvas.getImagePlateToVworld(volumeToImagePlate);
		volumeToImagePlate.invert();
		rootBranchGroup.getLocalToVworld(tmp);
		volumeToImagePlate.mul(tmp);

		// collect the cubes which should be displayed
		List<Cube> cubes = new ArrayList<Cube>();
		rootCube.collectCubesToDisplay(cubes, canvas, volumeToImagePlate, curAxis, curDir);
		System.out.println("Found " + cubes.size() + " cubes to display");
		ShapeGroup[] shapes = new ShapeGroup[cubes.size() * SIZE];
		System.out.println("This gives " + shapes.length + " shapes");
		int i = 0;
		for(Cube c : cubes) {
			CubeData cdata = c.getCubeData();
			for(ShapeGroup sg : cdata.shapes)
				shapes[i++] = sg;
		}

		// remove the old data
		BranchGroup prev = (BranchGroup)axisSwitch.getChild(DETAIL_AXIS);
		prev.detach();
		if(prev.numChildren() > 0)
			((Group)prev.getChild(0)).removeAllChildren();

		// update cube data
		updater.updateCubes(cubes);

		// sort according to z-order
		Arrays.sort(shapes);

		BranchGroup bg = getBranchGroup();
		OrderedGroup og = getOrderedGroup();
		displayShapes(og, shapes, curDir);

		bg.addChild(og);
		axisSwitch.addChild(bg);

		// add the ShapeGroups of the collected cubes to the scenegraph
		CubeDataRecycler.instance().clearAll();
		setUpdateFinished(true);
		if(isCancelUpdating()) {
			System.out.println("Aborted");
			rootCube.cleanup();
			setCancelUpdating(false);
		} else {
			axisSwitch.setWhichChild(DETAIL_AXIS);
			System.out.println("# shapes: " + countDetailShapes());
		}
	}

	/*
	 * private methods
	 */
	private final void displayShapes(OrderedGroup og, ShapeGroup[] shapes, int dir) {
		if(dir == FRONT) {
			for(int i = 0; i < shapes.length; i++) {
				if(isCancelUpdating())
					break;
				og.addChild(shapes[i].shape);
			}
		} else {
			for(int i = shapes.length - 1; i >= 0; i--) {
				if(isCancelUpdating())
					break;
				og.addChild(shapes[i].shape);
			}
		}
		IJ.showProgress(1);
	}

	private final int countInitialShapes() {
		int sum = 0;
		for(int i = 0; i < 6; i++) {
			OrderedGroup og = (OrderedGroup)((Group)axisSwitch.getChild(i)).getChild(0);
			sum += og.numChildren();
		}
		return sum;
	}

	private final int countDetailShapes() {
		return ((Group)((Group)axisSwitch.getChild(DETAIL_AXIS)).getChild(0)).numChildren();
	}

	private final OrderedGroup getOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
		return og;
	}

	private final BranchGroup getBranchGroup() {
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		return bg;
	}

	/*
	 * synchronization methods
	 */
	private final synchronized boolean isUpdateFinished() {
		return updateFinished;
	}

	private final synchronized void setUpdateFinished(boolean b) {
		this.updateFinished = b;
	}

	private synchronized final boolean isCancelUpdating() {
		return cancelUpdating;
	}

	private final synchronized void setCancelUpdating(boolean b) {
		cancelUpdating = b;
	}

	/*
	 * UniverseListener interface
	 */
	private Vector3d eyeVec = new Vector3d();
	public void transformationUpdated(View view){
		Point3d eyePt = Renderer.getViewPosInLocal(view, rootBranchGroup);
		if (eyePt == null)
			return;
		eyeVec.sub(eyePt, refPt);

		// select the axis with the greatest magnitude
		int axis = X_AXIS;
		double value = eyeVec.x;
		double max = Math.abs(eyeVec.x);
		if (Math.abs(eyeVec.y) > max) {
			axis = Y_AXIS;
			value = eyeVec.y;
			max = Math.abs(eyeVec.y);
		}
		if (Math.abs(eyeVec.z) > max) {
			axis = Z_AXIS;
			value = eyeVec.z;
			max = Math.abs(eyeVec.z);
		}

		// select the direction based on the sign of the magnitude
		int dir = value > 0.0 ? FRONT : BACK;

		if ((axis != curAxis) || (dir != curDir)) {
			curAxis = axis;
			curDir = dir;
			axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
		}
	}

	public void transformationStarted(View view){
		System.out.println("transform started");
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
		cancel();
	}
	public void transformationFinished(View view){
		update();
	}
	public void contentAdded(Content c){}
	public void contentRemoved(Content c){}
	public void contentChanged(Content c){}
	public void contentSelected(Content c){}
	public void canvasResized(){}
	public void universeClosed(){}
}


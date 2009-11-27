package octree;

import ij3d.UniverseListener;
import ij3d.Content;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Properties;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.Switch;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ij3d.AxisConstants;

public class VolumeOctree implements UniverseListener, AxisConstants {

	public static final int SIZE = 64;

	static final int DETAIL_AXIS = 6;

	private static final int[][] axisIndex = new int[3][2];

	private final Switch axisSwitch;

	private String imageDir;

	private final Cube rootCube;
	private final BranchGroup rootBranchGroup;
	private final UpdaterThread updater;

	int curAxis = Z_AXIS;
	int curDir = BACK;

	private final int maxLevel;
	private final int xdim, ydim, zdim;
	final float pw, ph, pd;
	private final Point3d refPt;


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
			axisSwitch.addChild(newOrderedGroup());
		}

		rootBranchGroup = new BranchGroup();
		rootBranchGroup.addChild(axisSwitch);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_LOCAL_TO_VWORLD_READ);

		Properties props = new Properties();

		try {
			props.load(new FileInputStream(new File(imageDir, "props.txt")));

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

		updater = new UpdaterThread(canvas);
		updater.run();
	}

// 	public void update() {
// 	}
//
// 	public void cancel() {
// 	}

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
			CubeData cdata = rootCube.cdata;
			cdata.prepareForAxis(axis[ai]);
			cdata.show();

			Arrays.sort(cdata.shapes);
			OrderedGroup fg = getOrderedGroup(axisIndex[axis[ai]][FRONT]);
			OrderedGroup bg = getOrderedGroup(axisIndex[axis[ai]][BACK]);
			for(int i = 0; i < SIZE; i++) {
				BranchGroup br = newBranchGroup();
				br.addChild(cdata.shapes[i].duplicate().shape);
				fg.addChild(br);
				br = newBranchGroup();
				br.addChild(cdata.shapes[i].duplicate().shape);
				bg.insertChild(br, 0);
			}
		}
		setWhichChild(axisIndex[curAxis][curDir]);
		System.out.println("# shapes: " + countInitialShapes());
	}

	final void removeAllCubes() {
		// remove the old data
		OrderedGroup og = getOrderedGroup(DETAIL_AXIS);
		for(int i = og.numChildren()-1; i >= 0; i--) {
			BranchGroup child = (BranchGroup)og.getChild(i);
			child.detach();
			child.removeAllChildren();
		}
	}

	final void addEmptyGroups(int axis, int dir) {
		// TODO needed to make a new one each time?
		List<Cube> cubes = new ArrayList<Cube>();
		rootCube.collectCubes(cubes, axis);

		ShapeGroup[] shapes = new ShapeGroup[cubes.size() * SIZE];
		int i = 0;
		for(Cube c : cubes)
			for(ShapeGroup sg : c.cdata.shapes)
				shapes[i++] = sg;
		Arrays.sort(shapes);
		// TODO could we pre-sort all cubes
		OrderedGroup og = getOrderedGroup(DETAIL_AXIS);
		if(dir == FRONT) {
			for(i = shapes.length - 1; i >= 0; i--) {
				BranchGroup bg = newBranchGroup();
				bg.addChild(shapes[i].shape);
				// TODO could we now invert the loop?
				og.insertChild(bg, 0);
			}
		} else {
			for(i = 0; i < shapes.length ; i++) {
				BranchGroup bg = newBranchGroup();
				bg.addChild(shapes[i].shape);
				// TODO same here
				og.insertChild(bg, 0);
			}
		}
	}

	final void axisChanged() {
		rootCube.hideSelf();
		rootCube.hideSubtree();
		// TODO this should go better
		removeAllCubes();
		addEmptyGroups(curAxis, curDir);
	}

	private Transform3D toVWorld = new Transform3D();
	final void volumeToIP(Canvas3D canvas, Transform3D ret) {
		canvas.getImagePlateToVworld(ret);
		ret.invert();
		rootBranchGroup.getLocalToVworld(toVWorld);
		ret.mul(toVWorld);
	}

	private Transform3D volToIP = new Transform3D();
	final void updateCubes(Canvas3D canvas) {
		System.out.println("updateCubes");
		// TODO new thread?

		volumeToIP(canvas, volToIP);
		// update cubes
		updater.submit(volToIP);
	}

	final void setWhichChild(int child) {
		axisSwitch.setWhichChild(child);
	}

	final OrderedGroup getOrderedGroup(int i) {
		return (OrderedGroup)axisSwitch.getChild(i);
	}

	static final BranchGroup newBranchGroup() {
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		return bg;
	}

	final int countDetailShapes() {
		return getOrderedGroup(DETAIL_AXIS).numChildren();
	}

	/*
	 * private methods
	 */
	private final int countInitialShapes() {
		int sum = 0;
		for(int i = 0; i < 6; i++) {
			OrderedGroup og = getOrderedGroup(i);
			sum += og.numChildren();
		}
		return sum;
	}

	private static final OrderedGroup newOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		return og;
	}

	/*
	 * UniverseListener interface
	 */
	private Vector3d eyeVec = new Vector3d();
	public void transformationUpdated(View view){
		Point3d eyePt = getViewPosInLocal(view, rootBranchGroup);
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
			setWhichChild(axisIndex[curAxis][curDir]);
			axisChanged();
			setWhichChild(DETAIL_AXIS);
		}
		updateCubes(view.getCanvas3D(0));
	}

	public void transformationStarted(View view){
// 		cancel();
	}
	public void transformationFinished(View view){
// 		update();
	}
	public void contentAdded(Content c){}
	public void contentRemoved(Content c){}
	public void contentChanged(Content c){}
	public void contentSelected(Content c){}
	public void canvasResized(){}
	public void universeClosed(){}

	private static Transform3D parentInv = new Transform3D();
	private static Point3d viewPosition = new Point3d();
	private static Transform3D t = new Transform3D();
	private static Point3d getViewPosInLocal(View view, Node node) {
		if (node == null )
			return null;
		if (!node.isLive())
			return null;
		//  get viewplatforms's location in virutal world
		Canvas3D canvas = (Canvas3D)view.getCanvas3D(0);
		canvas.getCenterEyeInImagePlate(viewPosition);
		canvas.getImagePlateToVworld(t);
		t.transform(viewPosition);

		// get parent transform
		node.getLocalToVworld(parentInv);
		parentInv.invert();

		// transform the eye position into the parent's coordinate system
		parentInv.transform(viewPosition);

		return viewPosition;
	}

	private class UpdaterThread {

		private Canvas3D canvas;
		private Transform3D nextT = new Transform3D();
		private Transform3D runningT = new Transform3D();
		private boolean available = false;
		private Thread thread;

		public UpdaterThread(Canvas3D canvas) {
			this.canvas = canvas;
		}

		public synchronized void submit(Transform3D t) {
			System.out.println("submit");
			nextT.set(t);
			available = true;
			notify();
			System.out.println("submit done");
		}


		private synchronized void fetchNext() {
			if(!available) {
				try {
					wait();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			runningT.set(nextT);
			available = false;
		}

		public void run() {
			thread = new Thread() {
				public void run() {
					while(true) {
						fetchNext();
						rootCube.update(canvas, runningT);
						System.out.println("updateCubes finished");
					}
				}
			};
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}

		// TODO cancel thread
	}
}


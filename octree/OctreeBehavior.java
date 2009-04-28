package octree;

import ij.IJ;
import ij3d.*;

import java.util.ArrayList;
import java.util.Arrays;
import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.concurrent.Executors;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OctreeBehavior extends Behavior {

	public static final int UPDATE = 1;

	private final WakeupOnBehaviorPost updateCrit;

	private final Canvas3D canvas;
	private final VolumeOctree octree;
	private final ExecutorService updateService;

	private boolean cancelled = false;
	private boolean finished = true;

	public OctreeBehavior(Canvas3D canvas, VolumeOctree octree) {
		this.canvas = canvas;
		this.octree = octree;
		updateCrit = new WakeupOnBehaviorPost(null, UPDATE);
		updateService = Executors.newSingleThreadExecutor();
	}

	public void initialize() {
		wakeupOn(updateCrit);
	}

	public synchronized void processStimulus(Enumeration criteria) {
		System.out.println("processStim");
		while(criteria.hasMoreElements()) {
			Object c = criteria.nextElement();
			if(c instanceof WakeupOnBehaviorPost) {
				int id = ((WakeupOnBehaviorPost)c).getTriggeringPostId();
				if(id == UPDATE) {
					updateService.submit(new Runnable() {
						public void run() {
							display(canvas);
						}
					});
				}
			}
		}
		wakeupOn(updateCrit);
	}

	public final void cancel() {
		if(!finished)
			cancelled = true;
	}

	private Transform3D volumeToImagePlate = new Transform3D();
	private Transform3D tmp = new Transform3D();
	private final void display(Canvas3D canvas) {
		finished = false;

		// calculate the current view parameters
		canvas.getImagePlateToVworld(volumeToImagePlate);
		volumeToImagePlate.invert();
		octree.getRootBranchGroup().getLocalToVworld(tmp);
		volumeToImagePlate.mul(tmp);

		// collect the cubes which should be displayed
		if(cancelled) {
			octree.getRootCube().cleanup();
			return;
		}
		List<Cube> cubes = new ArrayList<Cube>();
		octree.getRootCube().collectCubesToDisplay(cubes, canvas,
				volumeToImagePlate, octree.curAxis, octree.curDir);
		if(cancelled) {
			octree.getRootCube().cleanup();
			return;
		}
		ShapeGroup[] shapes = new ShapeGroup[cubes.size() * VolumeOctree.SIZE];
		int i = 0;
		for(Cube c : cubes) {
			CubeData cdata = c.getCubeData();
			for(ShapeGroup sg : cdata.shapes)
				shapes[i++] = sg;
		}
		if(cancelled) {
			octree.getRootCube().cleanup();
			return;
		}
		octree.removeAllCubes();

		// sort according to z-order
		Arrays.sort(shapes);

		octree.setCombinedWhichChild();
		OrderedGroup og = octree.getOrderedGroup(VolumeOctree.DETAIL_AXIS);
		displayShapes(og, shapes, octree.curDir);

		// add the ShapeGroups of the collected cubes to the scenegraph
		CubeDataRecycler.instance().clearAll();
		if(cancelled) {
			System.out.println("Aborted");
			octree.getRootCube().cleanup();
			cancelled = false;
		} else {
			octree.setWhichChild(VolumeOctree.DETAIL_AXIS);
			System.out.println("# shapes: " + octree.countDetailShapes());
		}
		finished = true;
	}

	private final void displayShapes(OrderedGroup og, ShapeGroup[] shapes, int dir) {
		if(dir == VolumeOctree.FRONT) {
			for(int i = shapes.length - 1; i >= 0 && !cancelled; i--) {
				if(!shapes[i].cube.cubeDataUpToDate())
					shapes[i].cube.updateCubeData();
				BranchGroup bg = VolumeOctree.getBranchGroup();
				bg.addChild(shapes[i].shape);
				og.insertChild(bg, 0);
				try {
					Thread.sleep(3);
				} catch(InterruptedException e) {}
				IJ.showProgress(shapes.length-i, shapes.length);
			}
		} else {
			for(int i = 0; i < shapes.length && !cancelled; i++) {
				if(!shapes[i].cube.cubeDataUpToDate())
					shapes[i].cube.updateCubeData();
				BranchGroup bg = VolumeOctree.getBranchGroup();
				bg.addChild(shapes[i].shape);
				og.insertChild(bg, 0);
				try {
					Thread.sleep(3);
				} catch(InterruptedException e) {}
				IJ.showProgress(i, shapes.length);
			}
		}
		IJ.showProgress(1);
	}
}


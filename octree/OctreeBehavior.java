package octree;

import ij3d.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.Enumeration;

public class OctreeBehavior extends Behavior {

	public static final int TRIGGER_ID = 1;

	private WakeupOnBehaviorPost postCrit;
	private WakeupOnElapsedFrames frameCrit;

	private Canvas3D canvas;
	private VolumeOctree octree;


	public OctreeBehavior(Canvas3D canvas, VolumeOctree octree) {
		this.canvas = canvas;
		this.octree = octree;
		postCrit = new WakeupOnBehaviorPost(null, TRIGGER_ID);
		frameCrit = new WakeupOnElapsedFrames(0);
	}

	public void initialize() {
		wakeupOn(postCrit);
	}

	public void processStimulus(Enumeration criteria) {
		while(criteria.hasMoreElements()) {
			Object c = criteria.nextElement();
			if(c instanceof WakeupOnBehaviorPost) {
				octree.display(canvas);
				wakeupOn(frameCrit);
			} else if(c instanceof WakeupOnElapsedFrames) {
				new Thread() {
					@Override
					public void run() {
						System.gc();
					}
				}.start();
				System.out.println("finished");
				wakeupOn(postCrit);
			}
		}
	}
}


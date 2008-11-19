package voltex;

import ij3d.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.Enumeration;

public class OctreeBehavior extends Behavior {

	public static final int TRIGGER_ID = 1;

	private WakeupOnBehaviorPost postCrit;

	private Canvas3D canvas;
	private VolumeOctree octree;


	public OctreeBehavior(Canvas3D canvas, VolumeOctree octree) {
		this.canvas = canvas;
		this.octree = octree;
		postCrit = new WakeupOnBehaviorPost(null, TRIGGER_ID);
	}

	public void initialize() {
		wakeupOn(postCrit);
	}

	public void processStimulus(Enumeration criteria) {
		WakeupOnBehaviorPost wakeup;
		while(criteria.hasMoreElements()) {
			wakeup = (WakeupOnBehaviorPost)criteria.nextElement();
			octree.display(canvas);
		}
		wakeupOn(postCrit);
	}
}


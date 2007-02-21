package voltex;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

import javax.vecmath.Point3d;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.Transform3D;

import ij.gui.Toolbar;

import java.util.Enumeration;

public class MouseNavigation {

	private TransformGroup objectGroup;

	public MouseNavigation(TransformGroup objectGroup, BranchGroup objRoot, 
								MouseBehaviorCallback callback) {

		this.objectGroup = objectGroup;
		BoundingSphere bounds =
				new BoundingSphere(new Point3d(0.0,0.0,0.0), 100000.0);

		MouseRotate mr = new MouseRotate() {
			public void processStimulus(Enumeration criteria) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					super.processStimulus(criteria);
				} else 
					wakeupOn (mouseCriterion);
			}				
		};
		mr.setupCallback(callback);
		mr.setTransformGroup(objectGroup);
		mr.setSchedulingBounds(bounds);
		mr.setFactor(0.05);
		objRoot.addChild(mr);
		
		MouseTranslate mt = new MouseTranslate() {
			public void processStimulus(Enumeration criteria) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					super.processStimulus(criteria);
				} else 
					wakeupOn (mouseCriterion);
			}	
		};
		mt.setTransformGroup(objectGroup);
		mt.setSchedulingBounds(bounds);
		objRoot.addChild(mt);
		
		MouseZoom mz = new MouseZoom() {
			public void processStimulus(Enumeration criteria) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					super.processStimulus(criteria);
				} else 
					wakeupOn (mouseCriterion);
			}	
		};
		mz.setTransformGroup(objectGroup);
		mz.setSchedulingBounds(bounds);
		mz.setFactor(0.1);
		objRoot.addChild(mz);
	}

	public void resetView() {
		objectGroup.setTransform(new Transform3D());
	}
}

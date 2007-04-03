package vis3d;

import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.picking.behaviors.PickingCallback;
import com.sun.j3d.utils.picking.behaviors.PickRotateBehavior;
import com.sun.j3d.utils.picking.behaviors.PickTranslateBehavior;
import com.sun.j3d.utils.picking.PickCanvas;

import javax.vecmath.Point3d;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.Canvas3D;

import ij.gui.Toolbar;

import java.util.Enumeration;

public class MouseNavigation {

	public MouseNavigation(BranchGroup scene, Canvas3D canvas, 
				TransformGroup scaleTG, PickingCallback callback) {
		// Picking
		int mode = PickCanvas.GEOMETRY;
		BoundingSphere b = new BoundingSphere();
		b.setRadius(10.0);

		PickRotateBehavior rot = 
				new PickRotateBehavior(scene, canvas, b, mode) { 
			public void processStimulus(Enumeration criteria) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					super.processStimulus(criteria);
				} else 
					wakeupOn (wakeupCondition);
			}
		};
		rot.setupCallback(callback);
		scene.addChild(rot);
		
		scene.addChild(
			new PickTranslateBehavior(scene, canvas, b, mode)); 
		MouseZoom myMouseZoom = new MouseZoom();
		myMouseZoom.setTransformGroup(scaleTG);
		myMouseZoom.setSchedulingBounds(b);
		scene.addChild(myMouseZoom);
	}
/*

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
	}*/
}

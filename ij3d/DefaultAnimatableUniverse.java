package ij3d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij3d.behaviors.ViewPlatformTransformer;
import ij3d.shapes.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Alpha;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

public abstract class DefaultAnimatableUniverse extends DefaultUniverse {

	private Vector3d rotationAxis = new Vector3d();

	private Transform3D centerXform = new Transform3D();
	private Transform3D animationXform = new Transform3D();
	private Transform3D rotationXform = new Transform3D();

	private boolean rotateAroundViewAxis = true;

	private RotationInterpolator rotpol;
	private Alpha animation;
	private TransformGroup animationTG;
	private TransformGroup rotationTG;

	public DefaultAnimatableUniverse(int width, int height) {
		super(width, height);

		animationTG = getAnimationTG();
		rotationTG = getRotationTG();

		// Set up the default rotation axis and origin
		updateRotationAxisAndCenter();

		// Animation
		animation = new Alpha(-1, 4000);
		animation.pause();
		animation.setStartTime(System.currentTimeMillis());
		BranchGroup bg = new BranchGroup();
		rotpol = new RotationInterpolator(animation, animationTG) {
			@Override public void processStimulus(java.util.Enumeration e) {
				super.processStimulus(e);
				if(!animation.isPaused()) {
					TransformGroup tg = null;
					transformChanged(0, tg);
				} else {
					// this is the point where we actually know that
					// the animation has stopped
					animationPaused();
				}
			}
		};
		rotpol.setTransformAxis(centerXform);
		rotpol.setSchedulingBounds(bounds);
		// set disabled; it's enabled at startAnimation
		rotpol.setEnable(false);
		bg.addChild(rotpol);
		animationTG.addChild(bg);

		addUniverseListener(new UniverseListener() {
			public void transformationStarted(View view) {}
			public void transformationFinished(View view) {}
			public void contentAdded(Content c) {}
			public void contentRemoved(Content c) {}
			public void canvasResized() {}
			public void universeClosed() {}
			public void contentSelected(Content c) {}
			public void transformationUpdated(View view) {}
			public void contentChanged(Content c) {}
		});
	}

	private ImageStack stack;

	private Transform3D rotate = new Transform3D();
	private Transform3D centerXformInv = new Transform3D();
	public ImagePlus record360() {
		updateRotationAxisAndCenter();
		try {
			Thread.currentThread().sleep(1000);
		} catch (Exception e) {e.printStackTrace();}
		centerXformInv.invert(centerXform);
		double deg2 = 2 * Math.PI * 2 / 360;
		int steps = (int)Math.round(2 * Math.PI / deg2);

		getCanvas().getView().stopView();

		double alpha = 0;

		for(int i = 0; i < steps; i++) {
			alpha = i * deg2;
			rotationXform.rotY(alpha);
			rotate.mul(centerXform, rotationXform);
			rotate.mul(rotate, centerXformInv);
			animationTG.setTransform(rotate);
			transformChanged(-1, animationTG);
			getCanvas().getView().renderOnce();
//			try {
//				Thread.currentThread().sleep(100);
//			} catch (Exception e) {e.printStackTrace();}
			win.updateImagePlus();
			ImageProcessor ip = win.getImagePlus().getProcessor();
			int w = ip.getWidth(), h = ip.getHeight();
			if(stack == null) 
				stack = new ImageStack(w, h);
			stack.addSlice("", ip);
		}
		getCanvas().getView().startView();
		incorporateAnimationInRotation();

		if(stack.getSize() == 0)
			return null;
		ImagePlus imp = new ImagePlus("Movie", stack);
		return imp;
	}

	public void rotateY(double rad) {
		viewTransformer.rotateY(
			viewTransformer.getRotationCenter(), rad);
		transformChanged(-1, rotationTG);
	}

	public ImagePlus record() {
		ImagePlus ret = null;
		if(!animation.isPaused()) {
			pauseAnimation();
			ImageProcessor ip = win.getImagePlus().getProcessor();
			stack = new ImageStack(ip.getWidth(), ip.getHeight());
			ret = record360();
			startAnimation();
		}
		return ret;
	}

	public void startAnimation() {
		animationTG.getTransform(animationXform);
		updateRotationAxisAndCenter();
		rotpol.setTransformAxis(centerXform);
		rotpol.setEnable(true);
		animation.resume();
		fireTransformationStarted();
	}

	public void pauseAnimation() {
		animation.pause();
	}

	public void animationPaused() {
		rotpol.setEnable(false);
		incorporateAnimationInRotation();
		animation.setStartTime(System.currentTimeMillis());
		TransformGroup tg = null;
		transformChanged(0, tg);
		fireTransformationFinished();
	}

	private Vector3d tmpV = new Vector3d();
	private Vector3d centerV = new Vector3d();

	private void incorporateAnimationInRotation() {
		rotationTG.getTransform(rotationXform);
		animationTG.getTransform(animationXform);
		rotationXform.mul(animationXform, rotationXform);

		animationXform.setIdentity();
		animationTG.setTransform(animationXform);
		rotationTG.setTransform(rotationXform);
	}

	private Vector3d v1 = new Vector3d();
	private Vector3d v2 = new Vector3d();
	private AxisAngle4d aa = new AxisAngle4d();

//	private BoundingSphere sphere;

	private void updateRotationAxisAndCenter() {
//		sphere = new BoundingSphere(new Point3f(rotationCenter), 10);
//		scene.addChild(sphere);
		rotationXform.setIdentity();

		if(rotateAroundViewAxis) {
			viewTransformer.getYDir(rotationAxis);
			v1.set(0, 1, 0);
			v2.cross(v1, rotationAxis);
			double angle = Math.acos(v1.dot(rotationAxis));
			aa.set(v2, angle);
			rotationXform.set(aa);
		}

		centerV.set(viewTransformer.getRotationCenter());
		viewTransformer.getZoomTranslation(tmpV);
		centerV.sub(tmpV);
		viewTransformer.getCenterTranslation(tmpV);
		centerV.sub(tmpV);
		viewTransformer.getTranslateTranslation(tmpV);
		centerV.sub(tmpV);
		centerXform.set(centerV);

		centerXform.mul(rotationXform);
	}
} 

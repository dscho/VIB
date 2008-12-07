package ij3d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
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
	private Point3d rotationCenter = new Point3d();

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

			public void transformationUpdated(View view) {
				somethingChanged();
			}

			public void contentChanged(Content c) {
				somethingChanged();
			}
		});
	}

	private ImageStack stack;
	private boolean freeRecording = false;
	private boolean animatedRecording = false;

	public void somethingChanged() {
		if(animatedRecording) {
			return;
		} else if(freeRecording) {
			win.updateImagePlus();
			ImageProcessor ip = win.getImagePlus().getProcessor();
			int w = ip.getWidth(), h = ip.getHeight();
			if(stack == null) 
				stack = new ImageStack(w, h);
			stack.addSlice("", ip);
		}
	}

	private Transform3D rotate = new Transform3D();
	public void record360() {
//		rotationsTG.getTransform(rotate);
//		double angle = 0;
//		double deg5 = Math.toRadians(5);
//		double pi2 = Math.PI*2;
//		int circles = (int)Math.round(pi2/deg5);
//		getCanvas().getView().stopView();
//		rotate.rotY(angle);
//		rotationsTG.setTransform(rotate);
//		transformChanged(-1, rotationsTG);
//		getCanvas().getView().renderOnce();
//		try {
//			Thread.currentThread().sleep(1000);
//		} catch (Exception e) {e.printStackTrace();}
//		for(int i = 0; i < circles; i++) {
//			angle = i * deg5;
//			rotate.rotY(angle);
//			rotationsTG.setTransform(rotate);
//			transformChanged(-1, rotationsTG);
//			getCanvas().getView().renderOnce();
//			try {
//				Thread.currentThread().sleep(100);
//			} catch (Exception e) {e.printStackTrace();}
//			win.updateImagePlus();
//			ImageProcessor ip = win.getImagePlus().getProcessor();
//			int w = ip.getWidth(), h = ip.getHeight();
//			if(stack == null) 
//				stack = new ImageStack(w, h);
//			stack.addSlice("", ip);
//			angle += 0.04;
//		}
//		getCanvas().getView().startView();
//		stopRecording().show();
	}

	public void rotateY(double rad) {
//		rotationsTG.getTransform(rotate);
//		getCanvas().getView().stopView();
//		rotate.rotY(rad);
//		rotationsTG.setTransform(rotate);
//		transformChanged(-1, rotationsTG);
//		getCanvas().getView().startView();
	}

	public void startRecording() {
		ImageProcessor ip = win.getImagePlus().getProcessor();
		stack = new ImageStack(ip.getWidth(), ip.getHeight());
		if(!animation.isPaused()) {
			pauseAnimation();
			freeRecording = false;
			animatedRecording = true;
			record360();
			startAnimation();
		} else {
			animatedRecording = false;
			freeRecording = true;
		}
	}

	public ImagePlus stopRecording() {
		animatedRecording = false;
		freeRecording = false;
		if(stack == null || stack.getSize() == 0)
			return null;
		stack.deleteSlice(1);
		ImagePlus imp = new ImagePlus("Movie", stack);
		stack = null;
		return imp;
	}

	public void startAnimation() {
		animationTG.getTransform(animationXform);
		updateRotationAxisAndCenter();
		rotpol.setTransformAxis(centerXform);
		rotpol.setEnable(true);
		animation.resume();
		if(freeRecording) {
			freeRecording = false;
			animatedRecording = true;
		}
		fireTransformationStarted();
	}

	public void pauseAnimation() {
		animation.pause();
	}

	public void animationPaused() {
		rotpol.setEnable(false);
		incorporateAnimationInRotation();
		animation.setStartTime(System.currentTimeMillis());
		if(animatedRecording) {
			animatedRecording = false;
			freeRecording = true;
		}
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

		centerV.set(rotationCenter);
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

package ij3d.behaviors;

import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

public class ViewPlatformTransformer {

	protected DefaultUniverse univ;
	protected ImageCanvas3D canvas;

	private BehaviorCallback callback;

	private TransformGroup centerTG;
	private TransformGroup rotationTG;
	private TransformGroup zoomTG;
	private TransformGroup translateTG;

	private Transform3D centerXform = new Transform3D();
	private Transform3D rotationXform = new Transform3D();
	private Transform3D zoomXform = new Transform3D();
	private Transform3D translateXform = new Transform3D();

	private Point3d origin = new Point3d(0, 0, 0);
	private Point3d eyePos = new Point3d();

	private final Point3d oneInX = new Point3d(1, 0, 0);
	private final Point3d oneInY = new Point3d(0, 1, 0);
	private final Point3d oneInZ = new Point3d(0, 0, 1);

	private Vector3d zDir = new Vector3d();
	private Vector3d xDir = new Vector3d();
	private Vector3d yDir = new Vector3d();

	private Vector3d centerV = new Vector3d();

	private Transform3D ipToVWorld = new Transform3D();

	public ViewPlatformTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
		this.callback = callback;
		this.centerTG = univ.getCenterTG();
		this.rotationTG = univ.getRotationTG();
		this.zoomTG = univ.getZoomTG();
		this.translateTG = univ.getTranslateTG();
	}

	public void zoomTo(Vector3d v, double distance) {
		v.scale(-distance);
		zoomXform.set(v);
		zoomTG.setTransform(zoomXform);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	public void zoomTo(double distance) {
		getZDir(zDir);
		zoomTo(zDir, distance);
	}

	private Transform3D tmp = new Transform3D();
	public void zoom(int units) {
		double factor = distanceEyeOrigin() / 100;
		getZDir(zDir);
		// let the factor be 1 percent of the distance between
		// eye position and origin

		zDir.scale(factor * units);

		zoomTG.getTransform(zoomXform);
		tmp.set(zDir);
		zoomXform.mul(tmp, zoomXform);

		zoomTG.setTransform(zoomXform);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	public void centerAt(Point3f center) {
		centerV.set(center.x, center.y, center.z);
		centerXform.set(centerV);
		centerTG.setTransform(centerXform);
		centerXform.setIdentity();
		translateTG.setTransform(centerXform);
		transformChanged(BehaviorCallback.TRANSLATE, centerXform);
	}

	private Point2d originInCanvas = new Point2d();
	private Point3d originOnIp = new Point3d();
	private Point3d currentPtOnIp = new Point3d();
	public void translateXY(int dxPix, int dyPix) {
		origin.set(0, 0, 0);
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		float dD = (float)eyePos.distance(origin);

		originInCanvas(originInCanvas);
		canvas.getPixelLocationInImagePlate(originInCanvas, originOnIp);
		ipToVWorld.transform(originOnIp);
		float dd = (float)eyePos.distance(originOnIp);

		canvas.getPixelLocationInImagePlate(
			(int)Math.round(originInCanvas.x+1),
			(int)Math.round(originInCanvas.y), currentPtOnIp);
		ipToVWorld.transform(currentPtOnIp);
		float dx = (float)originOnIp.distance(currentPtOnIp);

		canvas.getPixelLocationInImagePlate(
			(int)Math.round(originInCanvas.x),
			(int)Math.round(originInCanvas.y+1), currentPtOnIp);
		ipToVWorld.transform(currentPtOnIp);
		float dy = (float)originOnIp.distance(currentPtOnIp);

		float dX = -dx * dxPix * dD / dd;
		float dY = dy * dyPix * dD / dd;

		translateXY(dX, dY);
	}

	public void translate(Vector3d v) {
		getTranslateTranslation(tmpV);
		tmpV.sub(v);
		translateXform.set(tmpV);
		translateTG.setTransform(translateXform);
		transformChanged(BehaviorCallback.TRANSLATE, translateXform);
	}

	public void translateXY(double dx, double dy) {
		getXDir(xDir);
		getYDir(yDir);
		xDir.scale(dx);
		yDir.scale(dy);
		xDir.add(yDir);
		translate(xDir);
	}

	private AxisAngle4d aa = new AxisAngle4d();
	private Vector3d tmpV = new Vector3d();
	public void rotateX(Point3d center, double angle){
		// compose the translation to center
		centerV.set(-center.x, -center.y, -center.z);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		getXDir(xDir);
		aa.set(xDir, angle);
		tmp.set(aa);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// then transform back to the center of rotation
		rotationXform.mul(centerXform, rotationXform);
		// rotate
		rotationXform.mul(tmp, rotationXform);
		centerV.set(-centerV.x, -centerV.y, -centerV.z);
		centerXform.set(centerV);
		// translate back
		rotationXform.mul(centerXform, rotationXform);

		rotationTG.setTransform(rotationXform);
	}

	public void rotateY(Point3d center, double angle){
		centerV.set(-center.x, -center.y, -center.z);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		getYDir(yDir);
		aa.set(yDir, angle);
		tmp.set(aa);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// then transform back to the center of rotation
		rotationXform.mul(centerXform, rotationXform);
		// rotate
		rotationXform.mul(tmp, rotationXform);
		centerV.set(-centerV.x, -centerV.y, -centerV.z);
		centerXform.set(centerV);
		// translate back
		rotationXform.mul(centerXform, rotationXform);

		rotationTG.setTransform(rotationXform);
	}

	public void rotateZ(Point3d center, double angle){
		centerV.set(-center.x, -center.y, -center.z);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		getZDir(zDir);
		aa.set(zDir, angle);
		tmp.set(aa);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// then transform back to the center of rotation
		rotationXform.mul(centerXform, rotationXform);
		// rotate
		rotationXform.mul(tmp, rotationXform);
		centerV.set(-centerV.x, -centerV.y, -centerV.z);
		centerXform.set(centerV);
		// translate back
		rotationXform.mul(centerXform, rotationXform);

		rotationTG.setTransform(rotationXform);
	}

	private AxisAngle4d aa2 = new AxisAngle4d();
	private Transform3D tmp2 = new Transform3D();
	public void rotateXY(Point3d center, double angleX, double angleY) {

		centerV.set(-center.x, -center.y, -center.z);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		getXDir(xDir);
		aa.set(xDir, angleX);
		tmp.set(aa);

		getYDir(yDir);
		aa2.set(yDir, angleY);
		tmp2.set(aa2);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// then transform back to the center of rotation
		rotationXform.mul(centerXform, rotationXform);
		// rotate x
		rotationXform.mul(tmp, rotationXform);
		// rotate y
		rotationXform.mul(tmp2, rotationXform);
		// update center back transform
		centerV.set(-centerV.x, -centerV.y, -centerV.z);
		centerXform.set(centerV);
		// translate back
		rotationXform.mul(centerXform, rotationXform);

		rotationTG.setTransform(rotationXform);
		transformChanged(BehaviorCallback.ROTATE, rotationXform);
	}

	public void viewPlatformToVworldInverse(Transform3D t) {
		centerV.set(0, 0, 0);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		rotationTG.getTransform(rotationXform);
		t.mul(centerXform, rotationXform);
	}

	public void viewPlatformToVworld(Transform3D t) {
		centerV.set(0, 0, 0);
		getZoomTranslation(tmpV);
		centerV.sub(tmpV);
		getCenterTranslation(tmpV);
		centerV.sub(tmpV);
		getTranslateTranslation(tmpV);
		centerV.sub(tmpV);
		centerXform.set(centerV);

		rotationTG.getTransform(rotationXform);
		rotationXform.invert();

		t.mul(rotationXform, centerXform);
	}

	public void getTranslateTranslation(Vector3d v) {
		translateTG.getTransform(tmp);
		tmp.get(v);
	}

	public void getCenterTranslation(Vector3d v) {
		centerTG.getTransform(tmp);
		tmp.get(v);
	}

	public void getZoomTranslation(Vector3d v) {
		zoomTG.getTransform(tmp);
		tmp.get(v);
	}

	public void originInCanvas(Point2d out) {
		origin.set(0, 0, 0);
		pointInCanvas(origin, out);
	}

	private Point3d tmpP = new Point3d();	
	private Transform3D ipToVWorldInverse = new Transform3D();
	public void pointInCanvas(Point3d in, Point2d out) {
		tmpP.set(in);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorldInverse.invert(ipToVWorld);
		ipToVWorldInverse.transform(in);
		canvas.getPixelLocationFromImagePlate(in, out);
	}

	public double distanceEyeTo(Point3d p) {
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		return eyePos.distance(p);
	}

	public double distanceEyeOrigin() {
		origin.set(0, 0, 0);
		return distanceEyeTo(origin);
	}

	public void getXDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getXDir(v, ipToVWorld);
	}

	public void getXDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInX.set(1, 0, 0);
		ipToVWorld.transform(oneInX);
		ipToVWorld.transform(origin);
		v.sub(oneInX, origin);
		v.normalize();
	}

	public void getYDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getYDir(v, ipToVWorld);
	}

	public void getYDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInY.set(0, 1, 0);
		ipToVWorld.transform(oneInY);
		ipToVWorld.transform(origin);
		v.sub(oneInY, origin);
		v.normalize();
	}

	public void getZDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getZDir(v, ipToVWorld);
	}

	public void getZDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInZ.set(0, 0, 1);
		ipToVWorld.transform(oneInZ);
		ipToVWorld.transform(origin);
		v.sub(origin, oneInZ);
		v.normalize();
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}
}

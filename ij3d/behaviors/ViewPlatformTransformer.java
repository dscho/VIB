package ij3d.behaviors;

import ij3d.UniverseSettings;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Tuple3d;

/**
 * This class is a helper class which implements some functions for
 * transforming the view part of the scene graph.
 * The view transformation consists of 5 single transformations:
 *
 * A center transformation, which is responsible for shifting the view
 * to a position so that the content of the universe is centered.
 *
 * A zoom transformation, which translates the view backward and forward
 *
 * A translate transformation, which is adjusted manually, either explicitly
 * or interactively, when the user translates the view with the mouse.
 *
 * An animation transformation, which is changed when the universe is animated.
 *
 * A rotation transformation, which is adjusted manually, either explicitly
 * or interactively, when the user rotates the view with the mouse.
 *
 * The functions in this class mainly aim to facilitate transformations
 * related to the image plate.
 *
 * @author bene
 */
public class ViewPlatformTransformer {

	protected DefaultUniverse univ;
	protected ImageCanvas3D canvas;

	protected Point3d rotCenter;

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

	/**
	 * Initialize this ViewPlatformTransformer.
	 * @param univ
	 * @param callback
	 */
	public ViewPlatformTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
		this.callback = callback;
		this.centerTG = univ.getCenterTG();
		this.rotationTG = univ.getRotationTG();
		this.zoomTG = univ.getZoomTG();
		this.translateTG = univ.getTranslateTG();
		// Set the initial rotation center to whatever is set in
		// UniverseSettings
		if(UniverseSettings.globalRotationCenter ==
				UniverseSettings.ROTATION_AROUND_CENTER)
			rotCenter = ((Image3DUniverse)univ).getGlobalCenterPoint();
		else
			rotCenter = new Point3d();
	}

	/**
	 * Copies the rotation center into the given Tuple3d.
	 */
	public void getRotationCenter(Tuple3d ret) {
		ret.set(rotCenter);
	}

	/**
	 * Sets the rotation center to the specified point.
	 * Attention: No copy is made.
	 */
	public void setRotationCenter(Point3d rotCenter) {
		this.rotCenter = rotCenter;
	}

	/**
	 * Moves the view back (related ot the given direction)
	 * to the specified distance.
	 * @param distance
	 */
	public void zoomTo(Vector3d v, double distance) {
		v.scale(-distance);
		zoomXform.set(v);
		zoomTG.setTransform(zoomXform);
		univ.getViewer().getView().setBackClipDistance(2 * distance);
		univ.getViewer().getView().setFrontClipDistance(2 * distance / 100);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	/**
	 * Moves the view back (i.e. in the z-direction of the image plate)
	 * to the specified distance.
	 * @param distance
	 */
	public void zoomTo(double distance) {
		getZDir(zDir);
		zoomTo(zDir, distance);
	}

	private Transform3D tmp = new Transform3D();
	/**
	 * Zoom by the specified amounts of units.
	 * @param units
	 */
	public void zoom(int units) {
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

		getZDir(zDir);
		float factor = dx * dD / dd;
		zDir.scale(2 * units * factor);

		zoomTG.getTransform(zoomXform);
		tmp.set(zDir);
		zoomXform.mul(tmp, zoomXform);

		zoomTG.setTransform(zoomXform);
		zoomXform.get(centerV);
		double distance = centerV.length();
		univ.getViewer().getView().setBackClipDistance(2 * distance);
		univ.getViewer().getView().setFrontClipDistance(2 * distance / 100);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	/**
	 * Center the view at the given point.
	 * @param center
	 */
	public void centerAt(Point3d center) {
		// set the center transformation to the translation given by
		// the specified point
		centerV.set(center.x, center.y, center.z);
		centerXform.set(centerV);
		centerTG.setTransform(centerXform);
		// set the global translation to identity
		centerXform.setIdentity();
		translateTG.setTransform(centerXform);
		transformChanged(BehaviorCallback.TRANSLATE, centerXform);
	}

	private Point2d originInCanvas = new Point2d();
	private Point3d originOnIp = new Point3d();
	private Point3d currentPtOnIp = new Point3d();
	/**
	 * Translates the view suitable to a mouse movement by dxPix and dyPix
	 * on the canvas.
	 * @param dxPix
	 * @param dyPix
	 */
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

	private Vector3d v3f = new Vector3d();
	/**
	 * Translates the view by the specified distances along the x, y
	 * and z direction (of the vworld).
	 * @param v The distances in x, y and z direction, given in vworld
	 * dimensions.
	 */
	public void translate(Vector3d v) {
		getTranslateTranslation(tmpV);
		tmpV.sub(v);
		translateXform.set(tmpV);
		translateTG.setTransform(translateXform);
		v3f.set(v);
//		((Image3DUniverse)univ).getGlobalCenterPoint().sub(v3f);
		transformChanged(BehaviorCallback.TRANSLATE, translateXform);
	}

	/**
	 * Translates the view by the specified distances along the x and y
	 * direction (of the image plate).
	 * @param dx The distance in x direction, given in vworld dimensions.
	 * @param dy The distance in y direction, given in vworld dimensions.
	 */
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
	/**
	 * Rotates the view around the specified center by the specified
	 * angle around the x axis (of the image plate).
	 * @param center The rotation center
	 * @param angle The angle (in rad) around the x-axis
	 */
	public void rotateX(Point3d center, double angle){
		rotate(center, new Vector3d(1, 0, 0), angle);
	}

	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the x axis (of the image plate).
	 * @param angle The angle (in rad) around the x-axis
	 */
	public void rotateX(double angle){
		rotateX(rotCenter, angle);
	}

	/**
	 * Rotates the view around the specified center by the specified
	 * angle around the y axis (of the image plate).
	 * @param center The rotation center
	 * @param angle The angle (in rad) around the y-axis
	 */
	public void rotateY(Point3d center, double angle){
		rotate(center, new Vector3d(0, 1, 0), angle);
	}

	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the y axis (of the image plate).
	 * @param angle The angle (in rad) around the y-axis
	 */
	public void rotateY(double angle){
		rotateY(rotCenter, angle);
	}

	/**
	 * Rotates the view around the specified center by the specified
	 * angle around the z axis (of the image plate).
	 * @param center The rotation center
	 * @param angle The angle (in rad) around the z-axis
	 */
	public void rotateZ(Point3d center, double angle){
		rotate(center, new Vector3d(0, 0, 1), angle);
	}

	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the z axis (of the image plate).
	 * @param angle The angle (in rad) around the z-axis
	 */
	public void rotateZ(double angle){
		rotateZ(rotCenter, angle);
	}

	/**
	 * Rotates the view around the specified center by the specified
	 * angle around the given axis (of the image plate).
	 * @param center The rotation center
	 * @param axis The axis of rotation (in image plate coordinate system)
	 * @param angle The angle (in rad) around the z-axis
	 */
	public void rotate(Point3d center, Vector3d axis, double angle) {
		// compose the translation to center
		centerV.set(-center.x, -center.y, -center.z);
		getZoomTranslation(tmpV);
		centerV.add(tmpV);
		getCenterTranslation(tmpV);
		centerV.add(tmpV);
		getTranslateTranslation(tmpV);
		centerV.add(tmpV);
		centerXform.set(centerV);

		Vector3d axisVW = new Vector3d();
		getAxisVworld(axis, axisVW);
		aa.set(axisVW, angle);
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

		transformChanged(BehaviorCallback.ROTATE, rotationXform);
	}

	private AxisAngle4d aa2 = new AxisAngle4d();
	private Transform3D tmp2 = new Transform3D();
	/**
	 * Rotates the view around the specified center by the specified
	 * angles around the x and y axis (of the image plate).
	 * @param center The rotation center
	 * @param angleX The angle (in rad) around the x-axis
	 * @param angleY The angle (in rad) around the y-axis
	 */
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

	private Point3d p1 = new Point3d();
	private Vector3d tmpV2 = new Vector3d();
	/**
	 * Rotates the view around the center of view by the specified
	 * angles around the x and y axis (of the image plate).
	 * @param angleX The angle (in rad) around the x-axis
	 * @param angleY The angle (in rad) around the y-axis
	 */
	public void rotateXY(double angleX, double angleY) {
		getTranslateTranslation(tmpV2);
		p1.set(rotCenter);
		p1.add(tmpV2);

		rotateXY(p1, angleX, angleY);
	}

	/**
	 * Store inverse of the overall transformation from the view to vworld
	 * in the specified Transform3D.
	 * @param t
	 */
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

	/**
	 * Store the overall transformation from the view to vworld
	 * in the specified Transform3D.
	 * @param t
	 */
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

	/**
	 * Retrieves the manual translation vector of the view.
	 * @param v
	 */
	public void getTranslateTranslation(Vector3d v) {
		translateTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Retrieves the translation vector which is responsible for
	 * centering the view.
	 * @param v
	 */
	public void getCenterTranslation(Vector3d v) {
		centerTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Retrieves the translation vector which is responsible for the current
	 * zooming and stores it in the given Vector3d.
	 * @param v
	 */
	public void getZoomTranslation(Vector3d v) {
		zoomTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Stores the canvas position of the origin of the vworld in
	 * the specified Point2d.
	 * @param out
	 */
	public void originInCanvas(Point2d out) {
		origin.set(0, 0, 0);
		pointInCanvas(origin, out);
	}

	private Point3d tmpP = new Point3d();
	private Transform3D ipToVWorldInverse = new Transform3D();
	/**
	 * Calculates where the specified point in the vworld space is
	 * placed on the canvas and stores the result in the specified Point2d.
	 * @param in
	 * @param out
	 */
	public void pointInCanvas(Point3d in, Point2d out) {
		tmpP.set(in);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorldInverse.invert(ipToVWorld);
		ipToVWorldInverse.transform(in);
		canvas.getPixelLocationFromImagePlate(in, out);
	}

	/**
	 * Calculates the distance between the viewer's eye and an
	 * arbitrary point in the vworld space.
	 * @return
	 */
	public double distanceEyeTo(Point3d p) {
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		return eyePos.distance(p);
	}

	/**
	 * Calculates the distance between the viewer's eye and the
	 * origin in the vworld space.
	 * @return
	 */
	public double distanceEyeOrigin() {
		origin.set(0, 0, 0);
		return distanceEyeTo(origin);
	}

	/**
	 * Calculates from the specified axis in image plate coordinate
	 * system the corresponding vector in the vworld coordinate system.
	 */
	public void getAxisVworld(Vector3d axis, Vector3d axisVW) {
		canvas.getImagePlateToVworld(ipToVWorld);
		origin.set(0, 0, 0);
		oneInX.set(axis);
		ipToVWorld.transform(oneInX);
		ipToVWorld.transform(origin);
		axisVW.sub(oneInX, origin);
		axisVW.normalize();
	}

	/**
	 * Transforms the x-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 */
	public void getXDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getXDir(v, ipToVWorld);
	}

	/**
	 * Transforms the x-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
	public void getXDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInX.set(1, 0, 0);
		ipToVWorld.transform(oneInX);
		ipToVWorld.transform(origin);
		v.sub(oneInX, origin);
		v.normalize();
	}

	/**
	 * Stores the y-direction in the image plate coordinate system,
	 * i.e. the direction towards the user, in the given Vector3d.
	 * @param v Vector3d in which the result in stored.
	 */
	public void getYDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getYDir(v, ipToVWorld);
	}

	/**
	 * Transforms the y-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
	public void getYDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInY.set(0, 1, 0);
		ipToVWorld.transform(oneInY);
		ipToVWorld.transform(origin);
		v.sub(oneInY, origin);
		v.normalize();
	}

	/**
	 * Transforms the z-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 */
	public void getZDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getZDir(v, ipToVWorld);
	}

	/**
	 * Transforms the z-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
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

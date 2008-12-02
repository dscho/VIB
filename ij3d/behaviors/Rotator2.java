package ij3d.behaviors;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import java.awt.event.MouseEvent;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Rotator2 {

	private Transform3D tmp1 = new Transform3D();
	private Transform3D globalTransInverse = new Transform3D();
	private Transform3D globalRotInverse = new Transform3D();
	private Transform3D globalTranslate = new Transform3D();
	private Transform3D globalRotate = new Transform3D();
	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D();
	private Transform3D transformY = new Transform3D();
	private Transform3D translate = new Transform3D();

	private Vector3f transl = new Vector3f();

	private Point3d tPoint1 = new Point3d();
	private Point3d tPoint2 = new Point3d();
	private Point3d tPoint3 = new Point3d();
	private Point3d tCenter = new Point3d();

	private DefaultUniverse univ;
	private MouseBehaviorCallback callback;

	private int x_last, y_last;

	public Rotator2(DefaultUniverse univ, MouseBehaviorCallback callback) {
		this.univ = univ;
		this.callback = callback;
	}
	public void rotate(Content c, MouseEvent e) {
		System.out.println("rotate");
		int x = e.getX(), y = e.getY();
		univ.getCanvas().getPixelLocationInImagePlate(x_last, y_last, tPoint1);
		univ.getCanvas().getPixelLocationInImagePlate(x, y_last, tPoint2);
		univ.getCanvas().getPixelLocationInImagePlate(x_last, y, tPoint3);

		univ.getCanvas().getImagePlateToVworld(tmp1);
		
		BranchGroup ref = c == null ? univ.getScene() : c;
		ref.getLocalToVworld(tmp1);
		if(c != null) {
			c.getContent().getCenter(tCenter);
		} else {
			tCenter.set(((Image3DUniverse)univ).getGlobalCenterPoint());
		}
		tmp1.transform(tCenter);
		tmp1.transform(tPoint1);
		tmp1.transform(tPoint2);
		tmp1.transform(tPoint3);


		double ddx = tPoint2.distance(tPoint1);
		double ddy = tPoint3.distance(tPoint1);
		double ddc = tCenter.distance(tPoint1);

		System.out.println("ddx = " + ddx);
		System.out.println("ddy = " + ddy);
		System.out.println("ddc = " + ddc);

		float angleY = (float)Math.atan2(ddx, ddc);
		float angleX = (float)Math.atan2(ddy, ddc);

		if(x - x_last < 0)
			angleY = -angleY;
		if(y - y_last < 0)
			angleX = -angleX;

		System.out.println("angleX = " + angleX);
		System.out.println("angleY = " + angleY);

		rotate(c, angleX, angleY);

		if(angleY != 0)
			x_last = x;
		if(angleX != 0)
			y_last = y;
	}

	public void rotate(Content c, float x_angle, float y_angle) {
//		float x_angle = 0.03f * dy;
//		float y_angle = 0.03f * dx;
		transformX.rotX(x_angle);
		transformY.rotY(y_angle);

		TransformGroup tg = (c == null || c.isLocked()) ?
				univ.getGlobalRotate() : c.getLocalRotate();
		Point3f center = null;
		if(c==null || c.isLocked())
			center = ((Image3DUniverse)univ).getGlobalCenterPoint();
		else
			c.getContent().getCenter(center);
		tg.getTransform(currentXform);

		univ.getGlobalRotate().getTransform(globalRotate);
		univ.getGlobalTranslate().getTransform(globalTranslate);

		globalRotInverse.invert(globalRotate);
		globalTransInverse.invert(globalTranslate);

		if(c != null && !c.isLocked()) {
			transl.x = -center.x;
			transl.y = -center.y;
			transl.z = -center.z;
			translate.set(transl);
			currentXform.mul(translate, currentXform);
			currentXform.mul(globalRotate, currentXform);
		}

		currentXform.mul(transformX, currentXform);
		currentXform.mul(transformY, currentXform);

		if(c != null && !c.isLocked()) {
			currentXform.mul(globalRotInverse, currentXform);
			transl.x = -transl.x;
			transl.y = -transl.y;
			transl.z = -transl.z;
			translate.set(transl);
			currentXform.mul(translate, currentXform);
		}

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.ROTATE, currentXform);
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}
}

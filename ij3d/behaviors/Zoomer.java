package ij3d.behaviors;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import ij3d.Content;
import ij3d.DefaultUniverse;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

public class Zoomer {

	private DefaultUniverse univ;
	private MouseBehaviorCallback callback;

	private Transform3D transformX = new Transform3D();
	private Transform3D currentXform = new Transform3D();

	private Vector3f transl = new Vector3f();

	private int x_last, y_last;

	public Zoomer(DefaultUniverse univ, MouseBehaviorCallback callback) {
		this.univ = univ;
		this.callback = callback;
	}

	public void wheel_zoom(Content c, MouseEvent e) {
		MouseWheelEvent we = (MouseWheelEvent)e;
		int units = 0;
		if(we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			units = we.getUnitsToScroll();
		wheel_zoom(c, units);
	}

	public void wheel_zoom(Content c, int units) {
//		double factor = 0.9;
//		if(units != 0) {
//
//			transformX.setIdentity();
//
//			double scale = units > 0 ? 1f/Math.abs(factor)
//						: Math.abs(factor);
//
//			transformX.setScale(scale);
//			TransformGroup tg = univ.getGlobalScale();
//			tg.getTransform(currentXform);
//			currentXform.mul(transformX, currentXform);
//
//			tg.setTransform(currentXform);
//			transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);
//		}
	}

	public void zoom(Content c, MouseEvent e) {
		int y = e.getY();
		int dy = y - y_last;
		zoom(c, dy);

		x_last = e.getX();
		y_last = y;
	}

	public void zoom(Content c, int dy) {
//		double factor = 0.9f;
//		double ddy = dy < 0 ? -1d : 1d;
//		ddy *= factor;
//		if(ddy != 0) {
//			transformX.setIdentity();
//			double scale = ddy > 0 ? 1f/Math.abs(ddy)
//						: Math.abs(ddy);
//
//			transformX.setScale(scale);
//			TransformGroup tg = univ.getGlobalScale();
//			tg.getTransform(currentXform);
//			currentXform.mul(transformX, currentXform);
//
//			tg.setTransform(currentXform);
//			transformChanged(
//				MouseBehaviorCallback.TRANSLATE, currentXform);
//		}
	}

	public void zoom_old(Content c, int dy) {
		System.out.println("zoom");
		transl.x = 0f;
		transl.y = 0f;
		transl.z = 0.5f * dy;
		transformX.set(transl);

		//TransformGroup tg = univ.getGlobalRotate();
		TransformGroup tg = univ.getViewingPlatform().getViewPlatformTransform();
		tg.getTransform(currentXform);
		currentXform.mul(transformX, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);
	}

	public void zoom_old(Content c, MouseEvent e) {
		int y = e.getY();
		int dy = y - y_last;

		transl.x = 0f;
		transl.y = 0f;
		transl.z = 0.5f * dy;
		transformX.set(transl);

		//TransformGroup tg = univ.getGlobalRotate();
		TransformGroup tg = univ.getViewingPlatform().getViewPlatformTransform();
		tg.getTransform(currentXform);
		currentXform.mul(transformX, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);

		x_last = e.getX();
		y_last = y;
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}


}

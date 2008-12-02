package ij3d.behaviors;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import java.awt.event.MouseEvent;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Translator {

	private Initializer initializer;

	private DefaultUniverse univ;
	private MouseBehaviorCallback callback;

	private Point3d v1 = new Point3d();
	private Point3d v2 = new Point3d();

	private Vector3d translationPerDx = new Vector3d();
	private Vector3d translationPerDy = new Vector3d();

	private Vector3d translation = new Vector3d();
	private Transform3D translateNew = new Transform3D();
	private Transform3D translateOld = new Transform3D();

	private TransformGroup toTransform;
		
	private int xStart, yStart;

	public Translator(DefaultUniverse univ, MouseBehaviorCallback callback) {
		this.univ = univ;
		this.callback = callback;
		this.initializer = new Initializer();
	}

	public void init(Content c, int x, int y) {
		initializer.init(c, x, y);
	}

	public void translate(MouseEvent e) {
		translate(e.getX(), e.getY());
	}

	public void translate(int xNew, int yNew) {
		int dx = xNew - xStart;
		int dy = yNew - yStart;
		v1.scale(dx, translationPerDx);
		v2.scale(dy, translationPerDy);
		translation.add(v1, v2);
		translateNew.set(translation);
		translateNew.mul(translateOld);

		toTransform.setTransform(translateNew);
		transformChanged(MouseBehaviorCallback.TRANSLATE, translateNew);	
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}

	private class Initializer {
		private Point3d centerOld = new Point3d();
		private Point3d centerNew = new Point3d();
		
		private Transform3D ipToVWorld = new Transform3D();
		private Transform3D localToVWorld = new Transform3D();
		private Transform3D localToVWorldInverse = new Transform3D();
		private Point3d currentIpPtInVWorld = new Point3d();
		
		private Point3d eyePtInVWorld = new Point3d();
		private Point3d pickPtInVWorld = new Point3d();
		private Point3d ipPtInVWorld = new Point3d();

		private void init(Content c, int x, int y) {
			xStart = x;
			yStart = y;
			// get local to vworld direct and inverse transform
			if(c == null) {
//				localToVWorld.setIdentity();
//				localToVWorldInverse.setIdentity();
//				centerOld.set(((Image3DUniverse)univ).getGlobalCenterPoint());
//				pickPtInVWorld.set(centerOld);
//				toTransform = univ.getGlobalTranslate();
//				toTransform.getTransform(translateOld);
			} else {
				c.getLocalToVworld(localToVWorld);
				localToVWorldInverse.invert(localToVWorld);
				Point3d pickedPoint = univ.getPicker().getPickPoint(c, x, y);
				if(pickedPoint == null)
					return;
				pickPtInVWorld.set(pickedPoint);
				localToVWorld.transform(pickPtInVWorld);
				c.getContent().getCenter(centerOld);
				toTransform = c.getLocalTranslate();
				toTransform.getTransform(translateOld);
			}
			// get ip to vworld transform
			univ.getCanvas().getImagePlateToVworld(ipToVWorld);
			
			// get the pixel world coordinates
			univ.getCanvas().getPixelLocationInImagePlate(x, y, ipPtInVWorld);
			ipToVWorld.transform(ipPtInVWorld);
			// get the eye point in world coordinates
			univ.getCanvas().getCenterEyeInImagePlate(eyePtInVWorld);
			ipToVWorld.transform(eyePtInVWorld);

			// distance between eye and pixel
			double dEyePtToIpPt = eyePtInVWorld.distance(ipPtInVWorld);
			// distance between eye and picked point
			double dEyePtToPickPt = eyePtInVWorld.distance(pickPtInVWorld);

			// no increase x by 1 and see the effect on the global coords
			univ.getCanvas().getPixelLocationInImagePlate(x + 1, y, currentIpPtInVWorld);
			ipToVWorld.transform(currentIpPtInVWorld);
			double dx = currentIpPtInVWorld.x - ipPtInVWorld.x;
			double dy = currentIpPtInVWorld.y - ipPtInVWorld.y;
			double dX = dEyePtToPickPt / dEyePtToIpPt * dx;
			double dY = dEyePtToPickPt / dEyePtToIpPt * dy;
			
			localToVWorld.transform(centerOld, centerNew);
			centerNew.x += dX;
			centerNew.y += dY;
			localToVWorldInverse.transform(centerNew);
			translationPerDx.sub(centerNew, centerOld);
			
			// no increase y by 1 and see the effect on the global coords
			univ.getCanvas().getPixelLocationInImagePlate(x, y + 1, currentIpPtInVWorld);
			ipToVWorld.transform(currentIpPtInVWorld);
			dx = currentIpPtInVWorld.x - ipPtInVWorld.x;
			dy = currentIpPtInVWorld.y - ipPtInVWorld.y;
			dX = dEyePtToPickPt / dEyePtToIpPt * dx;
			dY = dEyePtToPickPt / dEyePtToIpPt * dy;
			
			localToVWorld.transform(centerOld, centerNew);
			centerNew.x += dX;
			centerNew.y += dY;
			localToVWorldInverse.transform(centerNew);
			translationPerDy.sub(centerNew, centerOld);
		}
	}
}

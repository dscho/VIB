package ij3d.behaviors;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import java.awt.event.MouseEvent;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Translator_1 {

	private DefaultUniverse univ;
	private MouseBehaviorCallback callback;

	private Point3d centerOld = new Point3d();
	private Point3d centerNew = new Point3d();
	private Vector3d translation = new Vector3d();
	private Transform3D translateNew = new Transform3D();
	private Transform3D translateOld = new Transform3D();

	private Transform3D ipToVWorld = new Transform3D();
	private Transform3D localToVWorld = new Transform3D();
	private Transform3D localToVWorldInverse = new Transform3D();

	private Point3d eyePtInVWorld = new Point3d();
	private Point3d pickPtInVWorld = new Point3d();
	private Point3d ipPtInVWorld = new Point3d();
	private double dEyePtToIpPt;
	private double dEyePtToPickPt;

	public Translator_1(DefaultUniverse univ, MouseBehaviorCallback callback) {
		this.univ = univ;
		this.callback = callback;
	}

	public void init(Content c, int x, int y) {
		if(c != null)
			initLocal(c, x, y);
		else
			initGlobal(x, y);
	}

	public void initLocal(Content c, int x, int y) {
		// get local to vworld direct and inverse transform
		c.getLocalToVworld(localToVWorld);
		localToVWorldInverse.invert(localToVWorld);
		// get ip to vworld transform
		univ.getCanvas().getImagePlateToVworld(ipToVWorld);

		// get the pixel world coordinates
		univ.getCanvas().getPixelLocationInImagePlate(x, y, ipPtInVWorld);
		ipToVWorld.transform(ipPtInVWorld);
		// get the eye point in world coordinates
		univ.getCanvas().getCenterEyeInImagePlate(eyePtInVWorld);
		ipToVWorld.transform(eyePtInVWorld);
		// get the picked point in world coordinates
		Point3d pickedPoint = univ.getPicker().getPickPoint(c, x, y);
		if(pickedPoint == null)
			return;
		pickPtInVWorld.set(pickedPoint);
		localToVWorld.transform(pickPtInVWorld);

		// distance between eye and pixel
		dEyePtToIpPt = eyePtInVWorld.distance(ipPtInVWorld);
		// distance between eye and picked point
		dEyePtToPickPt = eyePtInVWorld.distance(pickPtInVWorld);

		c.getContent().getCenter(centerOld);
		c.getLocalTranslate().getTransform(translateOld);
	}

	public void initGlobal(int x, int y) {
		univ.getCanvas().getImagePlateToVworld(ipToVWorld);

		univ.getCanvas().getPixelLocationInImagePlate(x, y, ipPtInVWorld);
		ipToVWorld.transform(ipPtInVWorld);
		univ.getCanvas().getCenterEyeInImagePlate(eyePtInVWorld);
		ipToVWorld.transform(eyePtInVWorld);
		pickPtInVWorld.set(((Image3DUniverse)univ).getGlobalCenterPoint());

		dEyePtToIpPt = eyePtInVWorld.distance(ipPtInVWorld);
		dEyePtToPickPt = eyePtInVWorld.distance(pickPtInVWorld);

		centerOld.set(((Image3DUniverse)univ).getGlobalCenterPoint());
		univ.getGlobalTranslate().getTransform(translateOld);
	}

	public void translate(Content c, MouseEvent e) {
		if(c != null)
			translateContent(c, e.getX(), e.getY());
		else
			translateGlobal(e.getX(), e.getY());
	}

	private Point3d currentIpPtInVWorld = new Point3d();
	public void translateContent(Content c, int xNew, int yNew) {
		univ.getCanvas().getPixelLocationInImagePlate(xNew, yNew, currentIpPtInVWorld);
		ipToVWorld.transform(currentIpPtInVWorld);
		double dx = currentIpPtInVWorld.x - ipPtInVWorld.x;
		double dy = currentIpPtInVWorld.y - ipPtInVWorld.y;
		double dX = dEyePtToPickPt / dEyePtToIpPt * dx;
		double dY = dEyePtToPickPt / dEyePtToIpPt * dy;
		translateContent(c, dX, dY);
	}

	public void translateGlobal(int xNew, int yNew) {
		univ.getCanvas().getPixelLocationInImagePlate(xNew, yNew, currentIpPtInVWorld);
		ipToVWorld.transform(currentIpPtInVWorld);
		double dx = currentIpPtInVWorld.x - ipPtInVWorld.x;
		double dy = currentIpPtInVWorld.y - ipPtInVWorld.y;
		double dX = dEyePtToPickPt / dEyePtToIpPt * dx;
		double dY = dEyePtToPickPt / dEyePtToIpPt * dy;
		translateGlobal(dX, dY);
	}

	public void translateGlobal(double dX, double dY) {
		centerNew.set(centerOld);
		centerNew.x += dX;
		centerNew.y += dY;
		translation.sub(centerNew, centerOld);
		translateNew.set(translation);
		translateNew.mul(translateOld);
		
		univ.getGlobalTranslate().setTransform(translateNew);
		transformChanged(MouseBehaviorCallback.TRANSLATE, translateNew);

	}

	public void translateContent(Content c, double dX, double dY) {
		localToVWorld.transform(centerOld, centerNew);
		centerNew.x += dX;
		centerNew.y += dY;
		localToVWorldInverse.transform(centerNew);
		translation.sub(centerNew, centerOld);
		translateNew.set(translation);
		translateNew.mul(translateOld);
		
		c.getLocalTranslate().setTransform(translateNew);
		transformChanged(MouseBehaviorCallback.TRANSLATE, translateNew);
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}

}

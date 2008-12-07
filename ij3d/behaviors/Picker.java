package ij3d.behaviors;

import com.sun.j3d.utils.pickfast.PickCanvas;
import com.sun.j3d.utils.pickfast.PickTool;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import java.awt.event.MouseEvent;
import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.SceneGraphPath;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import vib.BenesNamedPoint;

public class Picker {
	private DefaultUniverse univ;
	private ImageCanvas3D canvas;

	public Picker(DefaultUniverse univ) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
	}

	public void deletePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;
		int ind = c.getPointListPointIndexAt(p3d);
		if(ind != -1) {
			c.deletePointListPoint(ind);
		}
	}

	int movingIndex = -1;
	public void movePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;
		if(movingIndex == -1)
			movingIndex = c.getPointListPointIndexAt(p3d);
		if(movingIndex != -1) {
			c.setListPointPos(movingIndex, p3d);
		}
	}

	public void stopMoving() {
		movingIndex = -1;
	}

	public void pickPoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPoint2(c, e);
		if(p3d == null)
			return;
		BenesNamedPoint bnp = c.getPointListPointAt(p3d);
		if(bnp == null) {
			c.addPointListPoint(p3d);
		}
	}
	
	Point3d start = new Point3d();
	Transform3D tmp = new Transform3D();
	double z = 0.004245426;
	private Point3d getPickPoint2(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		PickTool pickTool = new PickTool(c);
//		ic3d.getPixelLocationInImagePlate(x, y, start);
//		ic3d.getImagePlateToVworld(tmp);
//		tmp.transform(start);

		c.getLocalToVworld(tmp);
		Point3f cInW = new Point3f();
		c.getContent().getCenter(cInW);
//		tmp.transform(cInW);
		System.out.println("cInW" + cInW);
//		start.z += 500;
		c.addPointListPoint(new Point3d(cInW));
		pickTool.setShapeCylinderSegment(new Point3d(cInW), new Point3d(0, 0 ,-1), 3);

		pickTool.setMode(PickInfo.PICK_GEOMETRY);
		pickTool.setFlags(PickInfo.NODE | PickInfo.ALL_GEOM_INFO);
		try {
			PickInfo[] result = pickTool.pickAllSorted();
			if(result == null || result.length == 0) {
				System.out.println("not picking anything");
				return null;
			}

			for(int i = 0; i < result.length; i++) {
				Node picked = result[i].getNode();
				String name = picked.getName();
				if(name == null || !name.equals("BB"))
					continue;
				PickInfo.IntersectionInfo[] iinfos = result[i].getIntersectionInfos();
				for(int j = 0; j < iinfos.length; j++) {
					Point3d intersection = iinfos[j].getIntersectionPoint();
					System.out.println("iinfo[" + j + "] inters = " + intersection);
					c.addPointListPoint(intersection);
				}
			}
			return null;
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public Point3d getPickPointGeometry(Content c, Point3d origin, Vector3d dir) {
		PickTool pickTool = new PickTool(c);
		pickTool.setShapeRay(origin, dir);

		pickTool.setMode(PickInfo.PICK_GEOMETRY);
		pickTool.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		try {
			PickInfo[] result = pickTool.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			for(int i = 0; i < result.length; i++) {
				Point3d intersection = result[i].getClosestIntersectionPoint();
				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}

	public Point3d getPickPointGeometry(Content c, MouseEvent e) {
		return getPickPointGeometry(c, e.getX(), e.getY());
	}

	public Point3d getPickPointGeometry(Content c, int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			for(int i = 0; i < result.length; i++) {
				Point3d intersection = result[i].getClosestIntersectionPoint();
				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}

	public Point3d getPickPointBoundingSphere(Content c, int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		c.showBoundingSphere(true);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null || result.length == 0)
				return null;
			for(int j = 0; j < result.length; j++) {
				SceneGraphPath path = result[j].getSceneGraphPath();
				for(int i = path.nodeCount() - 1; i >= 0; i--) {
					if(path.getNode(i) == c.getBoundingSphere()) {
						Point3d inters = result[j].getClosestIntersectionPoint();
						Transform3D inv = new Transform3D();
						c.getBoundingSphere().getTransform(inv);
						inv.transform(inters);
						return inters;
					}
				}
			}
			return null;
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static float getVolumePoint(Content c, Point3d p) {

		ImagePlus img = c.getImage();
		Calibration cal = img.getCalibration();
		double pw = cal.pixelWidth;
		double ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		int ix = (int)Math.round(p.x / pw);
		int iy = (int)Math.round(p.y / ph);
		int iz = (int)Math.round(p.z / pd);
		if(iz < 0 || iz >= img.getStackSize() ||
				iy < 0 || iy >= img.getHeight() ||
				ix < 0 || ix >= img.getWidth())
			return 0;
		else
			return img.getStack().getProcessor(iz + 1).getf(ix, iy);

	}

}

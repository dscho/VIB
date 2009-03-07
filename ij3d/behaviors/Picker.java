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
import javax.media.j3d.PickInfo;
import javax.media.j3d.SceneGraphPath;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import vib.BenesNamedPoint;

/**
 * This class is a helper class which implements functions for picking.
 * 
 * @author Benjamin Schmid
 */
public class Picker {
	private DefaultUniverse univ;
	private ImageCanvas3D canvas;

	/**
	 * Constructs a new Picker
	 * @param univ
	 */
	public Picker(DefaultUniverse univ) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
	}

	/**
	 * Deletes a landmark point of the specified Content at the given mouse
	 * position
	 * @param c
	 * @param e
	 */
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

	private int movingIndex = -1;
	
	/**
	 * Moves the picked landmark point to the position specified by the
	 * MouseEvent.
	 * @param c
	 * @param e
	 */
	public synchronized void movePoint(Content c, MouseEvent e) {
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

	/**
	 * Stop moving.
	 */
	public synchronized void stopMoving() {
		movingIndex = -1;
	}

	/**
	 * Adds a landmark point specfied by the position of the MouseEvent.
	 * @param c
	 * @param e
	 */
	public void addPoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;
		BenesNamedPoint bnp = c.getPointListPointAt(p3d);
		if(bnp == null) {
			c.addPointListPoint(p3d);
		}
	}
	
	/*
	private Point3d start = new Point3d();
	private Transform3D tmp = new Transform3D();
	private double z = 0.004245426;
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
	}*/

	/**
	 * Get the picked point using geometry picking. The pick line is specified
	 * by the given Point3d and Vector3d.
	 * @param c
	 * @param origin
	 * @param dir
	 * @return
	 */
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
				if(c.getType() != Content.VOLUME)
					return intersection;

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}

	/**
	 * Get the picked point, using geometry picking, for the specified
	 * canvas position.
	 * @param c
	 * @param e
	 * @return
	 */
	public Point3d getPickPointGeometry(Content c, MouseEvent e) {
		return getPickPointGeometry(c, e.getX(), e.getY());
	}

	/**
	 * Get the picked point, using geometry picking, for the specified
	 * canvas position.
	 * @param c
	 * @param e
	 * @return
	 */
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
				if(c.getType() != Content.VOLUME)
					return intersection;

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the Content at the specified canvas position
	 * @param x
	 * @param y
	 * @return
	 */
	public Content getPickedContent(int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null)
				return null;
			for(int i = 0; i < result.length; i++) {
				SceneGraphPath path = result[i].getSceneGraphPath();
				Content c = null;
				for(int j = path.nodeCount()-1; j >= 0; j--)
					if(path.getNode(j) instanceof Content)
						c = (Content)path.getNode(j);

				if(c == null)
					continue;

				if(c.getType() != Content.VOLUME)
					return c;

				Point3d intersection = result[i].getClosestIntersectionPoint();

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return c;
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

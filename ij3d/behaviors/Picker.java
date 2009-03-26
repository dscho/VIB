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
import vib.PointList;

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
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		int ind = pl.indexOfPointAt(p3d.x, p3d.y, p3d.z, tol);
		if(ind != -1) {
			pl.remove(ind);
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

		PointList pl = c.getPointList();
		if(movingIndex == -1)
			movingIndex = pl.indexOfPointAt(
					p3d.x, p3d.y, p3d.z, c.getLandmarkPointSize());
		if(movingIndex != -1) {
			pl.placePoint(pl.get(movingIndex), p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Stop moving.
	 */
	public synchronized void stopMoving() {
		movingIndex = -1;
	}

	/**
	 * Adds a landmark point specfied by the canvas position
	 * @param c
	 * @param x position in the canvas
	 * @param y position in the canvas
	 */
	public void addPoint(Content c, int x, int y) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, x, y);
		if(p3d == null)
			return;
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if(bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
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
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if(bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
	}

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

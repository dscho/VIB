package octree;

import java.io.File;

import java.util.List;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import ij3d.AxisConstants;

public class Cube implements AxisConstants {

	public static final int RESOLUTION_SUFFICIENT   = 0;
	public static final int RESOLUTION_UNSUFFICIENT = 1;
	public static final int OUTSIDE_CANVAS          = 2;

	public static final double RES_THRESHOLD = VolumeOctree.SIZE * 4;//Math.sqrt(3);

	final int x, y, z, level;
	final float pw, ph, pd;
	final String path;
	final String name;
	final String dir;

	private Cube[] children;
	final Point3d[] corners;
	private Point2d[] cornersInCanvas;
	private boolean visible = false;
	private boolean subtreeVisible = false;

	final CubeData cdata;

	private VolumeOctree octree;

	public Cube(VolumeOctree oct, String dir, int x, int y, int z, int l) {
		this.dir = dir + "/";
		this.octree = oct;
		this.x = x;
		this.y = y;
		this.z = z;
		this.level = l;
		this.name = x + "_" + y + "_" + z + "_" + l;
		this.path = this.dir + name + ".tif";
		if(exists()) {
			float[] cal = CubeData.readCalibration(path, null);
			pw = cal[0];
			ph = cal[1];
			pd = cal[2];
			double lx = VolumeOctree.SIZE * pw;
			double ly = VolumeOctree.SIZE * ph;
			double lz = VolumeOctree.SIZE * pd;
			cornersInCanvas = new Point2d[8];
			for(int i = 0; i < 8; i++)
				cornersInCanvas[i] = new Point2d();
			double minx = x * octree.pw, maxx = minx + lx;
			double miny = y * octree.ph, maxy = miny + ly;
			double minz = z * octree.pd, maxz = minz + lz;
			corners = new Point3d[8];
			corners[0] = new Point3d(minx, miny, minz);
			corners[7] = new Point3d(maxx, maxy, maxz);
			corners[1] = new Point3d(maxx, miny, minz);
			corners[2] = new Point3d(minx, maxy, minz);
			corners[3] = new Point3d(maxx, maxy, minz);
			corners[4] = new Point3d(minx, miny, maxz);
			corners[5] = new Point3d(maxx, miny, maxz);
			corners[6] = new Point3d(minx, maxy, maxz);
			this.cdata = new CubeData(this);
		} else {
			// such an object is hopefully never used.
			pw = ph = pd = -1;
			corners = null;
			this.cdata = null;
		}
	}

	public Cube createCube(VolumeOctree oct,
			String dir, int x, int y, int z, int l) {
		String name = x + "_" + y + "_" + z + "_" + l;
		if(new File(dir, name + ".tif").exists())
			return new Cube(oct, dir, x, y, z, l);
		return null;
	}

	public boolean exists() {
		return new File(path).exists();
	}

	public Cube[] getChildren() {
		return children;
	}

	/**
	 * axis One of X_AXIS, Y_AXIS or Z_AXIS
	 */
	public void collectCubes(List<Cube> cubes, int axis) {
		cdata.prepareForAxis(axis);
		cubes.add(this);
		if(children == null)
			return;
		for(Cube c : children)
			if(c != null)
				c.collectCubes(cubes, axis);
	}

	public void hideSelf() {
		if (this.visible) {
			this.cdata.hide();
			this.visible = false;
		}
	}

	public void hideSubtree() {
		if (this.subtreeVisible) {
			this.subtreeVisible = false;
			if (this.children == null)
				return;
			for (Cube localCube : this.children)
				if (localCube != null) {
					localCube.hideSelf();
					localCube.hideSubtree();
				}
		}
	}

	private void showSelf()
	{
		if (!(this.visible)) {
			this.cdata.show();
			this.visible = true;
		}
	}

	public void update(Canvas3D paramCanvas3D, Transform3D paramTransform3D) {
		// give the renderer a chance
		Thread.yield();
		int i = checkResolution(paramCanvas3D, paramTransform3D);
		if (i == OUTSIDE_CANVAS) {
			hideSelf();
			hideSubtree();
			return;
		}
		if ((i == RESOLUTION_UNSUFFICIENT) && (this.children != null)) {
			hideSelf();
			this.subtreeVisible = true;
			for (Cube localCube : this.children)
				if (localCube != null)
					localCube.update(paramCanvas3D, paramTransform3D);
		} else {
			showSelf();
			hideSubtree();
		}
	}

	public int checkResolution(Canvas3D paramCanvas3D, Transform3D paramTransform3D) {
		for (int i = 0; i < this.corners.length; ++i) {
			volumePointInCanvas(paramCanvas3D, paramTransform3D, this.corners[i], this.cornersInCanvas[i]);
		}
		if (outsideCanvas(paramCanvas3D)) {
			return OUTSIDE_CANVAS;
		}

		double d2 = this.cornersInCanvas[0].distance(this.cornersInCanvas[7]);
		double d1 = this.cornersInCanvas[1].distance(this.cornersInCanvas[6]); if (d1 > d2) d2 = d1;
		d1 = this.cornersInCanvas[2].distance(this.cornersInCanvas[5]); if (d1 > d2) d2 = d1;
		d1 = this.cornersInCanvas[3].distance(this.cornersInCanvas[4]); if (d1 > d2) d2 = d1;

		return ((d2 <= RES_THRESHOLD) ? RESOLUTION_SUFFICIENT : RESOLUTION_UNSUFFICIENT);
	}

	public void createChildren() {
		if(level == 1)
			return;
		int l = level >> 1;
		int s = VolumeOctree.SIZE;
		children = new Cube[8];
		children[0] = createCube(octree, dir, x,     y,     z,     l);
		children[1] = createCube(octree, dir, x+l*s, y,     z,     l);
		children[2] = createCube(octree, dir, x,     y+l*s, z,     l);
		children[3] = createCube(octree, dir, x+l*s, y+l*s, z,     l);
		children[4] = createCube(octree, dir, x,     y,     z+l*s, l);
		children[5] = createCube(octree, dir, x+l*s, y,     z+l*s, l);
		children[6] = createCube(octree, dir, x,     y+l*s, z+l*s, l);
		children[7] = createCube(octree, dir, x+l*s, y+l*s, z+l*s, l);
		// children should create their children too
		for(Cube cube : children)
			if(cube != null)
				cube.createChildren();
	}

	Point3d ptmp = new Point3d();
	private final void volumePointInCanvas(Canvas3D canvas, Transform3D volToIP,
						Point3d p, Point2d ret) {

		ptmp.set(p);
		volToIP.transform(ptmp);
		canvas.getPixelLocationFromImagePlate(ptmp, ret);
	}

	private final boolean outsideCanvas(Canvas3D canvas) {
		// check if left
		boolean found = true;
		for(int i = 0; i < 8; i++) {
			if(cornersInCanvas[i].x >= 0) {
				found = false;
				break;
			}
		}
		if(found) return true;
		// top
		found = true;
		for(int i = 0; i < 8; i++) {
			if(cornersInCanvas[i].y >= 0) {
				found = false;
				break;
			}
		}
		if(found) return true;

		int cw = canvas.getWidth(), ch = canvas.getHeight();
		// right
		found = true;
		for(int i = 0; i < 8; i++) {
			if(cornersInCanvas[i].x < cw) {
				found = false;
				break;
			}
		}
		if(found) return true;

		// left
		found = true;
		for(int i = 0; i < 8; i++) {
			if(cornersInCanvas[i].y < ch) {
				found = false;
				break;
			}
		}
		if(found) return true;
		return false;
	}

	@Override
	public String toString() {
		return name;
	}
}


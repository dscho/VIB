package octree;

import java.io.File;

import java.util.List;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import voltex.VolRendConstants;

public class Cube implements VolRendConstants {

	public static final int RESOLUTION_SUFFICIENT   = 0;
	public static final int RESOLUTION_UNSUFFICIENT = 1;
	public static final int OUTSIDE_CANVAS          = 2;

	public static final double RES_THRESHOLD = VolumeOctree.SIZE * 3;//Math.sqrt(3);

	final int x, y, z, level;
	final float pw, ph, pd;
	final String path;
	final String name;
	final String dir;

	private Cube[] children;
	final Point3d[] corners;

	private CubeData cdata;
	private boolean updateNeeded = true;

	private VolumeOctree octree;

	public Cube(VolumeOctree octree, String dir, int x, int y, int z, int l) {
		this.dir = dir + "/";
		this.octree = octree;
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
			corners = new Point3d[8];
			corners[0] = new Point3d(x * octree.pw, y * octree.ph, z * octree.pd);
			corners[7] = new Point3d(corners[0].x + lx, corners[0].y + ly, corners[0].z + lz);
			corners[1] = new Point3d(corners[7].x, corners[0].y, corners[0].z);
			corners[2] = new Point3d(corners[0].x, corners[7].y, corners[0].z);
			corners[3] = new Point3d(corners[7].x, corners[7].y, corners[0].z);
			corners[4] = new Point3d(corners[0].x, corners[0].y, corners[7].z);
			corners[5] = new Point3d(corners[7].x, corners[0].y, corners[7].z);
			corners[6] = new Point3d(corners[0].x, corners[7].y, corners[7].z);
		} else {
			// such an object is hopefully never used.
			pw = ph = pd = -1;
			corners = null;
		}
	}

	public boolean exists() {
		return new File(path).exists();
	}

	public Cube[] getChildren() {
		return children;
	}

	public boolean cubeDataUpToDate() {
		return !updateNeeded;
	}

	public CubeData getCubeData() {
		return cdata;
	}

	public void updateCubeData() {
		try {
			cdata.createData();
			updateNeeded = false;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void collectCubesToDisplay(List<Cube> cubes, Canvas3D canvas, Transform3D volToIP, int axis, int dir) {
		int state = checkResolution(canvas, volToIP);
		if(state == Cube.OUTSIDE_CANVAS) {
			undisplaySelf();
			undisplaySubtree();
			return;
		}
		if(state == Cube.RESOLUTION_UNSUFFICIENT && children != null) {
			// display children
			undisplaySelf();
			for(Cube c : children)
				if(c != null)
					c.collectCubesToDisplay(cubes, canvas, volToIP, axis, dir);
		} else {
			// display self
			if(cdata == null) {
				// The cube was not displayed at all; create new data
				undisplaySubtree();
				cdata = new CubeData(this);
				cdata.prepareForAxis(axis);
				updateNeeded = true;
			} else if(axis == cdata.axis) {
				// the CubeData is fine, just need to display it
				updateNeeded = false;
			} else {
				// CubeData exists, but has the wrong data. Reload it.
				cdata.prepareForAxis(axis);
				updateNeeded = true;
			}
			cubes.add(this);
		}
	}

	public int checkResolution(Canvas3D canvas, Transform3D volToIP) {
		double max, v;
		max = lengthInCanvas(canvas, volToIP, corners[0], corners[7]);
		v = lengthInCanvas(canvas, volToIP, corners[1], corners[6]); if(v > max) max = v;
		v = lengthInCanvas(canvas, volToIP, corners[2], corners[5]); if(v > max) max = v;
		v = lengthInCanvas(canvas, volToIP, corners[3], corners[4]); if(v > max) max = v;
		if(max < 0) {// outside canvas
			return OUTSIDE_CANVAS;
		}
		return max <= RES_THRESHOLD ? RESOLUTION_SUFFICIENT : RESOLUTION_UNSUFFICIENT;
	}

	private void undisplaySubtree() {
		if(children == null)
			return;
		for(Cube c : children) {
			if(c != null) {
				c.undisplaySelf();
				c.undisplaySubtree();
			}
		}
	}

	private void undisplaySelf() {
		if(cdata != null) {
			CubeDataRecycler.instance().deleteCubeData(cdata);
			cdata = null;
		}
	}

	public void createChildren() {
		if(level == 1)
			return;
		int l = level >> 1;
		int s = VolumeOctree.SIZE;
		children = new Cube[8];
		children[0] = new Cube(octree, dir, x,     y,     z,     l); if(!children[0].exists()) children[0] = null;
		children[1] = new Cube(octree, dir, x+l*s, y,     z,     l); if(!children[1].exists()) children[1] = null;
		children[2] = new Cube(octree, dir, x,     y+l*s, z,     l); if(!children[2].exists()) children[2] = null;
		children[3] = new Cube(octree, dir, x+l*s, y+l*s, z,     l); if(!children[3].exists()) children[3] = null;
		children[4] = new Cube(octree, dir, x,     y,     z+l*s, l); if(!children[4].exists()) children[4] = null;
		children[5] = new Cube(octree, dir, x+l*s, y,     z+l*s, l); if(!children[5].exists()) children[5] = null;
		children[6] = new Cube(octree, dir, x,     y+l*s, z+l*s, l); if(!children[6].exists()) children[6] = null;
		children[7] = new Cube(octree, dir, x+l*s, y+l*s, z+l*s, l); if(!children[7].exists()) children[7] = null;
		// children should create their children too
		for(Cube cube : children)
			if(cube != null)
				cube.createChildren();
	}

	private Point2d p1 = new Point2d();
	private Point2d p2 = new Point2d();
	private Point3d p13d = new Point3d();
	private Point3d p23d = new Point3d();
	private final double lengthInCanvas(Canvas3D canvas, Transform3D volToIP,
							Point3d one, Point3d another) {
		p13d.set(one);
		p23d.set(another);
		volumePointInCanvas(canvas, volToIP, p13d, p1);
		volumePointInCanvas(canvas, volToIP, p23d, p2);
		if(outsideCanvas(p1, canvas) && outsideCanvas(p2, canvas))
			return -1;
		return p1.distance(p2);
	}

	private final void volumePointInCanvas(Canvas3D canvas, Transform3D volToIP,
						Point3d p, Point2d ret) {

		volToIP.transform(p);
		canvas.getPixelLocationFromImagePlate(p, ret);
	}

	private final boolean outsideCanvas(Point2d p, Canvas3D canvas) {
		return p.x < 0 ||
			p.y < 0 ||
			p.x >= canvas.getWidth()
			|| p.y >= canvas.getHeight();
	}

	@Override
	public String toString() {
		return name;
	}
}


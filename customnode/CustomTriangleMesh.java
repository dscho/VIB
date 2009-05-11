package customnode;

import isosurface.MeshProperties;

import java.util.List;
import java.util.Arrays;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

public class CustomTriangleMesh extends CustomMesh {

	private double volume = 0.0;

	public CustomTriangleMesh(List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	public CustomTriangleMesh(List<Point3f> mesh, Color3f col, float trans) {
		super(mesh, col, trans);
		if(mesh != null) {
			Point3d center = new Point3d();
			double[][] inertia = new double[3][3];
			volume = MeshProperties.compute(mesh, center, inertia);
		}
	}

	public void setMesh(List<Point3f> mesh) {
		this.mesh = mesh;
		update();
	}

	@Override
	protected GeometryArray createGeometry() {
		if(mesh.size() < 3)
			return null;
		List<Point3f> tri = mesh;
		int nValid = tri.size();
		int nAll = 2 * nValid;

		Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		Color3f colors[] = new Color3f[nValid];
		Arrays.fill(colors, color);

		GeometryArray ta = new TriangleArray(nAll,
						TriangleArray.COORDINATES |
						TriangleArray.COLOR_3 |
						TriangleArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		GeometryInfo gi = new GeometryInfo(ta);
// 		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
// 		Stripifier st = new Stripifier();
// 		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_INTERSECT);
		result.setValidVertexCount(nValid);

		return result;
	}

	@Override
	public float getVolume() {
		return (float)volume;
	}
}

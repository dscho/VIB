package customnode;

import java.util.List;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

public class CustomQuadMesh extends CustomTriangleMesh {
	
	public CustomQuadMesh(List<Point3f> mesh, Color3f color, float trans) {
		super(mesh, color, trans);
	}
	
	@Override
	protected Geometry createGeometry() {
		List<Point3f> tri = mesh;
		Point3f[] coords = (Point3f[]) tri.toArray(new Point3f[] {});

		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		for (int i = 0; i < N; i++) {
			colors[i] = color;
		}

		GeometryArray ta = new TriangleArray(N, TriangleArray.COORDINATES
				| TriangleArray.COLOR_3 | TriangleArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		GeometryInfo gi = new GeometryInfo(ta);
		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
		Stripifier st = new Stripifier();
		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_INTERSECT);

		return result;
	}
}

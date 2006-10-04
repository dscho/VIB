package Quick3dApplet;

public class Octahedron {
	public static void add(RenderObject ro, Vec center, float radius,
			int col) {
		Vertex v1, v2, v3, v4, v5, v6;
		v1 = new Vertex(new Vec(center.x + radius, center.y, center.z));
		v2 = new Vertex(new Vec(center.x - radius, center.y, center.z));
		v3 = new Vertex(new Vec(center.x, center.y + radius, center.z));
		v4 = new Vertex(new Vec(center.x, center.y - radius, center.z));
		v5 = new Vertex(new Vec(center.x, center.y, center.z + radius));
		v6 = new Vertex(new Vec(center.x, center.y, center.z - radius));
		ro.addTri(new ColTri(v1, v5, v3, col));
		ro.addTri(new ColTri(v5, v2, v3, col));
		ro.addTri(new ColTri(v2, v6, v3, col));
		ro.addTri(new ColTri(v6, v1, v3, col));
		ro.addTri(new ColTri(v5, v1, v4, col));
		ro.addTri(new ColTri(v1, v6, v4, col));
		ro.addTri(new ColTri(v6, v2, v4, col));
		ro.addTri(new ColTri(v2, v5, v4, col));
	}
}


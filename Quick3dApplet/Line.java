package Quick3dApplet;

public final class Line extends Tri
{
	private int col;
	private float normx, normy, normz;
	private float lastY=0;

	static Vertex find3rdVertex(Vertex v1, Vertex v2) {
		Vec w1 = v1.getPos();
		Vec w2 = v2.getPos();
		Vec w;
		float dX = Math.abs(w1.x - w2.x);
		float dY = Math.abs(w1.y - w2.y);
		float dZ = Math.abs(w1.z - w2.z);
		if (dX < dY && dX < dZ)
			w = new Vec(0.1f, 0, 0);
		else if (dY < dZ)
			w = new Vec(0, 0.1f, 0);
		else
			w = new Vec(0, 0, 0.1f);

		return new Vertex(Vec.add(Vec.mul(Vec.add(w1, w2),
						0.5f), w));
	}

	public Line(Vertex ai, Vertex bi, int colour) {
		super(ai, bi, find3rdVertex(ai, bi));
		col = colour;
		Vec norm = getOrigNorm();
		norm.makeUnitVec();
		normx = norm.x; normy = norm.y; normz = norm.z;
	}

	public void setCol(int c) {
		col = c;
	}

	public int getCol() {
		return col;
	}

	public void flipNormal() { super.flipNormal(); normx=-normx; normy=-normy; normz=-normz; }

	public int draw(Render r, Pixstore px, int mi, float xStart, float y) {
		if (y>lastY) {
			px.pix[r.idx] = col; ++r.idx;
			lastY = y;
		}
		for(;r.idx<=mi; ++r.idx) {
			// Colour the pixel
			px.pix[r.idx] = col;
		}

		return DONE;
	}

	public FullTriInfo getFullTriInfo() {
		FullTriInfo ti = new FullTriInfo();
		ti.type = FullTriInfo.type_NetTri;
		ti.a.vert = a; ti.b.vert = b; ti.c.vert = c;
		ti.col = col;
		return ti;
	}
}


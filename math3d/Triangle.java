package math3d;

public class Triangle {
	public Point3d a, b, c;

	public Triangle() {}

	public Triangle(Point3d a, Point3d b, Point3d c) {
		this.a = a;
		this.b = b;
		this.c = c;
		calculateCircumcenter2();
	}

	/* circumcenter & radius */
	public Point3d center;
	public double cRadius2;

	public double calculateCircumcenter2() {
		Point3d x = b.minus(a);
		Point3d y = c.minus(a);
		double xx = x.scalar(x);
		double xy = x.scalar(y);
		double yy = y.scalar(y);

		double det = xx * yy - xy * xy;
		double alpha = 0.5 * (yy * xx - xy * yy) / det;
		double beta = 0.5 * (-xy * xx + xx * yy) / det;

		center = new Point3d(a.x + alpha * x.x + beta * y.x,
				a.y + alpha * x.y + beta * y.y,
				a.z + alpha * x.z + beta * y.z);

		cRadius2 = center.distance2(a);

		return cRadius2;
	}

	public static void test() {
		Triangle t = new Triangle();
		t.a = Point3d.random();
		t.a.z = 0;
		t.b = Point3d.random();
		t.b.z = 0;
		t.c = Point3d.random();
		t.c.z = 0;
		t.calculateCircumcenter2();
		Point3d c = t.center;
		double radius = Math.sqrt(t.cRadius2);
		System.out.println("%!PS-1.0\n"
				+t.a+" pop moveto\n"
				+t.b+" pop lineto\n"
				+t.c+" pop lineto closepath stroke\n"
				+t.a+" pop moveto\n"
				+c+" pop lineto stroke\n"
				+c+" pop "+radius+" 0 350 arc stroke\n"
				+"showpage\n");
	}
	public String toString() {
		return "{" + a + "; "+ b + "; " + c + "} ";
	}
}



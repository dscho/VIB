/*
 * At the moment, this class works on double's. To change that,
 * s/double \/\*dtype\*\//int/g
 */

package math3d;

public class Point3d {
	public double /*dtype*/ x, y, z;

	public Point3d() {
	}

	public Point3d(double /*dtype*/ x, double /*dtype*/ y, double /*dtype*/ z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Point3d minus(Point3d other) {
		return new Point3d(x - other.x,
				y - other.y,
				z - other.z);
	}

	public Point3d plus(Point3d other) {
		return new Point3d(x + other.x,
				y + other.y,
				z + other.z);
	}

	public double /*dtype*/ scalar(Point3d other) {
		return x * other.x + y * other.y + z * other.z;
	}

	public Point3d times(double /*dtype*/ factor) {
		return new Point3d(x * factor,
				y * factor,
				z * factor);
	}

	public Point3d vector(Point3d other) {
		return new Point3d(y * other.z - z * other.y,
				z * other.x - x * other.z,
				x * other.y - y * other.x);
	}

	public double length() {
		return Math.sqrt(scalar(this));
	}

	public double /*dtype*/ distance2(Point3d other) {
		double /*dtype*/ x1 = x - other.x;
		double /*dtype*/ y1 = y - other.y;
		double /*dtype*/ z1 = z - other.z;
		return x1 * x1 + y1 * y1 + z1 * z1;
	}

	public double distanceTo(Point3d other) {
		return Math.sqrt(distance2(other));
	}

	public static Point3d average(Point3d[] list) {
		Point3d result = new Point3d();
		for (int i = 0; i < list.length; i++)
			result = result.plus(list[i]);
		return result.times(1.0 / list.length);
	}

	static Point3d random() {
		return new Point3d(Math.random() * 400 + 50,
				Math.random() * 400 + 50,
				Math.random() * 400 + 50);
	}

	public String toString() {
		return "" + x + " " + y + " " + z;
	}
}



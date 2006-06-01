import ij.ImagePlus;
import ij.measure.Calibration;
import java.util.StringTokenizer;
import java.util.Vector;
import math3d.Point3d;
import math3d.Triangle;
import math3d.FastMatrixN;

public class FastMatrix {
	public double x, y, z;

	private double a00, a01, a02, a03,
		a10, a11, a12, a13,
		a20, a21, a22, a23;

	public FastMatrix() { }

	public FastMatrix(double f) { a00 = a11 = a22 = f; }

	public FastMatrix(double[][] m) {
		if ((m.length != 3 && m.length != 4)
				|| m[0].length != 4)
			throw new RuntimeException("Wrong dimensions: "
					+ m.length + "x"
					+ m[0].length);

		a00 = (double)m[0][0];
		a01 = (double)m[0][1];
		a02 = (double)m[0][2];
		a03 = (double)m[0][3];
		a10 = (double)m[1][0];
		a11 = (double)m[1][1];
		a12 = (double)m[1][2];
		a13 = (double)m[1][3];
		a20 = (double)m[2][0];
		a21 = (double)m[2][1];
		a22 = (double)m[2][2];
		a23 = (double)m[2][3];
	}

	/*
	public FastMatrix(Jama.Matrix m) {
		if ((m.getRowDimension() != 3 && m.getRowDimension() != 4)
				|| m.getColumnDimension() != 4)
			throw new RuntimeException("Wrong dimensions: "
					+ m.getRowDimension() + "x"
					+ m.getColumnDimension());

		a00 = (double)m.get(0,0);
		a01 = (double)m.get(0,1);
		a02 = (double)m.get(0,2);
		a03 = (double)m.get(0,3);
		a10 = (double)m.get(1,0);
		a11 = (double)m.get(1,1);
		a12 = (double)m.get(1,2);
		a13 = (double)m.get(1,3);
		a20 = (double)m.get(2,0);
		a21 = (double)m.get(2,1);
		a22 = (double)m.get(2,2);
		a23 = (double)m.get(2,3);
	}
	*/

	public void apply(double x, double y, double z) {
		this.x = x * a00 + y * a01 + z * a02 + a03;
		this.y = x * a10 + y * a11 + z * a12 + a13;
		this.z = x * a20 + y * a21 + z * a22 + a23;
	}

	public void apply(Point3d p) {
		this.x = (double)(p.x * a00 + p.y * a01 + p.z * a02 + a03);
		this.y = (double)(p.x * a10 + p.y * a11 + p.z * a12 + a13);
		this.z = (double)(p.x * a20 + p.y * a21 + p.z * a22 + a23);
	}

	public void applyWithoutTranslation(double x, double y, double z) {
		this.x = x * a00 + y * a01 + z * a02;
		this.y = x * a10 + y * a11 + z * a12;
		this.z = x * a20 + y * a21 + z * a22;
	}

	public void applyWithoutTranslation(Point3d p) {
		this.x = (double)(p.x * a00 + p.y * a01 + p.z * a02);
		this.y = (double)(p.x * a10 + p.y * a11 + p.z * a12);
		this.z = (double)(p.x * a20 + p.y * a21 + p.z * a22);
	}

	public Point3d getResult() {
		return new Point3d(x, y, z);
	}

	public FastMatrix scale(double x, double y, double z) {
		FastMatrix result = new FastMatrix();
		result.a00 = a00 * x;
		result.a01 = a01 * x;
		result.a02 = a02 * x;
		result.a03 = a03 * x;
		result.a10 = a10 * y;
		result.a11 = a11 * y;
		result.a12 = a12 * y;
		result.a13 = a13 * y;
		result.a20 = a20 * z;
		result.a21 = a21 * z;
		result.a22 = a22 * z;
		result.a23 = a23 * z;
		return result;
	}

	public FastMatrix times(FastMatrix o) {
		FastMatrix result = new FastMatrix();
		result.a00 = o.a00 * a00 + o.a10 * a01 + o.a20 * a02;
		result.a10 = o.a00 * a10 + o.a10 * a11 + o.a20 * a12;
		result.a20 = o.a00 * a20 + o.a10 * a21 + o.a20 * a22;
		result.a01 = o.a01 * a00 + o.a11 * a01 + o.a21 * a02;
		result.a11 = o.a01 * a10 + o.a11 * a11 + o.a21 * a12;
		result.a21 = o.a01 * a20 + o.a11 * a21 + o.a21 * a22;
		result.a02 = o.a02 * a00 + o.a12 * a01 + o.a22 * a02;
		result.a12 = o.a02 * a10 + o.a12 * a11 + o.a22 * a12;
		result.a22 = o.a02 * a20 + o.a12 * a21 + o.a22 * a22;
		apply(o.a03, o.a13, o.a23);
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}

	/* this inverts just the first 3 columns, interpreted as 3x3 matrix */
	private FastMatrix invert3x3() {
		double sub00 = a11 * a22 - a12 * a21;
		double sub01 = a10 * a22 - a12 * a20;
		double sub02 = a10 * a21 - a11 * a20;
		double sub10 = a01 * a22 - a02 * a21;
		double sub11 = a00 * a22 - a02 * a20;
		double sub12 = a00 * a21 - a01 * a20;
		double sub20 = a01 * a12 - a02 * a11;
		double sub21 = a00 * a12 - a02 * a10;
		double sub22 = a00 * a11 - a01 * a10;
		double det = a00 * sub00 - a01 * sub01 + a02 * sub02;

		FastMatrix result = new FastMatrix();
		result.a00 = sub00 / det;
		result.a01 = -sub10 / det;
		result.a02 = sub20 / det;
		result.a10 = -sub01 / det;
		result.a11 = sub11 / det;
		result.a12 = -sub21 / det;
		result.a20 = sub02 / det;
		result.a21 = -sub12 / det;
		result.a22 = sub22 / det;
		return result;
	}

	public FastMatrix inverse() {
		FastMatrix result = invert3x3();
		result.apply(-a03, -a13, -a23);
		result.a03 = result.x;
		result.a13 = result.y;
		result.a23 = result.z;
		return result;
	}

	public static FastMatrix rotate(double angle, int axis) {
		FastMatrix result = new FastMatrix();
		double c = (double)Math.cos(angle);
		double s = (double)Math.sin(angle);
		switch(axis) {
			case 0:
				result.a11 = result.a22 = c;
				result.a12 = -(result.a21 = s);
				result.a00 = (double)1.0;
				break;
			case 1:
				result.a00 = result.a22 = c;
				result.a02 = -(result.a20 = s);
				result.a11 = (double)1.0;
				break;
			case 2:
				result.a00 = result.a11 = c;
				result.a01 = -(result.a10 = s);
				result.a22 = (double)1.0;
				break;
			default:
				throw new RuntimeException("Illegal axis: "+axis);
		}
		return result;
	}

	public static FastMatrix rotateAround(double nx, double ny, double nz,
			double angle) {
		FastMatrix r = new FastMatrix();
		double c = (double)Math.cos(angle), s = (double)Math.sin(angle);

		r.a00 = -(c-1)*nx*nx + c;
		r.a01 = -(c-1)*nx*ny - s*nz;
		r.a02 = -(c-1)*nx*nz + s*ny;
		r.a03 = 0;
		r.a10 = -(c-1)*nx*ny + s*nz;
		r.a11 = -(c-1)*ny*ny + c;
		r.a12 = -(c-1)*ny*nz - s*nx;
		r.a13 = 0;
		r.a20 = -(c-1)*nx*nz - s*ny;
		r.a21 = -(c-1)*ny*nz + s*nx;
		r.a22 = -(c-1)*nz*nz + c;
		r.a23 = 0;

		return r;
	}

	/*
	 * Euler rotation means to rotate around the z axis first, then
	 * around the rotated x axis, and then around the (twice) rotated
	 * z axis.
	 */
	public static FastMatrix rotateEuler(double a1, double a2, double a3) {
		FastMatrix r = new FastMatrix();
		double c1 = (double)Math.cos(a1), s1 = (double)Math.sin(a1);
		double c2 = (double)Math.cos(a2), s2 = (double)Math.sin(a2);
		double c3 = (double)Math.cos(a3), s3 = (double)Math.sin(a3);

		r.a00 = c3*c1-c2*s1*s3;
		r.a01 = -s3*c1-c2*s1*c3;
		r.a02 = s2*s1;
		r.a03 = 0;
		r.a10 = c3*s1+c2*c1*s3;
		r.a11 = -s3*s1+c2*c1*c3;
		r.a12 = -s2*c1;
		r.a13 = 0;
		r.a20 = s2*s3;
		r.a21 = s2*c3;
		r.a22 = c2;
		r.a23 = 0;

		return r;
	}

	/*
	 * same as rotateEuler, but with a center different from the origin
	 */
	public static FastMatrix rotateEulerAt(double a1, double a2, double a3,
			double cx, double cy, double cz) {
		FastMatrix r = new FastMatrix();
		double c1 = (double)Math.cos(a1), s1 = (double)Math.sin(a1);
		double c2 = (double)Math.cos(a2), s2 = (double)Math.sin(a2);
		double c3 = (double)Math.cos(a3), s3 = (double)Math.sin(a3);

		r.a00 = c3*c1-c2*s1*s3;
		r.a01 = -s3*c1-c2*s1*c3;
		r.a02 = s2*s1;
		r.a03 = 0;
		r.a10 = c3*s1+c2*c1*s3;
		r.a11 = -s3*s1+c2*c1*c3;
		r.a12 = -s2*c1;
		r.a13 = 0;
		r.a20 = s2*s3;
		r.a21 = s2*c3;
		r.a22 = c2;
		r.a23 = 0;

		r.apply(cx, cy, cz);
		r.a03 = cx - r.x;
		r.a13 = cy - r.y;
		r.a23 = cz - r.z;

		return r;
	}

	/*
	 * Calculate the parameters needed to generate this matrix by
	 * rotateEulerAt()
	 */
	public void guessEulerParameters(double[] parameters) {
		if (parameters.length != 6)
			throw new IllegalArgumentException(
					"Need 6 parameters, got "
					+ parameters.length);
		guessEulerParameters(parameters, null);
	}

	public void guessEulerParameters(double[] parameters, Point3d center) {
		if (center != null && parameters.length != 9)
			throw new IllegalArgumentException(
					"Need 9 parameters, got "
					+ parameters.length);

		if (a21 == 0.0 && a20 == 0.0) {
			/*
			 * s2 == 0, therefore a2 == 0, therefore a1 and a3
			 * are not determined (they are both rotations around
			 * the z axis. Choose a3 = 0.
			 */
			parameters[2] = 0;
			parameters[1] = 0;
			parameters[0] = (double)Math.atan2(a10, a00);
		} else {
			parameters[2] = (double)Math.atan2(a20, a21);
			parameters[1] = (double)Math.atan2(
					Math.sqrt(a21 * a21 + a20 * a20), a22);
			parameters[0] = (double)Math.atan2(a02, -a12);
		}

		/*
		 * If a center of rotation was given, the parameters will
		 * contain:
		 * (angleZ, angleX, angleZ2, transX, transY, transZ,
		 *  centerX, centerY, centerZ) where trans is the translation
		 *  _after_ the rotation around center.
		 */
		if (center != null) {
			parameters[6] = (double)center.x;
			parameters[7] = (double)center.y;
			parameters[8] = (double)center.z;
			apply(center);
			parameters[3] = x - (double)center.x;
			parameters[4] = y - (double)center.y;
			parameters[5] = z - (double)center.z;
			return;
		}

		/*
		 * The center (if none was specified) is ambiguous along
		 * the rotation axis.
		 * To find a center, we rotate the origin twice, and
		 * calculate the circumcenter of the resulting triangle.
		 * This also happens to be the point on the axis which
		 * is closest to the origin.
		 */
		if (a03 == 0.0 && a13 == 0.0 && a23 == 0.0) {
			parameters[3] = parameters[4] = parameters[5] = 0;
		} else {
			apply(a03, a13, a23);
			Triangle t = new Triangle(
					new Point3d(0, 0, 0),
					new Point3d(a03, a13, a23),
					new Point3d(x, y, z));
			t.calculateCircumcenter2();
			parameters[3] = (double)t.center.x;
			parameters[4] = (double)t.center.y;
			parameters[5] = (double)t.center.z;
		}
	}

	public static FastMatrix translate(double x, double y, double z) {
		FastMatrix result = new FastMatrix();
		result.a00 = result.a11 = result.a22 = (double)1.0;
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}

	/*
	 * least squares fitting of a linear transformation which maps
	 * the points x[i] to y[i] as best as possible.
	 */
	public static FastMatrix bestLinear(Point3d[] x, Point3d[] y) {
		if (x.length != y.length)
			throw new RuntimeException("different lengths");

		double[][] a = new double[4][4];
		double[][] b = new double[4][4];

		for (int i = 0; i < a.length; i++) {
			a[0][0] += (double)(x[i].x * x[i].x);
			a[0][1] += (double)(x[i].x * x[i].y);
			a[0][2] += (double)(x[i].x * x[i].z);
			a[0][3] += (double)(x[i].x);
			a[1][1] += (double)(x[i].y * x[i].y);
			a[1][2] += (double)(x[i].y * x[i].z);
			a[1][3] += (double)(x[i].y);
			a[2][2] += (double)(x[i].z * x[i].z);
			a[2][3] += (double)(x[i].z);

			b[0][0] += (double)(x[i].x * y[i].x);
			b[0][1] += (double)(x[i].y * y[i].x);
			b[0][2] += (double)(x[i].z * y[i].x);
			b[0][3] += (double)(y[i].x);
			b[1][0] += (double)(x[i].x * y[i].y);
			b[1][1] += (double)(x[i].y * y[i].y);
			b[1][2] += (double)(x[i].z * y[i].y);
			b[1][3] += (double)(y[i].y);
			b[2][0] += (double)(x[i].x * y[i].z);
			b[2][1] += (double)(x[i].y * y[i].z);
			b[2][2] += (double)(x[i].z * y[i].z);
			b[2][3] += (double)(y[i].z);
		}

		a[1][0] = a[0][1];
		a[2][0] = a[0][2];
		a[2][1] = a[1][2];
		a[3][0] = a[0][3];
		a[3][1] = a[1][3];
		a[3][2] = a[2][3];
		a[3][3] = 1;
		FastMatrixN.invert(a);
		double[][] r = FastMatrixN.times(b, a);

		FastMatrix result = new FastMatrix();
		result.a00 = r[0][0];
		result.a01 = r[0][1];
		result.a02 = r[0][2];
		result.a03 = r[0][3];
		result.a10 = r[1][0];
		result.a11 = r[1][1];
		result.a12 = r[1][2];
		result.a13 = r[1][3];
		result.a20 = r[2][0];
		result.a21 = r[2][1];
		result.a22 = r[2][2];
		result.a23 = r[2][3];
		return result;
	}

	public static FastMatrix average(FastMatrix[] array) {
		FastMatrix result = new FastMatrix();
		int n = 0;
		for (int i = 0; i < array.length; i++)
			if (array[i] != null) {
				n++;
				result.a00 += array[i].a00;
				result.a01 += array[i].a01;
				result.a02 += array[i].a02;
				result.a03 += array[i].a03;
				result.a10 += array[i].a10;
				result.a11 += array[i].a11;
				result.a12 += array[i].a12;
				result.a13 += array[i].a13;
				result.a20 += array[i].a20;
				result.a21 += array[i].a21;
				result.a22 += array[i].a22;
				result.a23 += array[i].a23;
			}
		if (n > 0) {
			result.a00 /= (double)n;
			result.a01 /= (double)n;
			result.a02 /= (double)n;
			result.a03 /= (double)n;
			result.a10 /= (double)n;
			result.a11 /= (double)n;
			result.a12 /= (double)n;
			result.a13 /= (double)n;
			result.a20 /= (double)n;
			result.a21 /= (double)n;
			result.a22 /= (double)n;
			result.a23 /= (double)n;
		}
		return result;
	}

	/*
	 * parses both uniform 4x4 matrices (column by column), and
	 * 3x4 matrices (row by row).
	 */
	public static FastMatrix parseMatrix(String m) {
		FastMatrix matrix = new FastMatrix();
		StringTokenizer tokenizer = new StringTokenizer(m);
		try {
			/*
			 * Amira notates a uniform matrix in 4x4 notation,
			 * column by column.
			 * Common notation is to notate 3x4 notation, row by
			 * row, since the last row does not bear any
			 * information (but is always "0 0 0 1").
			 */
			boolean is4x4Columns = true;

			matrix.a00 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a10 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a20 = (double)Double.parseDouble(tokenizer.nextToken());
			double dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (dummy != 0.0) {
				is4x4Columns = false;
				matrix.a03 = dummy;
			}
			matrix.a01 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a11 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a21 = (double)Double.parseDouble(tokenizer.nextToken());
			dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a13 = dummy;

			matrix.a02 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a12 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a22 = (double)Double.parseDouble(tokenizer.nextToken());
			dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a23 = dummy;

			if (is4x4Columns) {
				if (!tokenizer.hasMoreTokens())
					is4x4Columns = false;
			} else if (tokenizer.hasMoreTokens())
				throw new RuntimeException("Not a uniform matrix: "+m);

			if (is4x4Columns) {
				matrix.a03 = (double)Double.parseDouble(tokenizer.nextToken());
				matrix.a13 = (double)Double.parseDouble(tokenizer.nextToken());
				matrix.a23 = (double)Double.parseDouble(tokenizer.nextToken());
				if (Double.parseDouble(tokenizer.nextToken()) != 1.0)
					throw new RuntimeException("Not a uniform matrix: "+m);
			} else {
				// swap rotation part
				dummy = matrix.a01; matrix.a01 = matrix.a10; matrix.a10 = dummy;
				dummy = matrix.a02; matrix.a02 = matrix.a20; matrix.a20 = dummy;
				dummy = matrix.a12; matrix.a12 = matrix.a21; matrix.a21 = dummy;
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return matrix;
	}

	public static FastMatrix[] parseMatrices(String m) {
		Vector vector = new Vector();
		StringTokenizer tokenizer = new StringTokenizer(m, ",");
		while (tokenizer.hasMoreTokens()) {
			String matrix = tokenizer.nextToken().trim();
			if (matrix.equals(""))
				vector.add(null);
			else
				vector.add(parseMatrix(matrix));
		}
		FastMatrix[] result = new FastMatrix[vector.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (FastMatrix)vector.get(i);
		return result;
	}

	public static FastMatrix fromCalibration(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FastMatrix result = new FastMatrix();
		result.a00 = (double)calib.pixelWidth;
		result.a11 = (double)calib.pixelHeight;
		result.a22 = (double)calib.pixelDepth;
		result.a03 = (double)calib.xOrigin;
		result.a13 = (double)calib.yOrigin;
		result.a23 = (double)calib.zOrigin;
		return result;
	}

	// 
	public static FastMatrix translateToCenter(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FastMatrix result = new FastMatrix();
		result.a00 = (double)1;
		result.a11 = (double)1;
		result.a22 = (double)1;
		result.a03 = (double)(calib.xOrigin + calib.pixelWidth * image.getWidth() / 2.0);
		result.a13 = (double)(calib.yOrigin + calib.pixelHeight * image.getHeight() / 2.0);
		result.a23 = (double)(calib.yOrigin + calib.pixelDepth * image.getStack().getSize() / 2.0);
		return result;
	}

	final public boolean isIdentity() {
		return isIdentity((double)1e-10);
	}

	final public boolean isIdentity(double eps) {
		return eps > (double)Math.abs(a00 - 1) &&
			eps > (double)Math.abs(a11 - 1) &&
			eps > (double)Math.abs(a22 - 1) &&
			eps > (double)Math.abs(a01) &&
			eps > (double)Math.abs(a02) &&
			eps > (double)Math.abs(a03) &&
			eps > (double)Math.abs(a10) &&
			eps > (double)Math.abs(a12) &&
			eps > (double)Math.abs(a13) &&
			eps > (double)Math.abs(a20) &&
			eps > (double)Math.abs(a21) &&
			eps > (double)Math.abs(a23);
	}

	public String resultToString() {
		return "" + x + " " + y + " " + z;
	}

	public String toString() {
		return "" + a00 + " " + a01 + " " + a02 + " " + a03 + "   "
			+ a10 + " " + a11 + " " + a12 + " " + a13 + "   "
			+ a20 + " " + a21 + " " + a22 + " " + a23 + "   ";
	}

	public String toStringForAmira() {
		return "" + a00 + " " + a10 + " " + a20 + " 0 "
			+ a01 + " " + a11 + " " + a21 + " 0 "
			+ a02 + " " + a12 + " " + a22 + " 0 "
			+ a03 + " " + a13 + " " + a23 + " 1";
	}

	public static void main(String[] args) {
		FastMatrix ma = parseMatrix("1 5 9 0 2 6 10 0 3 7 11 0 4 8 12 1");
		System.err.println("ma:\n"+ma);
		FastMatrix mb = parseMatrix("1 2 3 0 5 6 7 0 9 10 11 0");
		System.err.println("mb:\n"+mb);
		if (true)
			return;
		double a1 = 0, a2 = 0, a3 = 0;
		double cx = 1200, cy = 250, cz = 380;
		double[] params = new double[6];
		for (int i = 0; i < 15; i++) {
			if (i < 5)
				a1 += Math.PI/6.0;
			else if (i < 10)
				a2 += Math.PI/6.0;
			else
				a3 += Math.PI/12.0;
			FastMatrix m = rotateEulerAt(a1, a2, a3, cx, cy, cz);
			m.guessEulerParameters(params);
			System.err.println("+++ " + a1 + " " + a2 + " " + a3 + " " + cx + " " + cy + " " + cz);
			System.err.println(m.toString());
			System.err.println("--- " + params[0] + " " + params[1] + " " + params[2] + " " + params[3] + " " + params[4] + " " + params[5]);
			System.err.println(rotateEulerAt(params[0], params[1], params[2], params[3], params[4], params[5]).toString());
			System.out.println("$data setTransform "
					+ m.toStringForAmira());
			m.apply(cx, cy, cz);
			//System.err.println(m.resultToString());
			System.out.println("viewer redraw");
			System.out.println("sleep 1");
		}
		/*
		FastMatrix fromTemplate = FastMatrix.parseMatrix(""+(624.39/256)+" 0 0 0 0 "+(624.39/256)+" 0 0 0 0 "+(225.854/57)+" 0 0 0 0 1");
		FastMatrix fromModel = FastMatrix.parseMatrix(""+(648.716/256)+" 0 0 0 0 "+(648.716/256)+" 0 0 0 0 "+(204.005/52)+" 0 0 0 0 1");
		FastMatrix medullaRtrans = FastMatrix.parseMatrix("-0.368795 0.879484 0.00272346 0 -0.768404 -0.323649 0.462943 0 0.427849 0.176829 0.833778 0 708.495 123.043 -153.796 1");
		
		FastMatrix n = fromTemplate;
		n.apply(78.6146607088518f, 205.734800365156f, 31.0913908985451f);
		System.err.println(""+n);

		FastMatrix m = medullaRtrans.inverse();
		m.apply(191.743f, 501.792f, 123.195f);
		System.err.println(""+m);

		n = m.times(fromTemplate);
		n.apply(78.6146607088518f, 205.734800365156f, 31.0913908985451f);
		System.err.println(""+n);

		n = fromModel;
		n.apply(227, 175, 21);
		System.err.println(""+n);


		if (fromTemplate != null)
			return;

		FastMatrix from = translate(-400, -400, -200).times(new FastMatrix(20));
		FastMatrix rot = rotate((double)(Math.PI/2.0), 2);
		FastMatrix to = new FastMatrix((double)1.0/20).times(translate(400, 400, 200));

		FastMatrix cumul = to.times(rot.times(from));
		FastMatrix cumul2 = to.times(rot).times(from);

		from.apply(30,20,10);
		rot.apply(from.x, from.y, from.z);
		to.apply(rot.x, rot.y, rot.z);
		cumul.apply(30,20,10);
		cumul2.apply(30,20,10);

		System.err.println(""+(new FastMatrix((double)1.0/20).times(translate(400,400,200))));
		System.err.println("\n"+to+"\n"+cumul+"\n"+cumul2+"\n");

		if (from != null)
			return;

		Jama.Matrix m1 = new Jama.Matrix(4,4);
		m1.set(0,0,Math.random());
		m1.set(0,1,Math.random());
		m1.set(0,2,Math.random());
		m1.set(0,3,Math.random());
		m1.set(1,0,Math.random());
		m1.set(1,1,Math.random());
		m1.set(1,2,Math.random());
		m1.set(1,3,Math.random());
		m1.set(2,0,Math.random());
		m1.set(2,1,Math.random());
		m1.set(2,2,Math.random());
		m1.set(2,3,Math.random());
		m1.set(3,0,0);
		m1.set(3,1,0);
		m1.set(3,2,0);
		m1.set(3,3,1);
		Jama.Matrix m2 = new Jama.Matrix(4,4);
		m2.set(0,0,Math.random());
		m2.set(0,1,Math.random());
		m2.set(0,2,Math.random());
		m2.set(0,3,Math.random());
		m2.set(1,0,Math.random());
		m2.set(1,1,Math.random());
		m2.set(1,2,Math.random());
		m2.set(1,3,Math.random());
		m2.set(2,0,Math.random());
		m2.set(2,1,Math.random());
		m2.set(2,2,Math.random());
		m2.set(2,3,Math.random());
		m2.set(3,0,0);
		m2.set(3,1,0);
		m2.set(3,2,0);
		m2.set(3,3,1);
		Jama.Matrix m3 = m1.times(m2);
		FastMatrix t1 = new FastMatrix(m3);
		FastMatrix t2 = new FastMatrix(m1).times(new FastMatrix(m2));
		System.err.println("test:\n" + t1 + ",\n" + t2);

		m3 = new Jama.Matrix(4,4);
		  m3.set(0,0,1); m3.set(0,1,-2); m3.set(0,2,-1);
		  m3.set(1,0,2); m3.set(1,1,-3); m3.set(1,2,0);
		  m3.set(2,0,4); m3.set(2,1,-2); m3.set(2,2,1);
		  m3.set(3,3,1);
		FastMatrix t3 = new FastMatrix(m3.inverse());
		FastMatrix t4 = new FastMatrix(m3).inverse();
		System.err.println("test:\n" + t3 + ",\n" + t4);
		*/
	}
}


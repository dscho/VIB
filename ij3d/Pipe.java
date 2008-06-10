/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a cut-down version of the TrakEM2 Pipe class, which is here
   so that we can use the "makeTube" function to construct meshes from
   a series of points with diameters withouth having to include the
   complete TrakEM2_.jar as a dependency.

   It's not very sensible repeating all this code, of course - ideally
   most of this functionality should be in the 3D viewer, but one can
   work from this starting point to do that.

   As an example, you might use this like:

		double [][][] allPoints = Pipe.makeTube(x_points_d,
							y_points_d,
							z_points_d,
							diameters,
							4,       // resample - 1 means just "use mean distance between points", 3 is three times that, etc.
							12);     // "parallels" (12 means cross-sections are dodecagons)

		java.util.List triangles = Pipe.generateTriangles(allPoints,
								  1); // scale

		String title = "helloooo";

		univ.resetView();

		univ.addMesh(triangles,
			     new Color3f(Color.green),
			     title,
			     1); // threshold, ignored for meshes, I think

   (Mark Longair 2008-06-05) */

/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ij3d;

import java.util.List;
import java.util.ArrayList;

import javax.vecmath.Point3f;

import Jama.Matrix;

import ij.IJ;

public class Pipe {

	/** Will make a new double[] array, then fit in it as many points from the given array as possible according to the desired new length. If the new length is shorter that a.length, it will shrink and crop from the end; if larger, the extra spaces will be set with zeros. */
	static private final double[] copy(final double[] a, final int new_length) {
		final double[] b = new double[new_length];
		final int len = a.length > new_length ? new_length : a.length;
		System.arraycopy(a, 0, b, 0, len);
		return b;
	}

	private static class ResamplingData {

		private double[] rx, ry, rz,
			vx, vy, vz;
		private double[][] dep;

		/** Initialize with a starting length. */
		ResamplingData(final int length, final double[][] dep) {
			// resampled points
			rx = new double[length];
			ry = new double[length];
			rz = new double[length];
			// vectors
			vx = new double[length];
			vy = new double[length];
			vz = new double[length];
			// dependents
			if (null != dep) this.dep = new double[dep.length][length];
		}
		/** Arrays are enlarged if necessary.*/
		final void setP(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = xval;
			this.ry[i] = yval;
			this.rz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setV(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setPV(final int i, final double rxval, final double ryval, final double rzval, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = rxval;
			this.ry[i] = ryval;
			this.rz[i] = rzval;
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		final void resize(final int new_length) {
			this.rx = copy(this.rx, new_length);
			this.ry = copy(this.ry, new_length);
			this.rz = copy(this.rz, new_length);
			this.vx = copy(this.vx, new_length);
			this.vy = copy(this.vy, new_length);
			this.vz = copy(this.vz, new_length);
			if (null != dep) {
				// java doesn't have generators! ARGH
				double[][] dep2 = new double[dep.length][];
				for (int i=0; i<dep.length; i++) dep2[i] = copy(dep[i], new_length);
				dep = dep2;
			}
		}
		final double x(final int i) { return rx[i]; }
		final double y(final int i) { return ry[i]; }
		final double z(final int i) { return rz[i]; }
		/** Distance from point rx[i],ry[i], rz[i] to point x[j],y[j],z[j] */
		final double distance(final int i, final double x, final double y, final double z) {
			return Math.sqrt(Math.pow(x - rx[i], 2)
					 + Math.pow(y - ry[i], 2)
					 + Math.pow(z - rz[i], 2));
		}
		final void put(final VectorString3D vs, final int length) {
			vs.x = copy(this.rx, length); // crop away empty slots
			vs.y = copy(this.ry, length);
			vs.z = copy(this.rz, length);
			vs.vx = copy(this.vx, length);
			vs.vy = copy(this.vy, length);
			vs.vz = copy(this.vz, length);
			vs.length = length;
			if (null != dep) {
				vs.dep = new double[dep.length][];
				for (int i=0; i<dep.length; i++) vs.dep[i] = copy(dep[i], length);
			}
		}
		final void setDeps(final int i, final double[][] src_dep, final int[] ahead, final double[] weight, final int len) {
			if (null == dep) return;
			if (i >= rx.length) resize(i+10);
			//
			for (int k=0; k<dep.length; k++) {
				for (int j=0; j<len; j++) {
					dep[k][i] += src_dep[k][ahead[j]] * weight[j];
				}
			} // above, the ahead and weight arrays (which have the same length) could be of larger length than the given 'len', thus len is used.
		}
	}

	// A mini version of Vector3 taken from ini.trakem2.utils.Vector3
	private static class Vector3 {
		double x, y, z;
		public Vector3() {
			this(0,0,0);
		}
		public double length() {
			return Math.sqrt(x*x + y*y + z*z);
		}
		public Vector3(double x, double y, double z) {
			this.x = x; this.y = y; this.z = z;
		}
		public Vector3 normalize(Vector3 r) {
			if (r == null) r = new Vector3();
			double vlen = length();
			if (vlen != 0.0) {
				return r.set(x/vlen, y/vlen, z/vlen);
			}
			return null;
		}
		public Vector3 scale(double s, Vector3 r) {
			if (r == null) r = new Vector3();
			return r.set(s*x, s*y, s*z);
		}
		public Vector3 set(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}
	}

	private static class Vector {
		private double x, y, z;
		private double length;
		// 0 coords and 0 length, virtue of the 'calloc'
		Vector() {}
		Vector(final double x, final double y, final double z) {
			set(x, y, z);
		}
		Vector(final Vector v) {
			this.x = v.x;
			this.y = v.y;
			this.z = v.z;
			this.length = v.length;
		}
		final public Object clone() {
			return new Vector(this);
		}
		final void set(final double x, final double y, final double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.length = computeLength();
		}
		final void normalize() {
			if (0 == length) return;
			// check if length is already 1
			if (Math.abs(1 - length) < 0.00000001) return; // already normalized
			this.x /= length;
			this.y /= length;
			this.z /= length;
			this.length = computeLength(); // should be 1
		}
		final double computeLength() {
			return Math.sqrt(x*x + y*y + z*z);
		}
		final double length() {
			return length;
		}
		final void scale(final double factor) {
			this.x *= factor;
			this.y *= factor;
			this.z *= factor;
			this.length = computeLength();
		}
		final void add(final Vector v, final boolean compute_length) {
			this.x += v.x;
			this.y += v.y;
			this.z += v.z;
			if (compute_length) this.length = computeLength();
		}
		final void setLength(final double len) {
			normalize();
			scale(len);
		}
		final void put(final int i, final ResamplingData r) {
			r.setPV(i, r.x(i-1) + this.x, r.y(i-1) + this.y, r.z(i-1) + this.z, this.x, this.y, this.z);
		}
		/** As row. */
		final void put(final double[] d) {
			d[0] = x;
			d[1] = y;
			d[2] = z;
		}
		/** As column. */
		final void put(final double[][] d, final int col) {
			d[0][col] = x;
			d[1][col] = y;
			d[2][col] = z;
		}
		final void put(final int i, final double[] x, final double[] y, final double[] z) {
			x[i] = this.x;
			y[i] = this.y;
			z[i] = this.z;
		}
		final Vector getCrossProduct(final Vector v) {
			// (a1; a2; a3) x (b1; b2; b3) = (a2b3 - a3b2; a3b1 - a1b3; a1b2 - a2b1)
			return new Vector(y * v.z - z * v.y,
					  z * v.x - x * v.z,
					  x * v.y - y * v.x);
		}
		final void setCrossProduct(final Vector v, final Vector w) {
			this.x = v.y * w.z - v.z * w.y;
			this.y = v.z * w.x - v.x * w.z;
			this.z = v.x * w.y - v.y * w.x;
		}
		/** Change coordinate system. */
		final void changeRef(final Vector v_delta, final Vector v_i1, final Vector v_new1) { // this vector works like new2
			// ortogonal system 1: the target
			// (a1'; a2'; a3')
			Vector a2 = new Vector(  v_new1   );  // vL
			a2.normalize();
			Vector a1 = a2.getCrossProduct(v_i1); // vQ
			a1.normalize();
			Vector a3 = a2.getCrossProduct(a1);
			// no need //a3.normalize();

			final double[][] m1 = new double[3][3];
			a1.put(m1, 0);
			a2.put(m1, 1);
			a3.put(m1, 2);
			final Matrix mat1 = new Matrix(m1);

			// ortogonal system 2: the current
			// (a1'; b2'; b3')
			Vector b2 = new Vector(  v_delta  ); // vA
			b2.normalize();
			Vector b3 = a1.getCrossProduct(b2); // vQ2

			final double[][] m2 = new double[3][3];
			a1.put(m2, 0);
			b2.put(m2, 1);
			b3.put(m2, 2);
			final Matrix mat2 = new Matrix(m2).transpose();

			final Matrix R = mat1.times(mat2);
			final Matrix mthis = new Matrix(new double[]{this.x, this.y, this.z}, 1);
			// The rotated vector as a one-dim matrix
			// (i.e. the rescued difference vector as a one-dimensional matrix)
			final Matrix v_rot = R.transpose().times(mthis.transpose()); // 3x3 times 3x1, hence the transposing of the 1x3
			final double[][] arr = v_rot.getArray();
			// done!
			this.x = arr[0][0];
			this.y = arr[1][0];
			this.z = arr[2][0];
		}
	}

	// A mini version of VectorString3D taken from ini.trakem2.vector.VectorString3D:
	private static class VectorString3D {

		/** Points. */
		private double[] x, y, z;
		/** Vectors, after resampling. */
		private double[] vx, vy, vz;
		/** Relative vectors, after calling 'relative()'. */
		private double[] rvx, rvy, rvz;
		/** Length of points and vectors - since arrays may be a little longer. */
		private int length = 0;
		/** The point interdistance after resampling. */
		private double delta = 0;


		public VectorString3D(double[] x, double[] y, double[] z) {
			if (!(x.length == y.length && x.length == z.length))
				throw new RuntimeException("x,y,z must have the same length.");
			this.length = x.length;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/** Dependent arrays that will get resampled along. */
		private double[][] dep;

		/** Add an array that will get resampled along; must be of the same length as the value returned by length() */
		public void addDependent(final double[] a) throws Exception {
			if (a.length != this.length) throw new Exception("Dependent array must be of the same size as thevalue returned by length()");
			if (null == dep) {
				dep = new double[1][];
				dep[0] = a;
			} else {
				// resize and append
				double[][] dep2 = new double[dep.length + 1][];
				for (int i=0; i<dep.length; i++) dep2[i] = dep[i];
				dep2[dep.length] = a;
				dep = dep2;
			}
		}
		public double[] getDependent(final int i) {
			return dep[i];
		}

		/** Return the average point interdistance. */
		public double getAverageDelta() {
			double d = 0;
			for (int i=length -1; i>0; i--) {
				d += Math.sqrt( Math.pow(x[i] - x[i-1], 2) + Math.pow(y[i] - y[i-1], 2) + Math.pow(z[i] - z[i-1], 2));
			}
			return d / length;
		}

		/** Homogenize the average point interdistance to 'delta'. */
		public void resample(double delta) {
			if (Math.abs(delta - this.delta) < 0.0000001) {
				// delta is the same
				return;
			}
			this.delta = delta; // store for checking purposes
			this.resample();
		}

		/** The length of this string, that is, the number of points (and vectors) in it. */
		public final int length() { return length; }
		public double[] getPoints(final int dim) {
			switch (dim) {
			case 0: return x;
			case 1: return y;
			case 2: return z;
			}
			return null;
		}

		private boolean isClosed() {
			return false;
		}

		private final void recalculate(final double[] w, final int length, final double sum_) {
			double sum = 0;
			int q;
			for (q=0; q<length; q++) {
				w[q] = w[q] / sum_;
				sum += w[q];
			}
			double error = 1.0 - sum;
			// make it be an absolute value
			if (error < 0.0) {
				error = -error;
			}
			if (error < 0.005) {
				w[0] += 1.0 - sum;
			} else if (sum > 1.0) {
				recalculate(w, length, sum);
			}
		}

		private void resample() {
			// parameters
			final int MAX_AHEAD = 6;
			final double MAX_DISTANCE = 2.5 * delta;

			// convenient data carrier and editor
			final ResamplingData r = new ResamplingData(this.length, this.dep);
			final Vector vector = new Vector();

			// first resampled point is the same as point zero
			r.setP(0, x[0], y[0], z[0]);
			// the first vector is 0,0,0 unless the path is closed, in which case it contains the vector from last-to-first.

			// index over x,y,z
			int i = 1;
			// index over rx,ry,rz (resampled points)
			int j = 1;
			// some vars
			int t, s, ii, u, iu, k;
			int prev_i = i;
			double dist_ahead, dist1, dist2, sum;
			final double[] w = new double[MAX_AHEAD];
			final double[] distances = new double[MAX_AHEAD];
			final Vector[] ve = new Vector[MAX_AHEAD];
			int next_ahead;
			for (next_ahead = 0; next_ahead < MAX_AHEAD; next_ahead++) ve[next_ahead] = new Vector();
			final int[] ahead = new int[MAX_AHEAD];

			try {

				// start infinite loop
				for (;prev_i <= i;) {
					if (prev_i > i || (!isClosed() && i == this.length -1)) break;
					// get distances of MAX_POINTs ahead from the previous point
					next_ahead = 0;
					for (t=0; t<MAX_AHEAD; t++) {
						s = i + t;
						// fix 's' if it goes over the end
						if (s >= this.length) {
							if (isClosed()) s -= this.length;
							else break;
						}
						dist_ahead = r.distance(j-1, x[s], y[s], z[s]);
						if (dist_ahead < MAX_DISTANCE) {
							ahead[next_ahead] = s;
							distances[next_ahead] = dist_ahead;
							next_ahead++;
						}
					}
					if (0 == next_ahead) {
						// No points (ahead of the i point) are found within MAX_DISTANCE
						// Just use the next point as target towards which create a vector of length delta
						vector.set(x[i] - r.x(j-1), y[i] - r.y(j-1), z[i] - r.z(j-1));
						dist1 = vector.length();
						vector.setLength(delta);
						vector.put(j, r);
						if (null != dep) r.setDeps(j, dep, new int[]{i}, new double[]{1.0}, 1);

						//System.out.println("j: " + j + " (ZERO)  " + vector.computeLength() + "  " + vector.length());

						//correct for point overtaking the not-close-enough point ahead in terms of 'delta_p' as it is represented in MAX_DISTANCE, but overtaken by the 'delta' used for subsampling:
						if (dist1 <= delta) {
							//look for a point ahead that is over distance delta from the previous j, so that it will lay ahead of the current j
							for (u=i; u<this.length; u++) {
								dist2 = Math.sqrt(Math.pow(x[u] - r.x(j-1), 2)
										  + Math.pow(y[u] - r.y(j-1), 2)
										  + Math.pow(z[u] - r.z(j-1), 2));
								if (dist2 > delta) {
									prev_i = i;
									i = u;
									break;
								}
							}
						}
					} else {
						// Compose a point ahead out of the found ones.
						//
						// First, adjust weights for the points ahead
						w[0] = distances[0] / MAX_DISTANCE;
						double largest = w[0];
						for (u=1; u<next_ahead; u++) {
							w[u] = 1 - (distances[u] / MAX_DISTANCE);
							if (w[u] > largest) {
								largest = w[u];
							}
						}
						// normalize weights: divide by largest
						sum = 0;
						for (u=0; u<next_ahead; u++) {
							w[u] = w[u] / largest;
							sum += w[u];
						}
						// correct error. The closest point gets the extra
						if (sum < 1.0) {
							w[0] += 1.0 - sum;
						} else {
							recalculate(w, next_ahead, sum);
						}
						// Now, make a vector for each point with the corresponding weight
						vector.set(0, 0, 0);
						for (u=0; u<next_ahead; u++) {
							iu = i + u;
							if (iu >= this.length) iu -= this.length;
							ve[u].set(x[iu] - r.x(j-1), y[iu] - r.y(j-1), z[iu] - r.z(j-1));
							ve[u].setLength(w[u] * delta);
							vector.add(ve[u], u == next_ahead-1); // compute the length only on the last iteration
						}
						// correct potential errors
						if (Math.abs(vector.length() - delta) > 0.00000001) {
							vector.setLength(delta);
						}
						// set
						vector.put(j, r);
						if (null != dep) r.setDeps(j, dep, ahead, w, next_ahead);

						//System.out.println("j: " + j + "  (" + next_ahead + ")   " + vector.computeLength() + "  " + vector.length());


						// find the first point that is right ahead of the newly added point
						// so: loop through points that lay within MAX_DISTANCE, and find the first one that is right past delta.
						ii = i;
						for (k=0; k<next_ahead; k++) {
							if (distances[k] > delta) {
								ii = ahead[k];
								break;
							}
						}
						// correct for the case of unseen point (because all MAX_POINTS ahead lay under delta):
						prev_i = i;
						if (i == ii) {
							i = ahead[next_ahead-1] +1; //the one after the last.
							if (i >= this.length) {
								if (isClosed()) i = i - this.length; // this.length is the length of the x,y,z, the original points
								else i = this.length -1;
							}
						} else {
							i = ii;
						}
					}
					//advance index in the new points
					j += 1;
				} // end of for loop

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Some data: x,y,z .length = " + x.length + "," + y.length + "," + z.length
					   + "\nj=" + j + ", i=" + i + ", prev_i=" + prev_i
					);
			}


			dist_ahead = r.distance(j-1, x[this.length-1], y[this.length-1], z[this.length-1]);

			//System.out.println("delta: " + delta + "\nlast point: " + x[x.length-1] + ", " + y[y.length-1] + ", " + z[z.length-1]);
			//System.out.println("last resampled point: x,y,z " + r.x(j-1) + ", " + r.y(j-1) + ", " + r.z(j-1));
			//System.out.println("distance: " + dist_ahead);

			// see whether the subsampling terminated too early, and fill with a line of points.
			final int last_i = isClosed() ? 0 : this.length -1;
			if (dist_ahead > delta*1.2) {
				//TODO//System.out.println("resampling terminated too early. Why?");
				while (dist_ahead > delta*1.2) {
					// make a vector from the last resampled point to the last point
					vector.set(x[last_i] - r.x(j-1), y[last_i] - r.y(j-1), z[last_i] - r.z(j-1));
					// resize it to length delta
					vector.setLength(delta);
					vector.put(j, r);
					j++;
					dist_ahead = r.distance(j-1, x[last_i], y[last_i], z[last_i]);
				}
			}
			// done!
			r.put(this, j); // j acts as length of resampled points and vectors
			// vector at zero is left as 0,0 which makes no sense. Should be the last point that has no vector, or has it only in the event that the list of points is declared as closed: a vector to the first point. Doesn't really matter though, as long as it's clear: as of right now, the first point has no vector unless the path is closed, in which case it contains the vector from the last-to-first.
		}
	}

	static public double[][][] makeTube(double[] px, double[] py, double[] pz, double[] p_width_i, final int resample, final int parallels) {

		int n = px.length;

		// Resampling to get a smoother pipe
		try {
			VectorString3D vs = new VectorString3D(px, py, pz);
			vs.addDependent(p_width_i);
			vs.resample(vs.getAverageDelta() * resample);
			px = vs.getPoints(0);
			py = vs.getPoints(1);
			pz = vs.getPoints(2);
			p_width_i = vs.getDependent(0);
			//Utils.log("lengths:  " + px.length + ", " + py.length + ", " + pz.length + ", " + p_width_i.length);
			n = vs.length();
		} catch (Exception e) {
			IJ.error(""+e);
		}

		double[][][] all_points = new double[n+2][parallels+1][3];
		int extra = 1; // this was zero when not doing capping
		for (int cap=0; cap<parallels+1; cap++) {
			all_points[0][cap][0] = px[0];//p_i[0][0]; //x
			all_points[0][cap][1] = py[0]; //p_i[1][0]; //y
			all_points[0][cap][2] = pz[0]; //z_values[0];
			all_points[all_points.length-1][cap][0] = px[n-1]; //p_i[0][p_i[0].length-1];
			all_points[all_points.length-1][cap][1] = py[n-1]; //p_i[1][p_i[0].length-1];
			all_points[all_points.length-1][cap][2] = pz[n-1]; //z_values[z_values.length-1];
		}
		double angle = 2*Math.PI/parallels; //Math.toRadians(30);

		Vector3 v3_P12;
		Vector3 v3_PR;
		Vector3[] circle = new Vector3[parallels+1];
		double sinn, coss;
		int half_parallels = parallels/2;
		for (int i=0; i<n-1; i++) {
			//System.out.println(i + " : " + px[i] + ", " + py[i] + ", " + pz[i]);
			//First vector: from one realpoint to the next
			//v3_P12 = new Vector3(p_i[0][i+1] - p_i[0][i], p_i[1][i+1] - p_i[1][i], z_values[i+1] - z_values[i]);
			v3_P12 = new Vector3(px[i+1] - px[i], py[i+1] - py[i], pz[i+1] - pz[i]);

			//Second vector: ortogonal to v3_P12, made by cross product between v3_P12 and a modifies v3_P12 (either y or z set to 0)

			//checking that v3_P12 has not z set to 0, in which case it woundn´t be different and then the cross product not give an ortogonal vector as output

			//chosen random vector: the same vector, but with x = 0;
			/* matrix:
				1 1 1		1 1 1				1 1 1				1 1 1
				v1 v2 v3	P12[0] P12[1] P12[2]		P12[0] P12[1] P12[2]		P12[0] P12[1] P12[2]
				w1 w2 w3	P12[0]+1 P12[1] P12[2]		P12[0]+1 P12[1]+1 P12[2]+1	P12[0] P12[1] P12[2]+1

			   cross product: v ^ w = (v2*w3 - w2*v3, v3*w1 - v1*w3, v1*w2 - w1*v2);

			   cross product of second: v ^ w = (b*(c+1) - c*(b+1), c*(a+1) - a*(c+1) , a*(b+1) - b*(a+1))
							  = ( b - c           , c - a             , a - b            )

			   cross product of third: v ^ w = (b*(c+1) - b*c, c*a - a*(c+1), a*b - b*a)
							   (b		 ,-a            , 0);
							   (v3_P12.y	 ,-v3_P12.x     , 0);


			Reasons why I use the third:
				-Using the first one modifies the x coord, so it generates a plane the ortogonal of which will be a few degrees different when z != 0 and when z =0,
				 thus responsible for soft shiftings at joints where z values change
				-Adding 1 to the z value will produce the same plane whatever the z value, thus avoiding soft shiftings at joints where z values are different
				-Then, the third allows for very fine control of the direction that the ortogonal vector takes: simply manipulating the x coord of v3_PR, voilà.

			*/

			// BELOW if-else statements needed to correct the orientation of vectors, so there's no discontinuity
			if (v3_P12.y < 0) {
				v3_PR = new Vector3(v3_P12.y, -v3_P12.x, 0);
				v3_PR = v3_PR.normalize(v3_PR);
				v3_PR = v3_PR.scale(p_width_i[i], v3_PR);

				//vectors are perfectly normalized and scaled
				//The problem then must be that they are not properly ortogonal and so appear to have a smaller width.
				//   -not only not ortogonal but actually messed up in some way, i.e. bad coords.

				circle[half_parallels] = v3_PR;
				for (int q=half_parallels+1; q<parallels+1; q++) {
					sinn = Math.sin(angle*(q-half_parallels));
					coss = Math.cos(angle*(q-half_parallels));
					circle[q] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
				circle[0] = circle[parallels];
				for (int qq=1; qq<half_parallels; qq++) {
					sinn = Math.sin(angle*(qq+half_parallels));
					coss = Math.cos(angle*(qq+half_parallels));
					circle[qq] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
			} else {
				v3_PR = new Vector3(-v3_P12.y, v3_P12.x, 0);           //thining problems disappear when both types of y coord are equal, but then shifting appears
				/*
				Observations:
					-if y coord shifted, then no thinnings but yes shiftings
					-if x coord shifted, THEN PERFECT
					-if both shifted, then both thinnings and shiftings
					-if none shifted, then no shiftings but yes thinnings
				*/

				v3_PR = v3_PR.normalize(v3_PR);
				if (null == v3_PR) {
					System.out.println("vp_3r is null: most likely a point was repeated in the list, and thus the vector has length zero.");
				}
				v3_PR = v3_PR.scale(
						p_width_i[i],
						v3_PR);

				circle[0] = v3_PR;
				for (int q=1; q<parallels; q++) {
					sinn = Math.sin(angle*q);
					coss = Math.cos(angle*q);
					circle[q] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
				circle[parallels] = v3_PR;
			}
			// Adding points to main array
			for (int j=0; j<parallels+1; j++) {
				all_points[i+extra][j][0] = /*p_i[0][i]*/ px[i] + circle[j].x;
				all_points[i+extra][j][1] = /*p_i[1][i]*/ py[i] + circle[j].y;
				all_points[i+extra][j][2] = /*z_values[i]*/ pz[i] + circle[j].z;
			}
		}
		for (int k=0; k<parallels+1; k++) {
			all_points[n-1+extra][k][0] = /*p_i[0][n-1]*/ px[n-1] + circle[k].x;
			all_points[n-1+extra][k][1] = /*p_i[1][n-1]*/ py[n-1] + circle[k].y;
			all_points[n-1+extra][k][2] = /*z_values[n-1]*/ pz[n-1] + circle[k].z;
		}
		return all_points;
	}

	/** Accepts an arrays as that returned from methods generateJoints and makeTube: first dimension is the list of points, second dimension is the number of vertices defining the circular cross section of the tube, and third dimension is the x,y,z of each vertex. */
	static public List generateTriangles(final double[][][] all_points, final double scale) {
		int n = all_points.length;
		final int parallels = all_points[0].length -1;
		List list = new ArrayList();
		for (int i=0; i<n-1; i++) { //minus one since last is made with previous
			for (int j=0; j<parallels; j++) { //there are 12+12 triangles for each joint //it's up to 12+1 because first point is repeated at the end
				// first triangle in the quad
				list.add(new Point3f((float)(all_points[i][j][0] * scale), (float)(all_points[i][j][1] * scale), (float)(all_points[i][j][2] * scale)));
				list.add(new Point3f((float)(all_points[i][j+1][0] * scale), (float)(all_points[i][j+1][1] * scale), (float)(all_points[i][j+1][2] * scale)));
				list.add(new Point3f((float)(all_points[i+1][j][0] * scale), (float)(all_points[i+1][j][1] * scale), (float)(all_points[i+1][j][2] * scale)));

				// second triangle in the quad
				list.add(new Point3f((float)(all_points[i+1][j][0] * scale), (float)(all_points[i+1][j][1] * scale), (float)(all_points[i+1][j][2] * scale)));
				list.add(new Point3f((float)(all_points[i][j+1][0] * scale), (float)(all_points[i][j+1][1] * scale), (float)(all_points[i][j+1][2] * scale)));
				list.add(new Point3f((float)(all_points[i+1][j+1][0] * scale), (float)(all_points[i+1][j+1][1] * scale), (float)(all_points[i+1][j+1][2] * scale)));
			}
		}
		return list;
	}

	/** From my former program, A_3D_Editing.java and Pipe.java */
	static private Vector3 rotate_v_around_axis(final Vector3 v, final Vector3 axis, final double sin, final double cos) {

		final Vector3 result = new Vector3();
		final Vector3 r = axis.normalize(axis);

		result.set((cos + (1-cos) * r.x * r.x) * v.x + ((1-cos) * r.x * r.y - r.z * sin) * v.y + ((1-cos) * r.x * r.z + r.y * sin) * v.z,
			   ((1-cos) * r.x * r.y + r.z * sin) * v.x + (cos + (1-cos) * r.y * r.y) * v.y + ((1-cos) * r.y * r.z - r.x * sin) * v.z,
			   ((1-cos) * r.y * r.z - r.y * sin) * v.x + ((1-cos) * r.y * r.z + r.x * sin) * v.y + (cos + (1-cos) * r.z * r.z) * v.z);

		/*
		result.x += (cos + (1-cos) * r.x * r.x) * v.x;
		result.x += ((1-cos) * r.x * r.y - r.z * sin) * v.y;
		result.x += ((1-cos) * r.x * r.z + r.y * sin) * v.z;

		result.y += ((1-cos) * r.x * r.y + r.z * sin) * v.x;
		result.y += (cos + (1-cos) * r.y * r.y) * v.y;
		result.y += ((1-cos) * r.y * r.z - r.x * sin) * v.z;

		result.z += ((1-cos) * r.y * r.z - r.y * sin) * v.x;
		result.z += ((1-cos) * r.y * r.z + r.x * sin) * v.y;
		result.z += (cos + (1-cos) * r.z * r.z) * v.z;
		*/
		return result;
	}

}

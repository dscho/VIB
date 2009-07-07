/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
 * Example plugin on how to add spheres and tubes to the 3D Viewer.
 * Albert Cardona 2008-12-09
 * Released under the General Public License, latest version.
 */

package ij3d;

import ij.IJ;
import ij.WindowManager;
import ij.plugin.PlugIn;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.media.j3d.Transform3D;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import ij3d.Image3DUniverse;

public class Mesh_Maker implements PlugIn {

	public static void main(String[] args) {
		ij.ImageJ ij = new ij.ImageJ();
		IJ.runPlugIn("ij3d.Mesh_Maker", "");
		Image3DUniverse univ =  (Image3DUniverse) ((ImageWindow3D)WindowManager
					.getCurrentWindow()).getUniverse();
		System.out.println("bla");
	}

	public void run(String arg) {
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show();

		// define two spheres: an X,Y,Z point with a radius
		double x1 = 10,
		       y1 = 100,
		       z1 = 150,
		       r1 = 20;
		Color3f color1 = new Color3f(Color.pink);

		double x2 = 50,
		       y2 = 200,
		       z2 = 40,
		       r2 = 35;
		Color3f color2 = new Color3f(Color.white);

		// define a tube as a polyline in space
		double[] px = new double[]{100, 230, 320, 400};
		double[] py = new double[]{100, 120, 230, 400};
		double[] pz = new double[]{100, 200, 300, 400};
		double[] pr = new double[]{ 10,  15,  40,  110};
		Color3f colort = new Color3f(Color.yellow);

		//define a second tube as a curving spiral in space
		double[] px2 = new double[200];
		double[] py2 = new double[200];
		double[] pz2 = new double[200];
		double[] pr2 = new double[200];
		Color3f color_t2 = new Color3f(Color.magenta);
		for (int i=0; i<px2.length; i++) {
			double angle = Math.toRadians(10 * i);
			double radius = 50 + i*5;
			px2[i] = Math.cos(angle) * radius;
			py2[i] = Math.sin(angle) * radius;
			pz2[i] = i * 5;
			pr2[i] = 10;
		}

		// Add both spheres and the tubes
		//     Last parameter is the resampling (1 means no resampling)

		Content sph1 = univ.addMesh(createSphere(x1, y1, z1, r1, 12, 12), color1, "Sphere 1", 1);
		Content sph2 = univ.addMesh(createSphere(x2, y2, z2, r2, 12, 12), color2, "Sphere 2", 1);
		Content tube1 = univ.addMesh(createTube(px, py, pz, pr, 12, false), colort, "Tube", 1);
		Content tube2 = univ.addMesh(createTube(px2, py2, pz2, pr2, 12, false), color_t2, "Tube spiral", 1);
		Content disc1 = univ.addMesh(createDisc(100,100,50,3,3,3,50,12), new Color3f(Color.blue), "Disc 1", 1);

		// Extra:
		// Now modify some attributes:

		// Lock the transformation of the spiral:
		// so it can't be rotated relative to its center when selected
		// Try it: when selecting the spiral and rotating it with the mouse,
		// the whole view will rotate, not the spiral relative to itself.
		tube2.toggleLock();

		// Change the transparency of the first tube
		tube1.setTransparency(0.5f); // from 0 (opaque) to 1 (fully transparent)

		disc1.setTransparency(0.3f);

		// Move the small sphere in x,y,z to a new location
		sph1.applyTransform(new Transform3D(new double[]{1, 0, 0,  50,
			                                         0, 1, 0, 100,
			                                         0, 0, 1, -50,
			                                         0, 0, 0,   1 }));
	}

	static public List createSphere(final double x, final double y, final double z, final double r) {
		return createSphere(x, y, z, r, 12, 12);
	}

	static public List createSphere(final double x, final double y, final double z,
			                final double r, final int meridians, final int parallels) {
		final double[][][] globe = generateGlobe(meridians, parallels);
		// Scale by radius 'r', and traslate to x,y,z
		for (int j=0; j<globe.length; j++) {
			for (int k=0; k<globe[0].length; k++) {
				globe[j][k][0] = globe[j][k][0] * r + x;
				globe[j][k][1] = globe[j][k][1] * r + y;
				globe[j][k][2] = globe[j][k][2] * r + z;
			}
		}
		// create triangular faces and add them to the list
		final ArrayList list = new ArrayList();
		for (int j=0; j<globe.length-1; j++) { // the parallels
			for (int k=0; k<globe[0].length -1; k++) { // meridian points
				if(j != globe.length-2) {
					// half quadrant (a triangle)
					list.add(new Point3f((float)globe[j+1][k+1][0], (float)globe[j+1][k+1][1], (float)globe[j+1][k+1][2]));
					list.add(new Point3f((float)globe[j][k][0], (float)globe[j][k][1], (float)globe[j][k][2]));
					list.add(new Point3f((float)globe[j+1][k][0], (float)globe[j+1][k][1], (float)globe[j+1][k][2]));
				}
				if(j != 0) {
					// the other half quadrant
					list.add(new Point3f((float)globe[j][k][0], (float)globe[j][k][1], (float)globe[j][k][2]));
					list.add(new Point3f((float)globe[j+1][k+1][0], (float)globe[j+1][k+1][1], (float)globe[j+1][k+1][2]));
					list.add(new Point3f((float)globe[j][k+1][0], (float)globe[j][k+1][1], (float)globe[j][k+1][2]));
				}
			}
		}
		return list;
	}

	/** Generate a globe of radius 1.0 that can be used for any Ball. First dimension is Z, then comes a double array x,y. Minimal accepted meridians and parallels is 3.*/
	static public double[][][] generateGlobe(int meridians, int parallels) {
		if (meridians < 3) meridians = 3;
		if (parallels < 3) parallels = 3;
		/* to do: 2 loops:
		-first loop makes horizontal circle using meridian points.
		-second loop scales it appropiately and makes parallels.
		Both loops are common for all balls and so should be done just once.
		Then this globe can be properly translocated and resized for each ball.
		*/
		// a circle of radius 1
		double angle_increase = 2*Math.PI / meridians;
		double temp_angle = 0;
		final double[][] xy_points = new double[meridians+1][2];    //plus 1 to repeat last point
		xy_points[0][0] = 1;     // first point
		xy_points[0][1] = 0;
		for (int m=1; m<meridians; m++) {
			temp_angle = angle_increase*m;
			xy_points[m][0] = Math.cos(temp_angle);
			xy_points[m][1] = Math.sin(temp_angle);
		}
		xy_points[xy_points.length-1][0] = 1; // last point
		xy_points[xy_points.length-1][1] = 0;

		// Build parallels from circle
		angle_increase = Math.PI / parallels;   // = 180 / parallels in radians
		final double angle90 = Math.toRadians(90);
		final double[][][] xyz = new double[parallels+1][xy_points.length][3];
		for (int p=1; p<xyz.length-1; p++) {
			double radius = Math.sin(angle_increase*p);
			double Z = Math.cos(angle_increase*p);
			for (int mm=0; mm<xyz[0].length-1; mm++) {
				//scaling circle to apropiate radius, and positioning the Z
				xyz[p][mm][0] = xy_points[mm][0] * radius;
				xyz[p][mm][1] = xy_points[mm][1] * radius;
				xyz[p][mm][2] = Z;
			}
			xyz[p][xyz[0].length-1][0] = xyz[p][0][0];  //last one equals first one
			xyz[p][xyz[0].length-1][1] = xyz[p][0][1];
			xyz[p][xyz[0].length-1][2] = xyz[p][0][2];
		}

		// south and north poles
		for (int ns=0; ns<xyz[0].length; ns++) {
			xyz[0][ns][0] = 0;	//south pole
			xyz[0][ns][1] = 0;
			xyz[0][ns][2] = 1;
			xyz[xyz.length-1][ns][0] = 0;    //north pole
			xyz[xyz.length-1][ns][1] = 0;
			xyz[xyz.length-1][ns][2] = -1;
		}

		return xyz;
	}

	static public List createTube(final double[] x, final double[] y, final double[] z,
			              final double[] r, final int parallels, final boolean do_resample) {
		return Pipe.generateTriangles(Pipe.makeTube(x, y, z, r, 1, parallels, do_resample), 1);
	}

	static public List createDisc(double x, double y, double z,
				      double nx, double ny, double nz,
				      double radius,
				      int edgePoints ) {
		double ax, ay, az;

		if( Math.abs(nx) >= Math.abs(ny) ) {
			double scale = 1 / Math.sqrt( nx*nx + nz*nz  );
			ax = -nz * scale;
			ay = 0;
			az = nx * scale;
		} else {
			double scale = 1 / Math.sqrt( ny*ny + nz*nz  );
			ax = 0;
			ay = nz * scale;
			az = - ny * scale;
		}

		/* Now to find the other vector in that plane, do the
		 * cross product of (ax,ay,az) with (nx,ny,nz) */

		double bx = (ay * nz - az * ny);
		double by = (az * nx - ax * nz);
		double bz = (ax * ny - ay * nx);
		double bScale = 1 / Math.sqrt( bx*bx + by*by + bz*bz );
		bx *= bScale;
		by *= bScale;
		bz *= bScale;

		double [] circleX = new double[edgePoints+1];
		double [] circleY = new double[edgePoints+1];
		double [] circleZ = new double[edgePoints+1];

		for( int i = 0; i < edgePoints + 1; ++i ) {
			double angle = (i * 2 * Math.PI) / edgePoints;
			double c = Math.cos(angle);
			double s = Math.sin(angle);
			circleX[i] = x + radius * c * ax + radius * s * bx;
			circleY[i] = y + radius * c * ay + radius * s * by;
			circleZ[i] = z + radius * c * az + radius * s * bz;
		}
		final ArrayList list = new ArrayList();
		Point3f centre = new Point3f( (float)x, (float)y, (float)z );
		for( int i = 0; i < edgePoints; ++i ) {
			Point3f t2 = new Point3f( (float)circleX[i], (float)circleY[i], (float)circleZ[i] );
			Point3f t3 = new Point3f( (float)circleX[i+1], (float)circleY[i+1], (float)circleZ[i+1] );
			list.add( centre );
			list.add( t2 );
			list.add( t3 );
			list.add( centre );
			list.add( t3 );
			list.add( t2 );
		}
		return list;
	}
}

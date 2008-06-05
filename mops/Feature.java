package mops;

import process3d.Smooth_;
import vib.InterpolatedImage;
import vib.FastMatrix;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import math3d.Point3d;

import ij.measure.Calibration;

public class Feature extends Point3d
{
	/*
	 * Feature descriptor width. For simplicity, assume a 
	 * cubic descriptor for the moment.
	 */
	public static final int FD_WIDTH = 16;

	/*
	 * three-dimensional patch for at the position of this feature
	 */
	private InterpolatedImage patch;

	/*
	 * rotation matrix which aligns the patch in a way that
	 * the steepest gradient is in x-direction and the
	 * 2nd steepest is in y direction.
	 */
	private FastMatrix orientation = null;
	
	/**
	 * scale = sigma of the feature relative to the octave
	 */
	private double scale;

	Feature( double x, double y, double z, double scale )
	{
		super( x, y, z );
		this.scale = scale;
	}
	
	/*
	 * Sum up the gaussian weighted derivative in a sigma-dependent
	 * environment around the position of this feature. The result
	 * is normalized and stored in <code>orientation</code>.
	 * 
	 * @param smoothed is expected to be an image with sigma = 4.5 * {@link #sigma}
	 *   detected
	 */
	public void extractOrientation(InterpolatedImage smoothed) {
		Calibration cal = smoothed.getImage().getCalibration();
		int ix = (int)(x / cal.pixelWidth);
		int iy = (int)(y / cal.pixelHeight);
		int iz = (int)(z / cal.pixelDepth);
		
		float v2 = 2 * smoothed.getNoInterpolFloat( ix, iy, iz );
		
		
		double[][] h = new double[ 3 ][ 3 ];
		
		h[ 0 ][ 0 ] =
			smoothed.getNoInterpolFloat( ix + 1, iy, iz ) -
			v2 +
			smoothed.getNoInterpolFloat( ix - 1, iy, iz );
		h[ 1 ][ 1 ] =
			smoothed.getNoInterpolFloat( ix, iy + 1, iz ) -
			v2 +
			smoothed.getNoInterpolFloat( ix, iy - 1, iz );
		h[ 2 ][ 2 ] =
			smoothed.getNoInterpolFloat( ix, iy, iz + 1 ) -
			v2 +
			smoothed.getNoInterpolFloat( ix, iy, iz - 1 );
		
		h[ 0 ][ 1 ] = h[ 1 ][ 0 ] =
			( smoothed.getNoInterpolFloat( ix + 1, iy + 1, iz ) -
			  smoothed.getNoInterpolFloat( ix - 1, iy + 1, iz ) ) / 4 -
			( smoothed.getNoInterpolFloat( ix + 1, iy - 1, iz ) -
			  smoothed.getNoInterpolFloat( ix - 1, iy - 1, iz ) ) / 4;
		h[ 0 ][ 2 ] = h[ 2 ][ 0 ] =
			( smoothed.getNoInterpolFloat( ix + 1, iy, iz + 1 ) -
			  smoothed.getNoInterpolFloat( ix - 1, iy, iz + 1 ) ) / 4 -
			( smoothed.getNoInterpolFloat( ix + 1, iy, iz - 1 ) -
			  smoothed.getNoInterpolFloat( ix - 1, iy, iz - 1 ) ) / 4;
		h[ 1 ][ 2 ] = h[ 2 ][ 1 ] =
			( smoothed.getNoInterpolFloat( ix, iy + 1, iz + 1 ) -
			  smoothed.getNoInterpolFloat( ix, iy - 1, iz + 1 ) ) / 4 -
			( smoothed.getNoInterpolFloat( ix, iy + 1, iz - 1 ) -
			  smoothed.getNoInterpolFloat( ix, iy - 1, iz - 1 ) ) / 4;
		
		EigenvalueDecomposition evd =
			new EigenvalueDecomposition( new Matrix( h ) );
		
		double[] ev = evd.getRealEigenvalues();
		double[][] evect = evd.getV().getArray();
		
		
		// Sort the eigenvalues by ascending size.
		int i0 = 0;
		int i1 = 1;
		int i2 = 2;
		
		ev[ 0 ] = Math.abs( ev[ 0 ] );
		ev[ 1 ] = Math.abs( ev[ 1 ] );
		ev[ 2 ] = Math.abs( ev[ 2 ] );
		
		if ( ev[ i1 ] < ev[ i0 ] )
		{
			int temp = i0;
			i0 = i1;
			i1 = temp;
		}
		if ( ev[ i2 ] < ev[ i1 ] )
		{
			int temp = i1;
			i1 = i2;
			i2 = temp;
			if ( ev[ i1 ] < ev[ i0 ] )
			{
				temp = i0;
				i0 = i1;
				i1 = temp;
			}
		}
		
		
		
		double[][] sortedEigenvect = new double[ 3 ][ 3 ];
		
		int s = 0;
		int l = 0;
		
		ev[ 0 ] = Math.abs(  ev[ 0 ] );
		for ( int i = 1; i < ev.length; ++i )
		{
			ev[ i ] = Math.abs( ev[ i ] );
			if ( ev[ i ] < ev[ s ] ) s = i;
			if ( ev[ i ] > ev[ l ] ) l = i;
		}
		
		
			
	
		float[] gauss_k = create3DGaussianKernel(sigma);
		int diam = (int)Math.ceil(5 * sigma);
		if(diam % 2 == 0) diam++;
		int r = diam / 2;
		// TODO does this work if x-r < 0 etc ???
		InterpolatedImage.Iterator it = 
			smoothed.iterator(false, ix-r, iy-r, iz-r, ix+r, iy+r, iz+r);
		// sum up the gradients weighted by the gaussian
		// This is the steepest gradient direction
		float[] o = new float[3];
		for(int i = 0; i < gauss_k.length; i++) {
			it.next();
			float v = smoothed.getNoInterpolFloat(it.i, it.j, it.k);
			float v1 = smoothed.getNoInterpolFloat(it.i-1, it.j, it.k);
			float v2 = smoothed.getNoInterpolFloat(it.i, it.j-1, it.k);
			float v3 = smoothed.getNoInterpolFloat(it.i, it.j, it.k-1);
			float l = (float)Math.sqrt(
				(v-v1)*(v-v1) + (v-v2)*(v-v2) + (v-v3)*(v-v3));
			float w = gauss_k[i] * l;
			o[0] += w * it.i;
			o[1] += w * it.j;
			o[2] += w * it.k;
		}
		normalize(o);
		// find two vectors perpendicular to o (and to each other)
		float[] o1 = new float[] {o[1], -o[0], 0};
		// avoid the null vector
		if(o[0] == 0 && o[1] == 0)
			o1[0] = 0; o1[1] = -o[2]; o1[2] = o[1];
		normalize(o1);
		float[] o2 = new float[] {o[1]*o1[2] - o[2]*o1[1],
			o[2]*o1[0] - o[0]*o1[2], o[0]*o1[1] - o[1]*o1[0]};
		normalize(o2);

		gauss_k = create3DGaussianKernel(sigma);
		for(int ny = -r; ny <= +r; ny++) {
			for(int nx = -r; nx <= +r; nx++) {
// 				w = gaussk
			}
		}
	}

	private void normalize(float[] v) {
		float c = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		for(int i = 0; i < v.length; i++)
			v[i] /= c;
	}


	/*
	 * Illustration of the 2D case, which transfers to the 3D case:
	 *
	 *    patch after                     patch before
	 *    rotation:                       rotation
	 *
	 *   +----------+        M^-1         +---..
	 *   |          |    ------------>   /      ---..
	 *   |     +----|    <------------  /            -+
	 *   |          |         M        /      +..     /
	 *   +----------+                 +---..     --. /
	 *                                      ---..   /
	 *                                           -+
	 *  
	 *  One wants to get the coordinates (and values) of the patch
	 *  before rotation - those after the rotation are known, since
	 *  mid point and edge length are known.
	 *
	 *  So each point coordinate - as is after rotation - is transformed
	 *  by M^-1 (M being a rotational matrix).
	 *
	 *  The angle between the axes can be obtained by 
	 *  the rotation matrix M by the axis-angle definition.
	 */
	public void extractPatch(InterpolatedImage ii) {}

	public double featureDistance(Feature other) {
		return -1;
	}

	/*
	 * creates a 3D gaussian kernel in a 1D float array where
	 * z is the index changing slowest, then y and then x.
	 * kernel-diameter >= 5 * sigma
	 * The returned kernel is normalized.
	 */
	public float[] create3DGaussianKernel(float sigma) {
		// radius should at least be 2.5 * sigma
		int d = (int)Math.ceil(5 * sigma);
		d = d % 2 == 0 ? d + 1 : d;
		int r = d / 2;
		int wh = d * d;
		float[] kernel = new float[d * d * d];
		float sum = 0;
		float sigma_2 = sigma * sigma;
		for(int i = 0; i < kernel.length; i++) {
			int z = i / wh - r;
			int y = (i % wh) / d - r;
			int x = (i % wh) % d - r;
			float n = (float)Math.sqrt(x*x + y*y + z*z);
			kernel[i] = (float)Math.exp(-n*n / (2*sigma_2));
			sum += kernel[i];
		}
		// normalize
		for(int i = 0; i < kernel.length; i++)
			kernel[i] /= sum;

		return kernel;
	}

	public float[] create2DGaussianKernel(float sigma) {
		// radius should at least be 2.5 * sigma
		int d = (int)Math.ceil(5 * sigma);
		d = d % 2 == 0 ? d + 1 : d;
		int r = d / 2;
		float[] kernel = new float[d * d];
		float sum = 0;
		float sigma_2 = sigma * sigma;
		for(int i = 0; i < kernel.length; i++) {
			int y = i / d - r;
			int x = i % d - r;
			float n = (float)Math.sqrt(x*x + y*y);
			kernel[i] = (float)Math.exp(-n*n / (2*sigma_2));
			sum += kernel[i];
		}
		// normalize
		for(int i = 0; i < kernel.length; i++)
			kernel[i] /= sum;

		return kernel;
	}
}


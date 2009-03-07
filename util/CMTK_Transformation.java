/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.measure.Calibration;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import util.BatchOpener;
import landmarks.NamedPointSet;
import math3d.Point3d;
import vib.FastMatrix;
import vib.TransformedImage;
import vib.Resample_;
import distance.Correlation;
import distance.MutualInformation;

import landmarks.Affine_From_Landmarks;
import landmarks.Bookstein_From_Landmarks;
import landmarks.Rigid_From_Landmarks;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

import util.FileCreation;

import util.Overlay_Registered;

public class CMTK_Transformation {

	public CMTK_Transformation( ) { }

	int dimsx = -1, dimsy = -1, dimsz = -1;
	double domainx = Double.MIN_VALUE, domainy = Double.MIN_VALUE, domainz = Double.MIN_VALUE;
	double originx = Double.MIN_VALUE, originy = Double.MIN_VALUE, originz = Double.MIN_VALUE;
	double coeffs[][];

	double deltax = -1, deltay = -1, deltaz = -1;

	protected CMTK_Transformation( int dimsx, int dimsy, int dimsz,
				       double domainx, double domainy, double domainz,
				       double originx, double originy, double originz,
				       double coeffs[][] ) {
		this.dimsx = dimsx;
		this.dimsy = dimsy;
		this.dimsz = dimsz;
		this.domainx = domainx;
		this.domainy = domainy;
		this.domainz = domainz;
		this.originx = originx;
		this.originy = originy;
		this.originz = originz;
		this.coeffs = coeffs;

		this.deltax = domainx / (dimsx - 3);
		this.deltay = domainy / (dimsy - 3);
		this.deltaz = domainz / (dimsz - 3);
	}

	public static double degToRad( double d ) {
		return (d * Math.PI) / 180;
	}


	public byte trilinearInterpolate( double image_x, double image_y, double image_z, int width, int height, int depth, byte [][] v ) {

		double x_d = image_x - Math.floor(image_x);
		double y_d = image_y - Math.floor(image_y);
		double z_d = image_z - Math.floor(image_z);

		int x_f = (int)Math.floor(image_x); int x_c = (int)Math.ceil(image_x);
		int y_f = (int)Math.floor(image_y); int y_c = (int)Math.ceil(image_y);
		int z_f = (int)Math.floor(image_z); int z_c = (int)Math.ceil(image_z);

		/* Check that these values aren't
		   poking off the edge of the screen -
		   if so then make them zero. */

		double fff;
		double cff;
		double fcf;
		double ccf;

		double ffc;
		double cfc;
		double fcc;
		double ccc;

		if( (x_f < 0) || (x_c < 0) || (y_f < 0) || (y_c < 0) || (z_f < 0) || (z_c < 0) ||
		    (x_f >= width) || (x_c >= width) || (y_f >= height) || (y_c >= height) || (z_f >= depth) || (z_c >= depth) ) {

			fff = 0;
			cff = 0;
			fcf = 0;
			ccf = 0;
			ffc = 0;
			cfc = 0;
			fcc = 0;
			ccc = 0;

		} else {

			fff = v[z_f][width*y_f+x_f]&0xFF;
			cff = v[z_c][width*y_f+x_f]&0xFF;

			fcf = v[z_f][width*y_c+x_f]&0xFF;
			ccf = v[z_c][width*y_c+x_f]&0xFF;

			ffc = v[z_f][width*y_f+x_c]&0xFF;
			cfc = v[z_c][width*y_f+x_c]&0xFF;

			fcc = v[z_f][width*y_c+x_c]&0xFF;
			ccc = v[z_c][width*y_c+x_c]&0xFF;
		}

		// Now we should be OK to do the interpolation for real:

		double i1 = (1 - z_d) * (fff) + (cff) * z_d;
		double i2 = (1 - z_d) * (fcf) + (ccf) * z_d;

		double j1 = (1 - z_d) * (ffc) + (cfc) * z_d;
		double j2 = (1 - z_d) * (fcc) + (ccc) * z_d;

		double w1 = i1 * (1 - y_d) + i2 * y_d;
		double w2 = j1 * (1 - y_d) + j2 * y_d;

		double value_f = w1 * (1 - x_d) + w2 * x_d;

		int value = (int)Math.round(value_f);
		if( (value < 0) || (value > 255) )
			throw new RuntimeException("BUG: Out of range value!");

		return (byte)value;
	}


	public ImagePlus transform( ImagePlus templateImage, ImagePlus modelImage ) {

		int modelWidth = modelImage.getWidth();
		int modelHeight = modelImage.getHeight();
		int modelDepth = modelImage.getStackSize();
		double modelXSpacing = 1;
		double modelYSpacing = 1;
		double modelZSpacing = 1;
		Calibration modelCalibration = modelImage.getCalibration();
		if( modelCalibration != null ) {
			modelXSpacing = modelCalibration.pixelWidth;
			modelYSpacing = modelCalibration.pixelHeight;
			modelZSpacing = modelCalibration.pixelDepth;
		}

		int templateWidth = templateImage.getWidth();
		int templateHeight = templateImage.getHeight();
		int templateDepth = templateImage.getStackSize();
		double templateXSpacing = 1;
		double templateYSpacing = 1;
		double templateZSpacing = 1;
		Calibration templateCalibration = templateImage.getCalibration();
		if( templateCalibration != null ) {
			templateXSpacing = templateCalibration.pixelWidth;
			templateYSpacing = templateCalibration.pixelHeight;
			templateZSpacing = templateCalibration.pixelDepth;
		}

		IJ.showProgress( 0 );

		double [] result = new double[3];
		byte [][] imageBytes = new byte[templateDepth][templateWidth*templateHeight];

		ImageStack modelStack = modelImage.getStack();
		byte [][] modelBytes = new byte[modelDepth][];
		for( int z = 0; z < modelDepth; ++z )
			modelBytes[z] = (byte[])modelStack.getPixels( z + 1 );

		for( int zi = 0; zi < templateDepth; ++zi ) {
			for( int yi = 0; yi < templateHeight; ++yi )
				for( int xi = 0; xi < templateWidth; ++xi ) {
					double x = xi * templateXSpacing;
					double y = yi * templateYSpacing;
					double z = zi * templateZSpacing;
					// System.out.println( "Mapping "+x+", "+y+", "+z );
					transformPoint( x, y, z, result );
					double xt = result[0];
					double yt = result[1];
					double zt = result[2];
					byte value = trilinearInterpolate(
						xt/modelXSpacing,
						yt/modelYSpacing,
						zt/modelZSpacing,
						modelWidth,
						modelHeight,
						modelDepth,
						modelBytes );
					imageBytes[zi][yi*templateWidth+xi] = value;
				}
			IJ.showProgress( zi / (double)( templateDepth + 1 ) );
		}
		ImageStack newStack = new ImageStack( templateWidth, templateHeight );
		for( int z = 0; z < templateDepth; ++z ) {
			ByteProcessor bp = new ByteProcessor( templateWidth, templateHeight );
			bp.setPixels( imageBytes[z] );
			newStack.addSlice( "", bp );
		}
		IJ.showProgress( 1.0 );
		ImagePlus resultImage = new ImagePlus( "Transformed", newStack );
		resultImage.setCalibration( templateCalibration );
		return resultImage;
	}

	public double bSpline( int l, double u ) {
		switch (l) {
		case 0:
			double oneMinusU = 1 - u;
			return (oneMinusU * oneMinusU * oneMinusU) / 6.0;
		case 1:
			return ( 3 * u * u * u
				 - 6 * u * u
				 + 4 ) / 6.0;
		case 2:
			return ( -3 * u * u * u
				 + 3 * u * u
				 + 3 * u
				 + 1 ) / 6.0;
		case 3:
			return ( u * u * u ) / 6.0;
		default:
			throw new RuntimeException("bSpline()'s first parameter must be one of 0, 1, 2 or 3");
		}
	}

	public void transformPoint( double x, double y, double z, double [] result ) {

		double cellxD = x / deltax;
		double cellyD = y / deltay;
		double cellzD = z / deltaz;

		int uncappedgridi = (int)cellxD;
		int uncappedgridj = (int)cellyD;
		int uncappedgridk = (int)cellzD;

		int gridi = Math.min( uncappedgridi, dimsx - 4 );
		int gridj = Math.min( uncappedgridj, dimsy - 4 );
		int gridk = Math.min( uncappedgridk, dimsz - 4 );

		double u = cellxD - uncappedgridi;
		double v = cellyD - uncappedgridj;
		double w = cellzD - uncappedgridk;

		result[0] = 0;
		result[1] = 0;
		result[2] = 0;

		{
			for( int l = 0; l < 4; ++l )
				for( int m = 0; m < 4; ++m )
					for( int n = 0; n < 4; ++n ) {
						int c = (gridi+l) + dimsx * ((gridj+m) + dimsy * (gridk+n));
						double splineProduct = bSpline(l,u) * bSpline(m,v) * bSpline(n,w);
						result[0] += splineProduct * coeffs[c][0];
						result[1] += splineProduct * coeffs[c][1];
						result[2] += splineProduct * coeffs[c][2];
					}
		}
	}

	/* This function is more-or-less cut-and-pasted from Greg
	 * Jefferis code in Affine.R (GPL-ed) */

	public static FastMatrix parseTypedStreamAffine( File f ) {

		double tx = 0, ty = 0, tz = 0;
		double rx = 0, ry = 0, rz = 0;
		double sx = 0, sy = 0, sz = 0;
		double shx = 0, shy = 0, shz = 0;
		double cx = 0, cy = 0, cz = 0;

		try {
			BufferedReader br = new BufferedReader( new FileReader(f) );

			String n = "([-\\.0-9]+)";
			String space = "[ \\t]+";
			String re = "^\t\t";
			re += "(xlate|rotate|scale|shear|center)";
			re += space + n + space + n + space + n;
			Pattern p = Pattern.compile(re);

			String line = br.readLine();

			while( line != null ) {

				Matcher m = p.matcher(line);
				if( m.find() ) {
					double a = Double.parseDouble( m.group(2) );
					double b = Double.parseDouble( m.group(3) );
					double c = Double.parseDouble( m.group(4) );
					String parameter = m.group(1);
					if( parameter.equals("xlate") ) {
						tx = a; ty = b; tz = c;
					} else if( parameter.equals("rotate") ) {
						rx = a; ry = b; rz = c;
					} else if( parameter.equals("scale") ) {
						sx = a; sy = b; sz = c;
					} else if( parameter.equals("shear") ) {
						shx = a; shy = b; shz = c;
					} else if( parameter.equals("center") ) {
						cx = a; cy = b; cz = c;
					}
				}

				line = br.readLine();
			}
		} catch( IOException e ) {
			IJ.error("IOException in parseTypedStreamAffine: "+e);
			return null;
		}
		double alpha = degToRad( rx );
		double theta = degToRad( ry );
		double phi = degToRad( rz );
		double cos0 = Math.cos( alpha );
		double sin0 = Math.sin( alpha );
		double cos1 = Math.cos( theta );
		double sin1 = Math.sin( theta );
		double cos2 = Math.cos( phi );
		double sin2 = Math.sin( phi );
		double sin0xsin1 = sin0 * sin1;
		double cos0xsin1 = cos0 * sin1;
		double [][] rval = new double[4][4];
		rval[0][0] =  cos1*cos2 * sx;
		rval[1][0] = -cos1*sin2 * sx;
		rval[2][0] = -sin1 * sx;
		rval[3][0] = 0;
		rval[0][1] =  (sin0xsin1*cos2 + cos0*sin2) * sy;
		rval[1][1] = (-sin0xsin1*sin2 + cos0*cos2) * sy;
		rval[2][1] =  sin0*cos1 * sy;
		rval[3][1] = 0;
		rval[0][2] =  (cos0xsin1*cos2 - sin0*sin2) * sz;
		rval[1][2] = (-cos0xsin1*sin2 - sin0*cos2) * sz;
		rval[2][2] =  cos0*cos1 * sz;
		rval[3][2] = 0;
		rval[3][3] = 1;
		double shears[] = new double[3];
		// generate shears
		// make a copy
		//rval2=rval
		shears[0] = shx;
		shears[1] = shy;
		shears[2] = shz;
		for( int i = 2; i >= 0; --i ) {
			for( int j = 0; j < 3; ++j ) {
				rval[j][i] += shears[i] * rval[j][(i+1)%3];
			}
		}

		// transform rotation center
		double [] cM = new double[3];
		cM[0] = cx*rval[0][0] + cy*rval[0][1] + cz*rval[0][2];
		cM[1] = cx*rval[1][0] + cy*rval[1][1] + cz*rval[1][2];
		cM[2] = cx*rval[2][0] + cy*rval[2][1] + cz*rval[2][2];

		// set translations
		rval[0][3] = tx - cM[0] + cx;
		rval[1][3] = ty - cM[1] + cy;
		rval[2][3] = tz - cM[2] + cz;

		if( false )
			for( int i = 0; i < rval.length; ++i ) {
				System.out.println("rval["+i+"]: "+rval[i][0]+", "+rval[i][1]+", "+rval[i][2]+", "+rval[i][3]);
			}

		return new FastMatrix( rval );
	}

	public static CMTK_Transformation parseTypedStreamWarp( File f ) {

		String n = "([\\-.0-9]+)";
		Pattern pSplineSection = Pattern.compile("^[ \\t]+spline_warp \\{");
		Pattern pEndOfSection = Pattern.compile("^[ \\t]*}");
		Pattern pDims = Pattern.compile("^[ \\t]*dims "+n+" "+n+" "+n);
		Pattern pDomain = Pattern.compile("^[ \\t]*domain "+n+" "+n+" "+n);
		Pattern pOrigin = Pattern.compile("^[ \\t]*origin "+n+" "+n+" "+n);
		Pattern pCoefficients = Pattern.compile("^[ \\t]*coefficients "+n+" "+n+" "+n);
		Pattern pThreeNumbers = Pattern.compile("^[ \\t]+"+n+" "+n+" "+n);

		int dimsx = -1, dimsy = -1, dimsz = -1;
		double domainx = Double.MIN_VALUE, domainy = Double.MIN_VALUE, domainz = Double.MIN_VALUE;
		double originx = Double.MIN_VALUE, originy = Double.MIN_VALUE, originz = Double.MIN_VALUE;

		double coeffs[][] = null;

		// We make lots of very strict assumptions about the
		// format of the file, since I don't have a grammar
		// for it, and there's no point in trying to guess how
		// flexible it is...

		try {
			BufferedReader br = new BufferedReader( new FileReader(f) );
			String line;
			// Skip over everything up to "\tspline_warp"
			while( true ) {
				line = br.readLine();
				if( line == null ) {
					IJ.error("Couldn't find "+pSplineSection);
					return null;
				}
				Matcher m = pSplineSection.matcher(line);
				if( m.find() )
					break;
			}
			// Next we expect the an affine_xform section
			// so skip over that
			while( true ) {
				line = br.readLine();
				if( line == null ) {
					IJ.error("Couldn't find "+pEndOfSection);
					return null;
				}
				Matcher m = pEndOfSection.matcher(line);
				if( m.find() )
					break;
			}
			while( true ) {
				line = br.readLine();
				if( line == null )
					break;
				Matcher m;
				m = pDomain.matcher(line);
				if( m.find() ) {
					domainx = Double.parseDouble( m.group(1) );
					domainy = Double.parseDouble( m.group(2) );
					domainz = Double.parseDouble( m.group(3) );
					continue;
				}
				m = pDims.matcher(line);
				if( m.find() ) {
					dimsx = Integer.parseInt( m.group(1) );
					dimsy = Integer.parseInt( m.group(2) );
					dimsz = Integer.parseInt( m.group(3) );
					continue;
				}
				m = pOrigin.matcher(line);
				if( m.find() ) {
					originx = Double.parseDouble( m.group(1) );
					originy = Double.parseDouble( m.group(2) );
					originz = Double.parseDouble( m.group(3) );
					continue;
				}
				m = pCoefficients.matcher(line);
				if( m.find() ) {
					if( dimsx < 0 ) {
						IJ.error("Failed: got 'coefficients' before 'dims'");
						return null;
					}
					coeffs = new double[dimsx*dimsy*dimsz][3];
					coeffs[0][0] = Double.parseDouble( m.group(1) );
					coeffs[0][1] = Double.parseDouble( m.group(2) );
					coeffs[0][2] = Double.parseDouble( m.group(3) );
					int added = 1;
					while( true ) {
						line = br.readLine();
						m = pThreeNumbers.matcher(line);
						if( ! m.find() )
							break;
						coeffs[added][0] = Double.parseDouble( m.group(1) );
						coeffs[added][1] = Double.parseDouble( m.group(2) );
						coeffs[added][2] = Double.parseDouble( m.group(3) );
						++ added;
					}
					int pointsExpected = dimsx * dimsy * dimsz;
					if( pointsExpected != added ) {
						IJ.error("Number of coefficients ("+added+") didn't match expected number ("+pointsExpected+")");
						return null;
					}
					break;
				}
			}

		} catch( IOException e ) {
			IJ.error("IOException in parseTypedStreamAffine: "+e);
			return null;
		}

		if( domainx == Double.MIN_VALUE ) {
			IJ.error("Failed to find 'domain' line");
			return null;
		}

		if( originx == Double.MIN_VALUE ) {
			IJ.error("Failed to find 'origin' line");
			return null;
		}

		if( dimsx < 0 ) {
			IJ.error("Failed to find 'dims' line");
			return null;
		}

		if( coeffs == null ) {
			IJ.error("Failed to find 'coefficients' line");
			return null;
		}

		return new CMTK_Transformation(
			dimsx, dimsy, dimsz,
			domainx, domainy, domainz,
			originx, originy, originz,
			coeffs );
	}
}

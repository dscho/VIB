/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Bookstein;
import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import vib.FastMatrix;
import landmarks.NamedPointWorld;
import vib.oldregistration.RegistrationAlgorithm;

import util.Overlay_Registered;

public class Bookstein_From_Landmarks extends RegistrationAlgorithm implements PlugIn {


        public void run(String arg) {

                int[] wList = WindowManager.getIDList();
                if (wList==null) {
                        IJ.error("Bookstein_From_Landmarks.run(): No images are open");
                        return;
                }

                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }

                String none = "*None*";
                titles[wList.length] = none;

                GenericDialog gd = new GenericDialog("Thin Plate Spline Registration from Landmarks");
                gd.addChoice("Template stack:", titles, titles[0]);
                gd.addChoice("Stack to transform:", titles, titles[1]);

                gd.addCheckbox("Keep source images", true);
		gd.addCheckbox("Overlay result", true );

                gd.showDialog();
                if (gd.wasCanceled())
                        return;

                int[] index = new int[2];
                index[0] = gd.getNextChoiceIndex();
                index[1] = gd.getNextChoiceIndex();
                keepSourceImages = gd.getNextBoolean();
		boolean overlayResult = gd.getNextBoolean();

                sourceImages = new ImagePlus[2];

                sourceImages[0] = WindowManager.getImage(wList[index[0]]);
                sourceImages[1] = WindowManager.getImage(wList[index[1]]);

		ImagePlus transformed = register();

		if( overlayResult ) {
			ImagePlus merged = Overlay_Registered.overlayToImagePlus( sourceImages[0], transformed );
			merged.setTitle( "Registered and Overlayed" );
			merged.show();
		} else
			transformed.show();
        }

	double xSpacingTemplate;
	double xSpacingDomain;
	double ySpacingTemplate;
	double ySpacingDomain;
	double zSpacingTemplate;
	double zSpacingDomain;

	int templateWidth;
	int templateHeight;
	int templateDepth;

	int domainWidth;
	int domainHeight;
	int domainDepth;

	Bookstein templateToDomain;
	Bookstein domainToTemplate;

	Calibration templateCalibration;
	Calibration domainCalibration;

	public void generateTransformation( ) {
		if( sourceImages == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The source images must be set before calling generateTransformation()");
		if( sourceImages[0] == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The template image is null in generateTransformation()");
		if( sourceImages[1] == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The image to transform is null in generateTransformation()");

                NamedPointSet points0 = null;
                NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[0].getTitle()+"\"" );
                }

		try {
			points1 =  NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[1].getTitle()+"\"" );
                }

                ArrayList<String> commonPointNames = points0.namesSharedWith( points1, true );

                Point3d[] domainPoints=new Point3d[commonPointNames.size()];
                Point3d[] templatePoints=new Point3d[commonPointNames.size()];

                int i_index=0;
                for ( String s : commonPointNames ) {

			for( NamedPointWorld current : points0.pointsWorld ) {
                                if (s.equals(current.getName())) {
                                        Point3d p=new Point3d(current.x,
                                                              current.y,
                                                              current.z);
                                        templatePoints[i_index]=p;
                                        break;
                                }
			}

			for( NamedPointWorld current : points1.pointsWorld ) {
				if (s.equals(current.getName())) {
					Point3d p=new Point3d(current.x,
							      current.y,
							      current.z);
					domainPoints[i_index] = p;
					break;
				}

			}

                        ++i_index;
                }

		templateToDomain = new Bookstein( templatePoints, domainPoints );

		ImagePlus template = sourceImages[0];
		ImagePlus domain = sourceImages[1];

		xSpacingTemplate = 1;
		ySpacingTemplate = 1;
		zSpacingTemplate = 1;
		templateCalibration = template.getCalibration();
		if( templateCalibration != null ) {
			xSpacingTemplate = templateCalibration.pixelWidth;
			ySpacingTemplate = templateCalibration.pixelHeight;
			zSpacingTemplate = templateCalibration.pixelDepth;
		}

		xSpacingDomain = 1;
		ySpacingDomain = 1;
		zSpacingDomain = 1;
		domainCalibration = domain.getCalibration();
		if( domainCalibration != null ) {
			xSpacingDomain = domainCalibration.pixelWidth;
			ySpacingDomain = domainCalibration.pixelHeight;
			zSpacingDomain = domainCalibration.pixelDepth;
		}

		templateWidth = template.getWidth();
		templateHeight = template.getHeight();
		templateDepth = template.getStackSize();

		domainWidth = domain.getWidth();
		domainHeight = domain.getHeight();
		domainDepth = domain.getStackSize();

		validateTransformation();
	}

	/* We really don't want to have to construct a new Point3d
	   each time we transform a point; obviously this will all go
	   horribly wrong if you're using this from multiple threads. */

	Point3d p = new Point3d();

	public void transformTemplateToDomainWorld( double x, double y, double z, Point3d result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transformWorld() with an invalid transformation." );
		p.x = x; p.y = y; p.z = z;
		templateToDomain.apply( p );
		result.x = templateToDomain.x;
		result.y = templateToDomain.y;
		result.z = templateToDomain.z;
	}

	public void transformTemplateToDomain( int x, int y, int z, RegistrationAlgorithm.ImagePoint result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transform() with an invalid transformation." );

		p.x = x * xSpacingTemplate;
		p.y = y * ySpacingTemplate;
		p.z = z * zSpacingTemplate;

		templateToDomain.apply( p );

		double dxd = templateToDomain.x / xSpacingDomain;
		double dyd = templateToDomain.y / ySpacingDomain;
		double dzd = templateToDomain.z / zSpacingDomain;

		result.x = (int)Math.round( dxd );
		result.y = (int)Math.round( dyd );
		result.z = (int)Math.round( dzd );
	}

	public void transformDomainToTemplateWorld( double x, double y, double z, Point3d result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transformWorld() with an invalid transformation." );
		p.x = x; p.y = y; p.z = z;
		domainToTemplate.apply( p );
		result.x = domainToTemplate.x;
		result.y = domainToTemplate.y;
		result.z = domainToTemplate.z;
	}

	public void transformDomainToTemplate( int x, int y, int z, RegistrationAlgorithm.ImagePoint result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transform() with an invalid transformation." );

		p.x = x * xSpacingDomain;
		p.y = y * ySpacingDomain;
		p.z = z * zSpacingDomain;

		domainToTemplate.apply( p );

		double dxd = domainToTemplate.x / xSpacingTemplate;
		double dyd = domainToTemplate.y / ySpacingTemplate;
		double dzd = domainToTemplate.z / zSpacingTemplate;

		result.x = (int)Math.round( dxd );
		result.y = (int)Math.round( dyd );
		result.z = (int)Math.round( dzd );
	}

	public ImagePlus register() {

		generateTransformation();

		ImageStack newStack = new ImageStack( templateWidth, templateHeight );

		ImageStack domainStack = sourceImages[1].getStack();

		byte [][] domainPixels = new byte[domainDepth][];
		for( int z = 0; z < domainDepth; ++z )
			domainPixels[z] = ( byte[] ) domainStack.getPixels( z + 1 );

		RegistrationAlgorithm.ImagePoint result = new RegistrationAlgorithm.ImagePoint();

		IJ.showProgress( 0 );

		for( int z = 0; z < templateDepth; ++z ) {

			byte [] pixels = new byte[ templateWidth * templateHeight ];

			for( int y = 0; y < templateHeight; ++y )
				for( int x = 0; x < templateWidth; ++x ) {

					transformTemplateToDomain( x, y, z, result );

					int dx = result.x;
					int dy = result.y;
					int dz = result.z;

					if( dx < 0 || dy < 0 || dz < 0 ||
					    dx >= domainWidth ||
					    dy >= domainHeight ||
					    dz >= domainDepth )
						continue;

					pixels[y*templateWidth+x] =
						domainPixels[dz][dy*domainWidth+dx];
				}

			ByteProcessor bp = new ByteProcessor( templateWidth, templateHeight );
			bp.setPixels( pixels );
			newStack.addSlice( "", bp );

			IJ.showProgress( (z + 1) / (double)templateDepth );
		}

		IJ.showProgress( 1.0 );

		ImagePlus transformed = new ImagePlus( "Transformed", newStack );

		if( templateCalibration != null )
			transformed.setCalibration( templateCalibration );

		return transformed;
	}
}

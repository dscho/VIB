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

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import vib.FastMatrix;
import vib.transforms.OrderedTransformations;
import vib.transforms.FastMatrixTransform;
import vib.transforms.BooksteinTransform;
import landmarks.NamedPointWorld;
import vib.oldregistration.RegistrationAlgorithm;

public class Bookstein_From_Landmarks extends RegistrationAlgorithm implements PlugIn {

        OrderedTransformations transformation;

        OrderedTransformations getTransformation( ) {
                return transformation;
        }

        public ImagePlus produceOverlayed( ) {
                transformation=register();
                return transformation.createNewImage(sourceImages[0],sourceImages[1],true);
        }

        public ImagePlus produceMapped( ) {
                transformation=register();
                return transformation.createNewImageSingle(sourceImages[0],sourceImages[1],true);
        }

        public ImagePlus produceMappedRanges( int xmin, int xmax,
                                               int ymin, int ymax,
                                               int zmin, int zmax ) {
                transformation=register();
                return transformation.createNewImageSingle( sourceImages[1],
                                                            xmin, xmax,
                                                            ymin, ymax,
                                                            zmin, zmax );
        }

        public OrderedTransformations register() {

                if(transformation!=null)
                        return transformation;

                FastMatrixTransform toCorrectAspect0 = FastMatrixTransform.fromCalibrationWithoutOrigin(sourceImages[0]);
                FastMatrixTransform fromCorrectAspect0=toCorrectAspect0.inverse();

                FastMatrixTransform toCorrectAspect1 = FastMatrixTransform.fromCalibrationWithoutOrigin(sourceImages[1]);
                FastMatrixTransform fromCorrectAspect1=toCorrectAspect1.inverse();

                NamedPointSet points0 = null;
                NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[0].getTitle()+"\"");
                        System.out.println("for 0 in Bookstein_From_Landmarks.register()");
                        return null;
                }

		try {
			points1 =  NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\"");
                        System.out.println("for 1 in Bookstein_From_Landmarks.register()");
                        return null;
                }

                ArrayList<String> commonPointNames = points0.namesSharedWith(points1);

                Point3d[] domainPoints=new Point3d[commonPointNames.size()];
                Point3d[] templatePoints=new Point3d[commonPointNames.size()];

                int i_index=0;
                for (Iterator i=commonPointNames.listIterator();i.hasNext();) {

                        String s = (String)i.next();

                        // System.out.println("Point "+i_index+" is: "+s);

			// FIXME: now these NamedPoint are all in world coordinates

                        NamedPointWorld p0 = null;
                        NamedPointWorld p1 = null;

                        for (Iterator i0=points0.listIterator();i0.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i0.next();
                                if (s.equals(current.getName())) {
                                        Point3d p=new Point3d(current.x,
                                                              current.y,
                                                              current.z);
                                        // drawCrosshair(sourceImages[0],(int)p.x,(int)p.y,(int)p.z,current.name);
                                        toCorrectAspect0.apply(p);
                                        Point3d p_corrected=new Point3d(toCorrectAspect0.x,
                                                                        toCorrectAspect0.y,
                                                                        toCorrectAspect0.z);
                                        // System.out.println("      "+p);
                                        // System.out.println("   => "+p_corrected);
                                        templatePoints[i_index]=p_corrected;
                                        break;
                                }
                        }

                        for (Iterator i1=points1.listIterator();i1.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i1.next();
                                if (s.equals(current.getName())) {
                                        Point3d p=new Point3d(current.x,
                                                              current.y,
                                                              current.z);
                                        // drawCrosshair(sourceImages[1],(int)p.x,(int)p.y,(int)p.z,current.name);
                                        toCorrectAspect1.apply(p);
                                        Point3d p_corrected=new Point3d(toCorrectAspect1.x,
                                                                        toCorrectAspect1.y,
                                                                        toCorrectAspect1.z);
                                        // System.out.println("      "+p);
                                        // System.out.println("   => "+p_corrected);
                                        domainPoints[i_index]=p_corrected;
                                        break;
                                }
                        }

                        ++i_index;
                }

                BooksteinTransform b=new BooksteinTransform(domainPoints,templatePoints);

                transformation=new OrderedTransformations();

                transformation.addLast(toCorrectAspect1);
                transformation.addLast(b);
                transformation.addLast(fromCorrectAspect0);

                // System.out.println("Found transformation:\n"+transformation);
                OrderedTransformations inverted=transformation.inverse();

                return transformation;
        }

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

                gd.showDialog();
                if (gd.wasCanceled())
                        return;

                int[] index = new int[2];
                index[0] = gd.getNextChoiceIndex();
                index[1] = gd.getNextChoiceIndex();
                keepSourceImages = gd.getNextBoolean();

                sourceImages = new ImagePlus[2];

                sourceImages[0] = WindowManager.getImage(wList[index[0]]);
                sourceImages[1] = WindowManager.getImage(wList[index[1]]);

                System.out.println("REMOVEME: about to register");
                transformation=register();
                System.out.println("REMOVEME: found registration, now producing image.");
                ImagePlus newImage=transformation.createNewImage(sourceImages[0],sourceImages[1],true);
                newImage.show();
        }
}

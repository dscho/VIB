/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

import java.io.*;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;

import vib.transforms.OrderedTransformations;
import vib.transforms.FastMatrixTransform;
import vib.transforms.BooksteinTransform;

import landmarks.NamedPoint;
import landmarks.NamedPointSet;

import vib.oldregistration.*;

public class Bookstein_FromMarkers extends RegistrationAlgorithm implements PlugIn {

        static int test_column_x;
        static int test_column_y;

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

                NamedPointSet points0 = NamedPointSet.forImage(sourceImages[0]);
                NamedPointSet points1 = NamedPointSet.forImage(sourceImages[1]);

                if(points0==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[0].getTitle()+"\"");
                        System.out.println("for 0 in Bookstein_FromMarkers.register()");
                        return null;
                }

                if(points1==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\"");
                        System.out.println("for 1 in Bookstein_FromMarkers.register()");
                        return null;
                }

                ArrayList<String> commonPointNames = points0.namesSharedWith(points1);

		System.out.println("found "+commonPointNames.size()+" points in common.");
		
                Point3d[] domainPoints=new Point3d[commonPointNames.size()];
                Point3d[] templatePoints=new Point3d[commonPointNames.size()];

                int i_index=0;
                for (Iterator i=commonPointNames.listIterator();i.hasNext();) {

                        String s = (String)i.next();

                        // System.out.println("Point "+i_index+" is: "+s);

                        NamedPoint p0 = null;
                        NamedPoint p1 = null;

                        for (Iterator i0=points0.listIterator();i0.hasNext();) {
                                NamedPoint current=(NamedPoint)i0.next();
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
                                NamedPoint current=(NamedPoint)i1.next();
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

                Bookstein_FromMarkers.test_column_x=(int)templatePoints[1].x;
                Bookstein_FromMarkers.test_column_y=(int)templatePoints[1].x;

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
                        IJ.error("Bookstein_FromMarkers.run(): No images are open");
                        return;
                }

                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }

                String none = "*None*";
                titles[wList.length] = none;

                GenericDialog gd = new GenericDialog("Thin Plate Spline Registration from Markers");
                gd.addChoice("Template stack:", titles, titles[0]);
                gd.addChoice("Stack to transform:", titles, titles[1]);

                gd.addCheckbox("Keep source images", true);

                /*
                  String[] labels = {
                      "Pick best based on least-squares",
                      "Pick best from best 4 points"
                  };

                  boolean[] defaultValues = { false, true };

                  gd.addCheckboxGroup(2,1,labels,defaultValues);
                */

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

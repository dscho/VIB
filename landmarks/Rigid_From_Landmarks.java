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

import vib.transforms.FastMatrixTransform;
import landmarks.NamedPointWorld;
import vib.FastMatrix;
import vib.transforms.OrderedTransformations;
import vib.oldregistration.RegistrationAlgorithm;

import util.CombinationGenerator;

/* This method doesn't work terribly well, and is here largely for
 * comparison purposes. */

// FIXME: all NamedPoints are now NamedPointWorlds

public class Rigid_From_Landmarks extends RegistrationAlgorithm implements PlugIn {

        OrderedTransformations transformation;

        // FIXME: shouldn't be static
        static double lowestScoreOnRegistration=Double.MAX_VALUE;

        static double scoreFromAllLandmarks(OrderedTransformations t,
					    ArrayList<String> common,
					    NamedPointSet inImage0,
					    NamedPointSet inImage1) {

                double sum_squared_differences = 0.0;

                for (Iterator i=common.listIterator();i.hasNext();) {
                        String s = (String)i.next();
                        NamedPointWorld p0 = null;
                        NamedPointWorld p1 = null;

                        for (Iterator i0=inImage0.listIterator();i0.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i0.next();
                                if (s.equals(current.getName())) {
                                        p0 = current;
                                        break;
                                }
                        }

                        for (Iterator i1=inImage1.listIterator();i1.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i1.next();
                                if (s.equals(current.getName())) {
                                        p1 = current;
                                        break;
                                }
                        }

                        double[] p1_transformed = new double[3];
                        t.apply(p1.x,p1.y,p1.z,p1_transformed);

                        double distance = Math.sqrt(
                                (p1_transformed[0] - p0.x) * (p1_transformed[0] - p0.x) +
                                (p1_transformed[1] - p0.y) * (p1_transformed[1] - p0.y) +
                                (p1_transformed[2] - p0.z) * (p1_transformed[2] - p0.z)
                                );

                        // Obviously we don't need to do the square
                        // root, but it's useful to have for debugging...

                        sum_squared_differences += distance * distance;
                }

                return Math.sqrt(sum_squared_differences/common.size());

        }

        // This finds an affine mapping that maps a1 onto a2,
        // b1 onto b2, etc.

        public static FastMatrixTransform generateRigid(NamedPointWorld a1,
                                               NamedPointWorld b1,
                                               NamedPointWorld c1,

                                               NamedPointWorld a2,
                                               NamedPointWorld b2,
                                               NamedPointWorld c2) {

                FastMatrixTransform translate_a1_to_origin=new FastMatrixTransform(1.0);
                translate_a1_to_origin.setTranslation( - a1.x, - a1.y, - a1.z );

                FastMatrixTransform translate_origin_to_a2=new FastMatrixTransform(1.0);
                translate_origin_to_a2.setTranslation( a2.x, a2.y, a2.z );

                double[] a1_to_b1=new double[3];
                a1_to_b1[0] = b1.x - a1.x;
                a1_to_b1[1] = b1.y - a1.y;
                a1_to_b1[2] = b1.z - a1.z;

                double[] a2_to_b2=new double[3];
                a2_to_b2[0] = b2.x - a2.x;
                a2_to_b2[1] = b2.y - a2.y;
                a2_to_b2[2] = b2.z - a2.z;

                double[] a1_to_c1=new double[3];
                a1_to_c1[0] = c1.x - a1.x;
                a1_to_c1[1] = c1.y - a1.y;
                a1_to_c1[2] = c1.z - a1.z;

                double[] a2_to_c2=new double[3];
                a2_to_c2[0] = c2.x - a2.x;
                a2_to_c2[1] = c2.y - a2.y;
                a2_to_c2[2] = c2.z - a2.z;

                FastMatrix rotation=FastMatrix.rotateToAlignVectors(
                        a2_to_b2,
                        a2_to_c2,
                        a1_to_b1,
                        a1_to_c1 );

                double scale_to_c=Math.sqrt(a2_to_c2[0]*a2_to_c2[0]+
                                            a2_to_c2[1]*a2_to_c2[1]+
                                            a2_to_c2[2]*a2_to_c2[2]) /
                        Math.sqrt(a1_to_c1[0]*a1_to_c1[0]+
                                  a1_to_c1[1]*a1_to_c1[1]+
                                  a1_to_c1[2]*a1_to_c1[2]);

                double scale_to_b=Math.sqrt(a2_to_b2[0]*a2_to_b2[0]+
                                            a2_to_b2[1]*a2_to_b2[1]+
                                            a2_to_b2[2]*a2_to_b2[2]) /
                        Math.sqrt(a1_to_b1[0]*a1_to_b1[0]+
                                  a1_to_b1[1]*a1_to_b1[1]+
                                  a1_to_b1[2]*a1_to_b1[2]);

                double mean_scale=(scale_to_b+scale_to_c)/2;

                // System.out.println( "   ---- scale_to_b is: "+scale_to_b );
                // System.out.println( "   ---- scale_to_c is: "+scale_to_c );
                // System.out.println( "   ---- mean_scale is: "+mean_scale );

                FastMatrixTransform scale=new FastMatrixTransform(mean_scale);

                // Compose them all and return...

                FastMatrixTransform transformed_scaled=translate_a1_to_origin.composeWithFastMatrix(scale);
                FastMatrixTransform transformed_scaled_rotated=transformed_scaled.composeWithFastMatrix(rotation);
                return transformed_scaled_rotated.composeWithFastMatrix(translate_origin_to_a2);
        }

        public void run(String arg) {

                int[] wList = WindowManager.getIDList();
                if (wList==null) {
                        IJ.error("Rigid_From_Landmarks: No images are open.");
                        return;
                }

                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }

                String none = "*None*";
                titles[wList.length] = none;

                GenericDialog gd = new GenericDialog("Rigid Registration from Landmarks");
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

                transformation=register();
                ImagePlus newImage=transformation.createNewImage(sourceImages[0],sourceImages[1],true);
                newImage.show();

        }

        public ImagePlus produceOverlayed( ) {

                transformation=register();
                return transformation.createNewImage(sourceImages[0],sourceImages[1],true);
        }

        public OrderedTransformations register() {

                NamedPointSet points0 = null;
                NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[0].getTitle()+"\"");
                        System.out.println("for 0 in Rigid_From_Landmarks.register()");
                        return null;
                }

		try {
			points1 = NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\"");
                        System.out.println("for 1 in Rigid_From_Landmarks.register()");
                        return null;
                }

                Calibration c0=sourceImages[0].getCalibration();
                Calibration c1=sourceImages[1].getCalibration();

                FastMatrixTransform toAspect0=FastMatrixTransform.fromCalibrationWithoutOrigin(sourceImages[0]);
                FastMatrixTransform toAspect1=FastMatrixTransform.fromCalibrationWithoutOrigin(sourceImages[1]);
                FastMatrixTransform fromAspect0=toAspect0.inverse();

		/* No need to correct now...
                points0.correctWithCalibration(c0);
                points1.correctWithCalibration(c1);
		*/

                FastMatrixTransform affine=bestBetweenPoints(points0,points1);

                OrderedTransformations t = new OrderedTransformations();
                t.addLast(toAspect1);
                t.addLast(affine);
                t.addLast(fromAspect0);
                transformation=t;
                return t;
        }
}

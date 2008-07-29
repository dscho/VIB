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

import vib.oldregistration.RegistrationAlgorithm;

import vib.transforms.OrderedTransformations;
import vib.transforms.FastMatrixTransform;
import landmarks.NamedPoint;

import util.CombinationGenerator;

/* This method doesn't work terribly well, and is here largely for
 * comparison purposes. */

public class Affine_From_Landmarks extends RegistrationAlgorithm implements PlugIn {

        OrderedTransformations transformation;

        static double scoreFromAllLandmarks(OrderedTransformations t,
					    ArrayList<String> common,
					    NamedPointSet inImage0,
					    NamedPointSet inImage1) {

                double sum_squared_differences = 0.0;

                for (Iterator i=common.listIterator();i.hasNext();) {
                        String s = (String)i.next();
                        NamedPoint p0 = null;
                        NamedPoint p1 = null;

                        for (Iterator i0=inImage0.listIterator();i0.hasNext();) {
                                NamedPoint current=(NamedPoint)i0.next();
                                if (s.equals(current.getName())) {
                                        p0 = current;
                                        break;
                                }
                        }

                        for (Iterator i1=inImage1.listIterator();i1.hasNext();) {
                                NamedPoint current=(NamedPoint)i1.next();
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

        public static FastMatrixTransform generateAffine(NamedPoint a1,
							 NamedPoint b1,
							 NamedPoint c1,
							 NamedPoint d1,
							 
							 NamedPoint a2,
							 NamedPoint b2,
							 NamedPoint c2,
							 NamedPoint d2) {

                double[][] p = new double[3][4];

                p[0][0] = b1.x - a1.x;
                p[0][1] = c1.x - a1.x;
                p[0][2] = d1.x - a1.x;

                p[1][0] = b1.y - a1.y;
                p[1][1] = c1.y - a1.y;
                p[1][2] = d1.y - a1.y;

                p[2][0] = b1.z - a1.z;
                p[2][1] = c1.z - a1.z;
                p[2][2] = d1.z - a1.z;

                double[][] q = new double[3][4];

                q[0][0] = b2.x - a2.x;
                q[0][1] = c2.x - a2.x;
                q[0][2] = d2.x - a2.x;

                q[1][0] = b2.y - a2.y;
                q[1][1] = c2.y - a2.y;
                q[1][2] = d2.y - a2.y;

                q[2][0] = b2.z - a2.z;
                q[2][1] = c2.z - a2.z;
                q[2][2] = d2.z - a2.z;

                FastMatrixTransform P = new FastMatrixTransform(p);
                FastMatrixTransform Q = new FastMatrixTransform(q);

                FastMatrixTransform M = Q.times(P.inverse());

                M.apply( a1.x, a1.y, a1.z );

                double ox = a2.x - M.x;
                double oy = a2.y - M.y;
                double oz = a2.z - M.z;

                return M.composeWithFastMatrix(FastMatrixTransform.translate(ox,oy,oz));
        }

        public void run(String arg) {

                int[] wList = WindowManager.getIDList();
                if (wList==null) {
                        IJ.error("Affine_From_Landmarks: No images are open.");
                        return;
                }

                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }

                String none = "*None*";
                titles[wList.length] = none;

                GenericDialog gd = new GenericDialog("Affine Registration from Landmarks");
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

        public ImagePlus produceOverlayed( ) {

                transformation=register();
                return transformation.createNewImage(sourceImages[0],sourceImages[1],true);
        }


        public static FastMatrixTransform bestBetweenPoints( NamedPointSet points0, NamedPointSet points1 ) {

                ArrayList<String> commonPointNames = points0.namesSharedWith( points1 );

                int n = commonPointNames.size();

                if (n<4) {
                        String error = "There are fewer than 4 points in these two "+
                                "images that have been marked up with the same "+
                                "names:";
                        if(n==0) {
                                error += " (none in common)";
                        } else {
                                for(Iterator i=commonPointNames.iterator();i.hasNext();)
                                        error += "\n    "+i.next();
                        }
                        IJ.error(error);
                        return null;
                }

                int[] indices = new int[n];
                for(int i=0;i<n;++i)
                        indices[i] = i;

                CombinationGenerator generator = new CombinationGenerator(n,4);

                int[] bestChoiceSoFar;
                OrderedTransformations bestTranformationSoFar = null;
                FastMatrixTransform bestAsFastMatrix = null;
                double minimumScoreSoFar = -1.0; // Negative scores
                                                 // won't occur in
                                                 // practice...

                double totalCombinations = generator.getTotal().doubleValue();

                if(totalCombinations>1024) {
                        IJ.error("There are over 1024 combinations; you probably"+
                                 "shouldn't be using this method.");
                }

                IJ.showProgress(0.0);

                int done = 0;

                while(generator.hasMore()) {

                        int [] choice = generator.getNext();

                        // So, for each set of 4, generate an affine
                        // transformation between the two...

                        FastMatrixTransform affine = generateAffine(
                                points1.get(choice[0]),
                                points1.get(choice[1]),
                                points1.get(choice[2]),
                                points1.get(choice[3]),
                                points0.get(choice[0]),
                                points0.get(choice[1]),
                                points0.get(choice[2]),
                                points0.get(choice[3]) );

                        /*
                        System.out.println("--------------------------------------");
                        for(int j=0;j<4;++j) {

                                double x, y, z, x_ideal, y_ideal, z_ideal;

                                x = points1.get(choice[j]).x;
                                y = points1.get(choice[j]).y;
                                z = points1.get(choice[j]).z;

                                x_ideal = points0.get(choice[j]).x;
                                y_ideal = points0.get(choice[j]).y;
                                z_ideal = points0.get(choice[j]).z;

                                System.out.println("point ("+x+","+y+","+z+")");

                                System.out.println("which should map onto point ("+
                                                   x_ideal+","+
                                                   y_ideal+","+
                                                   z_ideal+")");
                                System.out.println("...actually maps onto:");
                                affine.apply(x,y,z);
                                System.out.println("point ("+affine.x+","+affine.y+","+
                                                   affine.z+")");
                        }
                        */

                        OrderedTransformations t = new OrderedTransformations();
                        t.addLast(affine);

                        double scoreFromLandmarks = scoreFromAllLandmarks(
                                t,
                                commonPointNames,
                                points0,
                                points1);

                        // System.out.println("Score was: "+scoreFromLandmarks);

                        if((minimumScoreSoFar<0)||(scoreFromLandmarks<minimumScoreSoFar)) {

                                bestTranformationSoFar = t;
                                bestAsFastMatrix = affine;
                                minimumScoreSoFar = scoreFromLandmarks;
                                bestChoiceSoFar = choice;

                        }

                        ++ done;
                        IJ.showProgress( done / totalCombinations );
                }

                IJ.showProgress(1.0);

                // System.out.println("Best score was: "+minimumScoreSoFar);

                return bestAsFastMatrix;
        }

        public OrderedTransformations register() {

		NamedPointSet points0 = NamedPointSet.forImage(sourceImages[0]);
		NamedPointSet points1 = NamedPointSet.forImage(sourceImages[1]);

                if(points0==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[0].getTitle()+"\"");
                        System.out.println("for 0 in Affine_From_Landmarks.register()");
                        return null;
                }

                if(points1==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\"");
                        System.out.println("for 1 in Affine_From_Landmarks.register()");
                        return null;
                }

                FastMatrixTransform affine=bestBetweenPoints( points0, points1 );

                OrderedTransformations t = new OrderedTransformations();
                t.addLast(affine);
                transformation=t;
                return t;
        }
}

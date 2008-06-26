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
import vib.FastMatrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import vib.transforms.FastMatrixTransform;

import vib.transforms.OrderedTransformations;

import landmarks.NamedPoint;

/* This method doesn't work terribly well, and is here largely for
 * comparison purposes. */

public class Best_Affine_From_Landmarks extends RegistrationAlgorithm implements PlugIn {

        OrderedTransformations transformation;

        double scoreFromAllMarkers(OrderedTransformations t,
                                   ArrayList<String> common,
                                   ArrayList<NamedPoint> inImage0,
                                   ArrayList<NamedPoint> inImage1) {

                double sum_squared_differences = 0.0;
                // FIXME:
                return sum_squared_differences;
        }




        // This finds an affine mapping that maps a1 onto a2,
        // b1 onto b2, etc.

        public FastMatrixTransform generateAffine(NamedPoint a1,
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
                        IJ.error("Best_Affine_From_Landmarks.run(): No images are open.");
                        return;
                }

                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }

                String none = "*None*";
                titles[wList.length] = none;

                GenericDialog gd = new GenericDialog("Affine Registration from Markers");
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
                ImagePlus impNew=transformation.createNewImage(sourceImages[0],sourceImages[1],true);
                impNew.show();

        }

        public ImagePlus produceOverlayed( ) {

                transformation=register();

                return transformation.createNewImage(sourceImages[0],sourceImages[1],true);
        }

        public OrderedTransformations register() {

                ArrayList<NamedPoint> points0 = NamedPoint.pointsForImage(sourceImages[0]);
                ArrayList<NamedPoint> points1 = NamedPoint.pointsForImage(sourceImages[1]);

                if(points0==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[0].getTitle()+"\"");
                        System.out.println("for 0 in Best_Affine_From_Landmarks.register()");
                        return null;
                }

                if(points1==null) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\"");
                        System.out.println("for 1 in Best_Affine_From_Landmarks.register()");
                        return null;
                }

                ArrayList<String> commonPointNames = NamedPoint.pointsInBoth(
                        points0,
                        points1);

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

                Point3d[] fromPoints = new Point3d[commonPointNames.size()];
                Point3d[] toPoints = new Point3d[commonPointNames.size()];

                int index = 0;

                for (Iterator i=commonPointNames.listIterator();i.hasNext();) {

                        String s = (String)i.next();
                        NamedPoint p0 = null;
                        NamedPoint p1 = null;

                        for (Iterator i0=points0.listIterator();i0.hasNext();) {
                                NamedPoint current=(NamedPoint)i0.next();
                                if (s.equals(current.getName())) {
                                        p0 = current;
                                        break;
                                }
                        }

                        for (Iterator i1=points1.listIterator();i1.hasNext();) {
                                NamedPoint current=(NamedPoint)i1.next();
                                if (s.equals(current.getName())) {
                                        p1 = current;
                                        break;
                                }
                        }

                        fromPoints[index] = p1.toPoint3d();
                        toPoints[index] = p0.toPoint3d();

                        ++ index;
                }

                FastMatrixTransform affine = new FastMatrixTransform(FastMatrix.bestLinear(fromPoints,toPoints));

                OrderedTransformations t = new OrderedTransformations();
                t.addLast(affine);

                return t;

        }
}

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
import vib.oldregistration.RegistrationAlgorithm;

import landmarks.NamedPointWorld;

/* This method doesn't work terribly well, and is here largely for
 * comparison purposes. */

public class Best_Affine_From_Landmarks extends RegistrationAlgorithm implements PlugIn {

        OrderedTransformations transformation;

        public void run(String arg) {

		throw new RuntimeException( "This method doesn't work very well, and will be replace shortly." );

/*
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
*/
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
                        IJ.error( "No corresponding .points file found "+
                                  "for image: \""+sourceImages[0].getTitle()+"\": " + e );
                        System.out.println("for 0 in Best_Affine_From_Landmarks.register()");
                        return null;
		}

		try {
			points1 = NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        IJ.error("No corresponding .points file found "+
                                 "for image: \""+sourceImages[1].getTitle()+"\": " + e );
                        System.out.println("for 1 in Best_Affine_From_Landmarks.register()");
                        return null;
		}

                ArrayList<String> commonPointNames = points0.namesSharedWith(points1);

                int n = commonPointNames.size();

                if( n < 4 ) {
                        String error = "There are fewer than 4 points in these two "+
                                "images that have been marked up with the same "+
                                "names:";
                        if( n == 0 ) {
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

			// FIXME: now the NamedPoints are in world coordinates

                        String s = (String)i.next();
                        NamedPointWorld p0 = null;
                        NamedPointWorld p1 = null;

                        for (Iterator i0=points0.listIterator();i0.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i0.next();
                                if (s.equals(current.getName())) {
                                        p0 = current;
                                        break;
                                }
                        }

                        for (Iterator i1=points1.listIterator();i1.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i1.next();
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

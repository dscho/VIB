/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.oldregistration;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import amira.AmiraParameters;
import amira.AmiraMeshEncoder;
import amira.AmiraMeshDecoder;
import vib.SegmentationViewerCanvas;
import vib.FastMatrix;

import math3d.Point3d;

import gui.GuiBuilder;
import ij.*;
import ij.io.*;
import ij.process.ByteProcessor;

import ij.gui.*;

import ij.ImageJ;

import ij.plugin.PlugIn;
import ij.plugin.MacroInstaller;

import ij.measure.Calibration;

import vib.transforms.OrderedTransformations;

import util.BatchOpener;
import util.FileAndChannel;

import landmarks.NamedPoint;

import vib.transforms.FastMatrixTransform;

public class CreateTemplate_ implements PlugIn {

        public void run(String arg) {

                // try {

                String baseName="/media/WD USB 2/corpus/central-complex/";

/*
                FileAndChannel[] filesAndChannels={
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y.lsm",1),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-71yxUAS-lacZ(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-71yxUAS-nod-lacZ(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-eastmost(A)c61(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-middle(C)c5(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-middle-ish(onlygoodoneon(E))210yxUAS-lacZ(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-northernmost(A)c61(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-theotherone(A)c61(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-westmost(B)c61(0).tif", 0),
                        new FileAndChannel("/media/WD USB 2/corpus/central-complex/dab-263y/single-thin-plate-with-mhl-westmost(D)c5(0).tif", 0)
                };
*/
                FileAndChannel[] filesAndChannels={
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0).lsm",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-dab-263y.tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-71yxUAS-lacZ(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-71yxUAS-nod-lacZ(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-eastmost(A)c61(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-middle(C)c5(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-middle-ish(onlygoodoneon(E))210yxUAS-lacZ(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-northernmost(A)c61(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-westmost(B)c61(0).tif",0),
                        new FileAndChannel("/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0)/single-thin-plate-with-mhl-westmost(D)c5(0).tif",0)
                };

                FileAndChannel[] filesAndChannelsOriginal={
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/dab-263y.lsm",1),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-71yxUAS-lacZ(0).lsm",0),
                        // new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-71yxUAS-nod-lacZ(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-eastmost(A)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-middle(C)c5(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-middle-ish(onlygoodoneon(E))210yxUAS-lacZ(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-northernmost(A)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-theotherone(A)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-westmost(B)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-westmost(D)c5(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAN.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAM.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AL.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AK.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AJ.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AH.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AG.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAF.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/210yAE.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/210yAD.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/210yAC.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yABwestmost.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAAeastmost.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAS.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAT.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AU.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAR.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/71yAQ.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/210yAP.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/210yAO.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AV.lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/c061AI().lsm",0)
                };

                int bestIndex = -1;
                OrderedTransformations bestToStandardBrainWithoutScaling=null;

                {
                        // Pick a template.  For each, try mapping it
                        // to the StandardBrain template and see which
                        // one is the "most rigid"...

                        double minimumDistance=Double.MAX_VALUE;
                        double minimumScoreRigid=Double.MAX_VALUE;

                        for( int i=0; i<filesAndChannelsOriginal.length; ++i ) {

                                Calibration c;

                                String standardBrainFileName="/home/s9808248/saturn1/standard-brain/data/vib-drosophila/AverageIntensity-interp.am";
                                FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

                                // System.out.println("standardBrainFileName is now: "+standardBrainFileName+", calling pointsForImage");

                                ArrayList<NamedPoint> standardBrainPoints=
                                        NamedPoint.pointsForImage(standardBrainFileName);

                                if( standardBrainPoints == null ) {
                                        IJ.error("No points file for standard brain file: "+standardBrainFileName);
                                        return;
                                }

                                ImagePlus[] standardBrain=BatchOpener.openFromFile(standardBrainFileName);
                                c = standardBrain[0].getCalibration();
                                NamedPoint.correctWithCalibration(standardBrainPoints,c);
                                FastMatrixTransform backToStandardBrain=FastMatrixTransform.fromCalibrationWithoutOrigin(c).inverse();

                                String candidateTemplateFileName=filesAndChannelsOriginal[i].getPath();
                                ArrayList<NamedPoint> candidateTemplatePoints=
                                        NamedPoint.pointsForImage(candidateTemplateFileName);

                                if( candidateTemplatePoints == null ) {
                                        IJ.error("No points file for candidate template file: "+candidateTemplateFileName);
                                        return;
                                }

                                ImagePlus[] candidateTemplate=BatchOpener.openFromFile(candidateTemplateFileName);
                                c = candidateTemplate[filesAndChannelsOriginal[i].getChannelZeroIndexed()].getCalibration();
                                NamedPoint.correctWithCalibration(candidateTemplatePoints,c);

                                FastMatrixTransform affineBetween=Affine_FromMarkers.bestBetweenPoints(standardBrainPoints,candidateTemplatePoints);

                                FastMatrixTransform[] decomposed=affineBetween.decomposeFully();

                                FastMatrixTransform shearing=decomposed[0];

                                Point3d i_unit=new Point3d(1,0,0);
                                Point3d j_unit=new Point3d(0,1,0);
                                Point3d k_unit=new Point3d(0,0,1);

                                shearing.apply(i_unit);
                                Point3d i_prime=shearing.getResult();
                                shearing.apply(j_unit);
                                Point3d j_prime=shearing.getResult();
                                shearing.apply(k_unit);
                                Point3d k_prime=shearing.getResult();

                                // Use the sum of distances squared of
                                // the unit vectors as a measure of
                                // non-rigidity...

                                DecimalFormat formatter=new DecimalFormat("#####.####");

                                // Also score the transformation based
                                // on how accurately the marker points
                                // match up.

                                ArrayList<String> commonPointNames=NamedPoint.pointsInBoth(standardBrainPoints,candidateTemplatePoints);
                                OrderedTransformations t=new OrderedTransformations();
                                t.addLast(affineBetween);

                                double scoreFromMarkers = Affine_FromMarkers.scoreFromAllMarkers(
                                        t,
                                        commonPointNames,
                                        standardBrainPoints,
                                        candidateTemplatePoints);

                                // System.out.println("Using candidate "+candidateTemplateFileName);
                                // System.out.println("  i goes to: "+formatter.format(i_prime.x)+", "+formatter.format(i_prime.y)+", "+formatter.format(i_prime.z));
                                // System.out.println("  j goes to: "+formatter.format(j_prime.x)+", "+formatter.format(j_prime.y)+", "+formatter.format(j_prime.z));
                                // System.out.println("  k goes to: "+formatter.format(k_prime.x)+", "+formatter.format(k_prime.y)+", "+formatter.format(k_prime.z));

                                double sum_distances_squared=
                                        i_unit.distance2(i_prime)+
                                        j_unit.distance2(j_prime)+
                                        k_unit.distance2(k_prime);

                                /*

                                System.out.println(candidateTemplateFileName+" with score "+sum_distances_squared+" and "+candidateTemplate[filesAndChannelsOriginal[i].getChannelZeroIndexed()].getStack().getSize());

                                */

                                Rigid_FromMarkers rigidMatcher=new Rigid_FromMarkers();
                                rigidMatcher.loadImages(standardBrainFC,filesAndChannelsOriginal[i]);
                                OrderedTransformations rigidTransformation=rigidMatcher.register();
                                double rigid_score=Rigid_FromMarkers.lowestScoreOnRegistration;

                                /*
                                if (sum_distances_squared<minimumDistance) {
                                        if(i==7) {
                                                minimumDistance=sum_distances_squared;
                                                bestIndex=i;
                                                bestToStandardBrainWithoutScaling=new OrderedTransformations();
                                                bestToStandardBrainWithoutScaling.addLast(FastMatrixTransform.fromCalibrationWithoutOrigin(candidateTemplate[filesAndChannelsOriginal[i].getChannelZeroIndexed()]));
                                                bestToStandardBrainWithoutScaling.addLast(decomposed[0]);
                                                bestToStandardBrainWithoutScaling.addLast(decomposed[2]);
                                                bestToStandardBrainWithoutScaling.addLast(decomposed[3]);
                                                bestToStandardBrainWithoutScaling.addLast(backToStandardBrain);
                                                bestToStandardBrainWithoutScaling.addLast(new FastMatrixTransform(0.60));
                                                bestToStandardBrainWithoutScaling.reduce();
                                        }
                                }
                                */

                                if(rigid_score<minimumScoreRigid) {
                                        minimumScoreRigid=rigid_score;
                                        bestIndex=i;
                                        bestToStandardBrainWithoutScaling=(OrderedTransformations)rigidTransformation.clone();
                                        bestToStandardBrainWithoutScaling.addLast(new FastMatrixTransform(2.5));
                                        bestToStandardBrainWithoutScaling.reduce();
                                }

                                System.out.println(rigid_score+"\t"+
                                                   sum_distances_squared+"\t"+
                                                   scoreFromMarkers+"\t"+
                                                   candidateTemplate[0].getStack().getSize()+"\t"+
                                                   candidateTemplateFileName);

                        }

                }

                System.gc();

                System.out.println("Best is: "+filesAndChannelsOriginal[bestIndex].getPath());
                String templateFileName=filesAndChannelsOriginal[bestIndex].getPath();
                int templateChannel=filesAndChannelsOriginal[bestIndex].getChannelZeroIndexed();
                ImagePlus templateImage;
                {
                        ImagePlus[] templateAllChannels=BatchOpener.openFromFile(templateFileName);
                        templateImage=templateAllChannels[templateChannel];
                }

                Calibration templateCalibration=templateImage.getCalibration();

                ArrayList<NamedPoint> pointsInTemplate = NamedPoint.pointsForImage(templateImage);

                ArrayList<NamedPoint> pointsInScaledStandardBrain = NamedPoint.transformPointsWith( pointsInTemplate, bestToStandardBrainWithoutScaling );

                double xmin, xmax;
                double ymin, ymax;
                double zmin, zmax;

                if( pointsInTemplate.size() < 1 ) {
                        IJ.error( "No points associated with the template image." );
                        return;
                }

                {
                        NamedPoint first=pointsInTemplate.get(0);
                        xmin = first.x; xmax = first.x;
                        ymin = first.y; ymax = first.y;
                        zmin = first.z; zmax = first.z;
                }

                for (Iterator iterator=pointsInTemplate.listIterator();iterator.hasNext();) {
                        NamedPoint current=(NamedPoint)iterator.next();
                        if(current.x < xmin)
                                xmin = current.x;
                        if(current.x > xmax)
                                xmax = current.x;
                        if(current.y < ymin)
                                ymin = current.y;
                        if(current.y > ymax)
                                ymax = current.y;
                        if(current.z < zmin)
                                zmin = current.z;
                        if(current.z > zmax)
                                zmax = current.z;
                }

                System.out.println("points xrange from "+xmin+" to "+xmax);
                System.out.println("points yrange from "+ymin+" to "+ymax);
                System.out.println("points zrange from "+zmin+" to "+zmax);

                int width=templateImage.getWidth();
                int height=templateImage.getHeight();
                int depth=templateImage.getStackSize();

                // First work out what the bounds of the new template
                // are going to be in the StandardBrain space.

                double sb_xmin, sb_xmax;
                double sb_ymin, sb_ymax;
                double sb_zmin, sb_zmax;

                {
                        NamedPoint first=pointsInTemplate.get(0);
                        double[] transformed=bestToStandardBrainWithoutScaling.apply(first.x,first.y,first.z);
                        sb_xmin = transformed[0]; sb_xmax = transformed[0];
                        sb_ymin = transformed[1]; sb_ymax = transformed[1];
                        sb_zmin = transformed[2]; sb_zmax = transformed[2];
                }

                for (Iterator iterator=pointsInTemplate.listIterator();iterator.hasNext();) {

                        NamedPoint current=(NamedPoint)iterator.next();
                        double[] transformed=bestToStandardBrainWithoutScaling.apply(current.x,current.y,current.z);

                        if(transformed[0] < sb_xmin)
                                sb_xmin = transformed[0];
                        if(transformed[0] > sb_xmax)
                                sb_xmax = transformed[0];
                        if(transformed[1] < sb_ymin)
                                sb_ymin = transformed[1];
                        if(transformed[1] > sb_ymax)
                                sb_ymax = transformed[1];
                        if(transformed[2] < sb_zmin)
                                sb_zmin = transformed[2];
                        if(transformed[2] > sb_zmax)
                                sb_zmax = transformed[2];
                }

                System.out.println("points xrange from "+sb_xmin+" to "+sb_xmax);
                System.out.println("points yrange from "+sb_ymin+" to "+sb_ymax);
                System.out.println("points zrange from "+sb_zmin+" to "+sb_zmax);

                double xrange=sb_xmax-sb_xmin;
                double yrange=sb_ymax-sb_ymin;
                double zrange=sb_zmax-sb_zmin;

                sb_xmin -= xrange * 0.2;
                sb_xmax += xrange * 0.2;

                sb_ymin -= yrange * 0.6;
                sb_ymax += yrange * 0.3;

                sb_zmin -= zrange * 0.1;
                sb_zmax += zrange * 0.1;

                int i_sb_xmin = (int)sb_xmin;
                int i_sb_xmax = (int)sb_xmax;

                int i_sb_ymin = (int)sb_ymin;
                int i_sb_ymax = (int)sb_ymax;

                int i_sb_zmin = (int)sb_zmin;
                int i_sb_zmax = (int)sb_zmax;

                width=(i_sb_xmax-i_sb_xmin)+1;
                height=(i_sb_ymax-i_sb_ymin)+1;
                depth=(i_sb_zmax-i_sb_zmin)+1;

                System.out.println("dimensions: "+width+","+height+","+depth);

                int[][][] sum=new int[depth][height][width];
                float[][][] sumsq=new float[depth][height][width];
                int[][][] n=new int[depth][height][width];
                int value;

                for(int i=0;i<filesAndChannelsOriginal.length;++i) {

                        FileAndChannel current=filesAndChannelsOriginal[i];

                        System.out.println("Now transforming:"+current.getPath());

                        // For each, find a transformation to the template.

                        Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();

                        matcher.loadImages(filesAndChannelsOriginal[bestIndex],current);
                        matcher.register();

                        OrderedTransformations completeTransformation=(OrderedTransformations)bestToStandardBrainWithoutScaling.clone();
                        completeTransformation.addFirst(matcher.getTransformation());
                        completeTransformation.reduce();

                        ImagePlus resultImage=completeTransformation.createNewImageSingle(
                                matcher.getDomain(),
                                i_sb_xmin, i_sb_xmax,
                                i_sb_ymin, i_sb_ymax,
                                i_sb_zmin, i_sb_zmax );

                        System.out.println("  now gather data from mapped version");

                        ImageStack stack=resultImage.getStack();

                        for(int z=0;z<stack.getSize();++z) {
                                byte [] p=(byte [])stack.getPixels(z+1);
                                for(int y=0;y<stack.getHeight();++y)
                                        for(int x=0;x<stack.getWidth();++x) {
                                                value = (int)( 0xFF & p[ x + y * width ] );
                                                if( value > 0 ) {
                                                        n[z][y][x] += 1;
                                                        sum[z][y][x] += value;
                                                        sumsq[z][y][x] += value*value;
                                                }
                                        }
                        }


                }

                /* Turn the sumsq array into an array of standard
                   deviations */

                double largestStandardDeviation=0;

                for( int z = 0; z < depth; ++z )
                        for( int y = 0; y < height; ++y )
                                for( int x = 0; x < width; ++x ) {
                                        double n_value=n[z][y][x];
                                        if( n_value > 0 ) {
                                                float sq=sumsq[z][y][x];
                                                sq = (float)Math.sqrt( (sq / n_value) - (sum[z][y][x]*sum[z][y][x])/(n_value*n_value) );
                                                if( sq > largestStandardDeviation )
                                                        largestStandardDeviation=sq;
                                                sumsq[z][y][x]=sq;
                                        } else
                                                sumsq[z][y][x] = 0;
                                }

                System.out.println("Largest standard deviation: "+largestStandardDeviation);

                WindowManager.closeAllWindows();
                System.gc();

                // Create template from the average...

                ImageStack stack = new ImageStack( width, height );

                for( int z = 0; z < depth; ++z ) {

                        byte [] greenPixels = new byte[ width * height ];

                        for( int y = 0; y < height; ++y )
                                for( int x = 0; x < width; ++x ) {

                                        if( n[z][y][x] > 0 )
                                                greenPixels[ x + y * width ] =
                                                        (byte)( sum[z][y][x] / n[z][y][x] );
                                        else
                                                greenPixels[ x + y * width ] = 0;

                                }

                        ByteProcessor bp = new ByteProcessor( width, height );

                        bp.setPixels( greenPixels );

                        stack.addSlice( null, bp );

                        IJ.showProgress( (double) (z + 1) / (2 * depth) );

                }

                ImagePlus impNew = new ImagePlus( "template", stack );
                impNew.setCalibration(templateCalibration);

                ImageStack stackSD = new ImageStack( width, height );

                for( int z = 0; z < depth; ++z ) {

                        byte [] greenPixels = new byte[ width * height ];

                        for( int y = 0; y < height; ++y )
                                for( int x = 0; x < width; ++x ) {

                                        if( n[z][y][x] > 0 )
                                                greenPixels[ x + y * width ] =
                                                        (byte)( sumsq[z][y][x] * 255 / largestStandardDeviation );
                                        else
                                                greenPixels[ x + y * width ] = 0;

                                }

                        ByteProcessor bp = new ByteProcessor( width, height );

                        bp.setPixels( greenPixels );

                        stackSD.addSlice( null, bp );

                        IJ.showProgress( (double) (z + 1 + depth) / (2 * depth) );

                }

                IJ.showProgress( 1.0 );

                ImagePlus impNewSD = new ImagePlus( "template standard deviations", stackSD );
                impNewSD.setCalibration(templateCalibration);

                // impNewSD.show();
                // impNew.show();

                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss");
                String templateFilename="/home/s9808248/saturn1/vib/ImageJ/template-"+sdf.format( now )+".tif";
                String templateSDFilename="/home/s9808248/saturn1/vib/ImageJ/template-"+sdf.format( now )+"-standard-deviations.tif";

                FileSaver fileSaver=new FileSaver(impNew);
                fileSaver.saveAsTiffStack(templateFilename);

                FileSaver fileSaverSD=new FileSaver(impNewSD);
                fileSaverSD.saveAsTiffStack(templateSDFilename);

                NamedPoint.savePointsFile( pointsInScaledStandardBrain, templateFilename+".points" );
                NamedPoint.savePointsFile( pointsInScaledStandardBrain, templateSDFilename+".points" );

        }

}

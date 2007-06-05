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
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import amira.AmiraParameters;
import amira.AmiraMeshEncoder;
import amira.AmiraMeshDecoder;

import vib.SegmentationViewerCanvas;

import vib.transforms.*;

import util.BatchOpener;
import util.FileAndChannel;
import util.CombinationGenerator;

import gui.GuiBuilder;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.MacroInstaller;

public class Evaluate_RegistrationAlgorithms implements PlugIn {

        public void run(String arg) {

                FileAndChannel[] filesAndChannels={
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-theotherone(A)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/dab-263y.lsm",1),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-71yxUAS-lacZ(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-71yxUAS-nod-lacZ(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-eastmost(A)c61(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-middle(C)c5(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-middle-ish(onlygoodoneon(E))210yxUAS-lacZ(0).lsm",0),
                        new FileAndChannel("/home/s9808248/saturn1/corpus/central-complex/mhl-northernmost(A)c61(0).lsm",0),
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

                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                String outputFile="/home/s9808248/saturn1/vib/ImageJ/"+sdf.format( now );

                FileOutputStream fos;
                BufferedOutputStream bos;
                OutputStreamWriter osw;

                boolean generateImages=false;

                try {

                        fos=new FileOutputStream(outputFile,true);
                        bos=new BufferedOutputStream(fos);
                        osw = new OutputStreamWriter(bos, "UTF-8");

                        osw.write("Template File\t"+
                                  "Output File\t"+
                                  "Bookstein (score unsmoothed)\t"+
                                  "Bookstein (score unsmoothed cropped)\t"+
                                  "Bookstein (score smoothed)\t"+
                                  "Bookstein (score equalized smoothed)\t"+
                                  "Affine (score unsmoothed)\t"+
                                  "Affine (score unsmoothed cropped)\t"+
                                  "Affine (score smoothed)\t"+
                                  "Affine (score equalized smoothed)\t"+
                                  "Rigid (score unsmoothed)\t"+
                                  "Rigid (score unsmoothed cropped)\t"+
                                  "Rigid (score smoothed)\t"+
                                  "Rigid (score equalized smoothed)\n");

                        int n=filesAndChannels.length;

                        CombinationGenerator generator = new CombinationGenerator(n,2);

                        double totalCombinations = generator.getTotal().doubleValue();

                        if(totalCombinations>1024) {
                                IJ.error("There are over 1024 combinations; you probably"+
                                         "shouldn't be using this method.");
                                return;
                        }

                        IJ.showProgress(0.0);

                        int done = 0;

                        while(generator.hasMore()) {

                                int [] choice = generator.getNext();

                                FileAndChannel f0=filesAndChannels[choice[0]];
                                FileAndChannel f1=filesAndChannels[choice[1]];

                                String file0=f0.getPath();
                                String file1=f1.getPath();

                                osw.write(file0+"\t"+file1+"\t");
                                osw.flush();

                                // Find the output filename...

                                File templateFile=new File(file0);
                                File domainFile=new File(file1);

                                System.out.println("---==== comparing: "+templateFile.getName()+" and "+domainFile.getName());

                                BoundsInclusive templatePointsBounds=BoundsInclusive.fromFileName(file0);

                                int last=file0.lastIndexOf('.');
                                String d=file0.substring(0,last);

                                String domainLeaf=domainFile.getName();
                                int lastInDomainLeaf=domainLeaf.lastIndexOf('.');
                                domainLeaf=domainLeaf.substring(0,lastInDomainLeaf);

                                File df=new File(d);
                                df.mkdir();
                                String dfs=df.getPath()+File.separator;

                                if (generateImages) {

                                        String mappedFileName=dfs+"single-thin-plate-with-"+domainLeaf+".tif";

                                        System.out.println("Output filename will be: "+mappedFileName);

                                        // ij.WindowManager.closeAllWindows();
                                        BatchOpener.closeAllWithoutConfirmation();

                                        Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();

                                        matcher.loadImages(f0,f1);
                                        matcher.register();
                                        ImagePlus resultImage=matcher.produceMapped();

                                        FileSaver fileSaver=new FileSaver(resultImage);
                                        fileSaver.saveAsTiffStack(mappedFileName);

                                        resultImage.close();

                                }

                                // ij.WindowManager.closeAllWindows();
                                BatchOpener.closeAllWithoutConfirmation();

                                {
                                        System.out.println("  Trying Bookstein...");

                                        String overlayedFileName=dfs+"merged-thin-plate-with-"+domainLeaf+".tif";

                                        if (generateImages)
                                                System.out.println("Output filename will be: "+overlayedFileName);

                                        // ij.WindowManager.closeAllWindows();
                                        BatchOpener.closeAllWithoutConfirmation();

                                        Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();

                                        matcher.loadImages(f0,f1);

                                        OrderedTransformations transformation=matcher.register();

                                        if (generateImages) {
                                                ImagePlus resultImage=matcher.produceOverlayed();
                                                FileSaver fileSaver=new FileSaver(resultImage);
                                                fileSaver.saveAsTiffStack(overlayedFileName);
                                                resultImage.close();
                                        }

                                        double scoreUnsmoothed=transformation.scoreTransformation(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        double scoreUnsmoothedCropped=transformation.scoreTransformation(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2,
                                                templatePointsBounds);

                                        double scoreSmoothed=transformation.scoreTransformationSmoothed(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        matcher.loadImages(f0,f1);
                                        transformation=matcher.register();

                                        double scoreEqualizedSmoothed=transformation.scoreTransformationEqualizedAndSmoothed(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        osw.write(scoreUnsmoothed+"\t"+scoreUnsmoothedCropped+"\t"+scoreSmoothed+"\t"+scoreEqualizedSmoothed);
                                        osw.flush();

                                        // ij.WindowManager.closeAllWindows();
                                        BatchOpener.closeAllWithoutConfirmation();

                                }

                                {
                                        System.out.println("  Trying Affine...");

                                        String overlayedFileName=dfs+"merged-affine-with-"+domainLeaf+".tif";

                                        if (generateImages)
                                                System.out.println("Output filename will be: "+overlayedFileName);

                                        // ij.WindowManager.closeAllWindows();
                                        BatchOpener.closeAllWithoutConfirmation();

                                        Affine_FromMarkers matcher=new Affine_FromMarkers();

                                        matcher.loadImages(f0,f1);

                                        if (generateImages) {
                                                ImagePlus resultImage=matcher.produceOverlayed();

                                                FileSaver fileSaver=new FileSaver(resultImage);
                                                fileSaver.saveAsTiffStack(overlayedFileName);

                                                resultImage.close();
                                        }

                                        OrderedTransformations transformation=matcher.register();

                                        double scoreUnsmoothed=transformation.scoreTransformation(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        double scoreUnsmoothedCropped=transformation.scoreTransformation(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2,
                                                templatePointsBounds);

                                        double scoreSmoothed=transformation.scoreTransformationSmoothed(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        matcher.loadImages(f0,f1);
                                        transformation=matcher.register();

                                        double scoreEqualizedSmoothed=transformation.scoreTransformationEqualizedAndSmoothed(
                                                matcher.sourceImages[0],
                                                matcher.sourceImages[1],
                                                2);

                                        osw.write(scoreUnsmoothed+"\t"+scoreUnsmoothedCropped+"\t"+scoreSmoothed+"\t"+scoreEqualizedSmoothed);
                                        osw.flush();

                                        // ij.WindowManager.closeAllWindows();
                                        BatchOpener.closeAllWithoutConfirmation();

                                }

                                osw.write("\n");
                                osw.flush();

                                ++ done;
                                IJ.showProgress( done / totalCombinations );
                        }

                        osw.close();
                        bos.close();
                        fos.close();

                } catch( IOException e ) {
                        System.out.println("Caught IOException: "+e);
                } finally {
                }

                IJ.showProgress(1.0);

        }

}

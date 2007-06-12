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
import ij.process.ColorProcessor;

import ij.gui.*;

import ij.ImageJ;

import ij.plugin.PlugIn;
import ij.plugin.MacroInstaller;

import ij.measure.Calibration;

import vib.transforms.OrderedTransformations;

import util.BatchOpener;
import util.FileAndChannel;

public class Create_Hanesch implements PlugIn {

        static final int NONE=0;
        static final int EB=9;
        static final int NOD=10;
        static final int FB=11;
        static final int PB=12;

        public void run(String arg) {

                // - take our big template image
                // - map that to the labelled standard brain image
                // - create a new image stack the same size as the big template
                // - go through each pixel in the template - if it maps to one in one of the central complex region, include it with the right colour
                // - set calibration on new image

                String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
                String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";

                FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

                String bigTemplateFileName="/media/WD USB 2/corpus/templates/template-2006-09-20T20.05.46.tif";

                FileAndChannel bigTemplateFC=new FileAndChannel(bigTemplateFileName,0);

                Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();
                matcher.loadImages(standardBrainFC,bigTemplateFC);
                OrderedTransformations transformation=matcher.register();

                ImageStack bigTemplateStack=matcher.getDomain().getStack();

                int newWidth=bigTemplateStack.getWidth();
                int newHeight=bigTemplateStack.getHeight();
                int newDepth=bigTemplateStack.getSize();

                ImagePlus labels;
                {
                        ImagePlus[] tmp=BatchOpener.openFromFile(standardBrainLabelsFileName);
                        labels=tmp[0];
                }
                System.out.println("   labels were: "+labels);

                ImageStack labelStack=labels.getStack();

                int templateWidth=labelStack.getWidth();
                int templateHeight=labelStack.getHeight();
                int templateDepth=labelStack.getSize();

                System.out.println("About to create stack of size: "+newWidth+","+newHeight+","+newDepth);

                ImageStack newStack=new ImageStack(newWidth,newHeight);

                int x, y, z;

                int x_in_domain;
                int y_in_domain;
                int z_in_domain;

                int x_in_template;
                int y_in_template;
                int z_in_template;

                byte[][] label_data=new byte[templateDepth][];
                for( z = 0; z < templateDepth; ++z )
                        label_data[z] = (byte[])labelStack.getPixels( z + 1 );

                byte [] redPixels = null;
                byte [] greenPixels = null;
                byte [] bluePixels = null;

                for( z=0;z<newDepth;++z) {

                        System.out.println("Creating slice: "+z);

                        byte[] domain_data=(byte[])bigTemplateStack.getPixels( z+1);

                        redPixels = new byte[ newWidth * newHeight ];
                        greenPixels = new byte[ newWidth * newHeight ];
                        bluePixels = new byte[ newWidth * newHeight ];

                        for(y=0;y<newHeight;++y) {
                                for(x=0;x<newWidth;++x) {

                                        double [] transformedPoint = new double[3];

                                        transformation.apply(x,y,z,transformedPoint);

                                        x_in_template=(int)transformedPoint[0];
                                        y_in_template=(int)transformedPoint[1];
                                        z_in_template=(int)transformedPoint[2];

                                        if( (z_in_template>=0) && (z_in_template<templateDepth) &&
                                            (y_in_template>=0) && (y_in_template<templateHeight) &&
                                            (x_in_template>=0) && (x_in_template<templateWidth) ) {

                                                byte label_value=label_data[z_in_template][y_in_template*templateWidth+x_in_template];
                                                byte value_in_domain=domain_data[y*newWidth+x];

                                                switch(label_value) {
                                                case NONE:
                                                        break;
                                                case EB:
                                                        greenPixels[y*newWidth+x] = value_in_domain;
                                                        break;
                                                case NOD:
                                                        redPixels[y*newWidth+x] = value_in_domain;
                                                        bluePixels[y*newWidth+x] = value_in_domain;
                                                        break;
                                                case FB:
                                                        bluePixels[y*newWidth+x] = value_in_domain;
                                                        break;
                                                case PB:
                                                        redPixels[y*newWidth+x] = value_in_domain;
                                                        break;
                                                }

                                        }
                                }
                        }
                        ColorProcessor cp = new ColorProcessor( newWidth, newHeight );
                        cp.setRGB( redPixels, greenPixels, bluePixels );
                        newStack.addSlice( null, cp );
                }

                ImagePlus impNew=new ImagePlus("hanesch stack",newStack);

		impNew.show();

                String outputFilename="/home/s9808248/saturn1/vib/ImageJ/hanesch.tif";
                FileSaver fileSaver=new FileSaver(impNew);
                fileSaver.saveAsTiffStack(outputFilename);

        }

}

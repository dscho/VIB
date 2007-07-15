/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

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

import vib.oldregistration.Bookstein_FromMarkers;

import vib.transforms.OrderedTransformations;

import util.BatchOpener;
import util.FileAndChannel;

public class CreateTracingVolume_ implements PlugIn {

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


		/* changes:
		      take our random image with point and trace labellings
		      map that to the labelled standard brain image
		      work out double the size of the standard brain image
		      go through each pixel in the standard brain image, colouring it with the 

		*/

                String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
                String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";

                FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

                String realImageFileName="/media/WD USB 2/corpus/central-complex/c061AK.lsm";

                FileAndChannel realImageFC=new FileAndChannel(realImageFileName,0);
		String tracesFileName = realImageFileName + ".traces";

		// ------------------------------------------------------------------------

		ArrayList< Path > allPaths = PathAndFillManager.loadTracingsFromFile(tracesFileName);

                Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();
                matcher.loadImages(standardBrainFC,realImageFC);
                OrderedTransformations transformation=matcher.register();

                ImageStack realImageStack=matcher.getDomain().getStack();
		
		ImagePlus standardBrain=matcher.getTemplate();

		double scaleStandard = 1;

                int newWidth = (int)( standardBrain.getWidth() * scaleStandard );
                int newHeight = (int)( standardBrain.getHeight() * scaleStandard );
                int newDepth = (int)( standardBrain.getStackSize() * scaleStandard );

                ImagePlus labels;
                {
                        ImagePlus[] tmp=BatchOpener.openFromFile(standardBrainLabelsFileName);
                        labels=tmp[0];
                }
                System.out.println("   labels were: "+labels);

		// need to get the AmiraParameters object for that image...
		
		AmiraParameters parameters = new AmiraParameters(labels);

		int materials = parameters.getMaterialCount();

		int redValues[] = new int[materials];
		int greenValues[] = new int[materials];
		int blueValues[] = new int[materials];

		for( int i=0; i < materials; i++ ) {

			double[] c = parameters.getMaterialColor(i);

			redValues[i] = (int)(255*c[0]);
			greenValues[i] = (int)(255*c[1]);
			blueValues[i] = (int)(255*c[2]);
		}

                ImageStack labelStack=labels.getStack();

                int templateWidth=labelStack.getWidth();
                int templateHeight=labelStack.getHeight();
                int templateDepth=labelStack.getSize();

                System.out.println("About to create stack of size: "+newWidth+","+newHeight+","+newDepth);

                ImageStack newStack=new ImageStack(newWidth,newHeight);

                int x, y, z;

                byte[][] label_data=new byte[templateDepth][];
                for( z = 0; z < templateDepth; ++z )
                        label_data[z] = (byte[])labelStack.getPixels( z + 1 );

                byte [][] redPixels = new byte[newDepth][];
                byte [][] greenPixels = new byte[newDepth][];
                byte [][] bluePixels = new byte[newDepth][];

                for( z=0;z<newDepth;++z) {

                        System.out.println("Creating slice: "+z);

                        redPixels[z] = new byte[ newWidth * newHeight ];
                        greenPixels[z] = new byte[ newWidth * newHeight ];
                        bluePixels[z] = new byte[ newWidth * newHeight ];
			
                        for(y=0;y<newHeight;++y) {
                                for(x=0;x<newWidth;++x) {

					int label_value=label_data[(int)(z/scaleStandard)][(int)(y/scaleStandard)*templateWidth+(int)(x/scaleStandard)]&0xFF;

					if( label_value >= materials ) {
						IJ.error( "A label value of " + label_value + " was found, which is not a valid material (max " + (materials - 1) + ")" );
						return;
					}

					redPixels[z][y*newWidth+x] = (byte)( redValues[label_value] / 1 );
					greenPixels[z][y*newWidth+x] = (byte)( greenValues[label_value] / 1 );
					bluePixels[z][y*newWidth+x] = (byte)( blueValues[label_value] / 1 );
						
				}
			}
		}

		double [] transformedPoint = new double[3];
					
		if( allPaths != null ) {
			// System.out.println("Have some allPaths paths to draw.");
			int paths = allPaths.size();
			// System.out.println("Paths to draw: "+paths);
			for( int i = 0; i < paths; ++i ) {

				Path p = (Path)allPaths.get(i);

				int last_x_in_template = -1;
				int last_y_in_template = -1;
				int last_z_in_template = -1;

				for( int k = 0; k < p.size(); ++k ) {

					int x_in_domain = p.x_positions[k];
					int y_in_domain = p.y_positions[k];
					int z_in_domain = p.z_positions[k];
					
					transformation.apply(x_in_domain,y_in_domain,z_in_domain,transformedPoint);
						
					int x_in_template=(int)transformedPoint[0];
					int y_in_template=(int)transformedPoint[1];
					int z_in_template=(int)transformedPoint[2];
					
					if( (last_x_in_template >= 0) &&
					    (last_y_in_template >= 0) &&
					    (last_z_in_template >= 0) ) {
						
						int xdiff = Math.abs( x_in_template - last_x_in_template );
						int ydiff = Math.abs( y_in_template - last_y_in_template );
						int zdiff = Math.abs( z_in_template - last_z_in_template );

						if( xdiff > 5 || ydiff > 5 || zdiff > 5 ) {
							System.out.println("too long in path: "+i+", at point "+k);
						}

						int xdiff_s = x_in_template - last_x_in_template;
						int ydiff_s = y_in_template - last_y_in_template;
						int zdiff_s = z_in_template - last_z_in_template;

						// Draw a line from last_ to current...
						
						// Shoddy algorithm for the moment.
						
						// In order of size, must be one of these options:
						//
						//    zdiff >= ydiff >= xdiff
						//    zdiff >= xdiff >= ydiff
						//    ydiff >= xdiff >= zdiff
						//    ydiff >= zdiff >= xdiff
						//    xdiff >= ydiff >= zdiff
						//    xdiff >= zdiff >= ydiff
						
						// For the moment i'm collapsing these into 3 cases (zdiff, ydiff or xdiff largest)
						
						// Each of these cases:
						
						// System.out.println( "x from: " + last_x_in_template + " to " + x_in_template );
						// System.out.println( "y from: " + last_y_in_template + " to " + y_in_template );
						// System.out.println( "z from: " + last_z_in_template + " to " + z_in_template );
						
						long line_x, line_y, line_z;

						if( (zdiff >= ydiff) && (zdiff >= xdiff) ) {
							
							if( zdiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int z_step;
								if( last_z_in_template <= z_in_template ) {
									z_step = 1;
								} else {
									z_step = -1;
								}
								line_z = last_z_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_z - last_z_in_template) / (double)zdiff;
									// System.out.println( proportion_along + " of xdiff_s " + xdiff_s ); 
									// System.out.println( proportion_along + " of ydiff_s " + ydiff_s ); 
									double y_delta = proportion_along * ydiff_s;
									double x_delta = proportion_along * xdiff_s;
									line_y = Math.round(y_delta + last_y_in_template);
									line_x = Math.round(x_delta + last_x_in_template);
									// System.out.println( "x is: "+line_x+" (width: "+newWidth+")");
									// System.out.println( "y is: "+line_y+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_z += z_step;
								} while( line_z != z_in_template );							
							}
							
						} else if( (ydiff >= zdiff) && (ydiff >= xdiff) ) {
							
							if( ydiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int y_step;
								if( last_y_in_template <= y_in_template ) {
									y_step = 1;
								} else {
									y_step = -1;
								}
								line_y = last_y_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_y - last_y_in_template) / (double)ydiff;
									// System.out.println( proportion_along + " of xdiff_s " + xdiff_s ); 
									// System.out.println( proportion_along + " of zdiff_s " + zdiff_s ); 
									double z_delta = proportion_along * zdiff_s;
									double x_delta = proportion_along * xdiff_s;
									line_z = Math.round(z_delta + last_z_in_template);
									line_x = Math.round(x_delta + last_x_in_template);
									// System.out.println( "x is: "+line_x+" (width: "+newWidth+")");
									// System.out.println( "z is: "+line_z+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_y += y_step;
								} while( line_y != y_in_template );						
							}
							
						} else if( (xdiff >= ydiff) && (xdiff >= zdiff) ) {
							
							if( xdiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int x_step;
								if( last_x_in_template <= x_in_template ) {
									x_step = 1;
								} else {
									x_step = -1;
								}
								line_x = last_x_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_x - last_x_in_template) / (double)xdiff;
									// System.out.println( proportion_along + " of ydiff_s " + ydiff_s ); 
									// System.out.println( proportion_along + " of zdiff_s " + zdiff_s ); 
									double z_delta = proportion_along * zdiff_s;
									double y_delta = proportion_along * ydiff_s;
									line_z = Math.round(z_delta + last_z_in_template);
									line_y = Math.round(y_delta + last_y_in_template);
									// System.out.println( "z is: "+line_z+" (depth: "+newDepth+")");
									// System.out.println( "y is: "+line_y+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_x += x_step;
								} while( line_x != x_in_template );						
								
							}
						}
					}
					
					last_x_in_template = x_in_template;
					last_y_in_template = y_in_template;
					last_z_in_template = z_in_template;
				}
			}
		}

		for( z = 0; z < newDepth; ++z ) {

			// System.out.println("Actually adding slice: "+z);
			
                        ColorProcessor cp = new ColorProcessor( newWidth, newHeight );
                        cp.setRGB( redPixels[z], greenPixels[z], bluePixels[z] );
                        newStack.addSlice( null, cp );
                }

                ImagePlus impNew=new ImagePlus("tracings stack",newStack);

		impNew.show();

                // String outputFilename="/home/s9808248/saturn1/vib/ImageJ/hanesch.tif";
                // FileSaver fileSaver=new FileSaver(impNew);
                // fileSaver.saveAsTiffStack(outputFilename);

        }

}
	

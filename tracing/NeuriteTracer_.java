/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This class is no longer used - just holding some code that I
 * don't want in SimpleNeuriteTracer_ for the moment. */

package tracing;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.io.*;

import vib.transforms.OrderedTransformations;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import client.ArchiveClient;




public class NeuriteTracer_ implements PlugIn {
	
	/* We may keep a tranformation which maps to a template with
	   label_data: */
	
	OrderedTransformations transformation;
	byte[][] label_data;
	String [] materialNames;       
	int materials;
	
	int label_stack_width;
	int label_stack_height;
	int label_stack_depth;
	
	ArchiveClient archiveClient;
	
	public void run( String ignored ) {
		
		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient = new ArchiveClient( applet, Macro.getOptions() );
		}
		
	}
	
	FileInfo file_info;

	public void loadCorrespondingPointsFile() {

		/*
		if( archiveClient == null ) {

			file_info = xy.getOriginalFileInfo();
			System.out.println( "file_info is: " + file_info		       

			if( file_info != null ) {
					
				// Is there a points file?
				
				File pointsFile = new File( file_info.directory, file_info.fileName + ".points" );
				System.out.println( "Looking for pointsFile: " + pointsFile );
				if( pointsFile.exists() ) {
					
					String realPath = file_info.directory+file_info.fileName;
					System.out.println( "real path is: " +realPath );
					
					String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
					String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";
					
					FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);
					
					if( standardBrainFC.exists() ) {
						
						FileAndChannel actualImageFC=new FileAndChannel(realPath,0);
						
						Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();
						matcher.loadImages(standardBrainFC,actualImageFC);
						transformation=matcher.register();
						
						ImagePlus labels;
						{
							ImagePlus[] tmp=BatchOpener.openFromFile(standardBrainLabelsFileName);
							labels=tmp[0];
						}
						
						AmiraParameters parameters = new AmiraParameters(labels);
						
						materials = parameters.getMaterialCount();
						
						ImageStack labelStack=labels.getStack();
						
						label_stack_width   = labelStack.getWidth();
						label_stack_height  = labelStack.getHeight();
						label_stack_depth   = labelStack.getSize();
						
						materialNames = new String[256];
						for( int i = 0; i < materials; ++i ) {
							materialNames[i] = parameters.getMaterialName(i);
						}
						
						label_data=new byte[label_stack_depth][];
						for( int z = 0; z < label_stack_depth; ++z )
							label_data[z] = (byte[])labelStack.getPixels( z + 1 );
						
					}
				}
			}
		*/
	}
	
	public void mouseMovedTo( int x_in_pane, int y_in_pane, int in_plane ) {

/*
		int [] p = new int[3];

		findPointInStack( x_in_pane, y_in_pane, in_plane, p );

		if( transformation != null ) {

			double [] transformedPoint = new double[3];
			
			transformation.apply(p[0],p[1],p[2],transformedPoint);
			
			int target_x = (int)transformedPoint[0];
			int target_y = (int)transformedPoint[1];
			int target_z = (int)transformedPoint[2];
		
			String message;

			if( target_x >= 0 && target_x < label_stack_width &&
			    target_y >= 0 && target_x < label_stack_height &&
			    target_z >= 0 && target_x < label_stack_depth ) {

				int material_index = label_data[target_z][label_stack_width*target_y+target_x]&0xFF;
				
				String material_name = materialNames[material_index];

				message = "point maps to: " + material_name;

			} else {

				message = "point maps to out-of-range position";


			}

			IJ.showStatus( message );

		}

		last_x = p[0];
		last_y = p[1];
		last_z = p[2];
*/		
	}
}

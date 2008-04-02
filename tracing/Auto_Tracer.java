/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Auto Tracer".
  
  The ImageJ plugin "Auto Tracer" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  
  The ImageJ plugin "Auto Tracer" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import features.Tubeness_;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import java.io.File;
import java.util.PriorityQueue;
import util.BatchOpener;

public class Auto_Tracer implements PlugIn {

	public class Point implements Comparable<Point> {
		public int x;
		public int y;
		public int z;
		public Point(int x,int y,int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public int compareTo(Point o) {
			return -Float.compare(tubeValues[z][y*width+x],tubeValues[o.z][o.y*width+o.x]);
		}
		@Override
		public String toString() {
			return "("+x+","+y+","+z+")["+tubeValues[z][y*width+x]+"]";
		}
	}
	
	int width;
	int height;
	int depth;
	
	PriorityQueue<Point> mostTubelikePoints=new PriorityQueue<Point>();
	float [][] tubeValues;
	
	public boolean dimensionsIdentical(ImagePlus a, ImagePlus b) {
		return a.getWidth() == b.getWidth() &&
		    a.getHeight() == b.getHeight() &&
		    a.getStackSize() == b.getStackSize();
	}
	
	public void autoTrace( ImagePlus image ) {

		FileInfo originalFileInfo = image.getOriginalFileInfo();
		String originalFileName=originalFileInfo.fileName;
		int lastDot=originalFileName.lastIndexOf(".");
		String beforeExtension=originalFileName.substring(0, lastDot);
		String tubesFileName=beforeExtension+".tubes.tif";
		ImagePlus tubenessImage = null;
		File tubesFile=new File(originalFileInfo.directory,tubesFileName);
		if( tubesFile.exists() ) {
			IJ.showStatus("Loading tubes file.");
			tubenessImage=BatchOpener.openFirstChannel(tubesFile.getAbsolutePath());
			if( tubenessImage == null ) {
				IJ.error("Failed to load tubes image from "+tubesFile.getAbsolutePath());
				return;
			}
		} else {
			IJ.showStatus("No tubes file found, generating anew...");
			Tubeness_ tubifier=new Tubeness_();
			tubenessImage=tubifier.generateTubenessImage(image);
			System.out.println("Got tubes file.");
			boolean saved=new FileSaver(tubenessImage).saveAsTiffStack(tubesFile.getAbsolutePath());
			if( ! saved ) {
				IJ.error("Failed to save tubes image to "+tubesFile.getAbsolutePath());
				return;
			}
		}
		if( ! dimensionsIdentical(image, tubenessImage)) {
			IJ.error("The dimensions of the image and the tube image didn't match.");
			return;
		}
		width=image.getWidth();
		height=image.getHeight();
		depth=image.getStackSize();
		ImageStack tubeStack=tubenessImage.getStack();
		tubeValues = new float[depth][];
		for(int z=0;z<depth;++z) {
			tubeValues[z]=(float[])tubeStack.getPixels(z+1);	
		}
		// Add all of those points to the priority queue....
		for(int z=0;z<depth;++z) {
			for(int y=0;y<height;++y) {
				for(int x=0;x<width;++x) {
					mostTubelikePoints.add(new Point(x,y,z));
				}
			}
		}
		
		// Now get the most tubelike point:
		Point p=mostTubelikePoints.poll();
		
		System.out.println("Got point "+p);
		
		AutoSearchThread ast=new AutoSearchThread(tubenessImage);
		
		
	}
	
	public void run(String arg0) {

		ImagePlus image = IJ.getImage();
		if( image == null ) {
			IJ.error("No current image for automatic tracing.");
			return;
		}
		
		autoTrace(image);
		
		
	}




}

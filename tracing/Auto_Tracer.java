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
import java.util.Comparator;
import util.BatchOpener;

import java.awt.Color;

import stacks.ThreePanes;
import stacks.PaneOwner;

public class Auto_Tracer extends ThreePanes implements PlugIn, PaneOwner, SearchProgressCallback {

	int width;
	int height;
	int depth;

	PriorityQueue<AutoPoint> mostTubelikePoints;
	float [][] tubeValues;

	public boolean dimensionsIdentical(ImagePlus a, ImagePlus b) {
		return a.getWidth() == b.getWidth() &&
		    a.getHeight() == b.getHeight() &&
		    a.getStackSize() == b.getStackSize();
	}

        /* Just for convenience, keep casted references to the
           superclass's InteractiveTracerCanvas objects */

        AutoTracerCanvas canvas;

        /* This override the method in ThreePanes... */

	@Override
        public TracerCanvas createCanvas( ImagePlus imagePlus, int plane ) {
                return new AutoTracerCanvas( imagePlus, this, plane, null );
        }

	public class TubenessComparator implements Comparator<AutoPoint> {

		int width, height, depth;
		float [][] tubeValues;

		public TubenessComparator( int width, int height, int depth, float [][] tubeValues ) {
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.tubeValues = tubeValues;
		}

		@Override
		public int compare( AutoPoint a, AutoPoint b ) {
			return -Float.compare(tubeValues[a.z][a.y*width+a.x],tubeValues[b.z][b.y*width+b.x]);
		}

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
	
		mostTubelikePoints=new PriorityQueue<AutoPoint>(512,new TubenessComparator(width,height,depth,tubeValues));

		// Add all of those points to the priority queue....
		for(int z=0;z<depth;++z) {
			for(int y=0;y<height;++y) {
				for(int x=0;x<width;++x) {
					if( tubeValues[z][y*width+x] > tubenessThreshold ) {
						mostTubelikePoints.add(new AutoPoint(x,y,z));
					}
				}
			}
		}

		SinglePathsGraph completePaths = new SinglePathsGraph(width,height,depth);

		while( mostTubelikePoints.size() > 0 ) {

			// Now get the most tubelike point:
			AutoPoint p=mostTubelikePoints.poll();

			System.out.println("Got point "+p);
			
			ast = new AutoSearchThread( image, /* original image */
						    tubeValues, /* the "tubeness" filtered image */
						    p, /* the point to start the search from */
						    tubenessThreshold,
						    completePaths );

			timeStarted = System.currentTimeMillis();

			ast.setDrawingColors( Color.CYAN, Color.CYAN );
			ast.setDrawingThreshold( -1 );

			ast.addProgressListener(this);

			canvas.addSearchThread(ast);

			ast.start();

			try {
				ast.join();
			} catch( InterruptedException e ) { }

			
			SinglePathsGraph recentGraph = ast.thisPathGraph;

			/* 
			   Now start the pruning...

			 */

			// Now remove all the points genuinely found in this search:

			

			// And merge the new graph into the ones so far:
		      
			


		}
	}

	AutoSearchThread ast;

	long timeStarted;

	int maxNodes = 100000;
	// int maxNodes = 4000;
	int maxSeconds = 10;
	float tubenessThreshold = 34.0f;

	/* This is a (hopefully accurate) description of the algorithm
	 * we're using here for automatic tracing.  The following
	 * parameters must be picked manually:


        [A] Tubeness threshold
        [B] Max time for each search
        [C] Max iterations for each search
        [D] Minimum mean tubeness along path segment
        [E] Lenth of path segment used to calculate "mean tubeness"
 
             - The image is preprocessed to find a "tubeness" value
               for each point in the image.  This gives a score to
               each point according to how tube-like the local shape
               of the image is.

             - The user should pick a threshold [A] for these
               tubneness values such at:

                  - Almost all of the values above that threshold
                    genuinely seem to be parts of neuron-like
                    structures.  However, one shouldn't set the
                    threshold such that there are clear paths along
                    the neurons of interest - scattered points are
                    fine, so the aim should be to minimize "false
                    positives" (i.e. points above the threshold that
                    are not plausibly part of any neuron-like
                    structure.

            - The above threshold points are put into a priority
              queue, where the most tube-like points are the first to
              be removed from the queue.

            - While there are still points in the priority queue, we
              do the following loop:

                 - Extract the most tube-like point left in the
                   priority queue.

                 - Begin a best-first search from that point.  Carry
                   on until a given number of iterations have been
                   reached [C] or a certain amount of time has been
                   reached [B].

                 - When each new point is added in the search check
                   whether it is either in a path found on a previous
                   iteration or above the tubeness threshold [A].  If
                   so, add that to a hashtable H.

                 - Once the search has terminated we first build paths
                   from the start point to each of the points we
                   recorded in the hashtable.  There should be a *lot*
                   of overlap, so while building this we:

                       - Reuse bits of paths that we've already found.
                         (Approximately described...)

                   We also record the rolling tubeness value over a
                   certain distance.

                 - Now we want to exclude any bits of paths that might
		   have big gaps in them.

                       - Delete parts of the paths where the rolling
                         average drops too low.

                 - For all of the points above the tubeness threshold
                   that we can still reach after the pruning, remove
                   them from the most-tube like priority queue.

	 */

	public void run(String arg0) {

		ImagePlus image = IJ.getImage();
		if( image == null ) {
			IJ.error("No current image for automatic tracing.");
			return;
		}

		long width = image.getWidth();
		long height = image.getHeight();
		long depth = image.getStackSize();
		
		long pointsInImage = width * height * depth;
		if( pointsInImage >= Integer.MAX_VALUE ) {
			IJ.error("This plugin currently only works with images with less that "+Integer.MAX_VALUE+" points.");
			return;
		 }

		single_pane = true;

		initialize(image);

		canvas = (AutoTracerCanvas)xy_canvas;

		autoTrace(image);
	}

	// ------------------------------------------------------------------------
	// Implement the methods in SearchProgressCallback here.
	// (Comments repeated from the interface file.)

	/* How many points have we considered? */
	
	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
                repaintAllPanes();
		// Also check whether we're over the requested number
		// of iterations or time:
		System.out.println("pointsInSearch "+(inOpen+inClosed)+" (will compare to maxNodes: "+maxNodes);
		long currentTime = System.currentTimeMillis();
		long timeSinceStarted = currentTime - timeStarted;
		System.out.println("miliseconds since start: "+timeSinceStarted+" ( maxSeconds is: "+maxSeconds+")");
		if( (inOpen + inClosed) > maxNodes || (timeSinceStarted / 1000) > maxSeconds ) {
			System.out.println("### Requesting stop...");
			ast.requestStop();
		}
	}
	
	/* Once finished is called, you should be able to get the
	 * result from whatever means you've implemented,
	 * e.g. TracerThreed.getResult() */
	
	public void finished( SearchThread source, boolean success ) {
		
	}
	
	/* This reports the current status of the thread, which may be:
	   
	   SearchThread.RUNNING
	   SearchThread.PAUSED
	   SearchThread.STOPPING
	*/
	
	public void threadStatus( SearchThread source, int currentStatus ) {

	}

	// ------------------------------------------------------------------------


}

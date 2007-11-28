/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import java.awt.Button;
import java.awt.Label;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

import ij.gui.StackWindow;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;
import ij.*;

/* This is subclassing StackWindow since I think we can probably get
 * away with just adding components to the bottom of the StackWindow. */
import java.util.ArrayList;
import java.util.Iterator;

public class ProgressWindow extends StackWindow implements ActionListener {
	
	int width;
	int height;
	int depth;
	
	DecimalFormat scoreFormatter;
	
	Name_Points plugin;
	public void setPlugin( Name_Points plugin ) {
		this.plugin = plugin;
	}
	
	ArrayList<FineTuneThread> fineTuneThreads;
	
	// Do make sure you make this big enough in the first place...
	public ProgressWindow( ImagePlus imp ) {
		this(imp, null);
		width = imp.getWidth();
		height = imp.getHeight();
		depth = imp.getStackSize();
		scoreFormatter = new DecimalFormat("0.0000");
	}
	
	Button useThis;
	Button cancel;
	Button refineBestSoFar;
	Label lowestScore;
	Label triedSoFar;
	
	RegistrationResult bestSoFar;
	
	void updateLowestScore( double s ) {
		lowestScore.setText( "Score: "+scoreFormatter.format(s) );
	}
	
	void updateTriedSoFar( int done, int outOf ) {
		triedSoFar.setText( "Seeds tried: "+done+" / "+outOf );
	}
	
	void addFineTuneThread( FineTuneThread f ) {
		fineTuneThreads.add(f);	    
	}
	
	void stopThreads() {
		
		for( Iterator<FineTuneThread> i = fineTuneThreads.iterator();
		     i.hasNext(); ) {
			FineTuneThread f = i.next();
			f.askToFinish();
		}
		
		for( Iterator<FineTuneThread> i = fineTuneThreads.iterator();
		     i.hasNext(); ) {
			FineTuneThread f = i.next();
			
			System.out.println("waiting for thread "+f.threadIndex+" to finish");
			try {
				f.join();
			} catch( InterruptedException e ) {
				System.out.println("Caught InterruptedException while joining...");
			}
			System.out.println("... done waiting for thread "+f.threadIndex);
		}	   
		
	}
	
	boolean useTheResult = true;
	
	@Override
	public void actionPerformed( ActionEvent e ) {

		Object source = e.getSource();

		System.out.println("Got event!");

		if (!(source == useThis || source == cancel || source == refineBestSoFar)) {
			return;
		}
		
		cancel.setEnabled(false);
		useThis.setEnabled(false);
		stopThreads();

		if (source == useThis) {
			useTheResult = true;
			triedSoFar.setText("Finishing...");
			plugin.fineTuneResults(bestSoFar);
		} else if (source == cancel) {
			useTheResult = false;
			triedSoFar.setText("Cancelling...");
			plugin.fineTuneResults(null);
		} else if (source == refineBestSoFar) {
			// FIXME: add this functionality...
		}

		close();
	}
	
	void startThreads() {
		for( Iterator<FineTuneThread> i = fineTuneThreads.iterator();
		     i.hasNext(); ) {
			FineTuneThread f = i.next();
			f.start();
		} 
	}
	
	public ProgressWindow(ImagePlus imp, ImageCanvas ic) {
		super( imp, ic );
                fineTuneThreads = new ArrayList<FineTuneThread>();
		useThis = new Button("Use This");
		cancel = new Button("Cancel");
		lowestScore = new Label("Score: (none yet)");
		triedSoFar = new Label("No attempts so far.");
		refineBestSoFar = new Button("Start Again From Best");
		useThis.addActionListener(this);
		cancel.addActionListener(this);
		add( useThis );
		add( cancel );
		add( lowestScore );
		add( triedSoFar );
		pack();
	}
	
	boolean notShowingBest = true;
	
	synchronized void showBest( ) {
		
                RegistrationResult r = bestSoFar;
		
		System.out.println("Updating progressDisplay with score: "+r.score);
		
		/* Instead, go through each slice and replace the
		 * values. */
		
		ImageStack existingStack = imp.getStack();
		
		int oldSlice = imp.getCurrentSlice();
		
		boolean setCentreSlice = false;
		if( existingStack.getSize() == 1 ) {
			// Then this probably is the first time
			// through, and it's nice to see the middle
			// slice (ish) for the first iterations.
			setCentreSlice = true;
		}
		
		int subtract_from_new_x = (r.overlay_width - width) / 2;
		int subtract_from_new_y = (r.overlay_height - height) / 2;
		
		for( int z = 0; z < r.overlay_depth; ++z ) {						
			
			byte [] reframedTransformedBytes = new byte[width*height];
			byte [] reframedFixedBytes = new byte[width*height];
			
			for( int y = 0; y < r.overlay_height; ++y ) {
				for( int x = 0; x < r.overlay_width; ++x ) {
					
					int reframed_x = x - subtract_from_new_x;
					int reframed_y = y - subtract_from_new_y;
					
					if( (reframed_x >= 0) && (reframed_y >= 0) &&
					    (reframed_x < width) && (reframed_y < height) ) {
						
						reframedTransformedBytes[reframed_y*width+reframed_x] =
							r.transformed_bytes[z][y*r.overlay_width+x];
						
						reframedFixedBytes[reframed_y*width+reframed_x] =
							r.fixed_bytes[z][y*r.overlay_width+x];
						
					}
				}
			}
			
			if( z < depth ) {
				// Then we're replacing a slice.
				ColorProcessor cp = (ColorProcessor)existingStack.getProcessor(z+1);
				cp.setRGB(reframedTransformedBytes,
					  reframedFixedBytes,
					  reframedTransformedBytes);
				
			} else {
				// Then we're adding a slice.
				ColorProcessor cp = new ColorProcessor(width, height);
				cp.setRGB(reframedTransformedBytes,
					  reframedFixedBytes,
					  reframedTransformedBytes);
				existingStack.addSlice("",cp);
			}
			
		}
		
		// Now in case the stack has shortened, remove the extra ones:
		
		for( int z = depth - 1; z >= r.overlay_depth; --z ) {
			existingStack.deleteSlice(z+1);
		}
		
		depth = r.overlay_depth;
		
		// Call setSlice in any case since this might cause
		// the scrollbar to update.
		
		int sliceToSet = oldSlice;
		if( setCentreSlice )
			sliceToSet = depth / 2;
		
		if( sliceToSet >= depth )
			sliceToSet = depth - 1;
		
		imp.setSlice(sliceToSet+1);
		
		if( setCentreSlice )
			pack();
		
	        imp.updateAndRepaintWindow();
		repaint();
		
		notShowingBest = false;
	}
	
        long timeLastProgressUpdate = 0;
	
	public synchronized void offerNewResult( RegistrationResult r ) {
		
		long timeCurrently = System.currentTimeMillis();
		long timeSinceLastUpdate = timeCurrently - timeLastProgressUpdate;		
		
		if( (bestSoFar == null) || (r.score < bestSoFar.score) ) {
			
                        bestSoFar = r;
                        updateLowestScore( r.score );
			notShowingBest = true;
			
			System.out.println("Found a better one: "+r.score);
			if( (timeLastProgressUpdate == 0) || timeSinceLastUpdate > 1000 ) {
				System.out.println("Yeah, it's been "+(timeSinceLastUpdate / 1000.0)+ "seconds.");
				showBest();
				timeLastProgressUpdate = timeCurrently;
			}
			
		}
		// But that might miss one, so check every 5 seconds
		// that the currently displayed on is the best.
		if( timeSinceLastUpdate > 5000 && notShowingBest ) {
			
			timeLastProgressUpdate = timeCurrently;
			showBest();
		}
		
	}
	
}

/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import java.awt.Button;
import java.awt.Label;
import java.text.DecimalFormat;

import ij.gui.StackWindow;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;
import ij.*;

/* This is subclassing StackWindow since I think we can probably get
 * away with just adding components to the bottom of the StackWindow. */

public class ProgressWindow extends StackWindow {
	
	int width;
	int height;
	int depth;

	DecimalFormat scoreFormatter;

	// Do make sure you make this big enough in the first place...
	public ProgressWindow(ImagePlus imp) {
		this(imp, null);
		width = imp.getWidth();
		height = imp.getHeight();
		depth = imp.getStackSize();
		scoreFormatter = new DecimalFormat("0.0000");
	}
	
	Button useThis;
	Button cancel;
	Label lowestScore;
	Label triedSoFar;

	RegistrationResult showingResult;
	
	void updateLowestScore( double s ) {
		lowestScore.setText( "Score: "+scoreFormatter.format(s) );
	}

	void updateTriedSoFar( int done, int outOf ) {
		triedSoFar.setText( "Seeds tried: "+done+" / "+outOf );
	}
	
	public ProgressWindow(ImagePlus imp, ImageCanvas ic) {
		super( imp, ic );
		useThis = new Button("Use This");
		cancel = new Button("Cancel");
		lowestScore = new Label("Score: (none yet)");
		triedSoFar = new Label("No attempts so far.");
		useThis.addActionListener(this);
		cancel.addActionListener(this);
		add( useThis );
		add( cancel );
		add( lowestScore );
		add( triedSoFar );
		pack();
	}
	
	void showThis( RegistrationResult r ) {

		System.out.println("Updating progressDisplay with score: "+r.score);

		showingResult = r;

		updateLowestScore( r.score );
		
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

	}
	
	
}

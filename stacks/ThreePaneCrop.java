/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import ij.*;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

import java.awt.*;
import java.awt.event.*;

/* A small dialog for confirming the region to crop and reporting
 * numerically what the current crop boundaries are. */

class CropDialog extends Dialog implements ActionListener, WindowListener {
	
	Button cropButton;
	Button cancelButton;
	
	public void windowClosing( WindowEvent e ) {
		owner.cancel();
		dispose();
	}
	
	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }    

	ThreePaneCrop owner;

	TextField x_min_field;
	TextField y_min_field;
	TextField z_min_field;

	TextField x_max_field;
	TextField y_max_field;
	TextField z_max_field;

	public CropDialog( String title, ThreePaneCrop owner ) {

		super( IJ.getInstance(), title, false );
		
		x_min_field = new TextField( "", 4 );
		y_min_field = new TextField( "", 4 );
		z_min_field = new TextField( "", 4 );
		x_max_field = new TextField( "", 4 );
		y_max_field = new TextField( "", 4 );
		z_max_field = new TextField( "", 4 );

		x_min_field.setEnabled(false);
		y_min_field.setEnabled(false);
		z_min_field.setEnabled(false);
		x_max_field.setEnabled(false);
		y_max_field.setEnabled(false);
		z_max_field.setEnabled(false);		

		addWindowListener( this );

		this.owner = owner;

		setLayout( new BorderLayout() );
		
		Panel parametersPanel = new Panel();

		parametersPanel.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 5;
		parametersPanel.add( new Label("Current Crop Boundaries (Maxima)"), c );

		c.gridwidth = 1;
		c.gridy = 1;
		c.gridx = 0; parametersPanel.add( new Label( "x from " ), c );
		c.gridx = 1; parametersPanel.add( x_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + owner.overall_min_x + ")  to "), c );
		c.gridx = 3; parametersPanel.add( x_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + owner.overall_max_x + ")"), c );

		c.gridy = 2;
		c.gridx = 0; parametersPanel.add( new Label( "y from " ), c );
		c.gridx = 1; parametersPanel.add( y_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + owner.overall_min_y + ")  to "), c );
		c.gridx = 3; parametersPanel.add( y_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + owner.overall_max_y + ")"), c );

		c.gridy = 3;
		c.gridx = 0; parametersPanel.add( new Label( "x from " ), c );
		c.gridx = 1; parametersPanel.add( z_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + owner.overall_min_z + ")  to "), c );
		c.gridx = 3; parametersPanel.add( z_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + owner.overall_max_z + ")"), c );

		Panel buttonPanel = new Panel();
		buttonPanel.setLayout( new FlowLayout() );

		cropButton = new Button("Crop");
		cropButton.addActionListener( this );
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener( this );

		buttonPanel.add( cropButton );
		buttonPanel.add( cancelButton );

		add( parametersPanel, BorderLayout.CENTER );
		add( buttonPanel, BorderLayout.SOUTH );

		pack();
		setVisible( true );
	}

	public void actionPerformed( ActionEvent e ) {
		
		Object source = e.getSource();
		
		if( source == cropButton ) {
			owner.performCrop();
			dispose();
		} else if( source == cancelButton ) {
			owner.cancel();
			dispose();
		}
	}

        public void paint(Graphics g) {
                super.paint(g);
        }

	public void updateCropBounds( int min_x, int max_x,
				      int min_y, int max_y,
				      int min_z, int max_z ) {
		
		x_min_field.setText( Integer.toString(min_x) );
		x_max_field.setText( Integer.toString(max_x) );

		y_min_field.setText( Integer.toString(min_y) );
		y_max_field.setText( Integer.toString(max_y) );

		z_min_field.setText( Integer.toString(min_z) );
		z_max_field.setText( Integer.toString(max_z) );

	}
}

public class ThreePaneCrop extends ThreePanes {
      
	public ThreePanesCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new ThreePaneCropCanvas( imagePlus, this, plane );
	}

	public void performCrop() {

		int original_width = xy.getWidth();

		int new_width = (max_x_offscreen - min_x_offscreen) + 1;
		int new_height = (max_y_offscreen - min_y_offscreen) + 1;
		
		int first_slice = min_z_offscreen + 1;
		int last_slice = max_z_offscreen + 1;

		ImageStack xy_stack=xy.getStack();
		ImageStack new_stack=new ImageStack( new_width, new_height );

		for( int slice = first_slice; slice <= last_slice; slice ++ ) {

			byte [] slice_bytes = (byte [])xy_stack.getPixels(slice);

			byte [] new_slice = new byte[new_width * new_height];
			for( int y = min_y_offscreen; y <= max_y_offscreen; ++y ) {
				System.arraycopy( slice_bytes, y * original_width + min_x_offscreen,
						  new_slice, (y - min_y_offscreen) * new_width,
						  new_width );
			}
			
			ByteProcessor bp = new ByteProcessor( new_width, new_height );
			
			bp.setPixels( new_slice );

			new_stack.addSlice( null, bp );

			IJ.showProgress( (slice - first_slice) / ((last_slice - first_slice) + 1) );
		}

		IJ.showProgress( 1 );
			       
		ImagePlus imagePlus = new ImagePlus( "cropped "+xy.getShortTitle(), new_stack );

		imagePlus.setCalibration(xy.getCalibration());
		if( xy.getProperty("Info") != null)
			imagePlus.setProperty("Info",xy.getProperty("Info"));
		xy.setFileInfo(xy.getOriginalFileInfo());

		imagePlus.show();
	}

	public void cancel() {
		closeAndReset();
	}

	int max_x_offscreen, min_x_offscreen;
	int max_y_offscreen, min_y_offscreen;
	int max_z_offscreen, min_z_offscreen;

	int overall_min_x, overall_max_x;
	int overall_min_y, overall_max_y;
	int overall_min_z, overall_max_z;
	
	public void setCropCuboid( int min_x, int max_x,
				   int min_y, int max_y,
				   int min_z, int max_z ) {

		min_x = Math.max( min_x, overall_min_x );
		min_y = Math.max( min_y, overall_min_y );
		min_z = Math.max( min_z, overall_min_z );

		max_x = Math.min( max_x, overall_max_x );
		max_y = Math.min( max_y, overall_max_y );
		max_z = Math.min( max_z, overall_max_z );

		((ThreePaneCropCanvas)xy_canvas).setCropBounds( min_x, max_x,
								min_y, max_y );

		((ThreePaneCropCanvas)xz_canvas).setCropBounds( min_x, max_x,
								min_z, max_z );

		((ThreePaneCropCanvas)zy_canvas).setCropBounds( min_z, max_z,
								min_y, max_y );

		min_x_offscreen = min_x;
		max_x_offscreen = max_x;
		
		min_y_offscreen = min_y;
		max_y_offscreen = max_y;
		
		min_z_offscreen = min_z;
		max_z_offscreen = max_z;

		dialog.updateCropBounds( min_x_offscreen,
					 max_x_offscreen,
					 min_y_offscreen,
					 max_y_offscreen,
					 min_z_offscreen,
					 max_z_offscreen );
	}

	CropDialog dialog;

	public ThreePaneCrop( ) {

	}

	public void initialize( ImagePlus imagePlus ) {

		super.initialize( imagePlus );

		overall_min_x = 0;
		overall_min_y = 0;
		overall_min_z = 0;

		overall_max_x = imagePlus.getWidth() - 1;
		overall_max_y = imagePlus.getHeight() - 1;
		overall_max_z = imagePlus.getStackSize() - 1;

		dialog = new CropDialog("Crop Options",this);

		setCropCuboid( 0, imagePlus.getWidth() - 1,
			       0, imagePlus.getHeight() - 1,
			       0, imagePlus.getStackSize() - 1 );
		
	}

	public void handleDraggedTo( int off_screen_x, int off_screen_y, int dragging, int in_plane ) {

		/* There may be one of 12 handles dragged (each corner
		   of the new cube). */

		int point[] = new int[3];
		
		findPointInStack( off_screen_x, off_screen_y, in_plane, point );

		if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_min_y = Math.min( point[1], max_y_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_max_y = Math.max( point[1], min_y_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_min_y = Math.min( point[1], max_y_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_max_y = Math.max( point[1], min_y_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_y = Math.min( point[1], max_y_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_min_y = Math.min( point[1], max_y_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_max_y = Math.max( point[1], min_y_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_y = Math.max( point[1], min_y_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, new_max_z );

		}
		
		repaintAllPanes();

	}


}

/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import stacks.ThreePanesCanvas;
import stacks.ThreePanes;

import util.Arrow;

class TracerCanvas extends ThreePanesCanvas implements KeyListener {
	
	private int maxArrows = 4;
	private Arrow[] arrows = new Arrow[maxArrows];

	boolean fillTransparent = false;

	Color transparentGreen = new Color( 0, 128, 0, 128 );
	
	public void setFillTransparent( boolean transparent ) {
		this.fillTransparent = transparent;
	}

	public void setArrow( int i, Arrow a ) {
		arrows[i] = a;
	}
	
	public Arrow getArrow( int i ) {
		return arrows[i];
	}
	
	public void unsetArrows( ) {
		for( int i = 0; i < maxArrows; ++i )
			arrows[i] = null;
	}
	
	private SimpleNeuriteTracer_ tracerPlugin;
	
	TracerCanvas( ImagePlus imp, SimpleNeuriteTracer_ plugin, int plane ) {
		super(imp,plugin,plane);
		tracerPlugin = plugin;
		// SimpleNeuriteTracer_.toastKeyListeners( IJ.getInstance(), "TracerCanvas constructor" );
		// addKeyListener( this );
		// System.out.println("Added keylistener");
	}

	private Connection unconfirmedSegment = null;
	private ArrayList< SegmentedConnection > completed = null;
	private boolean lastPathUnfinished;

	public void setPathUnfinished( boolean unfinished ) {
		this.lastPathUnfinished = unfinished;
	}

	public void setTemporaryConnection( Connection connection ) {
		this.unconfirmedSegment = connection;
	}
	
	public void setCompleted( ArrayList< SegmentedConnection > completed ) {
		this.completed = completed;
	}

	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();
		int flags = e.getModifiers();

		/*
		System.out.println("keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
				   + ") keyChar=\"" + keyChar + "\" (" + (int)keyChar + ") "
				   + KeyEvent.getKeyModifiersText(flags));
		*/
				   
		if( keyChar == 't' || keyChar == 'T' ) {

			// System.out.println( "Yes, running testPathTo" );
			tracerPlugin.testPathTo( last_x_in_pane, last_y_in_pane, plane );

		} else if( keyChar == 'y' || keyChar == 'Y' ) {

			// System.out.println( "Yes, running confirmPath" );
			tracerPlugin.confirmTemporary( );

		} else if( keyChar == 'n' || keyChar == 'N' ) {

			// System.out.println( "Yes, running cancelPath+" );
			tracerPlugin.cancelTemporary( );

		} else if( keyChar == 'f' || keyChar == 'F' ) {

			// System.out.println( "Finalizing that path" );
			tracerPlugin.finishedPath( );

		} else if( keyChar == 'v' || keyChar == 'V' ) {

			// System.out.println( "View paths as a stack" );
			tracerPlugin.makePathVolume( );

		} else if( keyChar == '5' ) {

			just_near_slices = ! just_near_slices;

		} else if( keyCode == KeyEvent.VK_SHIFT ) {
			
			tracerPlugin.mouseMovedTo( last_x_in_pane, last_y_in_pane, plane, true );
 
		}

		e.consume();
	}
	
	boolean just_near_slices = false;

	public void keyReleased(KeyEvent e) {}
	
	public void keyTyped(KeyEvent e) {}	

	/* Keep another Graphics for double-buffering... */

	private int backBufferWidth;
	private int backBufferHeight;

	private Graphics backBufferGraphics;
	private Image backBufferImage;

	private void resetBackBuffer() {

		if(backBufferGraphics!=null){
			backBufferGraphics.dispose();
			backBufferGraphics=null;
		}

		if(backBufferImage!=null){
			backBufferImage.flush();
			backBufferImage=null;
		}
		
		backBufferWidth=getSize().width;
		backBufferHeight=getSize().height;

		backBufferImage=createImage(backBufferWidth,backBufferHeight);
	        backBufferGraphics=backBufferImage.getGraphics();
	}

	public void paint(Graphics g) {
		
		if(backBufferWidth!=getSize().width ||
		   backBufferHeight!=getSize().height ||
		   backBufferImage==null ||
		   backBufferGraphics==null)
			resetBackBuffer();
		
		super.paint(backBufferGraphics);
		drawOverlay(backBufferGraphics);
		g.drawImage(backBufferImage,0,0,this);
	}
	
	public void mouseMoved( MouseEvent e ) {
		
		last_x_in_pane = offScreenX(e.getX());
		last_y_in_pane = offScreenY(e.getY());
		
		boolean shift_key_down = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;

		tracerPlugin.mouseMovedTo( last_x_in_pane, last_y_in_pane, plane, shift_key_down );
		
	}
	
	int last_x_in_pane;
	int last_y_in_pane;

	public void mouseClicked( MouseEvent e ) {
		
		// IJ.showStatus( "click at " + System.currentTimeMillis() + ": " + e );
		// IJ.showStatus( "click at (" + e.getX() + "," + e.getY() + ")" );
		
		if( tracerPlugin.setupLog ) {
			
			int x = offScreenX(e.getX());
			int y = offScreenY(e.getY());

			int z = imp.getCurrentSlice() - 1;
			
			tracerPlugin.setPositionAllPanes( x, y, z );

			/* FIXME: put this back at some point

			EigenResultsDouble er;
			try {
				er = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 2, 1.0f, false );
			} catch( Exception exception ) {
				IJ.error("Caught an exception while calculating the Hessian: "+exception);
				return;
			}
			
			tracerPlugin.logPosition( x, y, z, er.sortedValues[0], er.sortedValues[1], er.sortedValues[2] );

			*/
			
		} else if( tracerPlugin.setupEv ) {
			
			/* FIXME: put this back at some point

			int x = offScreenX(e.getX());
			int y = offScreenX(e.getY());
			int z = imp.getCurrentSlice() - 1;

			EigenResultsDouble er_2_around;
			EigenResultsDouble er_1_around;

			try {
				er_2_around = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 2, 1.0f, false );
				er_1_around = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 1, 1.0f, false );
			} catch( Exception exception ) {
				IJ.error("Caught an exception while calculating the Hessian: "+exception);
				return;
			}			

			Arrow arrow_1_around = new Arrow( Color.ORANGE, x, y, z,
							  er_1_around.sortedVectors[0][0],
							  er_1_around.sortedVectors[0][1],
							  er_1_around.sortedVectors[0][2],
							  100 );
			
			Arrow arrow_2_around = new Arrow( Color.RED, x, y, z,
							  er_2_around.sortedVectors[0][0],
							  er_2_around.sortedVectors[0][1],
							  er_2_around.sortedVectors[0][2],
							  100 );
			
			tracerPlugin.setArrow( 0, arrow_2_around );
			tracerPlugin.setArrow( 1, arrow_1_around );

			*/

		} else if( tracerPlugin.setupTrace ) {
			
			boolean join = e.isShiftDown();

			tracerPlugin.clickForTrace( offScreenX(e.getX()), offScreenY(e.getY()), plane, join );
			// tracerPlugin.startPath( offScreenX(e.getX()), offScreenY(e.getY()), plane, join );
			
		} else {
			
			IJ.error( "BUG: No operation chosen" );
			
		}
	}

	boolean dumpDrawnPoints = true;

	protected void drawPointsInArray( short [] points, Graphics g, Color c, boolean verbose ) {

		int currentSlice = imp.getCurrentSlice() - 1;
		g.setColor( c );
		
		if( points != null ) {

			/*
			if( plane == ThreePanes.XY_PLANE ) {
				System.out.println("drawPointsInArray drawing stuff in XY");
				Exception e = new Exception();
				e.printStackTrace();
			}
			*/
			
			long beforeLoop = System.currentTimeMillis();

			int n = points.length / 3;
			
			for( int i = 0; i < n; ++i ) {
				if( currentSlice == points[ 3*i + (2 - plane) ] ) {

					// Then draw that point.
					
					int x = points[ 3*i ];
					int y = points[ 3*i + 1 ];
					int z = points[ 3*i + 2 ];

					if( plane == ThreePanes.XY_PLANE ) {
						int sx = screenX(x);
						int sx_pixel_size = screenX(x+1) - sx;
						if( sx_pixel_size < 1 ) sx_pixel_size = 1;
						int sy = screenY(y);
						int sy_pixel_size = screenY(y+1) - sy;
						if( sy_pixel_size < 1 ) sy_pixel_size = 1;
						g.fillRect( screenX(x), screenY(y), sx_pixel_size, sy_pixel_size );
					} else if( plane == ThreePanes.XZ_PLANE ) {
						int sx = screenX(x);
						int sx_pixel_size = screenX(x+1) - sx;
						if( sx_pixel_size < 1 ) sx_pixel_size = 1;
						int sy = screenY(z);
						int sy_pixel_size = screenY(z+1) - sy;
						if( sy_pixel_size < 1 ) sy_pixel_size = 1;
						g.fillRect( screenX(x), screenY(z), sx_pixel_size, sy_pixel_size );
					} else if( plane == ThreePanes.ZY_PLANE ) {
						int sx = screenX(z);
						int sx_pixel_size = screenX(z+1) - sx;
						if( sx_pixel_size < 1 ) sx_pixel_size = 1;
						int sy = screenY(y);
						int sy_pixel_size = screenY(y+1) - sy;
						if( sy_pixel_size < 1 ) sy_pixel_size = 1;
						g.fillRect( screenX(z), screenY(y), sx_pixel_size, sy_pixel_size );
					}
				}
			}

			/* if( verbose && (ThreePanes.XY_PLANE == plane) )
				 System.out.println( "Drawing points in the XY plane took: " + ((float)(System.currentTimeMillis()-beforeLoop)/1000.0f) ); */

		}
	}

	protected void drawOverlay(Graphics g) {

		for( int i = maxArrows - 1; i >= 0; --i ) {
			// for( int i = 0; i < maxArrows; ++i ) {
			
			Arrow a = arrows[i];
			if( a == null )
				continue;
			
			g.setColor(a.c);
			
			if( plane == ThreePanes.XY_PLANE ) {
				g.drawLine( (int)( a.start_x ),
					    (int)( a.start_y ),
					    (int)( a.start_x + a.length * a.vx ),
					    (int)( a.start_y + a.length * a.vy ) );
			} else if( plane == ThreePanes.XZ_PLANE ) {
				g.drawLine( (int)( a.start_x ),
					    (int)( a.start_z ),
					    (int)( a.start_x + a.length * a.vx ),
					    (int)( a.start_z + a.length * a.vz ) );
			} else if( plane == ThreePanes.ZY_PLANE ) {
				g.drawLine( (int)( a.start_z ),
					    (int)( a.start_y ),
					    (int)( a.start_z + a.length * a.vz ),
					    (int)( a.start_y + a.length * a.vy ) );
			}
		}
		
		int current_z = -1;

		if( plane == ThreePanes.XY_PLANE ) {
			current_z = imp.getCurrentSlice() - 1;
		}

		synchronized(tracerPlugin.nonsense) {
			// Plot the tracing progress:
			drawPointsInArray( tracerPlugin.currentOpenBoundaryPoints, g, Color.CYAN, false );
		}

		synchronized(tracerPlugin.nonsense) {
			// Plot the filler progress:
			if( (tracerPlugin.currentSubthresholdFillerPoints != null) && (tracerPlugin.currentSubthresholdFillerPoints.length > 0) ) {
				drawPointsInArray( tracerPlugin.currentSubthresholdFillerPoints, g, fillTransparent ? transparentGreen : Color.GREEN, true );
				dumpDrawnPoints = false;
			}
		}

		if( completed != null ) {
			synchronized(tracerPlugin) {
				// System.out.println("Have some completed paths to draw.");
				int paths = completed.size();
				// System.out.println("Paths to draw: "+paths);
				for( int i = 0; i < paths; ++i ) {
					// System.out.println("Drawing path: "+i);
					Color color = Color.MAGENTA;
					if( (i == (paths - 1)) && lastPathUnfinished ) {
						color = Color.RED;
					}
					if( tracerPlugin.pathSelected(i) ) {
						color = Color.GREEN;
					}
					SegmentedConnection s = (SegmentedConnection)completed.get(i);
					int segments_in_path = s.connections.size();
					// System.out.println(""+segments_in_path+" segments in that path");
					for( int j = 0; j < segments_in_path; ++j ) {
						// System.out.println("drawing segment "+j);
						Connection connection = (Connection)s.connections.get(j);
						// FIXME: npe here?
						if( connection == null ) {
							System.out.println("BUG: connection is null");
						}
						if( plane == ThreePanes.XY_PLANE ) {
							if( just_near_slices )
								connection.drawConnectionAsPoints( this, g, color, plane, current_z, 2 );
							else
								connection.drawConnectionAsPoints( this, g, color, plane );
						} else
							connection.drawConnectionAsPoints( this, g, color, plane );
							
					}
				}
			}
		}

		if( unconfirmedSegment != null ) {
			synchronized(tracerPlugin) {
				unconfirmedSegment.drawConnectionAsPoints( this, g, Color.BLUE, plane );
			}
		}

		super.drawOverlay(g);
				
	}
	
	private double cost( int from_x,
			     int from_y,
			     int from_z,
			     int to_x,
			     int to_y,
			     int to_z ) {
		
		double xdiff = to_x - from_x;
		double ydiff = to_y - from_y;
		double zdiff = to_z - from_z;
		
		double distance = Math.sqrt( xdiff * xdiff +
					     ydiff * ydiff + 
					     zdiff * zdiff );
		
		if( distance > 1.001 )        
			throw new RuntimeException( "Currently can only calculate " +
						    "cost between this pixel and " +
						    "an adjacent one." );
		
		
		
		
		return -1;
		
	}
	
	Connection orangePath;
	
	public void setConnection( Connection c )  {
		orangePath = c;
	}

}

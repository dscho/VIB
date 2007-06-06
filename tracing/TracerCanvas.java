/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import tracing.NeuriteTracer_;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import stacks.ThreePanesCanvas;
import stacks.ThreePanes;

class Arrow {
	
	public Arrow( Color c,
		      double start_x, double start_y, double start_z,
		      double vx, double vy, double vz,
		      int length ) {
		
		this.c = c;
		this.start_x = start_x;
		this.start_y = start_y;
		this.start_z = start_z;
		this.vx = vx;
		this.vy = vy;
		this.vz = vz;
		this.length = length;
	}
	
	public Color c;
	
	public double start_x;
	public double start_y;
	public double start_z;
	
	public double vx;
	public double vy;
	public double vz;
	
	public int length;
}

class TracerCanvas extends ThreePanesCanvas implements KeyListener {
	
	private int maxArrows = 4;
	private Arrow[] arrows = new Arrow[maxArrows];
	
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
	
	private NeuriteTracer_ tracerPlugin;
	
	TracerCanvas( ImagePlus imp, NeuriteTracer_ plugin, int plane ) {
		super(imp,plugin,plane);
		tracerPlugin = plugin;
		// NeuriteTracer_.toastKeyListeners( IJ.getInstance(), "TracerCanvas constructor" );
		// addKeyListener( this );
		// System.out.println("Added keylistener");
	}

	private Connection unconfirmedSegment = null;
	private ArrayList< SegmentedConnection > completed = null;
	private boolean lastPathUnfinished;

	public void setPathUnfinished( boolean unfinished ) {
		this.lastPathUnfinished = unfinished;
	}

	public void setTemporaryPath( Connection path ) {
		this.unconfirmedSegment = path;
	}
	
	public void setCompleted( ArrayList< SegmentedConnection > completed ) {
		this.completed = completed;
	}

	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();
		int flags = e.getModifiers();
		System.out.println("keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
				   + ") keyChar=\"" + keyChar + "\" (" + (int)keyChar + ") "
				   + KeyEvent.getKeyModifiersText(flags));

		if( keyChar == 't' || keyChar == 'T' ) {

			System.out.println( "Yes, running testPathTo" );
			tracerPlugin.testPathTo( last_x_in_pane, last_y_in_pane, plane );

		} else if( keyChar == 'y' || keyChar == 'Y' ) {

			System.out.println( "Yes, running confirmPath" );
			tracerPlugin.confirmPath( );

		} else if( keyChar == 'n' || keyChar == 'N' ) {

			System.out.println( "Yes, running cancelPath+" );
			tracerPlugin.cancelPath( );

		} else if( keyChar == 'f' || keyChar == 'F' ) {

			System.out.println( "Finalizing that path" );
			tracerPlugin.finishedPath( );

		} else if( keyChar == 'v' || keyChar == 'V' ) {

			System.out.println( "View paths as a stack" );
			tracerPlugin.makePathVolume( );

		} else if( keyChar == '5' ) {

			just_near_slices = ! just_near_slices;

		}

		e.consume();
	}
	
	boolean just_near_slices = false;

	public void keyReleased(KeyEvent e) {}
	
	public void keyTyped(KeyEvent e) {}
	
	public void paint(Graphics g) {
		super.paint(g);
		drawOverlay(g);
	}
	
	public void mouseMoved( MouseEvent e ) {
		
		last_x_in_pane = offScreenX(e.getX());
		last_y_in_pane = offScreenY(e.getY());
		
		tracerPlugin.mouseMovedTo( last_x_in_pane, last_y_in_pane, plane );
		
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
			
			EigenResultsDouble er = tracerPlugin.analyzeAtPoint( x, y, z, 2, false );
			
			tracerPlugin.logPosition( x, y, z, er.sortedValues[0], er.sortedValues[1], er.sortedValues[2] );
			
		} else if( tracerPlugin.setupEv ) {
			
			int x = offScreenX(e.getX());
			int y = offScreenX(e.getY());
			int z = imp.getCurrentSlice() - 1;
			
			EigenResultsDouble er_2_around = tracerPlugin.analyzeAtPoint( x, y, z, 2, false );
			EigenResultsDouble er_1_around = tracerPlugin.analyzeAtPoint( x, y, z, 1, false );
			
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
			
		} else if( tracerPlugin.setupTrace ) {
			
			boolean join = e.isShiftDown();

			tracerPlugin.startPath( offScreenX(e.getX()), offScreenY(e.getY()), plane, join );
			
		} else {
			
			IJ.error( "BUG: No operation chosen" );
			
		}
	}

/*
	boolean arrow_set = false;
	
	public void setArrow( int start_x, int start_y, int start_z,
			      int v_x, int v_y, int v_z ) {
		
		this.start_x = start_x;
		this.start_y = start_y;
		this.start_z = start_z;
		
		this.length = Math.sqrt( v_x * v_x + v_y * v_y + v_z * v_z );
                
		this.v_x = v_x / length;
		this.v_y = v_y / length;
		this.v_z = v_z / length;
		
		arrow_set = true;
	}
	
	public void unsetArrow(  ) {
		arrow_set = false;
	}
*/
	
	/*
	double start_x;
	double start_y;
	double start_z;
	
	double v_x;
	double v_y;
	double v_z;
	
	double length;
	*/
	
	protected void drawOverlay(Graphics g) {

		super.drawOverlay(g);
		
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
		
		/*
		if( orangePath != null ) {
			orangePath.drawConnectionAsPoints( g, java.awt.Color.RED, plane );
			// orangePath.drawConnectionAsPoints( g, java.awt.Color.ORANGE, plane );
			// orangePath.drawConnection( g, java.awt.Color.ORANGE, plane );
		}
		*/

		int current_z = -1;

		if( plane == ThreePanes.XY_PLANE ) {
			current_z = imp.getCurrentSlice() - 1;
		}

		synchronized(tracerPlugin.nonsense) {

			// System.out.println("Considering nonsense for plane: "+plane);

			short [] boundaryPoints = tracerPlugin.currentOpenBoundaryPoints;
			int currentSlice = imp.getCurrentSlice() - 1;
			g.setColor( Color.CYAN );
			if( boundaryPoints != null ) {

				int points = boundaryPoints.length / 3;
				
				for( int i = 0; i < points; ++i ) {
					if( currentSlice == boundaryPoints[ 3*i + (2 - plane) ] ) {

						int x = boundaryPoints[ 3*i ];
						int y = boundaryPoints[ 3*i + 1 ];
						int z = boundaryPoints[ 3*i + 2 ];
						// Then draw that point.
						if( plane == ThreePanes.XY_PLANE ) {
							g.fillRect( screenX(x), screenY(y), 1, 1 );
						} else if( plane == ThreePanes.XZ_PLANE ) {
							g.fillRect( screenX(x), screenY(z), 1, 1 );
						} else if( plane == ThreePanes.ZY_PLANE ) {
							g.fillRect( screenX(z), screenY(y), 1, 1 );
						}
						
					}
				}
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
					if( i == (paths - 1) ) {
						color = Color.RED;
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
			
	static public void drawCrossHairs( Graphics g, Color c, int x, int y ) {
		g.setColor( c );
		int hairLength = 8;
		g.drawLine( x, y + 1, x, y + (hairLength - 1) );
		g.drawLine( x, y - 1, x, y - (hairLength - 1) );
		g.drawLine( x + 1, y, x + (hairLength - 1), y );
		g.drawLine( x - 1, y, x - (hairLength - 1), y );
	}
	
}

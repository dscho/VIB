/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".
  
  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.
  
  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;
import ij.IJ;

import stacks.ThreePanes;

import java.util.*;
import java.awt.Color;
import java.awt.Graphics;

/* This is the thread that explores the image using a variety of
   strategies, for example to trace tubular structures or surfaces. */

public abstract class SearchThread extends Thread {
	
	boolean verbose = Simple_Neurite_Tracer.verbose;
	
	public static final byte OPEN_FROM_START   = 1;
	public static final byte CLOSED_FROM_START = 2;
	public static final byte OPEN_FROM_GOAL    = 3;
	public static final byte CLOSED_FROM_GOAL  = 4;
	public static final byte FREE              = 5; // Indicates that this node isn't in a list yet...
	
	/* This calculates the cost of moving to a new point in the
	 * image.  This does not take into account the distance to
	 * this new point, only the value at it.  This will be
	 * post-multiplied by the distance from the last point.  So,
	 * if you want to take into account the curvature of the image
	 * at that point then you should do so in this method. */
	
	// The default implementation does a simple reciprocal of the
	// image value:
	
	protected double costMovingTo( int new_x, int new_y, int new_z ) {
		
		int value_at_new_point = slices_data[new_z][new_y*width+new_x] & 0xFF;
		
		if( value_at_new_point == 0 )
			return 2.0;
		else
			return 1.0 / value_at_new_point;
		
	}
	
	/* Use this for doing special progress updates, beyond what
	 * SearchProgressCallback provides. */
	
	protected void reportPointsInSearch( ) {
		for (Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext();) {
			SearchProgressCallback progress = j.next();
			progress.pointsInSearch(this, open_from_start.size() + (bidirectional ? open_from_goal.size() : 0), closed_from_start.size() + (bidirectional ? closed_from_goal.size() : 0));
		}
	}
	
	
	/* This is a factory method for creating specialized search
	 * nodes, subclasses of SearchNode: */
	
	protected SearchNode createNewNode( int x, int y, int z, float g, float h,
					    SearchNode predecessor,
					    byte searchStatus ) {
		return new SearchNode( x, y, z, g, h, predecessor, searchStatus );
	}
	
	/* This is called if the goal has been found in the search.
	 * If your search has no defined goal, then this will never be
	 * called, so don't bother to override it. */
	
	protected void foundGoal( Path pathToGoal ) {
		/* A dummy implementation that does nothing with this
		 * exciting news. */
	}
	
	/* */
	
	protected boolean atStart( int x, int y, int z ) {
		return false;
	}
	
	protected boolean atGoal( int x, int y, int z ) {
		return false;
	}

	Color openColor;
	Color closedColor;
	float drawingThreshold;

	void setDrawingColors( Color openColor, Color closedColor ) {
		this.openColor = openColor;
		this.closedColor = closedColor;
	}

	void setDrawingThreshold( float threshold ) {
		this.drawingThreshold = threshold;
	}
	
	/* If you need to force the distance between two points to
	 * always be greater than some value (e.g. to make your A star
	 * heuristic valid or something, then you should override this
	 * method and return that value. */
	
	protected double minimumCostPerUnitDistance( ) {
		return 0.0;
	}
	
	protected double minimum_cost_per_unit_distance;
	
	byte [][] slices_data;	

	ImagePlus imagePlus;
	
        double x_spacing;
        double y_spacing;
        double z_spacing;
	
	String spacing_units;
	
        int width;
        int height;
        int depth;
	
	/* The search may only be bidirectional if definedGoal is true */
	
	boolean bidirectional;
	
	/* If there is no definedGoal then the search is just
	   Dijkstra's algorithm (h = 0 in the A* search algorithm. */
	
	boolean definedGoal;
	
	boolean startPaused;
	
        int timeoutSeconds;
        long reportEveryMilliseconds;
        long lastReportMilliseconds;
	
	ArrayList< SearchProgressCallback > progressListeners;
	
	public void addProgressListener( SearchProgressCallback callback ) {
		progressListeners.add( callback );
	}
	
	/* The thread can be in one of these states:
	   
	   - STOPPING: the thread cannot be used again
	   - PAUSED: run() hasn't been started yet or the thread is paused
	   - RUNNING: the run method is going and the thread is unpaused
        */
	
	/* This can only be changed in a block synchronized on this object */
	
	private int threadStatus = PAUSED;
	
	public static final int RUNNING  = 0;
	public static final int PAUSED   = 1;
	public static final int STOPPING = 2;
	
	public int getThreadStatus( ) {
		return threadStatus;
	}
	
	// Safely stops the thread (for discarding the object.)
	
        public void requestStop( ) {
		if (verbose) System.out.println("requestStop called, about to enter synchronized");
		synchronized (this) {
			if (verbose) System.out.println("... entered synchronized");
			if( threadStatus == PAUSED ) {
				if (verbose) System.out.println("was paused so interrupting");
				this.interrupt();
				if (verbose) System.out.println("done interrupting");
			}
			threadStatus = STOPPING;
			reportThreadStatus();
			if (verbose) System.out.println("... leaving synchronized");
		}
		if (verbose) System.out.println("requestStop finished (threadStatus now "+threadStatus+")");
        }

	/** Override this method if you want to find out when a point
	 * was first discovered:
	 */
	protected void addingNode( SearchNode n ) { }
	
	public void reportThreadStatus( ) {
		for( Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			SearchProgressCallback progress = j.next();
			progress.threadStatus( this, threadStatus );
		}
	}
	
	public void reportFinished( boolean success ) {
		for( Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			SearchProgressCallback progress = j.next();
			progress.finished( this, success );
		}
	}
	
	// Toggles the paused or unpaused status of the thread.
	
        public void pauseOrUnpause( ) {
		// Toggle the paused status...
		if (verbose) System.out.println("pauseOrUnpause called, about to enter synchronized");
		synchronized (this) {
			if (verbose) System.out.println("... entered synchronized");
			switch( threadStatus) {
			case PAUSED:
				if (verbose) System.out.println("paused, going to switch to running - interrupting first");
				this.interrupt();
				if (verbose) System.out.println("finished interrupting");
				threadStatus = RUNNING;
				break;
			case RUNNING:
				if (verbose) System.out.println("running, going to switch to paused");
				threadStatus = PAUSED;
				break;
			default:
				// Do nothing, we're actually stopping anyway...
			}
			reportThreadStatus();
			if (verbose) System.out.println("... leaving synchronized");
		}
		if (verbose) System.out.println("pauseOrUnpause finished");
        }
	
        /* If you specify 0 for timeoutSeconds then there is no timeout. */
	
        public SearchThread( ImagePlus imagePlus,
			     boolean bidirectional,
			     boolean definedGoal,
			     boolean startPaused,
			     int timeoutSeconds,
			     long reportEveryMilliseconds ) {
		
                this.imagePlus = imagePlus;
		
		this.bidirectional = bidirectional;
		this.definedGoal = definedGoal;
		this.startPaused = startPaused;
		
		
                width = imagePlus.getWidth();
                height = imagePlus.getHeight();
                depth = imagePlus.getStackSize();		
		
		{
			ImageStack s = imagePlus.getStack();
			slices_data = new byte[depth][];
			for( int z = 0; z < depth; ++z ) {
				slices_data[z] = (byte []) s.getPixels( z + 1 );
			}
		}
		
		Calibration calibration = imagePlus.getCalibration();
		
		x_spacing = calibration.pixelWidth;
		y_spacing = calibration.pixelHeight;
		z_spacing = calibration.pixelDepth;
		spacing_units = calibration.getUnit();
		
		if( (x_spacing == 0.0) ||
		    (y_spacing == 0.0) ||
		    (z_spacing == 0.0) ) {
			
			IJ.error( "SearchThread: One dimension of the calibration information was zero: (" +
				  x_spacing + "," + y_spacing + "," + z_spacing + ")" );
			return;
			
		}
		
                this.timeoutSeconds = timeoutSeconds;
                this.reportEveryMilliseconds = reportEveryMilliseconds;
		
                closed_from_start = new PriorityQueue<SearchNode>();
                open_from_start = new PriorityQueue<SearchNode>();
		if( bidirectional ) {
			closed_from_goal = new PriorityQueue<SearchNode>();
			open_from_goal = new PriorityQueue<SearchNode>();
		}
		
		nodes_as_image = new SearchNode[depth][];
		
                minimum_cost_per_unit_distance = minimumCostPerUnitDistance();
		
		progressListeners = new ArrayList< SearchProgressCallback >();
		
        }
	
	PriorityQueue<SearchNode> closed_from_start;
	PriorityQueue<SearchNode> open_from_start;
	
	// The next two are null if the search is not bidirectional
	PriorityQueue<SearchNode> closed_from_goal;
	PriorityQueue<SearchNode> open_from_goal;
	
	SearchNode [][] nodes_as_image;
	
	@Override
        public void run( ) {
		
		if (verbose) System.out.println("New SearchThread running!");
		if (verbose) System.out.println("... with " + open_from_start.size() + " open nodes at the start" );
		if (verbose) System.out.println(" ... and " + closed_from_start.size() + " closed nodes at the start" );
		if( bidirectional ) {
			if (verbose) System.out.println("... with " + open_from_goal.size() + " open nodes at the goal" );
			if (verbose) System.out.println(" ... and " + closed_from_goal.size() + " closed nodes at the goal" );
		} else
			if (verbose) System.out.println(" ... unidirectional search");
		
		
		if( startPaused ) {
			if (verbose) System.out.println("... was asked to start it in the paused state.");
		} else {
			if (verbose) System.out.println("... was asked to start it unpaused.");
		}
		
		if( startPaused ) {
			synchronized (this) {
				threadStatus = PAUSED;
				reportThreadStatus();
			}
		} else {
			synchronized (this) {
				threadStatus = RUNNING;
				reportThreadStatus();
			}
		}
		
                long started_at = lastReportMilliseconds = System.currentTimeMillis();
		
		int loops_at_last_report = 0;
                int loops = 0;
		
		/*
		  We maintain the list of nodes in the search in a
		  couple of different data structures here, which is
		  bad for memory usage but good for the speed of the
		  search.  
		  
		  As well as keeping the nodes in priority lists, we
		  keep them in a set of arrays that are indexed in the
		  same way as voxels in the image.
		*/
		
                while( (open_from_start.size() > 0) ||
		       (bidirectional && (open_from_goal.size() > 0)) ) {
			
			if( threadStatus == STOPPING ) {
				reportThreadStatus();
                                setExitReason(CANCELLED);
                                reportFinished(false);
				return;
                        } else if( threadStatus == PAUSED ) {				
                                try {
                                        reportThreadStatus();
                                        Thread.sleep(4000);
                                } catch( InterruptedException e ) {
                                }
                        }
			
			// We only check every thousandth loop for
			// whether we should report the progress, etc.
			
                        if( 0 == (loops % 1000) ) {
				
                                long currentMilliseconds = System.currentTimeMillis();
		
                                long millisecondsSinceStart = currentMilliseconds - started_at;

                                if( (timeoutSeconds > 0) && (millisecondsSinceStart > (1000 * timeoutSeconds)) ) {
                                        if (verbose) System.out.println("Timed out...");
                                        setExitReason(TIMED_OUT);
                                        reportFinished( false );            
                                        return;
                                }
				
				long since_last_report = currentMilliseconds - lastReportMilliseconds;
				
                                if( (reportEveryMilliseconds > 0) && (since_last_report > reportEveryMilliseconds ) ) {
					
					int loops_since_last_report = loops - loops_at_last_report;
					if (verbose) System.out.println( "milliseconds per loop: " +
									 ( since_last_report / (double)loops_since_last_report ) );
					
                                        reportPointsInSearch();
					
					loops_at_last_report = loops;
                                }
                        }
			
                        boolean verbose = false;
			
                        SearchNode p = null;
                        SearchNode q = null;
			
                        if( open_from_start.size() > 0 ) {
				
                                // p = get_highest_priority( open_from_start, open_from_start_hash );
                                p = open_from_start.poll();
				nodes_as_image[p.z][p.y*width+p.x] = null;
                        }
			
                        if( bidirectional && (open_from_goal.size() > 0) ) {
				
                                // q = get_highest_priority( open_from_goal, open_from_goal_hash );
                                q = open_from_goal.poll();
				nodes_as_image[q.z][q.y*width+q.x] = null;
                        }
			
                        // Has the route from the start found the goal?
			
                        if( definedGoal && (p != null) && atGoal( p.x, p.y, p.z ) ) {
                                if (verbose) System.out.println( "Found the goal! (from start to end)" );
				foundGoal( p.asPath() );
                                setExitReason(SUCCESS);
                                reportFinished( true );
                                return;
                        }
			
                        // Has the route from the goal found the start?
			
                        if( bidirectional && definedGoal && (q != null) && atStart( q.x, q.y, q.z ) ) {
                                if (verbose) System.out.println( "Found the goal! (from end to start)" );
				foundGoal( q.asPathReversed() );
                                setExitReason(SUCCESS);
                                reportFinished( true );
                                return;
                        }
			
                        if( verbose ) {
				
                                if (verbose) System.out.println( "at loop: " + loops + " open_from_start: " +
								 open_from_start.size() + " closed_from_start:" +
								 closed_from_start.size() );
				
                                if (verbose) System.out.println( "         " + loops + " open_from_goal: " +
								 open_from_goal.size() + " closed_from_goal: " +
								 closed_from_goal.size() );
				
                        }
			
			/* To save some code duplication, we have a
			   loop that we go through exactly twice; the
			   first time deals with p (searching from the
			   start) and the second time deals with q
			   (searching from the goal). */
			
			for( int i = 0; i < 2; ++i ) {
				
				// "e" for "existingNode"
				SearchNode e = (i == 0) ? p : q;
				
				if( e == null )
					continue;
				
				if( i == 1 && ! bidirectional )
					break;
				
				if( i == 0 ) {
					e.searchStatus = CLOSED_FROM_START;
					closed_from_start.add( e );
					nodes_as_image[e.z][e.y*width+e.x] = e;
				} else {
					e.searchStatus = CLOSED_FROM_GOAL;
					closed_from_goal.add( e );
					nodes_as_image[e.z][e.y*width+e.x] = e;
				}
				
                                // Now look at the neighbours of e.  We're going to consider
                                // the 26 neighbours in 3D.
				
				for( int zdiff = -1; zdiff <= 1; zdiff++ ) {
					
					int new_z = e.z + zdiff;
					if( new_z < 0 || new_z >= depth )
						continue;
					
					if( nodes_as_image[new_z] == null ) {
						nodes_as_image[new_z] = new SearchNode[width*height];
					}
					
					for( int xdiff = -1; xdiff <= 1; xdiff++ )
						for( int ydiff = -1; ydiff <= 1; ydiff++ ) {
							
                                                        if( (xdiff == 0) && (ydiff == 0) && (zdiff == 0) )
                                                                continue;
							
							int new_x = e.x + xdiff;
							int new_y = e.y + ydiff;
							
                                                        if( new_x < 0 || new_x >= width )
                                                                continue;
							
                                                        if( new_y < 0 || new_y >= height )
                                                                continue;
							
                                                        double xdiffsq = (xdiff * x_spacing) * (xdiff * x_spacing);
                                                        double ydiffsq = (ydiff * y_spacing) * (ydiff * y_spacing);
                                                        double zdiffsq = (zdiff * z_spacing) * (zdiff * z_spacing);
							
                                                        float h_for_new_point = estimateCostToGoal( new_x, new_y, new_z, i );
							
                                                        double cost_moving_to_new_point = costMovingTo( new_x, new_y, new_z );
							if( cost_moving_to_new_point < minimum_cost_per_unit_distance ) {
								cost_moving_to_new_point = minimum_cost_per_unit_distance;
							}
							
                                                        float g_for_new_point = (float) ( e.g + Math.sqrt( xdiffsq + ydiffsq + zdiffsq ) * cost_moving_to_new_point );
							
                                                        float f_for_new_point = h_for_new_point + g_for_new_point;
							
							SearchNode newNode = createNewNode( new_x, new_y, new_z,
											    g_for_new_point, h_for_new_point,
											    e, FREE );
							
							
							// Is this newNode really new?
							SearchNode alreadyThere = nodes_as_image[new_z][new_y*width+new_x];
							
							if( alreadyThere == null ) {
								
								if( i == 0 ) {
									newNode.searchStatus = OPEN_FROM_START;
									open_from_start.add( newNode );
								} else {
									newNode.searchStatus = OPEN_FROM_GOAL;
									open_from_goal.add( newNode );
								}
								addingNode( newNode );
								nodes_as_image[new_z][new_y*width+new_x] = newNode;
								
							} else {
								
								if( bidirectional ) {
									
									Path result = null;
									boolean done = false;
									
									// If either of the next two if conditions are true
									// then we've finished.								       	
									
									if( (i == 0) && ((alreadyThere.searchStatus == OPEN_FROM_GOAL) ||
											 (alreadyThere.searchStatus == CLOSED_FROM_GOAL)) ) {
										
										if (verbose) System.out.println("Trying to add a new node from start, found a node in the goal search already there.");
										result = e.asPath();
                                                                                if (verbose) System.out.println("e.asPath() is: "+e.asPath());
                                                                                Path fromGoalReversed = alreadyThere.asPathReversed();
                                                                                if (verbose) System.out.println("fromGoalReversed is: "+fromGoalReversed);
										result.add( fromGoalReversed );
                                                                                if (verbose) System.out.println("added, that is: "+result);
										done = true;
										
									} else if( (i == 1) && ((alreadyThere.searchStatus == OPEN_FROM_START) ||
												(alreadyThere.searchStatus == CLOSED_FROM_START)) ) {
										
										
										if (verbose) System.out.println("Trying to add a new node from goal, found a node in the start search already there.");
										result = alreadyThere.asPath();
                                                                                if (verbose) System.out.println("alreadyThere.asPath() is "+alreadyThere.asPath());
                                                                                if (verbose) System.out.println("now the path from goal reversed is: "+e.asPathReversed());
										result.add( e.asPathReversed() );
                                                                                if (verbose) System.out.println("added, that is: "+result);
										done = true;
									}
									
									if( done ) {
										if (verbose) System.out.println("Searches met!");
										foundGoal( result );
                                                                                setExitReason(SUCCESS);
										reportFinished( true );
										return;
									}
									
								}
								
								// The other alternative is that this node is already in one
								// of the lists working from the start but has a better way
								// of getting to that point.
								
								if( alreadyThere.f > f_for_new_point ) {
									
									if( i == 0 ) {
										
										if( alreadyThere.searchStatus == OPEN_FROM_START ) {
											
											open_from_start.remove( alreadyThere );
											alreadyThere.setFrom( newNode );
                                                                                        alreadyThere.searchStatus = OPEN_FROM_START;
											open_from_start.add( alreadyThere );
											continue;
											
										} else if( alreadyThere.searchStatus == CLOSED_FROM_START ) {
											
											closed_from_start.remove( alreadyThere );
											alreadyThere.setFrom( newNode );
											alreadyThere.searchStatus = OPEN_FROM_START;
											open_from_start.add( alreadyThere );
											continue;
											
										}
										
									} else if( i == 1 ) {
										
										if( alreadyThere.searchStatus == OPEN_FROM_GOAL ) {
											
											open_from_goal.remove( alreadyThere );
											alreadyThere.setFrom( newNode );
                                                                                        alreadyThere.searchStatus = OPEN_FROM_GOAL;
											open_from_goal.add( alreadyThere );
											continue;
											
										} else if( alreadyThere.searchStatus == CLOSED_FROM_GOAL ) {
											
											closed_from_goal.remove( alreadyThere );
											alreadyThere.setFrom( newNode );
											alreadyThere.searchStatus = OPEN_FROM_GOAL;
											open_from_goal.add( alreadyThere );
											continue;
											
										}
										
									}
									
								}
								
                                                        }
							
                                                }					
				}
                        }
			
			++ loops;	     
		}
		
                /* If we get to here then we haven't found a route to
                   the point.  (With the current impmlementation this
                   shouldn't happen, so print a warning - probably the
                   programmer hasn't populated the open list to start
                   with.)  However, in this case let's return the best
                   path so far anyway... */
		
                if (verbose) System.out.println( "FAILED to find a route.  Shouldn't happen..." );
                setExitReason(POINTS_EXHAUSTED);
                reportFinished( false );            
                return;
		
        }
	
        /* This is the heuristic value for the A* search.  There's no
	 * defined goal in this default superclass implementation, so
	 * always return 0 so we end up with Dijkstra's algorithm. */
	
        float estimateCostToGoal( int current_x, int current_y, int current_z, int to_start_or_goal ) {
		return 0;
        }
	
        public static int SUCCESS = 0;
        public static int CANCELLED = 1;
        public static int TIMED_OUT = 2;
        public static int POINTS_EXHAUSTED = 3;
	
        public static String [] exitReasonStrings = { "SUCCESS",
                                                      "CANCELLED",
                                                      "TIMED_OUT",
                                                      "POINTS_EXHAUSTED" };
	
        protected int exitReason;
	
        /* This method is used to set the reason for the thread finishing */
        void setExitReason( int exitReason ) {
		this.exitReason = exitReason;
        }
	
        /* Use this to find out why the thread exited if you're not adding
           listeners to do that. */
        int getExitReason( ) {
		return exitReason;
        }
	
	/* This draws over the Graphics object the current progress of
	 * the search at this slice.  If openColor or closedColor are
	 * null then that means "don't bother to draw that list". */
	
	void drawProgressOnSlice( int plane,
				  int currentSliceInPlane,
				  ImageCanvas canvas,
				  Graphics g ){
		
		for( int i = 0; i < 2; ++i ) {
			
			// The first time through we draw the nodes in
			// the open list, the second time through we
			// draw the nodes in the closed list.
			
			byte start_status = (i == 0) ? OPEN_FROM_START : CLOSED_FROM_START;
			byte goal_status = (i == 0) ? OPEN_FROM_GOAL : CLOSED_FROM_GOAL;
			Color c = (i == 0) ? openColor : closedColor;
			if( c == null )
				continue;
			
			g.setColor(c);
			
			if( plane == ThreePanes.XY_PLANE ) {
				int z = currentSliceInPlane;
				for( int y = 0; y < height; ++y )
					for( int x = 0; x < width; ++x ) {
						SearchNode [] slice = nodes_as_image[z];
						if( slice == null )
							continue;
						SearchNode n = slice[y*width+x];
						if( n == null )
							continue;
						byte status = n.searchStatus;
						if( (drawingThreshold >= 0) && (n.g > drawingThreshold) ) {
							continue;
						}
						if( status == start_status || status == goal_status ) {
							int sx = canvas.screenX(x);
							int sx_pixel_size = canvas.screenX(x+1) - sx;
							if( sx_pixel_size < 1 ) sx_pixel_size = 1;
							int sy = canvas.screenY(y);
							int sy_pixel_size = canvas.screenY(y+1) - sy;
							if( sy_pixel_size < 1 ) sy_pixel_size = 1;
							g.fillRect( canvas.screenX(x), canvas.screenY(y), sx_pixel_size, sy_pixel_size );
						}
					}
			} else if( plane == ThreePanes.XZ_PLANE ) {
				int y = currentSliceInPlane;
				for( int z = 0; z < depth; ++ z )
					for( int x = 0; x < width; ++x ) {
						SearchNode [] slice = nodes_as_image[z];
						if( slice == null )
							continue;
						SearchNode n = slice[y*width+x];
						if( n == null )
							continue;
						byte status = n.searchStatus;
						if( (drawingThreshold >= 0) && (n.g > drawingThreshold) )
							continue;
						if( status == start_status || status == goal_status ) {
							int sx = canvas.screenX(x);
							int sx_pixel_size = canvas.screenX(x+1) - sx;
							if( sx_pixel_size < 1 ) sx_pixel_size = 1;
							int sy = canvas.screenY(z);
							int sy_pixel_size = canvas.screenY(z+1) - sy;
							if( sy_pixel_size < 1 ) sy_pixel_size = 1;
							g.fillRect( canvas.screenX(x), canvas.screenY(z), sx_pixel_size, sy_pixel_size );
						}
					}
			} else if( plane == ThreePanes.ZY_PLANE ) {
				int x = currentSliceInPlane;
				for( int y = 0; y < height; ++y )
					for( int z = 0; z < depth; ++z ) {
						SearchNode [] slice = nodes_as_image[z];
						if( slice == null )
							continue;
						SearchNode n = slice[y*width+x];
						if( n == null )
							continue;
						byte status = n.searchStatus;
						if( (drawingThreshold >= 0) && (n.g > drawingThreshold) )
							continue;
						if( status == start_status || status == goal_status ) {
							int sx = canvas.screenX(z);
							int sx_pixel_size = canvas.screenX(z+1) - sx;
							if( sx_pixel_size < 1 ) sx_pixel_size = 1;
							int sy = canvas.screenY(y);
							int sy_pixel_size = canvas.screenY(y+1) - sy;
							if( sy_pixel_size < 1 ) sy_pixel_size = 1;
							g.fillRect( canvas.screenX(z), canvas.screenY(y), sx_pixel_size, sy_pixel_size );
						}
					}
			}
		}
	}
	
	// Add a node, ignoring requests to add duplicate nodes...
	
	public void addNode( SearchNode n ) {
		
		if( nodes_as_image[n.z] == null ) {
			nodes_as_image[n.z] = new SearchNode[width*height];
		}
		
		if( nodes_as_image[n.z][n.y*width+n.x] != null ) {
			// Then there's already a node there,
			return;
		}
		
		if( n.searchStatus == OPEN_FROM_START ) {
			
			open_from_start.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;
			
		} else if( n.searchStatus == OPEN_FROM_GOAL ) {
			assert( ! (bidirectional && definedGoal ) );
			
			open_from_goal.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;
			
		} else if( n.searchStatus == CLOSED_FROM_START ) {
			
			closed_from_start.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;
			
		} else if( n.searchStatus == CLOSED_FROM_GOAL ) {
			assert( ! (bidirectional && definedGoal ) );
			
			closed_from_goal.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;
			
		}
		
	}
	
}

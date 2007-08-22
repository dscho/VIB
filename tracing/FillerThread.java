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

import ij.*;

import java.util.*;

public class FillerThread extends Thread {
	
	/* You should synchronize on this object if you want to rely
	 * on the pause status not changing.  (The run() method is not
	 * synchronized itself, for possibly dubious performance
	 * reasons.) */
	
        byte [][] slices_data;
	
        Simple_Neurite_Tracer plugin;
	
        double x_spacing;
        double y_spacing;
        double z_spacing;
	
        int width;
        int height;
        int depth;
	
        boolean reciprocal;
	
        int timeoutSeconds;
        long reportEveryMilliseconds;
        long lastReportMilliseconds;
	
        float threshold;
	
        double reciprocal_fudge = 0.5;
	
        float minimumDistanceInOpen = 0;

        public void setThreshold( double threshold ) {
                this.threshold = (float)threshold;
        }
	
        public float getDistanceAtPoint( int x, int y, int z ) {
		
                FillerNode f = new FillerNode( x, y, z, 0, null );
		
                /* We should synchronize this really :( */
		
                FillerNode foundInOpen = (FillerNode)open_from_start_hash.get(f);
                FillerNode foundInClosed = (FillerNode)closed_from_start_hash.get(f);
		
                if( foundInOpen != null )
                        return foundInOpen.g;
                else if( foundInClosed != null )
                        return foundInClosed.g;
                else
                        return -1.0f;
		
        }
	
        public void displayUpdate( ) {
		
                long started_update_at = System.currentTimeMillis();
		
                // Then report the open list...
		
                ArrayList<FillerNode> toReport = new ArrayList<FillerNode>();
		
                int i = 0;
		
                for( Iterator<FillerNode> j = open_from_start.iterator();
                     j.hasNext(); ) {
                        FillerNode current = j.next();
                        if( current.g <= threshold )
                                toReport.add(current);
                }
		
                for( Iterator<FillerNode> j = closed_from_start.iterator();
                     j.hasNext(); ) {
                        FillerNode current = j.next();
                        if( current.g <= threshold )
                                toReport.add(current);
                }
		
                System.out.println("Updating with "+toReport.size()+" points...");
		
                short [] asShorts = new short[toReport.size()*3];
		
                i = 0;
                for( Iterator<FillerNode> j = toReport.iterator();
                     j.hasNext();
                     i ++ ) {
                        FillerNode current = j.next();
                        asShorts[i*3] = (short)current.x;
                        asShorts[i*3+1] = (short)current.y;
                        asShorts[i*3+2] = (short)current.z;
                }
		
                System.out.println("Time to extract sub-threshold points: "+((System.currentTimeMillis()-started_update_at)/1000.0));
		
		for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			FillerProgressCallback progress = j.next();
			progress.pointsWithinThreshold( asShorts );
		}
        }
	
        PriorityQueue open_from_start = new PriorityQueue();
        PriorityQueue closed_from_start = new PriorityQueue();
	
        Hashtable open_from_start_hash = new Hashtable();
        Hashtable closed_from_start_hash = new Hashtable();
	
        // FIXME: may be buggy, synchronization issues
	
        Fill getFill( ) {
		
                Hashtable< FillerNode, Integer > h =
                        new Hashtable< FillerNode, Integer >();
		
                ArrayList< FillerNode > a =
                        new ArrayList< FillerNode >();
		
                // The tricky bit here is that we want to create a
                // Fill object with index
		
		int openBelow;
                int i = 0;
		
                for( Iterator<FillerNode> j = open_from_start.iterator();
                     j.hasNext(); ) {
                        FillerNode current = j.next();
                        if( current.g <= threshold ) {
                                h.put( current, new Integer(i) );
                                a.add( current );
                                ++ i;
                        }
                }
		
		openBelow = i;

		System.out.println("openBelow is: "+openBelow);
		
                for( Iterator<FillerNode> j = closed_from_start.iterator();
                     j.hasNext(); ) {
                        FillerNode current = j.next();
                        if( current.g <= threshold ) {
                                h.put( current, new Integer(i) );
                                a.add( current );
                                ++ i;
                        }
                }

                Fill fill = new Fill();

                fill.setThreshold( threshold );
                if( reciprocal )
                        fill.setMetric( "reciprocal-intensity-scaled" );
                else
                        fill.setMetric( "256-minus-intensity-scaled" );

                fill.setSpacing( x_spacing,
                                 y_spacing,
                                 z_spacing,
                                 plugin.spacing_units );

		System.out.println("... out of a.size() "+a.size()+" entries");

                for( i = 0; i < a.size(); ++i ) {
                        FillerNode f = a.get(i);
                        int previousIndex = -1;
                        FillerNode previous = f.getPredecessor();
                        if( previous != null ) {
                                Integer p = h.get(previous);
                                if( p != null ) {
                                        previousIndex = p.intValue();
                                }
                        }
                        fill.add( f.x, f.y, f.z, f.g, previousIndex, i < openBelow );
                }

                if( sourcePaths != null ) {
                        Path [] dummy = { };
                        fill.setSourcePaths( (Path [])( sourcePaths.toArray( dummy ) ) );
                }

                return fill;
        }

        ArrayList< Path > sourcePaths;

	boolean startPaused;

	ArrayList< FillerProgressCallback > progressListeners;

	public static FillerThread fromFill( Simple_Neurite_Tracer plugin,
					     boolean startPaused,
					     Fill fill ) {
		
		boolean reciprocal;
		float initialThreshold;
		String metric = fill.getMetric();

		if( metric.equals("reciprocal-intensity-scaled") ) {
			
			reciprocal = true;

		} else if( metric.equals("256-minus-intensity-scaled") ) {

			reciprocal = false;

		} else {

			IJ.error("Trying to load a fill with an unknown metric ('" + metric + "')");
			return null;

		}

		FillerThread result = new FillerThread( plugin,
							startPaused,
							reciprocal,
							fill.getThreshold(),
							5000 );

		ArrayList< FillerNode > tempNodes = new ArrayList< FillerNode >();

                for( Iterator it = fill.nodeList.iterator(); it.hasNext(); ) {
                        Fill.Node n = (Fill.Node)it.next();
			FillerNode f = new FillerNode( n.x,
						       n.y,
						       n.z,
						       (float)n.distance,
						       null );
			tempNodes.add(f);
		}

		for( int i = 0; i < tempNodes.size(); ++i ) {

			Fill.Node n = (Fill.Node)fill.nodeList.get(i);
			FillerNode f = tempNodes.get(i);
			if( n.previous >= 0 ) {
				f.setPredecessor( tempNodes.get(n.previous) );
			}

			if( n.open ) {
				result.open_from_start.add(f);
				result.open_from_start_hash.put(f,f);
			} else {
                                result.closed_from_start.add(f);
                                result.closed_from_start_hash.put(f,f);
			}

		}
		
		return result;

	}

	public void addProgressListener( FillerProgressCallback callback ) {
		progressListeners.add( callback );
	}

        /* If you specify 0 for timeoutSeconds then there is no timeout. */

        public FillerThread( Simple_Neurite_Tracer plugin,
			     boolean startPaused,
                             boolean reciprocal,
                             double initialThreshold,
			     long reportEveryMilliseconds ) {

		progressListeners = new ArrayList< FillerProgressCallback >();

		this.startPaused = startPaused;

                this.slices_data = plugin.slices_data;
                this.plugin = plugin;

                // Just get these from the plugin:

                this.x_spacing = plugin.x_spacing;
                this.y_spacing = plugin.y_spacing;
                this.z_spacing = plugin.z_spacing;
                this.width = plugin.width;
                this.height = plugin.height;
                this.depth = plugin.depth;

                this.reciprocal = reciprocal;
                this.timeoutSeconds = timeoutSeconds;
                setThreshold( initialThreshold );
                this.reportEveryMilliseconds = reportEveryMilliseconds;

                System.out.println("reportEveryMilliseconds is "+this.reportEveryMilliseconds);

                long lastThresholdChange = 0;

        }

	public void getSourcePathsFromPlugin() {
		
                PathAndFillManager pathAndFillManager = plugin.getPathAndFillManager();

                // Just get these from the plugin; this thread should be
                // created synchronized...

                sourcePaths = new ArrayList< Path >();

                for( int i = 0; i < pathAndFillManager.size(); ++i ) {

                        if( ! pathAndFillManager.selectedPaths[i] )
                                continue;

                        Path p = pathAndFillManager.getPath(i);

                        sourcePaths.add(p);

                        for( int k = 0; k < p.size(); ++k ) {
                                FillerNode f = new FillerNode( p.x_positions[k],
                                                               p.y_positions[k],
                                                               p.z_positions[k],
                                                               0,
                                                               null );
				
				if( null == open_from_start_hash.get(f) ) {
					open_from_start.add(f);
					open_from_start_hash.put(f,f);
				} else {
					System.out.println("Not adding duplicate point: "+f);
				}
                        }
                }

	}

	/* The thread can be in one of these states:
	      - stopping: if stopRequested is true, the thread cannot be used again
              - paused: if stopRequested is false and  */

	private int threadStatus;
	      
	public static final int RUNNING  = 0;
	public static final int PAUSED   = 1;
	public static final int STOPPING = 2;

	public int getThreadStatus( ) {
		return threadStatus;
	}
	
	// Safely stops the filler thread (on discarding the fill.)

        public void requestStop( ) {
		synchronized (this) {
			if( threadStatus == PAUSED ) {
				this.interrupt();
			}
			threadStatus = STOPPING;
			reportStatus();
		}
        }

	// Toggles the paused or unpaused status of the filler.
	
        public void pauseOrUnpause( ) {
		// Toggle the paused status...
		synchronized (this) {
			switch( threadStatus) {
			case PAUSED:
				this.interrupt();
				threadStatus = RUNNING;
				break;
			case RUNNING:
				threadStatus = PAUSED;
				break;
			default:
				// Do nothing, we're actually stopping anyway...
			}
			reportStatus();
		}
        }

	public void reportStatus( ) {
		for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			FillerProgressCallback progress = j.next();
			progress.fillerStatus( threadStatus );
		}
	}

        public void run( ) {

		setPriority( MIN_PRIORITY );

		if( startPaused ) {
			synchronized (this) {
				threadStatus = PAUSED;
				reportStatus();
			}
			displayUpdate();
			

			for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
				FillerProgressCallback progress = j.next();
				progress.maximumDistanceCompletelyExplored(minimumDistanceInOpen);
			}
		} else {
			synchronized (this) {
				threadStatus = RUNNING;
				reportStatus();
			}
		}

                System.out.println("Starting fillThread, with threshold at: "+threshold);

                long started_at = lastReportMilliseconds = System.currentTimeMillis();

                int loops = 0;

                while( open_from_start.size() > 0 ) {
			
			if( threadStatus == STOPPING ) {
				for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
					FillerProgressCallback progress = j.next();
					progress.fillerStatus( STOPPING );
				}
				return;
                        } else if( threadStatus == PAUSED ) {				
				synchronized (this) {
					if( threadStatus == PAUSED ) {
						try {
							for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
								FillerProgressCallback progress = j.next();
								progress.fillerStatus( PAUSED );
							}
							
							Thread.sleep(4000);
						} catch( InterruptedException e ) {
						}
					}
				}
                        }
			
			// We only update the display with the full
			// set of points every reportEveryMilliseconds
			// ms, since this is quite a costly operation.
			// We only even check whether that time has
			// elapsed every 5000 loops however...

                        if( 0 == (loops % 5000) ) {

                                long currentMilliseconds = System.currentTimeMillis();

                                if( (timeoutSeconds > 0) && ((currentMilliseconds - started_at) > (1000 * timeoutSeconds)) )
                                        break;
				
                                if( (reportEveryMilliseconds > 0) && ((currentMilliseconds - lastReportMilliseconds) > reportEveryMilliseconds ) ) {
                                        displayUpdate();
					for( Iterator<FillerProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
						FillerProgressCallback progress = j.next();
						progress.maximumDistanceCompletelyExplored(minimumDistanceInOpen);
					}							
                                }
                        }

                        boolean verbose = false;
                        // boolean verbose = (0 == (loops % 5000) );

                        FillerNode p = null;

                        if( open_from_start.size() > 0 ) {

                                // p = get_highest_priority( open_from_start, open_from_start_hash );
                                p = (FillerNode)open_from_start.poll();
                                open_from_start_hash.remove( p );

                        }

                        // Print out some status information...

                        if( verbose ) {

                                System.out.println( "at loop: " + loops + " open_from_start: " +
                                                    open_from_start.size() + "(" + open_from_start_hash.size() +
                                                    ") closed_from_start: " + closed_from_start.size() + "(" +
                                                    closed_from_start_hash.size() + ")" );

                        }

                        if( p != null ) {

                                minimumDistanceInOpen = p.g;

                                if( 0 == (loops % 5000) ) {
                                        System.out.println("Highest priority g is: "+p.g);
                                }

                                // add_node( closed_from_start, closed_from_start_hash, p );
                                closed_from_start.add( p );
                                closed_from_start_hash.put( p, p );

                                // Now look at the neighbours of p.  We're going to look
                                // at the 26 neighbours in 3D.

                                for( int xdiff = -1; xdiff <= 1; xdiff++ )
                                        for( int ydiff = -1; ydiff <= 1; ydiff++ )
                                                for( int zdiff = -1; zdiff <= 1; zdiff++ ) {

                                                        if( (xdiff == 0) && (ydiff == 0) && (zdiff == 0) )
                                                                continue;

                                                        int new_x = p.x + xdiff;
                                                        int new_y = p.y + ydiff;
                                                        int new_z = p.z + zdiff;

                                                        if( new_x < 0 || new_x >= width )
                                                                continue;

                                                        if( new_y < 0 || new_y >= height )
                                                                continue;

                                                        if( new_z < 0 || new_z >= depth )
                                                                continue;

                                                        double xdiffsq = (xdiff * x_spacing) * (xdiff * x_spacing);
                                                        double ydiffsq = (ydiff * y_spacing) * (ydiff * y_spacing);
                                                        double zdiffsq = (zdiff * z_spacing) * (zdiff * z_spacing);

                                                        int value_at_new_point = slices_data[new_z][new_y*width+new_x] & 0xFF;

                                                        double cost_moving_to_new_point;

                                                        if( reciprocal ) {
                                                                cost_moving_to_new_point = 1 / reciprocal_fudge;
                                                                if( value_at_new_point != 0 )
                                                                        cost_moving_to_new_point = 1.0 / value_at_new_point;
                                                        } else {
                                                                cost_moving_to_new_point = 256 - value_at_new_point;
                                                        }

                                                        float g_for_new_point = (float) ( p.g + Math.sqrt( xdiffsq + ydiffsq + zdiffsq ) * cost_moving_to_new_point );

                                                        FillerNode newNode = new FillerNode( new_x, new_y, new_z,
                                                                                             g_for_new_point,
                                                                                             p );

                                                        // FillerNode foundInClosed_From_Start = found_in( closed_from_start, closed_from_start_hash, newNode );
                                                        FillerNode foundInClosed = (FillerNode)closed_from_start_hash.get( newNode );

                                                        // FillerNode foundInOpen = found_in( open_from_start, open_from_start_hash, newNode );
                                                        FillerNode foundInOpen = (FillerNode)open_from_start_hash.get( newNode );

                                                        // Is there an exisiting route which is
                                                        // better?  If so, discard this new candidate...

                                                        if( (foundInClosed != null) && (foundInClosed.g <= g_for_new_point) ) {
                                                                continue;
                                                        }

                                                        if( (foundInOpen != null) && (foundInOpen.g <= g_for_new_point) ) {
                                                                continue;
                                                        }

                                                        if( foundInClosed != null ) {

                                                                // remove( closed_from_start, closed_from_start_hash, foundInClosed );
                                                                closed_from_start.remove( foundInClosed );
                                                                closed_from_start_hash.remove( foundInClosed );

                                                                // There may be references to this node,
                                                                // so we need to preserve that.

                                                                foundInClosed.setFrom( newNode );

                                                                // add_node( open_from_start, open_from_start_hash, foundInClosed );
                                                                open_from_start.add( foundInClosed );
                                                                open_from_start_hash.put( foundInClosed, foundInClosed );

                                                                continue;
                                                        }

                                                        if( foundInOpen != null ) {

                                                                // remove( open_from_start, open_from_start_hash, foundInOpen );
                                                                open_from_start.remove( foundInOpen );
                                                                open_from_start_hash.remove( foundInOpen );

                                                                // There may be references to this node,
                                                                // so we need to preserve that.

                                                                foundInOpen.setFrom( newNode );

                                                                // add_node( open_from_start, open_from_start_hash, foundInOpen );
                                                                open_from_start.add( foundInOpen );
                                                                open_from_start_hash.put( foundInOpen, foundInOpen );

                                                                continue;
                                                        }

                                                        // Otherwise we add a new node:

                                                        // add_node( open_from_start, open_from_start_hash, newNode );
                                                        open_from_start.add( newNode );
                                                        open_from_start_hash.put( newNode, newNode );

                                                }

                        }

                        ++ loops;

                }

        }

}

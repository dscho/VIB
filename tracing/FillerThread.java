/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.util.*;

public class FillerThread extends Thread {
	
	byte [][] slices_data;
	
	SimpleNeuriteTracer_ plugin;

	FillerProgressCallback progress;

	double x_spacing;
	double y_spacing;
	double z_spacing;
	
	int width;
	int height;
	int depth;
	
	boolean reciprocal;
	boolean preprocess;
	
	int timeoutSeconds;
	long reportEveryMilliseconds;
	long lastReportMilliseconds;
	
	float threshold;

	double reciprocal_fudge = 0.5;

	float minimumDistanceInOpen = 0;
		
	public void setThreshold( float threshold ) {
		this.threshold = threshold;
	}

	public float getDistanceAtPoint(int x, int y, int z) {

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
		
		progress.pointsWithinThreshold( asShorts );
	}

	PriorityQueue open_from_start = new PriorityQueue();
	PriorityQueue closed_from_start = new PriorityQueue();

	Hashtable open_from_start_hash = new Hashtable();
	Hashtable closed_from_start_hash = new Hashtable();

	/* If you specify 0 for timeoutSeconds then there is no timeout. */
	
	public FillerThread( SimpleNeuriteTracer_ plugin,
			     boolean reciprocal,
			     boolean preprocess,                        
			     float initialThreshold,
			     long reportEveryMilliseconds,
			     FillerProgressCallback progress ) {

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
		this.preprocess = preprocess;
		this.timeoutSeconds = timeoutSeconds;
		this.threshold = initialThreshold;
		this.reportEveryMilliseconds = reportEveryMilliseconds;

		System.out.println("reportEveryMilliseconds is "+this.reportEveryMilliseconds);
		
		long lastThresholdChange = 0;
		this.progress = progress;

		// Just get these from the plugin; this thread should be
		// created synchronized...
		
		for( int i = 0; i < plugin.selectedPaths.length; ++i ) {
			SegmentedConnection s = (SegmentedConnection)plugin.allPaths.get(plugin.selectedPaths[i]);
			int segments_in_path = s.connections.size();
			for( int j = 0; j < segments_in_path; ++j ) {				
				Connection connection = (Connection)s.connections.get(j);
				for( int k = 0; k < connection.size(); ++k ) {
					FillerNode f = new FillerNode( connection.x_positions[k],
								       connection.y_positions[k],
								       connection.z_positions[k],
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
			
	}
	
	boolean stopRequested;
	
	public void requestStop( ) {
		stopRequested = true;
	}

	public void run( ) {

		System.out.println("Starting fillThread, with threshold at: "+threshold);
		
		long started_at = lastReportMilliseconds = System.currentTimeMillis();
		
		int loops = 0;
		
		while( open_from_start.size() > 0 ) {
			
			if( stopRequested ) {
				progress.stopped();
				return;
			}
			
			if( 0 == (loops % 5000) ) {
				
				long currentMilliseconds = System.currentTimeMillis();
				
				if( (timeoutSeconds > 0) && ((currentMilliseconds - started_at) > (1000 * timeoutSeconds)) )
					break;
				
				if( (reportEveryMilliseconds > 0) && ((currentMilliseconds - lastReportMilliseconds) > reportEveryMilliseconds ) ) {
					displayUpdate();
					progress.maximumDistanceCompletelyExplored(minimumDistanceInOpen);
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

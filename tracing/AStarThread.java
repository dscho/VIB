/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.util.*;

import features.ComputeCurvatures;

/* This is the thread that explores between two points in an image,
 * doing an A* search with a choice of distance measures. */

public class AStarThread extends Thread {

        byte [][] slices_data;

        int start_x;
        int start_y;
        int start_z;
        int goal_x;
        int goal_y;
        int goal_z;

        SimpleNeuriteTracer_ plugin;

        double x_spacing;
        double y_spacing;
        double z_spacing;

        int width;
        int height;
        int depth;

        boolean reciprocal;

        ComputeCurvatures hessian;

        int timeoutSeconds;
        long reportEveryMilliseconds;
        long lastReportMilliseconds;

        AStarProgressCallback progress;

        Path result;

        /* If you specify 0 for timeoutSeconds then there is no timeout. */

        public AStarThread( byte [][] slices_data,
                            int start_x,
                            int start_y,
                            int start_z,
                            int goal_x,
                            int goal_y,
                            int goal_z,
                            SimpleNeuriteTracer_ plugin,
                            boolean reciprocal,
                            int timeoutSeconds,
                            long reportEveryMilliseconds,
                            ComputeCurvatures hessian,
                            AStarProgressCallback progress ) {

                this.slices_data = slices_data;

                this.start_x = start_x;
                this.start_y = start_y;
                this.start_z = start_z;
                this.goal_x = goal_x;
                this.goal_y = goal_y;
                this.goal_z = goal_z;

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
                this.reportEveryMilliseconds = reportEveryMilliseconds;

                this.hessian = hessian;

                this.progress = progress;

                this.result = null;
        }

        public Path getResult( ) {
                return result;
        }

        boolean stopRequested;

        public void requestStop( ) {
                stopRequested = true;
        }

        /* This cost doesn't take into account the distance between
         * the points - it will be post-multiplied by that value.
         *
         * The minimum cost should be > 0 - it is the value that is
         * used in calculating the heuristic for how far a given point
         * is from the goal. */

        public double costMovingTo( int new_x, int new_y, int new_z, int value_at_new_point, double minimumCost ) {

                double result;

                if( hessian == null ) {

                        if( reciprocal ) {
                                result = 1 / reciprocal_fudge;
                                if( value_at_new_point != 0 )
                                        result = 1.0 / value_at_new_point;
                        } else {
                                result = 256 - value_at_new_point;
                        }

                } else {

                        double [] hessianEigenValues = new double[3];

                        hessian.hessianEigenvaluesAtPoint( new_x, new_y, new_z,
                                                           true, hessianEigenValues, true );

                        /* FIXME: there's lots of literature on how to
                           pick this rule (see Sato et al,
                           "Three-dimensional multi-scale line filter
                           for segmentation and visualization of
                           curvilinear structures in medical images".
                           The rule I'm using here probably isn't optimal. */

                        double e0 = hessianEigenValues[0];
                        double e1 = hessianEigenValues[1];
                        double e2 = hessianEigenValues[2];

                        if( (hessianEigenValues[1] < 0) && (hessianEigenValues[2] < 0) ) {

                                double measure = Math.sqrt( hessianEigenValues[1] * hessianEigenValues[2] );

                                if( measure == 0 ) // This should never happen in practice...
                                        measure = 0.2;

                                result = 1 / measure;

                        } else {

                                result = 1 / 0.2;

                        }

                }

                if( result < minimumCost )
                        result = minimumCost;

                return result;
        }

        public void run( ) {

                ComputeCurvatures hessian = this.hessian;

                double minimum_cost_per_unit_distance;

                if( hessian == null ) {

                        minimum_cost_per_unit_distance = reciprocal ? ( 1 / 255.0 ) : 1;

                } else {

                        // minimum_cost_per_unit_distance = 1E-4;  /* for the ratio of e0/e1 */
                        // minimum_cost_per_unit_distance = 0.002; // 1;  /* for e1 - e0 */

                        minimum_cost_per_unit_distance = 1 / 60.0;
                }

                long started_at = lastReportMilliseconds = System.currentTimeMillis();

                int loops = 0;

                // System.out.println( "Starting AStar..." );

                PriorityQueue closed_from_start = new PriorityQueue();
                PriorityQueue open_from_start = new PriorityQueue();
                PriorityQueue closed_from_goal = new PriorityQueue();
                PriorityQueue open_from_goal = new PriorityQueue();

                Hashtable open_from_start_hash = new Hashtable();
                Hashtable closed_from_start_hash = new Hashtable();
                Hashtable open_from_goal_hash = new Hashtable();
                Hashtable closed_from_goal_hash = new Hashtable();

                AStarNode start = new AStarNode( start_x, start_y, start_z,
                                                 0,
                                                 estimateCostToGoal( start_x, start_y, start_z,
                                                                     goal_x, goal_y, goal_z,
                                                                     minimum_cost_per_unit_distance ),
                                                 null );

                AStarNode goal = new AStarNode( goal_x, goal_y, goal_z,
                                                0,
                                                estimateCostToGoal( goal_x, goal_y, goal_z,
                                                                    start_x, start_y, start_z,
                                                                    minimum_cost_per_unit_distance ),
                                                null );

                // add_node( open_from_start, open_from_start_hash, start );
                open_from_start.add( start );
                open_from_start_hash.put( start, start );

                // add_node( open_from_goal, open_from_goal_hash, goal );
                open_from_goal.add( goal );
                open_from_goal_hash.put( goal, goal );

                while( (open_from_start.size() > 0) && (open_from_goal.size() > 0) ) {

                        if( stopRequested ) {
                                progress.finished(false);
                                return;
                        }

                        if( 0 == (loops % 1000) ) {

                                long currentMilliseconds = System.currentTimeMillis();

                                if( (timeoutSeconds > 0) && ((currentMilliseconds - started_at) > (1000 * timeoutSeconds)) )
                                        break;

                                if( (reportEveryMilliseconds > 0) && ((currentMilliseconds - lastReportMilliseconds) > reportEveryMilliseconds ) ) {

                                        // Then report the open list...

                                        /* System.err.println("open_from_start.size() is "+open_from_start.size()+", "+
                                           "open_from_goal.size() is "+open_from_goal.size()); */

                                        short [] open = new short[ (open_from_start.size() + open_from_goal.size()) * 3 ];
                                        int i = 0;
                                        for( Iterator<AStarNode> j = open_from_start.iterator();
                                             j.hasNext();
                                             i ++ ) {
                                                AStarNode current = j.next();
                                                open[i*3] = (short)current.x;
                                                open[i*3+1] = (short)current.y;
                                                open[i*3+2] = (short)current.z;
                                        }
                                        for( Iterator<AStarNode> j = open_from_goal.iterator();
                                             j.hasNext();
                                             i ++ ) {
                                                AStarNode current = j.next();
                                                open[i*3] = (short)current.x;
                                                open[i*3+1] = (short)current.y;
                                                open[i*3+2] = (short)current.z;
                                        }

                                        progress.currentOpenBoundary(open);

                                        /* System.out.println("Reporting the currentOpen boundary took: "+
                                           ((System.currentTimeMillis() - currentMilliseconds) / 1000.0)); */
                                }


                        }

                        boolean verbose = false;

                        AStarNode p = null;
                        AStarNode q = null;

                        if( open_from_start.size() > 0 ) {

                                // p = get_highest_priority( open_from_start, open_from_start_hash );
                                p = (AStarNode)open_from_start.poll();
                                open_from_start_hash.remove( p );

                        }

                        if( open_from_goal.size() > 0 ) {

                                // q = get_highest_priority( open_from_goal, open_from_goal_hash );
                                q = (AStarNode)open_from_goal.poll();
                                open_from_goal_hash.remove( q );

                        }


                        // Has the route from the start found the goal?

                        if( (p != null) && (p.x == goal_x) && (p.y == goal_y) && (p.z == goal_z) ) {
                                // System.out.println( "Found the goal! (from start to end)" );
                                result = p.asPath();
                                progress.finished(true);
                                return;
                        }

                        // Has the route from the goal found the start?

                        if( (q != null) && (q.x == start_x) && (q.y == start_y) && (q.z == start_z) ) {
                                // System.out.println( "Found the goal! (from end to start)" );
                                result = p.asPathReversed();
                                progress.finished(true);
                                return;
                        }

                        // Has the route from the start found the route from the goal?

                        if( p != null ) {

                                AStarNode foundInRouteFromGoal = (AStarNode)open_from_goal_hash.get( p );
                                if( foundInRouteFromGoal == null ) {
                                        foundInRouteFromGoal = (AStarNode)closed_from_goal_hash.get( p );
                                        if( foundInRouteFromGoal != null )
                                                System.out.println( "found next open node in closed_from_goal_hash" );
                                }
                                if( foundInRouteFromGoal != null ) {
                                        // System.out.println( "Found the goal! (searches met...)" );
                                        AStarNode a = p.getPredecessor();
                                        if( a == null ) {
                                                result = foundInRouteFromGoal.asPathReversed();
                                                progress.finished(true);
                                                return;
                                        } else {
                                                result = a.asPath();
                                                result.add( foundInRouteFromGoal.asPathReversed() );
                                                progress.finished(true);
                                                return;
                                        }
                                }

                        } // FIXME: else q != null ?

                        // Print out some status information...

                        if( verbose ) {

                                System.out.println( "at loop: " + loops + " open_from_start: " +
                                                    open_from_start.size() + "(" + open_from_start_hash.size() +
                                                    ") closed_from_start: " + closed_from_start.size() + "(" +
                                                    closed_from_start_hash.size() + ")" );

                                System.out.println( "         " + loops + " open_from_goal: " +
                                                    open_from_goal.size() + "(" + open_from_goal_hash.size() +
                                                    ") closed_from_goal: " + closed_from_goal.size() + "(" +
                                                    closed_from_goal_hash.size() + ")" );

                        }

                        if( p != null ) {

                                // add_node( closed_from_start, closed_from_start_hash, p );
                                closed_from_start.add( p );
                                closed_from_start_hash.put( p, p );

                                // Now look at the neighbours of p.  We're going to consider
                                // the 26 neighbours in 3D.

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

                                                        float h_for_new_point = estimateCostToGoal( new_x, new_y, new_z,
                                                                                                    goal_x, goal_y, goal_z,
                                                                                                    minimum_cost_per_unit_distance );

                                                        int value_at_new_point = slices_data[new_z][new_y*width+new_x] & 0xFF;

                                                        double cost_moving_to_new_point = costMovingTo( new_x, new_y, new_z,
                                                                                                        value_at_new_point,
                                                                                                        minimum_cost_per_unit_distance );

                                                        float g_for_new_point = (float) ( p.g + Math.sqrt( xdiffsq + ydiffsq + zdiffsq ) * cost_moving_to_new_point );

                                                        float f_for_new_point = (float)( h_for_new_point + g_for_new_point );

                                                        AStarNode newNode = new AStarNode( new_x, new_y, new_z,
                                                                                           g_for_new_point, h_for_new_point,
                                                                                           p );

                                                        // AStarNode foundInClosed_From_Start = found_in( closed_from_start, closed_from_start_hash, newNode );
                                                        AStarNode foundInClosed = (AStarNode)closed_from_start_hash.get( newNode );

                                                        // AStarNode foundInOpen = found_in( open_from_start, open_from_start_hash, newNode );
                                                        AStarNode foundInOpen = (AStarNode)open_from_start_hash.get( newNode );

                                                        // Is there an exisiting route which is
                                                        // better?  If so, discard this new candidate...

                                                        if( (foundInClosed != null) && (foundInClosed.f <= f_for_new_point) ) {
                                                                continue;
                                                        }

                                                        if( (foundInOpen != null) && (foundInOpen.f <= f_for_new_point) ) {
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

                        if( q != null ) {

                                // add_node( closed_from_goal, closed_from_goal_hash, q );
                                closed_from_goal.add( q );
                                closed_from_goal_hash.put( q, q );

                                // Now we do the same for q: look for the 26 neighbours in 3D.

                                for( int xdiff = -1; xdiff <= 1; xdiff++ )
                                        for( int ydiff = -1; ydiff <= 1; ydiff++ )
                                                for( int zdiff = -1; zdiff <= 1; zdiff++ ) {

                                                        if( (xdiff == 0) && (ydiff == 0) && (zdiff == 0) )
                                                                continue;

                                                        int new_x = q.x + xdiff;
                                                        int new_y = q.y + ydiff;
                                                        int new_z = q.z + zdiff;

                                                        if( new_x < 0 || new_x >= width )
                                                                continue;

                                                        if( new_y < 0 || new_y >= height )
                                                                continue;

                                                        if( new_z < 0 || new_z >= depth )
                                                                continue;

                                                        double xdiffsq = (xdiff * x_spacing) * (xdiff * x_spacing);
                                                        double ydiffsq = (ydiff * y_spacing) * (ydiff * y_spacing);
                                                        double zdiffsq = (zdiff * z_spacing) * (zdiff * z_spacing);

                                                        float h_for_new_point = estimateCostToGoal( new_x, new_y, new_z,
                                                                                                    start_x, start_y, start_z,
                                                                                                    minimum_cost_per_unit_distance );

                                                        int value_at_new_point = slices_data[new_z][new_y*width+new_x] & 0xFF;

                                                        double cost_moving_to_new_point = costMovingTo( new_x, new_y, new_z,
                                                                                                        value_at_new_point,
                                                                                                        minimum_cost_per_unit_distance );

                                                        float g_for_new_point = (float) ( q.g + Math.sqrt( xdiffsq + ydiffsq + zdiffsq ) * cost_moving_to_new_point );

                                                        float f_for_new_point = (float)( h_for_new_point + g_for_new_point );

                                                        AStarNode newNode = new AStarNode( new_x, new_y, new_z,
                                                                                           g_for_new_point, h_for_new_point,
                                                                                           q );

                                                        // AStarNode foundInClosed = found_in( closed_from_goal, closed_from_goal_hash, newNode );
                                                        AStarNode foundInClosed = (AStarNode)closed_from_goal_hash.get( newNode );

                                                        // AStarNode foundInOpen = found_in( open_from_goal, open_from_goal_hash, newNode );
                                                        AStarNode foundInOpen = (AStarNode)open_from_goal_hash.get( newNode );

                                                        // Is there an exisiting route which is
                                                        // better?  If so, discard this new candidate...

                                                        if( (foundInClosed != null) && (foundInClosed.f <= f_for_new_point) ) {
                                                                continue;
                                                        }

                                                        if( (foundInOpen != null) && (foundInOpen.f <= f_for_new_point) ) {
                                                                continue;
                                                        }

                                                        if( foundInClosed != null ) {

                                                                // remove( closed_from_goal, closed_from_goal_hash, foundInClosed );
                                                                closed_from_goal.remove( foundInClosed );
                                                                closed_from_goal_hash.remove( foundInClosed );

                                                                // There may be references to this node,
                                                                // so we need to preserve that.

                                                                foundInClosed.setFrom( newNode );

                                                                // add_node( open_from_goal, open_from_goal_hash, foundInClosed );
                                                                open_from_goal.add( foundInClosed );
                                                                open_from_goal_hash.put( foundInClosed, foundInClosed );

                                                                continue;
                                                        }

                                                        if( foundInOpen != null ) {

                                                                // remove( open_from_goal, open_from_goal_hash, foundInOpen );
                                                                open_from_goal.remove( foundInOpen );
                                                                open_from_goal_hash.remove( foundInOpen );

                                                                // There may be references to this node,
                                                                // so we need to preserve that.

                                                                foundInOpen.setFrom( newNode );

                                                                // add_node( open_from_goal, open_from_goal_hash, foundInOpen );
                                                                open_from_goal.add( foundInOpen );
                                                                open_from_goal_hash.put( foundInOpen, foundInOpen );

                                                                continue;
                                                        }

                                                        // Otherwise we add a new node:

                                                        // add_node( open_from_goal, open_from_goal_hash, newNode );
                                                        open_from_goal.add( newNode );
                                                        open_from_goal_hash.put( newNode, newNode );

                                                }

                        }

                        ++ loops;

                }

                /* If we get to here then we haven't found a route to the
                   point.  (With the current impmlementation this shouldn't
                   happen, so print a warning.)  However, in this case let's
                   return the best option: */

                System.out.println( "FAILED to find a route.  Shouldn't happen..." );

                result = ((AStarNode)(open_from_start.poll())).asPath();
                progress.finished(false);
                return;

        }

        /* If we're taking the reciprocal of the value at the new
         * point as our cost, then values of zero cause a problem.
         * This is the value that we use instead of zero there. */

        static final double reciprocal_fudge = 0.5;

        /* This is the heuristic value for the A* search */

        float estimateCostToGoal( int current_x, int current_y, int current_z,
                                  int goal_x, int goal_y, int goal_z,
                                  double minimum_cost_per_unit_distance ) {

                double xdiff = (goal_x - current_x) * x_spacing;
                double ydiff = (goal_y - current_y) * y_spacing;
                double zdiff = (goal_z - current_z) * z_spacing;

                double distance = Math.sqrt( xdiff * xdiff + ydiff * ydiff + zdiff * zdiff );

                return (float) ( minimum_cost_per_unit_distance * distance );

        }

}

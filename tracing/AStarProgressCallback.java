package tracing;

public interface AStarProgressCallback {

    /* This is used to tell the caller where the open boundary is.
     * You're given an array of size 3n, where n is the number of
     * points in the open list.  It's of the form:
     * 
     *   [ x1, y1, z1, x2, y2, z2 ..., xn, yn, zn ]
     *

    */

    void currentOpenBoundary( short [] points );

    /* Once finished is called, AStarNode:getResult() will return the
     * path (or some path, if it's unsuccessful.) */

    void finished( boolean success );
    
}

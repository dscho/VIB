package tracing;

public interface FillerProgressCallback {

    /* This is used to tell the caller where every point within the
     * threshold distance is.
     * 
     * You're given an array of size 3n, where n is the number of
     * points in the open list.  It's of the form:
     * 
     *   [ x1, y1, z1, x2, y2, z2 ..., xn, yn, zn ]
     *

    */

    void pointsWithinThreshold( short [] points );

    void stopped();

    void maximumDistanceCompletelyExplored( float f );

}

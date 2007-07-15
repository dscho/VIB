/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

/* The x, y and z here are zero-indexed and refer to indexes into the
 * slice arrays (as opposed to being screen co-ordinates or adjusted
 * for aspect or calibration data.) */

class PointInImage {

	public int x, y, z;
	public PointInImage( int x, int y, int z ) {
		this.x = x; this.y = y; this.z = z;
	}

	// You can optionally set these two:       

	public Path onPath = null;
	public int onPathIndex = -1;

}

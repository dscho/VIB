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

    public void pointsWithinThreshold( short [] points );

    public void maximumDistanceCompletelyExplored( float f );
    
    public void fillerStatus( int currentStatus );

}

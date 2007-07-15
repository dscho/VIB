/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.util.*;

public class Fill {

    public double distanceThreshold;

    public class Node {
        public int x;
        public int y;
        public int z;
        public double distance;
        public int previous;
    }

    ArrayList< Node > nodeList;

    public Fill( ) {
        nodeList = new ArrayList< Node >();
    }

    public void add( int x, int y, int z, double distance, int previous ) {
        Node n = new Node();
        n.x = x;
        n.y = y;
        n.z = z;
        n.distance = distance;
        n.previous = previous;
        nodeList.add(n);
    }

}

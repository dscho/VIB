
package tracing;

import java.util.ArrayList;

public class SegmentedConnection {

    public SegmentedConnection() {
        connections = new ArrayList< Connection >();
    }

    ArrayList< Connection > connections;
    boolean join_at_start = false;
    
    public void startsAtJoin( boolean join_at_start ) {
        this.join_at_start = join_at_start;
    }

    public void addConnection( Connection c ) {
        connections.add( c );
    }

}

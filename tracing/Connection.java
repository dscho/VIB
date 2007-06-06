/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.awt.*;

import ij.gui.*;

import stacks.ThreePanes;

public class Connection {
	
	Connection( ) {
		points = 0;
		maxPoints = 128;
		x_positions = new int[maxPoints];
		y_positions = new int[maxPoints];
		z_positions = new int[maxPoints];
	}
	
	Connection( int reserve ) {
		points = 0;
		maxPoints = reserve;
		x_positions = new int[maxPoints];
		y_positions = new int[maxPoints];
		z_positions = new int[maxPoints];
	}
	
	public int points;
	public int maxPoints;
	
	public int x_positions[];
	public int y_positions[];
	public int z_positions[];
	
	int size( ) {
		return points;
	}
	
	PointInImage lastPoint( ) {
		if( points < 1 )
			return null;
		else
			return new PointInImage( x_positions[points-1],
						 y_positions[points-1],
						 z_positions[points-1] );
	}
	
	void expandTo( int newMaxPoints  ) {
		
		int [] new_x_positions = new int[newMaxPoints];
		int [] new_y_positions = new int[newMaxPoints];
		int [] new_z_positions = new int[newMaxPoints];
		System.arraycopy( x_positions,
				  0,
				  new_x_positions,
				  0,
				  points );
		System.arraycopy( y_positions,
				  0,
				  new_y_positions,
				  0,
				  points );
		System.arraycopy( z_positions,
				  0,
				  new_z_positions,
				  0,
				  points );
		x_positions = new_x_positions;
		y_positions = new_y_positions;
		z_positions = new_z_positions;
		maxPoints = newMaxPoints;
		
	}
	
	void add( Connection other ) {
		
		if( maxPoints < (points + other.points) ) {
			expandTo( points + other.points );
		}
		
		System.arraycopy( other.x_positions,
				  0,
				  x_positions,
				  points,
				  other.points );
		
		System.arraycopy( other.y_positions,
				  0,
				  y_positions,
				  points,
				  other.points );
		
		System.arraycopy( other.z_positions,
				  0,
				  z_positions,
				  points,
				  other.points );
		
		points = points + other.points;
	}
	
	Connection reversed( ) {
		Connection c = new Connection( points );
		c.points = points;
		for( int i = 0; i < points; ++i ) {
			c.x_positions[i] = x_positions[ (points-1) - i ];
			c.y_positions[i] = y_positions[ (points-1) - i ];
			c.z_positions[i] = z_positions[ (points-1) - i ];
		}
		return c;
	}
	
	void addPoint( int x, int y, int z ) {
		if( points >= maxPoints ) {
			expandTo( (int)( maxPoints * 1.2 + 1 ) );
		}
		x_positions[points] = x;
		y_positions[points] = y;
		z_positions[points++] = z;
	}
	
	public void drawConnectionAsPoints( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane ) {
		drawConnectionAsPoints( canvas, g, c, plane, 0, 0 );
	}
	
	
	public void drawConnectionAsPoints( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane, int z, int either_side ) {
		
		g.setColor( c );
		
		switch( plane ) {
			
		case ThreePanes.XY_PLANE:
		{
			if( either_side == 0 ) {
				for( int i = 0; i < points; ++i )
					g.drawLine( canvas.screenX(x_positions[i]), canvas.screenY(y_positions[i]), canvas.screenX(x_positions[i]), canvas.screenY(y_positions[i]) );
			} else {
				for( int i = 0; i < points; ++i )
					if( Math.abs(z_positions[i] - z) <= either_side ) {
						g.drawLine( canvas.screenX(x_positions[i]), canvas.screenY(y_positions[i]), canvas.screenX(x_positions[i]), canvas.screenY(y_positions[i]) );
					}
			}
		}
		break;
		
		case ThreePanes.XZ_PLANE:
		{
			for( int i = 0; i < points; ++i )
				g.drawLine( canvas.screenX(x_positions[i]), canvas.screenY(z_positions[i]), canvas.screenX(x_positions[i]), canvas.screenY(z_positions[i]) );
		}
		break;
		
		case ThreePanes.ZY_PLANE:
		{
			for( int i = 0; i < points; ++i )
				g.drawLine( canvas.screenX(z_positions[i]), canvas.screenY(y_positions[i]), canvas.screenX(z_positions[i]), canvas.screenY(y_positions[i]) );
		}
		break;
		
		}
		
	}

    /*
    public void drawConnection( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane, int z, int either_side ) {
        // FIXME: not implemented
    }

    public void drawConnection( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane ) {
        
        g.setColor( c );

        switch( plane ) {

        case TracerCanvas.XY_PLANE:
        {       
            g.drawPolyline( x_positions, y_positions, points );
        }
        break;

        case TracerCanvas.XZ_PLANE:
        {
            g.drawPolyline( x_positions, z_positions, points );
        }
        break;

        case TracerCanvas.ZY_PLANE:
        {
            g.drawPolyline( z_positions, y_positions, points );
        }
        break;

        }

    }
    */

}

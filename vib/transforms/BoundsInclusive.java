/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.transforms;

import ij.*;

import java.util.ArrayList;
import java.util.Iterator;

import landmarks.NamedPoint;

public class BoundsInclusive {
	
	public int xmin, ymin, zmin;
	public int xmax, ymax, zmax;
	
	public static BoundsInclusive fromFileName( String fileName ) {
		
		BoundsInclusive result=new BoundsInclusive();
		
		ArrayList<NamedPoint> pointsInTemplate = NamedPoint.pointsForImage(fileName);
		
		if( pointsInTemplate.size() < 1 ) {
			IJ.error( "No points associated with the template image." );
			return null;
		}
		
		{
			NamedPoint first=pointsInTemplate.get(0);
			result.xmin = (int)first.x; result.xmax = (int)first.x;
			result.ymin = (int)first.y; result.ymax = (int)first.y;
			result.zmin = (int)first.z; result.zmax = (int)first.z;
		}
		
		for (Iterator iterator=pointsInTemplate.listIterator();iterator.hasNext();) {
			NamedPoint current=(NamedPoint)iterator.next();
			if(current.x < result.xmin)
				result.xmin = (int)current.x;
			if(current.x > result.xmax)
				result.xmax = (int)current.x;
			if(current.y < result.ymin)
				result.ymin = (int)current.y;
			if(current.y > result.ymax)
				result.ymax = (int)current.y;
			if(current.z < result.zmin)
				result.zmin = (int)current.z;
			if(current.z > result.zmax)
				result.zmax = (int)current.z;
		}
		
		return result;
	}
	
}

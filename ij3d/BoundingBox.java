package ij3d;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class BoundingBox extends Shape3D {

	private Point3f min, max;

	public BoundingBox(Point3f min, Point3f max) {
		this(min, max, new Color3f(1, 0, 0));
	}
	
	public BoundingBox(Point3f min, Point3f max, Color3f color) {
		
		min.x -= 0; min.y -= 0; min.z -= 0;
		max.x += 0; max.y += 0; max.z += 0;
		Point3f[] p = new Point3f[8];
		p[0] = new Point3f(min.x, min.y, max.z);
		p[1] = new Point3f(max.x, min.y, max.z);
		p[2] = new Point3f(max.x, max.y, max.z);
		p[3] = new Point3f(min.x, max.y, max.z);
		p[4] = new Point3f(min.x, min.y, min.z);
		p[5] = new Point3f(max.x, min.y, min.z);
		p[6] = new Point3f(max.x, max.y, min.z);
		p[7] = new Point3f(min.x, max.y, min.z);

		Point3f[] coords = new Point3f[24];
		coords[0] = p[0];
		coords[1] = p[1];
		coords[2] = p[2];
		coords[3] = p[3];
		
		coords[4] = p[1];
		coords[5] = p[5];
		coords[6] = p[6];
		coords[7] = p[2];
		
		coords[8] = p[5];
		coords[9] = p[4];
		coords[10] = p[7];
		coords[11] = p[6];
		
		coords[12] = p[4];
		coords[13] = p[0];
		coords[14] = p[3];
		coords[15] = p[7];
		
		coords[16] = p[1];
		coords[17] = p[0];
		coords[18] = p[4];
		coords[19] = p[5];
		
		coords[20] = p[3];
		coords[21] = p[2];
		coords[22] = p[6];
		coords[23] = p[7];

		QuadArray ga = new QuadArray(24, 
				QuadArray.COORDINATES |
				QuadArray.COLOR_3 |
				QuadArray.NORMALS);
		ga.setCoordinates(0, coords);
		Color3f red = new Color3f(1, 0, 0);
		Color3f[] col = new Color3f[24];
		for(int i = 0; i < 24; i++) 
			col[i] = red;
		ga.setColors(0, col);

		setGeometry(ga);

		Appearance a = new Appearance();
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		a.setPolygonAttributes(pa);

		ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(color);
		a.setColoringAttributes(ca);

		setAppearance(a);
	}
} 


package marchingcubes;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import ij.ImagePlus;

import java.util.List;
import java.util.ArrayList;

public final class MCShape extends Shape3D{

	public MCShape(ImagePlus image, int seekV){
		this.setGeometry(createGeometry(image, seekV));
		this.setAppearance(createAppearance());
	}	

	public static Appearance createAppearance () {
		Appearance appearance = new Appearance();
		
		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		// appearance.setPolygonAttributes(polyAttrib);
		polyAttrib.setBackFaceNormalFlip(true);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		
		Material material = new Material();
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.5f,0.5f,0.5f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	private Geometry createGeometry(ImagePlus image, int seekValue) {

		List<Point3f> tList = MCCube.getTriangles(image, seekValue);
		Point3f[] coords = (Point3f[])tList.toArray(new Point3f[]{});
			
		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);
		for(int i=0; i<N; i++){
			colors[i] = blue;
		}
		
		TriangleArray ta = new TriangleArray (N, 
					TriangleArray.COORDINATES | 
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS);
		for(int i=0; i<N; i++) 
			if(coords[i]==null)
				System.out.println("coord " + i + " is null");
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		// initialize the geometry info here
		GeometryInfo gi = new GeometryInfo(ta);
		
		// generate normals
		NormalGenerator ng = new
		NormalGenerator();
		ng.generateNormals(gi);
		// stripify
		Stripifier st = new Stripifier();
		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		
		return result;
	}
}

package isosurface;

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

import marchingcubes.MCTriangulator;

public final class IsoShape extends Shape3D {

	private Color3f color = new Color3f(0.0f, 1.0f, 0.0f);
	private Triangulator triangulator = new MCTriangulator();
	
	public IsoShape() {
	}
	
	public IsoShape(ImagePlus image, int threshold){
		this.setImage(image, threshold);
	}

	public IsoShape(ImagePlus image, int threshold, Color3f color) {
		this.color = color;
		this.setGeometry(createGeometry(image, threshold));
		this.setAppearance(createAppearance());
	}

	public void setImage(ImagePlus image, int threshold) {
		this.setGeometry(createGeometry(image, threshold));
		this.setAppearance(createAppearance());
	}

	public void setColor(Color3f color) {
		this.color = color;
	}

	private static Appearance createAppearance () {
		Appearance appearance = new Appearance();
		
		PolygonAttributes polyAttrib = new PolygonAttributes();
		//polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		appearance.setColoringAttributes(colorAttrib);
		
		Material material = new Material();
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.5f,0.5f,0.5f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	private Geometry createGeometry(ImagePlus image, int threshold) {

		List<Point3f> tri = triangulator.getTriangles(image,threshold);
		Point3f[] coords = (Point3f[])tri.toArray(new Point3f[]{});
			
		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++){
			colors[i] = color;
		}
		
		TriangleArray ta = new TriangleArray (N, 
					TriangleArray.COORDINATES | 
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS);
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
		result.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
		result.setCapability(TriangleArray.ALLOW_INTERSECT);
		
		return result;
	}
}

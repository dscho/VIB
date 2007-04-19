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

import java.util.List;
import java.util.ArrayList;

public final class IsoShape extends Shape3D {

	Color3f color = new Color3f(0.0f, 1.0f, 0.0f);
	List mesh = null;
	private int threshold;

	public IsoShape(List mesh, int threshold){
		this.mesh = mesh;
		this.threshold = threshold;
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public IsoShape(List mesh, Color3f color, int threshold) {
		this.mesh = mesh;
		this.color = color;
		this.threshold = threshold;
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public void update() {
		if(mesh != null) {
			this.setGeometry(createGeometry());
			this.setAppearance(createAppearance());
		}
	}

	public void setColor(Color3f color) {
		float val = threshold/256f;
		this.color = color != null ? color : new Color3f(val, val, val);
		int N = ((GeometryArray)getGeometry()).getVertexCount();
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++){
			colors[i] = this.color;
		}
		((GeometryArray)getGeometry()).setColors(0, colors);
	}

	private Appearance createAppearance () {
		Appearance appearance = new Appearance();
		
		PolygonAttributes polyAttrib = new PolygonAttributes();
		//polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);
		
		Material material = new Material();
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.5f,0.5f,0.5f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	private Geometry createGeometry() {

		List<Point3f> tri = mesh;
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

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
	private float transparency;

	public IsoShape(List mesh, int threshold, float transparency){
		this.mesh = mesh;
		this.threshold = threshold;
		this.transparency = transparency;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public IsoShape(List mesh, Color3f color, int threshold, 
						float transparency) {
		this.mesh = mesh;
		this.color = color;
		this.threshold = threshold;
		this.transparency = transparency;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public void update() {
		if(mesh != null) {
			this.setGeometry(createGeometry());
			this.setAppearance(createAppearance());
		}
	}

	public List getMesh() {
		return mesh;
	}

	public Color3f getColor() {
		return color;
	}

	public float getTransparency() {
		return transparency;
	}

	public void calculateMinMaxCenterPoint(Point3f min, 
				Point3f max, Point3f center) {
		if(mesh == null) return;
		min.x = min.y = min.z = Float.MAX_VALUE;
		max.x = max.y = max.z = Float.MIN_VALUE;
		
		for(int i = 0; i < mesh.size(); i++) {
			Point3f p = (Point3f)mesh.get(i);
			if(p.x < min.x) min.x = p.x;
			if(p.y < min.y) min.y = p.y;
			if(p.z < min.z) min.z = p.z;
			if(p.x > max.x) max.x = p.x;
			if(p.y > max.y) max.y = p.y;
			if(p.z > max.z) max.z = p.z;
		}
		center.x = (max.x-min.x)/2;
		center.y = (max.y-min.y)/2;
		center.z = (max.z-min.z/2);
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

	public void setTransparency(float transparency) {
		TransparencyAttributes  ta = getAppearance().
						getTransparencyAttributes();
		if(transparency <= .01f) {
			this.transparency = 0.0f;
			ta.setTransparencyMode(TransparencyAttributes.NONE);
		} else {
			this.transparency = transparency;
			ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		}
		ta.setTransparency(transparency);
	}

	private Appearance createAppearance () {
		Appearance appearance = new Appearance();
		
		PolygonAttributes polyAttrib = new PolygonAttributes();
		//polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_BACK);
		polyAttrib.setBackFaceNormalFlip(false);
		appearance.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		TransparencyAttributes tr = new TransparencyAttributes();
		int mode = transparency == 0f ? TransparencyAttributes.NONE
					: TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		appearance.setTransparencyAttributes(tr);
		
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
		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
		Stripifier st = new Stripifier();
		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
		result.setCapability(TriangleArray.ALLOW_COUNT_READ);
		result.setCapability(TriangleArray.ALLOW_INTERSECT);
		
		return result;
	}
}

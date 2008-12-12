package isosurface;

import java.awt.event.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.List;

public final class IsoShape extends Shape3D {

	Color3f color = new Color3f(0.0f, 1.0f, 0.0f);
	List mesh = null;
	private int threshold;
	private float transparency;
	private boolean shaded = true;
	private float volume;
	private int mode = MeshGroup.TRIANGLES;
	private LineAttributes lineAttrs;

	public IsoShape(List mesh, int threshold, float transparency){
		this(mesh, new Color3f(0, 1, 0), threshold, transparency);
	}

	public IsoShape(List mesh, Color3f color, int threshold,
						float transparency) {
		this(mesh, color, threshold, transparency, MeshGroup.TRIANGLES);
	}

	public IsoShape(List mesh, Color3f color, int threshold, float transp, int mode) {
		this(mesh, color, threshold, transp, mode, new LineAttributes());
	}

	public IsoShape(List mesh, Color3f color, int threshold,
			float transp, int mode, LineAttributes attrs) {
		this.mesh = mesh;
		this.color = color;
		this.threshold = threshold;
		this.transparency = transp;
		this.lineAttrs = attrs;
		this.mode = mode;
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

	public boolean isShaded() {
		return shaded;
	}

	public void setShaded(boolean b) {
		this.shaded = b;
		PolygonAttributes pa = getAppearance().getPolygonAttributes();
		if(b)
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	public void calculateMinMaxCenterPoint(Point3f min,
				Point3f max, Point3f center) {

		if(mesh == null)
			return;

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
		double[][] inertia = new double[3][3];
		volume = (float)MeshProperties.compute(mesh, center, inertia);
		System.out.println("center = " + center);
		if(Float.isNaN(center.x) || Float.isNaN(center.y) || Float.isNaN(center.z)
			|| Float.isInfinite(center.x) || Float.isInfinite(center.y)|| Float.isInfinite(center.z)) {
			center.x = (max.x + min.x) / 2;
			center.y = (max.y + min.y) / 2;
			center.z = (max.z + min.z) / 2;
		}
	}

	public float getVolume() {
		return volume;
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
		appearance.setCapability(Appearance.
					ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
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

		appearance.setLineAttributes(lineAttrs);

		Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f,0.1f,0.1f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}

	private Geometry createLineGeometry() {
		List<Point3f> tri = mesh;
		Point3f[] coords = (Point3f[])tri.toArray(new Point3f[]{});

		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++)
			colors[i] = color;

		GeometryArray ta = null;
		if(mode == MeshGroup.LINES) {
			ta = new LineArray(N,
					LineArray.COORDINATES |
					LineArray.COLOR_3);
		} else if (mode == MeshGroup.LINE_STRIPS) {
			ta = new LineStripArray(N,
					LineArray.COORDINATES |
					LineArray.COLOR_3,
					new int[] {N});
		}

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		ta.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		ta.setCapability(GeometryArray.ALLOW_COUNT_READ);
		ta.setCapability(GeometryArray.ALLOW_INTERSECT);

		return ta;
	}

	private Geometry createTriangleGeometry() {
		List<Point3f> tri = mesh;
		Point3f[] coords = (Point3f[])tri.toArray(new Point3f[]{});

		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++){
			colors[i] = color;
		}

		GeometryArray ta = new TriangleArray (N,
					TriangleArray.COORDINATES |
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		GeometryInfo gi = new GeometryInfo(ta);
		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
		Stripifier st = new Stripifier();
		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_INTERSECT);

		return result;
	}

	private Geometry createGeometry() {
		if(mode == MeshGroup.LINES | mode == MeshGroup.LINE_STRIPS)
			return createLineGeometry();
		else
			return createTriangleGeometry();
	}
}

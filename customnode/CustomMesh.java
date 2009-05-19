package customnode;

import java.util.List;
import java.util.Arrays;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

public abstract class CustomMesh extends Shape3D {

	public static final Color3f DEFAULT_COLOR = new Color3f(0, 1, 0);

	protected Color3f color = DEFAULT_COLOR;
	protected List<Point3f> mesh = null;
	protected float transparency = 0;
	protected boolean shaded = true;

	protected CustomMesh() {}

	protected CustomMesh(List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	protected CustomMesh(List<Point3f> mesh, Color3f color, float transp) {
		this.mesh = mesh;
		if(color != null)
			this.color = color;
		this.transparency = transp;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	protected void update() {
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
		center.x = (max.x + min.x) / 2;
		center.y = (max.y + min.y) / 2;
		center.z = (max.z + min.z) / 2;
	}

	public abstract float getVolume();

	private int[] valid = new int[1];
	protected void addVerticesToGeometryStripArray(Point3f[] v) {
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		GeometryStripArray ga = (GeometryStripArray)getGeometry();
		int max = ga.getVertexCount();
		ga.getStripVertexCounts(valid);
		int idx = valid[0];
		if(idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}


		valid[0] = idx + v.length;
		ga.setStripVertexCounts(valid);

		ga.setCoordinates(idx, v);

		// update colors
		Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	protected void addVerticesToGeometryArray(Point3f[] v) {
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		GeometryArray ga = (GeometryArray)getGeometry();
		int max = ga.getVertexCount();
		int idx = ga.getValidVertexCount();
		if(idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}


		ga.setValidVertexCount(idx + v.length);
		ga.setCoordinates(idx, v);

		// update colors
		Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	protected void recalculateNormals(GeometryArray ga) {
		if(ga == null)
			return;
		if((ga.getVertexFormat() & GeometryArray.NORMALS) == 0)
			return;
		GeometryInfo gi = new GeometryInfo(ga);
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		gi.unindexify();
		ga.setNormals(0, gi.getNormals());
	}

	protected void addVertices(Point3f[] v) {
		if(mesh == null)
			return;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null) {
			mesh.addAll(Arrays.asList(v));
			setGeometry(createGeometry());
			return;
		}

		if(ga instanceof GeometryStripArray)
			addVerticesToGeometryStripArray(v);
		else
			addVerticesToGeometryArray(v);
	}

	public void setColor(Color3f color) {
		this.color = color != null ? color : DEFAULT_COLOR;
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
		ta.setTransparency(this.transparency);
	}

	protected Appearance createAppearance () {
		Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.
					ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
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
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f,0.1f,0.1f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}

	protected abstract GeometryArray createGeometry();
}

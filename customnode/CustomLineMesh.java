package customnode;

import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class CustomLineMesh extends CustomMesh {
	
	public static final int PAIRWISE = 0;
	public static final int CONTINUOUS = 1;
	
	public static final int SOLID    = LineAttributes.PATTERN_SOLID;
	public static final int DOT      = LineAttributes.PATTERN_DOT;
	public static final int DASH     = LineAttributes.PATTERN_DASH;
	public static final int DASH_DOT = LineAttributes.PATTERN_DASH_DOT;
	
	public static final int DEFAULT_MODE = CONTINUOUS;
	public static final int DEFAULT_PATTERN = SOLID;
	public static final float DEFAULT_LINEWIDTH = 1.0f;

	private int mode = DEFAULT_MODE;
	
	public CustomLineMesh(List<Point3f> mesh, int mode) {
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.mode = mode;
		this.mesh = mesh;
		this.update();
	}
	
	public CustomLineMesh(List<Point3f> mesh, int mode, 
			Color3f color, float transparency) {
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.color = color;
		this.mesh = mesh;
		this.mode = mode;
		this.transparency = transparency;
		this.update();
	}
		
	public void setPattern(int pattern) {
		getAppearance().getLineAttributes().setLinePattern(pattern);
	}
	
	public void setAntiAliasing(boolean b) {
		getAppearance().getLineAttributes().setLineAntialiasingEnable(b);
	}
	
	public void setLineWidth(float w) {
		getAppearance().getLineAttributes().setLineWidth(w);
	}
	
	@Override
	public float getVolume() {
		return 0;
	}
	
	@Override
	protected Appearance createAppearance() {
		Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
		appearance.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_READ);
		
		LineAttributes lineAttrib = new LineAttributes();
		lineAttrib.setCapability(LineAttributes.ALLOW_ANTIALIASING_WRITE);
		lineAttrib.setCapability(LineAttributes.ALLOW_PATTERN_WRITE);
		lineAttrib.setCapability(LineAttributes.ALLOW_WIDTH_WRITE);
		lineAttrib.setLineWidth(DEFAULT_LINEWIDTH);
		lineAttrib.setLinePattern(DEFAULT_PATTERN);
		appearance.setLineAttributes(lineAttrib);
		
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

		Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f, 0.1f, 0.1f);
		material.setDiffuseColor(0.1f, 0.1f, 0.1f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	@Override
	protected Geometry createGeometry() {
		List<Point3f> tri = mesh;
		Point3f[] coords = (Point3f[])tri.toArray(new Point3f[]{});

		int N = coords.length;
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++)
			colors[i] = color;

		GeometryArray ta = null;
		if(mode == PAIRWISE) {
			ta = new LineArray(N,
					LineArray.COORDINATES |
					LineArray.COLOR_3);
		} else if (mode == CONTINUOUS) {
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
}

package customnode;

import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class CustomPointMesh extends CustomMesh {

	public static final float DEFAULT_POINT_SIZE = 1f;

	public CustomPointMesh(List<Point3f> mesh) {
		super(mesh);
	}

	public CustomPointMesh(List<Point3f> mesh,
						Color3f color, float transparency) {
		super(mesh, color, transparency);
	}

	public void setPointSize(float pointsize) {
		getAppearance().getPointAttributes().setPointSize(pointsize);
	}

	public void setAntiAliasing(boolean b) {
		getAppearance().getPointAttributes().setPointAntialiasingEnable(b);
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

		PointAttributes pointAttrib = new PointAttributes();
		pointAttrib.setCapability(PointAttributes.ALLOW_ANTIALIASING_WRITE);
		pointAttrib.setCapability(PointAttributes.ALLOW_SIZE_WRITE);
		pointAttrib.setPointSize(DEFAULT_POINT_SIZE);
		appearance.setPointAttributes(pointAttrib);

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
		ta = new PointArray(N,
					PointArray.COORDINATES |
					PointArray.COLOR_3);


		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		ta.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		ta.setCapability(GeometryArray.ALLOW_COUNT_READ);
		ta.setCapability(GeometryArray.ALLOW_INTERSECT);

		return ta;
	}
}

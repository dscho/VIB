package surfaceplot;

import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.measure.Calibration;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Color;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.List;
import java.util.ArrayList;

public final class SurfacePlot extends Shape3D {

	private ImageProcessor image;
	private float pw = 1, ph = 1;
	private int w, h;
	private IndexedQuadArray geometry;
	private Appearance appearance;
	private Color3f color = null;
	private float transparency = 0f;

	private float maxVal = -1;

	public SurfacePlot(ImagePlus image) {
		this(image, null, 0f, 1);
	}

	public SurfacePlot(ImagePlus image, Color3f color,
							float transp, int res) {
		this.image = image.getProcessor();
		pw = image.getWidth();
		ph = image.getHeight();
		this.color = color;
		this.transparency = transp;

		w = this.image.getWidth() / res;
		h = this.image.getHeight() / res;
		this.image.setInterpolate(true);
		this.image = this.image.resize(w, h);

		Calibration cal = image.getCalibration();
		pw = (float)(pw * cal.pixelWidth / w);
		ph = (float)(ph * cal.pixelHeight / h);

		calculateMax();

		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);

		geometry = createGeometry();
		appearance = createAppearance();
		setGeometry(geometry);
		setAppearance(appearance);
	}

	private void calculateMax() {
		int wh = w * h;
		for(int i = 0; i < wh; i++) {
			float v = image.getf(i);
			if(v > maxVal)
				maxVal = v;
		}
	}


	public void calculateMinMaxCenterPoint(Point3f min, 
				Point3f max, Point3f center) {

		min.x = 0; min.y = 0; min.z = 0;
		max.x = w * pw; max.y = h * ph; max.z = maxVal;
		center.x = max.x / 2;
		center.y = max.y / 2;
		center.z = max.z / 2;
	}

	public Color3f getColor() {
		return color;
	}

	public void setColor(Color3f color) {
		this.color = color;

		int N = geometry.getVertexCount();
		Color3f colors[] = new Color3f[N];
		Point3f coord = new Point3f();
		for(int i = 0; i < N; i++) {
			geometry.getCoordinate(i, coord);
			colors[i] = color != null
					? color
					: new Color3f(Color.getHSBColor(
						coord.z / maxVal, 1, 1));
		}
		geometry.setColors(0, colors);
	}

	private Appearance createAppearance () {
		Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.
					ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
// 		colorAttrib.setColor(color);
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

	private IndexedQuadArray createGeometry() {

		if(image == null)
			return null;
		int w = image.getWidth(), h = image.getHeight();
		int nQuads = (w - 1) * (h - 1);
		int nIndices = w * h;
		int nVertices = nQuads * 4;

		IndexedQuadArray ta = new IndexedQuadArray (nIndices,
					TriangleArray.COORDINATES |
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS,nVertices);

		Point3f[] coords = new Point3f[nIndices];
		Color3f colors[] = new Color3f[nIndices];
		for(int i = 0; i < nIndices; i++) {
			float y = ph * (i / w);
			float x = pw * (i % w);
			float v = image.getf(i);
			coords[i] = new Point3f(x, y, v);
			colors[i] = color != null
					? color
					: new Color3f(Color.getHSBColor(
						coords[i].z / maxVal, 1, 1));
		}
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);


		int[] indices = new int[nVertices];
		int index = 0;
		for(int y = 0; y < h-1; y++) {
			for(int x = 0; x < w-1; x++) {
				indices[index++] = y * w + x;
				indices[index++] = (y+1) * w + x;
				indices[index++] = (y+1) * w + x+1;
				indices[index++] = y * w + x+1;
			}
		}
		ta.setCoordinateIndices(0, indices);
		ta.setColorIndices(0, indices);

		// initialize the geometry info here
		GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		IndexedQuadArray result = (IndexedQuadArray)gi
					.getIndexedGeometryArray();
		result.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
		result.setCapability(TriangleArray.ALLOW_COUNT_READ);
		result.setCapability(TriangleArray.ALLOW_INTERSECT);

		return result;
	}
}

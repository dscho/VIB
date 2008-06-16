package surfaceplot;

import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.IJ;

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

import voltex.Volume;

public final class SurfacePlot extends Shape3D {

	private Volume volume;
	private int slice = 1;
	private float pw = 1, ph = 1;
	private int w, h, d;
	private int maxVal = -1;
	private float maxZ = -1;
	private float zFactor = 1;

	private IndexedQuadArray[] geometry;
	private Appearance appearance;

	private Color3f color = null;
	private float transparency = 0f;
	private boolean shaded = true;

	public SurfacePlot(Volume volume, Color3f color, 
					float transp, final int slice) {
		this.volume = volume;
		this.slice = slice;
		pw = (float)volume.pw;
		ph = (float)volume.ph;
		this.color = color;
		this.transparency = transp;

		w = this.volume.xDim;
		h = this.volume.yDim;
		d = this.volume.zDim;

		calculateMax();
		calculateZFactor();

		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);

		geometry = new IndexedQuadArray[d];
		geometry[slice] = createGeometry(slice);
		appearance = createAppearance();
		setGeometry(geometry[slice]);
		setAppearance(appearance);
		new Thread() {
			public void run() {
				for(int g = 0; g < d; g++) {
					if(g != slice) {
						geometry[g] = createGeometry(g);
						IJ.showProgress(g+1, d);
					}
				}
			}
		}.start();
	}

	public void setSlice(int slice) {
		this.slice = slice;
		setGeometry(geometry[slice-1]);
	}

	private void calculateMax() {
		int wh = w * h;
		maxVal = 0;
		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int v = volume.load(x, y, z);
					if(v > maxVal)
						maxVal = v;
				}
			}
		}
	}

	private void calculateZFactor() {
		float realW = w * pw;
		float realH = h * ph;
		maxZ = realW > realH ? realW : realH;
		float zFactor = maxZ / maxVal;
	}

	public void calculateMinMaxCenterPoint(Point3f min, 
				Point3f max, Point3f center) {

		min.x = 0; min.y = 0; min.z = 0;
		max.x = w * pw; max.y = h * ph; max.z = maxZ;
		center.x = max.x / 2;
		center.y = max.y / 2;
		center.z = max.z / 2;
	}

	public Color3f getColor() {
		return color;
	}

	public float getTransparency() {
		return transparency;
	}

	public void setTransparency(float t) {
		this.transparency = t;
		TransparencyAttributes tr = appearance
					.getTransparencyAttributes();
		int mode = transparency == 0f ? TransparencyAttributes.NONE
					: TransparencyAttributes.FASTEST;
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
	}

	public void setColor(Color3f color) {
		this.color = color;

		for(int g = 0; g < geometry.length; g++) {
			int N = geometry[g].getVertexCount();
			Color3f colors[] = new Color3f[N];
			Point3f coord = new Point3f();
			for(int i = 0; i < N; i++) {
				geometry[g].getCoordinate(i, coord);
				colors[i] = color != null ? color
					: new Color3f(Color.getHSBColor(
						coord.z / maxZ, 1, 1));
			}
			geometry[g].setColors(0, colors);
		}
	}

	public boolean isShaded() {
		return shaded;
	}

	public void setShaded(boolean b) {
		this.shaded = b;
		PolygonAttributes pa = appearance.getPolygonAttributes();
		if(b)
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}


	private Appearance createAppearance () {
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

	private IndexedQuadArray createGeometry(int g) {

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
			float v = zFactor * volume.load(i%w, i/w, g);
			coords[i] = new Point3f(x, y, v);
			colors[i] = color != null
					? color
					: new Color3f(Color.getHSBColor(
						v / maxZ, 1, 1));
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

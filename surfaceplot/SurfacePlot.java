package surfaceplot;

import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.measure.Calibration;

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

public final class SurfacePlot extends Shape3D {

	private ImageProcessor image;
	private float pw = 1, ph = 1;
	private int w, h;
	private QuadArray geometry;
	private Appearance appearance;

	private float zFactor = 1;
	private float maxVal = -1;

	public SurfacePlot(ImagePlus image) {
		this.image = image.getProcessor();
		Calibration cal = image.getCalibration();
		pw = (float)cal.pixelWidth;
		ph = (float)cal.pixelHeight;
		w = image.getWidth();
		h = image.getHeight();

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

		if(maxVal < 0)
			calculateMax();
		min.x = 0; min.y = 0; min.z = 0;
		max.x = w * pw; max.y = h * ph; max.z = maxVal * zFactor;
		center.x = max.x / 2;
		center.y = max.y / 2;
		center.z = max.z / 2;
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
		float transparency = 0;
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
	
	private QuadArray createGeometry() {

		if(image == null)
			return null;

		int w = image.getWidth(), h = image.getHeight();
		int nQuads = (w - 1) * (h - 1);
		int nVertices = nQuads * 4;

		Point3f[] coords = new Point3f[nVertices];
		Color3f colors[] = new Color3f[nVertices];
		for(int i = 0; i < nVertices; i++)
			colors[i] = new Color3f(1, 0, 0);
		
		QuadArray ta = new QuadArray (nVertices, 
					TriangleArray.COORDINATES | 
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS);

		// calculate coordinates
		int index = 0;
		for(int y = 0; y < h-1; y++) {
			for(int x = 0; x < w-1; x++) {
				float v00 = image.getf(x, y);
				float v10 = image.getf(x, y+1);
				float v01 = image.getf(x+1, y);
				float v11 = image.getf(x+1, y+1);

				coords[index++] = new Point3f(x * pw, y * ph, v00 * zFactor);
				coords[index++] = new Point3f(x * pw, (y+1) * ph, v10 * zFactor);
				coords[index++] = new Point3f((x+1) * pw, (y+1) * ph, v11 * zFactor);
				coords[index++] = new Point3f((x+1) * pw, y * ph, v01 * zFactor);
			}
		}
				
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);
		// initialize the geometry info here
		GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
		QuadArray result = (QuadArray)gi.getGeometryArray();
		result.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
		result.setCapability(TriangleArray.ALLOW_COUNT_READ);
		result.setCapability(TriangleArray.ALLOW_INTERSECT);
		
		return result;
	}
}

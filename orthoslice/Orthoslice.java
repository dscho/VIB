package orthoslice;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

import voltex.AxisRenderer;
import voltex.Texture2DVolume;

public class Orthoslice extends AxisRenderer {

	private Texture2DVolume texVol;
	private float transparency;
	private Color3f color;

	private int x, y, z;

	public Orthoslice(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img);
		this.x = img.getWidth()/2;
		this.y = img.getHeight()/2;
		this.z = img.getStackSize()/2;
		this.transparency = tr;
		this.color = color;
		texVol = new Texture2DVolume(volume, cmodel);
	}

	public void fullReload() {
		texVol.loadTexture();
		clearData();
		if (volume.hasData()) {
			loadQuads();
		}
		setWhichChild();
	}

	public void setWhichChild() {
		axisSwitch.setWhichChild(Switch.CHILD_ALL);
	}

	public void setSlices(int x, int y, int z) {
		if(this.x != x) {
			this.x = x;
			setXSlice(x);
		} else if(this.y != y) {
			this.y = y;
			setYSlice(y);
		} else if(this.z != z) {
			this.z = z;
			setZSlice(z);
		}
	}

	public void setThreshold(int threshold) {
		float  value = threshold / 255f;
		value = Math.min(1f, value);
		value = Math.max(0.1f, value);
		this.threshold = (int)Math.round(value*255);;
		for(int i = 0; i < axisSwitch.numChildren(); i++) {
			Group g = (Group)axisSwitch.getChild(i);
			int num = g.numChildren();
			for(int y = 0; y < num; y++) {
				Shape3D shape = (Shape3D)
					((Group)g.getChild(y)).getChild(0);
				shape.getAppearance().
					getRenderingAttributes().
					setAlphaTestValue(value);
			}
		}
	}

	public void setTransparency(float transparency) {
		this.transparency = transparency;
		for(int i = 0; i < axisSwitch.numChildren(); i++) {
			Group g = (Group)axisSwitch.getChild(i);
			int num = g.numChildren();
			for(int y = 0; y < num; y++) {
				Shape3D shape = (Shape3D)
					((Group)g.getChild(y)).getChild(0);
				shape.getAppearance().
					getTransparencyAttributes().
						setTransparency(transparency);
			}
		}
	}

	public void setColorModel(IndexColorModel cmodel) {
		texVol.setColorModel(cmodel);
		fullReload();
	}

	public void setColor(Color3f color) {
		this.color = color;
		Color3f c = color != null ? color : new Color3f(1f, 1f, 1f);
		for(int i = 0; i < axisSwitch.numChildren(); i++) {
			Group g = (Group)axisSwitch.getChild(i);
			int num = g.numChildren();
			for(int y = 0; y < num; y++) {
				Shape3D shape = (Shape3D)
					((Group)g.getChild(y)).getChild(0);
				shape.getAppearance().
					getColoringAttributes().
						setColor(c);
			}
		}
	}

	private void loadQuads() {
		loadAxis(Z_AXIS);
		loadAxis(Y_AXIS);
		loadAxis(X_AXIS);
	}

	private void setYSlice(int y) {
		Group g = (Group)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		int r = y;
		TexCoordGeneration tg = texVol.yTg;
		setCoordsY();
		setCurCoordY(r);
		Texture2D tex = texVol.yTextures[r];
		
		((QuadArray)shape.getGeometry()).setCoordinates(
				0, quadCoords);

		shape.getAppearance().setTexture(tex);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	private void setZSlice(int z) {
		Group g = (Group)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		int r = z;
		TexCoordGeneration tg = texVol.zTg;
		setCoordsZ();
		setCurCoordZ(r);
		Texture2D tex = texVol.zTextures[r];
		
		((QuadArray)shape.getGeometry()).setCoordinates(
				0, quadCoords);

		shape.getAppearance().setTexture(tex);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	private void setXSlice(int x) {
		Group g = (Group)axisSwitch.getChild(axisIndex[X_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		int r = x;
		TexCoordGeneration tg = texVol.xTg;
		setCoordsX();
		setCurCoordX(r);
		Texture2D tex = texVol.xTextures[r];
		
		((QuadArray)shape.getGeometry()).setCoordinates(
				0, quadCoords);

		shape.getAppearance().setTexture(tex);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	private void loadAxis(int axis) {
		int r = 0;		// number of tex maps to create
		OrderedGroup frontGroup = null;
		Texture2D[] textures = null;
		TexCoordGeneration tg = null;

		switch (axis) {
		case Z_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.
					getChild(axisIndex[Z_AXIS][FRONT]);
			r = z;
			textures = texVol.zTextures;
			tg = texVol.zTg;
			setCoordsZ();
			setCurCoordZ(r);
			break;
		case Y_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.
					getChild(axisIndex[Y_AXIS][FRONT]);
			r = y;
			textures = texVol.yTextures;
			tg = texVol.yTg;
			setCoordsY();
			setCurCoordY(r);
			break;
		case X_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.
					getChild(axisIndex[X_AXIS][FRONT]);
			r = x;
			textures = texVol.xTextures;
			tg = texVol.xTg;
			setCoordsX();
			setCurCoordX(r);
			break;
		}

		Texture2D tex = textures[r];

		QuadArray quadArray = new QuadArray(4, 
					GeometryArray.COORDINATES);
		quadArray.setCoordinates(0, quadCoords);
		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
		quadArray.setCapability(QuadArray.ALLOW_COORDINATE_WRITE);

		Appearance a = getAppearance(textures[r], tg);

		Shape3D frontShape = new Shape3D(quadArray, a);
		frontShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

		BranchGroup frontShapeGroup = new BranchGroup();
		frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
		frontShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		frontShapeGroup.addChild(frontShape);
		frontGroup.addChild(frontShapeGroup);
	} 

	private Appearance getAppearance(Texture tex, TexCoordGeneration tg) {
		Appearance a = new Appearance();
		a.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
		a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		TextureAttributes texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.COMBINE);
		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
		//texAttr.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);

		TransparencyAttributes t = new TransparencyAttributes();
		t.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		t.setTransparencyMode(TransparencyAttributes.BLENDED);
		t.setTransparency(transparency);

		PolygonAttributes p = new PolygonAttributes();
		p.setCullFace(PolygonAttributes.CULL_NONE);

		Material m = new Material();
		m.setLightingEnable(false);
		
		ColoringAttributes c = new ColoringAttributes();
		c.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		c.setShadeModel(ColoringAttributes.FASTEST);
		if(color == null) {
			c.setColor(1f, 1f, 1f);
		} else {
			c.setColor(color);
		}
		
		a.setMaterial(m);
		a.setTransparencyAttributes(t);
		a.setTextureAttributes(texAttr);
		a.setTexture(tex);
		a.setTexCoordGeneration(tg);
		a.setPolygonAttributes(p);
		a.setColoringAttributes(c);
		return a;
	}
}

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

	public Orthoslice(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img);
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

	private void loadAxis(int axis) {
		int r = 0;		// number of tex maps to create
		OrderedGroup frontGroup = null;
		OrderedGroup backGroup = null;
		Texture2D[] textures = null;
		TexCoordGeneration tg = null;

		switch (axis) {
		case Z_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]);
			backGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][BACK]);
			r = volume.zDim / 2;
			textures = texVol.zTextures;
			tg = texVol.zTg;
			setCoordsZ();
			break;
		case Y_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]);
			backGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][BACK]);
			r = volume.yDim / 2;
			textures = texVol.yTextures;
			tg = texVol.yTg;
			setCoordsY();
			break;
		case X_AXIS:
			frontGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][FRONT]);
			backGroup = 
			(OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][BACK]);
			r = volume.xDim / 2;
			textures = texVol.xTextures;
			tg = texVol.xTg;
			setCoordsX();
			break;
		}


		switch (axis) {
			case Z_AXIS: setCurCoordZ(r); break;
			case Y_AXIS: setCurCoordY(r); break;
			case X_AXIS: setCurCoordX(r); break;
		}

		Texture2D tex = textures[r];


		QuadArray quadArray = new QuadArray(4, 
					GeometryArray.COORDINATES);
		quadArray.setCoordinates(0, quadCoords);
		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);

		Appearance a = getAppearance(textures[r], tg);

		Shape3D frontShape = new Shape3D(quadArray, a);

		BranchGroup frontShapeGroup = new BranchGroup();
		frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
		frontShapeGroup.addChild(frontShape);
		frontGroup.addChild(frontShapeGroup);

		Shape3D backShape = new Shape3D(quadArray, a);

		BranchGroup backShapeGroup = new BranchGroup();
		backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
		backShapeGroup.addChild(backShape);
		backGroup.insertChild(backShapeGroup, 0);
	} 

	private Appearance getAppearance(Texture tex, TexCoordGeneration tg) {
		Appearance a = new Appearance();

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
		a.setTexture(tex);
		a.setTextureAttributes(texAttr);
		a.setTexCoordGeneration(tg);
		a.setPolygonAttributes(p);
		a.setColoringAttributes(c);
		return a;
	}
}

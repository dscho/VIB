package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

public class Axis2DRenderer extends Renderer implements VolRendConstants {

	private Texture2DVolume texVol;
	private float transparency;
	private Color3f color;

	private int curAxis = Z_AXIS;
	private int curDir = FRONT;

	protected Switch axisSwitch;
	private BranchGroup root;
	protected int[][] axisIndex = new int[3][2];

	public Axis2DRenderer(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img);
		this.transparency = tr;
		this.color = color;
		texVol = new Texture2DVolume(volume, cmodel);

		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK] = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK] = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK] = 5;

		axisSwitch = new Switch();
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_READ);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());


		root = new BranchGroup();
		root.addChild(axisSwitch);
		root.setCapability(BranchGroup.ALLOW_DETACH);
		root.setCapability(BranchGroup.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	public BranchGroup getVolumeNode() {
		return root;
	}

	protected void clearData() {
		clearGroup(axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[Z_AXIS][BACK]));
		clearGroup(axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[Y_AXIS][BACK]));
		clearGroup(axisSwitch.getChild(axisIndex[X_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[X_AXIS][BACK]));
	}

	private void clearGroup(Node node) {
		Group group = (Group) node;
		int numChildren = group.numChildren();
		for (int i = numChildren-1; i >= 0; i--) {
			group.removeChild(i);
		}
		if ((numChildren = group.numChildren()) > 0) {
			System.out.println("clearGroup(): still got a kid");
		}
	}

	private OrderedGroup getOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_READ);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		return og;
	}

	public void eyePtChanged(View view) {

		Point3d eyePt = getViewPosInLocal(view, root);
		if (eyePt != null) {
			Point3d  volRefPt = volume.volRefPt;
			Vector3d eyeVec = new Vector3d();
			eyeVec.sub(eyePt, volRefPt);

			// compensate for different xyz resolution/scale
			eyeVec.x /= volume.xSpace;
			eyeVec.y /= volume.ySpace;
			eyeVec.z /= volume.zSpace;

			// select the axis with the greatest magnitude 
			int axis = X_AXIS;
			double value = eyeVec.x;
			double max = Math.abs(eyeVec.x);
			if (Math.abs(eyeVec.y) > max) {
				axis = Y_AXIS;
				value = eyeVec.y;
				max = Math.abs(eyeVec.y);
			}
			if (Math.abs(eyeVec.z) > max) {
				axis = Z_AXIS;
				value = eyeVec.z;
				max = Math.abs(eyeVec.z);
			}

			// select the direction based on the sign of the magnitude
			int dir = value > 0.0 ? FRONT : BACK;

			if ((axis != curAxis) || (dir != curDir)) {
				curAxis = axis;
				curDir = dir;
				setWhichChild();
			}
		}
	}

	protected void setWhichChild() {
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
	}
	public void fullReload() {
		texVol.loadTexture();
		clearData();
		if (volume.hasData()) {
			loadQuads();
		}
		setWhichChild();
	}

	public void setThreshold(int threshold) {
//		float value = threshold/255f;
//		value = Math.min(1f, value);
//		value = Math.max(0.1f, value);
//		this.threshold = (int)Math.round(value * 255);
//		for(int i = 0; i < axisSwitch.numChildren(); i++) {
//			Group g = (Group)axisSwitch.getChild(i);
//			int num = g.numChildren();
//			for(int y = 0; y < num; y++) {
//				Shape3D shape = (Shape3D)
//					((Group)g.getChild(y)).getChild(0);
//				shape.getAppearance().
//					getRenderingAttributes().
//					setAlphaTestValue(value);
//			}
//		}
	}

	public void setTransparency(float transparency) {
//		this.transparency = transparency;
//		for(int i = 0; i < axisSwitch.numChildren(); i++) {
//			Group g = (Group)axisSwitch.getChild(i);
//			int num = g.numChildren();
//			for(int y = 0; y < num; y++) {
//				Shape3D shape = (Shape3D)
//					((Group)g.getChild(y)).getChild(0);
//				shape.getAppearance().
//					getTransparencyAttributes().
//						setTransparency(transparency);
//			}
//		}
	}

	public void setColorModel(IndexColorModel cmodel) {
//		texVol.setColorModel(cmodel);
//		fullReload();
	}

	public void setColor(Color3f color) {
//		this.color = color;
//		Color3f c = color != null ? color : new Color3f(1f, 1f, 1f);
//		for(int i = 0; i < axisSwitch.numChildren(); i++) {
//			Group g = (Group)axisSwitch.getChild(i);
//			int num = g.numChildren();
//			for(int y = 0; y < num; y++) {
//				Shape3D shape = (Shape3D)
//					((Group)g.getChild(y)).
//							getChild(0);
//				shape.getAppearance().
//					getColoringAttributes().
//						setColor(c);
//			}
//		}
	}

	private void loadQuads() {
		loadAxis(Z_AXIS);
		loadAxis(Y_AXIS);
		loadAxis(X_AXIS);
	}

	private void loadAxis(int axis) {
//		int rSize = 0;		// number of tex maps to create
//		OrderedGroup frontGroup = null;
//		OrderedGroup backGroup = null;
//		Texture2D[] textures = null;
//		TexCoordGeneration tg = null;
//
//		frontGroup = 
//		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][FRONT]);
//		backGroup = 
//		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][BACK]);
//		switch (axis) {
//		case Z_AXIS:
//			rSize = volume.zDim;
//			textures = texVol.zTextures;
//			tg = texVol.zTg;
//			setCoordsZ();
//			break;
//		case Y_AXIS:
//			rSize = volume.yDim;
//			textures = texVol.yTextures;
//			tg = texVol.yTg;
//			setCoordsY();
//			break;
//		case X_AXIS:
//			rSize = volume.xDim;
//			textures = texVol.xTextures;
//			tg = texVol.xTg;
//			setCoordsX();
//			break;
//		}
//
//		for (int i=0; i < rSize; i ++) { 
//
//			switch (axis) {
//			case Z_AXIS: setCurCoordZ(i); break;
//			case Y_AXIS: setCurCoordY(i); break;
//			case X_AXIS: setCurCoordX(i); break;
//			}
//
//			Texture2D tex = textures[i];
//
//
//			QuadArray quadArray = new QuadArray(4, 
//						GeometryArray.COORDINATES);
//
//			quadArray.setCoordinates(0, quadCoords);
//			quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
//
//			Appearance a = getAppearance(textures[i], tg);
//
//			Shape3D frontShape = new Shape3D(quadArray, a);
//			frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
//
//			BranchGroup frontShapeGroup = new BranchGroup();
//			frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
//			frontShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//			frontShapeGroup.addChild(frontShape);
//			frontGroup.addChild(frontShapeGroup);
//
//			Shape3D backShape = new Shape3D(quadArray, a);
//			backShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
//
//			BranchGroup backShapeGroup = new BranchGroup();
//			backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
//			backShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//			backShapeGroup.addChild(backShape);
//			backGroup.insertChild(backShapeGroup, 0);
//		} 
	} 

	private Appearance getAppearance(Texture tex, TexCoordGeneration tg) {
		Appearance a = new Appearance();
		a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
		a.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);

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

		// Avoid rendering of voxels having an alpha value of zero
		RenderingAttributes ra = new RenderingAttributes();
		ra.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
		ra.setAlphaTestValue(0.1f);
		ra.setAlphaTestFunction(RenderingAttributes.GREATER);
		
		a.setMaterial(m);
		a.setTransparencyAttributes(t);
		a.setTexture(tex);
		a.setTextureAttributes(texAttr);
		a.setTexCoordGeneration(tg);
		a.setPolygonAttributes(p);
		a.setColoringAttributes(c);
		a.setRenderingAttributes(ra);
		return a;
	}
}

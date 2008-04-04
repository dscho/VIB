package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

public class VolumeRenderer extends Renderer implements VolRendConstants {

	protected float transparency;
	protected Color3f color;

	private int curAxis = Z_AXIS;
	private int curDir = FRONT;

	private BranchGroup root;

	protected AppearanceCreator appCreator;
	protected GeometryCreator geomCreator;

	protected Switch axisSwitch;
	protected int[][] axisIndex = new int[3][2];

	public VolumeRenderer(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img);
		this.transparency = tr;
		this.color = color;
		appCreator = new AppearanceCreator(volume, cmodel);
		geomCreator = new GeometryCreator(volume);

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
		for(int i = 0; i < 6; i++)
			axisSwitch.addChild(getOrderedGroup());

		root = new BranchGroup();
		root.addChild(axisSwitch);
		root.setCapability(BranchGroup.ALLOW_DETACH);
		root.setCapability(BranchGroup.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	public BranchGroup getVolumeNode() {
		return root;
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
				axisSwitch.setWhichChild(
					axisIndex[curAxis][curDir]);
			}
		}
	}

	public void fullReload() {
		appCreator.loadTexture();
		for(int i = 0; i < axisSwitch.numChildren(); i++) {
			((Group)axisSwitch.getChild(i)).removeAllChildren();
		}
		if (volume.hasData()) {
			loadQuads();
		}
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
	}

	public void setThreshold(int threshold) {
		float value = threshold/255f;
		value = Math.min(1f, value);
		value = Math.max(0.1f, value);
		this.threshold = (int)Math.round(value * 255);
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
		appCreator.setColorModel(cmodel);
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
					((Group)g.getChild(y)).
							getChild(0);
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

	protected void loadAxis(int axis) {
		int rSize = 0;		// number of tex maps to create
		OrderedGroup frontGroup = null;
		OrderedGroup backGroup = null;

		frontGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][FRONT]);
		backGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][BACK]);
		switch (axis) {
			case Z_AXIS: rSize = volume.zDim; break;
			case Y_AXIS: rSize = volume.yDim; break;
			case X_AXIS: rSize = volume.xDim; break;
		}

		for (int i=0; i < rSize; i++) { 
			GeometryArray quadArray = 
				geomCreator.getQuad(axis, i);
			Appearance a = appCreator.getAppearance(
				axis, i, color, transparency);
			Shape3D frontShape = new Shape3D(quadArray, a);

			frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

			BranchGroup frontShapeGroup = new BranchGroup();
			frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
			frontShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
			frontShapeGroup.addChild(frontShape);
			frontGroup.addChild(frontShapeGroup);

			Shape3D backShape = new Shape3D(quadArray, a);
			backShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

			BranchGroup backShapeGroup = new BranchGroup();
			backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
			backShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
			backShapeGroup.addChild(backShape);
			backGroup.insertChild(backShapeGroup, 0);
		} 
	} 
}

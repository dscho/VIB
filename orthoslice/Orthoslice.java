package orthoslice;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;
import java.util.BitSet;

import voltex.VolumeRenderer;
import voltex.Volume;

public class Orthoslice extends VolumeRenderer {

	int[] slices = new int[3];
	int[] dimensions = new int[3];
	boolean[] visible = new boolean[3];
	BitSet whichChild = new BitSet(6);

	public Orthoslice(ImagePlus img, Color3f color, 
					float tr, boolean[] channels) {
		super(img, color, tr, channels);
		volume.setTransparencyType(Volume.OPAQUE);
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();
		for(int i = 0; i < 3; i++) {
			slices[i] = dimensions[i] / 2;
			visible[i] = true;
			whichChild.set(i, true);
			whichChild.set(i+3, true);
		}
	}

	protected void loadAxis(int axis) {
		OrderedGroup frontGroup = null;
		OrderedGroup backGroup = null;

		frontGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][FRONT]);
		backGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][BACK]);

		int i = slices[axis];

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

	public void eyePtChanged(View view) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		axisSwitch.setChildMask(whichChild);
	}

	public void setSlices(int[] v) {
		for(int i = 0; i < 3; i++)
			if(slices[i] != v[i])
				setSlice(i, v[i]);
	}

	public int[] getSlices() {
		return slices;
	}

	public int getSlice(int axis) {
		return slices[axis];
	}

	public boolean isVisible(int axis) {
		return visible[axis];
	}

	public boolean[] getVisible() {
		return visible;
	}

	public void setVisible(int axis, boolean b) {
		if(visible[axis] != b) {
			visible[axis] = b;
			whichChild.set(axisIndex[axis][FRONT], b);
			whichChild.set(axisIndex[axis][BACK], b);
			axisSwitch.setChildMask(whichChild);
		}
	}

	public void setVisible(boolean[] b) {
		for(int i = 0; i < b.length; i++)
			setVisible(i, b[i]);
	}

	public void decrease(int axis) {
		setSlice(axis, slices[axis]-1);
	}

	public void increase(int axis) {
		setSlice(axis, slices[axis]+1);
	}

	public void setSlice(int axis, int v) {
		if(v >= dimensions[axis] || v < 0)
			return;
		slices[axis] = v;
		Group g = (Group)axisSwitch.getChild(axisIndex[axis][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println(num + " children, expected only 1");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(axis, v);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(axis, v);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(axis, v);
		shape.getAppearance().setTexCoordGeneration(tg);
	}
}

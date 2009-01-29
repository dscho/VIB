package orthoslice;

import java.awt.*;
import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;
import java.util.BitSet;

import voltex.VolumeRenderer;

public class Orthoslice extends VolumeRenderer {

	int[] slices = new int[3];
	int[] dimensions = new int[3];
	boolean[] visible = new boolean[3];
	BitSet whichChild = new BitSet(6);

	public Orthoslice(ImagePlus img, Color3f color, 
					float tr, boolean[] channels) {
		super(img, color, tr, channels);
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

	@Override
	protected void loadAxis(int axis) {

		Group front = (Group)axisSwitch.getChild(axisIndex[axis][FRONT]);
		Group back  = (Group)axisSwitch.getChild(axisIndex[axis][BACK]);
		int i = slices[axis];
		loadAxis(axis, i, front, back);
	} 

	@Override
	public void eyePtChanged(View view) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		axisSwitch.setChildMask(whichChild);
	}

	public void setSlices(int[] v) {
		for(int i = 0; i < 3; i++)
			if(slices[i] != v[i])
				setSlice(i, v[i]);
	}

	public int getSlice(int axis) {
		return slices[axis];
	}

	public boolean isVisible(int axis) {
		return visible[axis];
	}

	public void setVisible(int axis, boolean b) {
		if(visible[axis] != b) {
			visible[axis] = b;
			whichChild.set(axisIndex[axis][FRONT], b);
			whichChild.set(axisIndex[axis][BACK], b);
			axisSwitch.setChildMask(whichChild);
		}
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
		TexCoordGeneration tg = appCreator.getTg(axis);
		shape.getAppearance().setTexCoordGeneration(tg);
	}
}

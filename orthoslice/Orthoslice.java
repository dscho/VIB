package orthoslice;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

import voltex.VolumeRenderer;

public class Orthoslice extends VolumeRenderer {

	private int x, y, z;
	private int w, h, d;

	public Orthoslice(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img, cmodel, color, tr);
		this.w = img.getWidth();
		this.h = img.getHeight();
		this.d = img.getStackSize();
		this.x = w / 2;
		this.y = h / 2;
		this.z = d / 2;
	}

	protected void loadAxis(int axis) {
		OrderedGroup frontGroup = null;
		OrderedGroup backGroup = null;

		frontGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][FRONT]);
		backGroup = 
		(OrderedGroup)axisSwitch.getChild(axisIndex[axis][BACK]);

		int i = z;
		switch(axis) {
			case X_AXIS: i = x; break;
			case Y_AXIS: i = y; break;
			case Z_AXIS: i = z; break;
		}

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
		axisSwitch.setWhichChild(Switch.CHILD_ALL);
	}

	public void setSlices(int x, int y, int z) {
		if(this.x != x)
			setXSlice(x);
		else if(this.y != y)
			setYSlice(y);
		else if(this.z != z)
			setZSlice(z);
	}

	public void decreaseX() {
		setXSlice(x-1);
	}

	public void decreaseY() {
		setYSlice(y-1);
	}

	public void decreaseZ() {
		setZSlice(z-1);
	}

	public void increaseX() {
		setXSlice(x+1);
	}

	public void increaseY() {
		setYSlice(y+1);
	}

	public void increaseZ() {
		setZSlice(z+1);
	}

	public void setYSlice(int y) {
		if(y >= h || y < 0)
			return;
		this.y = y;
		Group g = (Group)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println(num + " children, expected only 1");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(Y_AXIS, y);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(Y_AXIS, y);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(Y_AXIS, y);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	public void setZSlice(int z) {
		if(z >= d || z < 0)
			return;
		this.z = z;
		Group g = (Group)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println(num + " children, expected only 1");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(Z_AXIS, z);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(Z_AXIS, z);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(Z_AXIS, z);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	public void setXSlice(int x) {
		if(x >= w || x < 0)
			return;
		this.x = x;
		Group g = (Group)axisSwitch.getChild(axisIndex[X_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println(num + " children, expected only 1");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(X_AXIS, x);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(X_AXIS, x);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(X_AXIS, x);
		shape.getAppearance().setTexCoordGeneration(tg);
	}
}

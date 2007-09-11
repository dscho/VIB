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

	public Orthoslice(ImagePlus img, IndexColorModel cmodel, 
					Color3f color, float tr) {
		super(img, cmodel, color, tr);
		this.x = img.getWidth()/2;
		this.y = img.getHeight()/2;
		this.z = img.getStackSize()/2;
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

	private void setYSlice(int y) {
		Group g = (Group)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(Y_AXIS, y);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(Y_AXIS, y);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(Y_AXIS, y);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	private void setZSlice(int z) {
		Group g = (Group)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(Z_AXIS, z);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(Z_AXIS, z);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(Z_AXIS, z);
		shape.getAppearance().setTexCoordGeneration(tg);
	}

	private void setXSlice(int x) {
		Group g = (Group)axisSwitch.getChild(axisIndex[X_AXIS][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println("more than one child");
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

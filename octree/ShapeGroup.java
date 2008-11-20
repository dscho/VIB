package octree;

import java.awt.image.BufferedImage;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Shape3D;
import voltex.VolRendConstants;

public class ShapeGroup extends BranchGroup implements VolRendConstants {

	float pos;

	public ShapeGroup(CubeData cdata, int axis, int index, String name) {
		super();
		this.setCapability(ALLOW_DETACH);
		Shape3D shape = new Shape3D(
			createGeometry(cdata, axis, index),
			createAppearance(cdata, axis, index));
		this.addChild(shape);
		this.setName(name);
	}

	void setCubeData(CubeData cdata, int axis, int index, String name) {
		Shape3D shape = (Shape3D)this.getChild(0);
		((GeometryArray)shape.getGeometry()).setCoordinates(0, 
			GeometryCreator.instance().getQuadCoordinates(cdata, axis, index));
		this.pos = GeometryCreator.instance().getPos();
		Appearance app = shape.getAppearance();
		app.setTexCoordGeneration(AppearanceCreator.instance().getTg(cdata, axis, index));
		BufferedImage img = null;
		switch(axis) {
			case X_AXIS: img = cdata.xImages[index];
			case Y_AXIS: img = cdata.yImages[index];
			case Z_AXIS: img = cdata.zImages[index];
		}
		((ImageComponent2D)app.getTexture().getImage(0)).set(img);
		shape.setAppearance(createAppearance(cdata, axis, index));
		this.setName(name);
	}

	GeometryArray createGeometry(CubeData cdata, int axis, int index) {
		GeometryArray arr = GeometryCreator.instance().getQuad(cdata, axis, index);
		this.pos = GeometryCreator.instance().getPos();
		return arr;
	}

	Appearance createAppearance(CubeData cdata, int axis, int index) {
		return AppearanceCreator.instance().getAppearance(cdata, axis, index);
	}
}

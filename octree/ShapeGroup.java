package octree;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;

public class ShapeGroup extends BranchGroup {

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
		shape.setGeometry(createGeometry(cdata, axis, index));
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

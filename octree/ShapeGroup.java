package octree;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Shape3D;

public class ShapeGroup implements Comparable {

	float pos;
	Shape3D shape;

	public ShapeGroup() {
		shape = new Shape3D();
		shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	}

	public void prepareForAxis(float pos) {
		this.pos = pos;
	}

	public void show(CubeData cdata, int index) {
		shape.setGeometry(createGeometry(cdata, index));
		shape.setAppearance(createAppearance(cdata, index));
	}

	public void hide() {
		shape.setGeometry(null);
		shape.setAppearance(null);
	}

	private static GeometryArray createGeometry(CubeData cdata, int index) {
		GeometryArray arr = GeometryCreator.instance().getQuad(cdata, index);
		return arr;
	}

	private static Appearance createAppearance(CubeData cdata, int index) {
		return AppearanceCreator.instance().getAppearance(cdata, index);
	}

	public int compareTo(Object o) {
		ShapeGroup sg = (ShapeGroup)o;
		if(pos < sg.pos) return -1;
		if(pos > sg.pos) return +1;
		return 0;
	}

	/*
	 * Used in displayInitial.
	 */
	public ShapeGroup duplicate() {
		ShapeGroup ret = new ShapeGroup();
		ret.shape.setGeometry(shape.getGeometry());
		ret.shape.setAppearance(shape.getAppearance());
		return ret;
	}
}


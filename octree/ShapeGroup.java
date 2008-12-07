package octree;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Shape3D;

public class ShapeGroup implements Comparable {

	float pos;
	Cube cube;
	Shape3D shape;

	public ShapeGroup(Cube cube) {
		this.cube = cube;
	}

	public ShapeGroup(ShapeGroup sg) {
		Shape3D sgShape = sg.shape;
		this.shape = new Shape3D(sgShape.getGeometry(), sgShape.getAppearance());
		this.cube = sg.cube;
		this.pos = sg.pos;
	}

	void setCubeData(Cube c, CubeData cdata, int index) {
		this.cube = c;
		if(this.shape == null) {
			shape = new Shape3D(createGeometry(cdata, index),
					createAppearance(cdata, index));
			return;
		}
		((GeometryArray)shape.getGeometry()).setCoordinates(0, 
			GeometryCreator.instance().getQuadCoordinates(cdata, index));
		Appearance app = shape.getAppearance();
		app.setTexCoordGeneration(cdata.tg);
		((ImageComponent2D)app.getTexture().getImage(0)).set(cdata.images[index]);
	}

	private GeometryArray createGeometry(CubeData cdata, int index) {
		GeometryArray arr = GeometryCreator.instance().getQuad(cdata, index);
		return arr;
	}

	private Appearance createAppearance(CubeData cdata, int index) {
		return AppearanceCreator.instance().getAppearance(cdata, index);
	}

	public int compareTo(Object o) {
		ShapeGroup sg = (ShapeGroup)o;
		if(pos < sg.pos) return -1;
		if(pos > sg.pos) return +1;
		return 0;
	}
}

package octree;

import java.util.LinkedList;
import java.util.List;
import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import voltex.AppearanceCreator;
import voltex.GeometryCreator;
import voltex.Volume;

public class ShapeGroupRecycler {
	
	private static ShapeGroupRecycler instance = null;
	
	private List<ShapeGroup> pool;

	private AppearanceCreator appCreator;
	private GeometryCreator geomCreator;

	private Volume lastVolume = null;

	private ShapeGroupRecycler() {
		appCreator = new AppearanceCreator();
		geomCreator = new GeometryCreator();
		pool = new LinkedList<ShapeGroup>();
	}

	public void clearAll() {
		pool.clear();
	}

	public static ShapeGroupRecycler instance() {
		if(instance == null)
			instance = new ShapeGroupRecycler();
		return instance;
	}

	public ShapeGroup newShapeGroup(Volume volume, int axis, int index, String name) {
		if(lastVolume == null || volume != lastVolume) {
			geomCreator.setVolume(volume);
			appCreator.setVolume(volume);
			lastVolume = volume;
		}
		GeometryArray g = geomCreator.getQuad(axis, index);
		Appearance a = appCreator.getAppearance(axis, index);
		float pos = geomCreator.getPos();
		if(pool.isEmpty())
			return new ShapeGroup(new Shape3D(g, a), pos, name);

		ShapeGroup sg = pool.remove(0);
		sg.pos = pos;
		sg.setName(name);
		Shape3D shape = (Shape3D)sg.getChild(0);
		shape.setAppearance(a);
		shape.setGeometry(g);

		return sg;
	}

	public void deleteShapeGroup(ShapeGroup sg) {
		pool.add(sg);
	}
}

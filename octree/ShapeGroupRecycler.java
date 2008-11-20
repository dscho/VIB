package octree;

import java.util.LinkedList;
import java.util.List;

public class ShapeGroupRecycler {
	
	private static ShapeGroupRecycler instance = null;
	
	private List<ShapeGroup> pool;

	private ShapeGroupRecycler() {
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

	public ShapeGroup newShapeGroup(CubeData cdata, int axis, int index, String name) {
		return new ShapeGroup(cdata, axis, index, name);
	}

	public void deleteShapeGroup(ShapeGroup sg) {
		pool.add(sg);
	}
}

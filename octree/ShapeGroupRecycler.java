package octree;

import java.util.LinkedList;

public class ShapeGroupRecycler {
	
	private static ShapeGroupRecycler instance = null;
	
	private LinkedList<ShapeGroup> pool;

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
		ShapeGroup sg = pool.pollFirst();
		if(sg == null)
			return new ShapeGroup(cdata, axis, index, name);
		sg.setCubeData(cdata, axis, index, name);
		return sg;
	}

	public void deleteShapeGroup(ShapeGroup sg) {
		pool.add(sg);
	}
}

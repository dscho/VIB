package octree;

import java.util.LinkedList;

public class CubeDataRecycler {
	
	private static CubeDataRecycler instance = null;
	
	private LinkedList<CubeData> pool;

	private CubeDataRecycler() {
		pool = new LinkedList<CubeData>();
	}

	public void clearAll() {
		pool.clear();
	}

	public static CubeDataRecycler instance() {
		if(instance == null)
			instance = new CubeDataRecycler();
		return instance;
	}

	public CubeData newCubeData(String path, float ox, float oy, float oz, float pw, float ph, float pd) {
		CubeData cdata = pool.pollFirst();
		if(cdata == null)
			return new CubeData(path, ox, oy, oz, pw, ph, pd);
		cdata.set(path, ox, oy, oz, pw, ph, pd);
		return cdata;
	}

	public void deleteCubeData(CubeData cdata) {
		pool.add(cdata);
	}
}

package octree;

import java.util.LinkedList;

public class CubeDataRecycler {
	
	private static CubeDataRecycler instance = null;
	private static int CACHE_SIZE;
	
	private CubeData[] pool;
	private int size;

	private CubeDataRecycler() {
		pool = new CubeData[CACHE_SIZE];
		size = 0;
	}

	public void clearAll() {
		for(int i = 0; i < size; i++)
			pool[i] = null;
		size = 0;
	}

	public static CubeDataRecycler instance() {
		if(instance == null)
			instance = new CubeDataRecycler();
		return instance;
	}

	public CubeData newCubeData(Cube cube) {
		if(size == 0)
			return new CubeData(cube);
		size--;
		CubeData cdata = pool[size];
		cdata.setCube(cube);
		return cdata;
	}

	public void deleteCubeData(CubeData cdata) {
		if(size < CACHE_SIZE)
			pool[size++] = cdata;
	}
}

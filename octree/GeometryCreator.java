package octree;

import javax.media.j3d.*;
import voltex.VolRendConstants;

public class GeometryCreator implements VolRendConstants {

	private float[] quadCoords = new float[12];
	private float pos;
	private static GeometryCreator instance;

	private GeometryCreator() {}

	public static GeometryCreator instance() {
		if(instance == null)
			instance = new GeometryCreator();
		return instance;
	}

	public float getPos() {
		return pos;
	}

	public GeometryArray getQuad(CubeData cdata, int index) {
		calculateQuad(cdata, index);
		QuadArray quadArray = new QuadArray(4, 
					GeometryArray.COORDINATES);

		quadArray.setCoordinates(0, quadCoords);
		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
		return quadArray;
	}

	public float[] getQuadCoordinates(CubeData cdata, int index) {
		calculateQuad(cdata, index);
		return quadCoords;
	}

	private void calculateQuad(CubeData cdata, int index) {
		switch(cdata.axis) {
			case X_AXIS: 	
				setCoordsY(cdata);
				setCoordsZ(cdata);
				setCurCoordX(index, cdata); 
				break;
			case Y_AXIS:
				setCoordsX(cdata);
				setCoordsZ(cdata);
				setCurCoordY(index, cdata); 
				break;
			case Z_AXIS:
				setCoordsX(cdata);
				setCoordsY(cdata);
				setCurCoordZ(index, cdata); 
				break;
		}
	}

	private void setCurCoordX(int i, CubeData cdata) {
		float curX = i * cdata.pw + cdata.minX;
		pos = (float)curX;
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	private void setCurCoordY(int i, CubeData cdata) {
		float curY = i * cdata.ph + cdata.minY;
		pos = (float)curY;
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	private void setCurCoordZ(int i, CubeData cdata) {
		float curZ = i * cdata.pd + cdata.minZ;
		pos = (float)curZ;
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void setCoordsX(CubeData cdata) {
		// lower left
		quadCoords[1] = cdata.minY;
		quadCoords[2] = cdata.minZ;
		// lower right
		quadCoords[4] = cdata.maxY;
		quadCoords[5] = cdata.minZ;
		// upper right
		quadCoords[7] = cdata.maxY;
		quadCoords[8] = cdata.maxZ;
		// upper left
		quadCoords[10] = cdata.minY;
		quadCoords[11] = cdata.maxZ;
	}

	private void setCoordsY(CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.minX;
		quadCoords[2] = cdata.minZ;
		// lower right
		quadCoords[3] = cdata.minX;
		quadCoords[5] = cdata.maxZ;
		// upper right
		quadCoords[6] = cdata.maxX;
		quadCoords[8] = cdata.maxZ;
		// upper left
		quadCoords[9] = cdata.maxX;
		quadCoords[11] = cdata.minZ;
	}

	private void setCoordsZ(CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.minX;
		quadCoords[1] = cdata.minY;
		// lower right
		quadCoords[3] = cdata.maxX;
		quadCoords[4] = cdata.minY;
		// upper right
		quadCoords[6] = cdata.maxX;
		quadCoords[7] = cdata.maxY;
		// upper left
		quadCoords[9] = cdata.minX;
		quadCoords[10] = cdata.maxY;
	}
}

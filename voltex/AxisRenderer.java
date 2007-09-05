package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

abstract public class AxisRenderer implements VolRendConstants{

	protected double[] quadCoords;
	protected Volume volume;

	public AxisRenderer(Volume volume) {
		this.volume = volume;
	}

	protected void setCurCoordX(int i) {
		double curX = i * volume.xSpace;
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	protected void setCurCoordY(int i) {
		double curY = i * volume.ySpace;
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	protected void setCurCoordZ(int i) {
		double curZ = i * volume.zSpace;
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void checkCoords() {
		if ((quadCoords == null) || (quadCoords.length != 12)) {
			quadCoords = new double[12];
		}
	}

	protected void setCoordsX() {
		checkCoords();

		// lower left
		quadCoords[1] = volume.minCoord.y;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[4] = volume.maxCoord.y;
		quadCoords[5] = volume.minCoord.z;
		// upper right
		quadCoords[7] = volume.maxCoord.y;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[10] = volume.minCoord.y;
		quadCoords[11] = volume.maxCoord.z;
	}

	protected void setCoordsY() {
		checkCoords();

		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[3] = volume.minCoord.x;
		quadCoords[5] = volume.maxCoord.z;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[9] = volume.maxCoord.x;
		quadCoords[11] = volume.minCoord.z;
	}

	protected void setCoordsZ() {
		checkCoords();

		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[1] = volume.minCoord.y;
		// lower right
		quadCoords[3] = volume.maxCoord.x;
		quadCoords[4] = volume.minCoord.y;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[7] = volume.maxCoord.y;
		// upper left
		quadCoords[9] = volume.minCoord.x;
		quadCoords[10] = volume.maxCoord.y;
	}
}

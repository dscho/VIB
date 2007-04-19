package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;

abstract public class AxisRenderer extends Renderer 
				implements VolRendConstants{


	protected Switch axisSwitch;
	private BranchGroup root;
	protected int[][] axisIndex = new int[3][2];
	protected double[] quadCoords;

	private int	curAxis = Z_AXIS;
	private int	curDir = FRONT;

	public AxisRenderer(ImagePlus image) {
		super(image);
		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK] = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK] = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK] = 5;

		axisSwitch = new Switch();
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_READ);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());
		axisSwitch.addChild(getOrderedGroup());


		root = new BranchGroup();
		root.addChild(axisSwitch);
		root.setCapability(BranchGroup.ALLOW_DETACH);
		root.setCapability(BranchGroup.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	public BranchGroup getVolumeNode() {
		return root;
	}

	protected void clearData() {
		clearGroup(axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[Z_AXIS][BACK]));
		clearGroup(axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[Y_AXIS][BACK]));
		clearGroup(axisSwitch.getChild(axisIndex[X_AXIS][FRONT]));
		clearGroup(axisSwitch.getChild(axisIndex[X_AXIS][BACK]));
	}

	protected void clearGroup(Node node) {
		Group group = (Group) node;
		int numChildren = group.numChildren();
		for (int i = numChildren-1; i >= 0; i--) {
			group.removeChild(i);
		}
		if ((numChildren = group.numChildren()) > 0) {
			System.out.println("clearGroup(): still got a kid");
		}
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

	protected OrderedGroup getOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_READ);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		return og;
	}

	public void eyePtChanged(View view) {

		Point3d eyePt = getViewPosInLocal(view, root);
		if (eyePt != null) {
			Point3d  volRefPt = volume.volRefPt;
			Vector3d eyeVec = new Vector3d();
			eyeVec.sub(eyePt, volRefPt);

			// compensate for different xyz resolution/scale
			eyeVec.x /= volume.xSpace;
			eyeVec.y /= volume.ySpace;
			eyeVec.z /= volume.zSpace;

			// select the axis with the greatest magnitude 
			int axis = X_AXIS;
			double value = eyeVec.x;
			double max = Math.abs(eyeVec.x);
			if (Math.abs(eyeVec.y) > max) {
				axis = Y_AXIS;
				value = eyeVec.y;
				max = Math.abs(eyeVec.y);
			}
			if (Math.abs(eyeVec.z) > max) {
				axis = Z_AXIS;
				value = eyeVec.z;
				max = Math.abs(eyeVec.z);
			}

			// select the direction based on the sign of the magnitude
			int dir = value > 0.0 ? FRONT : BACK;

			if ((axis != curAxis) || (dir != curDir)) {
				curAxis = axis;
				curDir = dir;
				setWhichChild();
			}
		}
	}

	protected void setWhichChild() {
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
	}
}

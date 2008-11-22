package octree;

import java.util.Arrays;
import javax.media.j3d.Group;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.Switch;
import voltex.VolRendConstants;

public class ShapeContainer implements VolRendConstants {
	
	final int xdim, ydim, zdim;
	final float pw, ph, pd;
	final int CUBE_SIZE = VolumeOctree.SIZE;

	static final int DETAIL_AXIS = 6;

	private static final int[][] axisIndex = new int[3][2];

	private boolean cancelUpdating = false;

	final Switch axisSwitch;

	public ShapeContainer(int xdim, int ydim, int zdim,
			float pw, float ph, float pd) {
		this.xdim = xdim;
		this.ydim = ydim;
		this.zdim = zdim;

		this.pw = pw;
		this.ph = ph;
		this.pd = pd;

		System.out.println("xdim = " + xdim);
		System.out.println("ydim = " + ydim);
		System.out.println("zdim = " + zdim);


		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK]  = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK]  = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK]  = 5;

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

		axisSwitch.addChild(getOrderedGroup());
	}

	public synchronized boolean isCancelUpdating() {
		return cancelUpdating;
	}

	public synchronized void setCancelUpdating(boolean b) {
		cancelUpdating = b;
	}

	public int countInitialShapes() {
		int sum = 0;
		for(int i = 0; i < 6; i++) {
			OrderedGroup og = (OrderedGroup)axisSwitch.getChild(i);
			sum += og.numChildren();
		}
		return sum;
	}

	public int countDetailShapes() {
		return ((Group)axisSwitch.getChild(DETAIL_AXIS)).numChildren();
	}

	public void displayRoughCube(Cube c) {
		OrderedGroup fg, bg;
		int[] axis = new int[] {X_AXIS, Y_AXIS, Z_AXIS};

		for(int ai = 0; ai < 3; ai++) {
			CubeData cdata = new CubeData(c);
			cdata.prepareForAxis(axis[ai]);
			try {
				cdata.createData();
			} catch(Exception e) {
				e.printStackTrace();
			}
			Arrays.sort(cdata.shapes);
			fg = (OrderedGroup)axisSwitch.getChild(axisIndex[axis[ai]][FRONT]);
			bg = (OrderedGroup)axisSwitch.getChild(axisIndex[axis[ai]][BACK]);
			for(int i = 0; i < CUBE_SIZE; i++) {
				fg.addChild(new ShapeGroup(cdata.shapes[i]));
				bg.insertChild(cdata.shapes[i], 0);
			}
		}
	}

	private final Group getOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		og.setCapability(OrderedGroup.ALLOW_CHILD_INDEX_ORDER_WRITE);
		return og;
	}
}

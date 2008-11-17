package voltex;

import javax.media.j3d.Group;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.Switch;

public class ShapeContainer implements VolRendConstants {
	
	final int xdim, ydim, zdim;
	final double pw, ph, pd;
	final int CUBE_SIZE = VolumeOctree.SIZE;

	private static final int[][] axisIndex = new int[3][2];

	final Switch axisSwitch;

	public ShapeContainer(int xdim, int ydim, int zdim,
			double pw, double ph, double pd) {
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
		axisSwitch.setWhichChild(axisIndex[Z_AXIS][FRONT]);
	}

	public void setAxis(int axis, int dir){
//		axisSwitch.setWhichChild(axisIndex[axis][dir]);
		this.curAxis = axis;
		this.curDir = dir;
	}

	private int curAxis = Z_AXIS;
	private int curDir = FRONT;

	public void displayCube(Cube c) {
		System.out.println("display cube " + c);
		if(c.isDisplayed())
			return;
		ShapeGroup[] shapes;

		// z-direction
		shapes = c.getZFrontShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT])).addChild(s);
		shapes = c.getZBackShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][BACK])).addChild(s);

		// y-direction
		shapes = c.getYFrontShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT])).addChild(s);
		shapes = c.getYBackShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][BACK])).addChild(s);

		// x-direction
		shapes = c.getXFrontShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][FRONT])).addChild(s);
		shapes = c.getXBackShapes();
		for(ShapeGroup s : shapes)
			((OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][BACK])).addChild(s);

	}

	public void undisplayCube(Cube c) {
		System.out.println("undisplay cube " + c);
		if(!c.isDisplayed())
			return;

		for(int i = 0; i < 6; i++) {
			OrderedGroup og = (OrderedGroup)axisSwitch.getChild(i);
			int n = og.numChildren();
			for(int k = n-1; k >= 0; k--) {
				ShapeGroup sg = (ShapeGroup)og.getChild(k);
				if(sg.getName().equals(c.name)) {
					og.removeChild(sg);
				}
			}
		}
	}
//	public void undisplayCube(Cube c) {
//		System.out.println("undisplay cube " + c);
//		if(!c.isDisplayed())
//			return;
//		ShapeGroup[] shapes;
//
//		// z-direction
//		shapes = c.getZFrontShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT])).removeChild(s);
//		shapes = c.getZBackShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][BACK])).removeChild(s);
//
//		// y-direction
//		shapes = c.getYFrontShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT])).removeChild(s);
//		shapes = c.getYBackShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][BACK])).removeChild(s);
//
//		// x-direction
//		shapes = c.getXFrontShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][FRONT])).removeChild(s);
//		shapes = c.getXBackShapes();
//		for(ShapeGroup s : shapes)
//			((OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][BACK])).removeChild(s);
//
//	}

	public void sort() {
		OrderedGroup og;
		int n;
		int[] indices;

		// z-dir
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][FRONT]);
		n = og.numChildren();
		indices = new int[n];
		for(int i = 0; i < n; i++)
			indices[i] = i;
		sortAscending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[Z_AXIS][BACK]);
		for(int i = 0; i < n; i++)
			indices[i] = i;
		sortDescending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);

		// y-dir
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][FRONT]);
		n = og.numChildren();
		indices = new int[n];
		for(int i = 0; i < n; i++)
			indices[i] = i;
		sortAscending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);
		for(int i = 0; i < n; i++)
			indices[i] = i;
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[Y_AXIS][BACK]);
		sortDescending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);

		// x-dir
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][FRONT]);
		n = og.numChildren();
		indices = new int[n];
		for(int i = 0; i < n; i++)
			indices[i] = i;
		sortAscending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);
		for(int i = 0; i < n; i++)
			indices[i] = i;
		og = (OrderedGroup)axisSwitch.getChild(axisIndex[X_AXIS][BACK]);
		sortDescending(og, indices, 0, n-1);
		og.setChildIndexOrder(indices);

	}

	private final Group getOrderedGroup() {
		OrderedGroup og = new OrderedGroup();
//		og.setCapability(Group.ALLOW_CHILDREN_READ);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		og.setCapability(OrderedGroup.ALLOW_CHILD_INDEX_ORDER_WRITE);
		return og;
	}

	private static final void sortDescending(OrderedGroup shapes, int[] indices, int left, int right) {
	     if(right > left) {
		     int piv = (left + right) / 2;
		     int pivNew = partitionDescending(shapes, indices, left, right, piv);
		     sortDescending(shapes, indices, left, pivNew - 1);
		     sortDescending(shapes, indices, pivNew + 1, right);
	     }
	}

	private static final int partitionDescending(OrderedGroup shapes, int[] indices, int left, int right, int piv) {
		float pivValue = ((ShapeGroup)shapes.getChild(indices[piv])).pos;
		swap(indices, piv, right);
		int storeIndex = left;
		for(int i = left; i < right; i++) {
			if(((ShapeGroup)shapes.getChild(indices[i])).pos >= pivValue) {
				swap(indices, i, storeIndex);
				storeIndex++;
			}
		}
		swap(indices, storeIndex, right);
		return storeIndex;
	}

	private static final void sortAscending(OrderedGroup shapes, int[] indices, int left, int right) {
	     if(right > left) {
		     int piv = (left + right) / 2;
		     int pivNew = partitionAscending(shapes, indices, left, right, piv);
		     sortAscending(shapes, indices, left, pivNew - 1);
		     sortAscending(shapes, indices, pivNew + 1, right);
	     }
	}

	private static final int partitionAscending(OrderedGroup shapes, int[] indices, int left, int right, int piv) {
		float pivValue = ((ShapeGroup)shapes.getChild(indices[piv])).pos;
		swap(indices, piv, right);
		int storeIndex = left;
		for(int i = left; i < right; i++) {
			if(((ShapeGroup)shapes.getChild(indices[i])).pos <= pivValue) {
				swap(indices, i, storeIndex);
				storeIndex++;
			}
		}
		swap(indices, storeIndex, right);
		return storeIndex;
	}

	private static final void swap(int[] array, int ind1, int ind2) {
		int tmp = array[ind1];
		array[ind1] = array[ind2];
		array[ind2] = tmp;
	}
}

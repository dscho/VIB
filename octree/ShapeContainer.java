package octree;

import ij.ImagePlus;
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

	public int countShapeGroups() {
		int sum = 0;
		for(int i = 0; i < 7; i++) {
			OrderedGroup og = (OrderedGroup)axisSwitch.getChild(i);
			sum += og.numChildren();
		}
		return sum;
	}

	public void displayRoughCube(Cube c) {
//		ImagePlus imp = CubeOpener.openCube(c.dir, c.name + ".tif");
//		Volume volume = new Volume(imp, Volume.TRANSLUCENT);
//		AppearanceCreator appCreator = new AppearanceCreator(volume);
//		GeometryCreator geomCreator = new GeometryCreator(volume);
		OrderedGroup fg, bg;
//		float pos;
		int[] axis = new int[] {X_AXIS, Y_AXIS, Z_AXIS};
		CubeData cdata = new CubeData(c.path, pw * c.x, ph * c.y, pd * c.z);
		try {
			cdata.loadZData();
			cdata.createXData();
			cdata.createYData();
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}

		for(int ai = 0; ai < 3; ai++) {
			fg = (OrderedGroup)axisSwitch.getChild(axisIndex[axis[ai]][FRONT]);
			bg = (OrderedGroup)axisSwitch.getChild(axisIndex[axis[ai]][BACK]);
			for(int i = 0; i < CUBE_SIZE; i++) {
//				GeometryArray g = geomCreator.getQuad(axis[ai], i);
//				Appearance a = appCreator.getAppearance(axis[ai], i);
//				pos = geomCreator.getPos();
//				fg.addChild(new ShapeGroup(new Shape3D(g, a), pos, c.name));
//				bg.addChild(new ShapeGroup(new Shape3D(g, a), pos, c.name));
//				insertAscending(fg, new ShapeGroup(new Shape3D(g, a), pos, c.name), 0, fg.numChildren()-1);
//				insertDescending(bg, new ShapeGroup(new Shape3D(g, a), pos, c.name), 0, bg.numChildren()-1);
				insertAscending(fg, new ShapeGroup(cdata, axis[ai], i, c.name), 0, fg.numChildren()-1);
				insertDescending(bg, new ShapeGroup(cdata, axis[ai], i, c.name), 0, bg.numChildren()-1);
			}
		}
	}

	public int displayCube(Cube c, int whichChild) {
		int curAxis = whichChild / 2;
		int curDir = whichChild % 2;
		System.out.println("display cube " + c);

		ImagePlus imp = CubeOpener.openCube(c.dir, c.name + ".tif");
//		Volume volume = new Volume(imp, Volume.TRANSLUCENT);

		OrderedGroup og = (OrderedGroup)axisSwitch.getChild(DETAIL_AXIS);
//		int dim = volume.zDim;
//		switch(curAxis) {
//			case Z_AXIS: dim = volume.zDim; break;
//			case Y_AXIS: dim = volume.yDim; break;
//			case X_AXIS: dim = volume.xDim; break;
//		}

		for(int i = 0; i < CUBE_SIZE; i++) {
//			ShapeGroup sg = ShapeGroupRecycler.instance().newShapeGroup(volume, curAxis, i, c.name);
			ShapeGroup sg = ShapeGroupRecycler.instance().newShapeGroup(c.getCubeData(), curAxis, i, c.name);
			if(curDir == FRONT)
				insertAscending(og, sg, 0, og.numChildren()-1);
			else
				insertDescending(og, sg, 0, og.numChildren()-1);
		}
		return whichChild;
	}

	public void undisplayCube(Cube c, int axishint) {
		System.out.println("undisplay cube " + c);
//		OrderedGroup og = (OrderedGroup)axisSwitch.getChild(axishint);
		OrderedGroup og = (OrderedGroup)axisSwitch.getChild(DETAIL_AXIS);
		int n = og.numChildren();
		for(int k = n-1; k >= 0; k--) {
			ShapeGroup sg = (ShapeGroup)og.getChild(k);
			if(sg.getName().equals(c.name)) {
				og.removeChild(sg);
				ShapeGroupRecycler.instance().deleteShapeGroup(sg);
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

	private static final void insertDescending(OrderedGroup shapes, ShapeGroup s, int left, int right) {
		if(shapes.numChildren() == 0 || s.pos >= ((ShapeGroup)shapes.getChild(left)).pos)
			shapes.insertChild(s, left);
		else if(s.pos <= ((ShapeGroup)shapes.getChild(right)).pos)
			shapes.insertChild(s, right+1);
		else {
			int piv = (left + right) / 2;
			float pivpos = ((ShapeGroup)shapes.getChild(piv)).pos;
			if(pivpos > s.pos)
				insertDescending(shapes, s, piv+1, right);
			else if(pivpos < s.pos)
				insertDescending(shapes, s, left, piv-1);
			else if(pivpos == s.pos)
				shapes.insertChild(s, piv);
		}
	}

	private static final void insertAscending(OrderedGroup shapes, ShapeGroup s, int left, int right) {
		if(shapes.numChildren() == 0 || s.pos <= ((ShapeGroup)shapes.getChild(left)).pos)
			shapes.insertChild(s, left);
		else if(s.pos >= ((ShapeGroup)shapes.getChild(right)).pos)
			shapes.insertChild(s, right+1);
		else {
			int piv = (left + right) / 2;
			float pivpos = ((ShapeGroup)shapes.getChild(piv)).pos;
			if(pivpos < s.pos)
				insertAscending(shapes, s, piv+1, right);
			else if(pivpos > s.pos)
				insertAscending(shapes, s, left, piv-1);
			else if(pivpos == s.pos)
				shapes.insertChild(s, piv);
		}
	}
}

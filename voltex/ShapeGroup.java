package voltex;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;

public class ShapeGroup extends BranchGroup {

	final float pos;

	public ShapeGroup(Shape3D shape, float pos, String name) {
		super();
		this.setCapability(ALLOW_DETACH);
		this.addChild(shape);
		this.setName(name);
		this.pos = pos;
	}
}

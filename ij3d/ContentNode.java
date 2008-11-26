package ij3d;

import javax.media.j3d.*;
import javax.vecmath.Point3f;

public abstract class ContentNode extends BranchGroup {

	protected Point3f min, max, center;

	public ContentNode() {
		super();
		setCapability(BranchGroup.ALLOW_DETACH);
	}

	public abstract void channelsUpdated();

	public abstract void thresholdUpdated();

	public abstract void colorUpdated();

	public abstract void transparencyUpdated();

	public abstract void shadeUpdated();

	public abstract void eyePtChanged(View view);

	public abstract void calculateMinMaxCenterPoint();

	public abstract float getVolume();
}


package ij3d.behaviors;

import javax.media.j3d.Transform3D;

public interface BehaviorCallback {

	public static final int ROTATE = 0;
	public static final int TRANSLATE = 0;

	public void transformChanged(int type, Transform3D t);

}

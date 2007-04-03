package vis3d;

import ij.ImagePlus;
import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;

public class Content extends BranchGroup {

	String name;
	Color3f color;
	
	protected TransformGroup pickTr;

	public Content(String name, Color3f color) {
		super();
		// create BranchGroup for this image
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);

		this.name = name;

		// create transformation for pickeing
		pickTr = new TransformGroup();
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		pickTr.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		addChild(pickTr);
	}

	public void eyePtChanged(View view) {
	}
}



package vis3d;

import ij.ImagePlus;
import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;

public class Content extends BranchGroup {

	String name;
	Color3f color;
	
	protected TransformGroup pickTr;

	public Content() {
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

	public Content(String name, Color3f color) {
		this();
		this.name = name;
		this.color = color;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setColor(Color3f color) {
		this.color = color;
	}

	public void eyePtChanged(View view) {
	}

	protected static Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				return colors[i];
			}
		}
		return null;
	}


	protected static String[] colorNames = new String[]{"None", "Black", 
				"White", "Red", "Green", "Blue", "Cyan", 
				"Magenta", "Yellow"};

	protected static Color3f[] colors = {
				null,
				new Color3f(0,    0,    0),
				new Color3f(1.0f, 1.0f, 1.0f),
				new Color3f(1.0f, 0,    0),
				new Color3f(0,    1.0f, 0),
				new Color3f(0,    0,    1.0f),
				new Color3f(0,    1.0f, 1.0f),
				new Color3f(1.0f, 0,    1.0f),
				new Color3f(1.0f, 1.0f, 0)};
}



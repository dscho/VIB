package vis3d;

import ij.ImagePlus;
import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;

public abstract class Content extends BranchGroup {

	String name;
	Color3f color;
	protected boolean selected;
	
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

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void setColor(String color) {
		if(color.equals(getColorName(this.color)))
			return;
		this.color = getColor(color);
		colorUpdated(this.color);
	}

	protected static Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				return colors[i];
			}
		}
		return null;
	}

	public abstract void eyePtChanged(View view);
	public abstract void colorUpdated(Color3f color);


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
	protected static String getColorName(Color3f col) {
		for(int i = 1; i < colors.length; i++) {
			if(colors[i].equals(col))
				return colorNames[i];
		}
		return "None";
	}	
}



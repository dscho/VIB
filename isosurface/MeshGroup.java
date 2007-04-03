package isosurface;

import java.util.List;

import vis3d.Content;
import javax.vecmath.Color3f;

public class MeshGroup extends Content {

	public MeshGroup(String name, Color3f color, List mesh) {
		super(name, color);
		// create the IsoShape for this image and add it
		IsoShape shape = new IsoShape(mesh, color, name);
		pickTr.addChild(shape);

		compile();
	}
}


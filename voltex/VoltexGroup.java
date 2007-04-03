package voltex;

import java.util.List;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import com.sun.j3d.utils.behaviors.picking.PickingCallback;
import ij.ImagePlus;

import vis3d.Content;

public class VoltexGroup extends Content {

	private Renderer renderer;

	public VoltexGroup(String name, Color3f color, ImagePlus image) {
		super(name, color);
		
		float scale = image.getWidth() * 
					(float)image.getCalibration().pixelWidth;

		renderer = new Axis2DRenderer(image, color);
		renderer.update();

		Point3d maxCoord = renderer.volume.maxCoord;
		Point3d minCoord = renderer.volume.minCoord;
		
		Transform3D translate = new Transform3D();
		translate.setTranslation(new Vector3d(
					-(maxCoord.x-minCoord.x)/2, 
					-(maxCoord.y-minCoord.y)/2, 
					-(maxCoord.z-minCoord.z)/2));
		TransformGroup tg = new TransformGroup(translate);
		
		pickTr.addChild(tg);
		tg.addChild(renderer.getVolumeNode());

		compile();
	}

	public void update() {	
		Volume volume = renderer.volume;
		volume.update();
		//updateCenter(volume.minCoord, volume.maxCoord);
		renderer.update();
	}
		
	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}
}


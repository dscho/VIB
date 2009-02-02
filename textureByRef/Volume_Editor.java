package textureByRef;

import javax.media.j3d.View;
import javax.vecmath.Tuple3d;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.ContentNode;
import ij3d.Image3DUniverse;

public class Volume_Editor implements PlugInFilter {
	
	private ImagePlus image;
	private EditableVolumeRenderer renderer;
	private EditableVolume volume;
	private ContentNode customNode;
	
	public static void main(String[] args) {
		new ij.ImageJ();
		ImagePlus imp = NewImage.createByteImage(
				"Edit volume", 250, 233, 57, NewImage.FILL_BLACK);
		imp.show();
		Volume_Editor vol = (Volume_Editor)IJ.
				runPlugIn("textureByRef.Volume_Editor", "");
		EditableVolume volume = vol.getVolume();
		drawSpiral(volume, 128, 128);
	}
	
	public static void drawSpiral(EditableVolume v, float cx, float cy) {
		final int circles = 3;
		final int dzPerCircle = 19;
		final int drPerCircle = 40;
		final int stepsPerCircle = 720;
		
		float r = 1;
		float x = cx + 1;
		float y = cy;
		float z = 0;
		float a = 0;
		
		final float daPerStep = 2 * (float)Math.PI / stepsPerCircle;
		final int steps = circles * stepsPerCircle;
		final float dzPerStep = (float)dzPerCircle / stepsPerCircle;
		final float drPerStep = (float)drPerCircle / stepsPerCircle;

		for(int s = 0; s < steps; s++) {
			v.set(Math.round(x), Math.round(y), Math.round(z), 255);
			a += daPerStep;
			r += drPerStep;
			x = (float)(cx + r * Math.cos(a));
			y = (float)(cy + r * Math.sin(a));
			z += dzPerStep;
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public EditableVolume getVolume() {
		return volume;
	}

	public void run(ImageProcessor arg0) {
		renderer = new EditableVolumeRenderer(image);
		renderer.fullReload();
		volume = renderer.getVolume();
		customNode = new MyNode(volume);
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show();
		Content c = new Content("blubber");
		c.display(customNode);
		univ.addContent(c);
	}

	public int setup(String arg0, ImagePlus arg1) {
		this.image = arg1;
		return DOES_8G;
	}
	
	private class MyNode extends ContentNode {
		
		private EditableVolume volume;
		
		public MyNode(EditableVolume volume) {
			this.volume = volume;
			addChild(renderer.getVolumeNode());
		}

		@Override
		public void channelsUpdated() {
			// do nothing
		}

		@Override
		public void colorUpdated() {
			// do nothing
		}

		@Override
		public void eyePtChanged(View view) {
			renderer.eyePtChanged(view);
		}

		@Override
		public void getCenter(Tuple3d center) {
			center.set(volume.volRefPt);
		}

		@Override
		public void getMax(Tuple3d max) {
			max.set(volume.maxCoord);
		}

		@Override
		public void getMin(Tuple3d min) {
			min.set(volume.minCoord);
		}

		@Override
		public float getVolume() {
			return 0;
		}

		@Override
		public void shadeUpdated() {
			// do nothing
		}

		@Override
		public void thresholdUpdated() {
			// do nothing
		}

		@Override
		public void transparencyUpdated() {
			// do nothing
		}
	}
}

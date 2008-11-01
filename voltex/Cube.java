package voltex;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Color3f;

import ij.ImagePlus;
import ij.IJ;

public class Cube {

	private Cube[] children;

	private int x, y, z;
	private String dir;

	private BranchGroup bg;
	private VolumeRenderer renderer;

	private String path;

	private int level;

	public Cube(String dir, int x, int y, int z, int l) {
		this.dir = dir;
		this.x = x;
		this.y = y;
		this.z = z;
		this.level = l;
		this.path = dir + "/" + x + "_" + y + "_" + z + "_" + l + ".tif";
		bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		bg.setCapability(BranchGroup.ALLOW_DETACH);
	}

	public BranchGroup getNode() {
		return bg;
	}

	public boolean exists() {
		return new File(path).exists();
	}

	public boolean resolutionSufficient() {
		return false;
	}

	public void display() {
		if(resolutionSufficient() || children == null || children.length == 0)
			displaySelf();
		else
			displayChildren();
	}

	private void unDisplayChildren() {
		if(children == null)
			return;
		for(Cube c : children) {
			c.bg.removeAllChildren();
			c.renderer = null;
			c.unDisplayChildren();
		}
	}

	private void displaySelf() {
		if(renderer != null)
			return;
System.out.println("loading path = " + path);
		ImagePlus imp = IJ.openImage(path);
		renderer = new VolumeRenderer(imp,
			new Color3f(1, 1, 1), 0f, new boolean[]{true, true, true});
		renderer.fullReload();
		bg.removeAllChildren();
		unDisplayChildren();
		renderer.getVolumeNode().compile();
		bg.addChild(renderer.getVolumeNode());
	}

	private void displayChildren() {
		if(renderer != null)
			bg.removeChild(renderer.getVolumeNode());
		for(Cube c : children) {
			c.display();
			bg.addChild(c.bg);
		}
	}

	public void createChildren() {
		if(level == 1)
			return;
		List<Cube> ch = new ArrayList<Cube>(8);
		int l = level >> 1;
		Cube c;
		int s = VolumeOctree.SIZE;
		c = new Cube(dir, x,     y,     z,     l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x+l*s, y,     z,     l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x,     y+l*s, z,     l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x+l*s, y+l*s, z,     l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x,     y,     z+l*s, l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x+l*s, y,     z+l*s, l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x,     y+l*s, z+l*s, l); if(c.exists()) ch.add(c);
		c = new Cube(dir, x+l*s, y+l*s, z+l*s, l); if(c.exists()) ch.add(c);
		children = new Cube[ch.size()];
		ch.toArray(children);
		// children should create their children too
		for(Cube cube : children)
			cube.createChildren();
	}
}


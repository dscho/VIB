package customnode;

import javax.media.j3d.View;
import javax.vecmath.Point3f;

import ij3d.Content;
import ij3d.ContentNode;

public class CustomMeshNode extends ContentNode {
	
	private CustomMesh mesh;
	private Content content;
	
	public CustomMeshNode(CustomMesh mesh, Content content) {
		this.mesh = mesh;
		this.content = content;
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}
	
	public CustomMesh getMesh() {
		return mesh;
	}

	@Override
	public void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		mesh.calculateMinMaxCenterPoint(min, max, center);
	}

	@Override
	public void channelsUpdated() {
		// do nothing
	}

	@Override
	public void colorUpdated() {
		mesh.setColor(content.getColor());
	}

	@Override
	public void eyePtChanged(View view) {
		// do nothing
	}

	@Override
	public float getVolume() {
		return mesh.getVolume();
	}

	@Override
	public void shadeUpdated() {
		mesh.setShaded(content.isShaded());
	}

	@Override
	public void thresholdUpdated() {
		// do nothing
	}

	@Override
	public void transparencyUpdated() {
		mesh.setTransparency(content.getTransparency());
	}
}

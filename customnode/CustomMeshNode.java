package customnode;

import javax.media.j3d.View;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;

import ij3d.Content;
import ij3d.ContentNode;

public class CustomMeshNode extends ContentNode {

	private CustomMesh mesh;
	private Content content;
	private Point3f min, max, center;

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
	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	@Override
	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	@Override
	public void getCenter(Tuple3d center) {
		center.set(this.center);
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

	private void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		mesh.calculateMinMaxCenterPoint(min, max, center);
	}
}

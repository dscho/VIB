package octree;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import javax.media.j3d.TexCoordGeneration;
import javax.vecmath.Vector4f;
import ij3d.AxisConstants;

public class CubeData implements AxisConstants {

	private static final int SIZE = VolumeOctree.SIZE;
	private static final int B_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	final float[] cal = new float[3];
	final float[] min = new float[3];
	final float[] max = new float[3];

	private final TexCoordGeneration tgx, tgy, tgz;

	int axis;

	BufferedImage[] images;
	byte[][] pixels;
	TexCoordGeneration tg;
	ShapeGroup[] shapes;
	Cube cube;
	boolean empty;

	public CubeData(Cube c) {
		this.cube = c;
		readCalibration(c.dir + c.name + ".info", cal);

		min[0] = (float)(c.x * c.octree.pw);
		min[1] = (float)(c.y * c.octree.ph);
		min[2] = (float)(c.z * c.octree.pd);

		max[0] = min[0] + SIZE * cal[0];
		max[1] = min[1] + SIZE * cal[1];
		max[2] = min[2] + SIZE * cal[2];

		float xTexGenScale = (float)(1.0 / (cal[0] * SIZE));
		float yTexGenScale = (float)(1.0 / (cal[1] * SIZE));
		float zTexGenScale = (float)(1.0 / (cal[2] * SIZE));

		tgz = new TexCoordGeneration();
		tgz.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * min[0])));
		tgz.setPlaneT(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * min[1])));

		tgx = new TexCoordGeneration();
		tgx.setPlaneS(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * min[1])));
		tgx.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * min[2])));

		tgy = new TexCoordGeneration();
		tgy.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * min[0])));
		tgy.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * min[2])));

		images = new BufferedImage[SIZE];
		pixels = new byte[SIZE][];
		shapes = new ShapeGroup[SIZE];
		for(int i = 0; i < SIZE; i++)
			shapes[i] = new ShapeGroup();

		empty = true;
	}

	public void prepareForAxis(int axis) {
		this.axis = axis;
		for(int i = 0; i < SIZE; i++)
			shapes[i].prepareForAxis(min[axis] + cal[axis] * i);
	}

	public void show() {
		try {
			createData();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		for(int i = 0; i < SIZE; i++)
			shapes[i].show(this, i);
	}

	public void hide() {
		for(int i = 0; i < SIZE; i++)
			shapes[i].hide();
		releaseData();
	}

	private void createData() throws Exception {
		for(int i = 0; i < SIZE; i++) {
			images[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);
			pixels[i] = ((DataBufferByte) images[i].getRaster().getDataBuffer()).getData();
		}
		switch(axis) {
			case X_AXIS: createXData(); break;
			case Y_AXIS: createYData(); break;
			case Z_AXIS: createZData(); break;
		}
		empty = false;
	}

	private void releaseData() {
		tg = null;
		for(int i = 0; i < SIZE; i++) {
			pixels[i] = null;
			images[i] = null;
		}
		empty = true;
	}

	private final void createZData() throws Exception {
		loadData(cube.dir + "/z/" + cube.name);
		tg = tgz;
	}

	private final void createXData() throws Exception {
		loadData(cube.dir + "/x/" + cube.name);
		tg = tgx;
	}

	private final void createYData() throws Exception {
		loadData(cube.dir + "/y/" + cube.name);
		tg = tgy;
	}

	public static final float[] readCalibration(String path, float[] ret) {
		if(ret == null)
			ret = new float[3];
		File f = new File(path);
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(f));
			if(in == null)
				return null;
			ret[0] = in.readFloat();
			ret[1] = in.readFloat();
			ret[2] = in.readFloat();
			in.close();
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return ret;
	}

	private void loadData(String path) throws Exception {
		File f = new File(path);
		DataInputStream in = new DataInputStream(
			new FileInputStream(f));
		for (int z = 0; z < SIZE; z++) {
			byte[] pix= pixels[z];
			int nPixels = pix.length;
			int read = 0;
			while (read < nPixels) {
				read += in.read(pix, read, nPixels - read);
			}
		}
		in.close();
	}
}


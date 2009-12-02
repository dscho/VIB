package octree;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import javax.media.j3d.NioImageBuffer;
import javax.media.j3d.TexCoordGeneration;
import javax.vecmath.Vector4f;
import ij3d.AxisConstants;

public class CubeData implements AxisConstants {

	private static final int SIZE = VolumeOctree.SIZE;

	final float[] cal = new float[3];
	final float[] min = new float[3];
	final float[] max = new float[3];

	NioImageBuffer[] zBuffer = new NioImageBuffer[SIZE];
	NioImageBuffer[] yBuffer = new NioImageBuffer[SIZE];
	NioImageBuffer[] xBuffer = new NioImageBuffer[SIZE];
	NioImageBuffer[] buffer;

	private final TexCoordGeneration tgx, tgy, tgz;

	int axis;

	TexCoordGeneration tg;
	ShapeGroup[] shapes;
	Cube cube;

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

		shapes = new ShapeGroup[SIZE];
		for(int i = 0; i < SIZE; i++)
			shapes[i] = new ShapeGroup();

		try {
			createData();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void prepareForAxis(int axis) {
		this.axis = axis;
		for(int i = 0; i < SIZE; i++)
			shapes[i].prepareForAxis(min[axis] + cal[axis] * i);
		switch(axis) {
			case X_AXIS: tg = tgx; buffer = xBuffer; break;
			case Y_AXIS: tg = tgy; buffer = yBuffer; break;
			case Z_AXIS: tg = tgz; buffer = zBuffer; break;
		}
	}

	public void show() {
		for(int i = 0; i < SIZE; i++)
			shapes[i].show(this, i);
	}

	public void hide() {
		for(int i = 0; i < SIZE; i++)
			shapes[i].hide();
	}

	private void createData() throws IOException {
		for(int i = 0; i < SIZE; i++) {
			createNioBuffers(zBuffer, cube.dir + "/z/" + cube.name);
			createNioBuffers(yBuffer, cube.dir + "/y/" + cube.name);
			createNioBuffers(xBuffer, cube.dir + "/x/" + cube.name);
		}
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

	private void createNioBuffers(NioImageBuffer[] buffer, String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
		FileChannel fc = fis.getChannel();
		int sz = (int)fc.size();
		MappedByteBuffer whole = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
		int s2 = SIZE * SIZE;
		NioImageBuffer.ImageType type = NioImageBuffer.ImageType.TYPE_BYTE_GRAY;
		for(int i = 0; i < SIZE; i++) {
			whole.position(i * s2);
			ByteBuffer bi = whole.slice();
			bi.limit(s2);
			buffer[i] = new NioImageBuffer(SIZE, SIZE, type, bi);
		}
	}
}


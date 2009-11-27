package octree;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.media.j3d.TexCoordGeneration;
import javax.vecmath.Vector4f;
import ij3d.AxisConstants;

public class CubeData implements AxisConstants {

	private static final int SIZE = VolumeOctree.SIZE;
	private static final int B_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	final String path;
	final float[] cal = new float[3];
	final float[] min = new float[3];
	final float[] max = new float[3];

	int axis;

	BufferedImage[] images;
	byte[][] pixels;
	TexCoordGeneration tg;
	ShapeGroup[] shapes;
	Cube cube;
	boolean empty;

	public CubeData(Cube c) {
		this.cube = c;
		this.path = c.path;
		min[0] = (float)c.corners[0].x;
		min[1] = (float)c.corners[0].y;
		min[2] = (float)c.corners[0].z;
		cal[0] = c.pw;
		cal[1] = c.ph;
		cal[2] = c.pd;

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
		loadZData();
		float xTexGenScale = (float)(1.0 / (cal[0] * SIZE));
		float yTexGenScale = (float)(1.0 / (cal[1] * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * min[0])));
		tg.setPlaneT(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * min[1])));
	}

	private final void createXData() throws Exception {
		loadZData();
		byte[][] tmp = new byte[SIZE][pixels[0].length];
		for(int z = 0, offsDst = 0; z < SIZE; z++) {
			for(int y = 0, offsSrc = 0; y < SIZE; y++, offsDst++) {
				for(int x = 0; x < SIZE; x++, offsSrc++) {
					tmp[x][offsDst] = pixels[z][offsSrc];
				}
			}
		}

		for(int i = 0; i < SIZE; i++)
			System.arraycopy(tmp[i], 0, pixels[i], 0, tmp[i].length);
		float yTexGenScale = (float)(1.0 / (cal[1] * SIZE));
		float zTexGenScale = (float)(1.0 / (cal[2] * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * min[1])));
		tg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * min[2])));
	}

	private final void createYData() throws Exception {
		loadZData();
		byte[][] tmp = new byte[SIZE][pixels[0].length];
		for(int y = 0, offsSrc = 0; y < SIZE; y++, offsSrc += SIZE) {
			for (int z = 0, offsDst = 0; z < SIZE; z++, offsDst += SIZE){
				System.arraycopy(pixels[z], offsSrc, tmp[y], offsDst, SIZE);
			}
		}
		for(int i = 0; i < SIZE; i++)
			System.arraycopy(tmp[i], 0, pixels[i], 0, tmp[i].length);
		float xTexGenScale = (float)(1.0 / (cal[0] * SIZE));
		float zTexGenScale = (float)(1.0 / (cal[2] * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * min[0])));
		tg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * min[2])));
	}

	public static final float[] readCalibration(String path, float[] ret) {
		if(ret == null)
			ret = new float[3];
		File f = new File(path);
		FileInputStream in = null;
		try {
			in = new FileInputStream(f);
			if(in == null)
				return null;
			ret[0] = readFloat(in);
			ret[1] = readFloat(in);
			ret[2] = readFloat(in);
			in.close();
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return ret;
	}

	public static final void writeZData(String path, byte[][] data, float pw, float ph, float pd) throws Exception {
		File f = new File(path);
		FileOutputStream out = new FileOutputStream(f, false);
		writeFloat(out, pw);
		writeFloat(out, ph);
		writeFloat(out, pd);
		for(int i = 0; i < data.length; i++) {
			out.write(data[i]);
		}
		out.flush();
		out.close();
	}

	private void loadZData() throws Exception {
		File f = new File(path);
		FileInputStream in = new FileInputStream(f);
		cal[0] = readFloat(in);
		cal[1] = readFloat(in);
		cal[2] = readFloat(in);
		for (int z = 0; z < SIZE; z++) {
			byte[] pix= pixels[z];
			int nPixels = pix.length;
			int read = 0;
			while (read < nPixels) {
				read += in.read(pix, read, nPixels - read);
			}
		}
		in.close();
		max[0] = min[0] + SIZE * cal[0];
		max[1] = min[1] + SIZE * cal[1];
		max[2] = min[2] + SIZE * cal[2];
	}

	private static final float readFloat(FileInputStream in) throws Exception {
		return Float.intBitsToFloat(readInt(in));
	}

	private static final int readInt(FileInputStream in) throws Exception {
		return (in.read()<<24) + (in.read()<<16) + (in.read()<<8) + in.read();
	}

	private static final void writeFloat(FileOutputStream out, float f) throws Exception{
		int i = Float.floatToIntBits(f);
		writeInt(out, i);
	}

	private static final void writeInt(FileOutputStream out, int i) throws Exception {
		out.write((i & 0xff000000) >> 24);
		out.write((i & 0xff0000) >> 16);
		out.write((i & 0xff00) >> 8);
		out.write((i & 0xff));
	}
}

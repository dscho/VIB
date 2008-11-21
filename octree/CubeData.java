package octree;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.media.j3d.TexCoordGeneration;
import javax.vecmath.Vector4f;
import voltex.VolRendConstants;

public class CubeData {

	private static final int SIZE = VolumeOctree.SIZE;
	private static final int B_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	String path;
	float pw, ph, pd;
	float minX, minY, minZ;
	float maxX, maxY, maxZ;
	int axis;

	BufferedImage[] images;
	byte[][] pixels;
	TexCoordGeneration tg;
	ShapeGroup[] shapes;

	public CubeData(String path, float ox, float oy, float oz) {
		this();
		set(path, ox, oy, oz);
	}

	public CubeData() {
		images = new BufferedImage[SIZE];
		pixels = new byte[SIZE][];
		for(int i = 0; i < SIZE; i++) {
			images[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);
			pixels[i] = ((DataBufferByte) images[i].getRaster().getDataBuffer()).getData();
		}
	}

	public void set(String path, float ox, float oy, float oz) {
		this.path = path;
		minX = ox;
		minY = oy;
		minZ = oz;
	}

	private void createShapes() {
		if(shapes == null) {
			shapes = new ShapeGroup[SIZE];
			for(int i = 0; i < SIZE; i++)
				shapes[i] = new ShapeGroup(this, i);
		} else {
			for(int i = 0; i < SIZE; i++)
				shapes[i].setCubeData(this, i);
		}
	}

	void createZData() throws Exception {
		loadZData();
		float xTexGenScale = (float)(1.0 / (pw * SIZE));
		float yTexGenScale = (float)(1.0 / (ph * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * minX)));
		tg.setPlaneT(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * minY)));
		axis = VolRendConstants.Z_AXIS;
		createShapes();
	}

	void createXData() throws Exception {
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
		float yTexGenScale = (float)(1.0 / (ph * SIZE));
		float zTexGenScale = (float)(1.0 / (pd * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * minY)));
		tg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * minZ)));
		axis = VolRendConstants.X_AXIS;
		createShapes();
	}

	void createYData() throws Exception {
		loadZData();
		byte[][] tmp = new byte[SIZE][pixels[0].length];
		for(int y = 0, offsSrc = 0; y < SIZE; y++, offsSrc += SIZE) {
			for (int z = 0, offsDst = 0; z < SIZE; z++, offsDst += SIZE){
				System.arraycopy(pixels[z], offsSrc, tmp[y], offsDst, SIZE);
			}
		}
		for(int i = 0; i < SIZE; i++)
			System.arraycopy(tmp[i], 0, pixels[i], 0, tmp[i].length);
		float xTexGenScale = (float)(1.0 / (pw * SIZE));
		float zTexGenScale = (float)(1.0 / (pd * SIZE));
		tg = new TexCoordGeneration();
		tg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * minX)));
		tg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * minZ)));
		axis = VolRendConstants.Y_AXIS;
		createShapes();
	}

	private void loadZData() throws Exception {
		File f = new File(path);
		FileInputStream in = new FileInputStream(f);
		pw = readFloat(in);
		ph = readFloat(in);
		pd = readFloat(in);
		for (int z = 0; z < SIZE; z++) {
			byte[] pix= pixels[z];
			int nPixels = pix.length;
			int read = 0;
			while (read < nPixels) {
				read += in.read(pix, read, nPixels - read);
			}
		}
		in.close();
		maxX = minX + SIZE * pw;
		maxY = minY + SIZE * ph;
		maxZ = minZ + SIZE * pd;
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

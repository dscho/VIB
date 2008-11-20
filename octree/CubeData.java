package octree;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.media.j3d.TexCoordGeneration;
import javax.vecmath.Vector4f;

public class CubeData {

	private static final int SIZE = VolumeOctree.SIZE;
	private static final int B_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	String path;
	float pw, ph, pd;
	float minX, minY, minZ;
	float maxX, maxY, maxZ;

	BufferedImage[] xImages;
	BufferedImage[] yImages;
	BufferedImage[] zImages;

	byte[][] xPixels;
	byte[][] yPixels;
	byte[][] zPixels;

	TexCoordGeneration xTg;
	TexCoordGeneration yTg;
	TexCoordGeneration zTg;

	public CubeData(String path, float ox, float oy, float oz) {
		this();
		set(path, ox, oy, oz);
	}

	public CubeData() {
		xImages = new BufferedImage[SIZE];
		yImages = new BufferedImage[SIZE];
		zImages = new BufferedImage[SIZE];

		xPixels = new byte[SIZE][];
		yPixels = new byte[SIZE][];
		zPixels = new byte[SIZE][];

		for(int i = 0; i < SIZE; i++) {
			xImages[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);
			yImages[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);
			zImages[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);

			xPixels[i] = ((DataBufferByte) xImages[i].getRaster().getDataBuffer()).getData();
			yPixels[i] = ((DataBufferByte) yImages[i].getRaster().getDataBuffer()).getData();
			zPixels[i] = ((DataBufferByte) zImages[i].getRaster().getDataBuffer()).getData();
		}
	}

	public void set(String path, float ox, float oy, float oz) {
		this.path = path;
		minX = ox;
		minY = oy;
		minZ = oz;
	}

	private void update() {
		maxX = minX + SIZE * pw;
		maxY = minY + SIZE * ph;
		maxZ = minZ + SIZE * pd;

		float xTexGenScale = (float)(1.0 / (pw * SIZE));
		float yTexGenScale = (float)(1.0 / (ph * SIZE));
		float zTexGenScale = (float)(1.0 / (pd * SIZE));

		zTg = new TexCoordGeneration();
		zTg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * minX)));
		zTg.setPlaneT(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * minY)));
		yTg = new TexCoordGeneration();
		yTg.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(float)(xTexGenScale * minX)));
		yTg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * minZ)));
		xTg = new TexCoordGeneration();
		xTg.setPlaneS(new Vector4f(0f, yTexGenScale, 0f, -(float)(yTexGenScale * minY)));
		xTg.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(float)(zTexGenScale * minZ)));
	}

	void createXData() {
		for(int x = 0; x < SIZE; x++) {
			byte[] dst = xPixels[x];
			for (int z = 0; z < SIZE; z++){
				byte[] src = zPixels[z];
				int offsDst = z * SIZE;
				for (int y = 0; y < SIZE; y++){
					int offsSrc = y * SIZE + x;
					dst[offsDst + y] = src[offsSrc];
				}
			}
		}
	}

	void createYData() {
		for(int y = 0; y < SIZE; y++) {
			byte[] dst = yPixels[y];
			for (int z = 0; z < SIZE; z++){
				byte[] src = zPixels[z];
				int offsSrc = y * SIZE;
				int offsDst = z * SIZE;
				System.arraycopy(src, offsSrc, dst, offsDst, SIZE);
			}
		}
	}

	void loadZData() throws Exception {
		File f = new File(path);
		FileInputStream in = new FileInputStream(f);
		pw = readFloat(in);
		ph = readFloat(in);
		pd = readFloat(in);
		for (int i = 0; i < SIZE; i++) {
			byte[] pixels = zPixels[i];
			int nPixels = pixels.length;
			int read = 0;
			while (read < nPixels) {
				read += in.read(pixels, read, nPixels - read);
			}
		}
		in.close();
		update();
	}

	void writeZData() throws Exception {
		writeZData(path, zPixels, pw, ph, pd);
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

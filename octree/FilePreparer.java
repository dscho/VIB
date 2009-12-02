package octree;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

import java.util.Properties;
import vib.Resample_;

public class FilePreparer {

	private static final Resample_.Averager accu = new Resample_.Averager();

	private static final class Volume {
		private MappedByteBuffer data;
		private FileChannel fc;
		private int w, h, d;
		private double pw, ph, pd;
		private int wh;

		Volume(File file, int w, int h, int d,
			double pw, double ph, double pd) throws IOException {

			FileInputStream fis = new FileInputStream(file);
			fc = fis.getChannel();
			int sz = (int)fc.size();
			this.data = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

			this.w = w; this.h = h; this.d = d;
			this.pw = pw; this.ph = ph; this.pd = pd;
			this.wh = w * h;
		}

		final void close() throws IOException {
			fc.close();
		}

		final int get(int x, int y, int z) {
			if(x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d)
				return 0;
			int i = z * wh + y * w + x;
			return (int)(0xff & data.get(i));
		}

		final void createBlock(int x, int y, int z, String dir, String file, int size) throws IOException {
			byte[] blob = new byte[size * size * size];
			int i = 0;
			for(int iz = 0; iz < size; iz++)
				for(int iy = 0; iy < size; iy++)
					for(int ix = 0; ix < size; ix++)
						blob[i++] = (byte)get(x + ix, y + iy, z + iz);

			DataOutputStream fos = new DataOutputStream(
				new FileOutputStream(dir + "/" + file + ".info"));
			fos.writeFloat((float)pw);
			fos.writeFloat((float)ph);
			fos.writeFloat((float)pd);
			fos.close();

			writeBlob(blob, dir + "/z/" + file);

			byte[] b = createYBlobFromZ(blob, size);
			writeBlob(b, dir + "/y/" + file);

			b = createXBlobFromZ(blob, size);
			writeBlob(b, dir + "/x/" + file);
		}

		final void writeBlob(byte[] blob, String file) throws IOException {
			DataOutputStream fos = new DataOutputStream(
				new FileOutputStream(file));
			fos.write(blob, 0, blob.length);
			fos.close();
		}

		final byte[] createYBlobFromZ(byte[] blob, int size) {
			byte[] ret = new byte[blob.length];
			for(int y = 0; y < size; y++) {
				for(int z = 0; z < size; z++) {
					System.arraycopy(blob,
						z * size * size + y * size,
						ret,
						y * size * size + z * size,
						size);
				}
			}
			return ret;
		}

		final byte[] createXBlobFromZ(byte[] blob, int size) {
			byte[] ret = new byte[blob.length];
			for(int z = 0; z < size; z++) {
				for(int y = 0; y < size; y++) {
					for(int x = 0; x < size; x++) {
						ret[x * size * size + z * size + y] = blob[z * size * size + y * size + x];
					}
				}
			}
			return ret;
		}

		final void downsample(String file, int fx, int fy, int fz) throws IOException {
			FileOutputStream out = new FileOutputStream(file);

			int ws = nextPow2(w), hs = nextPow2(h), ds = nextPow2(d);
			int wn = ws / fx, hn = hs / fy, dn = ds / fz;
			byte[] bytes = new byte[wn];
			for(int z = 0; z < ds; z += fz) {
				for(int y = 0; y < hs; y += fy) {
					for(int x = 0; x < ws; x += fx) {
						accu.reset();
						for(int i = 0; i < fx; i++) {
							int ix = x + i;
							for(int j = 0; j < fy; j++) {
								int iy = y + j;
								for(int k = 0; k < fz; k++) {
									int iz = z + k;
									accu.add(get(ix, iy, iz));
								}
							}
						}
						bytes[x / fx] = (byte)accu.get();
					}
					out.write(bytes, 0, wn);
				}
			}
		}
	}

	public static final void createFiles(String path, int size, String dir,
			int w, int h, int d,
			double pw, double ph, double pd) throws IOException {

		int wOrg = w, hOrg = h, dOrg = d;
		double pwOrg = pw, phOrg = ph, pdOrg = pd;
		int level = 1;

		new File(dir, "x").mkdir();
		new File(dir, "y").mkdir();
		new File(dir, "z").mkdir();

		while(true) {
			File file = new File(path);
			Volume v = new Volume(file, w, h, d, pw, ph, pd);
			for(int z = 0; z < d; z += size) {
				for(int y = 0; y < h; y += size) {
					for(int x = 0; x < w; x += size) {
						String n = (x*level) + "_" + (y*level) + "_" + (z*level) + "_" + level;
						v.createBlock(x, y, z, dir, n, size);
					}
				}
			}
			int fx = w > size ? 2 : 1;
			int fy = h > size ? 2 : 1;
			int fz = d > size ? 2 : 1;

			if(fx == 1 && fy == 1 && fz == 1)
				break;

			File downs = new File(dir, file.getName() + ".l" + level);
			v.downsample(downs.getPath(), fx, fy, fz);
			v.close();
			pw *= fx;
			ph *= fy;
			pd *= fz;
			w = nextPow2(w) / fx;
			h = nextPow2(h) / fy;
			d = nextPow2(d) / fz;

			path = downs.getPath();
			level <<= 1;
		}
		writeProperties(wOrg, hOrg, dOrg, pwOrg, phOrg, pdOrg, level, dir + "/props.txt");
	}

	private static final void writeProperties(int w, int h, int d,
			double pw, double ph, double pd,
			int l, String path) throws IOException {

		Properties props = new Properties();
		props.setProperty("width", Integer.toString(w));
		props.setProperty("height", Integer.toString(h));
		props.setProperty("depth", Integer.toString(d));
		props.setProperty("level", Integer.toString(l));
		props.setProperty("pixelWidth", Float.toString((float)pw));
		props.setProperty("pixelHeight", Float.toString((float)ph));
		props.setProperty("pixelDepth", Float.toString((float)pd));

		FileOutputStream fw = new FileOutputStream(new File(path));
		props.store(fw, "octree");
	}

	private static final int nextPow2(int n) {
		int retval = 2;
		while (retval < n) {
			retval = retval << 1;
		}
		return retval;
	}
}


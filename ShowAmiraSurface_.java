import Quick3dApplet.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.io.OpenDialog;
import java.io.DataInputStream;
import java.io.FileInputStream;
import amira.AmiraParameters;
import vib.Image3dCanvas;

public class ShowAmiraSurface_ implements PlugIn {
	public final int WIDTH = 400, HEIGHT = 400, BGCOLOR = 0x000000;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("AmiraFile", null);
		String dir=od.getDirectory();
		arg=od.getFileName();

		Image3dCanvas ic = new Image3dCanvas(arg, WIDTH, HEIGHT);
		ic.r.setViewOffset(new Vec(300, 300, -2500));
		ic.r.setLightDir(new Vec(0, 0, +1));
		ic.r.setBackgroundCol(BGCOLOR);

		try {
			getSurface(dir + arg, ic);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}

		ic.fitToWindow();
		ic.repaint();
	}

	public static void getSurface(String fileName, Image3dCanvas ic)
			throws java.io.IOException {
		FileInputStream f = new FileInputStream(fileName);
		DataInputStream input = new DataInputStream(f);

		if (!input.readLine().startsWith("# HyperSurface 0.1 BINARY"))
			throw new RuntimeException("No Amira surface");

		String header = "";
		String line;
		while ((line = input.readLine()) != null &&
				!line.startsWith("Vertices"))
			header += line + "\n";
		AmiraParameters params = new AmiraParameters(header);

		int vertexCount = Integer.parseInt(line.substring(9));
		Vertex[] vertices = new Vertex[vertexCount];
		for (int i = 0; i < vertexCount; i++) {
			float x = input.readFloat();
			float y = input.readFloat();
			float z = input.readFloat();
			vertices[i] = new Vertex(new Vec(x, y, z));
		}

		while ((line = input.readLine()) != null &&
				!line.trim().startsWith("Patches"));

		int patchCount = Integer.parseInt(line.substring(8));
		for (int p = 0; p < patchCount; p++) {
			int color = 0xff0000ff;

			RenderObject ro = new RenderObject();
			while ((line = input.readLine()) != null &&
					!line.trim().startsWith("Triangles"))
				if (line.startsWith("InnerRegion")) {
					String name = line.substring(12);
					int m = params.getMaterialID(name);
					double[] c = params.getMaterialColor(m);
					color = ((int)(255 * c[0])) << 16 |
						((int)(255 * c[1])) << 8 |
						((int)(255 * c[2]));
					if (color == 0)
						color = 0x7f7f7f;
					ic.objectTitles.put(ro, name);
				}

			int triangleCount = Integer.parseInt(
					line.trim().substring(10));
			for (int i = 0; i < triangleCount; i++) {
				int i1 = input.readInt() - 1;
				int i2 = input.readInt() - 1;
				int i3 = input.readInt() - 1;
				Vertex v1 = vertices[i1];
				Vertex v2 = vertices[i2];
				Vertex v3 = vertices[i3];
				ro.addTri(new ColTri(v1, v3, v2, color));
			}
			ro.optimise();
			ic.objects.addElement(ro);
		}
	}
}


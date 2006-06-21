import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.io.SaveDialog;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.*;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.*;
import java.awt.image.*;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

public class AmiraMeshWriter_ implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Choose Window");
		int c = AmiraParameters.addWindowList(gd, "window", false);
		if (c == 0) {
			IJ.error("No window?");
			return;
		}
		if (c > 1) {
			gd.showDialog();
			if (gd.wasCanceled())
				return;
		}

		String title = gd.getNextChoice();
		Object frame = WindowManager.getImage(title);
		if (frame == null)
			frame = WindowManager.getFrame(title);
		else {
			int type = ((ImagePlus)frame).getType();
			if (type != ImagePlus.GRAY8 &&
					type != ImagePlus.COLOR_256) {
				IJ.error("Invalid image type");
				return;
			}
		}
		if (frame == null) {
			IJ.error("No window?");
			return;
		}

        writeImage(frame);

    }

    public static void writeImage(Object frame) {
        SaveDialog od = new SaveDialog("AmiraFile", null, ".am");
        String dir=od.getDirectory();
        String name=od.getFileName();
        if(name==null)
            return;

        if (frame instanceof TextWindow) {
            TextWindow t = (TextWindow)frame;
            AmiraTableEncoder e = new AmiraTableEncoder(t);
            if (!e.write(dir + name))
                IJ.error("Could not write to " + dir + name);
            return;
        }

        AmiraMeshEncoder e=new AmiraMeshEncoder(dir+name);

        if(!e.open()) {
            IJ.error("Could not write "+dir+name);
            return;
        }

        if(!e.write((ImagePlus)frame))
            IJ.error("Error writing "+dir+name);
    }

    public static String[] getWindowList() {
		Vector v = new Vector();
		MenuBar mbar = Menus.getMenuBar();
		Menu menu = null;
		for (int i = 0; i < mbar.getMenuCount(); i++)
			if (mbar.getMenu(i).getLabel().equals("Window")) {
				menu = mbar.getMenu(i);
				break;
			}
		if (menu == null)
			throw new RuntimeException("no Window menu?");
		for (int i = 0; i < WindowManager.getWindowCount(); i++) {
			ImagePlus img = WindowManager.getImage(i + 1);
			v.add(img.getTitle());
		}
		for (int i = 0; i < menu.getItemCount(); i++) {
			String title = menu.getItem(i).getLabel();
			if (WindowManager.getFrame(title) != null)
				v.add(title);
		}

		String[] result = new String[v.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)v.get(i);
		return result;
	}
};

class AmiraMeshEncoder {
	private int width,height,numSlices;
	private int mode;
	final public int RAW = 0;
	final public int RLE = 1;
	final public int ZLIB = 2;
	private Hashtable parameters;
	private Vector materials;

	private String path;
	private RandomAccessFile file;
	private long offsetOfStreamLength;
	private String line;
	private byte[] rleOverrun;
	private int rleOverrunLength;
	private ZOutputStream zStream;
	private int zLength;

	public AmiraMeshEncoder(String path_) {
		path = path_;
		width = height = numSlices = -1;
		offsetOfStreamLength = 0;
		rleOverrunLength = 0;
		mode = ZLIB;
	}

	public boolean open() {
		try {
			file=new RandomAccessFile(path,"rw");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public boolean writeHeader(ImagePlus ip) {
		try {
			AmiraParameters parameters=new AmiraParameters(ip);
			if (parameters.isAmiraLabelfield(ip))
				mode = RLE;
			Date date=new Date();
			file.writeBytes("# AmiraMesh 3D BINARY 2.0\n"
				+"# CreationDate: "+date.toString()+"\n"
				+"\n"
				+"define Lattice "+width+" "+height+" "+numSlices+"\n"
				+"\n"
				+"Parameters {\n"
				+parameters.toString()
				+"}\n"
				+"\n"
				+"Lattice { byte "
				+ ( mode == RLE ? "Labels" : "Data")
				+ " } @1");
			if (mode == RLE) {
				file.writeBytes("(HxByteRLE,");
				offsetOfStreamLength=file.getFilePointer();
				file.writeBytes("          ");
			} else if (mode == ZLIB) {
				file.writeBytes("(HxZip,");
				offsetOfStreamLength=file.getFilePointer();
				file.writeBytes("          ");
			}
			file.writeBytes("\n"
				+"\n"
				+"# Data section follows\n"
				+"@1\n");
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return true;
	}

	// TODO: adjust Colors of Materials

	public boolean write(ImagePlus ip) {
		IJ.showStatus("Writing "+path+" (AmiraMesh) ...");

		width=ip.getWidth();
		height=ip.getHeight();
		numSlices=ip.getStackSize();

		if(!writeHeader(ip))
			return false;

		try {
			long offsetOfData=file.getFilePointer();

			ImageStack is=ip.getStack();
			for(int k=1;k<=numSlices;k++) {
				ByteProcessor ipro=(ByteProcessor)is.getProcessor(k);
				byte[] pixels=(byte[])ipro.getPixels();
				if (mode == RLE)
					writeRLE(pixels);
				else if (mode == ZLIB)
					writeZlib(pixels);
				else
					file.write(pixels);
				IJ.showProgress(k, numSlices);
			}

			if (mode == ZLIB)
				zStream.finish();

			// fix file size
			long eof=file.getFilePointer();
			file.setLength(eof);

			// fix up stream length
			if (mode == RLE || mode == ZLIB) {
				long length = eof - offsetOfData;
				file.seek(offsetOfStreamLength);
				file.writeBytes("" + length + ")\n");
				file.seek(eof);
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}

		IJ.showStatus("");

		return true;
	}

	public void writeRLE(byte[] pixels) throws IOException {
		for(int i=0;i<pixels.length;) {
			if(i+1>=pixels.length) {
				file.writeByte(1);
				file.writeByte(pixels[i]);
				i++;
			} else if(pixels[i]==pixels[i+1]) {
				int j;
				for(j=2;j<127 && j+i+1<pixels.length && pixels[i]==pixels[i+j];j++);
				file.writeByte(j);
				file.writeByte(pixels[i]);
				i+=j;
			} else {
				int j;
				for(j=1;j<127 && j+i+1<pixels.length && pixels[i+j]!=pixels[i+j+1];j++);
				file.writeByte(j|0x80);
				file.write(pixels,i,j);
				i+=j;
			}
		}
	}

	public void writeZlib(byte[] pixels) throws IOException {
		if (zStream == null)
			zStream = new ZOutputStream(new BufferedOutputStream(new FileOutputStream(file.getFD())), JZlib.Z_BEST_COMPRESSION);
		zStream.write(pixels, 0, pixels.length);
	}
}

class AmiraTableEncoder {
	AmiraParameters parameters;
	TextWindow textWindow;
	int columnCount, rowCount;
	String[] headings;
	float[][] values;
	int[] textSizes;
	String[][] text;

	public AmiraTableEncoder(TextWindow window) {
		textWindow = window;
		TextPanel panel = window.getTextPanel();
		String h = panel.getColumnHeadings();
		headings = h.split("\t");
		for (int i = 0; i < headings.length; i++)
			if (headings[i].equals("") || headings[i].equals(" "))
				headings[i] = "column" + i;

		columnCount = headings.length;
		rowCount = panel.getLineCount();
		text = new String[rowCount][columnCount];
		for (int i = 0; i < rowCount; i++)
			text[i] = panel.getLine(i).split("\t");

		if (window instanceof AmiraTable)
			parameters = new AmiraParameters(
					((AmiraTable)window).properties);
		else {
			String p = "Parameters {\n"
				+ AmiraTable.getParameterString(rowCount,
						headings)
				+ "}\n";

			parameters = new AmiraParameters(p);
		}

		// analyse type
		values = new float[columnCount][];
		textSizes = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			values[i] = new float[rowCount];
			textSizes[i] = -1;
			for (int j = 0; j < rowCount; j++) {
				try {
					values[i][j] = Float.parseFloat(
							text[j][i]);
				} catch(NumberFormatException e) {
					values[i] = null;
					textSizes[i] = 0;
					for (j = 0; j < rowCount; j++)
						textSizes[i] +=
							text[j][i].length() + 1;
				}
			}
		}
	}

	public boolean write(String name) {
		try {
			FileWriter writer = new FileWriter(name);
			Date date=new Date();
			writer.write("# AmiraMesh 3D ASCII 2.0\n"
					+ "# CreationDate: " + date + "\n\n");

			for (int i = 0; i < columnCount; i++)
				writer.write("define " + headings[i] + " "
						+ (values[i] != null ? rowCount
							: textSizes[i]) + "\n");

			writer.write("\nParameters {\n" + parameters
					+ "\n}\n\n");

			for (int i = 0; i < columnCount; i++)
				writer.write(headings[i] + " { "
						+ (values[i] != null ? "float"
							: "byte")
						+ " " + headings[i] + " } @"
						+ (i + 1) + "\n");

			writer.write("\n#Data section follows\n");

			for (int i = 0; i < columnCount; i++) {
				writer.write("@" + (i + 1) + "\n");
				if (values[i] != null)
					for (int j = 0; j < rowCount; j++)
						writer.write(text[j][i] + "\n");
				else
					writeStringColumn(writer, i);
				writer.write("\n");
			}

			writer.close();
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			IJ.error(e.toString());
		}
		return false;
	}

	void writeStringColumn(FileWriter writer, int i) throws IOException {
		for (int j = 0; j < rowCount; j++) {
			byte[] b = text[j][i].getBytes();
			for (int k = 0; k < b.length; k++)
				writer.write(Integer.toString(b[k]) + "\n");
			writer.write("0\n");
		}
	}
}


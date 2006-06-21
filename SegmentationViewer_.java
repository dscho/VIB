import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** This plugin is an example showing how to add a non-destructive 
  graphic overlay to an image or stack. */
public class SegmentationViewer_ implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();

		if(AmiraParameters.isAmiraLabelfield(imp)) {
			IJ.error("The current image is a labelfield!");
			return;
		}

		// find labelfields
		Vector labelfields=new Vector();
		ImagePlus labels;
		for(int i=1; (labels=WindowManager.getImage(i))!=null; i++) {
			// TODO: check dimensions
			if(AmiraParameters.isAmiraLabelfield(labels))
				labelfields.add(labels);
		}
		// TODO: show dialog to create a labelfield
		if(labelfields.size()<1) {
			IJ.error("No labelfields loaded");
			return;
		}

		if(labelfields.size()>1) {
			String[] list=new String[labelfields.size()];
			for(int i=0;i<list.length;i++)
				list[i]=((ImagePlus)labelfields.get(i)).getTitle();

			GenericDialog gd=new GenericDialog("Parameters");
			gd.addChoice("Labelfield",list,list[0]);
			gd.showDialog();
			if(gd.wasCanceled())
				return;

			labels=(ImagePlus)labelfields.get(gd.getNextChoiceIndex());
		} else
			labels=(ImagePlus)labelfields.get(0);

		SegViewerCanvas cc = new SegViewerCanvas(imp, labels);
		if (imp.getStackSize()>1)
			new StackWindow(imp, cc);
		else
			new ImageWindow(imp, cc);
	}

	static class SegViewerCanvas extends ImageCanvas {
		final static int OUTLINE=1, FILL=2;
		int mode=FILL;
		int alpha=128; // if mode==FILL, use this transparency to fill

		ImagePlus labels;
		int w,h;
		Color[] label_colors; // these are the up to 256 material colors

		Vector[] contours; // each element is a vector of polygons
		Vector[] colors; // these are the corresponding colors

		IdleThread idle;
		private final boolean debug = false;

		SegViewerCanvas(ImagePlus imp,ImagePlus labels) {
			super(imp);
			this.labels=labels;
			w=labels.getWidth();
			h=labels.getHeight();
			AmiraParameters parameters=new AmiraParameters(labels);
			label_colors=new Color[parameters.getMaterialCount()];
			for(int i=0;i<label_colors.length;i++) {
				double[] c=parameters.getMaterialColor(i);
				int red=(int)(255*c[0]);
				int green=(int)(255*c[1]);
				int blue=(int)(255*c[2]);
				label_colors[i]=new Color(red,green,blue);
			}
			int depth=labels.getStack().getSize();
			if(debug)
				System.err.println("depth: "+depth);
			contours=new Vector[depth];
			colors=new Vector[depth];
			if(mode==OUTLINE) {
				idle=new IdleThread(imp.getCurrentSlice());
				idle.setPriority(Thread.MIN_PRIORITY);
				idle.start();
			}
		}

		public void updateSlice(int slice){
			colors[slice-1] = null;
			contours[slice-1] = null;
			createContoursIfNotExist(slice);
		}

		/*
		 * This class implements a Cartesian polygon in progress.
		 * The edges are supposed to be of unit length, and parallel to
		 * one axis.
		 * It is implemented as a deque to be able to add points to both
		 * sides.
		 * The points should be added such that for each pair of consecutive
		 * points, the inner part is on the left.
		 */
		static class Outline {
			int[] x, y;
			int first, last, reserved;
			final int GROW = 10;

			public Outline() {
				reserved = GROW;
				x = new int[reserved];
				y = new int[reserved];
				first = last = GROW / 2;
			}

			private void needs(int newCount, int offset) {
				if (newCount > reserved || (offset > first)) {
					if (newCount < reserved + GROW + 1)
						newCount = reserved + GROW + 1;
					int[] newX = new int[newCount];
					int[] newY = new int[newCount];
					System.arraycopy(x, 0, newX, offset, last);
					System.arraycopy(y, 0, newY, offset, last);
					x = newX;
					y = newY;
					first += offset;
					last += offset;
					reserved = newCount;
				}
			}

			public Outline push(int x, int y) {
				needs(last + 1, 0);
				this.x[last] = x;
				this.y[last] = y;
				last++;
				return this;
			}

			public Outline shift(int x, int y) {
				needs(last + 1, GROW);
				first--;
				this.x[first] = x;
				this.y[first] = y;
				return this;
			}

			public Outline push(Outline o) {
				int count = o.last - o.first;
				needs(last + count, 0);
				System.arraycopy(o.x, o.first, x, last, count);
				System.arraycopy(o.y, o.first, y, last, count);
				last += count;
				return this;
			}

			public Outline shift(Outline o) {
				int count = o.last - o.first;
				needs(last + count + GROW, count + GROW);
				first -= count;
				System.arraycopy(o.x, o.first, x, first, count);
				System.arraycopy(o.y, o.first, y, first, count);
				return this;
			}

			public Polygon getPolygon() {
				// TODO: optimize out long straight lines
				int count = last - first;
				int[] x1 = new int[count];
				int[] y1 = new int[count];
				System.arraycopy(x, first, x1, 0, count);
				System.arraycopy(y, first, y1, 0, count);
				return new Polygon(x1, y1, count);
			}

			public String toString() {
				String res = "(first:" + first
					+ ",last:" + last + ",reserved:" + reserved + ":";
				if (last > x.length) System.err.println("ERROR!");
				for (int i = first; i < last && i < x.length; i++)
					res += "(" + x[i] + "," + y[i] + ")";
				return res + ")";
			}
		}

		class ContourFinder {
			int slice;
			byte[] pixels;
			GeneralPath[] paths;
			Outline[] outline;

			public ContourFinder(int slice) {
				this.slice=slice;
				pixels=(byte[])labels.getStack().getProcessor(slice+1).getPixels();
				paths = new GeneralPath[256];
			}

			// no check!
			final byte get(int x,int y) { return pixels[y * w + x]; }

			/*
			 * Construct all outlines simultaneously by traversing the rows
			 * from top to bottom.
			 *
			 * The open ends of the polygons are stored in outline[]:
			 * if the polygon ends at the left of the pixel at x in the
			 * previous row, and the pixel is not contained in the polygin,
			 * outline[2 * x] contains the partial outline;
			 * if the polygon contains the pixel, outline[2 * x + 1] holds
			 * the partial outline.
			 */
			public void initContours() {
				contours[slice]=new Vector();
				colors[slice]=new Vector();

				// actually find the outlines
				ArrayList polygons = new ArrayList();
				outline = new Outline[2 * w + 2];

				for (int y = 0; y <= h; y++)
					for (int x = 0; x < w; x++)
						handle(x, y);

				for (int i = 1; i < paths.length; i++) {
					if (paths[i] != null) {
						contours[slice].add(paths[i]);
						colors[slice].add(label_colors[i]);
					}
				}
			}

			final private Outline newOutline(int left, int right,
					int x1, int x2, int y) {
				outline[left] = outline[right] = new Outline();
				outline[left].push(x1, y);
				outline[left].push(x2, y);
				return outline[left];
			}

			final private Outline mergeOutlines(Outline left, Outline right) {
				left.push(right);
				for (int k = 0; k < outline.length; k++)
					if (outline[k] == right) {
						outline[k] = left;
						return outline[k];
					}
				throw new RuntimeException("assertion failed!");
			}

			final private Outline moveOutline(int from, int to) {
				outline[to] = outline[from];
				outline[from] = null;
				return outline[to];
			}

			private void closeOutline(byte material, Outline outline) {
				int m = material & 0xff;
				if (paths[m] == null)
					paths[m] = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
				paths[m].append(outline.getPolygon(), false);
			}

			private void handle(int x, int y) {
				byte m = (y < h ? get(x, y) : 0);
				byte mPrev = (y > 0 ? get(x, y - 1) : 0);
				byte mLeft = (x > 0 && y < h ? get(x - 1, y) : 0);
				byte mRight = (x < w - 1 && y < h ? get(x + 1, y) : 0);
				byte mPrevLeft = (x > 0 && y > 0 ? get(x - 1, y - 1) : 0);
				byte mPrevRight = (x < w - 1 && y > 0 ? get(x + 1, y - 1) : 0);

				Outline left1 = outline[2 * x];
				Outline left2 = outline[2 * x + 1];
				Outline right2 = outline[2 * x + 2];
				Outline right1 = outline[2 * x + 3];
				outline[2 * x] = outline[2 * x + 3] = null;
				outline[2 * x + 1] = outline[2 * x + 2] = null;

				if (mPrev != 0 && mPrev != m) {
					// lower edge
					// - both null: new outline
					// - left == null: shift
					// - right == null: push
					// - right == left: close
					// - right != left: push
					int l = 2 * x, r = 2 * x + 3;
					if (left2 == null && right2 == null)
						newOutline(l, r, x, x + 1, y);
					else if (left2 == null)
						outline[l] = right2.shift(x, y);
					else if (right2 == null)
						outline[r] = left2.push(x + 1, y);
					else if (left2 == right2)
						closeOutline(mPrev, left2);
					else
						mergeOutlines(left2, right2);
					left2 = right2 = null;
				}
				if (m != 0 && mPrev != m) {
					// upper edge:
					// - left and right are null: new outline
					// - left null: push
					// - right null: shift
					// - left == right: close
					// - left != right: merge
					int l = 2 * x + 1, r = 2 * x + 2;
					if (left1 != null && mLeft != m) {
						outline[2 * x] = left1;
						left1 = null;
					}
					if (right1 != null && (mRight != m || mPrevRight != m)) {
						outline[2 * x + 3] = right1;
						right1 = null;
					}
					if (left1 == null && right1 == null)
						newOutline(l, r, x + 1, x, y);
					else if (left1 == null)
						outline[l] = right1.push(x, y);
					else if(right1 == null)
						outline[r] = left1.shift(x + 1, y);
					else if (left1 == right1)
						closeOutline(m, left1);
					else
						mergeOutlines(right1, left1);
					left1 = right1 = null;
				}
				if (left1 != null)
					outline[2 * x] = left1;
				if (left2 != null)
					outline[2 * x + 1] = left2;
				if (right1 != null)
					outline[2 * x + 3] = right1;
				if (right2 != null)
					outline[2 * x + 2] = right2;
				if (m != 0 && mLeft != m) {
					// left edge
					int l = 2 * x + 1;
					if (outline[l] == null)
						outline[l] = left2;
					outline[l].push(x, y + 1);
				}
				if (mLeft != 0 && mLeft != m) {
					// right edge
					int l = 2 * x + 0;
					if (outline[l] == null)
						outline[l] = left1;
					outline[l].shift(x, y + 1);
				}
			}
		}

		public synchronized void createContoursIfNotExist(int slice) {
			if(contours[slice-1]!=null)
				return;
			ContourFinder finder=new ContourFinder(slice-1);
			finder.initContours();
		}

		class IdleThread extends Thread {
			int counter,slice;

			public IdleThread(int slice) {
				this.slice=slice;
				counter=0;
			}

			public void run() {
				if(debug)
					System.err.println("start idle thread");
				while(true) {
					if(slice-counter>0)
						createContoursIfNotExist(slice-counter);
					else if(slice+counter>contours.length) {
						if(debug)
							System.err.println("stop idle thread");
						return;
					}
					if(slice+counter<=contours.length)
						createContoursIfNotExist(slice+counter);
					counter++;
				}
			}
		}

		public void paint(Graphics g) {
			//ejp.tracer.TracerAPI.enableTracing();
			createContoursIfNotExist(imp.getCurrentSlice());
			super.paint(g);
			drawOverlay(g);
			//ejp.tracer.TracerAPI.disableTracing();
		}

		void drawOverlay(Graphics g) {
			int slice=imp.getCurrentSlice();
			double magnification=getMagnification();

			for(int i=0;i<contours[slice-1].size();i++) {
				g.setColor((Color)colors[slice-1].get(i));
				Shape poly = (Shape)contours[slice-1].get(i);

				// take offset into account (magnification very high)
				if(magnification!=1.0) {
					AffineTransform trans = (((Graphics2D)g).getDeviceConfiguration()).getDefaultTransform();
					trans.setTransform(magnification, 0,
							0, magnification,
							-srcRect.x * magnification,
							-srcRect.y * magnification);
					poly = trans.createTransformedShape(poly);
				}

				((Graphics2D)g).draw(poly);
				if(mode==FILL) {
					Color c=(Color)colors[slice-1].get(i);
					Color c1=new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
					g.setColor(c1);
					((Graphics2D)g).fill(poly);
				}
			}
		}
	}
}


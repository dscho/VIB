import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
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

		CustomCanvas cc = new CustomCanvas(imp,labels);
		if (imp.getStackSize()>1)
			new StackWindow(imp, cc);
		else
			new ImageWindow(imp, cc);
	}

} // Side_Panel class

/* TODO: add button to create surface

	surfaceGen(radius):
		Set donePixels;
		foreach slice
			foreach Polygon
				if desiredMaterial
					foreach pixel of Polygon which is not done:
						foreach pixel2 of connectedMaterial and inside radius
							TODO: approximate gradient
							mark pixel2 as done
						add pixel with gradient

	writeLandmarks to check

*/

class CustomCanvas extends ImageCanvas {
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

	CustomCanvas(ImagePlus imp,ImagePlus labels) {
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

	//TODO: getConnectedMaterials (returns Polygon[materials][no])
	//do not work via Polygons! implement painter's algorithm


	class ContourFinder {
		int slice;
		byte[] pixels;
		Set donePixels; // contains (y*w+x)*256+label

		final int xdelta[]={0,1,0,-1};
		final int ydelta[]={1,0,-1,0};
		final int directions=xdelta.length;

		public ContourFinder(int slice) {
			this.slice=slice;
			pixels=(byte[])labels.getStack().getProcessor(slice+1).getPixels();
			donePixels=new HashSet();
		}

		// no check!
		byte get(int x,int y) { return pixels[y*w+x]; }

		// tells if this is the upper left border of a contour
		boolean isUpperLeft(int x,int y) {
			byte m=get(x,y);
			return (m!=0 &&
				(y==0 || get(x,y-1)!=m) &&
				 (x==0 || get(x-1,y)!=m));
		}

		boolean done(int x,int y,byte m) {
			Integer index=new Integer(256*(w*y+x)+m);
			return (donePixels.contains(index));
		}

		void setDone(int x,int y,byte m) {
			Integer index=new Integer(256*(w*y+x)+m);
			donePixels.add(index);
		}

		boolean inside(int x,int y) {
			return x>=0 && y>=0 && x<w && y<h;
		}

/*
	dir=0: delta=(0,1),  left=(1,0),  right=(-1,0), leftright=(0,0, -1,0)
	dir=1: delta=(1,0),  left=(0,-1), right=(0,1),  leftright=(0,-1, 0,0)
	dir=2: delta=(0,-1), left=(-1,0), right=(1,0),  leftright=(-1,-1, 0,-1)
	dir=3: delta=(-1,0), left=(0,1),  right=(0,-1), leftright=(-1,0, -1,-1)
*/

		boolean isBorder(int x,int y,byte m,int direction) {
			int left=(direction+directions/4)%directions;
			int right=(direction+3*directions/4)%directions;
			int xdir=xdelta[direction],ydir=ydelta[direction];
			int xleft=x+(xdir+xdelta[left]-1)/2,yleft=y+(ydir+ydelta[left]-1)/2;
			int xright=x+(xdir+xdelta[right]-1)/2,yright=y+(ydir+ydelta[right]-1)/2;
			return inside(xleft,yleft) && get(xleft,yleft)==m
				&& (!inside(xright,yright) || get(xright,yright)!=m);
		}

		// TODO: remove
		void assert1(boolean shouldBeTrue) {
			/* if(!shouldBeTrue) {
				double a=1.0/0;
			} */
		}

		public Polygon getContour(int x,int y) {
			if(!isUpperLeft(x,y))
				return null;
			byte m=get(x,y);
			if(m==0 || done(x,y,m))
				return null;
			int direction=0;
			Polygon poly=new Polygon();

			int x0=x,y0=y;
			while(true) {
				poly.addPoint(x,y);
				if(isUpperLeft(x,y))
					setDone(x,y,m);
				int i;
				for(i=directions/2+1;
					i<directions+directions/2
					&& !isBorder(x,y,m,(direction+i)%directions);
					i++);
				assert1(i<directions+directions/2);
				direction=(direction+i)%directions;
				x+=xdelta[direction]; y+=ydelta[direction];
				if(x==x0 && y==y0)
					return poly;
			}
		}

		public void initContours() {
			contours[slice]=new Vector();
			colors[slice]=new Vector();

			// actually find the outlines
			for(int i=0;i<w;i++)
				for(int j=0;j<h;j++) {
					Polygon poly=getContour(i,j);
					if(poly!=null) {
						contours[slice].add(poly);
						colors[slice].add(label_colors[get(i,j)]);
					}
				}
		}
	}

	public synchronized void createContoursIfNotExist(int slice) {
		if(contours[slice-1]!=null)
			return;
		//System.err.println(slice);
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
		if(false && mode==FILL) {
			createContoursIfNotExist(slice);
			byte[] pixels2=(byte[])labels.getStack().getProcessor(slice).getPixels();
			PaintersAlgorithm iter=new PaintersAlgorithm(w,h,pixels2);
			iter.init(w/2,h/2);
			g.setColor(new Color(0,255,0));
			do {
				g.fillRect((int)magnification*iter.x,(int)magnification*iter.y,(int)magnification,(int)magnification);
			} while(iter.next());

/*
			byte[] pixels1=(byte[])imp.getStack().getProcessor(slice).getPixels();
			byte[] pixels2=(byte[])labels.getStack().getProcessor(slice).getPixels();
			for(int i=0;i<w;i++)
				for(int j=0;j<w;j++) {
					byte b=pixels2[i+w*j];
					if(b!=0) {
						Color c1=label_colors[b<0?b+256:b];
						int b1=(int)pixels1[i+w*j];
						b1=(b1<0?b1+256:b1);
						Color c=new Color(c1.getRed()*b1/255,c1.getGreen()*b1/255,c1.getBlue()*b1/255);
						g.setColor(c);
						g.fillRect((int)magnification*i,(int)magnification*j,(int)magnification,(int)magnification);
					}
				}
*/
		} else {
			for(int i=0;i<contours[slice-1].size();i++) {
				g.setColor((Color)colors[slice-1].get(i));
				Polygon poly=(Polygon)contours[slice-1].get(i);
				// TODO: take offset into account (magnification very high)
				if(magnification!=1.0) {
					int x[]=new int[poly.npoints];
					int y[]=new int[poly.npoints];
					for(int j=0;j<poly.npoints;j++) {
						x[j]=(int)(magnification*poly.xpoints[j]);
						y[j]=(int)(magnification*poly.ypoints[j]);
					}
					poly=new Polygon(x,y,poly.npoints);
				}
				g.drawPolygon(poly);
				if(mode==FILL) {
					Color c=(Color)colors[slice-1].get(i);
					Color c1=new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
					g.setColor(c1);
					g.fillPolygon(poly);
				}
			}
		}
	}

	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		//IJ.log("mousePressed: ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
	}

} // CustomCanvas inner class



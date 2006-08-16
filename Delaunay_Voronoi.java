import delaunay.DelaunayTriangulation;
import delaunay.Pnt;
import delaunay.Simplex;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/** Draw Delaunay triangulation or Voronoi Diagram as an overlay. */
public class Delaunay_Voronoi implements PlugIn {

	public final int DELAUNAY = 1;
	public final int VORONOI = 2;
	int mode = DELAUNAY;

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		CustomCanvas cc = new CustomCanvas(imp);
		if (imp.getStackSize()>1)
			new StackWindow(imp, cc).addKeyListener(cc);
		else
			new ImageWindow(imp, cc).addKeyListener(cc);
	}

	class CustomCanvas extends ImageCanvas implements KeyListener {
		DelaunayTriangulation delaunay;
		final double inf;

		CustomCanvas(ImagePlus imp) {
			super(imp);
			inf = imp.getWidth() + imp.getHeight();
			initDelaunay();
			addKeyListener(this);
		}

		public void paint(Graphics g) {
			super.paint(g);
			drawOverlay(g);
		}

		void drawOverlay(Graphics g) {
			if (delaunay == null)
				return;

			for (Iterator iter = delaunay.iterator();
					iter.hasNext(); ) {
				Simplex triangle = (Simplex)iter.next();

				if (mode == DELAUNAY) {
					Iterator iter2 = triangle.iterator();
					Pnt a = (Pnt)iter2.next();
					Pnt b = (Pnt)iter2.next();
					Pnt c = (Pnt)iter2.next();
					draw(g, a, b);
					draw(g, a, c);
					draw(g, b, c);
				} else {
					Iterator iter2 = delaunay
						.neighbors(triangle).iterator();
					while (iter2.hasNext())
						draw(g, triangle,
							(Simplex)iter2.next());
				}
			}
		}

		void draw(Graphics g, Pnt a, Pnt b) {
			if (mode == VORONOI || (Math.abs(a.coord(0)) < inf &&
					Math.abs(b.coord(0)) < inf))
				g.drawLine((int)a.coord(0), (int)a.coord(1),
						(int)b.coord(0), (int)b.coord(1));
		}

		void draw(Graphics g, Simplex a, Simplex b) {
			draw(g, Pnt.circumcenter((Pnt[])a.toArray(new Pnt[0])),
				Pnt.circumcenter((Pnt[])b.toArray(new Pnt[0])));
		}

		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			initDelaunay();
			repaint();
		}

		public void keyTyped(KeyEvent e) {}
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				mode = mode == DELAUNAY ? VORONOI : DELAUNAY;
				repaint();
			}
		}
		public void keyReleased(KeyEvent e) {}

		public void initDelaunay() {
			delaunay = null;

			Roi roi = imp.getRoi();
			if (roi == null || !(roi instanceof PointRoi))
				return;

			PointRoi r = (PointRoi)roi;
			Rectangle rect = r.getBounds();
			int n = r.getNCoordinates();
			int[] x = r.getXCoordinates();
			int[] y = r.getYCoordinates();

			Simplex initial = new Simplex(new Pnt[] {
					new Pnt(-inf, -inf),
					new Pnt(-inf, 5 * inf),
					new Pnt(5 * inf, -inf)});
			delaunay = new DelaunayTriangulation(initial);
			for (int i = 0; i < n; i++)
				delaunay.delaunayPlace(new Pnt(x[i] + rect.x,
							y[i] + rect.y));
		}
	}
}
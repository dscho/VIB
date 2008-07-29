/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.app.gui;

import java.io.File;
import java.awt.*;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

import vib.app.module.*;
import vib.app.*;

public class ProgressIndicator implements ModuleListener {

	private String[] modules = new String[] {
				new SplitChannels().getName(), 
				new vib.app.module.Label().getName(),
				new Resample().getName(),
				new TissueStatistics().getName(),
				new TransformImages().getName(),
				new AverageBrain().getName()};

	private String[] files;

	private int [][] done;

	private static final int NOT_DONE = 0;
	private static final int DONE = 1;
	private static final int EXCEPTION = 2;

	private static final int WIDTH = 800;
	private static final int HEIGHT = 500;

	private Garten garten;

	public ProgressIndicator(Options options) {
		done = new int[options.fileGroup.size()+1][modules.length];
		files = new String[options.fileGroup.size()];
		for(int i = 0; i < options.fileGroup.size(); i++) {
			files[i] = options.fileGroup.get(i).getName();
		}
		showDialog();
	}
	
	public static void main(String[] args) {
		Options op = new Options();
		op.loadFrom("/Users/bene/Desktop/VIB/options.config");
		new ProgressIndicator(op);
	}
	
	public void moduleFinished(Module m, int index) {
		if(index < 0)
			return;
		int modIndex = getModuleIndex(m.getName());
		if(modIndex != -1 && done[index][modIndex] != DONE) {
			done[index][modIndex] = DONE;
			garten.draw();
		}
	}

	public void exceptionOccurred(Module m, int index) {
		if(index < 0)
			return;
		int modIndex = getModuleIndex(m.getName());
		if(modIndex != -1) {
			done[index][modIndex] = EXCEPTION;
			garten.draw();
		}
	}

	private int getModuleIndex(String name) {
		for(int i = 0; i < modules.length; i++) {
			if(modules[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	public void showDialog() {
		garten = new Garten();
		garten.image.show();
		garten.draw();
	}
	
	class Garten  {

		Font f = new Font("Verdana", Font.BOLD, 12);
		int colW = calculateColWidth();
		int rowH = calculateRowHeight();
		int cols = modules.length;
		int rows = files.length;
		final int xIndent = 10;
		final int yIndent = 10;
		final int strIndent = 5;
		ImagePlus image;

		Garten() {
			ImageProcessor ip = new ColorProcessor(
				colW * (cols+1) + 2 * xIndent,
				rowH * (rows+1) + 2 * yIndent);
			ip.setBackgroundValue((double)0);
			image = new ImagePlus("Progress", ip);
		}

		public void draw() {
			// FIXME: MHL - this throws an exception on some platforms...
			Graphics g = image.getProcessor().
					createImage().getGraphics();
			g.setFont(f);
			// draw horizontal lines
			for(int i = 0; i < rows; i++) {
				g.drawLine(xIndent, 
					yIndent + strIndent + (i+1) * rowH,
					(cols+1) * colW,
					yIndent + strIndent + (i+1) * rowH);
			}

			// draw vertical lines
			for(int i = 0; i < cols; i++) {
				g.drawLine(
					xIndent + ((i+1)*colW),
					yIndent,
					xIndent + ((i+1)*colW),
					yIndent + strIndent + (rows+1) * rowH);
			}

			// draw column names
			for(int i = 0; i < modules.length; i++) {
				g.drawString(modules[i], 
						(i+1)*colW+xIndent+strIndent, 
						rowH + yIndent);
			}

			// draw file names
			for(int i = 0; i < files.length; i++) {
				g.drawString(files[i],
						xIndent + strIndent,
						yIndent + (i+2)*rowH);
			}

			// draw spots
			for(int i = 0; i < files.length; i++) {
				for(int j = 0; j < modules.length; j++) {
					switch (done[i][j]) {
						case DONE: drawOK(i, j, g);
								break;
						case NOT_DONE: drawNY(i, j, g);
								break;
						case EXCEPTION: drawEX(i, j, g);
								break;
					}
				}
			}
			image.updateAndDraw();
		}

		public void drawNY(int file, int mod, Graphics g) {
			int xOffs = xIndent + (mod+1) * colW;
			int yOffs = strIndent + yIndent + (file+1) * rowH;
			int r = (rowH-5)/2;
			int cx = xOffs + colW / 2;
			int cy = yOffs + rowH / 2;
			g.setColor(Color.GRAY);
			g.fillOval(cx - r, cy - r, 2*r, 2*r);
			g.setColor(Color.BLACK);
			g.drawOval(cx - r, cy - r, 2*r, 2*r);
		}
		
		public void drawOK(int file, int mod, Graphics g) {
			int xOffs = xIndent + (mod+1) * colW;
			int yOffs = strIndent + yIndent + (file+1) * rowH;
			int r = (rowH-5)/2;
			int cx = xOffs + colW / 2;
			int cy = yOffs + rowH / 2;
			g.setColor(Color.GREEN);
			g.fillOval(cx - r, cy - r, 2*r, 2*r);
			g.setColor(Color.BLACK);
			int cross = (int) (r * SIN45);
			g.drawLine(cx-cross, cy, cx, cy+cross);
			g.drawLine(cx, cy+cross, cx+cross, cy-cross);
			g.setColor(Color.WHITE);
			g.drawLine(cx+cross, cy-cross, cx+r, cy-r);
		}
		
		public void drawEX(int file, int mod, Graphics g) {
			int xOffs = xIndent + (mod+1) * colW;
			int yOffs = strIndent + yIndent + (file+1) * rowH;
			int r = (rowH-5)/2;
			int cx = xOffs + colW / 2;
			int cy = yOffs + rowH / 2;
			g.setColor(Color.RED);
			g.fillOval(cx - r, cy - r, 2*r, 2*r);
			g.setColor(Color.BLACK);
			g.drawOval(cx - r, cy - r, 2*r, 2*r);
			int cross = (int) (r * SIN45);
			g.drawLine(cx-cross, cy-cross, cx+cross, cy+cross);
			g.drawLine(cx-cross, cy+cross, cx+cross, cy-cross);
		}
		
		final float SIN45 = (float)(0.5 * Math.sqrt(2.0));
		public int calculateColWidth() {
			int w = 0;
			FontMetrics fm = new Frame().getFontMetrics(f);
			for(int i = 0; i < modules.length; i++) {
				int c = fm.stringWidth(modules[i]);
				if(w < c)
					w = c;
			}
			for(int i = 0; i < files.length; i++) {
				int c = fm.stringWidth(files[i]);
				if(w < c)
					w = c;
			}
			return w + 10;
		}
		
		public int calculateRowHeight() {
			return new Frame().getFontMetrics(f).getHeight() + 10;
		}
	}
}

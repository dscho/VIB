package vib_app.gui;

import java.awt.*;
import java.awt.event.*;

public class VIBTextArea extends Panel {

	private String text = "";
	private Font font = new Font("Monospace", Font.BOLD, 12);
	private Color foreground = Color.BLACK;
	private Color background = Color.WHITE;
	private int borderWidth = 1;
	private int marginX = 1;
	private int marginY = 1;

	public VIBTextArea(String text, int borderWidth) {
		this(text);
		this.borderWidth = borderWidth;
	}

	public VIBTextArea(String text) {
		this.text = text;
	}

	public void setFont(Font font) {
		this.font = font;
		this.repaint();
	}

	public void setText(String text) {
		synchronized(this) {
		this.text = text;
		repaint();
		}
	}

	public void setMarginX(int m) {
		this.marginX = m;
		repaint();
	}

	public void setMarginY(int m) {
		this.marginY = m;
		repaint();
	}

	public void setBorderWidth(int w) {
		this.borderWidth = w;
		repaint();
	}

	public void setForeGround(Color foreground) {
		this.foreground = foreground;
		repaint();
	}

	public void setBackground(Color background) {
		this.background = background;
		repaint();
	}
	
	public Dimension getPreferredSize() {
		FontMetrics fm = getFontMetrics(font);
		// Just put it in one line
		return new Dimension(
				fm.stringWidth(text) + 2*marginX + 2*borderWidth,
				fm.getHeight() + 2*marginY + 2*borderWidth);
	}


	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public void paint(Graphics g) {
		// draw the border
		if(borderWidth > 0) {
			g.setColor(foreground);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(background);
			g.fillRect(borderWidth, 
					borderWidth, 
					getWidth() - 2*borderWidth, 
					getHeight() - 2*borderWidth);
		}
		g.setFont(font);
		g.setColor(foreground);
		FontMetrics fm = getFontMetrics(font);
		int lineHeight = fm.getHeight();
		int w = this.getWidth() - 2*borderWidth - 2*marginX;
		char[] chars = text.toCharArray();
		int x = marginX + borderWidth, y = marginY + lineHeight;
		int offset = 0;
		
		int i = 0;
		while(i < chars.length) {
			// look for the index up to which a String can be drawn in one line
			for(; i < chars.length && 
					fm.charsWidth(chars, offset, i-offset) < w; i++);
			if(i < chars.length) i--;
			// draw the line
			g.drawChars(chars, offset, i-offset, x, y);
			// save current position
			offset = i;
			// move to the next line
			y += lineHeight;
		}
	}

	public static void main(String[] args) {
		Frame f = new Frame();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		f.setLayout(gridbag);

		VIBTextArea mine = new VIBTextArea("AberAberAberAberAberAber" + 
				"AberAberAberAberAberAber", 3);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = c.weighty = 0.5;
		gridbag.setConstraints(mine, c);
		f.add(mine);

		//f.setSize(200,200);
		f.pack();
		f.setVisible(true);
	}
}
	

package vib.app.gui;

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
	private char[] chars;

	public VIBTextArea(String text, int borderWidth) {
		this(text);
		this.borderWidth = borderWidth;
	}

	public VIBTextArea(String text) {
		this.text = text;
		this.chars = text.toCharArray();
	}

	public void setFont(Font font) {
		this.font = font;
		this.repaint();
	}

	public void setText(String text) {
		this.text = text;
		this.chars = text.toCharArray();
//		Graphics g = getGraphics();
//		if(g != null){
//			paint(g);
//		}
		repaint();
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

	public void setForeground(Color foreground) {
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
		Dimension pref =  new Dimension(
				fm.stringWidth(text) + 2*marginX + 2*borderWidth,
				fm.getHeight() + 2*marginY + 2*borderWidth);
		return pref;
	}


	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public void paint(Graphics g) {
		// draw the border
		if(borderWidth > 0) {
			g.setColor(foreground);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		// draw the background
		g.setColor(background);
		g.fillRect(borderWidth, 
				borderWidth, 
				getWidth() - 2*borderWidth, 
				getHeight() - 2*borderWidth);
		// and here comes the text
		g.setFont(font);
		g.setColor(foreground);
		FontMetrics fm = getFontMetrics(font);
		int lineHeight = fm.getHeight();
		int w = this.getWidth() - 2*borderWidth - 2*marginX;
		int x = marginX + borderWidth, y = marginY + lineHeight + borderWidth;
		int offset = 0;
		int previousSpacePosition = -1;
		
		int i = offset;
		while(i < chars.length) {
			// look for the space index up to which a String 
			// can be drawn in one line
			for(i = offset; i < chars.length; i++) {
				if(chars[i] == ' ') {
					previousSpacePosition = i;
				} 
					
				// it's not a ' ', but it's the last character
				else if(i == chars.length - 1) 
					previousSpacePosition = i+1;
			
				// if it's not a ' ' and previousSpacePosition is still -1,
				// and we exceed already the line width, then
				// we set it to i-1, in order to just draw the string
				// as much as possible, and break.
				else if(fm.charsWidth(chars, offset, i-offset) > w) {
					if(previousSpacePosition == -1)
						previousSpacePosition = i-1;
					break;
				}
			}
				
			// draw the line
			g.drawChars(chars, offset, previousSpacePosition-offset, x, y);
			// save current position
			offset = previousSpacePosition;
			previousSpacePosition = -1;
			// move to the next line
			y += lineHeight;
		}
	}

	public static void main(String[] args) {
		Frame f = new Frame();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		f.setLayout(gridbag);

		VIBTextArea mine = new VIBTextArea(
				"AberAberAberAberAberAber" + 
				"AberAberAberAberAberAber " + 
				"Aber Aber Aber Aber Aber Aber " + 
				"Aber Aber Aber Aber Aber Aber", 3);
		mine.setMarginX(3);
		mine.setMarginY(3);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = c.weighty = 0.5;
		gridbag.setConstraints(mine, c);
		f.add(mine);

		//f.setSize(200,200);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.pack();
		f.setVisible(true);
	}
}
	

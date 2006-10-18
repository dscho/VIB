package vib.app.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;

public class Bubble extends Component {

	public static final int BUBBLE_W = 150;
	public static final int BUBBLE_H = 100;
	public static final int LINE_W = 3;
	public static final int INDENT = 30;

	private static final Font FONT = new Font("Monospace", Font.BOLD, 14);

	private Color bg = Color.YELLOW;
	private Color fontColor = Color.BLACK;
	private Color selected_bg = new Color(255,160,90);
	private String line1;
	private String line2;
	private boolean selected = false;

	private String actionCommand;
	private List<ActionListener> listeners = new ArrayList<ActionListener>();

	private boolean repaintStringOnly = false;
	
	public Bubble(String line1, String line2){
		super();
		this.setSize(new Dimension(BUBBLE_W+2*LINE_W, BUBBLE_H+2*LINE_W));
		this.line1 = line1;
		this.line2 = line2;
		this.addMouseListener(new MouseAdapter(){
			public void mouseEntered(MouseEvent e) {
				setFontColor(Color.BLUE);
			}
			public void mouseExited(MouseEvent e) {
				setFontColor(Color.BLACK);
			}
			public void mouseClicked(MouseEvent e) {
				ActionEvent event = new ActionEvent(Bubble.this, 
						ActionEvent.ACTION_PERFORMED, actionCommand);
				processActionEvent(event);
			}
		});
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		repaint();
	}

	public boolean isSelected() {
		return selected;
	}

	public void setActionCommand(String command) {
		this.actionCommand = command;
	}

	public void addActionListener(ActionListener l) {
		this.listeners.add(l);
	}

	public void removeActionListener(ActionListener l) {
		this.listeners.remove(l);
	}

	public void processActionEvent(ActionEvent e) {
		for(int i = 0; i < listeners.size(); i++) {
			((ActionListener)listeners.get(i)).actionPerformed(e);
		}
	}
	
	public Point getCenter() {
		Point loc = this.getLocation();
		return new Point(loc.x + LINE_W + BUBBLE_W/2,
				loc.y + LINE_W + BUBBLE_H/2);
	}

	public boolean contains(Point p) {
		return p.distance(getCenter()) < 50;
	}

	public void setFontColor(Color c) {
		this.fontColor = c;
		repaintStringOnly = true;
		repaint();
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		Point loc = new Point(0,0);
		Graphics2D g2d = (Graphics2D)g;
		if(!repaintStringOnly) {
			g2d.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2d.setStroke(new BasicStroke(LINE_W));
			Color backg = selected ? selected_bg : bg;
			g2d.setColor(backg);
			g2d.fillOval(loc.x + LINE_W, loc.y + LINE_W, BUBBLE_W, BUBBLE_H);
			g2d.setColor(Color.BLACK);
			g2d.drawOval(loc.x + LINE_W, loc.y + LINE_W, BUBBLE_W, BUBBLE_H);
		} else {
			repaintStringOnly = false;
		}
		g2d.setFont(FONT);
		g2d.setColor(fontColor);
		g2d.drawString(line1, loc.x + INDENT, loc.y + BUBBLE_H/2 - 3);
		g2d.drawString(line2, loc.x + INDENT, 
				loc.y + BUBBLE_H/2 + FONT.getSize());
		
	}
}


package vib.app.gui;

import java.awt.Panel;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainCanvas extends Panel implements ActionListener {

	public static final String OPTIONS_BUBBLE = "options";
	public static final String REGISTRATION_BUBBLE = "registration";
	public static final String RESULTS_BUBBLE = "results";
	
	private Bubble options;
	private Bubble registration;
	private Bubble results;

	private Arrow arr1, arr2;
	
	private Bubble activeBubble = null;

	public static int WIDTH = 200;
	
	private static final Point optionsLoc = new Point(20, 50);
	private static final Point registrationLoc = new Point(20, 200);
	private static final Point resultsLoc = new Point(20, 350);


	public MainCanvas(ActionListener l) {
		setPreferredSize(new Dimension(WIDTH, 480));
		setBackground(Color.ORANGE);
		setLayout(null);

		options = createBubble("Options &", "Preprocessing", 
								optionsLoc, l, OPTIONS_BUBBLE);
		options.addActionListener(this);

		registration = createBubble("Registration &","Averaging",
								registrationLoc, l, REGISTRATION_BUBBLE);
		registration.addActionListener(this);
		
		results = createBubble("Results", "Probability Map", 
								resultsLoc, l, RESULTS_BUBBLE);
		results.addActionListener(this);

		Point arr1Tail = new Point(options.getCenter());
		arr1Tail.translate(0, 1 * Bubble.BUBBLE_H / 4);
		Point arr1Head = new Point(registration.getCenter());
		arr1Head.translate(0, -1 * Bubble.BUBBLE_H / 2);
		Point arr1Cont = new Point(arr1Tail.x, arr1Head.y);
		arr1 = new Arrow(this, arr1Tail, arr1Head, arr1Cont, Color.RED);
		
		Point arr2Tail = new Point(registration.getCenter());
		arr2Tail.translate(0, 1 * Bubble.BUBBLE_H / 4);
		Point arr2Head = new Point(results.getCenter());
		arr2Head.translate(0, -1 * Bubble.BUBBLE_H / 2);
		Point arr2Cont = new Point(arr2Tail.x, arr2Head.y); 
		arr2 = new Arrow(this, arr2Tail, arr2Head, arr2Cont, Color.RED);
		
		add(arr1);
		add(arr2);
		add(options);
		add(registration);
		add(results);
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		super.paint(g);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof Bubble) {
			deactivateActiveBubble();
			Bubble bub = (Bubble)e.getSource();
			bub.setSelected(true);
			activeBubble = bub;			
		}
	}

	public void deactivateActiveBubble() {
		if(activeBubble != null) {
			activeBubble.setSelected(false);
			activeBubble = null;
		}
	}

	private final Bubble createBubble(String label1, String label2, 
					Point location, ActionListener l, String command) {
		Bubble bubble = new Bubble(label1, label2);
		bubble.setActionCommand(command);
		bubble.addActionListener(l);
		bubble.setLocation(location);
		return bubble;
	}	
	
	public static void main(String[] args) {
		java.awt.Frame f = new java.awt.Frame();
		f.add(new MainCanvas(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
			}
		}));
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				System.exit(0);
			}
		});
	}
}


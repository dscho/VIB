package vib_app.gui;

import java.awt.Panel;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;

import java.awt.event.ActionListener;

public class MainCanvas extends Panel {

	private Bubble options;
	private Bubble registration;
	private Bubble results;

	public static int WIDTH = 200;
	
	private static final Point optionsLoc = new Point(20, 50);
	private static final Point registrationLoc = new Point(20, 200);
	private static final Point resultsLoc = new Point(20, 350);

	private Arrow arr1, arr2;
	
	public MainCanvas(ActionListener l) {
		setPreferredSize(new Dimension(WIDTH, 480));
		setBackground(Color.ORANGE);
		setLayout(null);

		options = new Bubble(Color.YELLOW, "Options &", "Preprocessing");
		options.setActionCommand("options");
		options.addActionListener(l);
		options.setLocation(optionsLoc);

		registration = new Bubble(Color.YELLOW, "Registration &","Averaging");
		registration.setActionCommand("registration");
		registration.addActionListener(l);
		registration.setLocation(registrationLoc);

		results = new Bubble(Color.YELLOW, "Results", "Probability Map");
		results.setActionCommand("results");
		results.addActionListener(l);
		results.setLocation(resultsLoc);

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


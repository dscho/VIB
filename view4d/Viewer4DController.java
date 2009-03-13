package view4d;

import ij.gui.GenericDialog;

import java.awt.*;
import java.awt.event.*;

import java.net.URL;
import java.awt.image.ImageProducer;

import vib.segment.ImageButton;

/**
 * This class implements the window with the controls for the 4D viewer.
 *
 * @author Benjamin Schmid
 */
public class Viewer4DController implements ActionListener {

	private static final String[] FILES = new String[] {
				"icons/first.png",
				"icons/previous.png",
				"icons/next.png",
				"icons/last.png",
				"icons/play.png",
				"icons/pause.png",
				"icons/faster.png",
				"icons/slower.png"};

	private static final String[] COMMANDS = new String[] {
			"FIRST", "PREV", "NEXT", "LAST",
			"PLAY", "PAUSE", "FASTER", "SLOWER"};


	private ImageButton[] buttons = new ImageButton[FILES.length];
	private Viewer4D viewer4d;

	/**
	 * Initializes a new Viewer4DController;
	 * opens a new new window with the control buttons for the 4D viewer.
	 * @param viewer
	 */
	public Viewer4DController(Viewer4D viewer) {
		this.viewer4d = viewer;

		GenericDialog gd = new GenericDialog("4D Viewer");
		Panel p = new Panel(new FlowLayout());
		for(int i = 0; i < FILES.length; i++) {
			buttons[i] = new ImageButton(loadIcon(FILES[i]));
			buttons[i].addActionListener(this);
			buttons[i].setActionCommand(COMMANDS[i]);
			p.add(buttons[i]);
		}
		gd.addPanel(p);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				viewer4d.releaseContents();
			}
		});
		gd.setModal(false);
		gd.showDialog();
	}

	private Image loadIcon(String name) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource(name);
			img = Toolkit.getDefaultToolkit()
				.createImage((ImageProducer)url.getContent());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (img == null)
			throw new RuntimeException("Image not found: " + name);
		return img;
	}

	public void actionPerformed(ActionEvent e) {
		for(int i = 0; i < buttons.length; i++)
			buttons[i].repaint();

		String command = e.getActionCommand();
		if(command.equals("NEXT")) {
			viewer4d.next();
		} else if(command.equals("PREV")) {
			viewer4d.previous();
		} else if(command.equals("PLAY")) {
			viewer4d.play();
		} else if(command.equals("FIRST")) {
			viewer4d.first();
		} else if(command.equals("LAST")) {
			viewer4d.last();
		} else if(command.equals("PAUSE")) {
			viewer4d.pause();
		} else if(command.equals("FASTER")) {
			viewer4d.faster();
		} else if(command.equals("SLOWER")) {
			viewer4d.slower();
		}
	}
}


package vib_app.gui;

import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

public class VIBMenuBar extends MenuBar {

	public VIBMenuBar(ActionListener l) {
		super();
		MenuItem item;
		
		Menu proj = new Menu("Project");
		item = new MenuItem("New Project", new MenuShortcut(KeyEvent.VK_N));
		item.addActionListener(l);
		proj.add(item);
		item = new MenuItem("Open Project", new MenuShortcut(KeyEvent.VK_O));
		item.addActionListener(l);
		proj.add(item);
		item = new MenuItem("Close Project",new MenuShortcut(KeyEvent.VK_W));
		item.addActionListener(l);
		proj.add(item);
		proj.addSeparator();
		item = new MenuItem("Quit", new MenuShortcut(KeyEvent.VK_Q));
		item.addActionListener(l);
		proj.add(item);
		add(proj);

		/*
		Menu vib = new Menu("VIB");
		vib.add(new MenuItem("Load Leica data set"));
		vib.add(new MenuItem("Split channels"));
		vib.add(new MenuItem("Create labelfields"));
		vib.add(new MenuItem("Resample"));
		vib.add(new MenuItem("Calculate tissue statistics"));
		vib.add(new MenuItem("Register"));
		vib.add(new MenuItem("Calculate average brain"));
		vib.addSeparator();
		vib.add(new MenuItem("VIB assistent", new MenuShortcut(KeyEvent.VK_A)));
		add(vib);

		Menu show = new Menu("Show");
		show.add(new MenuItem("Image from file"));
		add(show);
		*/
		
		Menu help = new Menu("Help");
		item = new MenuItem("Contents", new MenuShortcut(KeyEvent.VK_F1));
		item.addActionListener(l);
		help.add(item);
		add(help);

		setHelpMenu(help);
	}
}

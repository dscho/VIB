package vib.app.gui;

import vib.app.Options;
import vib.app.Executer;
import vib.app.FileGroup;

import vib.app.module.Resample;
import vib.app.module.Load;
import vib.app.module.Label;
import vib.app.module.Module;

import vib.app.gui.dialog.LabelPanel;
import vib.app.gui.dialog.OptionsDialog;
import vib.app.gui.dialog.Console;

import java.awt.Color;
import java.awt.Panel;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.ScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.File;

import ij.ImagePlus;

public class MainFrame extends Frame {

	private Options options;
	private MainCanvas menu;
	private Panel cards;
	//private OptionsDialog optionsPanel;
	//private PreprocessingDialog preprocessingPanel;
	//private LabelPanel labelPanel;
	//private Panel emptyPanel;
	private ActionListener vibListener = new VIBActionListener();

	public MainFrame(Options options) {
		super("The VIB Protocol");
		this.options = options;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		menu = new MainCanvas(vibListener);
		add(menu, BorderLayout.WEST);
		
		cards = new Panel();
		cards.setPreferredSize(new Dimension(520, 480));
		cards.setBackground(Color.ORANGE);
		add(cards);
		
		pack();
		setVisible(true);

		if(options.isValid()) {
			Executer ex = new Executer(options);
			ex.setOutputPanel(cards);
			ex.run();
		}
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public void enableAllInput(boolean b) {
		// TODO
	}

	private class VIBActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			String command = e.getActionCommand();
			if(command.equals("Cancel")) {
				// do nothing
			} else if(command.equals("options")) {
				Executer ex = new Executer(options);
				ex.setOutputPanel(cards);
				ex.run();
			} else if(command.equals("Quit")) {
			} else if(command.equals("registration")) {
			} else if(command.equals("results")) {
			}
		}
	}
}

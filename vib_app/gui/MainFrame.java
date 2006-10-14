package vib_app.gui;

import vib_app.App;
import vib_app.Options;
import vib_app.FileGroup;

import vib_app.module.Load;
import vib_app.module.Label;
import vib_app.module.Module;

import vib_app.gui.dialog.OptionsDialog;
import vib_app.gui.dialog.PreprocessingDialog;

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
	private OptionsDialog optionsPanel;
	private PreprocessingDialog preprocessingPanel;
	private Panel emptyPanel;
	private ActionListener vibListener = new VIBActionListener();

	public MainFrame(Options options) {
		super("The VIB Protocol");
		this.options = options;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		this.setMenuBar(new VIBMenuBar(vibListener));
		
		menu = new MainCanvas(vibListener);
		add(menu, BorderLayout.WEST);
		
		emptyPanel = new Panel();
		optionsPanel = new OptionsDialog(options, vibListener);
		preprocessingPanel = new PreprocessingDialog("Everything fine");
		
		cards = new Panel(new CardLayout());
		cards.setPreferredSize(new Dimension(520, 480));
		cards.setBackground(Color.ORANGE);
		cards.add(emptyPanel, "empty");
		cards.add(optionsPanel, "options");
		cards.add(preprocessingPanel, "preprocessing");
		add(cards);
		
		pack();
		setVisible(true);
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public void disposeCurrentPanel() {
		CardLayout cl = (CardLayout)(cards.getLayout());
		cl.show(cards, "empty");
	}

	private class VIBActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			String command = e.getActionCommand();
			if(command.equals("Cancel")) {
				disposeCurrentPanel();
			} if(command.equals("options")) {
				optionsPanel.setOptions(options);
				CardLayout cl = (CardLayout)(cards.getLayout());
				cl.show(cards, command);
			} else if(command.equals("preprocessing")) {
				CardLayout cl = (CardLayout)(cards.getLayout());
				cl.show(cards, command);
				FileGroup fg = options.getFileGroup();
				int numChannel = options.getNumChannels();
				Module m = null;
				for(int i = 0; i < fg.size(); i++) {
					System.out.println("loading " + fg.get(i).getName() +"...");
					m = new Load(fg.get(i), numChannel);
					m.addMessageReceiver(preprocessingPanel);
					ImagePlus imp = (ImagePlus)m.execute();
					System.out.println(imp.getTitle());
					System.out.println("labelling...");
					m = new Label(imp);
					m.execute();
				}
			} else if(command.equals("Quit")) {
				System.exit(0);
			} else if(command.equals("registration")) {
//				vib_app.module.Module m = new vib_app.module.Label();
//				m.execute();
			} else if(command.equals("results")) {
			}
		}
	}
}

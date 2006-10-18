package vib.app.gui.dialog;

import vib.app.Options;
import vib.app.FileGroup;

import ij.IJ;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class OptionsDialog extends Panel implements ActionListener {

	private Button saveButton, loadButton, okButton, cancelButton;
	private Button workingDirButton;
	private Button fileGroupButton;
	private Choice templateChoice;
	private TextField workingDirTF;

	private Options copyOptions;
	private Options origOptions;
	
	private Panel cards;
	private FilesPanel filesPanel;
	private ChannelsPanel channelsPanel;
	private AlgorithmsPanel algorithmsPanel;

	private ActionListener listener;
	
	private FileDialog fd = new FileDialog(new Frame());
	
	/*
	public OptionsDialog(Frame parent, Options o) {

		super(parent);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		this.origOptions = o;
		this.copyOptions = o.clone();
		this.createGUI();
		pack();
		setVisible(true);
		
	}*/

	public OptionsDialog(Options o, ActionListener l) {
		super(new BorderLayout());
		this.listener = l;
		this.origOptions = o;
		this.copyOptions = o.clone();
		this.createGUI();
		setPreferredSize(new Dimension(480, 480));
	}
	
	public void createGUI() {
		// title
		Label l = new Label("Options", Label.CENTER);
		l.setFont(new Font("Monospace", Font.BOLD, 20));
		l.setForeground(Color.RED);
		this.add(l, BorderLayout.NORTH);

		// cards panel
		cards = new Panel(new CardLayout());
		filesPanel = new FilesPanel(copyOptions);
		channelsPanel = new ChannelsPanel(copyOptions);
		algorithmsPanel = new AlgorithmsPanel(copyOptions);
		cards.add(filesPanel, "Files");
		cards.add(channelsPanel, "Channels");
		cards.add(algorithmsPanel, "Algorithms");
		this.add(cards, BorderLayout.CENTER);

		// card selector
		final List list = new List(3);
		list.setBackground(new Color(255,255,100));
		list.setFont(new Font("Monospace", Font.BOLD, 12));
		list.add("Files");
		list.add("Channels");
		list.add("Algorithms");
		list.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				CardLayout cl = (CardLayout)(cards.getLayout());
				cl.show(cards, list.getSelectedItem());
			}
		});
		list.select(0);
		this.add(list, BorderLayout.WEST);
		
		// buttons panel
		Panel buttons = new Panel(new FlowLayout());
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.addActionListener(listener);
		buttons.add(cancelButton);
		loadButton = new Button("Load options");
		loadButton.addActionListener(this);
		buttons.add(loadButton);
		saveButton = new Button("Save options");
		saveButton.addActionListener(this);
		buttons.add(saveButton);
		okButton = new Button("Continue with preprocessing >>");
		okButton.setActionCommand("preprocessing");
		okButton.addActionListener(this);
		okButton.addActionListener(listener);
		buttons.add(okButton);
		this.add(buttons, BorderLayout.SOUTH);

	}

	public void fillForms() {
		System.out.println("fillForms()");
		this.filesPanel.fillForms();
		this.channelsPanel.fillForms();
		this.algorithmsPanel.fillForms();
	}

	public void setOptions(Options options) {
		this.origOptions = options;
		this.copyOptions = origOptions.clone();
		this.filesPanel.setOptions(copyOptions);
		this.channelsPanel.setOptions(copyOptions);
		this.algorithmsPanel.setOptions(copyOptions);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == okButton) {
			origOptions.copy(copyOptions);
			origOptions.setResamplingFactor(
					algorithmsPanel.getResamplingFactor());
			//this.dispose();
		} else if(e.getSource() == cancelButton) {
			//this.dispose();
		} else if(e.getSource() == loadButton) {
			fd.setVisible(true);
			String f = fd.getDirectory() + fd.getFile();
			if(f != null){
				File file = new File(f);
				if(file.exists()) {
					copyOptions.loadFrom(f);
					this.filesPanel.setOptions(copyOptions);
					this.channelsPanel.setOptions(copyOptions);
					this.algorithmsPanel.setOptions(copyOptions);
				} else {
					IJ.showMessage("Can't load options file " + f);	
					return;
				}
			}
		} else if(e.getSource() == saveButton) {
			copyOptions.setResamplingFactor(
					algorithmsPanel.getResamplingFactor());
			fd.setVisible(true);
			String f = fd.getDirectory() + fd.getFile();
			copyOptions.saveTo(f);
		}
	}
}

package vib.app.gui;

import vib.app.FileGroup;

import java.io.File;

import java.awt.List;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Insets;
import java.awt.Font;
import java.awt.ScrollPane;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.FlowLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FileDialog;
import java.awt.Dimension;
import java.awt.TextField;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FileGroupDialog extends Dialog implements ActionListener {

	private final FileDialog fd = new FileDialog(this, "Open...", 
			FileDialog.LOAD);

	private FileGroup workingFG;
	private FileGroup originalFG;
	private boolean showWholePath = false;
	
	private List list;
	private TextField nameTF;
	private Button ok, add, delete, cancel;
	private Checkbox wholePath;

	public FileGroupDialog(Frame owner, FileGroup fg) {
		super(owner);
		setModal(true);
		setTitle("Edit filegroup");
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		setPreferredSize(new Dimension(300,480));
		this.originalFG = fg;
		this.workingFG = fg.clone(); // working copy
		list = new List();
		list.setMultipleMode(true);
		createList();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);

		Label name = new Label("File group name");
		name.setFont(new Font("Monospace", Font.BOLD, 14));
		c.gridx = c.gridy = 0;
		c.insets = new Insets(5,5,5,5);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(name, c);
		this.add(name);

		nameTF = new TextField(20);
		nameTF.setText(this.workingFG.getName());
		c.gridx++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		gridbag.setConstraints(nameTF, c);
		this.add(nameTF);

		ScrollPane scroll = new ScrollPane();
		scroll.add(list);
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = c.weighty = 0.5;
		gridbag.setConstraints(scroll, c);
		this.add(scroll);

		wholePath = new Checkbox("Show whole path", showWholePath);
		wholePath.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showWholePath = wholePath.getState();
				createList();
				repaint();
			}
		});
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		gridbag.setConstraints(wholePath, c);
		this.add(wholePath);
		
		Panel buttons = new Panel(new FlowLayout());
		add = new Button("Add");
		add.addActionListener(this);
		buttons.add(add);
		delete = new Button("Delete");
		delete.addActionListener(this);
		buttons.add(delete);
		cancel = new Button("Cancel");
		cancel.addActionListener(this);
		buttons.add(cancel);
		ok = new Button("OK");
		ok.addActionListener(this);
		buttons.add(ok);
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(buttons, c);
		this.add(buttons);
		
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == ok) {
			originalFG.copy(workingFG);
			originalFG.setName(nameTF.getText());
			setModal(false);
			this.dispose();			
		} else if(e.getSource() == cancel) {
			this.dispose();
		} else if(e.getSource() == add) {
			fd.setVisible(true);
			String f = fd.getDirectory() + fd.getFile();
			if(f != null)
				if(!workingFG.add(f))
					System.out.println("File " + f + 
							" could not be added to the filegroup");
			createList();
			repaint();			
		} else if(e.getSource() == delete) {
			int[] idx = list.getSelectedIndexes();
			for(int i = 0; i < idx.length; i++){
				workingFG.remove(idx[i]);
			}
			createList();
			repaint();
		}
	}

	private void createList() {
		list.clear();
		for(int i = 0; i < workingFG.size(); i++) {
			if(showWholePath)
				list.add(workingFG.get(i).getAbsolutePath());
			else
				list.add(workingFG.get(i).getName());
		}
	}

	public static void main(String[] args) {
		FileGroup fg = new FileGroup("TestGroup");
		fg.add("/home/bene/apt_list");
		fg.add("/home/bene/tmp.java");
		fg.add("/home/bene/tmp.class");

		java.awt.Frame f = new java.awt.Frame();

		FileGroupDialog d = new FileGroupDialog(f, fg);
		fg.debug();
	}
}


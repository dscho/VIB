package vib.app.gui;

import vib.app.FileGroup;

import java.io.File;

import java.awt.List;
import java.awt.GridLayout;
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

public class FileGroupDialog extends Panel implements ActionListener {

	private final FileDialog fd = new FileDialog(new Frame(), "Open...", 
			FileDialog.LOAD);

	private FileGroup files;
	private boolean showWholePath = false;
	
	private List list;
	private TextField nameTF;
	private Button add, delete, template;
	private Checkbox wholePath;

	public FileGroupDialog(FileGroup fg) {
		super();
		this.files = fg;
		list = new List();
	//	list.setMultipleMode(true);
		createList();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);

		Label name = new Label("Files: ");
		name.setFont(new Font("Monospace", Font.BOLD, 14));
		c.gridx = c.gridy = 0;
		c.insets = new Insets(5,5,5,5);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(name, c);
		this.add(name);

//		nameTF = new TextField(20);
//		nameTF.setText(this.workingFG.getName());
//		c.gridx++;
//		c.gridwidth = GridBagConstraints.REMAINDER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.weightx = 0.5;
//		gridbag.setConstraints(nameTF, c);
//		this.add(nameTF);

		ScrollPane scroll = new ScrollPane();
		scroll.add(list);
		scroll.setPreferredSize(new Dimension(300,100));
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = c.weighty = 0.5;
		gridbag.setConstraints(scroll, c);
		this.add(scroll);

		Panel buttons = new Panel(new GridLayout(3,1));
		add = new Button("Add to files");
		add.addActionListener(this);
		buttons.add(add);
		delete = new Button("Delete from files");
		delete.addActionListener(this);
		buttons.add(delete);
		template = new Button("Use as template");
		template.addActionListener(this);
		buttons.add(template);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(buttons, c);
		this.add(buttons);

		wholePath = new Checkbox("Show absolute path", showWholePath);
		wholePath.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showWholePath = wholePath.getState();
				createList();
				repaint();
			}
		});
		c.gridy++;
		c.gridx--;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		gridbag.setConstraints(wholePath, c);
		this.add(wholePath);
		
	}

	public File getSelected() {
		int selected = list.getSelectedIndex();
		if(selected != -1)
			return files.get(selected);
		return null;
	}

	public Button getTemplateButton() {
		return template;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == add) {
			fd.setVisible(true);
			String f = fd.getDirectory() + fd.getFile();
			if(f != null)
				if(!files.add(f))
					System.out.println("File " + f + 
							" could not be added to the filegroup");
			createList();
			repaint();			
		} else if(e.getSource() == delete) {
			int[] idx = list.getSelectedIndexes();
			for(int i = 0; i < idx.length; i++){
				files.remove(idx[i]);
			}
			createList();
			repaint();
		}
	}

	public void update() {
		createList();
		repaint();
	}

	private void createList() {
		list.clear();
		for(int i = 0; i < files.size(); i++) {
			if(showWholePath)
				list.add(files.get(i).getAbsolutePath());
			else
				list.add(files.get(i).getName());
		}
	}

	public static void main(String[] args) {
		FileGroup fg = new FileGroup("TestGroup");
		fg.add("/home/bene/apt_list");
		fg.add("/home/bene/tmp.java");
		fg.add("/home/bene/tmp.class");

		java.awt.Frame f = new java.awt.Frame();
		FileGroupDialog d = new FileGroupDialog(fg);
		f.add(d);
		f.pack();
		f.show();
		fg.debug();
	}
}


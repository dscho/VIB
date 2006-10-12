package vib_app.gui;

import java.io.File;

import java.util.List;
import java.util.ArrayList;

import java.awt.Color;
import java.awt.Point;
import java.awt.Font;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;

import java.awt.font.FontRenderContext;

import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;

public class FileTree extends Canvas {
	
	private static final Font FONT = new Font("Monospaced", Font.BOLD, 14);
	private static final int INDENT = 10;
	private static final int LINE_H = FONT.getSize() + 2;
	private static final int OFFS_X = 10;
	private static final int OFFS_Y = 20;
	
	private FileNode root;
	private int selectedIndex = -1;
	private List visibleFiles = new ArrayList();
	private List files = new ArrayList();		
	private int depth = 0;

	public FileTree() {
		this.setBackground(Color.WHITE);
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK |
				AWTEvent.KEY_EVENT_MASK);
	}
	
	public FileTree(FileNode rootDir) {
		this();
		this.root = rootDir;
		this.rescan();
	}

	public FileTree(File r) {
		this();
		this.root = new FileNode(r);
		this.rescan();
	}

	public void setDirectory(File dir) {
		this.root = new FileNode(dir);
		this.rescan();
	}
		
	public void paint(Graphics g) {
		for(int index = 0; index < visibleFiles.size(); index++) {
			FileNode node = (FileNode)visibleFiles.get(index);
		
			boolean selected = index == selectedIndex;
			boolean dir =  node.getFile().isDirectory();
			boolean exp = node.isExpanded();
			
			String name = node.getFile().getName();
			if(dir) 
				name = exp ? "- "  + name : "+ " + name;
			else
				name = "  " + name;
			
			FontRenderContext frc = ((Graphics2D)g).getFontRenderContext();
			int w = (int)FONT.getStringBounds(name, 0, name.length(), frc).
						getWidth();

			Color fillC = selected ? Color.BLUE : Color.WHITE;
			Color fontC = selected ? Color.WHITE : Color.BLACK;

			g.setColor(fillC);
			g.fillRect(OFFS_X + node.getDepth() * INDENT,
					OFFS_Y + (index-1) * LINE_H, w, LINE_H);
			
			g.setColor(fontC);
			g.setFont(FONT);
			g.drawString(name, 
						OFFS_X + node.getDepth() * INDENT, 
						OFFS_Y + index * LINE_H - 2);
		}
	}

	public void calcVisibleFiles() {
		visibleFiles.clear();
		for(int i = 0; i < files.size(); i++){
			FileNode node = (FileNode)files.get(i);
			int depth = node.getDepth();
			visibleFiles.add(node);
			if(!node.isExpanded()){
				while(++i < files.size()){
					boolean nextIsChild = ((FileNode)files.get(i)).
								getDepth() > depth;
					if(!nextIsChild)
						break;
				}
				--i;
			}
		}
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(640, 480);
	}
	
	public void processMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			int index = (e.getY() + LINE_H - OFFS_Y) / LINE_H; 
			FileNode node = (FileNode)visibleFiles.get(index);
			if(e.getX() - OFFS_X - node.getDepth() * INDENT < 10) {
				node.toggleExpand();
				calcVisibleFiles();
			} else {
				selectedIndex = index;
			}
			repaint();
		}
	}

	public void processKeyEvent(KeyEvent e) {
		if (e.getID() == KeyEvent.KEY_PRESSED) {
			int code = e.getKeyCode();
			if (code == e.VK_UP && selectedIndex > 0)
				--selectedIndex;
			else if (code == e.VK_DOWN && selectedIndex < getItemCount() - 1)
				++selectedIndex;
			else if (code == e.VK_RIGHT && selectedIndex != -1) {
				FileNode n = (FileNode)visibleFiles.get(selectedIndex);
				if(!n.isExpanded() && n.getFile().isDirectory()) {
					n.toggleExpand();
					calcVisibleFiles();
				}
			}
			else if (code == e.VK_LEFT && selectedIndex != -1) {
				FileNode n = (FileNode)visibleFiles.get(selectedIndex);
				if(n.isExpanded() && n.getFile().isDirectory()) {
					n.toggleExpand();
					calcVisibleFiles();
				}
			}
			repaint();
		}
	}

	public int getItemCount() {
		if(files.size() == 0)
			rescan();	
		return visibleFiles.size();
	}
		

	public void rescan() {
		files.clear();
		scanFileSystem(root);
		calcVisibleFiles();
		repaint();
	}
	
	private void scanFileSystem(FileNode node) {
		node.setDepth(depth);
		files.add(node);
		FileNode[] children = node.children();
		int l = children.length;
		if(l != 0){
			depth++;
			for(int i = 0; i < l; i++) {
				scanFileSystem(children[i]);
			}
			depth--;
		}
	}		

	public class FileNode {
		private File file;
		private boolean expanded = true;
		private int depth;

		public FileNode(File file) {
			if(!file.exists()) {
				System.out.println(file.getPath() + " does not exist!");
				return;
			}
			this.file = file;
		}

		public void toggleExpand() {
			this.expanded = !this.expanded;
		}

		public FileNode[] children() {
			if(!file.isDirectory())
				return new FileNode[]{};
			File[] files = file.listFiles();
			FileNode[] children = new FileNode[files.length];
			for(int i = 0; i < files.length; i++) {
				children[i] = new FileNode(files[i]);
			}
			return children;
		}

		public boolean isExpanded() {
			return expanded;
		}

		public int getDepth() {
			return depth;
		}

		public void setDepth(int depth) {
			this.depth = depth;
		}

		public File getFile() {
			return file;
		}

		public String toString() {
			return file.getName() + " (" + depth + ")";
		}
	}

	public static void main(String[] args) {
		java.awt.Frame frame = new java.awt.Frame("Test");
		frame.setSize(640, 480);
		frame.add(new FileTree(new File("/home/bene/tmp")));
		frame.addWindowListener(new java.awt.event.WindowAdapter(){
			public void windowClosing(java.awt.event.WindowEvent e){
				System.exit(0);
			}
		});
		frame.setVisible(true);
	}
}


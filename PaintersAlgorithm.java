import java.util.Stack;

public class PaintersAlgorithm {
	private int w,h;
	private byte color;
	private byte[] pixels;
	private boolean[] done;
	private Stack stack;

	public PaintersAlgorithm(int w,int h,byte[] pixels) {
		this.w=w;
		this.h=h;
		this.pixels=pixels;
	}

	public int x,y;

	public void init(int x,int y) {
		this.x=x;
		this.y=y;
		color=pixels[x+w*y];
		done=new boolean[w*h];
		stack=new Stack();
	}

	private class BranchPoint {
		int x,y,step;
		BranchPoint(int x,int y,int step) {
			this.x=x; this.y=y; this.step=step;
		}
	}

	private boolean inside(int x,int y) {
		return x>=0 && y>=0 && x<w && y<h;
	}
	private final static int[] X={-1,-1,0,1,1,1,0,-1};
	private final static int[] Y={0,1,1,1,0,-1,-1,-1};
	public boolean next() {
		return next(x,y,0);
	}
	boolean next(int x,int y,int step) {
		while(true) {
			for(int i=step;i<X.length;i++) {
				int x1=x+X[i];
				int y1=y+Y[i];
				if(inside(x1,y1) && !done[x1+y1*w] &&
						pixels[x1+y1*w]==color) {
					if(i+1<X.length)
						stack.push(new BranchPoint(x,y,i+1));
					this.x=x1;
					this.y=y1;
					done[x+y*w]=true;
					return true;
				}
			}
			if(stack.empty())
				return false;
			BranchPoint p=(BranchPoint)stack.pop();
			x=p.x; y=p.y; step=p.step;
		}
	}
}


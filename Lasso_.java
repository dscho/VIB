import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

public class Lasso_ implements PlugInFilter{
	private byte[] pixels;
	private int[] dijkstra;

	private int w, h;

	public void run(ImageProcessor ip){
		w = ip.getWidth();
		h = ip.getHeight();

		pixels = (byte[])ip.getPixels();
		initDijkstra(100, 100);
		new ImagePlus("Dijkstra", new FloatProcessor(w, h, dijkstra)).show();
	}

	final int get(int x, int y) {
		return pixels[x + w * y] & 0xff;
	}

	final static int[] stepX = { -1, 0, 1, 1, 1, 0, -1, -1 };
	final static int[] stepY = { -1, -1, -1, 0, 1, 1, 1, 0 };
	final static int[] stepW = { 4, 3, 4, 3, 4, 3, 4, 3 };

	private class PixelQueue {
		private int[] xQueue, yQueue, cost;
		private boolean[] pixelInQueue;
		private int i;

		public int x, y;

		public PixelQueue() {
			xQueue = new int[w * h];
			yQueue = new int[w * h];
			cost = new int[w * h];
			pixelInQueue = new boolean[w * h];
			i = 0;
		}

		public int size() {
			return i;
		}

		public void push(int x, int y, int cost) {
			if (pixelInQueue[x + w * y]) {
				int index = findIndex(x, y);
				if (this.cost[index] < cost)
					return;
				remove(index);
			}
			int index = binarySearch(cost);
			if (index < i)
				insertAt(index);
			else
				i++;
			xQueue[index] = x;
			yQueue[index] = y;
			this.cost[index] = cost;
			pixelInQueue[x + w * y] = true;
		}

		public void pop() {
			i--;
			x = xQueue[i];
			y = yQueue[i];
			pixelInQueue[x + w * y] = false;
		}

		private void insertAt(int index) {
			System.arraycopy(xQueue, index, xQueue, index + 1, i - index);
			System.arraycopy(yQueue, index, yQueue, index + 1, i - index);
			System.arraycopy(cost, index, cost, index + 1, i - index);
			i++;
		}

		private void remove(int index) {
			i--;
			System.arraycopy(xQueue, index + 1, xQueue, index, i - index);
			System.arraycopy(yQueue, index + 1, yQueue, index, i - index);
			System.arraycopy(cost, index + 1, cost, index, i - index);
		}

		private int findIndex(int x, int y) {
			for (int j = 0; j < i; j++)
				if (xQueue[j] == x && yQueue[j] == y)
					return j;
			return -1;
		}

		private int binarySearch(int cost) {
			int i1 = -1, i2 = i;
			while (i1 + 1 < i2) {
				int i3 = (i1 + i2) / 2;
				if (this.cost[i3] >= cost)
					i1 = i3;
				else
					i2 = i3;
			}
			return i1 + 1;
		}
	}

	private void initDijkstra(int x, int y) {
		dijkstra = new int[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Integer.MAX_VALUE;

		PixelQueue queue = new PixelQueue();
		queue.push(x, y, 0);
		dijkstra[x + w * y] = 0;

		int nr = 0, total = w * h;
		while (queue.size() > 0) {
			queue.pop();
			x = queue.x;
			y = queue.y;
			int weight = dijkstra[x + w * y];
			int value = get(x, y);

			for (int i = 0; i < stepW.length; i++) {
				int x2 = x + stepX[i];
				int y2 = y + stepY[i];

				if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h)
					continue;
				int cost = Math.abs(get(x2, y2) - value);
				int newW = weight + stepW[i] * cost;
				if (dijkstra[x2 + w * y2] > newW) {
					queue.push(x2, y2, newW);

					if (dijkstra[x2 + w * y2] == Integer.MAX_VALUE) {
						nr++;
						if ((nr % 100) == 0)
							IJ.showProgress(nr, total);
					}
					dijkstra[x2 + w * y2] = newW;
				}
			}
		}
	}

	public int setup(String arg, ImagePlus img){
		return DOES_8G | NO_CHANGES;
	}
}

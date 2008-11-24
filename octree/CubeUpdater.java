package octree;

import java.util.List;

public class CubeUpdater {

	private List<Cube> queue;

	private static final int nThreads = Runtime.getRuntime().availableProcessors();
	private Thread[] threads = new WorkingThread[nThreads];
	private Thread currentThread;
	private boolean cancelled = false;

	public void updateCubes(List<Cube> cubes) {
		queue = cubes;
		for(int i = 0; i < nThreads; i++) {
			threads[i] = new WorkingThread();
			threads[i].start();
		}
		cancelled = false;
		currentThread = Thread.currentThread();
		for(Thread th : threads) {
			try {
				th.join();
			} catch(InterruptedException e) {
				System.out.println("interrupted");
			}
		}
		for(int i = 0; i < nThreads; i++)
			threads[i] = null;
	}

	public void cancel() {
		cancelled = true;
		currentThread.interrupt();
	}

	public synchronized Cube poll() {
		return queue.remove(queue.size() - 1);
	}

	private final class WorkingThread extends Thread {
		@Override
		public void run() {
			while(true) {
				Cube c = null;
				synchronized(CubeUpdater.this) {
					if(queue.isEmpty() || cancelled)
						break;
					c = poll();
				}
				try {
					if(!c.cubeDataUpToDate())
						c.updateCubeData();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}

import ij.plugin.PlugIn;

public class CollectGarbage_ implements PlugIn {
	public void run(String dummy) {
		System.gc();
		System.gc();
	}
}


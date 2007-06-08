package ij3d;

public interface UniverseListener {

	public void transformationStarted();
	public void transformationUpdated();
	public void transformationFinished();

	public void contentAdded(Content c);
	public void contentRemoved(Content c);
	public void contentChanged(Content c);
	public void canvasResized();
}

package vib_app.module;

public interface MessageReceiver {

	public void setMessage(String message);

	public void setState(boolean busy);
}

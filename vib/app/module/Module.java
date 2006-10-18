package vib.app.module;

import java.util.List;
import java.util.ArrayList;

public abstract class Module {

	public static final int DEPENDENCY_FINE = 0;
	public static final int DEPENDENDY_VIOLATED = 1;
	public static final int DATA_MISSING = 2;

	private List<MessageReceiver> messageReceiver = 
		new ArrayList<MessageReceiver>();
	
	public static class Error {
		String message;
		int id;

		public Error(int id, String message) {
			this.id = id; 
			this.message = message;
		}
	}

	public void addMessageReceiver(MessageReceiver mReceiver) {
		messageReceiver.add(mReceiver);
	}

	public void removeMessageReceiver(MessageReceiver mReceiver) {
		messageReceiver.remove(mReceiver);
	}
	
	protected void broadcast(String message) {
		for(int i = 0; i < messageReceiver.size(); i++) {
			messageReceiver.get(i).setMessage(message);
		}
	}

	protected void reportState(boolean busy) {
		for(int i = 0; i < messageReceiver.size(); i++) {
			messageReceiver.get(i).setState(busy);
		}
	}
	
	public abstract Error checkDependency();
	
	public abstract Object execute();
}

package vib_app.module;

public abstract class Module {

	public static final int DEPENDENCY_FINE = 0;
	public static final int DEPENDENDY_VIOLATED = 1;
	public static final int DATA_MISSING = 2;

	public static class Error {
		String message;
		int id;

		public Error(int id, String message) {
			this.id = id; 
			this.message = message;
		}
	}

	public abstract Error checkDependency();
	
	public abstract Object execute();

}

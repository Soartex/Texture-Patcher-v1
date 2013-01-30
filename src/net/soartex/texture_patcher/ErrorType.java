package net.soartex.texture_patcher;

enum ErrorType {

	NO_INTERNET         (true, "Could not connect to the server! Perhaps you have no internet?"),

	NO_EXTERNAL_CONFIG  (true, "The externalconfig.txt file is missing!"),
	BAD_EXTERNAL_CONFIG (true, "The externalconfig.txt file is the default!"),
	INCOMPLETE_CONFIG   (true, "The configuration file is incomplete!"),
	BAD_CONFIG          (true, "The configuration file could not be parsed as JSON!"),
	CONFIG_NOT_FOUND    (true, "The configuration file could not be found on the server!");

	private boolean fatal;
	private String message;

	private ErrorType (final boolean fatal, final String message) {

		this.fatal = fatal;
		this.message = message;

	}

	protected boolean isFatal () {

		return fatal;

	}

	protected String getMessage () {

		return message;

	}

}
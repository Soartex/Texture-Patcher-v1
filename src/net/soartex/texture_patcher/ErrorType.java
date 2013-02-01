package net.soartex.texture_patcher;

enum ErrorType {

	WINDOW_CLOSED ("The window was closed!"),
	NO_INTERNET   ("Could not connect to the server! Perhaps you have no internet?"),
	UNKNOWN_ERROR ("An unforseen error occured!"),

	NO_EXTERNAL_CONFIG      ("The externalconfig.txt file is missing!"),
	DEFAULT_EXTERNAL_CONFIG ("The externalconfig.txt file is the default!"),
	BAD_EXTERNAL_CONFIG     ("The URL in the externalconfig.txt file is bad!"),
	INCOMPLETE_CONFIG       ("The configuration file is incomplete!"),
	BAD_CONFIG              ("The configuration file could not be parsed as JSON!"),
	CONFIG_NOT_FOUND        ("The configuration file could not be found on the server!"),

	SETTING_SKIN_FAILED     ("Unable to set desired skin!"),

	UPDATE_CHECK_FAILED ("Unable to check for updates!");

	private String message;

	private ErrorType (final String message) {

		this.message = message;

	}

	protected String getMessage () {

		return message;

	}

}
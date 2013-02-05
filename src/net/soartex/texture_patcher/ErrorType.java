package net.soartex.texture_patcher;

enum ErrorType {

	UNEXPECTED_EXCEPTION ("An unexpected exception occured!"),

	WINDOW_CLOSED        ("The window was closed! (This ErrorType is used for technical purposes.)"),
	CANNOT_FIND_SERVER   ("Could not connect to the server! Perhaps you have no internet?"),

	EXTERNAL_CONFIG_MISSING ("The externalconfig.txt file is missing!"),
	EXTERNAL_CONFIG_DEFAULT ("The externalconfig.txt file is the default!"),
	EXTERNAL_CONFIG_BAD     ("The URL in the externalconfig.txt file is bad!"),
	CONFIG_INCOMPLETE       ("The configuration file is incomplete!"),
	CONFIG_BAD              ("The configuration file could not be parsed as JSON!"),
	CONFIG_NOT_FOUND        ("The configuration file could not be found on the server!"),
	CONFIG_LOADING_FAILED   ("An error occured while loading the configuration!"),

	SETTING_SKIN_FAILED ("Unable to set desired skin!"),
	SETTING_ICON_FAILED ("Unable to set icon from configured URL!"),

	MOD_LOADING_FAILED ("An error occured while loading the mods!"),

	UPDATE_CHECK_FAILED ("Unable to check for updates!");

	private String message;

	private ErrorType (final String message) {

		this.message = message;

	}

	protected String getMessage () {

		return message;

	}

}
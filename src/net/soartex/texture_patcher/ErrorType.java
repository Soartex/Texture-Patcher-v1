package net.soartex.texture_patcher;

enum ErrorType {

	// General

	UNEXPECTED_EXCEPTION ("An unexpected exception occured!"),
	WINDOW_CLOSED        ("The window was closed!"),

	// Loading the config.

	EXTERNAL_CONFIG_MISSING ("The externalconfig.txt file is missing!"),
	EXTERNAL_CONFIG_DEFAULT ("The externalconfig.txt file is the default!"),
	CONFIG_INCOMPLETE       ("The configuration file is incomplete!"),
	CONFIG_LOADING_FAILED   ("An error occured while loading the configuration!"),

	// Initializing the window.

	SKIN_SETTING_FAILED          ("Unable to set desired skin!"),
	WINDOW_INITIALIZATION_FAILED ("An error occured while initializing the window!"),
	ICON_SETTING_FAILED          ("Unable to set icon from configured URL!"),

	// Loading files.

	FILE_LOADING_FAILED    ("An error occured while loading the files!"),
	MOD_LOADING_FAILED     ("An error occured while loading the mods!"),
	MODPACK_LOADING_FAILED ("An error occured while loading the modpacks!"),

	// Initializing the components.

	COMPONENT_INITIALIZATION_FAILED ("An error occured while initializing the components!"),

	// Checking for updates.

	UPDATE_CHECKING_FAILED ("An error occured while checking for updates!"),

	// Creating the crash log.

	CRASH_LOG_CREATING_FAILED ("An error occured while creating the crash log!"),

	// Website link.

	WEBSITE_OPENING_FAILED ("An error occured while loading the website link!"),

	// Modpack selection.

	MODPACK_SELECTION_FAILED ("An error occured while loading the modpack!"),

	// Pack downloading.

	PACK_DOWNLOADING_FAILED ("An error occured while downloading the texture-pack!");

	private String message;

	private ErrorType (final String message) {

		this.message = message;

	}

	protected String getMessage () {

		return message;

	}

}
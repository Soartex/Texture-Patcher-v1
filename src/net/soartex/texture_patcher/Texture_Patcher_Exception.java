package net.soartex.texture_patcher;

final class Texture_Patcher_Exception extends Exception {

	private static final long serialVersionUID = 1L;

	protected ErrorType type;
	protected Throwable cause;

	protected Texture_Patcher_Exception (final ErrorType type, final Throwable cause) {

		this.type = type;
		this.cause = cause;

	}

	@Override public String getMessage () {

		return type.getMessage();

	}

	protected ErrorType getType () {

		return type;

	}

}
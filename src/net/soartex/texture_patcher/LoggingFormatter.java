package net.soartex.texture_patcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

final class LoggingFormatter extends Formatter {

	protected SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

	@Override public String format (final LogRecord lr) {

		// Format the message with the date and level in front of it.

		return "[" + format.format(new Date()) + "] [" + lr.getLevel() + "] " + lr.getMessage();

	}

}
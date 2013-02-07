package net.soartex.texture_patcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

final class Logging {

	protected static final class LoggingHandler extends StreamHandler {

		@Override public void publish (final LogRecord lr) {

			// Print to System.out if the level is INFO, System.err if it is not.

			if (lr.getLevel() == Level.INFO) System.out.println(getFormatter().format(lr));

			else System.err.println(getFormatter().format(lr));

		}

	}

	protected static final class LoggingFormatter extends Formatter {

		protected SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

		@Override public String format (final LogRecord lr) {

			// Format the message with the date and level in front of it.

			return "[" + format.format(new Date()) + "] [" + lr.getLevel() + "] " + lr.getMessage();

		}

	}

}
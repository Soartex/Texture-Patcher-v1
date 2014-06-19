package net.soartex.texture_patcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

final class Logging {

	protected static final class LoggingHandler extends StreamHandler {

		protected final Texture_Patcher t_p;

		protected LoggingHandler (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the logging handler.

			this.t_p = t_p;

		}

		@Override public void publish (final LogRecord lr) {

			// Print to System.out if the level is INFO, System.err if it is not.

			final String log = getFormatter().format(lr).trim();

			if (lr.getLevel() == Level.INFO) System.out.println(log);

			else System.err.println(log);

			t_p.logs.add(log);

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
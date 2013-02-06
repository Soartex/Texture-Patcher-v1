package net.soartex.texture_patcher;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

class LoggingHandler extends StreamHandler {

	@Override public void publish (final LogRecord lr) {

		// Print to System.out if the level is INFO, System.err if it is not.

		if (lr.getLevel() == Level.INFO) System.out.println(getFormatter().format(lr));

		else System.err.println(getFormatter().format(lr));

	}

}
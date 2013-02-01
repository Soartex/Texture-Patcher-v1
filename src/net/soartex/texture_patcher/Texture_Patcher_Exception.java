package net.soartex.texture_patcher;

import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

final class Texture_Patcher_Exception extends Exception {

	private static final long serialVersionUID = 1L;

	protected Texture_Patcher t_p;

	protected ErrorType type;
	protected Throwable cause;

	protected Texture_Patcher_Exception (final Texture_Patcher t_p, final ErrorType type, final Throwable cause) {

		this.t_p = t_p;

		this.type = type;
		this.cause = cause;

	}

	@Override public Throwable getCause () {

		return cause;

	}

	@Override public String getMessage () {

		return type.getMessage();

	}

	protected ErrorType getType () {

		return type;

	}

	protected void showMessageDialog (final String title, final int type) {

		if (this.type == ErrorType.UNKNOWN_ERROR) showExceptionDialog();

		else JOptionPane.showMessageDialog(t_p.frame, getMessage(), title, type);

	}

	protected void showExceptionDialog () {

		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(t_p.frame, getMessage() + "\r\nWould you like to see details about what happened?", "Error!", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)) {

			final StringWriter sw = new StringWriter();

			getCause().printStackTrace(new PrintWriter(sw));

			sw.flush();

			final JTextArea text = new JTextArea(sw.toString());
			text.setEditable(false);

			final JScrollPane pane = new JScrollPane(text);

			pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			pane.setPreferredSize(new Dimension(400, 300));

			JOptionPane.showMessageDialog(t_p.frame, pane, "Error!", JOptionPane.PLAIN_MESSAGE);

		}

	}

}
package net.soartex.texture_patcher;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.URI;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.concurrent.TimeUnit;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import javax.swing.filechooser.FileFilter;

final class Listeners {

	protected static final class TableListener implements MouseListener {

		protected final JTable table;

		protected final Texture_Patcher t_p;

		protected TableListener (final JTable table, final Texture_Patcher t_p) {

			this.table = table;

			this.t_p = t_p;

		}

		@Override public void mouseClicked (final MouseEvent e) {

			final int row = table.rowAtPoint(e.getPoint());
			final int col = table.columnAtPoint(e.getPoint());

			if (col == 0) {

				t_p.tableData[row][0] = !(Boolean)t_p.tableData[row][0];
				table.updateUI();

			}

		}

		@Override public void mouseEntered (final MouseEvent arg0) {}

		@Override public void mouseExited (final MouseEvent arg0) {}

		@Override public void mousePressed(final MouseEvent arg0) {}

		@Override public void mouseReleased (final MouseEvent arg0) {}


	}

	protected static final class WebsiteListener implements ActionListener {

		protected final Texture_Patcher t_p;

		protected WebsiteListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			try {

				Desktop.getDesktop().browse(new URI(t_p.config.getProperty("url")));

			} catch (final Exception e1) {

				e1.printStackTrace();

			}

		}

	}

	protected static final class ModpackListener implements ActionListener {

		protected final Texture_Patcher t_p;

		protected ModpackListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			if (e.getActionCommand().equals("Select All")) {

				for (int i = 0; i < t_p.tableData.length; i++) {

					t_p.tableData[i][0] = true;

				}

				t_p.table.updateUI();

			} else if (e.getActionCommand().equals("Select None")) {

				for (int i = 0; i < t_p.tableData.length; i++){

					t_p.tableData[i][0]= false;

				}

				t_p.table.updateUI();

			} else {

				for (final String modpack : t_p.modpacks.keySet()){

					if (e.getActionCommand().equals(modpack)) {

						try {

							final BufferedReader in = new BufferedReader(new InputStreamReader(t_p.modpacks.get(modpack).openStream()));

							for (int i = 0; i < t_p.tableData.length; i++) {

								t_p.tableData[i][0]= false;

							}

							t_p.table.updateUI();

							String readline;

							while ((readline = in.readLine()) != null) {

								for (int i = 0; i < t_p.tableData.length; i++) {

									try {

										if (readline.replace(" ", "").replace("_", " ").equals(t_p.tableData[i][1])) {

											t_p.tableData[i][0] = true;

										}

									} catch (final Exception e2) {

										e2.printStackTrace();

									}

								}

							}

						} catch (final IOException e1) {

							e1.printStackTrace();

						}

						break;

					}

				}

			}

		}

	}

	protected static final class BrowseListener implements ActionListener {

		protected final Texture_Patcher t_p;

		protected BrowseListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			final JFileChooser fileChooser = new JFileChooser();

			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setFileFilter(new ZipFileFilter());

			if (fileChooser.showOpenDialog(t_p.frame) != JFileChooser.APPROVE_OPTION) return;

			final File file = fileChooser.getSelectedFile();

			t_p.path.setText(file.getAbsolutePath());

			t_p.checkUpdate.setEnabled(true);
			t_p.patch.setEnabled(true);

			t_p.prefsnode.put("path", file.getAbsolutePath());

		}

		protected final class ZipFileFilter extends FileFilter {

			@Override public boolean accept (final File f) {

				return f.isDirectory() || f.getName().endsWith(".zip");

			}

			@Override public String getDescription() {

				return "Texture Pack Archive (*.zip)";

			}

		}

	}

	protected static final class CheckUpdateListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;

		protected long time;

		protected File TEMP_C;

		protected CheckUpdateListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			new Thread(this).start();

		}

		@Override public void run () {

			time = System.currentTimeMillis();

			TEMP_C = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_C_" + time);

			extractTexturepack();

			checkUpdate();

			delete(TEMP_C);

		}

		protected String getTMP () {

			final String OS = System.getProperty("os.name").toUpperCase();

			if (OS.contains("WIN")) return System.getenv("TMP");

			else if (OS.contains("MAC") || OS.contains("DARWIN")) return System.getProperty("user.home") + "/Library/Caches/";
			else if (OS.contains("NUX")) return System.getProperty("user.home");

			return System.getProperty("user.dir");

		}

		protected void extractTexturepack () {

			delete(TEMP_C);

			TEMP_C.delete();
			TEMP_C.deleteOnExit();

			TEMP_C.mkdirs();

			try {

				final ZipInputStream zipin = new ZipInputStream(new FileInputStream(new File(t_p.path.getText())));

				ZipEntry zipEntry;

				final byte[] buffer = new byte[1024 * 1024];

				while ((zipEntry = zipin.getNextEntry()) != null) {

					final String fileName = zipEntry.getName();
					final File destinationFile = new File(TEMP_C.getAbsolutePath() + File.separator + fileName);

					if (!destinationFile.getName().equals("modslist.csv")) continue;

					if (zipEntry.isDirectory()) {

						new File(destinationFile.getParent()).mkdirs();

					} else {

						try {

							System.out.println("Extracting: " + destinationFile.getAbsolutePath());

							new File(destinationFile.getParent()).mkdirs();

							final FileOutputStream out = new FileOutputStream(destinationFile);

							int length;

							while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

								out.write(buffer, 0, length);

							}

							out.close();

						} catch (final Exception e) {

							e.printStackTrace();

						}

					}
				}

				zipin.close();

			} catch (final IOException e) {

				e.printStackTrace();

			}

		}

		protected void checkUpdate () {

			final File modslist = new File(TEMP_C, "modslist.csv");

			if (modslist.exists()) {

				try {

					final ArrayList<String> updates = new ArrayList<String>();

					final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(modslist)));

					String readline;

					while ((readline = in.readLine()) != null) {

						for (final Object[] row : t_p.tableData) {

							if (readline.split(",")[0].equals(row[1])) {

								final long olddate = new SimpleDateFormat("MM/dd/yyyy").parse(readline.split(",")[1]).getTime();
								final long newdate = new SimpleDateFormat("MM/dd/yyyy").parse((String) row[5]).getTime();

								if (olddate < newdate) updates.add((String) row[1]);

							}

						}

					}

					in.close();

					if (updates.isEmpty()) JOptionPane.showMessageDialog(t_p.frame, "No updates are avaible for the mods you have already patched!", "No updates!", JOptionPane.INFORMATION_MESSAGE);

					else {

						final int option = JOptionPane.showConfirmDialog(t_p.frame, "Updates are available!" + "\r\n" + "Do you want to have them selected on the table?", "Updates!", JOptionPane.YES_NO_OPTION);

						if (option == JOptionPane.YES_OPTION) {

							selectUpdated(updates);

						}

					}

				} catch (final Exception e) {

					e.printStackTrace();

				}

			} else {

				JOptionPane.showMessageDialog(t_p.frame, "This texture-pack has never been patched before!", "Warning!", JOptionPane.WARNING_MESSAGE);

			}

		}

		protected void selectUpdated (final ArrayList<String> updates) {

			for (final String mod : updates) {

				for (final Object[] row : t_p.tableData) {

					if (mod.equals(row[1])) row[0] = true;

				}

			}

			t_p.table.updateUI();

		}

		protected void delete (final File f) {

			f.delete();

			if (f.isFile()) return;

			final File[] files = f.getAbsoluteFile().listFiles();

			if (files == null) return;

			for (final File file : files) {

				delete(file);

				f.delete();

			}

		}

	}

	protected static final class PatchListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;
		protected ProgressDialog progressdialog;

		protected long time;

		protected File TEMP_A;
		protected File TEMP_B;

		protected PatchListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		protected String getTMP () {

			final String OS = System.getProperty("os.name").toUpperCase();

			if (OS.contains("WIN")) return System.getenv("TMP");

			else if (OS.contains("MAC") || OS.contains("DARWIN")) return System.getProperty("user.home") + "/Library/Caches/";
			else if (OS.contains("NUX")) return System.getProperty("user.home");

			return System.getProperty("user.dir");

		}

		@Override public void actionPerformed (final ActionEvent e) {

			new Thread(this).start();

		}

		@Override public void run () {

			time = System.currentTimeMillis();

			TEMP_A = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_A_" + time);
			TEMP_B = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_B_" + time);

			progressdialog = new ProgressDialog();

			progressdialog.setString("Extracting texture pack file (--/--)");
			progressdialog.setProgressValue(0);

			progressdialog.open();

			extractTexturepack();

			progressdialog.setString("Compiling mods list...");
			progressdialog.setProgressValue(25);

			compileModsList();

			progressdialog.setString("Downloading  mod (--/--)");
			progressdialog.setProgressValue(25);

			downloadMods();

			progressdialog.setString("Extracting  mod (--/--)");
			progressdialog.setProgressValue(50);

			extractMods();

			progressdialog.setString("Compressing texture pack file (--/--)");
			progressdialog.setProgressValue(75);

			compileTexturepack();

			progressdialog.setString("Done!");
			progressdialog.setProgressValue(100);

			delay(2500);

			progressdialog.close();

			t_p.frame.requestFocus();

		}

		protected void delay (final long time) {

			try {

				TimeUnit.MILLISECONDS.sleep(time);

			} catch (final Exception e) {

				e.printStackTrace();

			}

		}

		protected void extractTexturepack () {

			delete(TEMP_A);

			TEMP_A.delete();
			TEMP_A.deleteOnExit();

			TEMP_A.mkdirs();

			try {

				int count = 0;

				final ZipFile zipfile = new ZipFile(new File(t_p.path.getText()));

				final int progressamount = zipfile.size();
				int progresscount = 0;

				zipfile.close();

				final ZipInputStream zipin = new ZipInputStream(new FileInputStream(new File(t_p.path.getText())));

				ZipEntry zipEntry;

				final byte[] buffer = new byte[1024 * 1024];

				while ((zipEntry = zipin.getNextEntry()) != null) {

					final String fileName = zipEntry.getName();
					final File destinationFile = new File(TEMP_A.getAbsolutePath() + File.separator + fileName);

					progressdialog.setString("Extracting texture pack file (" + ++count + "/" + progressamount + ")");

					if (zipEntry.isDirectory()) {

						new File(destinationFile.getParent()).mkdirs();

					} else {

						try {

							System.out.println("Extracting: " + destinationFile.getAbsolutePath());

							new File(destinationFile.getParent()).mkdirs();

							final FileOutputStream out = new FileOutputStream(destinationFile);

							int length;

							while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

								out.write(buffer, 0, length);

							}

							out.close();

						} catch (final Exception e) {

							e.printStackTrace();

						}

					}

					if (++progresscount >= progressamount / 25) {

						progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

						progresscount = 0;

					}

				}

				zipin.close();

			} catch (final IOException e) {

				e.printStackTrace();

			}

		}

		protected void compileModsList () {

			final File modslist = new File(TEMP_A, "modslist.csv");

			String modslistcontents = "";

			if (modslist.exists()) {

				try {

					final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(modslist)));

					String readline;

					reading: while ((readline = in.readLine()) != null) {

						rowing: for (final Object[] row : t_p.tableData) {

							if ((Boolean) row[0] == false) continue rowing;

							if (readline.split(",")[0].equals(row[1])) {

								continue reading;

							}

						}

					modslistcontents = modslistcontents.concat(readline + getLineSeparator());

					}

					in.close();

				} catch (final IOException e) {

					e.printStackTrace();

				}

			}

			for (final Object[] row : t_p.tableData) {

				if ((Boolean) row[0] == false) continue;

				modslistcontents = modslistcontents.concat((String) row[1] + "," + (String) row[5] + getLineSeparator());

			}

			modslistcontents = modslistcontents.trim();

			try {

				final PrintWriter out = new PrintWriter(new FileOutputStream(modslist));

				out.print(modslistcontents);

				out.close();

			} catch (final IOException e) {

				e.printStackTrace();

			}

		}

		protected void downloadMods () {

			delete(TEMP_B);

			TEMP_B.delete();
			TEMP_B.deleteOnExit();

			TEMP_B.mkdirs();

			final byte[] buffer = new byte[1024 * 1024];

			final ArrayList<String> modslist = new ArrayList<String>();

			for (final Object[] element : t_p.tableData) {

				if (element[0] != null && (Boolean) element[0]){

					modslist.add((String) element[1]);

				}

			}

			int count = 0;

			final int progressamount = modslist.size() / 25;
			int progresscount = 0;

			for (final String mod : modslist) {

				try {

					final String modurl = t_p.config.getProperty("zipsurl") + mod.replace(" ", "_") + ".zip";

					InputStream in;

					try {

						in = new URL(modurl).openStream();

					} catch (final IOException e) {

						e.printStackTrace();

						in = new URL(modurl).openStream();

					}

					System.out.println("Downloading: " + mod);

					progressdialog.setString("Downloading mod (" + ++count + "/" + modslist.size() + ")");

					final File destinationFile = new File(TEMP_B.getAbsolutePath() + File.separator + new File(modurl).getName()).getAbsoluteFile();

					destinationFile.getParentFile().mkdirs();

					final FileOutputStream out = new FileOutputStream(destinationFile);

					int length;

					while ((length = in.read(buffer, 0, buffer.length)) > -1) {

						out.write(buffer, 0, length);

					}

					out.close();

				} catch (final Exception e) {

					e.printStackTrace();

				}

				if (++progresscount >= progressamount) {

					progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

					progresscount = 0;

				}

			}

		}

		protected void extractMods () {

			final ArrayList<File> files = new ArrayList<File>();

			getFiles(TEMP_B, files);

			int count = 0;

			final int progressamount = files.size() / 25;
			int progresscount = 0;

			for (final File file : files) {

				System.out.println("Extracting: " + file.getName());

				progressdialog.setString("Extracting mod (" + ++count + "/" + files.size() + ")");

				try {

					final ZipInputStream zipin = new ZipInputStream(new FileInputStream(file));

					ZipEntry zipEntry;

					final byte[] buffer = new byte[1024 * 1024];

					while ((zipEntry = zipin.getNextEntry()) != null) {

						final String fileName = zipEntry.getName();
						final File destinationFile = new File(TEMP_A.getAbsolutePath() + File.separator + fileName);

						if (zipEntry.isDirectory()) {

							new File(destinationFile.getParent()).mkdirs();

						} else {

							try {

								new File(destinationFile.getParent()).mkdirs();

								final FileOutputStream out = new FileOutputStream(destinationFile);

								int length;

								while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

									out.write(buffer, 0, length);

								}

								out.close();

							} catch (final Exception e) {

								e.printStackTrace();

							}

						}

					}

					zipin.close();

				} catch (final IOException e) {

					e.printStackTrace();

				}

				if (++progresscount >= progressamount) {

					progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

					progresscount = 0;

				}

			}

			delete(TEMP_B);

		}

		protected void compileTexturepack () {

			try {

				final FileOutputStream out = new FileOutputStream(new File(t_p.path.getText()));
				final ZipOutputStream zipout = new ZipOutputStream(out);

				System.out.println("Compiling zip: " + new File(t_p.path.getText()).getAbsolutePath());

				final ArrayList<File> files = new ArrayList<File>();

				getFiles(TEMP_A, files);

				final byte[] buffer = new byte[1024 * 1024];

				int count = 0;

				final int progressamount = files.size() / 25;
				int progresscount = 0;

				for (final File file : files) {

					final String temp = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(TEMP_A.getName()), file.getAbsolutePath().length());
					final String zipentrypath = temp.substring(temp.indexOf(File.separator) + 1, temp.length());

					System.out.println("Compressing: " + zipentrypath);

					progressdialog.setString("Compressing texture pack file (" + ++count + "/" + files.size() + ")");

					final ZipEntry zipentry = new ZipEntry(zipentrypath.replace("\\", "/"));

					zipout.putNextEntry(zipentry);

					final FileInputStream in = new FileInputStream(file);

					int length;

					while ((length = in.read(buffer, 0, buffer.length)) > -1) {

						zipout.write(buffer, 0, length);

					}

					in.close();

					zipout.closeEntry();

					if (++progresscount >= progressamount) {

						progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

						progresscount = 0;

					}

				}

				zipout.close();
				out.close();

			} catch (final IOException e) {

				e.printStackTrace();

			}

			delete(TEMP_A);

		}

		protected String getLineSeparator () {

			final String OS = System.getProperty("os.name").toUpperCase();

			if (OS.contains("WIN")) return "\r\n";

			else return "\n";

		}

		protected void getFiles (final File f, final ArrayList<File> files) {

			if (f.isFile()) return;

			final File[] afiles = f.getAbsoluteFile().listFiles();

			if (afiles == null) return;

			for (final File file : afiles) {

				if (file.isDirectory()) getFiles(file, files);

				else files.add(file.getAbsoluteFile());

			}

		}

		protected void delete (final File f) {

			f.delete();

			if (f.isFile()) return;

			final File[] files = f.getAbsoluteFile().listFiles();

			if (files == null) return;

			for (final File file : files) {

				delete(file);

				f.delete();

			}

		}

		protected final class ProgressDialog {

			protected static final long serialVersionUID = 1L;

			protected final JFrame frame;
			protected final JProgressBar progress;
			protected final JLabel status;

			protected ProgressDialog () {

				frame = new JFrame("Patching...");
				frame.setLayout(new GridBagLayout());
				frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				frame.setIconImage(t_p.frame.getIconImage());

				final Insets insets = new Insets(2, 2, 1, 2);

				GridBagConstraints gbc = new GridBagConstraints();

				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				gbc.weighty = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.anchor = GridBagConstraints.NORTH;
				gbc.insets = insets;

				progress = new JProgressBar();
				progress.setStringPainted(true);

				frame.add(progress, gbc);

				gbc = new GridBagConstraints();

				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				gbc.weighty = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.anchor = GridBagConstraints.NORTH;
				gbc.insets = insets;

				status = new JLabel("", SwingConstants.CENTER);
				frame.add(status, gbc);

				frame.setSize(250, 75);
				frame.setResizable(false);

				frame.setLocationRelativeTo(t_p.frame);

			}

			protected void open () {

				t_p.frame.setEnabled(false);

				frame.setVisible(true);

			}

			protected void close () {

				frame.dispose();

				t_p.frame.setEnabled(true);

			}

			protected void setProgressValue (final int value) {

				progress.setValue(value);

			}

			protected int getProgressValue () {

				return progress.getValue();

			}

			protected void setString (final String value) {

				status.setText(value);

			}

		}

	}

	protected static final class ExitListener implements WindowListener {

		protected final Texture_Patcher t_p;

		protected ExitListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void windowActivated (final WindowEvent arg0) {}

		@Override public void windowClosed (final WindowEvent arg0) {}

		@Override public void windowClosing (final WindowEvent e) {

			t_p.loadingFrame.setVisible(false);
			t_p.loadingFrame.dispose();

			t_p.frame.setVisible(false);
			t_p.frame.dispose();

			t_p.stopped = true;

		}

		@Override public void windowDeactivated (final WindowEvent arg0) {}

		@Override public void windowDeiconified (final WindowEvent arg0) {}

		@Override public void windowIconified (final WindowEvent arg0) {}

		@Override public void windowOpened (final WindowEvent arg0) {}

	}

}
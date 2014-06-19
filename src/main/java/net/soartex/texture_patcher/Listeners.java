package net.soartex.texture_patcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class Listeners {

	// Listener classes.

	protected static final class TableListener implements MouseListener {

		protected final Texture_Patcher t_p;

		protected TableListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void mouseClicked (final MouseEvent e) {

			// Calculate the column and row on the table.

			final int row = t_p.table.rowAtPoint(e.getPoint());
			final int column = t_p.table.columnAtPoint(e.getPoint());

			// If the column is the checkbox column, toggle the boolean value.

			if (column == 0) {

				t_p.table.setValueAt(!((Boolean) t_p.table.getValueAt(row, column)), row, column);
				t_p.table.updateUI();

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

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			try {

				// Navigate to the URL specified by the configuration.

				Desktop.getDesktop().browse(new URI((String) t_p.options.get("url")));

			} catch (final Exception e1) {

				// Happens if unable to open the URL.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.WEBSITE_OPENING_FAILED, e1);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

	}

	protected static final class ModpackListener implements ActionListener {

		protected final Texture_Patcher t_p;

		protected ModpackListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			try {

				if (e.getActionCommand().equals("Select All")) {

					// Select all of the mods.

					for (int i = 0; i < t_p.tableData.length; i++) {

						t_p.tableData[i][0] = true;

					}

					t_p.table.updateUI();

				} else if (e.getActionCommand().equals("Select None")) {

					// Deselect all of the mods.

					for (int i = 0; i < t_p.tableData.length; i++){

						t_p.tableData[i][0]= false;

					}

					t_p.table.updateUI();

				} else {

					// Check mods based on the selected modpack.

					for (final Object modpack : t_p.modpacks.keySet()){

						if (e.getActionCommand().equals(modpack)) {

							final BufferedReader in = new BufferedReader(new InputStreamReader(new URL((String) t_p.modpacks.get(modpack)).openStream()));

							for (int i = 0; i < t_p.tableData.length; i++) {

								t_p.tableData[i][0]= false;

							}

							t_p.table.updateUI();

							String readline;

							while ((readline = in.readLine()) != null) {

								for (int i = 0; i < t_p.tableData.length; i++) {

									if (readline.replace("_", " ").equals(t_p.tableData[i][1])) {

										t_p.tableData[i][0] = true;

									}

								}

							}

							break;

						}

					}

				}

			} catch (final Exception e1) {

				// Happens if unable to load modpack file.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.MODPACK_SELECTION_FAILED, e1);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

	}

	protected static final class BrowseListener implements ActionListener {

		protected final Texture_Patcher t_p;

		protected BrowseListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			try {

				// Initialize the file chooser.

				final JFileChooser fileChooser = new JFileChooser();

				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileFilter(new ZipFileFilter());

				// Resolve the stored directory and files.

				final File lastDir = new File(t_p.prefsnode.get("lastDir", System.getProperty("user.dir")));

				if (lastDir.exists()) {

					fileChooser.setCurrentDirectory(lastDir);

				} else {

					fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

					t_p.path.setText("");

					t_p.checkUpdate.setEnabled(false);
					t_p.patch.setEnabled(false);

					t_p.prefsnode.remove("path");
					t_p.prefsnode.remove("lastDir");

				}

				if (fileChooser.showOpenDialog(t_p.frame) != JFileChooser.APPROVE_OPTION) return;

				// Save the selected file.

				final File file = fileChooser.getSelectedFile();

				t_p.path.setText(file.getAbsolutePath());

				t_p.checkUpdate.setEnabled(true);
				t_p.patch.setEnabled(true);

				t_p.prefsnode.put("path", file.getAbsolutePath());
				t_p.prefsnode.put("lastDir", file.getParent());
				
				// Clear cell highlighting
				try {
					TableRender tableRender = new TableRender(new ArrayList<Integer>(), new ArrayList<Integer>(), Color.red, Color.yellow);
					t_p.table.getColumnModel().getColumn(1).setCellRenderer(tableRender);
					t_p.table.updateUI();
				} catch (Exception e2){}

			} catch (final Exception e1) {

				// Happens if an error occurs while opening the browse dialog.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.BROWSING_FAILED, e1);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

	}

	protected static final class DownloadPackListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;

		protected ProgressDialog progressdialog;

		protected File file;

		protected DownloadPackListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			try {

				// Check if the texture artist has provided a pack URL.

				if (t_p.options.get("packurl") == null) {

					JOptionPane.showMessageDialog(t_p.frame, "The texture-artist has not provided a URL for a pack to download!", "Error!", JOptionPane.ERROR_MESSAGE);

					return;

				}

				// Open the browse dialog, and check if a file has been selected.

				if (!openBrowseDialog()) return;

				// Start the downloading process.

				new Thread(this).start();

			} catch (final Exception e1) {

				// Happens if an error occurs while downloading the pack.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.PACK_DOWNLOADING_FAILED, e1);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

		protected boolean openBrowseDialog () {

			// Initialize the file chooser.

			final JFileChooser fileChooser = new JFileChooser();

			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setFileFilter(new ZipFileFilter());

			// Resolve the stored directory and files.

			final File lastDir = new File(t_p.prefsnode.get("lastDir", System.getProperty("user.dir")));

			if (lastDir.exists()) {

				fileChooser.setCurrentDirectory(lastDir);

			} else {

				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

				t_p.prefsnode.remove("lastDir");

			}

			// Loop for overwrite dialog.

			while (true) {

				if (fileChooser.showSaveDialog(t_p.frame) != JFileChooser.APPROVE_OPTION) return false;

				if (fileChooser.getSelectedFile().exists()) {

					final int option = JOptionPane.showConfirmDialog(t_p.frame, "This file you selected already exists!\r\nDo you wish to overwrite?", "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

					if (option == JOptionPane.NO_OPTION) {

						continue;

					} else if (option == JOptionPane.CANCEL_OPTION) {

						return false;

					} else {

						break;

					}

				} else {

					break;

				}

			}

			// Adds .zip to the filename if it isn't present.

			if (fileChooser.getSelectedFile().getAbsolutePath().endsWith(".zip")) {

				file = fileChooser.getSelectedFile().getAbsoluteFile();

			} else {

				file = new File(fileChooser.getSelectedFile().getParent(), fileChooser.getSelectedFile().getName() + ".zip");

			}

			return true;

		}

		@Override public void run () {

			try {

				// Initialize the progress dialog.

				progressdialog = new ProgressDialog(t_p);

				progressdialog.setProgressValue(0);
				progressdialog.setString("Downloading texture-pack...");

				progressdialog.open();

				// Download the pack.

				final URL packurl = new URL((String) t_p.options.get("packurl"));

				float size = 0;

				final byte[] buffer = new byte[1024];

				InputStream in;

				try {

					final URLConnection connection = packurl.openConnection();

					in = connection.getInputStream();

					size = connection.getContentLength();

				} catch (final IOException e) {

					// Retrying in the case of a 502.

					t_p.logger.log(Level.WARNING, "IOException while downloading the texture-pack, trying again!");

					final URLConnection connection = packurl.openConnection();

					in = connection.getInputStream();

				}

				t_p.logger.log(Level.INFO, "Downloading: " + t_p.options.get("packurl") + ".");

				final float progressamount = size / 102400;
				float progresscount = 0;

				file.getParentFile().mkdirs();

				final FileOutputStream out = new FileOutputStream(file);

				int length;

				while ((length = in.read(buffer, 0, buffer.length)) > -1) {

					out.write(buffer, 0, length);

					if (++progresscount >= progressamount) {

						progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

						progresscount = 0;

					}

				}

				out.close();

				// Close the progress dialog.

				progressdialog.setString("Done!");
				progressdialog.setProgressValue(100);

				delay(1500);

				progressdialog.close();

				// Update the path field, buttons, and stored paths.

				t_p.path.setText(file.getAbsolutePath());

				t_p.checkUpdate.setEnabled(true);
				t_p.patch.setEnabled(true);

				t_p.prefsnode.put("path", file.getAbsolutePath());
				t_p.prefsnode.put("lastDir", file.getParent());

				t_p.frame.requestFocus();

			} catch (final Exception e1) {

				// Happens if an error occurs while downloading the pack.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.PACK_DOWNLOADING_FAILED, e1);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

	}

	protected static final class CheckUpdateListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;

		protected long time;

		protected File TEMP_C;

		protected CheckUpdateListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			// Start the update checking process.

			new Thread(this).start();

		}

		@Override public void run () {

			try {

				// Create the temporary folder.

				time = System.currentTimeMillis();

				TEMP_C = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_C_" + time);

				// Extract the texture-pack.

				extractTexturepack();

				// Checks the modslist.csv file for updates.

				checkUpdate();

				// Deletes the temporary folder.

				delete(TEMP_C);

			} catch (final Exception e) {

				// Happens if an error occurs while checking the texture-pack for updates.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.PACK_DOWNLOADING_FAILED, e);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			}

		}

		protected void extractTexturepack () throws IOException {

			// Make sure the temporary folder is ready.

			delete(TEMP_C);

			TEMP_C.delete();
			TEMP_C.deleteOnExit();

			TEMP_C.mkdirs();

			// Extract the texture-pack.

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

					t_p.logger.log(Level.INFO, "Extracting: " + destinationFile.getAbsolutePath());

					new File(destinationFile.getParent()).mkdirs();

					final FileOutputStream out = new FileOutputStream(destinationFile);

					int length;

					while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

						out.write(buffer, 0, length);

					}

					out.close();

				}
			}

			zipin.close();

		}

		protected void checkUpdate () throws IOException, ParseException {

			final File modslist = new File(TEMP_C, "modslist.csv");

			if (modslist.exists()) {

				final ArrayList<String> updates = new ArrayList<String>();
				
				final ArrayList<Integer> rows1 = new ArrayList<Integer>();
				
				final ArrayList<Integer> rows2 = new ArrayList<Integer>();

				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(modslist)));

				String readline;

				while ((readline = in.readLine()) != null) {

					int counter = 0;
					
					for (final Object[] row : t_p.tableData) {

						if (readline.split(",")[0].equals(row[1])) {

							long olddate = Long.MAX_VALUE;

							try {

								olddate = Long.parseLong(readline.split(",")[1]);

							} catch (final Exception e) {

								olddate = new SimpleDateFormat("MM/dd/yyyy").parse(readline.split(",")[1]).getTime();

							}

							final long newdate = ((Date) row[5]).getTime();

							// Add if it needs to be updated
							if (olddate < newdate) {
								updates.add((String) row[1]);
								rows1.add(counter);
							}
							// Add to generic list of what is installed
							else {
								rows2.add(counter);
							}

						}

						counter++;

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
				
				// Display highlighting boxes
				try {
					TableRender tableRender = new TableRender(rows1, rows2, Color.red, Color.yellow);
					t_p.table.getColumnModel().getColumn(1).setCellRenderer(tableRender);
					t_p.table.updateUI();
				} catch (Exception e2){}

			} else {

				JOptionPane.showMessageDialog(t_p.frame, "This texture-pack has never been patched before!", "Never patched!", JOptionPane.INFORMATION_MESSAGE);

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

	}

	protected static final class PatchListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;
		protected ProgressDialog progressdialog;

		protected long time;

		protected File TEMP_A;
		protected File TEMP_B;

		protected PatchListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void actionPerformed (final ActionEvent e) {

			// Start the patching process.

			new Thread(this).start();

		}

		@Override public void run () {

			try {

				// Resolve the temporary folders.

				time = System.currentTimeMillis();

				TEMP_A = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_A_" + time);
				TEMP_B = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_B_" + time);

				// Initialize the progress dialog.

				progressdialog = new ProgressDialog(t_p);

				progressdialog.setString("Extracting texture pack file (--/--)");
				progressdialog.setProgressValue(0);

				progressdialog.open();

				// Extract the texture pack.

				extractTexturepack();

				progressdialog.setString("Compiling mods list...");
				progressdialog.setProgressValue(25);

				// Compile the mods list.

				compileModsList();

				progressdialog.setString("Downloading  mod (--/--)");
				progressdialog.setProgressValue(25);

				// Download the mods.

				downloadMods();

				progressdialog.setString("Extracting  mod (--/--)");
				progressdialog.setProgressValue(50);

				// Extract the mod.

				extractMods();

				progressdialog.setString("Compressing texture pack file (--/--)");
				progressdialog.setProgressValue(75);

				// Compile the texture pack.

				compileTexturepack();

				// Return to normal.

				progressdialog.setString("Done!");
				progressdialog.setProgressValue(100);

				delay(1500);

				progressdialog.close();

				t_p.frame.requestFocus();

			} catch (final Exception e) {

				// Happens if an error occurs while patching.

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.PATCHING_FAILED, e);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

				t_p_e.printStackTrace();

				t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

				progressdialog.close();

			}

		}

		protected void extractTexturepack () throws IOException {

			// Make sure the temporary folder is ready.

			delete(TEMP_A);

			TEMP_A.delete();
			TEMP_A.deleteOnExit();

			TEMP_A.mkdirs();

			// Calculate progress percents.

			int progressamount = 0;
			int progresscount = 0;

			try {

				final ZipFile zipfile = new ZipFile(new File(t_p.path.getText()));

				if (zipfile.size() == 0) return;

				progressamount = zipfile.size();

				zipfile.close();

			} catch (final Exception e) {

				t_p.logger.log(Level.WARNING, "Error opening zip file! Could it be emtpy?");

				e.printStackTrace();

			}

			int count = 0;

			// Extract the texture pack.

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

					t_p.logger.log(Level.INFO, "Extracting: " + destinationFile.getAbsolutePath());

					new File(destinationFile.getParent()).mkdirs();

					final FileOutputStream out = new FileOutputStream(destinationFile);

					int length;

					while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

						out.write(buffer, 0, length);

					}

					out.close();

				}

				if (++progresscount >= progressamount / 25) {

					progressdialog.setProgressValue(progressdialog.getProgressValue() + 1);

					progresscount = 0;

				}


			}

			zipin.close();

		}

		protected void compileModsList () throws IOException {

			// Get the modslist file ready.

			final File modslist = new File(TEMP_A, "modslist.csv");

			final ArrayList<String> mods = new ArrayList<String>();

			if (modslist.exists()) {

				// Merge the modslist if it already exists.

				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(modslist)));

				String readline;

				reading: while ((readline = in.readLine()) != null) {

					rowing: for (final Object[] row : t_p.tableData) {

						if ((Boolean) row[0] == false) continue rowing;

						if (readline.split(",")[0].equals(row[1])) {

							continue reading;

						}

					}

				mods.add(readline);

				}

				in.close();

			}

			// Create the new modslist.

			for (final Object[] row : t_p.tableData) {

				if ((Boolean) row[0] == false) continue;

				mods.add((String) row[1] + "," + ((Date) row[5]).getTime());

			}

			final PrintWriter out = new PrintWriter(new FileWriter(modslist));

			for (final String mod : mods) {

				out.println(mod);

			}

			out.close();

		}

		protected void downloadMods () {

			// Make sure the temporary folder is ready.

			delete(TEMP_B);

			TEMP_B.delete();
			TEMP_B.deleteOnExit();

			TEMP_B.mkdirs();

			// Download the mods.

			final byte[] buffer = new byte[1024 * 1024];

			final ArrayList<String> modslist = new ArrayList<String>();

			for (final Object[] element : t_p.tableData) {

				if (element[0] != null && (Boolean) element[0]){

					modslist.add((String) element[1]);

				}

			}

			int count = 0;

			for (final String mod : modslist) {

				try {

					final String modurl = t_p.options.get("zipsurl") + mod.replace(" ", "_") + ".zip";

					InputStream in;

					try {

						in = new URL(modurl).openStream();

					} catch (final IOException e) {

						// Retrying in the case of a 502.

						t_p.logger.log(Level.WARNING, "IOException while downloading the mod, trying again!");

						e.printStackTrace();

						in = new URL(modurl).openStream();

					}

					t_p.logger.log(Level.INFO, "Downloading mod: " + mod);

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

					t_p.logger.log(Level.WARNING, "Unable to download mod " + mod + ".");

					e.printStackTrace();

				}

			}

		}

		protected void extractMods () {

			// Extract the mods.

			final ArrayList<File> files = new ArrayList<File>();

			getFiles(TEMP_B, files);

			int count = 0;

			for (final File file : files) {

				t_p.logger.log(Level.INFO, "Extracting mod: " + file.getName());

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

							new File(destinationFile.getParent()).mkdirs();

							final FileOutputStream out = new FileOutputStream(destinationFile);

							int length;

							while ((length = zipin.read(buffer, 0, buffer.length)) > -1) {

								out.write(buffer, 0, length);

							}

							out.close();

						}

					}

					zipin.close();

				} catch (final IOException e) {

					t_p.logger.log(Level.WARNING, "Unable to extract " + file.getAbsolutePath() + " .");

					e.printStackTrace();

				}

			}

			delete(TEMP_B);

		}

		protected void compileTexturepack () throws IOException {

			// Compile the texture pack.

			final FileOutputStream out = new FileOutputStream(new File(t_p.path.getText()));
			final ZipOutputStream zipout = new ZipOutputStream(out);

			t_p.logger.log(Level.INFO, "Compiling texture pack: " + new File(t_p.path.getText()).getAbsolutePath());

			final ArrayList<File> files = new ArrayList<File>();

			getFiles(TEMP_A, files);

			final byte[] buffer = new byte[1024 * 1024];

			int count = 0;

			final int progressamount = files.size() / 25;
			int progresscount = 0;

			for (final File file : files) {

				final String temp = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(TEMP_A.getName()), file.getAbsolutePath().length());
				final String zipentrypath = temp.substring(temp.indexOf(File.separator) + 1, temp.length());

				t_p.logger.log(Level.INFO, "Compressing: " + zipentrypath);

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

			delete(TEMP_A);

		}

	}

	protected static final class ExitListener implements WindowListener {

		protected final Texture_Patcher t_p;

		protected ExitListener (final Texture_Patcher t_p) {

			// Receive the texture patcher instance for the listener.

			this.t_p = t_p;

		}

		@Override public void windowActivated (final WindowEvent arg0) {}

		@Override public void windowClosed (final WindowEvent arg0) {}

		@Override public void windowClosing (final WindowEvent e) {

			// Save the window's size and location.

			if (t_p.frame.isVisible()) {

				t_p.prefsnode.putInt("max", t_p.frame.getExtendedState());

				if (t_p.frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {

					t_p.prefsnode.putInt("x", t_p.frame.getX());
					t_p.prefsnode.putInt("y", t_p.frame.getY());

					t_p.prefsnode.putInt("width", t_p.frame.getWidth());
					t_p.prefsnode.putInt("height", t_p.frame.getHeight());

				}

			}

			// Dispose of the windows.

			if (!t_p.loadingFrame.isVisible()) {

				final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(t_p, ErrorType.WINDOW_CLOSED, null);

				t_p.logger.log(Level.SEVERE, t_p_e.getMessage());

			}

			t_p.loadingFrame.setVisible(false);
			t_p.loadingFrame.dispose();

			t_p.frame.setVisible(false);
			t_p.frame.dispose();

		}

		@Override public void windowDeactivated (final WindowEvent arg0) {}

		@Override public void windowDeiconified (final WindowEvent arg0) {}

		@Override public void windowIconified (final WindowEvent arg0) {}

		@Override public void windowOpened (final WindowEvent arg0) {}

	}

	// Shared classes.

	protected static final class ZipFileFilter extends FileFilter {

		@Override public boolean accept (final File f) {

			return f.isDirectory() || f.getName().endsWith(".zip");

		}

		@Override public String getDescription() {

			return "Texture Pack Archive (*.zip)";

		}

	}

	protected static final class ProgressDialog {

		protected final Texture_Patcher t_p;

		protected static final long serialVersionUID = 1L;

		protected final JFrame frame;
		protected final JProgressBar progress;
		protected final JLabel status;

		protected ProgressDialog (final Texture_Patcher t_p) {

			this.t_p = t_p;

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
			t_p.frame.requestFocus();

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

	// Shared methods.

	protected static String getTMP () {

		// Resolve the system's temporary directory.

		final String OS = System.getProperty("os.name").toUpperCase();

		if (OS.contains("WIN")) return System.getenv("TMP");

		else if (OS.contains("MAC") || OS.contains("DARWIN")) return System.getProperty("user.home") + "/Library/Caches/";
		else if (OS.contains("NUX")) return System.getProperty("user.home");

		return System.getProperty("user.dir");

	}

	protected static void delay (final long time) {

		// Create a thread delay for the given time.

		try {

			TimeUnit.MILLISECONDS.sleep(time);

		} catch (final Exception e) {

			e.printStackTrace();

		}

	}

	protected static void getFiles (final File f, final ArrayList<File> files) {

		// Return all the files in a directory and its subdirectories.

		if (f.isFile()) return;

		final File[] afiles = f.getAbsoluteFile().listFiles();

		if (afiles == null) return;

		for (final File file : afiles) {

			if (file.isDirectory()) getFiles(file, files);

			else files.add(file.getAbsoluteFile());

		}

	}

	protected static void delete (final File f) {

		// Delete a folder and all of its subdirectories.

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
package net.soartex.patcher;

import java.awt.Desktop;

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

import java.net.URI;
import java.net.URL;

import java.util.ArrayList;

import java.util.concurrent.TimeUnit;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.WindowConstants;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

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

	protected static final class WebsiteListener implements MenuListener {

		protected final Texture_Patcher t_p;

		protected WebsiteListener (final Texture_Patcher t_p) {

			this.t_p = t_p;

		}

		@Override public void menuCanceled (final MenuEvent e) {}

		@Override public void menuDeselected (final MenuEvent e) {

			try {

				Desktop.getDesktop().browse(new URI(t_p.config.getProperty("url")));

			} catch (final Exception e1) {

				e1.printStackTrace();

			}

		}

		@Override public void menuSelected (final MenuEvent e) {}

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

										if (readline.equals(t_p.tableData[i][1])) {

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

			t_p.selectedFile = file;

			t_p.patchitem.setEnabled(true);

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

	protected static final class PatchListener implements ActionListener, Runnable {

		protected final Texture_Patcher t_p;

		protected final long time = System.currentTimeMillis();

		protected final File TEMP_A = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_A_" + time);
		protected final File TEMP_B = new File(getTMP() + File.separator + ".Texture_Patcher_Temp_B_" + time);

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

			final ProgressDialog progress = new ProgressDialog();

			progress.setString("Extracting texture pack...");
			progress.setProgressValue(0);

			progress.open();

			extractTexturepack();

			progress.setString("Downloading mods...");
			progress.setProgressValue(25);

			downloadMods();

			progress.setString("Extracting mods...");
			progress.setProgressValue(50);

			extractMods();

			progress.setString("Compiling texture pack...");
			progress.setProgressValue(75);

			compileTexturepack();

			progress.setString("Done!");
			progress.setProgressValue(100);

			try {

				TimeUnit.MILLISECONDS.sleep(2500);

			} catch (final Exception e) {

				e.printStackTrace();

			}

			progress.close();

		}

		protected void extractTexturepack () {

			delete(TEMP_A);

			TEMP_A.delete();
			TEMP_A.deleteOnExit();

			TEMP_A.mkdirs();

			try {

				final ZipInputStream zipin = new ZipInputStream(new FileInputStream(t_p.selectedFile));

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

			for (final String mod : modslist) {

				try {

					final String modurl = t_p.config.getProperty("zipsurl") + mod.replace(" ", "_") + ".zip";

					final InputStream in = new URL(modurl).openStream();

					System.out.println("Downloading: " + mod);

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

			}

		}

		protected void extractMods () {

			final ArrayList<File> files = new ArrayList<File>();

			getFiles(TEMP_B, files);

			for (final File file : files) {

				System.out.println("Extracting: " + file.getName());

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

			}

			delete(TEMP_B);

		}

		protected void compileTexturepack () {

			try {

				final FileOutputStream out = new FileOutputStream(t_p.selectedFile);
				final ZipOutputStream zipout = new ZipOutputStream(out);

				System.out.println("Compiling zip: " + t_p.selectedFile.getAbsolutePath());

				final ArrayList<File> files = new ArrayList<File>();

				getFiles(TEMP_A, files);

				final byte[] buffer = new byte[1024 * 1024];

				for (final File file : files) {

					final String temp = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(TEMP_A.getName()), file.getAbsolutePath().length());
					final String zipentrypath = temp.substring(temp.indexOf(File.separator) + 1, temp.length());

					System.out.println("Compressing: " + zipentrypath);

					final ZipEntry zipentry = new ZipEntry(zipentrypath);

					zipout.putNextEntry(zipentry);

					final FileInputStream in = new FileInputStream(file);

					int length;

					while ((length = in.read(buffer, 0, buffer.length)) > -1) {

						zipout.write(buffer, 0, length);

					}

					in.close();

					zipout.closeEntry();

				}

				zipout.close();
				out.close();

			} catch (final IOException e) {

				e.printStackTrace();

			}

			delete(TEMP_A);

		}

		protected static void getFiles (final File f, final ArrayList<File> files) {

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

			protected JFrame frame;
			protected JProgressBar progress;

			protected ProgressDialog () {

				frame = new JFrame("Patching...");
				frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				frame.setIconImage(t_p.frame.getIconImage());

				progress = new JProgressBar();
				progress.setStringPainted(true);

				frame.add(progress);

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

			protected void setString (final String value) {

				progress.setString(value);

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
package net.soartex.texture_patcher;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Texture_Patcher main class.
 * 
 * @author REDX36
 * @version 1.1
 *
 */
public final class Texture_Patcher implements Runnable {

	// Program variables.

	protected final static float VERSION = 1.1F;
	protected static boolean debug = false;

	protected final Preferences prefsnode = Preferences.userNodeForPackage(getClass());
	protected final Logger logger = Logger.getLogger(getClass().getName() + "." + System.currentTimeMillis());

	protected JSONObject config;
	protected JSONObject options;
	protected JSONObject mods;
	protected JSONObject modpacks;

	protected Object[][] tableData;

	// Swing objects.

	protected JFrame frame;
	protected JFrame loadingFrame;

	protected JTextField path;
	protected JButton checkUpdate;
	protected JButton patch;
	protected JTable table;

	// Public methods

	/**
	 * Sets certain Mac OSX Cocoa system flags, checks arguments for debug mode, and runs the patcher in a separate thread.
	 * 
	 * @param args If <code>args</code> is longer than 0, and <code>args[0]</code> evaluates to <code>true</code>,
	 * 
	 */
	public static void main (final String[] args) {

		// Set certain properties specific to Mac OSX Cocoa.

		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Texture-Patcher v." + VERSION);

		// Check if the patcher is being run in debug mode.

		debug = Boolean.parseBoolean(args.length > 0 ? args[0] : "");

		// Start the patcher in its own thread.

		new Thread(new Texture_Patcher()).start();

	}

	@Override public void run () {

		try {

			// Initialize the logger.

			initializeLogger();

			// Load the configuration.

			loadConfig();

			// Initialize the window.

			initializeWindow();

			// Load the files.

			loadFiles();

			// Initialize the components.

			initializeComponents();

			// Open the window.

			frame.setVisible(true);

			// Check for updates.

			checkUpdate();

		} catch (final Texture_Patcher_Exception e) {

			// Happens in the event of a caught but fatal error.

			logger.log(Level.SEVERE, e.getMessage());

			e.printStackTrace();

			if (e.getType() != ErrorType.WINDOW_CLOSED) e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			if (frame != null) frame.dispose();
			if (loadingFrame != null) loadingFrame.dispose();

		} catch (final Throwable t) {

			// Happens in the event of an uncaught error.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.UNEXPECTED_EXCEPTION, t);

			logger.log(Level.SEVERE, t_p_e.getMessage());

			t_p_e.printStackTrace();

			t_p_e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			if (frame != null) frame.dispose();
			if (loadingFrame != null) loadingFrame.dispose();

		}

	}

	// Protected methods.

	protected void initializeLogger () {

		// Initialize the logger with the custom handlers and formatters.

		logger.setLevel(Level.INFO);

		final Logging.LoggingHandler handler = new Logging.LoggingHandler();
		handler.setFormatter(new Logging.LoggingFormatter());

		logger.addHandler(handler);
		logger.setUseParentHandlers(false);

	}

	@SuppressWarnings("unchecked")
	protected void loadConfig () throws Texture_Patcher_Exception {

		try {

			String readLine = "";

			// Find external config file, first by class loader resource, then by the file system.

			if (getClass().getClassLoader().getResource("externalconfig.txt") != null)

				readLine = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResource("externalconfig.txt").openStream())).readLine();

			else if (!debug) {

				throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_MISSING, null);

			}

			// Used for testing.

			if (debug) readLine = "http://soartex.net/texture-patcher/data/config.json";

			// Checks if the externalconfig.txt is the default.

			if (readLine.startsWith("#")) {

				throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_DEFAULT, null);

			}

			// Loads the JSON file.

			config = (JSONObject) new JSONParser().parse(new InputStreamReader(new URL(readLine).openStream()));

			options = (JSONObject) config.get("options");
			mods = (JSONObject) config.get("mods");
			modpacks = (JSONObject) config.get("modpacks");

			// Checks if the root or zips URLs are missing.

			if (options.get("zipsurl") == null) {

				throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_INCOMPLETE, null);

			}

			if (options.get("name") == null) options.put("name", "Texture Patcher");

			// Determine errors.

		} catch (final Texture_Patcher_Exception e) {

			// Happens for TPE's thrown in the method body for if statements.

			throw e;

		} catch (final Exception e) {

			// Happens for all other errors.

			throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_LOADING_FAILED, null);

		}

	}

	protected void initializeWindow () throws Texture_Patcher_Exception {

		// Set the skin from the configuration.

		try {

			if ((String) options.get("skin") != null) {

				if (options.get("skin").equals("native")) {

					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				} else {

					for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

						if (info.getName().equalsIgnoreCase((String) options.get("skin"))) {

							UIManager.setLookAndFeel(info.getClassName());

							break;

						}

					}

				}

			}

		} catch (final Exception e) {

			// Happens if the skin cannot be found, or if an error occurs while setting it.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.SKIN_SETTING_FAILED, e);

			logger.log(Level.WARNING, t_p_e.getMessage());

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

		try {

			// Configure the frame.

			frame = new JFrame((String) options.get("name") + (options.get("name").equals("Texture Patcher") ? " v." : " Patcher v."  ) + VERSION);
			frame.setLayout(new GridBagLayout());

			frame.setLocation(prefsnode.getInt("x", 50), prefsnode.getInt("y", 50));
			frame.setSize(prefsnode.getInt("width", 500), prefsnode.getInt("height", 600));

			frame.setExtendedState(prefsnode.getInt("max", Frame.NORMAL));

			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new Listeners.ExitListener(this));

		} catch (final Exception e) {

			// Happens if an error occurs while initializing the window.

			throw new Texture_Patcher_Exception(this, ErrorType.WINDOW_INITIALIZATION_FAILED, e);

		}

		// Load the frame icon.

		try {

			final URL iconurl = new URL(options.get("iconurl") == null ? "http://soartex.net/texture-patcher/icon.png" : (String) options.get("iconurl"));

			frame.setIconImage(Toolkit.getDefaultToolkit().createImage(iconurl));

		} catch (final Exception e) {

			// Happens if IO error occurs while setting the icon.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.ICON_SETTING_FAILED, e);

			logger.log(Level.WARNING, t_p_e.getMessage());

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

	}

	protected void loadFiles () throws Texture_Patcher_Exception {

		try {

			// Initialize the loading dialog.

			loadingFrame = new JFrame("Loading files...");
			loadingFrame.setLayout(new GridBagLayout());

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

			// Initialize the progress bar.

			final JProgressBar progress = new JProgressBar(SwingConstants.HORIZONTAL);

			progress.setIndeterminate(true);
			loadingFrame.add(progress, gbc);

			gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			// Initialize the static message label.

			final JLabel label = new JLabel("Please wait patiently while we load your files...", SwingConstants.CENTER);
			loadingFrame.add(label, gbc);

			gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			// Initialize the loading mod number label.

			final JLabel message = new JLabel("--", SwingConstants.CENTER);
			loadingFrame.add(message, gbc);

			gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			// Initialize the the loading mod name label.

			final JLabel name = new JLabel("--", SwingConstants.CENTER);
			loadingFrame.add(name,gbc);

			loadingFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			loadingFrame.addWindowListener(new Listeners.ExitListener(this));

			loadingFrame.setIconImage(frame.getIconImage());

			loadingFrame.pack();
			loadingFrame.setResizable(false);

			loadingFrame.setLocation(150, 150);
			loadingFrame.setVisible(true);

			// Load the mods from the config file.

			tableData = loadMods(message, name);

			// Happens in the window is closed.

			if (!loadingFrame.isVisible()) throw new Texture_Patcher_Exception(this, ErrorType.WINDOW_CLOSED, null);

			// Load the modpacks from the config file.

			loadModpacks(message, name);

			// Happens in the window is closed.

			if (!loadingFrame.isVisible()) throw new Texture_Patcher_Exception(this, ErrorType.WINDOW_CLOSED, null);

			// Return to the frame.

			loadingFrame.dispose();

			frame.requestFocus();

		} catch (final Texture_Patcher_Exception e) {

			// Happens for TPE's thrown during mod loading or modpack loading.

			throw e;

		} catch (final Exception e) {

			// Happens if an error occurs while loading the mods.

			throw new Texture_Patcher_Exception(this, ErrorType.FILE_LOADING_FAILED, e);

		}

	}

	protected Object[][] loadMods (final JLabel message, final JLabel title) throws Texture_Patcher_Exception {

		try {

			// Stores each row as an array of the column values.

			final ArrayList<Object[]> rows = new ArrayList<Object[]>();

			int count = 0;

			// Sort the JSON object.

			@SuppressWarnings("unchecked")
			final TreeMap<Object, Object> tmods = new TreeMap<Object, Object>(mods);

			for (final Object mod : tmods.keySet()) {

				// If the window was closed.

				if (loadingFrame.isVisible() != true) {

					break;

				}

				if (mod.equals("")) continue;

				// URL of the mod zip.

				final URL zipurl = new URL((String) options.get("zipsurl") + ((String) mod).replace(" ", "_") + ".zip");

				try {

					URLConnection connection;;

					try {

						connection = zipurl.openConnection();

					} catch (final IOException e) {

						// Retrying in the case of a 502.

						logger.log(Level.WARNING, "IOException while loading mod " + mod + ", trying again!");

						connection = zipurl.openConnection();

					}

					// CHeck if the file exists.

					connection.getInputStream().close();

					// Collect the data for the table.

					final Object[] row = new Object[6];

					row[0] = false;

					row[1] = mod;

					row[2] = ((JSONObject) mods.get(mod)).get("version") == null ? "Unknown" : (String)((JSONObject) mods.get(mod)).get("version");
					row[3] = ((JSONObject) mods.get(mod)).get("mcversion") == null ? "Unknown" : (String)((JSONObject) mods.get(mod)).get("mcversion");

					final int size = connection.getContentLength();

					if (size == -1) {

						row[4] = "Unknown";

					} else {

						if (size > 1024) row[4] = size / 1024 + " kb";
						else row[4] = String.valueOf(size) + " bytes";

					}

					row[5] = new Date(connection.getLastModified());

					message.setText("Loading mod # " + ++count);
					title.setText((String) row[1]);

					logger.log(Level.INFO, "Loading mod: " + mod + ".");

					rows.add(row);

				} catch (final Exception e) {

					// Happens if any error occurs while loading the mod.

					logger.log(Level.WARNING, "Unable to load mod " + mod + ".");

					e.printStackTrace();

					continue;

				}

			}

			// Collect the rows into an two dimensional array.

			final Object[][] temp = new Object[rows.size()][];

			for (int i = 0; i < rows.size(); i++){

				temp[i]= rows.get(i);

			}

			return temp;

		} catch (final Exception e) {

			// Happens if an error occurs while loading the mods.

			throw new Texture_Patcher_Exception(this, ErrorType.MOD_LOADING_FAILED, e);

		}

	}

	protected void loadModpacks (final JLabel message, final JLabel title) throws Texture_Patcher_Exception {

		try {

			// Iterate through the JSON modpack object.

			int count = 0;

			for (final Object modpack : modpacks.keySet()) {

				// If the window was closed.

				if (loadingFrame.isVisible() != true) {

					break;

				}

				try {

					// Load the modpack and make sure that the modpack file exists.

					final URL modpackURL = new URL((String) modpacks.get(modpack));

					modpackURL.openStream();

					message.setText("Loading modpack # " + ++count);
					title.setText((String) modpack);

					logger.log(Level.INFO, "Loading modpack: " + modpack + ".");

				} catch (final Exception e) {

					// Happens if any error occurs while loading the modpack.

					e.printStackTrace();

					logger.log(Level.WARNING, "Unable to load modpack " + modpack + ".");

					continue;

				}

			}

		} catch (final Exception e) {

			// Happens if an error occurs while loading the modpacks.

			throw new Texture_Patcher_Exception(this, ErrorType.MODPACK_LOADING_FAILED, e);

		}

	}

	protected void initializeComponents () throws Texture_Patcher_Exception {

		try {

			// Initialize the path text field.

			final Insets insets = new Insets(1, 3, 1, 3);

			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 4;
			gbc.weightx = 4;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			path = new JTextField(prefsnode.get("path", ""));

			path.setEditable(false);

			frame.add(path, gbc);

			// Initialize the browse button.

			gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			final JButton browse = new JButton("Browse");
			browse.addActionListener(new Listeners.BrowseListener(this));

			frame.add(browse, gbc);

			// Initialize the download pack button.

			gbc = new GridBagConstraints();

			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			final JButton downloadPack = new JButton("Download Pack");
			downloadPack.addActionListener(new Listeners.DownloadPackListener(this));

			frame.add(downloadPack, gbc);

			// Initialize the check for updates button.

			gbc = new GridBagConstraints();

			gbc.gridx = 2;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			checkUpdate = new JButton("Check For Updates");
			checkUpdate.addActionListener(new Listeners.CheckUpdateListener(this));
			checkUpdate.setEnabled(false);

			frame.add(checkUpdate, gbc);

			// Initialize the patch button.

			gbc = new GridBagConstraints();

			gbc.gridx = 3;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			patch = new JButton("Patch");
			patch.addActionListener(new Listeners.PatchListener(this));
			patch.setEnabled(false);

			frame.add(patch, gbc);

			// Resolve the preferences stored path and make sure it exists.

			if (!path.getText().equals("") && new File(path.getText()).exists()) {

				checkUpdate.setEnabled(true);
				patch.setEnabled(true);

			} else {

				prefsnode.remove("path");

				path.setText("");

			}

			// Initialize the table.

			gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 4;
			gbc.weightx = 4;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.insets = insets;

			table = new JTable(new TableModel(tableData));
			table.setFillsViewportHeight(true);
			table.setAutoCreateRowSorter(true);
			table.getTableHeader().setReorderingAllowed(false);
			table.getColumnModel().getColumn(0).setMaxWidth(25);
			table.addMouseListener(new Listeners.TableListener(this));

			frame.add(new JScrollPane(table), gbc);

			// Initialize the menu.

			final JMenuBar menubar = new JMenuBar();

			final JMenu menu = new JMenu("File");

			if ((String) options.get("url") != null) {

				final JMenuItem website = new JMenuItem((String) options.get("name") + " Website");
				website.addActionListener(new Listeners.WebsiteListener(this));
				menu.add(website);

			}

			menu.addSeparator();

			final ButtonGroup group = new ButtonGroup();

			// Load the modpack menu items.

			JRadioButtonMenuItem modpacksitems;

			if (modpacks != null) {

				@SuppressWarnings("unchecked")
				final TreeMap<Object, Object> tmodpacks = new TreeMap<Object, Object>(modpacks);

				for (final Object modpack : tmodpacks.keySet()) {

					modpacksitems = new JRadioButtonMenuItem((String) modpack);
					modpacksitems.setSelected(false);
					modpacksitems.addActionListener(new Listeners.ModpackListener(this));
					group.add(modpacksitems);
					menu.add(modpacksitems);

				}

				if (!modpacks.isEmpty()) menu.addSeparator();

			}

			JRadioButtonMenuItem selectitems;

			selectitems = new JRadioButtonMenuItem("Select All");
			selectitems.setSelected(false);
			selectitems.addActionListener(new Listeners.ModpackListener(this));

			group.add(selectitems);
			menu.add(selectitems);

			selectitems = new JRadioButtonMenuItem("Select None");
			selectitems.setSelected(true);
			selectitems.addActionListener(new Listeners.ModpackListener(this));

			group.add(selectitems);
			menu.add(selectitems);

			menubar.add(menu);

			frame.setJMenuBar(menubar);

		} catch (final Exception e) {

			// Happens if an error occurs while initializing the components.

			throw new Texture_Patcher_Exception(this, ErrorType.COMPONENT_INITIALIZATION_FAILED, e);

		}

	}

	protected void checkUpdate () {

		try {

			// Check for updates by comparing the internal version number with the server version number.

			final URL versionurl = new URL("http://soartex.net/texture-patcher/latestversion.txt");

			final float latestversion = Float.parseFloat(new BufferedReader(new InputStreamReader(versionurl.openStream())).readLine());

			if (latestversion > VERSION) {

				JOptionPane.showMessageDialog(frame, "There is a new version of the patcher available: " + latestversion + " (Current version: " + VERSION + ")\r\n Download the update for the texture artists site, or tell them to update!", "Warning!", JOptionPane.WARNING_MESSAGE);

			}

		} catch (final Exception e) {

			// Happens if any error occurs while checking for updates.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.UPDATE_CHECKING_FAILED, e);

			logger.log(Level.WARNING, t_p_e.getMessage());

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

	}

}
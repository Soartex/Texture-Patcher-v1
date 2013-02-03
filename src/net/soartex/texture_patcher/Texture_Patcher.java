package net.soartex.texture_patcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;
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
import org.json.simple.parser.ParseException;

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

	protected final Preferences prefsnode = Preferences.userNodeForPackage(Texture_Patcher.class);
	protected HashMap<String, String> config = new HashMap<String, String>();

	protected static boolean debug = false;

	protected Object[][] tableData;
	protected HashMap<String, URL> modpacks;

	// Swing objects.

	protected JFrame frame;
	protected JFrame loadingFrame;

	protected JTextField path;
	protected JButton checkUpdate;
	protected JButton patch;
	protected JTable table;

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

			loadConfig();

			initializeWindow();

			loadFiles();

			loadModpacks();

			initializeComponents();

			open();

			checkUpdate();

		} catch (final Texture_Patcher_Exception e) {

			e.printStackTrace();

			if (e.getType() != ErrorType.WINDOW_CLOSED) e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

			return;

		}

	}

	@SuppressWarnings("unchecked")
	protected void loadConfig () throws Texture_Patcher_Exception {

		try {

			String readLine = "";

			// Find external config file, first by class loader resource, then by the file system.

			if (getClass().getClassLoader().getResource("externalconfig.txt") != null)

				readLine = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResource("externalconfig.txt").openStream())).readLine();

			else if (!debug)

				throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_MISSING, null);

			// Used for testing.

			if (debug) readLine = "http://soartex.net/texture-patcher/data/config.json";

			// Checks if the externalconfig.txt is the default.

			if (readLine.startsWith("#"))

				throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_DEFAULT, null);

			// Loads the JSON file.

			config = (HashMap<String, String>) new JSONParser().parse(new InputStreamReader(new URL(readLine).openStream()));

			// Checks if the root or zips URLs are missing.

			if (config.get("rooturl") == null || config.get("zipsurl") == null)

				throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_INCOMPLETE, null);

			// Resolves file URLs based on the root URL.

			final String rooturl = config.get("rooturl");

			config.put("modsurl", rooturl + "/mods.json");
			config.put("modpacksurl", rooturl + "/modpacks.json");

			if (config.get("name") == null) config.put("name", "Texture Patcher");

			// Determine errors.

		} catch (final FileNotFoundException e) {

			throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_NOT_FOUND, e);

		} catch (final MalformedURLException e) {

			// Happens if the URL in the externalconfig.txt is malformed.

			throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_BAD, e);

		} catch (final IOException e) {

			// Happens if the URL's host cannot be resolved.

			throw new Texture_Patcher_Exception(this, ErrorType.CANNOT_FIND_SERVER, e);

		} catch (final ParseException e) {

			// Happens if the config file cannot be parsed as JSON.

			throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_BAD, e);

		} catch (final Texture_Patcher_Exception e) {

			// Happens for TPE's thrown in the method body for if statements.

			throw e;

		} catch (final Exception e) {

			// Happens for all other errors.

			throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_LOADING_FAILED, e);

		}

	}

	protected void initializeWindow () {

		// Set the skin from the configuration.

		try {

			if (config.get("skin") != null) {

				if (config.get("skin").equals("native")) {

					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				} else {

					for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

						if (info.getName().equalsIgnoreCase(config.get("skin"))) {

							UIManager.setLookAndFeel(info.getClassName());

							break;

						}

					}

				}

			}

		} catch (final Exception e) {

			// Happens if the skin cannot be found, or if an error occurs while setting it.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.SETTING_SKIN_FAILED, e);

			t_p_e.printStackTrace();

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

		// Configure the frame.

		frame = new JFrame(config.get("name") + (config.get("name").equals("Texture Patcher") ? " v." : " Patcher v."  ) + VERSION);
		frame.setLayout(new GridBagLayout());

		frame.setLocation(50, 50);

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new Listeners.ExitListener(this));

		// Load the frame icon.

		try {

			final URL iconurl = new URL(config.get("iconurl") == null ? "http://soartex.net/texture-patcher/icon.png" : config.get("iconurl"));

			frame.setIconImage(Toolkit.getDefaultToolkit().createImage(iconurl));

		} catch (final Exception e) {

			// Happens if IO error occurs while setting the icon.

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.SETTING_ICON_FAILED, e);

			t_p_e.printStackTrace();

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

	}

	protected void loadFiles () throws Texture_Patcher_Exception {

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

		final JLabel message = new JLabel("Please wait patiently while we load your files...", SwingConstants.CENTER);
		loadingFrame.add(message, gbc);

		gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.insets = insets;

		final JLabel modMessage = new JLabel("Loading mod # 0", SwingConstants.CENTER);
		loadingFrame.add(modMessage, gbc);

		gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.insets = insets;

		final JLabel modName = new JLabel("--", SwingConstants.CENTER);
		loadingFrame.add(modName,gbc);

		loadingFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		loadingFrame.addWindowListener(new Listeners.ExitListener(this));

		loadingFrame.setIconImage(frame.getIconImage());

		loadingFrame.pack();
		loadingFrame.setResizable(false);

		loadingFrame.setLocation(150, 150);
		loadingFrame.setVisible(true);

		final Object[][] temp = loadTable(modMessage, modName);

		if (!loadingFrame.isVisible()) throw new Texture_Patcher_Exception(this, ErrorType.WINDOW_CLOSED, null);

		tableData = new Object[temp.length][];

		for (int i = 0; i < temp.length; i++) {

			final Object[] temp2 = {false,
					temp[i][0],
					temp[i][1],
					temp[i][2],
					temp[i][3],
					temp[i][4]};

			tableData[i] = temp2;

		}

		loadingFrame.dispose();

		frame.requestFocus();

	}

	@SuppressWarnings("unchecked")
	protected Object[][] loadTable (final JLabel modMessage, final JLabel modTitle) {

		final ArrayList<String[]> itemsInfo = new ArrayList<String[]>();

		try {

			final HashMap<String, JSONObject> mods = (HashMap<String, JSONObject>) new JSONParser().parse(new InputStreamReader(new URL(config.get("modsurl")).openStream()));

			int count = 0;

			final TreeSet<String> modset = new TreeSet<String>(mods.keySet());

			for (final String mod : modset) {

				if (loadingFrame.isVisible() != true) {

					break;

				}

				final URL zipurl = new URL(config.get("zipsurl") + mod.replace(" ", "_") + ".zip");

				try {

					URLConnection connection;;

					try {

						connection = zipurl.openConnection();

					} catch (final IOException e) {

						e.printStackTrace();

						connection = zipurl.openConnection();

					}

					connection.getInputStream().close();

					final String[] itemtext = new String[5];

					itemtext[0] = mod;

					itemtext[1] = mods.get(mod).get("version") == null ? "Unknown" : (String) mods.get(mod).get("version");
					itemtext[2] = mods.get(mod).get("mcversion") == null ? "Unknown" : (String) mods.get(mod).get("mcversion");

					try {

						final int size = connection.getContentLength();

						if (size == -1) {

							itemtext[3] = "Unknown";

						} else {

							if (size > 1024) itemtext[3] = size / 1024 + " kb";
							else itemtext[3] = String.valueOf(size) + " bytes";

						}

					} catch (final Exception e) {

						itemtext[3] = "Unknown";

					}

					try {

						itemtext[4] = new SimpleDateFormat("MM/dd/yyyy").format(new Date(connection.getLastModified()));

					} catch (final Exception e) {

						itemtext[4] = "Unknown";

					}

					itemsInfo.add(itemtext);

					modMessage.setText("Loading mod # " + count++);
					modTitle.setText(itemtext[0]);

					System.out.println("Loading: " + itemtext[0]);

				} catch (final IOException e) {

					e.printStackTrace();

					continue;

				}

			}

		} catch (final Exception e) {

			e.printStackTrace();

		}

		final Object[][] temp = new Object[itemsInfo.size()][];

		for (int i = 0; i < itemsInfo.size(); i++){

			temp[i]= itemsInfo.get(i);

		}

		return temp;
	}

	@SuppressWarnings("unchecked")
	protected void loadModpacks () {

		modpacks = new HashMap<String, URL>();

		try {

			final HashMap<String, String> jsonModpacks = (HashMap<String, String>) new JSONParser().parse(new InputStreamReader(new URL(config.get("modpacksurl")).openStream()));

			for (final String modpack : jsonModpacks.keySet()) {

				try {

					final URL modpackURL = new URL(config.get("rooturl") + jsonModpacks.get(modpack));

					modpackURL.openStream();

					modpacks.put(modpack, modpackURL);

				} catch (final IOException e) {

					e.printStackTrace();

					continue;

				}

			}

		} catch (final Exception e) {

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.MODPACK_LOADING_FAILED, e);

			t_p_e.printStackTrace();

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

	}

	protected void initializeComponents () {

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

		if (!path.getText().equals("") && new File(path.getText()).exists()) {

			checkUpdate.setEnabled(true);
			patch.setEnabled(true);

		} else {

			path.setText("");

		}

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

		table.getColumnModel().getColumn(0).setMaxWidth(25);
		table.addMouseListener(new Listeners.TableListener(table, this));

		frame.add(new JScrollPane(table), gbc);

		final JMenuBar menubar = new JMenuBar();

		final JMenu menu = new JMenu("File");

		if (config.get("url") != null) {

			final JMenuItem website = new JMenuItem(config.get("name") + " Website");
			website.addActionListener(new Listeners.WebsiteListener(this));
			menu.add(website);

		}

		menu.addSeparator();

		final ButtonGroup group = new ButtonGroup();

		JRadioButtonMenuItem modpacksitems;

		if (modpacks != null) {

			for (final String modpack : modpacks.keySet()) {

				modpacksitems = new JRadioButtonMenuItem(modpack);
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

	}

	protected void open () {

		frame.setSize(500, 600);

		frame.setVisible(true);

	}

	protected void checkUpdate () {

		try {

			final URL versionurl = new URL("http://soartex.net/texture-patcher/latestversion.txt");

			final float latestversion = Float.parseFloat(new BufferedReader(new InputStreamReader(versionurl.openStream())).readLine());

			if (latestversion > VERSION) {

				JOptionPane.showMessageDialog(frame, "There is a new version of the patcher available: " + latestversion + " (Current version: " + VERSION + ")\r\n Download the update for the texture artists site, or tell them to update!", "Warning!", JOptionPane.WARNING_MESSAGE);

			}

		} catch (final Exception e) {

			final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.UPDATE_CHECK_FAILED, e);

			t_p_e.printStackTrace();

			t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

		}

	}

}
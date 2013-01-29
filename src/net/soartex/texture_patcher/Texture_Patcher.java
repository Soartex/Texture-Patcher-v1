package net.soartex.texture_patcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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

import org.json.simple.parser.JSONParser;

public final class Texture_Patcher implements Runnable {

	protected final static float VERSION = 1.1F;

	protected final Preferences prefsnode = Preferences.userNodeForPackage(Texture_Patcher.class);
	protected HashMap<String, String> config = new HashMap<String, String>();

	protected boolean stopped = false;
	protected static boolean debug = false;

	protected JFrame frame;
	protected JFrame loadingFrame;

	protected JTextField path;
	protected JButton checkUpdate;
	protected JButton patch;
	protected JTable table;

	protected Object[][] tableData;
	protected HashMap<String, URL> modpacks;

	public static void main (final String[] args) {

		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Texture-Patcher v." + VERSION);

		if (args.length > 0 && Boolean.parseBoolean(args[0])) debug = true;

		new Thread(new Texture_Patcher()).start();

	}

	@Override public void run () {

		loadConfig();

		if (stopped) return;

		initializeWindow();

		if (stopped) return;

		loadFiles();

		if (stopped) return;

		loadModpacks();

		if (stopped) return;

		initializeComponents();

		if (stopped) return;

		open();

		if (stopped) return;

		checkUpdate();

		if (stopped) return;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) protected void loadConfig () {

		try {

			String readLine;

			if (new File("externalconfig.txt").exists()) {

				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("externalconfig.txt")));

				readLine = in.readLine();

				in.close();

			} else if (getClass().getClassLoader().getResource("externalconfig.txt") != null) {

				readLine = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResource("externalconfig.txt").openStream())).readLine();

			} else {

				JOptionPane.showMessageDialog(null, "No externalconfig.txt file!", "Error!", JOptionPane.ERROR_MESSAGE);

				stopped = true;

				return;

			}

			// TODO: Used for testing.
			if (debug) readLine = "http://soartex.net/texture-patcher/data/config.json";

			if (readLine.startsWith("#")) {

				JOptionPane.showMessageDialog(null, "externalconfig.txt file is the default!", "Error!", JOptionPane.ERROR_MESSAGE);

				stopped = true;

				return;

			}

			if (readLine.endsWith(".json") || readLine.endsWith(".JSON")) {

				config = (HashMap) new JSONParser().parse(new InputStreamReader(new URL(readLine).openStream()));

			} else {

				final Properties props = new Properties();

				props.load(new URL(readLine).openStream());

				config.putAll((Map) props);

				System.out.println(config.get("rooturl"));

			}

			final String rooturl = config.get("rooturl");

			if (rooturl == null || config.get("zipsurl") == null) {

				JOptionPane.showMessageDialog(null, "The configuration file is incomplete!", "Error!", JOptionPane.ERROR_MESSAGE);

				stopped = true;

				return;

			}

			config.put("versionurl", "http://soartex.net/texture-patcher/latestversion.txt");
			config.put("modsurl", rooturl + "/mods.csv");
			config.put("modpacksurl", rooturl + "/modpacks.csv");

			if (config.get("name") == null) config.put("name", "Texture Patcher");

		} catch (final Exception e) {

			e.printStackTrace();

			stopped = true;

		}

	}

	protected void initializeWindow () {

		if (config.get("skin") != null) {

			if (config.get("skin").equals("native")) {

				try {

					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				} catch (final Exception e) {

					e.printStackTrace();

				}

			} else {

				try {

					for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

						if (info.getName().equalsIgnoreCase(config.get("skin"))) {

							UIManager.setLookAndFeel(info.getClassName());

							break;

						}

					}

				} catch (final Exception e) {

					e.printStackTrace();

				}

			}

		}

		frame = new JFrame(config.get("name") + (config.get("name").equals("Texture Patcher") ? " v." : " Patcher v."  ) + VERSION);
		frame.setLayout(new GridBagLayout());

		frame.setLocation(50, 50);

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new Listeners.ExitListener(this));

		try {

			final URL iconurl = new URL(config.get("iconurl") == null ? "http://soartex.net/patcher/texture-patcher/icon.png" : config.get("iconurl"));

			frame.setIconImage(Toolkit.getDefaultToolkit().createImage(iconurl));

		} catch (final Exception e) {

			e.printStackTrace();

			if (new File("icon.png").exists()) {

				frame.setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));

			} else if (getClass().getClassLoader().getResource("icon.png") != null) {

				frame.setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("icon.png")));

			}

		}

	}

	protected void loadFiles () {

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

	protected Object[][] loadTable (final JLabel modMessage, final JLabel modTitle) {

		final ArrayList<String[]> itemsInfo = new ArrayList<String[]>();

		try {

			final URL tabledata = new URL(config.get("modsurl"));

			final BufferedReader in = new BufferedReader(new InputStreamReader(tabledata.openStream()));

			String readline;

			int count = 0;

			while ((readline = in.readLine()) != null) {

				if (loadingFrame.isVisible() != true) {

					break;

				}

				final URL zipurl = new URL(config.get("zipsurl") + readline.split(",")[0].replace(" ", "") + ".zip");

				try {

					URLConnection connection;;

					try {

						connection = zipurl.openConnection();

					} catch (final IOException e) {

						e.printStackTrace();

						connection = zipurl.openConnection();

					}

					connection.getInputStream().close();

					final String[] split = readline.split(",");

					if (split.length < 1) {

						continue;

					}

					final String[] itemtext = new String[5];

					itemtext[0] = split[0].replace(" ", "").replace("_", " ");

					if (split.length == 1) {

						itemtext[1] = "Unknown";
						itemtext[2] = "Unknown";

					} else {

						itemtext[1] = split[1].replace(" ", "");
						itemtext[2] = split[2].replace(" ", "");

					}

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

		} catch (final IOException e) {

			e.printStackTrace();

		}

		final Object[][] temp = new Object[itemsInfo.size()][];

		for (int i = 0; i < itemsInfo.size(); i++){

			temp[i]= itemsInfo.get(i);

		}

		return temp;
	}

	protected void loadModpacks () {

		modpacks = new HashMap<String, URL>();

		try {

			final URL modpacksurl = new URL(config.get("modpacksurl"));

			final BufferedReader in = new BufferedReader(new InputStreamReader(modpacksurl.openStream()));

			String readline;

			while ((readline = in.readLine()) != null) {

				try {

					new URL(config.get("rooturl") + readline.split(",")[1]).openStream();

				} catch (final IOException e) {

					e.printStackTrace();

					continue;

				}

				if (!readline.contains(",") || readline.split(",").length < 2) {

					continue;

				}

				modpacks.put(readline.split(",")[0].replace(" ", "").replace("_", " "), new URL(config.get("rooturl") + readline.split(",")[1].replace(" ", "")));

			}

		} catch (final Exception e){

			e.printStackTrace();

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

		for (final String modpack: modpacks.keySet()) {

			modpacksitems = new JRadioButtonMenuItem(modpack);
			modpacksitems.setSelected(false);
			modpacksitems.addActionListener(new Listeners.ModpackListener(this));
			group.add(modpacksitems);
			menu.add(modpacksitems);

		}

		JRadioButtonMenuItem selectitems;

		if (!modpacks.isEmpty()) menu.addSeparator();

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

			final URL versionurl = new URL(config.get("versionurl"));

			final float latestversion = Float.parseFloat(new BufferedReader(new InputStreamReader(versionurl.openStream())).readLine());

			if (latestversion > VERSION) {

				JOptionPane.showMessageDialog(frame, "There is a new version of the patcher available: " + latestversion + " (Current version: " + VERSION + ")\r\n Download the update for the texture artists site, or tell them to update!", "Warning!", JOptionPane.WARNING_MESSAGE);

			}

		} catch (final IOException e) {

			e.printStackTrace();

			JOptionPane.showMessageDialog(frame, "Unable to check for updates!", "Error!", JOptionPane.ERROR_MESSAGE);

		}

	}

}
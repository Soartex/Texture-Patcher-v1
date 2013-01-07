package net.soartex.patcher;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
import java.util.Properties;

import javax.swing.Box;
import javax.swing.ButtonGroup;
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
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public final class Texture_Patcher implements Runnable {

	protected final float VERSION = 1.0F;

	protected final Properties config = new Properties();

	protected boolean stopped = false;

	protected JFrame frame;
	protected JTable table;
	protected JFrame loadingFrame;
	protected JMenuItem patchitem;

	protected Object[][] tableData;
	protected HashMap<String, URL> modpacks;

	protected File selectedFile;

	public static void main (final String[] args) {

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

	protected void loadConfig () {

		try {

			if (new File("externalconfig.txt").exists()) {

				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("externalconfig.txt")));

				final String readLine = in.readLine();

				in.close();

				if (readLine.startsWith("#")) {

					JOptionPane.showMessageDialog(null, "externalconfig.txt file is the default!", "Error!", JOptionPane.ERROR_MESSAGE);

					stopped = true;

					return;

				}

				config.load(new URL(readLine).openStream());

			} else if (getClass().getClassLoader().getResource("externalconfig.txt") != null) {

				final String readLine = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResource("externalconfig.txt").openStream())).readLine();

				if (readLine.startsWith("#")) {

					JOptionPane.showMessageDialog(null, "externalconfig.txt file is the default!", "Error!", JOptionPane.ERROR_MESSAGE);

					stopped = true;

					return;

				}

				config.load(new URL(readLine).openStream());

			} else {

				JOptionPane.showMessageDialog(null, "No externalconfig.txt file!", "Error!", JOptionPane.ERROR_MESSAGE);

				stopped = true;

				return;

			}

			final String rooturl = config.getProperty("rooturl");

			if (rooturl == null || config.getProperty("zipsurl") == null) {

				JOptionPane.showMessageDialog(null, "The configuration file is incomplete!", "Error!", JOptionPane.ERROR_MESSAGE);

				stopped = true;

				return;

			}

			config.put("versionurl", "http://soartex-fanver.github.com/Texture-Patcher/latestversion.txt");
			config.put("modsurl", rooturl + "/mods.csv");
			config.put("modpacksurl", rooturl + "/modpacks.csv");

			if (config.getProperty("name") == null) config.put("name", "Texture Patcher");

		} catch (final IOException e) {

			e.printStackTrace();

			stopped = true;

		}

	}

	protected void initializeWindow () {

		frame = new JFrame(config.getProperty("name") + (config.getProperty("name").equals("Texture Patcher") ? " v." : "Patcher v."  ) + VERSION);

		frame.setLocation(50, 50);

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new Listeners.ExitListener(this));

		if (new File("icon.png").exists()) {

			frame.setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));

		} else if (getClass().getClassLoader().getResource("icon.png") != null) {

			frame.setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("icon.png")));

		}

	}

	protected void loadFiles () {

		loadingFrame = new JFrame("Loading files...");
		loadingFrame.setLayout(new GridLayout(4,1));

		final JProgressBar progress = new JProgressBar(SwingConstants.HORIZONTAL);

		progress.setIndeterminate(true);
		loadingFrame.add(progress, BorderLayout.NORTH);

		final JLabel message = new JLabel("Please wait patiently while we load your files...", SwingConstants.CENTER);
		loadingFrame.add(message);

		final JLabel modMessage = new JLabel("Loading mod # 0", SwingConstants.CENTER);
		loadingFrame.add(modMessage);

		final JLabel modName = new JLabel("--", SwingConstants.CENTER);
		loadingFrame.add(modName);

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

	}

	protected Object[][] loadTable (final JLabel modMessage, final JLabel modTitle) {

		final ArrayList<String[]> itemsInfo = new ArrayList<String[]>();
		final ArrayList<String> itemsInfoUrl = new ArrayList<String>();

		try {

			final URL tabledata = new URL(config.getProperty("modsurl"));

			final BufferedReader in = new BufferedReader(new InputStreamReader(tabledata.openStream()));

			String readline;

			int count = 0;

			while ((readline = in.readLine()) != null) {

				if (loadingFrame.isVisible() != true) {

					break;

				}

				final URL zipurl = new URL(config.getProperty("zipsurl") + readline.split(",")[0].replace(" ", "_") + ".zip");

				try {

					final URLConnection connection = zipurl.openConnection();

					connection.getInputStream().close();

					final String[] split = readline.split(",");

					if (split.length < 1) {

						continue;

					}

					final String[] itemtext = new String[5];

					itemtext[0] = split[0];

					if (split.length == 1) {

						itemtext[1] = "Unknown";
						itemtext[2] = "Unknown";

					} else {

						itemtext[1] = split[1];
						itemtext[2] = split[2];

					}

					try {

						final int size = connection.getContentLength();

						if (size == -1) {

							itemtext[3] = "Unknown";

						} else {

							if (size > 1024 && size < 1024 * 1024) itemtext[3] = size / 1024 + " kb";

							else if (size > 1024 * 1024) itemtext[3] = size / (1024 * 1024) + " mb";

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
					itemsInfoUrl.add(zipurl.toString());

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

			final URL modpacksurl = new URL(config.getProperty("modpacksurl"));

			final BufferedReader in = new BufferedReader(new InputStreamReader(modpacksurl.openStream()));

			String readline;

			while ((readline = in.readLine()) != null) {

				try {

					new URL(config.getProperty("rooturl") + "modpacks/" + readline.split(",")[1]).openStream();

				} catch (final IOException e) {

					e.printStackTrace();

					continue;

				}

				if (!readline.contains(",") || readline.split(",").length < 2) {

					continue;

				}

				modpacks.put(readline.split(",")[0], new URL(config.getProperty("rooturl") + "modpacks/" + readline.split(",")[1]));

			}

		} catch (final Exception e){

			e.printStackTrace();

		}

	}

	protected void initializeComponents () {

		table = new JTable(new TableModel(tableData));
		table.setFillsViewportHeight(true);

		table.setAutoCreateRowSorter(true);

		table.getColumnModel().getColumn(0).setMaxWidth(25);
		table.addMouseListener(new Listeners.TableListener(table, this));

		frame.add(new JScrollPane(table));

		final JMenuBar menubar = new JMenuBar();

		if (config.getProperty("url") != null) {

			final JMenu website = new JMenu(config.getProperty("name") + " Website");
			website.addMenuListener(new Listeners.WebsiteListener(this));
			menubar.add(website);

		}

		final JMenu modpacksmenu = new JMenu("Modpacks");

		final ButtonGroup group = new ButtonGroup();

		JRadioButtonMenuItem modpacksitems;

		for (final String modpack: modpacks.keySet()) {

			modpacksitems = new JRadioButtonMenuItem(modpack);
			modpacksitems.setSelected(false);
			modpacksitems.addActionListener(new Listeners.ModpackListener(this));
			group.add(modpacksitems);
			modpacksmenu.add(modpacksitems);

		}

		JRadioButtonMenuItem selectitems;

		if (!modpacks.isEmpty()) modpacksmenu.addSeparator();

		selectitems = new JRadioButtonMenuItem("Select All");
		selectitems.setSelected(false);
		selectitems.addActionListener(new Listeners.ModpackListener(this));

		group.add(selectitems);
		modpacksmenu.add(selectitems);

		selectitems = new JRadioButtonMenuItem("Select None");
		selectitems.setSelected(true);
		selectitems.addActionListener(new Listeners.ModpackListener(this));

		group.add(selectitems);
		modpacksmenu.add(selectitems);

		menubar.add(modpacksmenu);

		menubar.add(Box.createHorizontalGlue());

		final JMenu patchmenu = new JMenu("Patch");

		final JMenuItem browseitem = new JMenuItem("Browse");
		browseitem.addActionListener(new Listeners.BrowseListener(this));
		patchmenu.add(browseitem);

		patchmenu.addSeparator();

		patchitem = new JMenuItem("Patch");
		patchitem.addActionListener(new Listeners.PatchListener(this));
		patchitem.setEnabled(false);
		patchmenu.add(patchitem);

		menubar.add(patchmenu);

		frame.setJMenuBar(menubar);

	}

	protected void open () {

		frame.setSize(500, 600);

		frame.setVisible(true);

	}

	protected void checkUpdate () {

		try {

			final URL versionurl = new URL(config.getProperty("versionurl"));

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
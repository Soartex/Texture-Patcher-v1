package net.soartex.texture_patcher;

import com.google.common.io.Resources;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Texture_Patcher main class.
 *
 * @author REDX36
 * @version 1.1
 */
public final class Texture_Patcher implements Runnable {

    // Program variables.

    protected final static float VERSION = 2.3F;
    protected static boolean debug = false;

    protected final Preferences prefsnode = Preferences.userNodeForPackage(getClass());
    protected final Logger logger = Logger.getLogger(getClass().getName() + "." + System.currentTimeMillis());
    protected ArrayList<String> logs = new ArrayList<String>();

    protected JSONObject config;
    protected JSONObject options;
    protected JSONObject mods;
    protected JSONObject modpacks;
    protected JSONObject branches;

    protected Object[][] tableData;

    // Swing objects.

    protected JFrame frame;
    protected JFrame loadingFrame;

    protected JTextField path;
    protected JButton checkUpdate;
    protected JButton patch;
    protected JTable table;

    OkHttpClient client = new OkHttpClient();

    // Public methods

    /**
     * Sets certain Mac OSX Cocoa system flags, checks arguments for debug mode, and runs the patcher in a separate thread.
     *
     * @param args If <code>args</code> is longer than 0, and <code>args[0]</code> evaluates to <code>true</code>,
     */
    public static void main(final String[] args) {

        // Set certain properties specific to Mac OSX Cocoa.

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Texture-Patcher v." + VERSION);

        // Check if the patcher is being run in debug mode.

        debug = Boolean.parseBoolean(args.length > 0 ? args[0] : "");

        // Start the patcher in its own thread.

        new Thread(new Texture_Patcher()).start();

    }

    @Override
    public void run() {

        try {

            // Set the native look ad feel for before the skin in the config is loaded.

            try {

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            } catch (final Exception e1) {

                e1.printStackTrace();

            }

            // Initialize the logger.

            initializeLogger();

            // Load the branch configuration.

            String url = loadModBranch();

            // Load the configuration.

            loadConfig(url);

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

            if (e.getType() != ErrorType.WINDOW_CLOSED) {

                e.printStackTrace();

                e.showDialog("Error!", JOptionPane.ERROR_MESSAGE);

                createCrashLog();

            }

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

            createCrashLog();

        }

    }

    // Protected methods.

    protected void initializeLogger() {

        // Initialize the logger with the custom handlers and formatters.

        logger.setLevel(Level.INFO);

        final Logging.LoggingHandler handler = new Logging.LoggingHandler(this);
        handler.setFormatter(new Logging.LoggingFormatter());

        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

    }

    @SuppressWarnings("unchecked")
    protected String loadModBranch() throws Texture_Patcher_Exception {

        try {
            String readLine = "";
            String readLine2 = "";

            // Find external config file, first by class loader resource, then by the file system.
            if (getClass().getClassLoader().getResource("externalconfig.txt") != null) {
                List<String> cfgLines = Resources.readLines(Resources.getResource("externalconfig.txt"), Charset.defaultCharset());

                LinkedList<String> tmpList = new LinkedList<String>();
                for (String entry : cfgLines) {
                    if (entry.startsWith("#") || entry.isEmpty()) {
                        continue;
                    }

                    tmpList.add(entry);
                }

                readLine = tmpList.get(0);
                readLine2 = tmpList.get(1);
            } else if (!debug) {
                throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_MISSING, null);
            }

            // Used for testing.
            if (debug) readLine = "http://soartex.net/texture-patcher/data/config.json";
            if (debug) readLine2 = "http://soartex.net/texture-patcher/data/branches.json";

            // If the second config line for branches is not there. Return normal branch
            if (readLine2 == null || readLine2.isEmpty()) {
                return readLine;
            }

            // Loads the JSON file for branch
            config = (JSONObject) new JSONParser().parse(getHTTPString(readLine2));

            if (config.containsKey("branches")) {
                branches = (JSONObject) config.get("branches");
            } else {
                return readLine;
            }

            // Load the branches data
            ArrayList<String[]> branchUrl = new ArrayList<String[]>();
            final TreeMap<Object, Object> branchest = new TreeMap<Object, Object>(branches);

            for (final Object branch : branchest.keySet()) {
                String readName = String.valueOf(((JSONObject) branches.get(branch)).get("name"));
                String readUrl = String.valueOf(((JSONObject) branches.get(branch)).get("url"));
                //if there is a url then add it to the list
                if (readUrl != null && readName != null) {
                    String[] temp = new String[2];
                    temp[0] = readName;
                    temp[1] = readUrl;
                    branchUrl.add(temp);
                }
            }

            // Display Option Pane for User
            String[] possibleValues = new String[branchUrl.size()];
            for (int i = 0; i < branchUrl.size(); i++) {
                possibleValues[i] = branchUrl.get(i)[0];
            }
            String selectedValue = (String) JOptionPane.showInputDialog(null, "Please Select a Mod Branch", "Select A Branch", JOptionPane.PLAIN_MESSAGE, null, possibleValues, possibleValues[possibleValues.length - 1]);

            // If cancel or exit is pressed the return value is null. Therefor exit the program
            if (selectedValue == null) {
                System.exit(0);
            }

            // Find the selected branch
            int selectedBranch = branchUrl.size() - 1;
            for (int i = 0; i < branchUrl.size(); i++) {
                if (branchUrl.get(i)[0].equalsIgnoreCase(selectedValue)) {
                    selectedBranch = i;
                }
            }

            //return the branch
            return branchUrl.get(selectedBranch)[1];

            // Determine errors.

        } catch (final Exception e) {

            // Happens for all other errors.

            throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_LOADING_FAILED, e);

        }
    }

    @SuppressWarnings("unchecked")
    protected void loadConfig(final String branch) throws Texture_Patcher_Exception {

        try {

            String readLine = branch;

            // Checks if the externalconfig.txt is the default.

            if (readLine.startsWith("#")) {

                throw new Texture_Patcher_Exception(this, ErrorType.EXTERNAL_CONFIG_DEFAULT, null);

            }

            // Loads the JSON file.

            config = (JSONObject) new JSONParser().parse(getHTTPString(readLine));

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

            throw new Texture_Patcher_Exception(this, ErrorType.CONFIG_LOADING_FAILED, e);

        }

    }

    protected void initializeWindow() throws Texture_Patcher_Exception {

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

            t_p_e.printStackTrace();

            t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

        }

        try {

            // Configure the frame.

            frame = new JFrame((String) options.get("name") + (options.get("name").equals("Texture Patcher") ? " v." : " Patcher v.") + VERSION);
            frame.setLayout(new GridBagLayout());

            frame.setSize(prefsnode.getInt("width", 500), prefsnode.getInt("height", 600));

            if (prefsnode.getInt("x", -1000) != -1000 && prefsnode.getInt("y", -1000) != -1000) {

                frame.setLocation(prefsnode.getInt("x", 50), prefsnode.getInt("y", 50));

            } else {

                frame.setLocationRelativeTo(null);

            }

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

            // Happens if an error occurs while setting the icon.

            final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.ICON_SETTING_FAILED, e);

            logger.log(Level.WARNING, t_p_e.getMessage());

            t_p_e.printStackTrace();

            t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

        }

    }

    protected void loadFiles() throws Texture_Patcher_Exception {

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
            loadingFrame.add(name, gbc);

            loadingFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            loadingFrame.addWindowListener(new Listeners.ExitListener(this));

            loadingFrame.setIconImage(frame.getIconImage());

            loadingFrame.pack();
            loadingFrame.setResizable(false);

            loadingFrame.setLocationRelativeTo(null);
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

    protected Object[][] loadMods(final JLabel message, final JLabel title) throws Texture_Patcher_Exception {

        try {

            // Stores each row as an array of the column values.

            final ArrayList<Object[]> rows = new ArrayList<Object[]>();

            int count = 0;

            // Sort the JSON object.

            @SuppressWarnings("unchecked")
            final TreeMap<Object, Object> tmods = new TreeMap<Object, Object>(mods);

            for (final Object mod : tmods.keySet()) {

                // If the window was closed.
                if (loadingFrame.isVisible() != true)
                    break;

                // URL of the mod zip.

                final URL zipurl = new URL((String) options.get("zipsurl") + ((String) mod).replace(" ", "_") + ".zip");

                try {
                    Response curResp = getHeadResp((String) options.get("zipsurl") + ((String) mod).replace(" ", "_") + ".zip");

                    if (curResp == null || curResp.code() != 200)
                        continue;

                    // Collect the data for the table.

                    final Object[] row = new Object[6];

                    row[0] = false;

                    row[1] = mod;

                    row[2] = String.valueOf(((JSONObject) mods.get(mod)).get("version") == null ? "Unknown" : ((JSONObject) mods.get(mod)).get("version"));
                    row[3] = String.valueOf(((JSONObject) mods.get(mod)).get("mcversion") == null ? "Unknown" : ((JSONObject) mods.get(mod)).get("mcversion"));

                    final int size = Integer.parseInt(curResp.headers().get("Content-Length"));

                    if (size == -1) {

                        row[4] = "Unknown";

                    } else {

                        if (size > 1024) row[4] = size / 1024 + " kb";
                        else row[4] = String.valueOf(size) + " bytes";

                    }

                    row[5] = curResp.headers().getDate("Last-Modified");

                    message.setText("Loading mod # " + ++count);
                    title.setText((String) row[1]);

                    logger.log(Level.INFO, "Loading mod #" + count + ": " + mod + ".");

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

            for (int i = 0; i < rows.size(); i++) {

                temp[i] = rows.get(i);

            }

            return temp;

        } catch (final Exception e) {

            // Happens if an error occurs while loading the mods.

            throw new Texture_Patcher_Exception(this, ErrorType.MOD_LOADING_FAILED, e);

        }

    }

    protected void loadModpacks(final JLabel message, final JLabel title) throws Texture_Patcher_Exception {

        try {

            // Iterate through the JSON modpack object.

            int count = 0;

            for (final Object modpack : modpacks.keySet()) {

                // If the window was closed.

                if (loadingFrame.isVisible() != true)
                    break;

                try {
                    // Load the modpack and make sure that the modpack file exists.
                    Response curResp = getHeadResp((String) modpacks.get(modpack));

                    if (curResp == null || curResp.code() != 200)
                        continue;

                    message.setText("Loading modpack # " + ++count);
                    title.setText((String) modpack);

                    logger.log(Level.INFO, "Loading modpack #" + count + ": " + modpack + ".");

                } catch (final Exception e) {

                    // Happens if any error occurs while loading the modpack.

                    e.printStackTrace();

                    logger.log(Level.WARNING, "Unable to load modpack #" + count + ": " + modpack + ".");

                    continue;

                }

            }

        } catch (final Exception e) {

            // Happens if an error occurs while loading the modpacks.

            throw new Texture_Patcher_Exception(this, ErrorType.MODPACK_LOADING_FAILED, e);

        }

    }

    protected void initializeComponents() throws Texture_Patcher_Exception {

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

    protected void checkUpdate() {

        try {

            // Check for updates by comparing the internal version number with the server version number.
            final float latestversion = Float.parseFloat(getHTTPString("http://soartex.net/texture-patcher/latestversion.txt"));

            if (latestversion > VERSION) {

                JOptionPane.showMessageDialog(frame, "There is a new version of the patcher available: " + latestversion + " (Current version: " + VERSION + ")\r\n Download the update for the texture artists site, or tell them to update!", "Warning!", JOptionPane.WARNING_MESSAGE);

            }

        } catch (final Exception e) {

            // Happens if any error occurs while checking for updates.

            final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.UPDATE_CHECKING_FAILED, e);

            logger.log(Level.WARNING, t_p_e.getMessage());

            t_p_e.printStackTrace();

            t_p_e.showDialog("Warning!", JOptionPane.WARNING_MESSAGE);

        }

    }

    protected void createCrashLog() {

        try {

            final String filename = "Texture-Patcher-" + VERSION + " (" + new SimpleDateFormat("MM-dd-yyyy HH-mm-ss").format(new Date()) + ").log";

            final File file = new File(filename);

            final PrintWriter out = new PrintWriter(new FileWriter(file));

            for (final String log : logs) {

                out.println(log);

            }

            out.close();

            System.out.println("A crash log can be found at " + file.getAbsolutePath() + " !");

        } catch (final Exception e) {

            // Happens if an error occurs while create the crash log.

            final Texture_Patcher_Exception t_p_e = new Texture_Patcher_Exception(this, ErrorType.CRASH_LOG_CREATING_FAILED, e);

            t_p_e.printStackTrace();

        }

    }

    String getHTTPString(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    byte[] getFileBytes(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().bytes();
    }

    Response getHeadResp(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .method("HEAD", null)
                .build();

        Response response = client.newCall(request).execute();
        return response;
    }
}
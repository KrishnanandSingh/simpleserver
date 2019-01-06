package kn.server;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import kn.server.nt.ServerFrame;
import kn.server.util.ServerConfig;
import kn.server.util.Utility;

/**
 * @author krishnanand
 *
 */
public class ServerRunner implements ActionListener {
	private static Logger logger = Logger.getLogger(ServerRunner.class.getName());
	private static SimpleServer simpleServer;
	public MenuItem startItem = null;
	public MenuItem stopItem = null;

	public static SimpleServer getSimpleServer() {
		return simpleServer;
	}

	public static void setSimpleServer(SimpleServer simpleServer) {
		ServerRunner.simpleServer = simpleServer;
	}

	MenuItem getStartItem() {
		return startItem;
	}

	void setStartItem(MenuItem startItem) {
		this.startItem = startItem;
	}

	MenuItem getStopItem() {
		return stopItem;
	}

	void setStopItem(MenuItem stopItem) {
		this.stopItem = stopItem;
	}

	public static void main(String[] args) {
		String argument = null;
		boolean ntMode = false;
		if (args.length > 0) {
			argument = args[0];
			if (argument.equalsIgnoreCase("-nt")) {
				System.out.println("Server will start in no tray mode.");
				ntMode = true;
			} else if (argument.equalsIgnoreCase("-h") || argument.equalsIgnoreCase("--help")) {
				{
					System.out.println("Start with no arg for tray mode");
					System.out.println("**************OR***************");
					System.out.println("-nt   :   for no tray mode");
				}
			}
		}
		ServerRunner serverRunner = new ServerRunner();
		simpleServer = new SimpleServer();
		try {
			serverRunner.startUpServer(ntMode);
		} catch (AWTException e) {
			logger.log(Level.SEVERE, "Shutting down");
			logger.log(Level.SEVERE, Utility.getStackTrace(e));
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Shutting down");
			logger.log(Level.SEVERE, Utility.getStackTrace(e));
		} catch (Exception e) {
			logger.log(Level.SEVERE, Utility.getStackTrace(e));
		}
	}

	public void startUpServer(boolean ntMode) throws UnsupportedEncodingException, AWTException {
		if (!SystemTray.isSupported()) {
			logger.log(Level.WARNING, "SystemTray is not supported on this platform");
			logger.log(Level.WARNING, "Starting in non tray mode");
			this.startUpNTmode();
		} else {
			if (!ntMode) {
				this.startUpTrayMode();
			} else {
				this.startUpNTmode();
			}
		}
		addServerToShutdownHook();
	}

	private void startUpNTmode() {
		Runnable runner = new Runnable() {
			public void run() {
				String pathToFileOnDisk = ServerConfig.getString("Server.ntIcon");
				ImageIcon img = new ImageIcon(pathToFileOnDisk);
				ServerFrame serverRunner = new ServerFrame(simpleServer);
				serverRunner.setIconImage(img.getImage());
			}
		};
		EventQueue.invokeLater(runner);
	}

	private void startUpTrayMode() throws AWTException, UnsupportedEncodingException {
		PopupMenu popMenu = new PopupMenu();
		this.startItem = new MenuItem("Start");
		this.stopItem = new MenuItem("Stop");
		MenuItem exit = new MenuItem("Exit");

		exit.addActionListener(this);
		ActionListener startListener = new StartActionListner(simpleServer, this);
		startItem.addActionListener(startListener);
		ActionListener stopListener = new StopActionListner(simpleServer, this);
		stopItem.addActionListener(stopListener);
		popMenu.add(startItem);
		popMenu.add(stopItem);
		popMenu.addSeparator();
		popMenu.add(exit);
		this.stopItem.setEnabled(false);
		String defaultEncoding = System.getProperty("file.encoding");
		URL icon_path = null;
		icon_path = getClass().getClassLoader().getResource(ServerConfig.getString("Server.trayicon"));
		TrayIcon trayIcon = null;
		if (icon_path != null) {
			logger.info("Fetching tray icon from: "+icon_path.getPath());
			Image img = Toolkit.getDefaultToolkit().getImage(icon_path);
			trayIcon = new TrayIcon(img, ServerConfig.getString("Server.name"), popMenu);
			trayIcon.setImageAutoSize(true);
			SystemTray.getSystemTray().add(trayIcon);
		}else{
			// icon not found
			logger.severe("Icon not found at: "+ServerConfig.getString("Server.trayicon"));
		}
	}

	/**
	 * Ensures that server is stopped even on Ctrl+C or JVM Shutdown
	 */
	private static void addServerToShutdownHook() {
		if (simpleServer != null) {
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					if (simpleServer.isStarted()) {
						try {
							simpleServer.stop();
						} catch (Exception e) {
							logger.log(Level.SEVERE, Utility.getStackTrace(e));
						}
					}
				}
			}, "Stop Server Hook"));
		}

	}

	public void actionPerformed(ActionEvent actionEvent) {
		// Listens for Exit event
		if (simpleServer.isStarted()) {
			try {
				simpleServer.stop();
			} catch (Exception e) {
				logger.log(Level.SEVERE, Utility.getStackTrace(e));
			}
		}
		System.exit(0);
	}
}
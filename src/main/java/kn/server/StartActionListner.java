package kn.server;

import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;

import kn.server.util.Utility;

/**
 * @author krishnanand
 *
 */
public class StartActionListner implements ActionListener {
	private static Logger logger = Logger.getLogger(StartActionListner.class.getName());
	private static SimpleServer simpleServer;
	private ServerRunner serverRunner;

	public StartActionListner(SimpleServer simpleServer, ServerRunner serverRunner) {
		StartActionListner.simpleServer = simpleServer;
		this.serverRunner = serverRunner;
	}

	public void actionPerformed(ActionEvent actionEvent) {

		synchronized (serverRunner) {
			try {
				if (simpleServer.isStopped()) {
					simpleServer.start();
				} else {
					logger.log(Level.WARNING, "Already running");
				}
				MenuItem start = (MenuItem) actionEvent.getSource();
				start.setEnabled(false);
				serverRunner.getStopItem().setEnabled(true);
			} catch (Exception e) {
				if (e.getCause() instanceof BindException) {
					logger.log(Level.SEVERE, "Port already in use, try running on different port");
				} else {
					logger.log(Level.SEVERE, Utility.getStackTrace(e));
				}
			}
		}
	}
}
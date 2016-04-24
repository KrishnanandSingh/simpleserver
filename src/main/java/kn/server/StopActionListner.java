package kn.server;

import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import kn.server.util.Utility;

/**
 * @author krishnanand
 *
 */
public class StopActionListner implements ActionListener {

	private static SimpleServer simpleServer;
	private ServerRunner serverRunner;
	private static Logger logger = Logger.getLogger(ServerRunner.class.getName());

	public StopActionListner(SimpleServer simpleServer, ServerRunner serverRunner) {
		StopActionListner.simpleServer = simpleServer;
		this.serverRunner = serverRunner;
	}

	public void actionPerformed(ActionEvent actionEvent) {
		synchronized (serverRunner) {
			if (simpleServer.isStarted()) {
				try {
					simpleServer.stop();
				} catch (Exception e) {
					logger.log(Level.SEVERE, Utility.getStackTrace(e));
				}
			} else {
				logger.log(Level.WARNING, "Not running");
			}
			MenuItem stop = (MenuItem) actionEvent.getSource();
			stop.setEnabled(false);
			serverRunner.getStartItem().setEnabled(true);
		}
	}
}
package kn.server.nt;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;

import kn.server.SimpleServer;
import kn.server.util.Utility;

/**
 * @author krishnanand
 *
 */
public class ServerStartStopActionListner implements ActionListener {
	private static Logger logger = Logger.getLogger(ServerStartStopActionListner.class.getName());
	private final SimpleServer simpleServer;

	public ServerStartStopActionListner(SimpleServer simpleServer) {
		this.simpleServer = simpleServer;
	}

	public void actionPerformed(ActionEvent actionEvent) {
		JButton btnStartStop = (JButton) actionEvent.getSource();
		if (simpleServer.isStarted()) {
			btnStartStop.setText("Stopping...");
			btnStartStop.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			try {
				simpleServer.stop();
			} catch (Exception e) {
				logger.log(Level.SEVERE, Utility.getStackTrace(e));
				btnStartStop.setText("Stop");
				btnStartStop.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			btnStartStop.setText("Start");
			btnStartStop.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} else if (simpleServer.isStopped()) {
			btnStartStop.setText("Starting...");
			btnStartStop.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			try {
				simpleServer.start();
				btnStartStop.setText("Stop");
				btnStartStop.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			} catch (Exception e) {
				if (e.getCause() instanceof BindException) {
					logger.log(Level.SEVERE, "Port already in use, try running on different port");
				} else {
					logger.log(Level.SEVERE, Utility.getStackTrace(e));
				}
				btnStartStop.setText("Start");
				btnStartStop.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
}
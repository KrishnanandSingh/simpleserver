package kn.server.nt;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;

import kn.server.SimpleServer;
import kn.server.util.ServerConfig;

/**
 * @author krishnanand
 *
 */
public class ServerFrame extends JFrame {
	private static Logger logger = Logger.getLogger(ServerFrame.class.getName());
	private static final long serialVersionUID = 1L;
	private JButton btnStartStop;

	public ServerFrame(final SimpleServer simpleServer) {
		super(ServerConfig.getString("Server.name"));

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.log(Level.WARNING, "System LookAndFeel  not supported");
			logger.log(Level.WARNING, "Continuing with default");
		}
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		btnStartStop = new JButton("Start");
		btnStartStop.addActionListener(new ServerStartStopActionListner(simpleServer));
		add(btnStartStop, BorderLayout.CENTER);
		setSize(200, 200);
		setVisible(true);
	}
}
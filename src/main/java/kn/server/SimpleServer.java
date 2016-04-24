package kn.server;

import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import kn.server.nt.ServerStartStopActionListner;
import kn.server.util.ServerConfig;
import kn.server.util.Utility;

/**
 * @author krishnanand
 *
 */
public class SimpleServer implements Container {
	private static Logger logger = Logger.getLogger(ServerStartStopActionListner.class.getName());
	private static Connection CONNECTION;
	private static boolean isStarted;

	public void handle(Request request, Response response) {
		try {
			PrintStream body = response.getPrintStream();
			long time = System.currentTimeMillis();

			response.setValue("Content-Type", "text/plain");
			response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			body.println("Hello World");
			body.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, Utility.getStackTrace(e));
		}
	}

	public SimpleServer() {

	}

	public synchronized void start() throws Exception {
		Container reqHandler = new SimpleServer();
		SocketProcessor server = new ContainerSocketProcessor(reqHandler);
		CONNECTION = new SocketConnection(server);
		int SERVER_PORT = Integer.parseInt(ServerConfig.getString("Server.port"));
		SocketAddress address = new InetSocketAddress(SERVER_PORT);
		CONNECTION.connect(address);
		isStarted = true;
	}

	public synchronized void stop() throws Exception {
		CONNECTION.close();
		isStarted = false;
	}

	public boolean isStarted() {
		return isStarted;
	}

	public boolean isStopped() {
		return !isStarted;
	}
}
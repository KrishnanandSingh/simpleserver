package kn.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.http.Part;
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
			String uri = request.getPath().getPath();
			if (uri.endsWith("uploadScript")) {
				System.out.println("Request Recieved");
				serveRequest(request);
				// uploadFile(request);
			} else {

				PrintStream body = response.getPrintStream();
				long time = System.currentTimeMillis();

				response.setValue("Content-Type", "text/plain");
				response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
				response.setDate("Date", time);
				response.setDate("Last-Modified", time);

				body.println("Hello World");
				body.close();
			}
			PrintStream body = response.getPrintStream();
			long time = System.currentTimeMillis();

			response.setValue("Content-Type", "text/plain");
			response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			body.println("Uplopaded World");
			body.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, Utility.getStackTrace(e));
		}
	}

	/*
	 * private void uploadFile(Request request) { // Create a factory for
	 * disk-based file items DiskFileItemFactory factory = new
	 * DiskFileItemFactory();
	 * 
	 * // Configure a repository (to ensure a secure temp location is used) File
	 * temp_dir_file = File.createTempFile("apache_commons", "upload"); File
	 * tmp_dir = temp_dir_file.getParentFile(); factory.setRepository(tmp_dir);
	 * 
	 * // Create a new file upload handler ServletFileUpload upload = new
	 * ServletFileUpload(factory); // Parse the request List<FileItem> items =
	 * upload.parseRequest(null); }
	 */

	public SimpleServer() {

	}

	public synchronized void start() throws Exception {
		SocketProcessor server = new ContainerSocketProcessor(this);
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

	public String serveRequest(Request request) {
		StringBuilder errBuff = new StringBuilder();
		String responseMessage = null;
		Part part = request.getPart("script");
		if (part != null) {
			// Create path components to save the file
			String uploadDirectoryPath = null;
			OutputStreamWriter out = null;
			try {
				String path = ClassLoader.getSystemClassLoader().getResource(".").getPath();
				logger.finest("path:" + path);
				String currPath = URLDecoder.decode(path, "UTF-8");
				logger.finest("currPath:" + currPath);

				uploadDirectoryPath = currPath;
				logger.finest("uploadDirectoryPath:" + uploadDirectoryPath);
				System.out.println(uploadDirectoryPath);
				File file = new File(uploadDirectoryPath + File.separator + part.getFileName());
				/*
				 * out = new OutputStreamWriter(new FileOutputStream(file));
				 * InputStream filecontent = part.getInputStream(); int read =
				 * 0; while ((read = filecontent.read()) != -1) {
				 * out.write(read); }
				 */

				DataInputStream d = new DataInputStream(part.getInputStream());

				DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));

				int count;
				while ((count = d.read()) != -1) {
					dout.write(count);
				}
				d.close();
				dout.close();

			} catch (UnsupportedEncodingException e) {
				errBuff.append("Couldn't upload file");
				logger.finest(e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				errBuff.append("Couldn't upload file");
				logger.finest(e.getMessage());
				e.printStackTrace();
			} finally {
				try {
					if (out != null)
						out.close();
					System.out.println("out closed");
				} catch (IOException e) {
				}
			}
			responseMessage = "file uploaded";
		} else {
			errBuff.append("provide script");
		}
		if (errBuff.length() != 0) {
			logger.info(errBuff.toString());
			responseMessage = "{\"error\": \"" + errBuff.toString() + "\"}";
		}
		return responseMessage;
	}

}
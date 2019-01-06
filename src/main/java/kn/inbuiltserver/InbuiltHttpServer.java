/**
 * 
 */
package kn.inbuiltserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import kn.server.util.ServerConfig;

/**
 * @author Krishnanand Singh
 * @version %I%
 */
public class InbuiltHttpServer {
	private static Logger LOGGER = LoggerFactory.getLogger(InbuiltHttpServer.class);
	private static HttpServer httpServer;
	private static String CONTEXT_PATH = "/mobiWatchAgent/android";

	/**
	 * @return the cONTEXT_PATH
	 */
	public static String getCONTEXT_PATH() {
		return CONTEXT_PATH;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InstantiationException
	 */
	public static void startServer() throws IOException, InstantiationException {
		try {
			// setting backlog to 0 will set default backlog for this platform
			int BACKLOG = Integer.parseInt(ServerConfig.getString("backlog"));
			int PORT = Integer.parseInt(ServerConfig.getString("port"));
			int MAX_SERVER_THREADS = Integer.parseInt(ServerConfig.getString("threads"));

			// create a server but do not bind to any address
			httpServer = HttpServer.create();
			// creating socket address at localhost and 8080
			InetSocketAddress serverAddress = new InetSocketAddress(PORT);
			// binding server to the given address and allowing max number of
			// connections to be queued before getting rejected
			httpServer.bind(serverAddress, BACKLOG);
			// create a fixed thread pool to execute requets in parallel and
			// with limit on max server threads
			Executor serverThreads = Executors.newFixedThreadPool(MAX_SERVER_THREADS);
			// binding this thread pool with server
			httpServer.setExecutor(serverThreads);
			// create a context
			HttpContext androidContext = httpServer.createContext(CONTEXT_PATH);

			InbuiltHttpServer mobiWatchHttpServer = new InbuiltHttpServer();
			// set handler to this context
			HttpHandler mobiWatchHandler = new FrontController();
			androidContext.setHandler(mobiWatchHandler);

			// set filter to this context
			Filter filter = mobiWatchHttpServer.new RequestFilter();
			androidContext.getFilters().add(filter);

			httpServer.start();

		} catch (IOException e) {
			System.err.println(e.getMessage() + ". exiting...");
		}

	}

	private class RequestFilter extends Filter {

		private String DESC = "Parses request parameters and adds to" + "exchange attributes as key value pairs";

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.net.httpserver.Filter#description()
		 */
		@Override
		public String description() {
			return DESC;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.net.httpserver.Filter#doFilter(com.sun.net.httpserver.
		 * HttpExchange, com.sun.net.httpserver.Filter.Chain)
		 */
		@Override
		public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
			try {
				if ("get".equalsIgnoreCase(exchange.getRequestMethod())) {
					parseGetRequestQuery(exchange);

				} else if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {

					// checking if post request is multipart. if not,
					// parse the request query
					// else leave unprocessed
					Headers requestHeaders = exchange.getRequestHeaders();
					List<String> types = requestHeaders.get("Content-type");

					boolean normalPostRequest = true;
					if (types != null) {
						for (String type : types) {
							String[] contentTypeHeaders = type.split(";");
							for (int i = 0; i < contentTypeHeaders.length; i++) {
								if (contentTypeHeaders[i].trim().equalsIgnoreCase("multipart/form-data")) {
									normalPostRequest = false;
									break;
								}
							}
						}
					}

					if (normalPostRequest) {
						parsePostRequestQuery(exchange);
					} else {
						// process multipart/form-data
						createPart(exchange);
					}
				}
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				LOGGER.debug(sw.toString());
			}
			// forward the filtered request to next filter in the list if any
			// otherwise to the handler associated with this context
			chain.doFilter(exchange);
		}

		/**
		 * @param exchange
		 * @throws IOException
		 */
		private void parsePostRequestQuery(HttpExchange exchange) throws IOException {
			InputStream is = exchange.getRequestBody();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			// as the query comes in a single line
			String query = br.readLine();
			if (query != null && query.length() > 0) {
				LOGGER.debug("Requested uri(POST): " + exchange.getRequestURI().getPath());
				// decoding the raw query
				String decodedQuery = URLDecoder.decode(query, "UTF-8");
				Map<String, String> parameters = parseQuery(decodedQuery);
				exchange.setAttribute("parameters", parameters);
			}
		}

		/**
		 * @param exchange
		 */
		private void parseGetRequestQuery(HttpExchange exchange) {
			String query = exchange.getRequestURI().getQuery();
			// check if this is not a queried request then ignore it
			if (query != null) {
				LOGGER.debug("Requested uri(GET): " + exchange.getRequestURI().getPath());
				Map<String, String> parameters = parseQuery(query);
				exchange.setAttribute("parameters", parameters);
			}
		}

		/**
		 * @param query
		 * @return
		 */
		private Map<String, String> parseQuery(String query) {
			int NAME_IDX = 0;
			int VALUE_IDX = 1;
			Map<String, String> parameters = new HashMap<String, String>();
			for (String param : query.split("&")) {
				String nameValuePairs[] = param.split("=");
				if (nameValuePairs.length > 1) {
					LOGGER.debug(nameValuePairs[NAME_IDX] + " = " + nameValuePairs[VALUE_IDX]);
					parameters.put(nameValuePairs[NAME_IDX], nameValuePairs[VALUE_IDX]);
				}
			}
			return parameters;
		}

		/**
		 * @param exchange
		 * @return
		 * @return
		 */
		private void createPart(HttpExchange exchange) {
			String boundary = getBoundary(exchange.getRequestHeaders());
			String prefix = "--";
			String suffix = "--";
			String startPart = prefix + boundary;
			String endPart = prefix + boundary + suffix;

			// requestBody is the input stream from the whole request body
			InputStream is = exchange.getRequestBody();
			InputStreamReader isr;
			BufferedReader br = null;
			String line = "";
			try {
				isr = new InputStreamReader(is, "ISO-8859-15");
				br = new BufferedReader(isr);
				boolean partStarted = false;
				boolean hasFileContents = false;
				Map<String, String> partHeader = new HashMap<String, String>();
				
				Part part = null;
				String partName = null;
				while ((line = br.readLine()) != null) {
					if (!partStarted) {
						if (line.equals(startPart)) {
							partStarted = true;
						}
					} else {
						// part has been started
						if (!hasFileContents) {
							// parse disposition header
							if (line.startsWith("Content-Disposition:")) {
								partHeader = getContentDispositionHeader(line);
								partName = partHeader.get("name");
								part = new Part();
							}
							if (line.startsWith("Content-Type:")) {
								partHeader.put("Content-Type", line.split(" ")[1]);
							}
							if (line.isEmpty()) {
								// after these headers an empty line
								// marks the begining of file contents
								if (part != null) {
									hasFileContents = true;
									part.setPartHeader(partHeader);

									LinkedList<Character> charbuff = new LinkedList<>();
									int c;
									while ((c = br.read()) != -1) {
										charbuff.add((char) c);
									}
									Iterator<Character> iter = charbuff.descendingIterator();
									int limit = endPart.length();
									// add 2 as there is an empty line between
									// the
									// filecontent
									// and the endpart i.e. '\r'+'\n'
									limit += 2;
									int i = 0;
									while (iter.hasNext() && i++ < limit) {
										iter.next();
										iter.remove();
									}

									Object[] fileContents = charbuff.toArray();
									part.setFileContents(fileContents);

									exchange.setAttribute(partName, part);

									break;
								}
							}
						}
					}
				}

			} catch (IOException e) {
				LOGGER.debug(e.getMessage());
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException e) {
				}
			}

		}

		private Map<String, String> getContentDispositionHeader(String line) {
			Map<String, String> headerMap = new HashMap<>();
			String[] dispositionHeader = line.split(" ");
			dispositionHeader = removeTrailIfExists(';', dispositionHeader);

			headerMap.put("Content-Disposition", dispositionHeader[1]);

			for (int i = 2; i < dispositionHeader.length; i++) {
				// 0 and 1 were 'Content-Disposition:'
				// and
				// 'form-data'
				// getting parameters from rest
				String key = null, value = null;
				key = dispositionHeader[i].split("=")[0];
				String str = dispositionHeader[i].split("=")[1];
				// str contains value prefixed and
				// suffixed
				// by '"'
				value = str.substring(1, str.length() - 1);
				headerMap.put(key, value);
			}
			return headerMap;
		}

		/**
		 * @param ch
		 * @param parentArr
		 * @return parent array with removed trailing ch
		 */
		private String[] removeTrailIfExists(char ch, String[] parentArr) {
			int i = 0;
			while (i < parentArr.length) {
				int endIndex = parentArr[i].lastIndexOf(ch);
				if (endIndex != -1)
					parentArr[i] = parentArr[i].substring(0, endIndex);
				i++;
			}
			return parentArr;
		}

		/**
		 * @param requestHeaders
		 * @return boundary <br>
		 *         outermost enclosing boundary of the multipart request
		 */
		private String getBoundary(Headers requestHeaders) {
			String boundary = null;
			List<String> types = requestHeaders.get("Content-type");
			boolean isMultipart = false;
			if (types != null) {
				for (String type : types) {
					// multipart/form-data, boundary=AaB03x
					String[] contentTypeHeaders = type.split(" ");
					for (int i = 0; i < contentTypeHeaders.length; i++) {
						// checking if its really multipart/form-data
						if (contentTypeHeaders[i].trim().equalsIgnoreCase("multipart/form-data,")
								|| contentTypeHeaders[i].trim().equalsIgnoreCase("multipart/form-data;")) {
							isMultipart = true;
							break;
						}
					}
					if (isMultipart) {
						String[] splitted = type.split("=");
						boundary = splitted[1];
						break;
					}
				}
			}
			return boundary;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		// stop in 1 sec
		httpServer.stop(1);
		super.finalize();
	}

}
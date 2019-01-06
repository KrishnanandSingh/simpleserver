/**
 * 
 */
package kn.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author krishnanand
 *
 */
public class Utility {

	/**
	 * prints stacktrace to string and returns that
	 * @param e
	 * @return String 
	 */
	public static String getStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionMessage = sw.toString();
		return exceptionMessage;
	}

}

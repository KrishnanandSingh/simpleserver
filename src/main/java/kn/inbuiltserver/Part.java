/**
 * 
 */
package kn.inbuiltserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author M1028099
 *
 */
/**
 * @author M1028099
 *
 */
public class Part {
	
	private Map<String, String> partHeader = new HashMap<String, String>();
	private Object[] fileContents;
	
	/**
	 * 
	 */
	public Part() {
		
	}
	
	/**
	 * contains these headers: <br> 
	 * <ul>
	 * 	<li>"Content-Disposition"
	 * 	<li>"Content-Type"
	 * 	<li> and other headers following "Content-Disposition" header(in the same line)
	 * </ul>
	 * @return the partHeader
	 */
	public Map<String, String> getPartHeader() {
		return partHeader;
	}

	/**
	 * @param partHeader the partHeader to set
	 */
	public void setPartHeader(Map<String, String> partHeader) {
		this.partHeader = partHeader;
	}

	/**
	 * @param fileContents the fileContents to set
	 */
	public void setFileContents(Object[] fileContents) {
		this.fileContents = fileContents;
	}

	/**
	 * @return the inputStream
	 */
	public InputStream getInputStream() {
		return new InputStream() {
			int head = 0;
			int tail = Part.this.fileContents.length;
			@Override
			public int read() throws IOException {
				if(head != tail){
					return (Character)fileContents[head++];
				}else{
					return -1;
				}
			}
		};
	}


}

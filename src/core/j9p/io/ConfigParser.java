
package j9p.io;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public abstract class ConfigParser extends DefaultHandler {

	private String lastLevel = "";
	private Stack<String> position = new Stack<String>();
	protected boolean done = false;

	public String getPosition () {
		StringBuffer buf = new StringBuffer();
		for (String ent : position) {
			buf.append ('/');
			buf.append (ent);
		}
		return buf.toString();
	}
	
	public boolean isDone () { return done; }
	
	public abstract void enterElement (String name, Attributes attrs) throws Exception;
	
	public void startElement (String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
		
		String name = ( "".equals( localName ) ) ? qName : localName;
		
		String pos = name;
		if (lastLevel.startsWith (name)) {
			int instCount = 1;
			String s = lastLevel.substring (name.length());
			if (s.length() > 0) {
				s = s.substring(1);
				try {
					instCount = Integer.parseInt (s);
				} catch (NumberFormatException e) { instCount = 1; }
			}
			pos += ":" + ++instCount; 
		}
		position.push (pos);
		lastLevel = "";
		
		//System.out.println ("Entering: " + name);
		try {
			enterElement (name, attrs);
		}
		// something went terribly wrong...
		catch (Exception e) {
			
			System.out.flush();
			System.err.println ("Exception starting " + getPosition() + " ...");
			
			// re-throw internal parser exception 
			if (e instanceof SAXException)
				throw (SAXException) e;
			
			// wrap other exceptions
			String msg = e.getMessage();
			throw new SAXException (msg);
		}
	}

	public abstract void leaveElement (String name) throws Exception;
	
	public void endElement (String namespaceURI, String localName, String qName) throws SAXException {
		
		String name = ( "".equals( localName ) ) ? qName : localName;
		
		//System.out.println ("Leaving: " + name);
		try {
			leaveElement (name);
			lastLevel = position.pop();
		}
		// something went terribly wrong...
		catch (Exception e) {

			System.err.println ("Exception finishing " + getPosition() + " ...");
			
			// re-throw internal parser exception 
			if (e instanceof SAXException)
				throw (SAXException) e;
			
			// wrap other exceptions
			String msg = e.getMessage();
			throw new SAXException (msg);
		}
	}
}

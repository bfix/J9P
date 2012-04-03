
//******************************************************************
//*   PGMID.        PARSER FOR XML AUTHENTICATION CONFIGURATION.   *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/04.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
//*                 AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.       *
//******************************************************************
//*                                                                *
//*  StyxLib: Java-based Styx server framework                     *
//*                                                                *
//*  Copyright (C) 2009-2012, Bernd R. Fix                         *
//*                                                                *
//*  This program is free software; you can redistribute it and/or *
//*  modify it under the terms of the GNU Lesser General Public    *
//*  License (LGPL) as published by the Free Software Foundation;  *
//*  either version 3 of the License, or (at your option) any      *
//*  later version.                                                *
//*                                                                *
//*  This program is distributed in the hope that it will be use-  *
//*  ful, but WITHOUT ANY WARRANTY; without even the implied       *
//*  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR       *
//*  PURPOSE. See the GNU General Public License for more details. *
//*                                                                *
//*  You should have received a copy of the GNU General Public     *
//*  Licenses along with this program; if not, see                 *
//*  <http://www.gnu.org/licenses/>.                               *
//*                                                                *
//******************************************************************

package j9p.auth;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import j9p.io.ConfigParser;


//=================================================================
/**
 * <p>Read authentication configuration from XML file.</p>
 * <p>The configuration can specify custom authentication protocol
 * handlers, that are used to handle custom authentication schemes.</p>
 * <p>Since the representation of an identity is scheme-specific, so
 * is the XML configuration. Handlers can therefore parse their
 * own XML configuration, so this parser delegates parser calls
 * to a handler instance.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AuthConfigParser extends ConfigParser {
	
	//=================================================================
	/*
	 * XML parse state attributes:
	 */
	private Authenticator	inst = Authenticator.getInstance();
	private String			proto = null;
	private String			domain = null;
	private ConfigParser	customParser = null;
	
	//=================================================================
	/**
	 * <p>Start of new Element in XML.</p>
	 * @param name String - element name
	 * @param attrs Attributes - element attributes
	 */
	public void enterElement (String name, Attributes attrs) throws Exception {
		
		// check for active custom parser
		if (customParser != null) {
			// delegate parsing to handler.
			customParser.enterElement (name, attrs);
			return;
		}
		//-------------------------------------------------------------
		// <Keyring ...>: List of identities
		//-------------------------------------------------------------
		if (name.equals ("Keyring")) {
			done = false;
			return;
		}
		//-------------------------------------------------------------
		// <Protocols ...>:  Custom protocol handler list
		//-------------------------------------------------------------
		if (name.equals ("Protocols")) {
			return;
		}
		//-------------------------------------------------------------
		// <Protocol ...>:  Custom protocol handler definition
		//-------------------------------------------------------------
		if (name.equals ("Protocol")) {
			// get attributes
			String type = attrs.getValue ("type");
			String clName = attrs.getValue ("class");
			// register protocol handler
			try {
				Class<?> cl = Class.forName (clName);
				AP_Generic.registerHandler (type, cl.asSubclass (AP_Generic.class));
				//Class<? extends AP_Generic> cl = (Class<? extends AP_Generic>) Class.forName (clName);
				//AP_Generic.registerHandler (type, cl);
			}
			catch (Exception e) {
				throw new SAXException ("Unknown authentication protocol class: " + clName);
			}
			return;
		}
		//-------------------------------------------------------------
		// <Identity ...>:  Identity data
		//-------------------------------------------------------------
		if (name.equals ("Identity")) {
			// read attributes
			proto = attrs.getValue ("proto");
			domain = attrs.getValue ("domain");

			// find matching handler:
			AP_Generic parser = AP_Generic.getInstance (proto);
			if (parser == null) 
				throw new SAXException ("Unknown authentication protocol: " + proto);
			parser.initParse (attrs);
			
			// parse custom config XML for handler
			customParser = parser;
			return;
		}
	}
	//=================================================================
	/**
	 * <p>End of Element in XML.</p>
	 * @param name String - element name
	 */
	public void leaveElement (String name) throws Exception {

		// check for active custom parser
		if (customParser != null) {
			// delegate parsing to handler.
			customParser.leaveElement (name);
			// check if handler is done-
			if (!customParser.isDone())
				return;
			
			// handle specific result.
			if (customParser instanceof AP_Generic) {
				AP_Generic handler = (AP_Generic) customParser;
				
				// we have a new identity for the given auth protocol:
				AuthProtocolHandler.Identity id = handler.getIdentity();
				
				// if no identity is associated with is protocol
				// it is anonymous (but counts as authenticated)
				if (id != null) {
					// register identity
					String idName = proto;
					if (domain != null)
						idName += "@" + domain;
					inst.addIdentity (idName, id);
					// assemble message
					StringBuffer buf = new StringBuffer();
					buf.append ("   Identity '");
					buf.append (id.getName());
					buf.append ("' registered for ");
					if (domain == null) {
						buf.append ("negotiation.");
					} else {
						buf.append ("'");
						buf.append (idName);
						buf.append ("'.");
					}
					System.out.println (buf.toString());
				}
			}
			// disable custom parsing
			customParser = null;
		}
		//-------------------------------------------------------------
		// </Keyring>
		//-------------------------------------------------------------
		if (name.equals ("Keyring")) {
			done = true;
			return;
		}
		//-------------------------------------------------------------
		// </Protocols>
		//-------------------------------------------------------------
		if (name.equals ("Protocols")) {
			return;
		}
		//-------------------------------------------------------------
		// </Protocol>
		//-------------------------------------------------------------
		if (name.equals ("Protocol")) {
			return;
		}
	}
	//=================================================================
	/**
	 * <p>Collect PCDATA between tags.</p>
	 * @param ch char[] - character array of data
	 * @param start int - offset into array
	 * @param length int - length of data 
	 */
	public void characters (char[] ch, int start, int length) throws SAXException {
		if (customParser != null)
			customParser.characters (ch, start, length);
	}
}

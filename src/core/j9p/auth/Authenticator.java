
//******************************************************************
//*   PGMID.        AUTHENTICATION PROCESS HANDLER.                *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/09.                                      *
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

import j9p.Message;
import j9p.auth.AuthProtocolHandler.Identity;
import java.io.FileInputStream;
import java.util.Hashtable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>An <b>Authenticator</b> handles multiple authentication schemes used
 * in Styx protocols. It returns a user credential that is later used to check
 * access permissions on namespace entries.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Authenticator {

	//=================================================================
	// static initialization and access methods.
	//=================================================================
	/**
	 * <p>Authenticator instance (singleton).</p>
	 */
	private static Authenticator inst = new Authenticator ();
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get access to (global) Authenticator instance.</p> 
	 * @return Authenticator - singleton instance
	 */
	public static Authenticator getInstance () {
		return inst;
	}
	
	//=================================================================
	/**
	 * <p>List of managed identities.</p>
	 */
	private Hashtable<String,Identity> identities = null;
	
	//=================================================================
	/**
	 * <p>Instantiate a new Authenticator (only used in static init).</p>
	 */
	private Authenticator () {
		identities = new Hashtable<String,Identity>();
	}
	
	//=================================================================
	/**
	 * <p>Add (server) identities to Authenticator.</p>
	 * @param name String - name of server (user@dom)
	 * @param id Identity - server identity
	 * @return boolean - successful operation?
	 */
	protected boolean addIdentity (String name, Identity id) {
		identities.put (name, id);
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get identity based on name.</p>
	 * @param name String - identity name
	 * @return Identity - associated instance
	 */
	public Identity getIdentity (String name) {
		return identities.get (name);
	}
	
	//=================================================================
	/**
	 * <p>Check if a message (usually the first message in a session)
	 * is a authentication protocol message.</p>
	 * @param msg StyxMessage - incoming message
	 * @return boolean - is a authentication message?
	 */
	public boolean isAuthMessage (Message msg) {
		// check fifth byte of message for 'newline' character
		boolean rc = (msg.getByte(4) == '\n');
		if (rc) {
			try {
				// check length field
				int dLen = Integer.parseInt(msg.getDirectString (0, 4));
				rc = (dLen == msg.size() - 5);
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		return rc;
	}

	//=================================================================
	/**
	 * <p>Read authentication data from XML configuration file.</p> 
	 * @param fname String - name of configuration file
	 * @return boolean - successful operation?
	 */
	public boolean readAuthConfig (String fname) {

		// parse configuration file.
		AuthConfigParser parser = new AuthConfigParser();
		try {
			// create reader on configuration file
			InputSource src = new InputSource (new FileInputStream (fname));
			XMLReader rdr = XMLReaderFactory.createXMLReader();
			rdr.setContentHandler (parser);
			rdr.parse (src);
			// report success.
			return true;
		}
		catch (Exception e) {
			// print any available exception info
			System.out.flush();
			String msg = e.getMessage();
			if (e != null)
				System.err.println ("***** " + msg);
			return false;
		}
	}

	//=================================================================
	/**
	 * <p>Test run for Authenticator.</p> 
	 * @param argv String[] - command line arguments.
	 */
	public static void main (String[] argv) {
		
		// access authenticator instance
		Authenticator auth = Authenticator.getInstance();
		
		// Read authentication data
		if (!auth.readAuthConfig (argv[0])) {
			System.out.println ("Auth config failed!");
			return;
		}
	}
}

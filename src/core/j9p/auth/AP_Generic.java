
//******************************************************************
//*   PGMID.        GENERIC AUTHENTICATION HANDLER (ABSTRACT BASE).*
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

import java.util.Hashtable;
import j9p.io.ConfigParser;


//=================================================================
/**
 * <p>Abstract base class for classes that implement the
 * AuthProtocolHandler interface to provide custom authentication
 * mechanism. The class also provide static helpers to instantiate
 * handlers by protocol name.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class AP_Generic extends ConfigParser implements AuthProtocolHandler {

	//=================================================================
	//	static section
	//=================================================================
	/**
	 * 	<p>List of registered handlers (can be extended overwritten
	 *  by definitions in the authentication configuration file).</p>
	 */
	private static Hashtable<String,Class<? extends AP_Generic>> handlers;
	/**
	 * <p>Names for built-in authentication handlers.</p>
	 */
	protected static String[] pNames = {
		"p9any", "p9sk1", "p9sk2", "inferno"
	};
	// static initializer
	static {
		// allocate new class list of (auth) protocol handlers.
		handlers = new Hashtable<String,Class<? extends AP_Generic>>();
		
		// set built-in handlers.
		handlers.put ("inferno", AP_Inferno.class);
		handlers.put ("p9any",   AP_p9any.class);
		handlers.put ("p9sk1",   AP_p9sk1.class);
		handlers.put ("p9sk2",   AP_p9sk2.class);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Register handler instance for protocol with given name.</p>
	 * <p>The implementing class must have been derived from AP_Generic.</p>
	 * <p>You can overwrite built-in handlers by using a built-in
	 * protocol name.</p>
	 * @param name String - named of handled protocol
	 * @param cl Class<?> - 
	 */
	public static void registerHandler (String name, Class<? extends AP_Generic> cl) {
		handlers.put (name, cl);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Return a new instance of a handler for given authentication
	 * protocol number. This only works for built-in handlers.</p>
	 * @param protoId int - internal protocol number
	 * @return AP_Generic - handler object
	 */
	public static AP_Generic getInstance (int protoId) {
		// check for valid argument range
		if (protoId < 0 || protoId >= pNames.length)
			return null;
		return getInstance (pNames [protoId]);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Is the given protocol supported?</p>
	 * @param protoName String - name of protocol
	 * @return boolean - protocol handler registered
	 */
	public static boolean isSupported (String protoName) {
		for (String s : pNames) {
			if (s.equals (protoName))
				return true;
		}
		return false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Return a new instance of a handler for given authentication
	 * protocol name.</p>
	 * @param protoName String - name of protocol
	 * @return AP_Generic - handler object
	 */
	public static AP_Generic getInstance (String protoName) {
		try {
			Class<?> cl = handlers.get (protoName);
			if (cl == null)
				return null;
			AP_Generic inst = (AP_Generic) cl.newInstance();
			return inst;
		}
		catch (Exception e) {
			return null;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Delay execution for some defined time.</p> 
	 * @param millis
	 */
	public static void sleep (long millis) {
		try { Thread.sleep (millis); } catch (InterruptedException e) { }
	}
	
	//=================================================================
	// Overridable methods.
	//=================================================================
	/**
	 * <p>Initialize protocol handler and use given identity to
	 * authenticate this instance to remote peer.</p> 
	 * @param id Identity - our identity
	 * @param asServer boolean - act as server side
	 * @return boolean - initialization successful?
	 */
	public abstract boolean init (Identity id, boolean asServer);
}

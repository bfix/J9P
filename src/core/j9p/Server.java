
//******************************************************************
//*   PGMID.        STYX NAMESPACE SERVER.                         *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/02.                                      *
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

package j9p;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.net.ServerSocket;

import j9p.auth.Authenticator;
import j9p.server.NamespaceManager;
import j9p.server.ServerSessionHandler;
import j9p.util.Args;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A Server provides a namespace server utilizing the 9P protocol (Inferno,
 * Plan9). The server configuration as well as the user-specific namespaces
 * are defined in an XML configuration file.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Server {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private ServerSessionHandler hdlr = null;
	private int port = 6666;
	private boolean forceAuth = false;
	
	//=================================================================
	/**
	 * <p>Constructor: Instantiate new Server</p>
	 * @param port int - Styx communication port
	 * @param maxConn int - number of max. client connections
	 * @param auth boolean - authentication required?
	 * @param mgr NamespaceManager - namespace manager of server
	 */
	protected Server (int port, int maxConn, boolean auth, NamespaceManager mgr) {
		// save port and flags
		this.port = port;
		forceAuth = auth;
		// allocate session handler (shared among sessions)
		hdlr = new ServerSessionHandler (maxConn, mgr);
	}
	
	//=================================================================
	/**
	 * <p>Run StyxServer.</p>
	 */
	protected void run () {
		try {
		 	// setup a server socket on port
			ServerSocket svr = new ServerSocket (port);
			System.out.println ("[StyxServer] server socket created on port " + svr.getLocalPort() + "...");
			
		 	// as long as there are possible clients...
			boolean active = true;
			while (active) {
				
				// wait for free slot
				if (!hdlr.isSessionAvailable()) {
					active = false;
					continue;
				}

				// wait for a client request and
				// spawn a client session.
				System.out.println ("[StyxServer] Listening for client request...");
				if (!hdlr.startSession (svr.accept (), forceAuth))
					System.out.println ("[StyxServer] Discarding client request...");
			}
			// close open connections.
			System.out.println ("[StyxServer] shutting down ...");
			svr.close ();
		}
		// something went terribly wrong.
		catch (Exception e) {
			System.out.println ("[StyxServer] exception caught: " + e.getMessage());
		}
	}
	
	//=================================================================
	/**
	 * 
	 * @param argv
	 */
	public static void main (String[] argv) {
		
		// punch welcome message
		System.out.println ("==================================");
		System.out.println ("J9P/StyxServer v" + Library.getVersion());
		System.out.println ("Author: " + Library.getAuthor());
		System.out.println ("==================================");
		System.out.println ();
		System.out.flush();
		
		// parse commandline options
		Args args = new Args (argv, "p:s:a:");
		int port = args.getWordOpt ("-p", 6666);
		int maxSessions = args.getWordOpt ("-s", 10);
		String authConfig = args.getStringOpt ("-a", null);
		boolean auth = (authConfig != null);
		
		int argc = args.getNumArgs();
		if (argc != 1) {
			System.err.println ("Only one positional argument (namespace config) allowed!");
			System.err.println ("Usage: StyxServer [-p <port>] [-s <maxSessions>] [-a <auth config>] <namespace config>");
			System.err.println ("Defaults: Port = 6666, maxSessions = 10");
			System.err.flush();
			return;
		}
		
		System.out.println ("Using port " + port + " for max. " + maxSessions + " concurrent sessions.");
		
		// read namespace configuration
		String nsConfig = args.getStringArg (0, null);
		NamespaceManager mgr = new NamespaceManager();
		System.out.println ("Reading namespace configurarations from '" + nsConfig + "'...");
		if (!mgr.readConfig (nsConfig)) {
			System.err.println ("Can't read namespace definitions from file");
			System.err.println (" '" + nsConfig + "' -- terminating...");
			System.err.flush();
			return;
		}

		// Read authentication data
		if (auth) {
			System.out.println ("Authentication MANDATORY - using '" + authConfig + "' as configuration data file...");
			if (!Authenticator.getInstance().readAuthConfig (authConfig)) {
				System.err.println ("Reading/parsing of authentication data failed!");
				System.err.print   ("Correct problem or re-start server without authentication (no '-a' option)");
				System.err.flush();
				return;
			}
		} else
			System.out.println ("Authentication SWITCHED OFF - not required for clients.");
		
		// instantiate new Styx server
		Server srv = new Server (port, maxSessions, auth, mgr);
		System.out.println ();
		System.out.println ("Now serving up to " + maxSessions + " sessions on port " + port + " ...");
		
		// run server
		srv.run ();
	}
}

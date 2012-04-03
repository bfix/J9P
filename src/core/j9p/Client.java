
//******************************************************************
//*   PGMID.        STYX NAMESPACE CLIENT.                         *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/11.                                      *
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

import java.net.InetSocketAddress;
import java.net.Socket;

import j9p.auth.Authenticator;
import j9p.client.ClientSessionHandler;
import j9p.util.Args;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Namespace client that connects to a 9P server.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Client {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private ClientSessionHandler hdlr = null;
	
	//=================================================================
	/**
	 * <p>Constructor: Instantiate new StyxClient</p>
	 */
	protected Client () {
	}

	//=================================================================
	/**
	 * <p>Run StyxServer.</p>
	 * <p>The protocol versions defined are: 0=Styx,1=9P,2=9P2000</p>
	 * @param srv InetSocketAddress - Styx server address
	 * @param auth boolean - authentication required?
	 * @param version int - requested protocol version
	 */
	protected void run (InetSocketAddress srv, boolean auth, int version) {
		try {
			// connect to server
			System.out.println ("[StyxClient] Connecting to server '" + srv + "'...");
			Socket s = new Socket (srv.getAddress(), srv.getPort());
	
			// allocate session handler (shared among sessions)
			hdlr = new ClientSessionHandler (1, version);
			hdlr.startSession (s, auth);
		}
		// something went terribly wrong.
		catch (Exception e) {
			System.out.println ("[StyxClient] exception caught: " + e);
		}
	}
	
	//=================================================================
	/**
	 * 
	 * @param argv
	 */
	public static void main (String[] argv) {
		
		// punch welcome message
		System.out.println ("==============================");
		System.out.println ("Client v0.1b, (c)2008-2012 >Y<");
		System.out.println ("==============================");
		System.out.println ();
		System.out.flush();
		
		// parse commandline options
		Args args = new Args (argv, "p:a:v!");
		int port = args.getWordOpt ("-p", 6666);
		String authData = args.getStringOpt ("-a", null);
		boolean auth = (authData != null);
		String vStr = args.getStringOpt ("-v", "2");
		
		// check for correct number of positional arguments.
		int argc = args.getNumArgs();
		if (argc != 1) {
			System.err.println ("Only one positional argument (server address) allowed!");
			System.err.println ("Usage: StyxClient [-p <port>] [-a] <server>");
			System.err.println ("Defaults: Port = 6666, no authentication");
			System.err.flush();
			return;
		}

		// check protocol version
		int version = (vStr.length() == 1 ? vStr.charAt(0) - '0' : -1);
		if (version < 0 || version > 2) {
			System.err.println ("No valid protocol version [0|1|2] specified -- terminating...");
			System.err.flush();
			return;
		}
		
		// get server address.
		String serverAddr = args.getStringArg (0, null);
		if (serverAddr == null) {
			System.err.println ("No valid server address available -- terminating...");
			System.err.flush();
			return;
		}

		// Read authentication data
		Authenticator.getInstance().readAuthConfig (authData);

		// instantiate new Styx client
		Client client = new Client ();
		InetSocketAddress srv = new InetSocketAddress (serverAddr, port);

		// run client
		client.run (srv, auth, version);
	}
}

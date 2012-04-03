
//******************************************************************
//*   PGMID.        STYX SESSION IMPLEMENTATION.                   *
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

package j9p.server;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.net.Socket;

import j9p.auth.AP_Generic;
import j9p.auth.AuthProtocolHandler;
import j9p.auth.Authenticator;
import j9p.auth.Credential;
import j9p.auth.AuthProtocolHandler.Identity;
import j9p.crypto.SecureChannel;
import j9p.proto.V1;
import j9p.proto.V2;
import j9p.Message;
import j9p.Session;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>ServerSession</b> is an object representing the connection of a
 * client to the served namespace. A session is (normally) ended by a client
 * or a  connection timeout.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerSession extends Session {

	//=================================================================
	/*
	 * Attributes:
	 */
	protected NamespaceManager nsMgr = null;
	
	//=================================================================
	/**
	 * <p>Constructor: Instantiate a new client session
	 * @param socket Socket - socket to communication channel
	 * @param needAuth boolean - authentication required?
	 * @param hdlr ServerSessionHandler - reference to session handler
	 * @param nsMgr NamespaceManager - reference to namespace manager
	 * @throws IOException - channel failure
	 */
	public ServerSession (
		Socket socket, boolean needAuth,
		ServerSessionHandler hdlr, NamespaceManager nsMgr
	) throws IOException {
		super (socket, hdlr);
		this.nsMgr = nsMgr;
		this.useAuth = needAuth;
	}

	//=================================================================
	/**
	 *  <p>Run the established session with the client.</p>
	 */
	public void run () {
		// Access Authenticator instance
		Authenticator auth = Authenticator.getInstance();
		try {
			// peek into first message
			Message msgIn = comm.peekNextMessage();
			if (msgIn == null)
				return;
				
			// check for authentication protocol
			if (auth.isAuthMessage (msgIn)) {
				
				// initialize protocol handler
				AP_Generic authHandler = AP_Generic.getInstance ("inferno");
				if (authHandler == null)
					throw new Exception ("Can't handle authentication protocol 'inferno'");
				
				// manage authentication process
				int rc = authHandler.process (comm);
				if (rc == AuthProtocolHandler.AUTH_NOT_SUPPORTED)
					throw new Exception ("authentication protocol 'inferno' not supported.");
				if (rc == AuthProtocolHandler.AUTH_SUCCESS)
					userCred = authHandler.getPeerCredential();
				if (userCred != null) {
					// successful authentication.
					// initialize channel security
					SecureChannel secureCh = userCred.getSecureChannel ();
					if (secureCh != null && secureCh.isSecured())
						comm.wrapWith (secureCh);
				}
				// can't complete mandatory authentication
				else if (useAuth)
					throw new Exception ("Can't handle mandatory authentication");
			}
			// check credential
			if (userCred == null) {
				// we have no authenticated credential, so we create
				// an "unsafe" (empty) dummy. This will be set with
				// specific values during a call to "auth()/attach()"
				userCred = new Credential (null);
			}

			// get next message
			msgIn = comm.getNextMessage();
			
			// check for protocol version message
			int type = V2.isCompatible (msgIn);
			if (type == V2.Tversion) {
				// Protocol version '9P2000':
				// instantiate protocol implementation
				delegate = new ServerProtocol_V2 (nsMgr, useAuth);
			}
			else if (V1.isCompatible (msgIn) > 0) {
				// Protocol version '9P' or 'Styx'
				// instantiate protocol implementation
				delegate = new ServerProtocol_V1 (nsMgr, useAuth);
			}
			
			// handle messages until the session is closed.
			active = true;
			while (active) {
				// valid message received?
				if (msgIn != null) {
					// handle message in its own thread:
					Operation op = new Operation (msgIn);
					op.start();
				}
				// get next incoming message
				msgIn = comm.getNextMessage();
			}
		}
		// something went terribly wrong.
		catch (Exception e) {
			System.out.println ("[StyxSession " + id + "] exception caught: " + e.getMessage());
			e.printStackTrace();
		}
		// in the end, my friend...
		finally {
			// drop internal references.
			if (delegate != null)
				delegate.reset();
			// close the sockets.
			try {
				socket.close ();
			} catch (Exception e) {}
		}
		// notify session handler
		hdlr.stopping (id);
	}
}

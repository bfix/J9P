
//******************************************************************
//*   PGMID.        STYX CLIENT SESSION.                           *
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

package j9p.client;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.net.Socket;
import java.io.IOException;
import j9p.Session;
import j9p.crypto.SecureChannel;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Run a <b>ClientSession</b> with a namespace server.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ClientSession extends Session {
	
	//=================================================================
	/**
	 * <p>Reference to actual protocol handler instance.</p>
	 */
	protected int version = -1;
	protected ClientProtocol handler = null;
	
	//=================================================================
	/**
	 * <p>Instantiate a new client session with a namespace server.</p>
	 * <p>The protocol versions defined are: 0=Styx,1=9P,2=9P2000</p>
	 * @param s Socket - communication channel
	 * @param forceAuth boolean - authentication required?
	 * @param hdlr ClientSessionHandler - session manager
	 * @param version int - requested protocol version
	 * @throws IOException - communication failure
	 */
	public ClientSession (Socket s, boolean forceAuth, ClientSessionHandler hdlr, int version) throws IOException {
		super (s, hdlr);
		useAuth = forceAuth;
		
		// instantiate delegation object based on version.
		this.version = version;
		if (version == 0 || version == 1)
			handler = new ClientProtocol_V1();
		else if (version == 2)
			handler = new ClientProtocol_V2();
		
		// give handler access to the communication channel
		handler.setChannel (comm);
	}
	
	//=================================================================
	/**
	 * <p>Run client session.</p>
	 */
	public void run () {
		try {
			// handle authentication process.
			if (version == 0 && useAuth) {
				// Access Authenticator instance
				//Authenticator auth = Authenticator.getInstance();
				// authenticate (to) server
				//userCred = auth.authenticate (comm, false, null);
				if (userCred == null)
					throw new Exception ("authentication failed!");
				SecureChannel secureCh = userCred.getSecureChannel ();
				if (secureCh != null && secureCh.isSecured()) {
					System.out.println ("Switching on channel security...");
					comm.wrapWith (secureCh);
				}
			}
			
			if (version == 2) {
				// start with version negotiation.
				Type.Version peer = handler.negotiateVersion ("9P2000", 2048+23);
				System.out.println (peer.version + ", msize=" + peer.msize);
				
				// start authentication
				userCred = handler.authenticate ("test", "");
			}
		}
		catch (Exception e) {
			System.err.println ("[StyxClient] exception caught -- " + e.getMessage());
			return;
		}
	}
}

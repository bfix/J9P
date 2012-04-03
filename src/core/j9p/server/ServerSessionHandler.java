
//******************************************************************
//*   PGMID.        STYX SERVER SESSION HANDLER IMPLEMENTATION.    *
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
import j9p.SessionHandler;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>SessionHandler</b> is an extended NamespaceManager that also
 * handles server sessions and limits the maximum number of open
 * connections.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerSessionHandler extends SessionHandler {
	
	protected NamespaceManager nsMgr = null;
	
	//=================================================================
	/**
	 * <p>Instantiate new SessionHandler and limit number of sessions.</p>
	 * @param max int - maximum number of concurrent sessions
	 */
	public ServerSessionHandler (int max, NamespaceManager mgr) {
		super (max);
		nsMgr = mgr;
	}
	
	//=================================================================
	/**
	 * <p>Start a StyxSession on socket.</p>
	 * @param s Socket - socket for communication
	 * @param needAuth boolean - authentication required?
	 * @return boolean - StyxSession up and running
	 */
	public boolean startSession (Socket s, boolean needAuth) {
		try {
			// start session on socket
			ServerSession session = new ServerSession (s, needAuth, this, nsMgr);
			System.out.println ("[StyxServer] Processing client request '" + session.getSessionId() + "'.");
			session.start();
			numSessions++;
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
}

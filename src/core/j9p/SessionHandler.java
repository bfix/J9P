
//******************************************************************
//*   PGMID.        STYX SESSION ABSTRACT BASE HANDLER.            *
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

import java.net.Socket;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>SessionHandler</b> handles and monitors 9P sessions. It
 * limits the maximum number of open connections between peers.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class SessionHandler {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected int		maxSessions;	// maximum number of concurrent sessions
	protected int		numSessions;	// current number of open sessions
	protected int		id;				// counter for id generation
	protected Object	semaphore;		// semaphore for wait operations

	//=================================================================
	/**
	 * <p>Instantiate new SessionHandler and limit number of sessions.</p>
	 * @param max int - maximum number of concurrent sessions
	 */
	public SessionHandler (int max) {
		semaphore = new Object();
		maxSessions = max;
		numSessions = 0;
		id = 0;
	}
	
	//=================================================================
	/**
	 * <p>Start a Session on socket.</p>
	 * @param s Socket - socket for communication
	 * @param needAuth boolean - authentication required?
	 * @return boolean - StyxSession up and running
	 */
	public abstract boolean startSession (Socket s, boolean needAuth);
	
	//=================================================================
	/**
	 * <p>Session with given id is finished.</p>
	 * @param id int - session id
	 */
	public void stopping (int id) {
		System.out.println ("[StyxSession] Session '" + id + "' finished.");
		
		// decrement session counter.
		synchronized (semaphore) {
			numSessions--;
			semaphore.notify();
		}
	}
	
	//=================================================================
	/**
	 * <p>Get next session id.</p>
	 * @return int - session id
	 */
	public int getNextId () {
		return ++id;
	}
	
	//=================================================================
	/**
	 * <p>Check if session is available. This call blocks if the
	 * maximum number of open sessions is reached.</p>
	 * @return boolean - session available?
	 */
	public boolean isSessionAvailable () {
		// limit reached?
		if (numSessions < maxSessions)
			// no: go ahead.
			return true;
		
		// wait for open slot. 
		synchronized (semaphore) {
			try {
				semaphore.wait();
				return true;
			}
			catch (InterruptedException e) {
				return false;
			}
		}
	}
}

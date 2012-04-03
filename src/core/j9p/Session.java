
//******************************************************************
//*   PGMID.        STYX SESSION ABSTRACT BASE CLASS.              *
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import j9p.auth.Credential;
import j9p.io.StreamChannel;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>Session</b> is an object representing the connection between
 * a client and a server. A session is (normally) ended by a client or a
 * connection timeout.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class Session extends Thread {
	
	//=================================================================
	/**
	 * <p>Inner interface for Styx protocol version handlers.</p>
	 */
	public static interface Protocol {
		
		//-------------------------------------------------------------
		/**
		 * <p>Process a Styx message for user with given credential.</p>
		 * @param id int - session identifier
		 * @param in Message - incoming message from client
		 * @param cr Credential - user credential
		 * @return Message - reply message for client
		 */
		Message process (int id, Message in, Credential cr);
		
		//-------------------------------------------------------------
		/**
		 * <p>Peek into Message and return the tag of the operation.</p>
		 * @param in Message - message to be inspected
		 * @return int - tag of operation in message
		 */
		int getTag (Message in);
		
		//-------------------------------------------------------------
		/**
		 * <p>Check if message is a "flush()" operation that
		 * will terminate another pending operation.</p>
		 * @param in Message - message to be inspected
		 * @return int - tag of flushed operation (or 0)
		 */
		int isFlushing (Message in);

		//-------------------------------------------------------------
		/**
		 * <p>Pretty-print a message.</p>
		 * @param in Message - message to be printed
		 * @return String - printable Styx message
		 */
		String toString (Message in);

		//-------------------------------------------------------------
		/**
		 * <p>Reset protocol handler instance.</p> 
		 */
		void reset ();
	}
	
	//=================================================================
	/**
	 * <p>Every operation triggered by an incoming styx message will
	 * run in its own thread with happens to be an instance of
	 * the inner class Operation.</p> 
	 */
	protected class Operation extends Thread {
		
		//-------------------------------------------------------------
		/**
		 * <p>Incoming message to be processed.</p>
		 */
		private Message msgIn = null;
		
		//-------------------------------------------------------------
		/**
		 * <p>Construct a new Operation instance.</p> 
		 * @param in Message - message to be processed
		 */
		public Operation (Message in) {
			msgIn = in;
		}
		//-------------------------------------------------------------
		/**
		 * <p>Run operation thread: Process message and send reply.</p>
		 */
		public void run () {
			
			// get tag of operation and register thread.
			int tag = delegate.getTag (msgIn);
			synchronized (pendingOps) {
				pendingOps.put (tag, this);
			}
			// check for "flush()" operations that
			// require special handling.
			int oldTag = delegate.isFlushing (msgIn);
			if (oldTag > 0) {
				// terminate operation "oldTag"
				synchronized (pendingOps) {
					// get associated thread
					Thread thOp = pendingOps.get (oldTag);
					if (thOp != null) {
						// terminate operation thread
						pendingOps.remove (oldTag);
						Thread killer = thOp;
					    thOp = null;
					    killer.interrupt();
					}
				}
			}
			
			// handle message and write response. This make
			// take some time and can be "interrupted" by a
			// flush() operation on behalf of the client.
			try {
				Message msgOut = delegate.process (id, msgIn, userCred);
				if (msgOut != null)
					comm.sendMessage (msgOut);
			} catch (IOException e) { }
			
			// we are about to finish.
			synchronized (pendingOps) {
				pendingOps.remove (tag);
			}
		}
	}

	//=================================================================
	/*
	 * Attributes:
	 */
	protected int id;						// id of the process.
	protected Socket socket;				// reference to socket.
	protected Channel comm;					// communication channel
	protected SessionHandler hdlr;			// session handler
	protected Protocol delegate = null;		// protocol implementation
	protected boolean active = false;		// is current connection active?

	protected boolean useAuth = false;		// authentication required?
	protected Credential userCred = null;	// user credential (after auth)
	
	protected Hashtable<Integer,Thread> pendingOps = null;	// list of pending operations.
	

	//=================================================================
	/**
	 * <p>Constructor: Instantiate a new session
	 * @param socket Socket - socket to communication channel
	 * @param hdlr SessionHandler - reference to session handler
	 * @throws IOException - channel failure
	 */
	public Session (Socket socket, SessionHandler hdlr) throws IOException {
		// initialize attributes:
		this.socket = socket;
		this.id = hdlr.getNextId();
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		//socket.setSoTimeout (5000);
		comm = new StreamChannel (is, os);
		this.hdlr = hdlr;
		pendingOps = new Hashtable<Integer,Thread>(); 
	}
	
	//=================================================================
	/**
	 * <p>Get id of this session.</p>
	 * @return int - session id
	 */
	public int getSessionId() {
		return id;
	}

	//=================================================================
	/**
	 *  <p>Run the established session with the client.</p>
	 */
	public abstract void run ();
}

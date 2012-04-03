
//******************************************************************
//*   PGMID.        STYX COMMUNICATION CHANNEL IMPLEMENTATION.     *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/23.                                      *
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

package j9p.io;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import j9p.Message;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>StreamChannel</b> is a communication path between a 9P
 * server and client based on input and output streams.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class StreamChannel extends StackableChannel {

	//=================================================================
	/*
	 * Constants.
	 */
	private static final int	BUF_SIZE	= 16384;
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected byte[]		buffer;			// message buffer
	protected InputStream	is;				// session input
	protected OutputStream	os;				// session output
	protected Message		pending;		// pending message (from peek)
	
	//=================================================================
	/**
	 * <p>Constructor: create channel from two streams.</p>
	 * @param is InputStream - stream to read from
	 * @param os OutputStream - stream to write to
	 */
	public StreamChannel (InputStream is, OutputStream os) {
		// set stack references
		top = this;
		next = this;
		// set stream references
		this.is = is;
		this.os = os;
		buffer = new byte [BUF_SIZE];
		pending = null;
	}
	
	//=================================================================
	//	Communication methods: Directly implemented (no further
	//	indirection (wrapper)). 
	//=================================================================
	/**
	 * <p>Get next message from client.</p>
	 * @return Message - received message (or null)
	 * @throws IOException
	 */
	public Message getNextMessage () throws IOException {
		
		// handle layered channels.
		if (top != this && !isNested (NEST_GET)) {
			// flag traversal.
			setNesting (NEST_GET);
			// yes: call method on top-level channel
			Message msgIn = top.getNextMessage();
			clearNesting (NEST_GET);
			return msgIn;
		}
		// check for pending message (from peeking)
		if (pending != null) {
			Message msgIn = pending;
			pending = null;
			return msgIn;
		}

		// read incoming message from stream
		int avail = is.read (buffer);
		if (avail < 1)
			return null;
		
		// construct Styx message
		Message msgIn = new Message (buffer, avail);
		return msgIn;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next message by peeking into the buffer; the same
	 * message is return by a subsequent 'getNextMessage()' call.</p> 
	 * @return Message - next message in queue (or null)
	 * @throws IOException
	 */
	public Message peekNextMessage () throws IOException {
		
		// handle layered channels.
		if (top != this && !isNested (NEST_PEEK)) {
			// flag traversal.
			setNesting (NEST_PEEK);
			// yes: call method on top-level channel
			Message msgIn = top.peekNextMessage();
			clearNesting (NEST_PEEK);
			return msgIn;
		}

		// check for previous peek.
		if (pending != null)
			return pending;
		
		// get next message as pending
		Message msgIn = getNextMessage();
		pending = msgIn;
		return msgIn;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check if channel has pending message (already read, but
	 * not fetched by 'getNextMessage()'.</p>
	 * @return boolean - message pending?
	 */
	public boolean hasPendingMessage () {
		return pending != null;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Sent response message to client.</p>
	 * @param msg Message - service reply
	 * @return boolean - successful operation?
	 * @throws IOException
	 */
	public boolean sendMessage (Message msg) throws IOException {
		
		// handle layered channels.
		if (top != this && !isNested (NEST_SEND)) {
			// flag traversal.
			setNesting (NEST_SEND);
			// yes: call method on top-level channel
			boolean rc = top.sendMessage(msg);
			clearNesting (NEST_SEND);
			return rc;
		}
		
		// send out message to peer
		byte[] data = msg.asByteArray(true);
		os.write (data);
		os.flush();
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Close channel.</p>
	 * @throws IOException 
	 */
	public void close () throws IOException {
		
		// handle layered channels.
		if (top != this && !isNested (NEST_CLOSE)) {
			// flag traversal.
			setNesting (NEST_CLOSE);
			// yes: call method on top-level channel
			top.close();
			clearNesting (NEST_CLOSE);
			return;
		}

		// close streams
		is.close();
		os.close();
	}

	//=================================================================
	//	Low-level access to channel
	//=================================================================
	/**
	 * <p>Read bytes from channel into buffer and return number of
	 * read bytes.</p>
	 * @param buffer byte[] - data buffer
	 * @return int - number of bytes read
	 * @throws IOException
	 */
	public int readBytes (byte[] buffer) throws IOException {
		return is.read (buffer);
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write section of buffer to channel.</p>
	 * @param buffer byte[] - data buffer
	 * @throws IOException
	 */
	public void writeBytes (byte[] buffer) throws IOException {
		os.write (buffer);
	}
}

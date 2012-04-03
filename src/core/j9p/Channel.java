
//******************************************************************
//*   PGMID.        STYX CHANNEL INTERFACE.                        *
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

package j9p;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>Channel</b> is a communication path between a 9P/Styx
 * server and client. Channels can be stacked/unstacked to establish
 * multiple layered transformations on incoming and outgoing messages.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public interface Channel {

	//=================================================================
	//  Wrapper methods. 
	//=================================================================
	/**
	 * Wrap this instance of Channel with another instance. The
	 * wrapping instance is exposed to the using side and uses the
	 * wrapped channel as transport mechanism.</p>
	 * @param ch Channel - wrapping channel (new outer layer)
	 * @return boolean - wrapping successful?
	 */
	boolean wrapWith (Channel ch);

	//-----------------------------------------------------------------
	/**
	 * <p>Peel of Channel layer around this instance.</p> 
	 * @return Channel - former wrapping channel (outer layer).
	 */
	Channel peelOff ();

	//-----------------------------------------------------------------
	/**
	 * <p>Check if we are a wrapped instance.</p> 
	 * @return boolean - channel is nested
	 */
	boolean isWrapped ();

	//=================================================================
	//	Communication methods
	//=================================================================
	/**
	 * <p>Get next message from client.</p>
	 * @return Message - received message (or null)
	 * @throws IOException
	 */
	Message getNextMessage () throws IOException;
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get next message by peeking into the buffer; the same
	 * message is return by a subsequent 'getNextMessage()' call.</p> 
	 * @return Message - next message in queue (or null)
	 * @throws IOException
	 */
	Message peekNextMessage () throws IOException;
	
	//-----------------------------------------------------------------
	/**
	 * <p>Check if channel has pending message (already read, but
	 * not fetched by 'getNextMessage()'.</p>
	 * @return boolean - message pending?
	 */
	boolean hasPendingMessage ();
	
	//-----------------------------------------------------------------
	/**
	 * <p>Sent response message to client.</p>
	 * @param msg Message - service reply
	 * @return boolean - successful operation?
	 * @throws IOException
	 */
	boolean sendMessage (Message msg) throws IOException;
	
	//-----------------------------------------------------------------
	/**
	 * <p>Close channel.</p>
	 * @throws IOException
	 */
	void close () throws IOException;

	//=================================================================
	//	Low-level access to channel.
	//	This by-passes all wrapping instances and accesses the basic
	//	(raw) data associated with the bottom channel. Channels throw
	//	an IOException if they can't handle this.
	//=================================================================
	/**
	 * <p>Read bytes from channel into buffer and return number of
	 * read bytes.</p>
	 * @param buffer byte[] - data buffer
	 * @return int - number of bytes read
	 * @throws IOException
	 */
	int readBytes (byte[] buffer) throws IOException;
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write section of buffer to channel.</p>
	 * @param buffer byte[] - data buffer
	 * @throws IOException
	 */
	void writeBytes (byte[] buffer) throws IOException;
}

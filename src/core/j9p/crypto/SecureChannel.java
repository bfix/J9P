
//******************************************************************
//*   PGMID.        STYX STREAM CHANNEL NTERFACE.                  *
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

package j9p.crypto;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import j9p.Message;
import j9p.io.StackableChannel;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>SecureChannel</b> utilizes cryptographic methods to ensure the
 * secrecy (encryption) and integrity (digest) of a Styx message send
 * or received along an encapsulated Channel.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class SecureChannel extends StackableChannel {

	//=================================================================
	/*
	 * Attributes:
	 */
	protected ChannelDigester	hasher;		// HMAC algorithm
	protected ChannelCipher		encrypter;	// encryption worker
	protected ChannelCipher		decrypter;	// decryption worker
	protected int				countIn;	// message counter (incoming)
	protected int				countOut;	// message counter (outgoing)
	protected Message			pending;	// pending message (peeking)

	//=================================================================
	/**
	 * <p>Construct a secure channel that can act as a wrapper for
	 * other, existing and opened channels.</p> 
	 * @param secret byte[] - shared secret
	 * @param algs String - algorithm definitions
	 * @throws NoSuchAlgorithmException 
	 */
	public SecureChannel (byte[] secret, String algs)
		throws NoSuchAlgorithmException
	{
		// reset counters
		countIn = countOut = 0;
		
		String digAlg = null;
		String encAlg = null;
		pending = null;
		
		// parse definition string
		int pos = algs.indexOf ('/');
		if (pos < 0) {
			// we have only one algorithm: find out
			// whether it is a digest or encryption alg.
			if (ChannelDigester.isSupported (algs))
				// we have a digester but no cipher
				digAlg = algs;
			else if (ChannelCipher.isSupported (algs)) {
				// we have a cipher but no digester
				encAlg = algs;
			}
		} else {
			// split the definition string
			digAlg = algs.substring (0, pos);
			if (!ChannelDigester.isSupported(digAlg))
				throw new NoSuchAlgorithmException (digAlg);
			encAlg = algs.substring (pos+1);
			if (!ChannelCipher.isSupported(encAlg))
				throw new NoSuchAlgorithmException (encAlg);
		}
		
		// instantiate worker objects
		if (digAlg != null)
			hasher = new ChannelDigester (digAlg, secret);
		if (encAlg != null) {
			encrypter = new ChannelCipher (encAlg, secret, true);
			decrypter = new ChannelCipher (encAlg, secret, false);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Secure channel set up?</p> 
	 * @return boolean - uses channel security
	 */
	public boolean isSecured () {
		return (hasher != null || encrypter != null);
	}
	
	//=================================================================
	//	Communication methods
	//=================================================================
	/**
	 * <p>Get next message from client.</p>
	 * <p>Decrypt/check incoming message from secure channel.</p>
	 * @return Message - received message (or null)
	 * @throws IOException
	 */
	public Message getNextMessage () throws IOException {
		
		// check for pending message from peek.
		if (pending != null) {
			Message msg = pending;
			pending = null;
			return msg;
		}
		
		// read incoming message from wrapped channel
		Message msg = next.getNextMessage();
		if (msg == null)
			return null;

		// get length of message
		int size = (msg.getByte() << 8) | msg.getByte();
		// check for padding (block ciphers)
		int padding = 0;
		if ((size & 0x8000) == 0)
			padding = msg.getByte();
		else
			size &= 0x7FFF;

		// extract message data.
		byte[] data = msg.getArray (size);
		
		//-------------------------------------------------------------
		// check for decryption
		//-------------------------------------------------------------
		if (decrypter != null) {
			// decrypt message
			data = decrypter.process (data);
			if (data == null)
				throw new IOException ("Decryption failed!");
			// remove padding.
			if (padding > 0)
				size -= padding;
		}
		
		//-------------------------------------------------------------
		// check for message digest
		//-------------------------------------------------------------
		if (hasher != null) {
			// check digest
			int pos = hasher.checkMessage (data, size, countIn);
			if (pos < 0)
				throw new IOException ("Hash mismatch");
			// move message to begin of buffer
			size -= pos;
			System.arraycopy (data, pos, data, 0, size);
		}

		// construct Styx message from data
		countIn++;
		Message msgIn = new Message (data, size);
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
		
		// check for previous peek.
		if (pending != null)
			return pending;
		
		// get next message
		Message msgIn = getNextMessage();
		if (msgIn == null)
			return null;
		
		// save message
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
	 * <p>Encrypt/protect Message for transmission on secure channel.</p>
	 * @param msg Message - service reply
	 * @return boolean - successful operation?
	 * @throws IOException
	 */
	public boolean sendMessage (Message msg) throws IOException {
		
		// get raw data of message
		byte[] data = msg.asByteArray(true);
		int size = data.length;
		int padding = 0;

		//-------------------------------------------------------------
		// check for message digest.
		//-------------------------------------------------------------
		if (hasher != null) {
			// get HMAC and message
			data = hasher.digestMessage (data, countOut);
			size = data.length;
		}
		
		//-------------------------------------------------------------
		// check for encryption.
		//-------------------------------------------------------------
		if (encrypter != null) {
			// encrypt data
			byte[] newData = encrypter.process (data);
			if (newData == null) {
				System.out.println ("Encryption failed!");
				return false;
			}
			// compute padding.
			int newSize = newData.length;
			padding = newSize - size;
			// adjust references
			data = newData;
			size = newSize;
		}
		// assemble outgoing message
		Blob b = new Blob();
		if (padding == 0) {
			b.putByte ((size >> 8) | 0x80);
			b.putByte (size & 0xFF);
		} else {
			b.putByte ((size >> 8));
			b.putByte (size & 0xFF);
			b.putByte (padding);
		}
		b.putArray (data);
		data = b.asByteArray(false);
		size = data.length;
		
		// send message
		countOut++;
		msg = new Message (data, size);
		return next.sendMessage (msg);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Close channel.</p>
	 * @throws IOException 
	 */
	public void close () throws IOException {
		next.close();
	}

	//=================================================================
	//	Low-level access to channel not possible.
	//=================================================================
	/**
	 * <p>Read bytes from channel into buffer and return number of
	 * read bytes.</p>
	 * @param buffer byte[] - data buffer
	 * @return int - number of bytes read
	 * @throws IOException
	 */
	public int readBytes (byte[] buffer) throws IOException {
		throw new IOException ("no low-level access.");
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write section of buffer to channel.</p>
	 * @param buffer byte[] - data buffer
	 * @throws IOException
	 */
	public void writeBytes (byte[] buffer) throws IOException {
		throw new IOException ("no low-level access.");
	}
}

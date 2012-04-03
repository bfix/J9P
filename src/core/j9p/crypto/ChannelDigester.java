
//******************************************************************
//*   PGMID.        STYX COMM CHANNEL DIGESTER (HMAC).             *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/17.                                      *
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A ChannelDigester computes (and checks) the HMAC data for messages send
 * over a secure Styx communication channel.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ChannelDigester {

	//=================================================================
	//	Static attributes and methods.
	//=================================================================
	/*
	 * Static declarations for supported message digests.
	 * (Mapping between Styx and Java algorithm names) 
	 */
	private static final String[] algs     = { "sha",  "md4", "md5" };
	private static final String[] algNames = { "SHA1", "MD4", "MD5" };
	
	//-----------------------------------------------------------------
	/**
	 * <p>Check if a digest algorithm is supported.</p>
	 * @param alg String - algorithm name
	 * @return boolean - algorithm supported?
	 */
	public static boolean isSupported (String alg) {
		return getAlgId (alg) != -1;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get list index of digest algorithm (or -1 if the algorithm
	 * is not supported).</p>
	 * @param alg String - algorithm name
	 * @return int - list index of algorithm (or -1)
	 */
	private static int getAlgId (String alg) {
		for (int n = 0; n < algs.length; n++)
			if (algs[n].equals (alg))
				return n;
		return -1;
	}

	//=================================================================
	/*
	 * Attributes:
	 */
	private MessageDigest worker = null;	// worker instance
	private byte[] secret = null;			// shared secret
	
	//=================================================================
	/**
	 * <p>Instantiate a new ChannelDigester with given algorithm.</p>
	 * @param alg String - algorithm name
	 * @param secret byte[] - shared secret
	 * @throws NoSuchAlgorithmException 
	 */
	public ChannelDigester (String alg, byte[] secret) throws NoSuchAlgorithmException {
		// save shared secret
		this.secret = secret.clone();
		
		// get index of algorithm
		int idx = getAlgId (alg);
		if (idx == -1)
			throw new NoSuchAlgorithmException("Unknown algorithm: " + alg);
		// set reference to worker instance
		worker = MessageDigest.getInstance (algNames [idx]);
	}
	
	//=================================================================
	/**
	 * <p>Check HMAC of incoming message. The message occupies the
	 * first 'size' bytes of 'msg' and has the counter 'msgNum'.
	 * The digest includes the shared secret from authentication.</p> 
	 * @param msg byte[] - message buffer
	 * @param size int - size of incoming message
	 * @param msgNum int - counter of incoming message
	 * @return int - start position of Styx message in incoming data
	 */
	public int checkMessage (byte[] msg, int size, int msgNum) {
		
		// digest message.
		int pos = worker.getDigestLength();
		int msgSize = size - pos;
		worker.reset();
		feed (secret);
		feed (msg, pos, msgSize);
		feed (msgNum);
		
		// check digest
		byte[] hash = worker.digest ();
		int count = hash.length;
		for (int n = 0; n < count; n++)
			if (hash[n] != msg[n])
				return -1;
		return pos;
	}
	
	//=================================================================
	/**
	 * <p>Digest message and prefix the message with HMAC data.</p>
	 * @param msg byte[] - outgoing message
	 * @param msgNum int - counter of outgoing message
	 * @return byte[] - new message data
	 */
	public byte[] digestMessage (byte[] msg, int msgNum) {
		
		// digest message.
		worker.reset();
		feed (secret);
		feed (msg);
		feed (msgNum);
		byte[] hash = worker.digest ();
		
		// construct complete message (HMAC + data).
		int payloadSize = hash.length + msg.length;
		byte[] res = new byte [payloadSize];
		System.arraycopy (hash, 0, res, 0, hash.length);
		System.arraycopy (msg, 0, res, hash.length, msg.length);
		return res;
	}

	//=================================================================
	//	Helper methods.
	//=================================================================
	/**
	 * <p>Digest section of byte array.</p>
	 * @param data byte[] - data buffer
	 * @param pos int - start position of section
	 * @param length int - size of section
	 */
	protected void feed (byte[] data, int pos, int length) {
		worker.update (data, pos, length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Digest complete byte array.</p>
	 * @param data byte[] - data buffer
	 */
	protected void feed (byte[] data) {
		feed (data, 0, data.length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Digest big-endian message count.</p>
	 * @param val int - message count
	 */
	protected void feed (int val) {
		worker.update ((byte)((val >> 24) & 0xFF));
		worker.update ((byte)((val >> 16) & 0xFF));
		worker.update ((byte)((val >>  8) & 0xFF));
		worker.update ((byte)( val        & 0xFF));
	}
}

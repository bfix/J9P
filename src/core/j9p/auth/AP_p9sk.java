
//******************************************************************
//*   PGMID.        P9SK BASE AUTHENTICATION PROTOCOL HANDLER.     *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/09.                                      *
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

package j9p.auth;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.xml.sax.Attributes;
import j9p.Channel;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 *  <p>Abstract base class for 'p9sk1' and 'p9sk2' protocol handlers.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class AP_p9sk extends AP_p9any {
	
	//=================================================================
	/*
	 * Constants for process states:
	 */
	protected static final int STATE_INITIAL	=  1; 
	protected static final int STATE_TICKET		=  2; 
	protected static final int STATE_DONE		=  3; 
	/*
	 * Message type constants (see <authsrv.h>)
	 */
	protected static final int	AuthTreq		=  1;	// ticket request
	/*
	 * encrypted message types: 
	 */
	protected static final int	AuthTs			= 64;	// ticket encrypted with server's key
	protected static final int	AuthTc			= 65;	// ticket encrypted with client's key
	protected static final int	AuthAs			= 66;	// server generated authenticator
	protected static final int	AuthAc			= 67;	// client generated authenticator
	/*
	 * Field and record length constants:
	 */
	protected static final int LEN_ANAME		= 28;
	protected static final int LEN_AUTHDOM		= 48;
	protected static final int LEN_CHALL		=  8;
	protected static final int LEN_DESKEY		=  7;
	
	protected static final int LEN_TS			=  2*LEN_ANAME + LEN_CHALL + LEN_DESKEY + 1;
	protected static final int LEN_AS			=  LEN_CHALL + 5;
	protected static final int LEN_AC			=  LEN_CHALL + 5;
	
	//=================================================================
	/**
	 * <p>A (virtual) identity assigned to 'p9any'.</p> 
	 */
	protected static abstract class Id implements Identity {
		// Attributes:
		protected String	name = null;		// name of identity (user)
		protected String	domain = null;		// authdom
		protected String	password = null;	// user password
		//-------------------------------------------------------------
		/**
		 * <p>Get name of identity.</p>
		 * @return String - name of identity
		 */
		public String getName()			{ return name; }
		//-------------------------------------------------------------
		/**
		 * <p>Get name of authentication protocol.</p>
		 * @return String - name of authentication protocol
		 */
		public abstract String getAuthProtocol();
	}

	//=================================================================
	//=================================================================
	/*
	 * Attributes:
	 */
	protected Id			id = null;		// associated identity
	protected Credential	peerCr = null;	// authenticated credential
	protected int			skVersion = 1;	// defaults to p9sk1
	
	//-----------------------------------------------------------------
	/**
	 * <p>Hidden constructor: Handler instances are returned by
	 * calling 'AP_Generic.getInstance(...)'.</p>
	 */
	protected AP_p9sk () {
		rdState = STATE_INITIAL;
		wrState = STATE_INITIAL;
		isServer = false;
		id = null;
		peerCr = null;
		skVersion = 1;
		try {
			// instantiate cipher instance
			enc = Cipher.getInstance ("DES/ECB/NoPadding");
		} catch (Exception e) {
			enc = null;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Initialize protocol handler and use given identity to
	 * authenticate this instance to remote peer.</p> 
	 * @param idIn Identity - our identity
	 * @param asServer boolean - act as server side
	 * @return boolean - initialization successful?
	 */
	public boolean init (Identity idIn, boolean asServer) {
		
		isServer = asServer;
		if (idIn instanceof Id) {
			id = (Id) idIn;
			user = id.name;
			password = id.password;
			domain = id.domain;
			return true;
		}
		return false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get name of handled authentication protocol.</p>
	 * @return String - name of authentication protocol 
	 */
	public abstract String getProtocolName ();

	//-----------------------------------------------------------------
	/**
	 * <p>Get the peer credential after a successful authentication.</p>
	 * @return Credential - authenticated peer credential (or null)
	 */
	public Credential getPeerCredential() {
		return peerCr;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get associated identity.</p>
	 * <p>When called during an authentication process, the identity
	 * used to initialize the handler is returned. If called during
	 * configuration setup, the identity defined in the configuration
	 * is returned.</p>
	 * @return Identity - assigned/defined identity (or null)
	 */
	public abstract Identity getIdentity ();

	//=================================================================
	//	AuthProtocolHandler process methods
	//	(see also 'man 6 authsrv' in Plan9)
	//=================================================================
	/**
	 * <p>Handle authentication on an established channel.</p>
	 * <p>The handler will take over the channel as long as the
	 * authentication process is running; the J9P framework will not
	 * see (or handle) any traffic on the channel until this call
	 * terminated. This traffic format is authentication protocol
	 * specific.</p>
	 * <p>If a handler does not support direct communication
	 * with the peer, it should return AUTH_NOT_SUPPORTED. The
	 * handler can only used in P9ANY-based negotiations using
	 * the Tauth message type.</p>
	 * <p>The result of the authentication process is returned
	 * (either AUTH_FAILED or AUTH_SUCCESS).  
	 * @param ch Channel - established connection
	 * @return int - processing mode
	 * @throws IOException - communication failure
	 */
	public int process (Channel ch) throws IOException {
		return AUTH_NOT_SUPPORTED;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>The peer is requesting data, so the handler must return
	 * binary data to be send to the peer.</p> 
	 * @param data Blob - blob to receive data (stream mode)
	 * @return int - processing state (AUTH_???)
	 * @throws IOException - communication failure
	 */
	public int getDataForPeer (Blob data) throws IOException {
		
		int rc = AUTH_CONTINUE;
		
		switch (rdState) {
			//---------------------------------------------------------
			// initial state:
			//---------------------------------------------------------
			case STATE_INITIAL: {
				// generate challenge
				generateChallenge();
				
				if (isServer) {
					// use server challenge for peer (p9sk2)
					if (skVersion == 2)
						System.arraycopy (challenge, 0, peerChallenge, 0, LEN_CHALL);
					
					// generate ticket request
					byte[] ticket = createTicketRequest (AuthTreq, id.name, id.domain);
					data.putArray (ticket);
				}
				// as client:
				else {
					if (skVersion == 1)
						// client: send challenge
						data.putArray (challenge);
				}
				rdState = STATE_TICKET;
			} break;
			
			//---------------------------------------------------------
			// handle ticketing
			//---------------------------------------------------------
			case STATE_TICKET: {
				if (isServer) {
					// generate authenticator to proof we know the
					// shared key (encrypted with shared key).
					byte[] raw = createAuthenticator (AuthAs);
					data.putArray (raw);
					
					// assemble credential and signal success
					peerCr = new Credential (idRequested);
					peerCr.addGroup (idRequested);
					peerCr.setSecret (sharedKey);
					rc = AUTH_SUCCESS;
				}
				rdState = STATE_DONE;
			} break;
		}
		return rc;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>The peer is requesting data, so the handler must return
	 * binary data to be send to the peer.</p> 
	 * @param data Blob - blob to receive data (stream mode)
	 * @return int - processing state (AUTH_???)
	 * @throws IOException - communication failure
	 */
	public int handlePeerData (Blob data) throws IOException {
		
		switch (wrState) {
			//---------------------------------------------------------
			// initial state:
			//---------------------------------------------------------
			case STATE_INITIAL:
				if (isServer) {
					if (skVersion == 1)
						// server: extract peer challenge
						peerChallenge = data.asByteArray (true);
				}
				else {
					// client: parse ticket request @@@
				}
				wrState = STATE_TICKET;
				break;
			//---------------------------------------------------------
			// handle ticket (request/response)
			//---------------------------------------------------------
			case STATE_TICKET:
				if (isServer) {
					// extract ticket data and handle ticket.
					byte[] authTsData = data.getArray (LEN_TS);
					int rc = handleTicket (authTsData, id.password);

					// check for correct record type
					// and successful operation
					if (rc != AuthTs) {
						info = "unknown server key";
						rc = -1;
					}
					if (rc < 0)
						return AUTH_FAILED;

					// extract and handle authenticator.
					byte[] authAcData = data.getArray (LEN_TS, LEN_AC);
					rc = handleAuthenticator (authAcData);

					// check for correct record type
					if (rc != AuthAc) {
						info = "invalid authenticator";
						rc = -1;
					}
					if (rc < 0)
						return AUTH_FAILED;

					// everything is fine.
					wrState = STATE_DONE;
				}
				break;
		}

		return AUTH_CONTINUE;
	}
	//=================================================================
	// Ticket-related methods
	//=================================================================
	/*
	 * Attributes:
	 */
	protected byte[]	challenge = null;			// our challenge
	protected byte[]	peerChallenge = null;		// challenge send by peer
	protected String	idClient = null;			// peer identifier
	protected String	idRequested = null;			// peer identifier on this server
	
	//-----------------------------------------------------------------
	/**
	 * <p>Generate challenge.</p>
	 */
	protected void generateChallenge () {
		// generate challenge
		SecureRandom rnd = new SecureRandom();
		challenge = new byte [LEN_CHALL];
		rnd.nextBytes(challenge);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Generate a ticket request (server-side).</p> 
	 * @param type int - request type (AuthTreq, AuthChal)
	 * @param name String - name of server
	 * @param domain String - server domain
	 * @return byte[] - generated ticket
	 */
	protected byte[] createTicketRequest (int type, String name, String domain) {

		// server: generate ticket request
		// <type>, IDs, DN, CHs, -, -
		Blob data = new Blob();
		data.putByte (type);
		data.putString (name, LEN_ANAME);		// server's encryption id
		data.putString (domain, LEN_AUTHDOM);	// server's authentication domain
		data.putArray (challenge);				// challenge from server
		data.putString ("", LEN_ANAME);			// host's encryption id
		data.putString ("", LEN_ANAME);			// uid of requesting user on host
		return data.asByteArray (false);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Create encrypted authenticator (AuthAs, AuthAc).</p>
	 * @param type
	 * @return
	 */
	protected byte[] createAuthenticator (int type) {
		Blob data = new Blob();
		data.putByte (type);
		data.putArray (peerChallenge);
		data.putInt (0);
		byte[] raw = data.asByteArray (false);
		
		// encrypt authenticator with shared key
		encrypt (sharedKey, raw, 0, raw.length);
		return raw;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Handle ticket information: extract user identification
	 * (client,requested) and shared secret (key).</p> 
	 * @param ticket byte[] - ticket data
	 * @param password String - password to decrypt ticket
	 * @return int - ticket type or error code (<0)
	 */
	protected int handleTicket (byte[] ticket, String password) {
		
		// decrypt ticket
		byte[] keyData = convertPasswordToKey (password);
		decrypt (keyData, ticket, 0, ticket.length);
		int type = ticket[0];
		
		// check for matching challenge
		for (int n = 0; n < LEN_CHALL; n++)
			if (ticket[n+1] != challenge[n]) {
				info = "challenge mismatch";
				return -1;
			}
		
		// extract shared secret and user info
		Blob b = new Blob (ticket);
		int pos = LEN_CHALL+1;
		idClient = b.getString (pos, LEN_ANAME);
		pos += LEN_ANAME;
		idRequested = b.getString (pos, LEN_ANAME);
		pos += LEN_ANAME;
		sharedKey = b.getArray (pos, LEN_DESKEY);
		return type;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Handle encrypted authenticator.</p> 
	 * @param authenticator byte[] - authenticator data
	 * @return int - authenticator type or error code (<0)
	 */
	protected int handleAuthenticator (byte[] authenticator) {
		
		// decrypt authenticator
		decrypt (sharedKey, authenticator, 0, authenticator.length);
		int type = authenticator[0];
		
		// check for matching challenge
		for (int n = 0; n < LEN_CHALL; n++)
			if (authenticator[n+1] != challenge[n]) {
				info = "challenge mismatch";
				return -1;
			}

		// check for reset counter (replay attack)
		for (int n = 0; n < 4; n++)
			if (authenticator[n+LEN_CHALL+1] != 0) {
				info = "possible replay attack";
				return -1;
			}
		// return type of authenticator.
		return type;
	}

	//=================================================================
	//	Cryptographic methods.
	//=================================================================
	/*
	 * Attributes: 
	 */
	protected Cipher enc = null;			// cipher engine (DES/ECB)
	protected byte[] sharedKey = null;		// shared key from ticket
	//-----------------------------------------------------------------
	/**
	 * <p>Decrypt block of data using DES in EBC mode.</p>
	 * @param keyData byte[] - DES key (7 or 8 bytes)
	 * @param data byte[] - data to be decrypted
	 * @param pos int - start position
	 * @param count int - number of bytes to process
	 * @return boolean - successful?
	 */
	protected boolean decrypt (byte[] keyData, byte[] data, int pos, int count) {
		
		// check for cipher instance and minimum data size
		if (enc == null || count < 8)
			return false;

		// make sure we are using 8 byte keys
		if (keyData.length == 7)
			keyData = expandKey (keyData);
		
		try {
			// instantiate decryption worker
			SecretKeySpec key = new SecretKeySpec (keyData, "DES");
			AlgorithmParameterSpec spec = null;
			enc.init (Cipher.DECRYPT_MODE, key, spec);

			// split into 7 byte blocks
			count--;
			int numLeft = count % 7;
			int numBlks = count / 7;
			
			// decrypt last block first
			if (numLeft > 0) {
				int p2 = pos + count - 7;
				byte[] out = enc.update (data, p2, 8);
				System.arraycopy (out, 0, data, p2, 8);
			}
			// decrypt other blocks (descending)
			int p2 = pos + 7 * numBlks;
			for (int n = 0; n < numBlks; n++) {
				p2 -= 7;
				byte[] out = enc.update (data, p2, 8);
				System.arraycopy (out, 0, data, p2, 8);
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Encrypt data with key using DES/ECB.</p> 
	 * @param keyData byte[] - DES key (7 or 8 bytes)
	 * @param data byte[] - data block
	 * @param pos int - start position
	 * @param count int - number of bytes to process
	 * @return boolean - successful?
	 */
	protected boolean encrypt (byte[] keyData, byte[] data, int pos, int count) {
		
		// check for cipher instance and minimum data size
		if (enc == null || count < 8)
			return false;
		
		// make sure we are using 8 byte keys
		if (keyData.length == 7)
			keyData = expandKey (keyData);
		
		try {
			// instantiate encryption worker
			SecretKeySpec key = new SecretKeySpec (keyData, "DES");
			AlgorithmParameterSpec spec = null;
			enc.init (Cipher.ENCRYPT_MODE, key, spec);

			// split data into 7 bytes blocks
			count--;
			int numLeft = count % 7;
			int numBlk = count / 7;
			// process blocks
			for (int n = 0; n < numBlk; n++) {
				byte[] out = enc.update (data, pos, 8);
				System.arraycopy (out, 0, data, pos, 8);
				pos += 7;
			}
			// process last incomplete block
			if (numLeft > 0) {
				pos =  count - 7;
				byte[] out = enc.update (data, pos, 8);
				System.arraycopy (out, 0, data, pos, 8);
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert password to 56-bit DES key</p>
	 * @param password String - password string
	 * @return byte[] - DES-56 key
	 */
	protected byte[] convertPasswordToKey (String password) {

		// check for minimum length
		if (password == null)
			return null;
		int count = password.length(); 
		if (count < 8)
			// fill up with blanks
			password += "         ".substring (count);
		
		// convert password to binary data
		byte[] pw = password.getBytes();
		count = pw.length;
		int pos = 0;
		
		// allocate key
		byte[] key = new byte [7];
		
		// process complete password...
		for (;;) {
			// mangle key from next password data block
			for (int n = 0; n < 7; n++) {
				int p1 = pw[pos+n]   & 0xFF; p1 >>= n;
				int p2 = pw[pos+n+1] & 0xFF; p2 <<= (7 - n);
				key[n] = (byte)(p1 + p2);
			}
			// address next password block
			pos += 8;
			if (pos == count)
				return key;
			int pending = count - pos;
			if (pending < 8)
				pos -= 8 - pending;
			
			// encrypt next block of password data
			encrypt (key, pw, pos, 8);
		} 
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert 7-byte DES key to internal representation (8 bytes).</p> 
	 * @param key byte[] - key data
	 * @return byte[] - transformed key
	 */
	protected byte[] expandKey (byte[] key) {
		
		// combine to long
		long val = 0;
		for (int n = 0; n < 7; n++) {
			val <<= 8;
			val |= (key[n] & 0xFF);
		}
		// split to bytes
		byte[] out = new byte [8];
		for (int n = 8; n > 0; n--) {
			byte b = (byte)(val & 0x7F);
			out[n-1] = (byte)(b << 1);
			val >>= 7;
			// compute and set parity
			boolean parity = true;
			for (int j = 0; j < 8; j++)
				if ((b & (1 << j)) != 0)
					parity = !parity;
			if (parity)
				out[n-1] |= 1;
		}
		// return expanded key
		return out;
	}
	
	//=================================================================
	//	XML parser for configuration data
	//=================================================================
	/*
	 * Attributes:
	 */
	protected String	user = null;
	protected String	password = null;
	protected String	domain = null;

	//-----------------------------------------------------------------
	/**
	 * <p>Initialize handler with attributes of the &lt;Identity .../&gt;
	 * element.</p>
	 * @param attrs Attributes - list of attributes
	 * @return boolean - initialization successful?
	 */
	public boolean initParse (Attributes attrs) {
		// get user credentials
		user = attrs.getValue ("user");
		password = attrs.getValue ("password");
		domain = attrs.getValue ("domain");
		// do we act as server?
		isServer = "server".equals(attrs.getValue("role"));
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Start of new Element in XML.</p>
	 * @param name String - element name
	 * @param attrs Attributes - element attributes
	 */
	public void enterElement (String name, Attributes attrs) throws Exception {
		// no internal elements
	}

	//=================================================================
	/**
	 * <p>End of Element in XML.</p>
	 * @param name String - element name
	 */
	public void leaveElement (String name) throws Exception {
		
		//-------------------------------------------------------------
		// </Identity>
		//-------------------------------------------------------------
		if (name.equals ("Identity")) {
			// we have finished parsing custom data
			done = true;
			return;
		}
	}
}

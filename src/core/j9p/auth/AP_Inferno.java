
//******************************************************************
//*   PGMID.        INFERNO AUTHENTICATION HANDLER.                *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/04.                                      *
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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Vector;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import j9p.Channel;
import j9p.Message;
import j9p.io.ContentHandler;
import j9p.util.Blob;


//=================================================================
/**
 * <p>Inferno authentication scheme: Used by Inferno before
 * sending any 9P/Styx messages to authenticate (and protect) a
 * connection between peers.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AP_Inferno extends AP_Generic {

	//=================================================================
	/*
	 * Constants for verify results:
	 */
	protected static final int VERIFY_OK				=  0;
	protected static final int VERIFY_EXPIRED			=  1;
	protected static final int VERIFY_HASH_MISMATCH		=  2;
	protected static final int VERIFY_FAILED			=  3;
	/*
	 * Constants for internal processing state
	 */
	protected static final int STATE_VERSION			=  1;
	protected static final int STATE_AUTH_DATA			=  2;
	protected static final int STATE_AUTH_DATA2			=  3;
	protected static final int STATE_AUTH_DATA3			=  4;
	protected static final int STATE_KEY_EXCHANGE		=  5;
	protected static final int STATE_ACK_OK				=  6;
	protected static final int STATE_ALGORITHMS			=  7;
	protected static final int STATE_DONE				=  8;

	//=================================================================
	/**
	 * <p>A identity assigned with the server/client instance
	 * is used to authenticate a peer.</p> 
	 */
	public static class Id implements Identity {
		// Attributes:
		protected String			name = null;	// name of identity
		protected String			proto = null;	// protocol name
		protected Certificate		certCA = null;	// signer certificate
		protected Certificate		cert = null;	// identity certificate
		protected PrivateKey		prvKey = null;	// identity private key
		protected DH_KeyExchange	dhParam = null;	// DH key exchange
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
		public String getAuthProtocol()	{ 
			return proto;
		}
	}

	//=================================================================
	/*
	 * Involved identities
	 */
	private Id			id = null;					// "our" identity
	private Credential	peerCr = null;				// peer credential (on success)
	
	//=================================================================
	//=================================================================
	/**
	 * <p>Hidden constructor: Handler instances are returned by
	 * calling 'AP_Generic.getInstance(...)'.</p>
	 */
	protected AP_Inferno () {
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
		
		// check if we have a n identity of correct type
		if (idIn == null || !(idIn instanceof Id))
			return false;
		id = (Id) idIn;
		
		// setup process attributes:
		dh = id.dhParam.clone();
		c0 = id.cert;
		s0 = dh.init();
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get name of handled authentication protocol.</p>
	 * @return String - name of authentication protocol 
	 */
	public String getProtocolName () {
		return "inferno";
	}
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
	 * <p>Get identity associated identity.</p>
	 * <p>When called during an authentication process, the identity
	 * used to initialize the handler is returned if it matches the
	 * given role (if set). If called during configuration
	 * setup, the identity defined in the configuration is returned
	 * with given role assignment.</p>
	 * @return Identity - assigned/defined identity (or null)
	 */
	public Identity getIdentity () {
		
		// check if a new Identity can be set-up:
		if (id == null && done) {
			// yes: create identity from configuration data
			id = new Id();
			id.name = prvKeyName;
			id.certCA = certs [1];
			id.cert = certs[0];
			id.dhParam = new DH_KeyExchange (alpha, modulusP);
			id.prvKey = serverKey;
			id.proto = "inferno";
		}
		return id;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Return notification from handler. If a string is returned,
	 * it contains the last error message from the handler. Call this
	 * method after a process method returned AUTH_FAILED or null.</p> 
	 * @return String - error message (or null)
	 */
	public String getInfo () {
		return err;
	}

	//=================================================================
	//	AuthProtocolHandler process methods
	//=================================================================
	/*
	 * internal state
	 */
	private int			rdState = STATE_VERSION;	// read state
	private int			wrState = STATE_VERSION;	// write state
	private boolean		isServer = false;			// act as server
	private String		err = null;					// (last) error message
	/*
	 * Preferred settings:
	 */
	private String		version	= "1";				// preferred version
	private String		algos	= "sha/rc4_128";	// preferred algorithms
	/*
	 * collected authentication data:
	 */
	private DH_KeyExchange	dh; 					// DH info (this)
	private BigInteger		s0, s1;					// DH values (this, peer)
	private Certificate		c0, c1;					// certificates (this,peer)
	private Certificate		a0, a1;					// DH alpha certs (this,peer)
	
	//-----------------------------------------------------------------
	/**
	 * <p>Handle authentication on an established styx channel.</p>
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
			
		//=============================================================
		// Collect and publish authentication data re-using the
		// the 'sendDataToPeer()' and 'handlePeerData()' methods:
		//=============================================================
		boolean receiving = true;
		Blob data = null;
		int mode = AUTH_CONTINUE;
		
		// check for pending message
		if (ch.hasPendingMessage()) {
			data = ch.getNextMessage();
			// start with processing this message
			mode = AUTH_NEED_DATA;
		}
		
		while (mode != AUTH_SUCCESS && mode != AUTH_FAILED) {

			// handler needs more data to finish process step?
			if (mode != AUTH_NEED_DATA) {
				// no: can send available data to client
				data = new Blob();
				mode = getDataForPeer (data);
				if (data.size() > 0)
					ch.sendMessage (new Message (data.asByteArray(false)));
				data = null;
			}
			if (mode > AUTH_WAIT)
				sleep (mode);
			// handler has more data to finish step
			if (mode != AUTH_PENDING_DATA) {
				// get data from client
				if (receiving) {
					if (data == null) {
						String[] section = getNextMessage (ch);
						String content = ContentHandler.createSection (section);
						data = new Blob();
						data.putDirectString (content);
					}
				}
				mode = handlePeerData (data);
				data = null;
				if (mode == AUTH_NO_MORE_DATA)
					receiving = false;
				else if (mode == AUTH_NEED_DATA)
					sleep (200);
			}
			if (mode > AUTH_WAIT)
				sleep (mode);
		}
		return mode;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next message from client.</p>
	 * @param comm Channel - communication channel
	 * @return String[] - list of content strings
	 * @throws IOException - communication error
	 */
	private String[] getNextMessage (Channel ch) throws IOException {
		// read next section
		String[] content = ContentHandler.getNextSection (ch);

		// check for error messages.
		if ("*ERROR*".equals (content[0]))
			// terminate on error
			throw new IOException (content[1]);

		return content;
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
		String content = null;
		
		switch (rdState) {
			//---------------------------------------------------------
			// version message requested
			//---------------------------------------------------------
			case STATE_VERSION: {
				// as server wait for client to send first.
				if (isServer && wrState == STATE_VERSION)
					return AUTH_CONTINUE;
				// send our version information.
				rdState = STATE_AUTH_DATA;
				data.putDirectString ("000" + version.length() + "\n" + version);
				return AUTH_CONTINUE;
			}
			//---------------------------------------------------------
			// Authentication data requested:
			// 		* s0 = alpha^r0 mod p
			//		* certificate and public key
			//---------------------------------------------------------
			case STATE_AUTH_DATA:
				// as server wait for client to send first.
				if (isServer && wrState < STATE_KEY_EXCHANGE)
					return AUTH_CONTINUE;
				// send authentication data (part 1)
				content = ContentHandler.createSection (ContentHandler.toBase64 (s0));
				data.putDirectString (content);
				rdState = STATE_AUTH_DATA2;
				return AUTH_PENDING_DATA;
			case STATE_AUTH_DATA2:
				// send authentication data (part 2)
				data.putDirectString (c0.toContent());
				data.putDirectString (id.prvKey.getPublicKey().toContent());
				// start sending key exchange data
				rdState = STATE_KEY_EXCHANGE;
				return AUTH_WAIT + 1000;
		
			//---------------------------------------------------------
			// Diffie-Hellman key exchange: send signed (s0||s1) to peer
			//---------------------------------------------------------
			case STATE_KEY_EXCHANGE: {
				// sign DH key exchange packet
				String aa = ContentHandler.toBase64 (s0) + ContentHandler.toBase64 (s1);
				a0 = sign (id.prvKey, aa.getBytes(), "sha1", 120);
				rdState = STATE_ACK_OK;
				data.putDirectString (a0.toContent());
				return AUTH_CONTINUE;
			}
			//---------------------------------------------------------
			// Acknowledge the successful operation
			//---------------------------------------------------------
			case STATE_ACK_OK: {
				// as server wait for client to send first.
				if (isServer && wrState < STATE_KEY_EXCHANGE)
					return AUTH_CONTINUE;
				// acknowledge success
				rdState = (isServer ? STATE_DONE : STATE_ALGORITHMS);
				data.putDirectString ("0002\nOK");
				return AUTH_WAIT + 100;
			}
			//---------------------------------------------------------
			// Send our preferred algorithms for channel security
			//---------------------------------------------------------
			case STATE_ALGORITHMS: {
				// send algorithm
				data.putDirectString (ContentHandler.createSection(algos));
				rdState = STATE_DONE;
				return AUTH_CONTINUE;
			}
			//---------------------------------------------------------
			// wait for process to complete.
			//---------------------------------------------------------
			case STATE_DONE:
				if (rdState == STATE_DONE && wrState == STATE_DONE)
					return AUTH_SUCCESS;
				return AUTH_CONTINUE;
		}
		// should not come here: return failed
		return AUTH_FAILED;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Peer has send authentication data to be processed by the
	 * handler.</p> 
	 * @param b Blob - data as send from peer
	 * @return int - processing mode
	 * @throws IOException - communication failure
	 */
	public int handlePeerData (Blob b) throws IOException {

		// split incoming data into sections. It is assumed
		// that peers only send one or more complete sections
		// within on message (no fragmentation allowed!)
		Vector<String[]> sections = null;
		String[] content = null;
		int pos = 0;
		if (b != null) {
			sections = ContentHandler.getSections (b);
			content = sections.elementAt(0);
			
			// check for error message
			if ("*ERROR*".equals (content[0])) {
				System.out.println ("[AP_Inferno] " + content[1]);
				System.out.flush();
				return AUTH_FAILED;
			}
		}

		switch (wrState) {
			//---------------------------------------------------------
			// version message received
			//---------------------------------------------------------
			case STATE_VERSION:
				// parse version information
				String vv = content[0];
				if (!version.equals(vv)) {
					// @@@ check for valid version
					if (false) {
						err = "incompatible authentication protocol version";
						return AUTH_FAILED;
					}
					version = vv;
				}
				wrState = STATE_AUTH_DATA;
				peerCr = null;
				return AUTH_CONTINUE;
				
			//---------------------------------------------------------
			// Peer has send its authentication data
			//---------------------------------------------------------
			case STATE_AUTH_DATA:
				// send version info first
				if (rdState == STATE_VERSION)
					return AUTH_CONTINUE;
				// get value of s1 from peer
				s1 = ContentHandler.fromBase64 (content[0]);
				// check for replay attack
				// more sections available?
				if (sections.size() == 1) {
					wrState = STATE_AUTH_DATA2;
					// signal we need more data
					return AUTH_NEED_DATA;
				}
				content = sections.elementAt (++pos);
				// intended fall through!
			case STATE_AUTH_DATA2:
				// get certificate of peer (client)
				c1 = new Certificate (content);
				if (sections.size() == pos+1) {
					wrState = STATE_AUTH_DATA3;
					// signal we need more data
					return AUTH_NEED_DATA;
				}
				content = sections.elementAt (++pos);
				// intended fall through!
			case STATE_AUTH_DATA3:
				// get public key of peer
				c1.pubHolder = new PublicKey (content);
				c1.holder = c1.pubHolder.name;
				
				// derive authdom from name of signer
				String authdom = c1.signer;
				int idx = authdom.indexOf('.');
				authdom = authdom.substring (idx+1);

				// do we have a valid id for ourself?
				if (id == null) {
					// no: lookup our matching id
					Authenticator auth = Authenticator.getInstance();
					Identity idIn = auth.getIdentity ("inferno@" + authdom);
					if (idIn == null || !(idIn instanceof Id)) {
						err = "unknown authdom";
						return AUTH_FAILED;
					}
					// prepare identity
					id = (Id) idIn;
					init (id, true);
				}
				// check for replay attack.
				if (s0.equals (s1)) {
					err = "Possible replay attack!";
					return AUTH_FAILED;
				}
				// check client certificate
				if (!c1.signer.equals (id.certCA.holder)) {
					err = "unknown signer for certificate";
					return AUTH_FAILED;
				}
				c1.pubSigner = id.certCA.pubHolder;
				
				
				
				// verify integrity of peer certificate
				String dataStr = c1.pubHolder.toContent();
				byte[] data = dataStr.substring(5).getBytes();
				int rc = verify (id.certCA.pubHolder, data, c1, 0);
				if (rc != VERIFY_OK) {
					switch (rc) {
						case VERIFY_HASH_MISMATCH:
						case VERIFY_FAILED:
							err = "pk doesn't match certificate!";
							break;
						case VERIFY_EXPIRED:
							err = "certificate expired!";
							break;
						default:
							err = "certificate check failed (" + rc + ")";
							break;
					}
					return AUTH_FAILED;
				}
				wrState = STATE_KEY_EXCHANGE;
				return AUTH_CONTINUE;
					
			//---------------------------------------------------------
			//	Handle key exchange data from peer:
			//	read and check signed (s'||s)
			//---------------------------------------------------------
			case STATE_KEY_EXCHANGE:
				// send auth data info first
				if (rdState < STATE_KEY_EXCHANGE)
					return AUTH_CONTINUE;
				// read DH certificate
				a1 = new Certificate (content);
				String aa = ContentHandler.toBase64 (s1) + ContentHandler.toBase64 (s0);
				// verify DH response.
				rc = verify (c1.pubHolder, aa.getBytes(), a1, 0);
				if (rc != VERIFY_OK) {
					switch (rc) {
						case VERIFY_HASH_MISMATCH:
						case VERIFY_FAILED:
							err = "pk doesn't match DH certificate!";
							break;
						case VERIFY_EXPIRED:
							err = "DH certificate expired!";
							break;
						default:
							err = "DH certificate check failed (" + rc + ")";
							break;
					}
					return AUTH_FAILED;
				}
				wrState = STATE_ACK_OK;
				return AUTH_CONTINUE;
				
			//---------------------------------------------------------
			// Handle peer acknowledge for authentication process
			//---------------------------------------------------------
			case STATE_ACK_OK:
				// send DH certificate info first
				if (rdState == STATE_KEY_EXCHANGE)
					return AUTH_CONTINUE;
				// check peer evaluation
				if (!"OK".equals (content[0])) {
					err = "authentication failed";
					return AUTH_FAILED;
				}
				wrState = STATE_ALGORITHMS;
				return AUTH_CONTINUE;
						
			//---------------------------------------------------------
			// Handle peer selection of algorithms
			//---------------------------------------------------------
			case STATE_ALGORITHMS:
				// send ACK first
				if (rdState == STATE_ACK_OK)
					return AUTH_CONTINUE;
				// get algorithms
				String alg = content[0];
				// @@@ check algorithms
				
				// compute shared secret
				byte[] secret = dh.getSharedSecret (s1);
				
				// assemble credential.
				peerCr = new Credential (c1.holder);
				peerCr.addGroup (c1.holder);
				peerCr.setSecret (secret);
				peerCr.setSecureChannelDef (alg);
				
				// report successful authentication
				wrState = STATE_DONE;
				return AUTH_NO_MORE_DATA;
				
			//---------------------------------------------------------
			// wait for process to complete.
			//---------------------------------------------------------
			case STATE_DONE:
				if (rdState == STATE_DONE && wrState == STATE_DONE)
					return AUTH_SUCCESS;
				return AUTH_NO_MORE_DATA;
		}		
		// handle out-of-sync cases
		err = "Invalid internal state -- aborting";
		return AUTH_FAILED;
	}

	//=================================================================
	//	Signature generation and verify
	//=================================================================
	/**
	 * <p>Sign data with private key. If a negative value for expiresAt
	 * is specified, the absolute value in seconds is added to current
	 * time (relative expiresAt).</p>
	 * @param data byte[] - data to be signed
	 * @param digest String - hashing algorithm
	 * @param expiresAt int - epoch of expiration date
	 * @return Certificate - Generated certificate
	 */
	protected Certificate sign (PrivateKey key, byte[] data, String digest, int expiresAt) {
		
		// check for relative date
		if (expiresAt < 0)
			expiresAt = (int) (new Date().getTime() / 1000) - expiresAt;
		
		// compute digest of data
		BigInteger actHash = digest (digest, data, key.name, expiresAt);
		if (actHash == null)
			return null;
		
		// encrypt digest with private key (no optimized)
		BigInteger a = actHash.modPow (key.d, key.m);
		
		// assemble certificate
		Certificate res = new Certificate (key.name, expiresAt);
		res.encryption = key.encAlg;
		res.digest = digest;
		res.signer = key.name;
		res.signature = a;
		return res;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Validate a signature (from a certificate) of data with
	 * given public key of signer.</p>
	 * @param data byte[] - signed data
	 * @param cert Certificate - signature cert
	 * @param now int - epoch to use for check (0 = now)
	 * @return int - return code (0 = OK)
	 */
	protected int verify (PublicKey signer, byte[] data, Certificate cert, int now) {

		// current epoch time value
		if (now == 0)
			now = (int) (new Date().getTime() / 1000);

		// certificate expired?
		if (cert.expiresAt != 0 && cert.expiresAt < now)
			return VERIFY_EXPIRED;

		// compute digest of data
		BigInteger actHash = digest (cert.digest, data, signer.name, cert.expiresAt);
		if (actHash == null)
			return VERIFY_HASH_MISMATCH;
		
		// decrypt signature value with public signer key
		BigInteger sigHash = cert.signature.modPow (signer.e, signer.m);
		// compare values
		if (sigHash.equals (actHash))
			return VERIFY_OK;
		return VERIFY_FAILED;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Compute message digest over data.</p>
	 * @param digest String - hashing algorithm
	 * @param data byte[] - data to be digested
	 * @param name String - name of signer
	 * @param expireAt int - epoch of expiration
	 * @return BigInteger - digest as BigInteger value
	 */
	private BigInteger digest (String digest, byte[] data, String name, int expiresAt) {
		// compute digest of data
		try {
			MessageDigest hasher = MessageDigest.getInstance (digest.toUpperCase());
			hasher.update (data);
			hasher.update ((name + " " + expiresAt).getBytes());
			return new BigInteger (1, hasher.digest());
		}
		catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	//=================================================================
	//	XML parser for configuration data
	//=================================================================
	/*
	 * XML parse state attributes:
	 */
	private String			prvKeyName = null;
	private Certificate[]	certs = new Certificate [2];
	private int				pos = 0;
	private PublicKey		key = null;
	private PrivateKey		serverKey = null;
	private BigInteger		alpha, modulusP;
	private StringBuffer	pcData = null;
	private boolean			inSignature = false;
	private boolean			inCertificate = false;
	
	//-----------------------------------------------------------------
	/**
	 * <p>Initialize handler with attributes of the &lt;Identity .../&gt;
	 * element.</p>
	 * @param attrs Attributes - list of attributes
	 * @return boolean - initialization successful?
	 */
	public boolean initParse (Attributes attrs) {
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Start of new Element in XML.</p>
	 * @param name String - element name
	 * @param attrs Attributes - element attributes
	 */
	public void enterElement (String name, Attributes attrs) throws Exception {

		//-------------------------------------------------------------
		// <Certificate ...>: Plan 9 / Inferno certificate
		//-------------------------------------------------------------
		if (name.equals ("Certificate")) {
			// parse attributes.
			int expiresAt = Integer.parseInt (attrs.getValue ("expires"));
			if (expiresAt < 0)
				throw new SAXException ("Invalid expiration data in certificate");
			String cName = attrs.getValue ("name");
			// allocate a new certificate
			certs[pos] = new Certificate (cName, expiresAt);
			// flag for key usage
			inSignature = false;
			inCertificate = true;
			return;
		}
		//-------------------------------------------------------------
		// <Signature ...>:   certificate section: signature data. 
		//-------------------------------------------------------------
		if (name.equals ("Signature")) {
			// get message digest algorithm
			certs[pos].digest = attrs.getValue ("digest");
			// flag key usage
			inSignature = true;
			return;
		}
		//-------------------------------------------------------------
		// <PrivateKey ...>:  certificate section: private key of
		//					  certificate holder. 
		//-------------------------------------------------------------
		if (name.equals ("PrivateKey")) {
			// get name of key owner
			prvKeyName = attrs.getValue ("name");
			// private keys can only occur outside certificates as
			// certificate holder key.
			if (inCertificate)
				throw new SAXException ("No private keys inside certificates allowed");
			// allocate key object
			key = serverKey = new PrivateKey (prvKeyName);
			return;
		}
		//-------------------------------------------------------------
		// <PublicKey ...>:  certificate section: public key (of
		//					 certificate holder or signing authority). 
		//-------------------------------------------------------------
		if (name.equals ("PublicKey")) {
			// get name of key owner
			String owner = attrs.getValue ("name");
			if (!inCertificate)
				throw new SAXException ("No public keys outside certificates allowed");
			// allocate key object
			key = new PublicKey (owner);
			return;
		}
		//-------------------------------------------------------------
		// <Modulus ...>:  RSA modulus of current key 
		// <PublicExp ...>:  RSA public exponent of current key 
		// <PrivateExp ...>:  RSA private exponent of current key
		// <PrimeP ...>:  RSA prime p of current key 
		// <PrimeQ ...>:  RSA prime q of current key 
		// <PrimeExpP ...>:  RSA prime p exponent of current key 
		// <PrimeExpQ ...>:  RSA prime q exponent of current key 
		// <Coefficient ...>:  RSA coefficient of current key
		// <Value ...>: Signature data
		// <Alpha ...>:  DH parameter "alpha" 
		// <P ...>:  DH parameter "p"
		//-------------------------------------------------------------
		if (	name.equals ("Modulus")
			||	name.equals ("PublicExp")
			||	name.equals ("PrivateExp")
			||	name.equals ("PrimeP")
			||	name.equals ("PrimeQ")
			||	name.equals ("PrimeExpP")
			||	name.equals ("PrimeExpQ")
			||	name.equals ("Coefficient")
			||	name.equals ("Value")
			||	name.equals ("Alpha")
			||	name.equals ("P")
		) {
			pcData = new StringBuffer();
			return;
		}
		//-------------------------------------------------------------
		// <DHparam ...>:  Diffie-Hellman key exchange parameters
		//-------------------------------------------------------------
		if (name.equals ("DHparam")) {
			// reset parameters
			alpha = modulusP = null;
			return;
		}
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
		//-------------------------------------------------------------
		// </Certificate>
		//-------------------------------------------------------------
		if (name.equals ("Certificate")) {
			inCertificate = false;
			// increment counter in certificate list
			pos++;
			return;
		}
		//-------------------------------------------------------------
		// </Signature>
		//-------------------------------------------------------------
		if (name.equals ("Signature")) {
			// set key usage
			inSignature = false;
			return;
		}
		//-------------------------------------------------------------
		// </Value>
		//-------------------------------------------------------------
		if (name.equals ("Value")) {
			// convert Base64-encoded signature value
			certs[pos].signature = ContentHandler.fromBase64 (pcData.toString());
			return;
		}
		//-------------------------------------------------------------
		// </PrivateKey>
		//-------------------------------------------------------------
		if (name.equals ("PrivateKey")) {
			key = null;
			return;
		}
		//-------------------------------------------------------------
		// </PublicKey>
		//-------------------------------------------------------------
		if (name.equals ("PublicKey")) {
			// check where the key occurred.
			if (inSignature) {
				// within a signature it's the signer key
				certs[pos].pubSigner = key;
				certs[pos].signer = key.name;
			} else {
				// It's the public key of certificate holder
				certs[pos].pubHolder = key;
			}
			// reset key reference
			key = null;
			return;
		}
		//-------------------------------------------------------------
		// </Modulus> 
		//-------------------------------------------------------------
		if (name.equals ("Modulus")) {
			BigInteger m = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PublicKey) {
				PublicKey pub = (PublicKey) key;
				pub.m = m;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </PublicExp> 
		//-------------------------------------------------------------
		if (name.equals ("PublicExp")) {
			BigInteger e = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PublicKey) {
				PublicKey pub = (PublicKey) key;
				pub.e = e;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </PrivateExp> 
		//-------------------------------------------------------------
		if (name.equals ("PrivateExp")) {
			BigInteger d = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.d = d;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </PrimeP> 
		//-------------------------------------------------------------
		if (name.equals ("PrimeP")) {
			BigInteger p = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.p = p;
			}
			pcData = null;
			return;
		}

		//-------------------------------------------------------------
		// </PrimeQ> 
		//-------------------------------------------------------------
		if (name.equals ("PrimeQ")) {
			BigInteger q = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.q = q;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </PrimeExpP> 
		//-------------------------------------------------------------
		if (name.equals ("PrimeExpP")) {
			BigInteger pE = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.pE = pE;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </PrimeExpQ> 
		//-------------------------------------------------------------
		if (name.equals ("PrimeExpQ")) {
			BigInteger qE = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.qE = qE;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </Coefficient> 
		//-------------------------------------------------------------
		if (name.equals ("Coefficient")) {
			BigInteger crt = ContentHandler.fromBase64 (pcData.toString());
			if (key != null && key instanceof PrivateKey) {
				PrivateKey prv = (PrivateKey) key;
				prv.crt = crt;
			}
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </DHparam>
		//-------------------------------------------------------------
		if (name.equals ("DHparam")) {
			return;
		}
		//-------------------------------------------------------------
		// </Alpha> 
		//-------------------------------------------------------------
		if (name.equals ("Alpha")) {
			alpha = ContentHandler.fromBase64 (pcData.toString());
			pcData = null;
			return;
		}
		//-------------------------------------------------------------
		// </P>
		//-------------------------------------------------------------
		if (name.equals ("P")) {
			modulusP = ContentHandler.fromBase64 (pcData.toString());
			pcData = null;
			return;
		}
	}
	
	//=================================================================
	/**
	 * <p>Collect PCDATA between tags.</p>
	 * @param ch char[] - character array of data
	 * @param start int - offset into array
	 * @param length int - length of data 
	 */
	public void characters (char[] ch, int start, int length) throws SAXException {
		// are we collecting PCDATA?
		if (pcData == null)
			return;
		// append new data
		pcData.append (ch, start, length);
	}
}

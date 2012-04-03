
//******************************************************************
//*   PGMID.        P9ANY AUTHENTICATION PROTOCOL NEGOTIATOR.      *
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
import java.util.Vector;
import org.xml.sax.Attributes;
import j9p.Channel;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 *  <h3>P9any (description from Plan9 man 6 authsrv)</h3>
 *  <p>P9any is the standard Plan 9 authentication protocol. It
 *  consists of a negotiation to determine a common protocol, followed
 *  by the agreed-upon protocol. The negotiation protocol is:</p>
 *	<ul>
 *		<li>S-&gt;C: "v.2 proto@authdom proto@authdom ..."</li>
 *		<li>C-&gt;S: "proto dom"</li>
 *		<li>S-&gt;C: "OK"</li>
 *	</ul>
 *	<p>Each message is a NUL-terminated UTF string. The server begins
 *  by sending a list of proto, authdom pairs it is willing to use.
 *  The client responds with its choice. Requiring the client to wait
 *  for the final OK ensures that the client will not start the chosen
 *  protocol until the server is ready.</p>
 *  <p>The p9any protocol is the protocol used by all Plan 9 services.
 *  The file server runs it over special authentication files (see
 *  fauth(2) and attach(5)). Other services, such as cpu(1) and
 *  exportfs(4), run p9any over the network and then use Kn to derive
 *  an ssl(3) key to encrypt the rest of their communications.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AP_p9any extends AP_Generic {
	
	//=================================================================
	/*
	 * Constants for process states:
	 */
	private static final int STATE_INITIAL		=  1; 
	private static final int STATE_SELECT		=  2; 
	private static final int STATE_ACK			=  3; 
	private static final int STATE_DONE			=  4;
	
	//=================================================================
	/**
	 * <p>A (virtual) identity assigned to 'p9any'.</p> 
	 */
	public static class Id implements Identity {
		// Attributes:
		protected String			version = null;	// protocol version
		protected Vector<String>	domains = null;	// known domain protos
		//-------------------------------------------------------------
		/**
		 * <p>Get name of identity.</p>
		 * @return String - name of identity
		 */
		public String getName()			{ return "p9any"; }
		//-------------------------------------------------------------
		/**
		 * <p>Get name of authentication protocol.</p>
		 * @return String - name of authentication protocol
		 */
		public String getAuthProtocol()	{ return "p9any"; }
	}

	//=================================================================
	//=================================================================
	/*
	 * Attributes:
	 */
	private Id	id = null;		// associated identity
	
	//-----------------------------------------------------------------
	/**
	 * <p>Hidden constructor: Handler instances are returned by
	 * calling 'AP_Generic.getInstance(...)'.</p>
	 */
	protected AP_p9any () {
		domains = new Vector<String>();
		version = "v.2";
		rdState = STATE_INITIAL;
		wrState = STATE_INITIAL;
		isServer = false;
		id = null;
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
			version = id.version;
			domains = id.domains;
			return true;
		}
		return false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get name of handled authentication protocol.</p>
	 * @return String - name of authentication protocol 
	 */
	public String getProtocolName () {
		return "p9any";
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get the peer credential after a successful authentication.</p>
	 * @return Credential - authenticated peer credential (or null)
	 */
	public Credential getPeerCredential() {
		return null;
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
	public Identity getIdentity () {
		if (id == null && done) {
			// create new identity (configuration)
			id = new Id();
			id.version = version;
			id.domains = domains;
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
	public String getInfo() {
		return info;
	}

	//=================================================================
	//	AuthProtocolHandler process methods
	//=================================================================
	/*
	 * internal state
	 */
	protected int		rdState = STATE_INITIAL;	// process state (read)
	protected int		wrState = STATE_INITIAL;	// process state (write)
	protected boolean	isServer = false;			// act as server
	protected String	info = null;				// info / error message
	
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
		
		StringBuffer buf = new StringBuffer();
		int rc = AUTH_CONTINUE;
		
		switch (rdState) {
			//---------------------------------------------------------
			// initial state: server sends supported authdoms,
			// client performs state transition to STATE_SELECT.
			//---------------------------------------------------------
			case STATE_INITIAL:
				if (isServer) {
					// send authdoms
					buf.append (version);
					for (String dom : domains) {
						buf.append (' ');
						buf.append (dom);
					}
					rdState = STATE_ACK;
					break;
				}
				// intended fall through
			//---------------------------------------------------------
			//	client only: select authdom
			//---------------------------------------------------------
			case STATE_SELECT:
				// send selected protocol
				buf.append (selectedProto);
				buf.append (' ');
				buf.append (selectedDomain);
				rdState = STATE_DONE;
				break;
			//---------------------------------------------------------
			//	server only: send acknowledge to peer
			//---------------------------------------------------------
			case STATE_ACK:
				buf.append ("OK");
				info = selectedProto + "@" + selectedDomain;
				rc = AUTH_DELEGATED;
				break;
		}
		// assemble blob
		data.putDelimString (buf.toString());
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
	public int handlePeerData(Blob data) throws IOException {
		
		// split data into lines
		String content = data.getDelimString ();
		String[] lines = content.split("\n");
		
		switch (wrState) {
			//---------------------------------------------------------
			// initial state: client receives authdoms, server performs
			// state change to STATE_SELECT
			//---------------------------------------------------------
			case STATE_INITIAL:
				if (!isServer) {
					// handle server authdoms
					String[] param = lines[0].split (" ");
					if (!version.equals (param[0])) {
					}
					int pos = param[0].indexOf('@');
					selectedProto = param[0].substring (0,pos);  
					selectedDomain = param[0].substring (pos+1);
					wrState = STATE_ACK;
					return AUTH_CONTINUE;
				}
				// intended fall through for servers
			//---------------------------------------------------------
			// server only: client has selected an authdom
			//---------------------------------------------------------
			case STATE_SELECT:
				int pos = lines[0].indexOf(' ');
				selectedProto = lines[0].substring (0,pos);  
				selectedDomain = lines[0].substring (pos+1);
				wrState = STATE_ACK;
				return AUTH_CONTINUE;
			//---------------------------------------------------------
			//	client only: wait for server acknowledge
			//---------------------------------------------------------
			case STATE_ACK:
				if ("OK".equals (lines[0])) {
					wrState = STATE_ACK;
					return AUTH_CONTINUE;
				}
				return AUTH_FAILED;
		}
		// unhandled state
		return AUTH_FAILED;
	}
	
	//=================================================================
	//	XML parser for configuration data
	//=================================================================
	/*
	 * Attributes:
	 */
	private String			version;
	private Vector<String>	domains;
	private String			selectedDomain;
	private String			selectedProto;

	//-----------------------------------------------------------------
	/**
	 * <p>Initialize handler with attributes of the &lt;Identity .../&gt;
	 * element.</p>
	 * @param attrs Attributes - list of attributes
	 * @return boolean - initialization successful?
	 */
	public boolean initParse (Attributes attrs) {
		// set version (if specified)
		String vv = attrs.getValue ("version");
		if (vv != null)
			version = vv;
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

		//-------------------------------------------------------------
		// <Domain ...>: domain specification
		//-------------------------------------------------------------
		if (name.equals ("Domain")) {
			String dom = attrs.getValue ("name");

			// get associated protocol.
			String proto = attrs.getValue ("proto");
			if (!isSupported (proto)) {
				// no registered handler
				System.out.println ("   No valid protocol for domain '" + dom + "' (" + proto + ")");
				return;
			}
			// register domain
			System.out.println ("   Domain '" + dom + "' registered for protocol '" + proto + "'");
			domains.add (proto + "@" + dom);
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
		// </Domain>
		//-------------------------------------------------------------
		if (name.equals ("Domain")) {
			return;
		}
	}
}


//******************************************************************
//*   PGMID.        INTERFACE FOR AUTHENTICATION HANDLERS.         *
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

import org.xml.sax.Attributes;

import j9p.Channel;
import j9p.util.Blob;


//=================================================================
/**
 * <p>The interface AuthProtocolHandlers is implemented by derived
 * classes that handle specific authentication protocols.</p>
 * <p>A list of built-in authentication schemes include:</p>
 * <ul>
 * 	<li>p9sk1 - a Plan 9 shared key protocol.</li>
 * 	<li>p9sk2 - a variant of p9sk1.</li>
 *	<li>inferno - Inferno authentication protocol.</li>
 * </ul>  
 * <p>
 * <p>Custom handlers (AP_???) are usually derived from AP_Generic.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public interface AuthProtocolHandler {
	
	//=================================================================
	/*
	 * Protocol constants for built-in handlers:
	 */
	int V_P9SK1		= 1;			// Plan9 shared key authentication
	int V_P9SK2		= 2;			// older version of p9sk
	int V_INFERNO	= 3;			// Inferno 4thEd. authentication
	/*
	 * Processing modes:
	 */
	int AUTH_NO_IDENTITY	= -2;	// no identity available
	int AUTH_NOT_SUPPORTED	= -1;	// protocol not supported
	int AUTH_FAILED			=  0;	// authentication failed
	int AUTH_SUCCESS		=  1;	// authentication successful
	int AUTH_NEED_DATA		=  2;	// need more data
	int AUTH_PENDING_DATA	=  3;	// have more data
	int AUTH_CONTINUE		=  4;	// continue with next step
	int AUTH_WAIT			=  5;	// wait n seconds for 'AUTH_WAIT+n'
	int AUTH_NO_MORE_DATA	=  6;	// no more data needed
	int AUTH_DELEGATED		=  7;	// a follow-up handler must be used

	//=================================================================
	// Inner interface that represents an Identity in the specific
	// authentication scheme.
	//=================================================================

	interface Identity {
		/**
		 * <p>Get name of identity.</p>
		 * @return String - name of identity
		 */
		String getName ();
		//-------------------------------------------------------------
		/**
		 * <p>Get name of authentication protocol.</p>
		 * @return String - name of authentication protocol
		 */
		String getAuthProtocol ();
	}

	//=================================================================
	//=================================================================
	/**
	 * <p>Initialize protocol handler and use given identity to
	 * authenticate this instance to remote peer. Not used during
	 * parsing.</p> 
	 * @param id Identity - our identity
	 * @param asServer boolean - act as server side
	 * @return boolean - initialization successful?
	 */
	boolean init (Identity id, boolean asServer);
	//-----------------------------------------------------------------
	/**
	 * <p>Initialize handler with attributes of the &lt;Identity .../&gt;
	 * element.</p>
	 * @param attrs Attributes - list of attributes
	 * @return boolean - initialization successful?
	 */
	boolean initParse (Attributes attrs);
	//-----------------------------------------------------------------
	/**
	 * <p>Get name of handled authentication protocol.</p>
	 * @return String - name of authentication protocol 
	 */
	String	getProtocolName ();
	//-----------------------------------------------------------------
	/**
	 * <p>Get associated identity.</p>
	 * <p>When called during an authentication process, the identity
	 * used to initialize the handler is returned. If called during
	 * configuration setup, the identity defined in the configuration
	 * is returned.</p>
	 * @return Identity - assigned/defined identity (or null)
	 */
	Identity getIdentity ();
	//-----------------------------------------------------------------
	/**
	 * <p>Get the peer credential after a successful authentication.</p>
	 * @return Credential - authenticated peer credential (or null)
	 */
	Credential getPeerCredential ();
	//-----------------------------------------------------------------
	/**
	 * <p>Return info from handler: This is either the last
	 * error message from the handler (on AUTH_FAILED) or the name
	 * of a successive handler protocol (on AUTH_DELEGATED).</p> 
	 * @return String - error message (or null)
	 */
	String getInfo ();

	//=================================================================
	//	AuthProtocolHandler process methods
	//=================================================================
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
	 * the Rauth message type.</p>
	 * <p>The result of the authentication process is returned
	 * (either AUTH_FAILED or AUTH_SUCCESS).  
	 * @param ch Channel - established connection
	 * @return int - processing mode
	 * @throws IOException - communication failure
	 */
	int process (Channel ch) throws IOException;
	//-----------------------------------------------------------------
	/**
	 * <p>The peer is requesting data, so the handler must return
	 * binary data to be send to the peer.</p> 
	 * @param data Blob - blob to receive data (stream mode)
	 * @return int - processing state (AUTH_???)
	 * @throws IOException - communication failure
	 */
	int getDataForPeer (Blob data) throws IOException;
	//-----------------------------------------------------------------
	/**
	 * <p>Peer has send authentication data to be processed by the
	 * handler.</p> 
	 * @param data Blob - data as send from peer
	 * @return int - processing state (AUTH_???)
	 * @throws IOException - communication failure
	 */
	int handlePeerData (Blob data) throws IOException;
}

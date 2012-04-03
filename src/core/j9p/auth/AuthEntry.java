
//******************************************************************
//*   PGMID.        NAMESPACE ENTRY FOR AUTHENTICATION.            *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/27.                                      *
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

import j9p.auth.AuthProtocolHandler.Identity;
import j9p.ns.Entry;
import j9p.ns.File;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>AuthEntry</b> is a synthetic namespace entry that is transient
 * for an authentication process and removed during an "attach()" call.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AuthEntry extends File {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private AuthProtocolHandler handler = null;	// authentication instance
	private Identity id = null;					// identity used in auth
	private Authenticator auth = null;			// manager instance

	//=================================================================
	//=================================================================
	/**
	 * <p>Instantiate an AuthEntry.</p>
	 */
	public AuthEntry () {
		// flag as special entry.
		stat.flags |= Entry.DMAUTH;
		
		// we start with the negotiation protocol handler.
		auth = Authenticator.getInstance();
		handler = new AP_p9any();
		id = auth.getIdentity ("p9any");
		if (id != null)
			handler.init (id, true);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get the peer credential after a successful authentication.</p>
	 * @return Credential - authenticated peer credential (or null)
	 */
	public Credential getCredential() {
		return handler.getPeerCredential();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Update the argument credential with the values defined
	 * by the credential from authentication.</p>
	 * @param curr Credential - current user credential
	 * @return boolean - update of credential successful?
	 */
	public boolean updateCredential (Credential curr) {
		Credential cr = getCredential();
		if (cr.isAuthenticated()) {
			curr.overwrite (cr);
			return true;
		}
		return false;
	}
	
	//=================================================================
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file (null)
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int size, AttributeHandler fmt) {

		// get data from handler.
		try {
			Blob data = new Blob();
			int rc = handler.getDataForPeer (data);
			if (rc == AuthProtocolHandler.AUTH_DELEGATED)
				chainHandler();
			return data.asByteArray (false);
		}
		catch (Exception e) {
			return null;
		}
	}
	//=================================================================
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public int write (Handle hdl, byte[] data, long offset, int size) {

		try {
			// send data to handler.
			Blob b = new Blob (data, size);
			int num = b.size();
			int rc = handler.handlePeerData (b);

			// we are delegating further calls to another handler.
			if (rc == AuthProtocolHandler.AUTH_DELEGATED)
				chainHandler();
			return num;
		}
		catch (Exception e) {
			return AuthProtocolHandler.AUTH_FAILED;
		}
	}
	//=================================================================
	/**
	 * <p>set follow-up handler (delegation).</p> 
	 */
	private void chainHandler () {
		// get next handler instance by parsing
		// the identity specification returned by 'p9any'
		String idSpec = handler.getInfo();
		// split id spec
		int pos = idSpec.indexOf('@');
		String proto = idSpec.substring (0, pos);
		// instantiate protocol
		AuthProtocolHandler next = AP_Generic.getInstance (proto);
		if (next != null) {
			handler = next;
			id = auth.getIdentity (idSpec);
			handler.init (id, true);
		}
	}
	//=================================================================
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public long getSize () {
		// return our size
		return 0;
	}
}

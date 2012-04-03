
//******************************************************************
//*   PGMID.        USER CREDENTIAL (AUTHENTICATION/AUTHORIZATION).*
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

package j9p.auth;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import j9p.crypto.SecureChannel;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The credential of an user controls his/her access to sections of the
 * namespace. It is authorized during the authentication of the user.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Credential {

	//=================================================================
	/*
	 * Attributes:
	 */
	protected boolean			authenticated = false;	// authenticated credential
	protected String			userId = "*";			// user identifier
	protected Vector<String>	groupIds = null;		// list of associated groups
	protected byte[]			sharedSecret = null;	// shared secret
	protected String			algorithms = null;		// channel algorithms
	protected String			aname = null;			// name of attached namespace
	
	//=================================================================
	/**
	 * <p>Instantiate a new Credential for user and group memberships.</p> 
	 * @param user String - user identifier
	 */
	public Credential (String user) {
		if (user != null)
			userId = user;
		groupIds = new Vector<String>();
		sharedSecret = null;
		authenticated = false;
	}
	
	//=================================================================
	/**
	 * <p>Check if credential was constructed during an successful
	 * authentication.</p> 
	 * @return boolean - authenticated credential
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	//=================================================================
	/**
	 * <p>Get name of credential holder.</p> 
	 * @return String - name of user
	 */
	public String getUser () {
		return userId;
	}
	
	//=================================================================
	/**
	 * <p>Check if credential holder is member of a specific group.</p>
	 * @param group String - group to be tested
	 * @return boolean - owner is member in group
	 */
	public boolean isMember (String group) {
		for (String gid : groupIds) {
			if (gid.equals(group))
				return true;
		}
		return false;
	}
	
	//=================================================================
	/**
	 * <p>Return the gid of the holders main group assignment.</p>
	 * <p>If no group assignments are available, the user name
	 * is used as group identifier.</p>
	 * @return String - group identifier
	 */
	public String getGroup () {
		if (groupIds.size() < 1)
			return userId;
		return groupIds.firstElement();
	}

	//=================================================================
	/**
	 * <p>Set user name for credential. This is only possible
	 * on an empty credential with user = "*".<p> 
	 * @param user String - name of credential owner
	 * @return boolean - name change successful?
	 */
	public boolean setUser (String user) {
		if (user != null && userId.equals ("*")) {
			userId = user;
			return true;
		}
		return false;
	}
	
	//=================================================================
	/**
	 * <p>Add group to list of associated groups.</p>
	 * @param gid String - group name
	 * @return boolean - group added?
	 */
	public boolean addGroup (String gid) {
		if (groupIds.contains (gid))
			return false;
		groupIds.add (gid);
		return true;
	}
	
	//=================================================================
	/**
	 * <p>Set secure channel algorithms.</p>
	 * @param alg String - encoded algorithms
	 */
	protected void setSecureChannelDef (String alg) {
		algorithms = alg;
	}
	
	//=================================================================
	/**
	 * <p>Return secure channel around Styx channel for user session.</p>
	 */
	public SecureChannel getSecureChannel () {
		if (sharedSecret == null || algorithms == null)
			return null;
		try {
			return new SecureChannel (sharedSecret, algorithms);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	//=================================================================
	/**
	 * <p>Get value of shared secret.</p> 
	 * @return byte[] - copy of shared secret
	 */
	public byte[] getSecret () {
		return sharedSecret.clone();
	}
	
	//=================================================================
	/**
	 * <p>Set shared secret for channel encryption.</p>
	 * @param data byte[] - shared secret data
	 * @return boolean - successful operation?
	 */
	protected boolean setSecret (byte[] data) {
		sharedSecret = data;
		authenticated = true;
		return true;
	}
	
	//=================================================================
	/**
	 * <p>Overwrite credential with data from another credential.</p>
	 * @param cr Credential - source credential
	 */
	protected void overwrite (Credential cr) {
		authenticated = cr.authenticated;
		userId = new String (cr.userId);
		sharedSecret = cr.sharedSecret.clone();
		algorithms = (cr.algorithms == null ? null : new String (cr.algorithms));
		aname = (cr.aname == null ? null : new String (cr.aname));
		groupIds = new Vector<String>();
		for (String gid : cr.groupIds)
			groupIds.add (gid);
	}
}

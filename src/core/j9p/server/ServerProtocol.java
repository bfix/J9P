
//******************************************************************
//*   PGMID.        GENERIC STYX PROTOCOL IMPLEMENTATION.          *
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

package j9p.server;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.util.Enumeration;
import java.util.Hashtable;
import j9p.auth.AuthEntry;
import j9p.auth.Credential;
import j9p.ns.Directory;
import j9p.ns.Namespace;
import j9p.ns.Entry;
import j9p.ns.Permissions;
import j9p.ns.Entry.Handle;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The abstract base class <b>ServerProtocol</b> handles a stateful
 * communication protocol (mapping between fid and namespace entries).
 * Derived classes implement the older Inferno protocol and the newer
 * 9P protocol (9P2000).</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class ServerProtocol extends Entry.AttributeHandler implements ServerSession.Protocol {
	
	//=================================================================
	/*
	 * Constants for TAGs and FIDs
	 */
	public static final int NOTAG       = 0xFFFF;
	public static final int NOFID       = 0xFFFFFFFF;

	//=================================================================
	/**
	 * <p>Inner class result to encapsulate a function return
	 * value with an Entry reference, string message, long value
	 * and a return code (status).</p>
	 */
	protected static class Result {
		/*
		 * Constants for return code field.
		 */
		public static final int RC_INVALID				= -1;
		public static final int RC_OK					=  0;
		public static final int RC_IMPLAUSIBLE_MSIZE	=  1;
		public static final int RC_UNKNOWN_VERSION		=  2;
		public static final int RC_FID_IN_USE			=  3;
		public static final int RC_NO_AUTH_ENTRY		=  4;
		public static final int RC_NOT_AUTHENTICATED	=  5;
		public static final int RC_NO_NAMESPACE_AVAIL	=  6;
		public static final int RC_NO_ENTRY 			=  7;
		public static final int RC_NO_PERMISSION		=  8;
		public static final int RC_NO_PARENT_DIRECTORY	=  9;
		public static final int RC_CREATE_FAILED		= 10;
		public static final int RC_OPEN_FAILED			= 11;
		public static final int RC_IN_EXCLUSIVE_USE = 0;

		//-------------------------------------------------------------
		/*
		 * Attributes:
		 */
		public int		rc;		// return code (see above)
		public Entry[]	ent;	// reference to entries (maybe null)
		public String	msg;	// optional message string
		public long		val;	// optional integer value

		//-------------------------------------------------------------
		/**
		 * <p>Instantiate new result with given return code and
		 * entry reference (single list element).</p>
		 * @param rcode int - return code
		 * @param e Entry - entry reference (or null)
		 */
		public Result (int rcode, Entry e) {
			rc = rcode;
			ent = new Entry[] { e };
		}
		//-------------------------------------------------------------
		/**
		 * <p>Allocate empty result object.</p>
		 */
		public Result () {
			rc = RC_INVALID;
		}
		//-------------------------------------------------------------
		/**
		 * <p>Get entry from single item list.</p>
		 * @return Entry - (single) entry
		 */
		public Entry getEntry() {
			if (ent != null && ent.length == 1)
				return ent[0];
			return null;
		}
	}
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private Hashtable<Integer,Entry> assoc = null;	// fid/Entry map
	private Hashtable<Integer,Handle> accessors;	// list of opened files
	private NamespaceManager nsHdlr = null;			// namespace handler
	private boolean withAuth = false;				// authentication required?

	protected Namespace ns = null;					// operational namespace
	protected int maxMsgSize = 8192;				// maximum size of messages 
	
	//=================================================================
	/**
	 * <p>Instantiate a new ServerProtocol as a message handler
	 * that operates on a namespace provided by the namespace handler.</p>
	 * @param nsHdlr NamespaceHandler - namespace provider
	 * @param needAuth boolean - authentication required?
	 */
	protected ServerProtocol (NamespaceManager nsHdlr, boolean needAuth) {
		assoc = new Hashtable<Integer,Entry>();
		accessors = new Hashtable<Integer,Handle>();
		this.nsHdlr = nsHdlr;
		withAuth = needAuth;
	}
	
	//=================================================================
	/**
	 * <p>Reset protocol handler instance.</p> 
	 */
	public void reset () {
		
		// "clunk" all fids.
		for (Enumeration<Integer> e = accessors.keys(); e.hasMoreElements(); ) {
			int fid = e.nextElement();
			// release fid
			releaseFid (fid);
		}
		// empty lists
		assoc.clear();
		accessors.clear();
	}
	
	//#################################################################
	//===================>>>  SERVER OPERATIONS  <<<===================
	//#################################################################
	// Comments on message types taken from the book "Inferno Programming
	// with Limbo" by Phillip Stanley-Marbell or from Inferno's man pages.
	//=================================================================
	/**
	 * <p>Negotiate protocol version: the only accepted version is
	 * "9P2000", any other version request yields a "unknown" response.
	 * A version message starts a new session and resets all fid assocs.</p>
	 * @param msize int - maximum size of messages for this session
	 * @param version String - requested version
	 * @return Result
	 */
	protected Result version (int msize, String version) {
		
		// check maximum message size
		if (msize < 1024 || msize > 16384)
			// Implausible value
			return new Result (Result.RC_IMPLAUSIBLE_MSIZE, null);
		// set as default for our processing.
		maxMsgSize = msize;
		
		// "clunk" all fids.
		reset();
		
		// check version string.
		Result res = new Result();
		if ("9P2000".equals(version)) {
			res.msg = version;
			res.rc = Result.RC_OK;
		} else {
			res.msg = "unknown";
			res.rc = Result.RC_UNKNOWN_VERSION;
		}
		return res;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get a "special" namespace file for authentication.</p>
	 * @param fid int - file identifier for auth file
	 * @param uid String - user name
	 * @param aname String - name of attached namespace
	 * @return Result
	 */
	protected Result auth (int fid, String uid, String aname) {
		
		// check fid.
		if (assoc.containsKey(fid))
			return new Result (Result.RC_FID_IN_USE, null);
		
		// create an authentication file.
		// This is no visible in the namespace because
		// it is not linked to any container (directory)
		AuthEntry auth = new AuthEntry ();
		Permissions perm = new Permissions ("auth", "auth", Permissions.PERM_666);
		auth.init ("AUTH", perm, "  ");
		// associate it with fid.
		assoc.put (fid, auth);
		
		// set result values.
		int rc = (auth != null ? Result.RC_OK : Result.RC_NO_AUTH_ENTRY);
		return new Result (rc, auth);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Client authenticates with server as user UID and provides a fid
	 * point to the filesystem ANAME of server's tree, server provides
	 * its unique identifier, QID, for the root of the filesystem to
	 * which the client is attached. This is usually the first message
	 * pair that transpires in a connection of a client to a server.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param afid int - authentication file identifier (or -1)
	 * @param uid String - user name (client)
	 * @param aname String - name to attach to (not used)
	 * @param cr Credential - user credential
	 * @return Result
	 */
	protected Result attach (int fid, int afid, String uid, String aname, Credential cr) {
		// check for authentication fid
		if (afid != -1) {
			// get associated entry
			Entry e = assoc.get (afid);
			if (e instanceof AuthEntry) {
				// we have an authentication entry.
				AuthEntry auth = (AuthEntry) e;
				// check credential
				Credential newCr = auth.getCredential();
				if (newCr == null || !newCr.isAuthenticated())
					return new Result (Result.RC_NOT_AUTHENTICATED, auth);

				// overwrite current credential with
				// credential from authentication.
				auth.updateCredential (cr);
			} else
				// error condition
				return new Result (Result.RC_NO_AUTH_ENTRY, e);
		}
		// check for required authentication
		if (withAuth && !cr.isAuthenticated())
			// not authenticated
			return new Result (Result.RC_NOT_AUTHENTICATED, null);

		// if we have an empty credential (if no authentication
		// is required and performed), initialize it with
		// the user name specified in the attach call.
		if (!withAuth && cr.setUser (uid)) {
			// we need to fill an empty credential.
			cr.addGroup (uid);
		}
		
		// get associated namespace
		ns = nsHdlr.getUserNS (uid);
		if (ns == null)
			return new Result (Result.RC_NO_NAMESPACE_AVAIL, null);
		
		// get root directory of attached (mounted) namespace...
		Directory root = ns.getRoot();
		// ... and remember association with fid.
		assoc.put (fid, root);
		// return root directory.
		return new Result (Result.RC_OK, root);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Have new fid, NEWFID, associate with the file associated with an
	 * existing fid. This usually occurs right before a fid is moved to
	 * point to another entry in the current directory with a Walk
	 * transaction.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param newFid int - cloned file identifier
	 * @return Result
	 */
	protected Result clone (int fid, int newFid) {
		// get entry associated with old fid
		Entry e = assoc.get (fid);
		if (e == null)
			return new Result (Result.RC_NO_ENTRY, null);

		// associate with new fid
		assoc.put  (newFid, e);
		return new Result (Result.RC_OK, e);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Associate FID with file named NAME in the current directory,
	 * server provides its unique QID for the file the FID now points to.</p>
	 * @param fid int - file identifier of directory
	 * @param newFid int - file identifier for entry (or -1)
	 * @param name String - walk to entry with given name
	 * @return Result
	 */
	protected Result walk (int fid, int newFid, String[] names) {
		
		// get directory associated with fid.
		Entry e = assoc.get (fid);
		if (e == null || !(e instanceof Directory))
			return new Result (Result.RC_NO_PARENT_DIRECTORY, e);

		// check newFid
		if (fid != newFid && assoc.containsKey(newFid))
			return new Result (Result.RC_FID_IN_USE, e);
		
		// process all names in list (path fragments)
		int count = names.length;
		Entry[] list = new Entry [count];
		int pos = 0;
		for (int n = 0; n < count; n++) {
			// go down the directories...
			if (e instanceof Directory) {
				Directory dir = (Directory) e;
				e = list[pos++] = dir.getEntryByName (names[n]);
				if (e == null) {
					pos--;
					break;
				}
			}
			// if one entry is not a directory
			// the walk terminates.
			else if (n < count-1)
				break;
		}
		// check if the (end-point) file exists
		if (pos == count) {
			// set new association for last component
			if (newFid != NOFID)
				fid = newFid;
			// we have a new association for fid.
			assoc.put  (fid, e);
		} else {
			// truncate result list
			Entry[] trunc = new Entry [pos];
			System.arraycopy (list, 0, trunc, 0, pos);
			list = trunc;
		}
		
		// return result
		Result res = new Result();
		res.rc = Result.RC_OK;
		res.ent = list;
		return res;
	}
	//-----------------------------------------------------------------
	/**
	 * <p<Remove the association between an entry in the namespace of
	 * the server and a fid.</p>
	 * @param fid int - file identifier (entry in namespace)
	 */
	protected void clunk (int fid) {
		// release fid
		releaseFid (fid);
		// remove association and handle
		assoc.remove (fid);
		accessors.remove (fid);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Retrieve file attributes. The format and contents of
	 * the attributes depend on the underlying protocol version.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @return byte[] - binary entry status data
	 */
	protected byte[] stat (int fid) {
		// get entry associated with fid
		Entry e = assoc.get (fid);
		if (e == null)
			return null;
		return getStat(e);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set file attributes.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param statData byte[] - entry attributes to be set
	 * @param cr Credential - user credential
	 * @return int - return code of operation (0=OK)
	 */
	protected int wstat (int fid, byte[] statData, Credential cr) {
		// get entry associated with fid
		Entry e = assoc.get (fid);
		if (e == null)
			return -1;
		// read attributes
		// set as new attributes
		if (!setStat (e, statData, cr))
			return -2;
		// report success.
		return 0;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check permissions and open file associated with fid, server
	 * returns its unique identifier for the file, the QID.</p>
	 * <p>The open request asks the file server to check permissions
     * and prepare a fid for I/O with subsequent read and write
     * messages. The mode field determines the type of I/O: 0
     * (called OREAD in Sys), 1 (OWRITE), 2 (ORDWR), and 3 (OEXEC)
     * mean read access, write access, read and write access, and
     * execute access, to be checked against the permissions for
     * the file. In addition, if mode has the OTRUNC (16r10) bit
     * set, the file is to be truncated, which requires write
     * permission (if the file is append-only, and permission is
     * granted, the open succeeds but the file will not be
     * truncated); if the mode has the ORCLOSE (16r40) bit set, the
     * file is to be removed when the fid is clunked, which
     * requires permission to remove the file from its directory.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param mode int - access mode
	 * @param cr Credential - user credential
	 * @return Result
	 */
	protected Result open (int fid, int mode, Credential cr) {

		// get entry associated with fid
		Entry e = assoc.get (fid);
		if (e == null)
			return new Result (Result.RC_NO_ENTRY, null);

		// check permission and get file handle
		Handle hdl = e.open (cr, mode);
		if (hdl.rc != Handle.RC_OK) {
			switch (hdl.rc) {
				case Handle.RC_IN_EXCLUSIVE_USE:	return new Result (Result.RC_IN_EXCLUSIVE_USE, e);
				case Handle.RC_NO_PERMISSION:		return new Result (Result.RC_NO_PERMISSION, e);
				case Handle.RC_NO_SUCH_FILE:		return new Result (Result.RC_NO_ENTRY, e);
				default:							return new Result (Result.RC_OPEN_FAILED, e);
			}
		}
		// associate handle with fid
		accessors.put (fid, hdl);
		
		// return entry
		return new Result (Result.RC_OK, e);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Create and open a file named NAME in the current directory with
	 * permissions PERM, and associate it with fid. If the most significant
	 * bit of mode is set, the created file is a directory.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param name String - name of new entry
	 * @param perm int - access permissions
	 * @param mode int - access mode
	 * @param cr Credential - user credential
	 * @return Result
	 */
	protected Result create (int fid, String name, int perm, int mode, Credential cr) {

		// get directory associated with fid.
		Entry e = assoc.get (fid);
		if (e == null || !(e instanceof Directory))
			return new Result (Result.RC_NO_PARENT_DIRECTORY, e);
		Directory dir = (Directory) e;

		// allocate new entry
		e = dir.create ((mode & 0x80) != 0, name, perm, cr);
		if (e == null)
			return new Result (Result.RC_CREATE_FAILED, null);
		// set new qidPath
		e.setId (ns.getNextId());

		// associate new entry with fid.
		assoc.put  (fid, e);
		
		// open new file
		Handle hdl = e.open (cr, mode);
		if (hdl.rc != Handle.RC_OK) {
			switch (hdl.rc) {
				case Handle.RC_IN_EXCLUSIVE_USE:	return new Result (Result.RC_IN_EXCLUSIVE_USE, e);
				case Handle.RC_NO_PERMISSION:		return new Result (Result.RC_NO_PERMISSION, e);
				default:							return new Result (Result.RC_CREATE_FAILED, e);
			}
		}
		// associate handle with fid
		accessors.put (fid, hdl);

		int rc = (hdl.rc == Handle.RC_OK ? Result.RC_OK : Result.RC_OPEN_FAILED);
		return new Result (rc, e);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read COUNT bytes from offset OFFSET from file associated with
	 * fid, server returns number of bytes read and data.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param offset long - offset into content
	 * @param count int - number of bytes to read
	 * @return byte[] - read data
	 */
	protected byte[] read (int fid, long offset, int count) {
		// get entry associated with fid
		Entry e = assoc.get (fid);
		if (e == null)
			return null;
		// get handle to file (from open)
		Handle hdl = accessors.get (fid);
		if (hdl == null && !(e instanceof AuthEntry))
			return null;
		// read data from entry.
		return e.read (hdl, offset, count, this);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Write COUNT bytes to file associated with fid at offset OFFSET,
	 * server returns number of bytes successfully written.</p>
	 * @param fid int - file identifier (entry in namespace)
	 * @param offset long - offset into data
	 * @param count int - number of bytes to be written
	 * @param data byte[] - data buffer
	 * @return int - number of bytes written
	 */
	protected int write (int fid, long offset, int count, byte[] data) {
		// get entry associated with fid
		Entry e = assoc.get (fid);
		if (e == null)
			return 0;
		// get handle to file (from open)
		Handle hdl = accessors.get (fid);
		if (hdl == null && !(e instanceof AuthEntry))
			return 0;
		// write data to entry and return number of bytes written.
		return e.write (hdl, data, offset, count);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Remove file associated with fid.</p> 
	 * @param fid int - file identifier (entry in namespace)
	 */
	protected void remove (int fid) {
		// get associated entry
		Entry e = assoc.get (fid);
		// drop associations
		assoc.remove(fid);
		accessors.remove (fid);
		// unlink from namespace
		e.remove();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Interrupt pending operation with message tag OLDTAG.</p> 
	 * @param oldTag int - tag of pending operation
	 */
	protected void flush (int oldTag) {
	}
	
	//=================================================================
	/**
	 * <p>Release a fid (and all associated references).
	 * Does not remove the fid from lists!!</p> 
	 * @param fid int - fiel identifier to be released
	 */
	private void releaseFid (int fid) {
		
		// release handle
		Handle hdl = accessors.get (fid);
		if (hdl != null) {
			hdl.release();
		
			// release entry
			Entry ent = assoc.get (fid);
			ent.release();
		}
	}
}

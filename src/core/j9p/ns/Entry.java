
//******************************************************************
//*   PGMID.        ABSTRACT BASE CLASS FOR NAMESPACE ENTRIES.     *
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

package j9p.ns;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.util.Date;
import java.util.Hashtable;
import j9p.ns.handlers.Process;
import j9p.auth.Credential;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A namespace <b>Entry</b> can either be a container for other entries
 * (Directory) or a leaf in the namespace tree (e.g. a file).</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class Entry {
	
	//=================================================================
	/*
	 * additional mode flags
	 */
	public static final int DMDIR		= 0x80000000;	// file is a directory
	public static final int DMAPPEND	= 0x40000000;	// append to file only
	public static final int DMEXCL		= 0x20000000;	// exclusive use
	public static final int DMAUTH		= 0x08000000;	//
	public static final int DMTMP		= 0x04000000;	// transient file
	/**
	 * <p>Default device name for namespace entries.</p>
	 */
	protected static final String DEFAULT_DEVICE = "#y";	// default device name
	
	//=================================================================
	/**
	 * <p>Inner class representing the entry (file) attributes. This
	 * is independent from any attribute representations used in
	 * specific implementations of the Styx protocol (common enclosure).</p>
	 */
	public static class Attributes {
		
		public String		name;			// entry name
		public Permissions	perm;			// file permissions
		public int			flags;			// Styx flags (DM????)
		public long			qidPath;		// internal unique entry id
		public int			atime;			// time of last access
		public int			mtime;			// time of last modification
		public String		device;			// mount device
	}
	
	//=================================================================
	/**
	 * <p>Abstract base class for handling protocol-specific entry
	 * attributes.</p>
	 */
	public static abstract class AttributeHandler {
		
		//-------------------------------------------------------------
		/**
		 * <p>Get unique identifier of entry.</p>
		 * @param e Entry - namespace entry
		 * @return byte[] - unique identifier of entry 
		 */
		public abstract byte[] getQid (Entry e);
		
		//-------------------------------------------------------------
		/**
		 * <p>Get attributes for namespace entry.</p> 
		 * @param e Entry - namespace entry
		 * @return byte[] - file attributes
		 */
		public abstract byte[] getStat (Entry e);
		
		//-------------------------------------------------------------
		/**
		 * <p>Set attributes for namespace entry.</p> 
		 * @param e Entry - namespace entry
		 * @param stat byte[] - file attributes
		 * @param cr Credential - user credential
		 * @return boolean - attributes changed?
		 */
		public abstract boolean setStat (Entry e, byte[] stat, Credential cr);
		
		//-------------------------------------------------------------
		/**
		 * <p>Get file attributes of entry.</p>
		 * @param e Entry - namespace entry
		 * @return Attributes - file attributes
		 */
		protected final Attributes getAttributes (Entry e) {
			return e.stat;
		}
		//-------------------------------------------------------------
		/**
		 * <p>Set file attributes of namespace entry.</p>
		 * @param e Entry - namespace entry (file)
		 * @param attr Attributes - new file attributes
		 * @param cr Credential - user credential
		 * @return boolean - attributes changed?
		 */
		protected final boolean setAttributes (Entry e, Attributes attr, Credential cr) {
			// check permission to change attributes
			if (!e.stat.perm.canAccess (cr, Permissions.OWRITE, null))
				return false;
			
			// check attributes
			boolean valid = true; // @@@ checkAttributes();
			if (valid) {
				// set new attributes.
				e.stat = attr;
				return true;
			}
			return false;
		}
	}
	
	//=================================================================
	/**
	 * <p>A Handle encapsulates information related to an open file.
	 * It is invalidated as soon as the associated fid is clunked.</p>
	 */
	public static class Handle {
		
		//-------------------------------------------------------------
		/*
		 * Constants of return (status) code:
		 */
		public static final int RC_OK				= 0;
		public static final int RC_IN_EXCLUSIVE_USE	= 1;
		public static final int RC_NO_PERMISSION	= 2;
		public static final int RC_NO_SUCH_FILE		= 3;
		
		//-------------------------------------------------------------
		/*
		 * Attributes:
		 */
		public int			rc = -1;				// return (status) code
		public boolean		forRead = false;		// opened for read
		public boolean		forWrite = false;		// opened for write
		public long			lastReadOfs = -1;		// last offset for read
		public int			lastReadNum = 0;		// number of bytes in last read
		public long			lastWriteOfs = -1;		// last offset for write
		public int			lastWriteNum = 0;		// number of bytes in last write
		public Credential	userCredential = null;	// user credentials
		
		//-------------------------------------------------------------
		/**
		 * <p>Release handle instance (clean-up).</p> 
		 */
		public void release () { }
	}
	
	//=================================================================
	//=================================================================
	/*
	 *	Attributes: 
	 */
	protected Directory		parent = null;		// reference to parent (directory)
	protected Attributes	stat = null;		// reference to file attributes
	protected int			inUse = 0;			// number of accessors
	
	//=================================================================
	/**
	 * <p>Instantiate a new Entry.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public Entry (String name, Permissions perm, String device) {
		this();
		init (name, perm, device);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate a new Entry without initialization. This
	 * call is only used during entry creation of files handled
	 * by implementations of the Directory or File interface.</p>
	 */
	protected Entry () {
		parent = null;
		stat = new Attributes();
		stat.name = null;
		inUse = 0;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Initialize </p> 
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public void init (String name, Permissions perm, String device) {
		// can only initialize un-initialized entries.
		if (stat.name != null)
			return;
		// allocate and set attributes
		stat.name = name;
		stat.perm = perm;
		stat.mtime = stat.atime = getEpoch();
		stat.device = (device == null ? DEFAULT_DEVICE : device);
	}

	//=================================================================
	/**
	 * <p>Start a (handler) process running in its own thread.
	 * Configuration parameters are passed in as a list
	 * of named parameters with string values.</p>
	 * <p>This method is overwitten by derived classes that
	 * require one or more handler concurrent threads.</p> 
	 * @param p Hashtable<String,String> - parameter set
	 * @return Process - started thread (or null)
	 */
	public Process startHandler (Hashtable<String,String> p) {
		return null;
	}

	//=================================================================
	//	Synchronize timestamps of entry.
	//=================================================================
	/**
	 * <p>Flag the entry as "modified".</p> 
	 */
	public void setModified() {
		stat.mtime = getEpoch();
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Flag the entry as "accessed".</p> 
	 */
	public void setAccessed() {
		stat.atime = getEpoch();
	}
	
	//=================================================================
	//	abstract methods for read, write and remove
	//=================================================================

	//-----------------------------------------------------------------
	/**
	 * <p>Open file for user with credential for access.</p>
	 * @param cr Credential - authenticated user credential
	 * @param mode int - access mode
	 * @return Handle - handle to opened file
	 */
	public Handle open (Credential cr, int mode) {
		// allocate handle
		Handle hdl = new Handle();
		
		// check exclusive use.
		if ((stat.flags & DMEXCL) != 0) {
			if (inUse > 0) {
				// can only be opened once.
				hdl.rc = Handle.RC_IN_EXCLUSIVE_USE;
				return hdl;
			}
		}
		// check access permissions.
		if (!stat.perm.canAccess (cr, mode, parent)) {
			// no access granted.
			hdl.rc = Handle.RC_NO_PERMISSION;
			return hdl;
		}
		
		// file "opened".
		inUse++;
		hdl.rc = Handle.RC_OK;
		hdl.forRead  = Permissions.withReadAccess (mode);
		hdl.forWrite = Permissions.withWriteAccess (mode);
		hdl.userCredential = cr;
		return hdl;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Release file (close).</p> 
	 */
	public void release () {
		inUse--;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes. The methods takes a AttributeHandler argument
	 * to be able to serve read requests on directory entries.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be read
	 * @param fmt EntryFormatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public abstract byte[] read (Handle hdl, long offset, int size, AttributeHandler fmt);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public abstract int write (Handle hdl, byte[] data, long offset, int size);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Remove entry from namespace</p> 
	 * @return boolean successful operation?
	 */
	public boolean remove () {

		// can't delete entry without parent
		// (e.g. root directory).
		if (parent == null)
			return false;

		// notify parent directory of deletion.
		parent.remove (this);
		// commit suicide...
		return true;
	}

	//=================================================================
	// Useful getter methods.
	//=================================================================
	
	/**
	 * <p>Get name of entry.</p>
	 * @return String - name of entry
	 */
	public String getName () {
		return stat.name;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Return a copy of the permission flags for an entry.</p>
	 * @return Permissions - file permissions
	 */
	public Permissions getPermissions () {
		return stat.perm.clone();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public abstract long getSize ();
	
	//=================================================================
	// helper methods to set internal attributes.
	//=================================================================
	
	/**
	 * <p>Set unique identifier for entry.</p>
	 * @param id long - entry identifier
	 * @return boolean - operation successful?
	 */
	public boolean setId (long id) {
		if (stat.qidPath != 0)
			return false;
		stat.qidPath = id;
		return true;
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Set parent reference for namespace entry.</p> 
	 * @param d Directory - parent directory
	 * @return boolean - successful operation?
	 */
	public boolean setParent (Directory d) {
		// can be set only once
		if (parent != null)
			return false;
		parent = d;
		return true;
	}

	//-----------------------------------------------------------------
	/**
	 * <p>Set access properties for entry.</p>
	 * @param uid String - user id
	 * @param gid String - group id
	 * @param mode int - permission flags
	 */
	public void setPermissions (String uid, String gid, Integer mode) {
		if (uid != null)  stat.perm.setOwner (uid);
		if (gid != null)  stat.perm.setGroup (gid);
		if (mode != null) stat.perm.setMode  (mode);
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get epoch value (seconds since January 1st, 1970)</p>
	 * @return int - epoch
	 */
	private int getEpoch() {
		return (int) (new Date().getTime() / 1000);
	}
}

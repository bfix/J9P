
//******************************************************************
//*   PGMID.        LOGICAL NAMESPACE DIRECTORY.                   *
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

package j9p.ns.handlers;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.util.Vector;
import j9p.auth.Credential;
import j9p.ns.Directory;
import j9p.ns.Entry;
import j9p.ns.Permissions;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>LogicalDirectory</b> is a namespace entry that only exists
 * during a server session and is not persistent across server
 * invocations. It is used to construct artificial namespaces.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class LogicalDirectory extends Directory {

	//=================================================================
	/**
	 * <p>List of contained entries.</p>
	 */
	protected Vector<Entry> list;
	/**
	 * <p>Directory listing (for "read" operations).</p>
	 */
	protected byte[] dirList;
	/**
	 * <p>Modified directory content.</p>
	 */
	protected boolean dirty;
	/**
	 * <p>Directory listing in use?</p>
	 */
	protected boolean inUse;
	
	//=================================================================
	/**
	 * <p>Instantiate a new logical directory in the namespace.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public LogicalDirectory (String name, Permissions perm, String device) {
		super (name, perm, device);
		prepare();
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	public LogicalDirectory () {
		prepare();
	}

	//-----------------------------------------------------------------
	/**
	 * <p>Initialize object.</p>
	 */
	protected void prepare () {
		list = new Vector<Entry>();
		dirList = null;
		dirty = true;
		inUse = false;
	}
	
	//=================================================================
	/**
	 * <p>Remove directory from namespace</p> 
	 * @return boolean successful operation?
	 */
	public boolean remove () {
		
		// can't delete a directory that is not empty.
		if (list.size() > 0)
			return false;
		// basic removal
		return super.remove();
	}

	//=================================================================
	//	Manage list of contained entries.
	//=================================================================
	/**
	 * <p>Get number of entries in this directory.</p>
	 * @return int - number of directory entries
	 */
	public int numEntries () {
		return list.size();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get indexed entry in directory.</p>
	 * @param pos int - directory index
	 * @return Entry - selected entry
	 */
	public Entry getEntryAt (int pos) {
		return list.elementAt (pos);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get named namespace entry.</p>
	 * @param name String - name of entry (sub-directory)
	 * @return Entry - associated namespace entry
	 */
	public Entry getEntryByName (String name) {
		for (Entry e : list) {
			if (name.equals(e.getName()))
				return e;
		}
		// not found
		return null;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add entry to directory.</p>
	 * @param e Entry - entry to be added
	 * @return boolean - successful operation
	 */
	public boolean add (Entry e) {
		list.add (e);
		e.setParent (this);
		setModified();
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Remove entry from directory.</p>
	 * @param e Entry - entry to be removed
	 * @return boolean - remove successful?
	 */
	public boolean remove (Entry e) {
		setModified();
		return list.remove(e);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Create a new entry with given name and access mode.
	 * The user credential is used to check permissions.</p>
	 * @param asDir boolean - create (sub-)directory? 
	 * @param name String - name of entry
	 * @param perm int - access mode (permissions)
	 * @param cr Credential - user credential
	 * @return Entry - new entry (or null)
	 */
	public Entry create (boolean asDir, String name, int perm, Credential cr) {
		
		// check if we can write to this directory.
		Permissions permDir = getPermissions();
		if (!permDir.canAccess (cr, Permissions.OWRITE, parent))
			// create not allowed in directory
			return null;
		
		// assemble entry permissions.
		Permissions permEnt = new Permissions (cr.getUser(), cr.getGroup(), perm);
		
		// allocate new entry
		Entry e = (asDir ?
				new LogicalDirectory (name, permEnt, DEFAULT_DEVICE)
			:	new LogicalFile      (name, permEnt, DEFAULT_DEVICE)
		);
		// add entry to directory.
		if (add (e))
			return e;
		// report failure.
		return null;
	}
	
	//=================================================================
	/**
	 * <p>Flag the directory as "modified".</p> 
	 */
	public void setModified() {
		super.setModified();
		dirty = true;
	}
	
	//=================================================================
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset int - offset into entry content
	 * @param size int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int size, AttributeHandler fmt) {
		// flag entry as accessed.
		setAccessed();
		
		// assemble directory listing if required.
		if (dirty || dirList == null)
			generateListing (fmt);
		inUse = true;

		// check bounds
		int pos = (int) offset;
		int listSize = dirList.length;
		if (pos < 0 || pos > listSize-1 || size < 0)
			return null;
		
		// create response
		int count = Math.min (size, listSize - pos);
		byte[] res = new byte [count];
		System.arraycopy (dirList, pos, res, 0, count);
		
		// release listing after final read.
		if ( (pos+size) > listSize)
			inUse = false;
		
		// return content.
		return res;
	}
	
	//=================================================================
	/**
	 * <p>Generate directory listing. The listing is probably only
	 * available during read operations.</p> 
	 * @param fmt Formatter - protocol-specific entry representation
	 */
	private void generateListing (AttributeHandler fmt) {
		
		// traverse entries in directory
		Blob res = new Blob();
		for (Entry e : list) {
			byte[] stat = fmt.getStat (e);
			res.putArray (stat);
		}
		dirList = res.asByteArray(false);
	}
}

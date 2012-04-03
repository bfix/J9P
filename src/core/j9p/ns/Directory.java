
//******************************************************************
//*   PGMID.        ABSTRACT BASE FOR NAMESPACE DIRECTORIES.       *
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

import j9p.auth.Credential;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A Directory is a container of namespace entries (either files or other
 * directories) and helps build tree-like namespace structures.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 * @see j9p.ns.Entry
 */
public abstract class Directory extends Entry {
	
	//=================================================================
	/**
	 * <p>Create a new directory of given name.</p> 
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public Directory (String name, Permissions perm, String device) {
		super (name, perm, device);
		stat.flags = Entry.DMDIR;
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	protected Directory () {
		stat.flags = Entry.DMDIR;
	}
	
	//=================================================================
	//	Manage list of contained entries.
	//=================================================================
	/**
	 * <p>Get number of entries in this directory.</p>
	 * @return int - number of directory entries
	 */
	public abstract int numEntries ();
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get indexed entry in directory.</p>
	 * @param pos int - directory index
	 * @return Entry - selected entry
	 */
	public abstract Entry getEntryAt (int pos);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get named namespace entry.</p>
	 * @param name String - name of entry (sub-directory)
	 * @return Entry - associated namespace entry
	 */
	public abstract Entry getEntryByName (String name);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Add entry to directory.</p>
	 * @param e Entry - entry to be added
	 * @return boolean - successful operation
	 */
	public abstract boolean add (Entry e);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Remove entry from directory.</p>
	 * @param e Entry - entry to be removed
	 * @return boolean - remove successful?
	 */
	public abstract boolean remove (Entry e);
	
	//=================================================================
	//	abstract methods for directory operations
	//=================================================================

	/**
	 * <p>Create a new entry with given name and access mode.
	 * The user credential is used to check permissions.</p>
	 * @param asDir boolean - create (sub-)directory? 
	 * @param name String - name of entry
	 * @param mode int - access mode (permissions)
	 * @param cr Credential - user credential
	 * @return Entry - new entry (or null)
	 */
	public abstract Entry create (boolean asDir, String name, int mode, Credential cr);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes. This method fails for directories!</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset int - offset into entry content
	 * @param size int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public final int write (Handle hdl, byte[] data, long offset, int size) {
		return 0;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public final long getSize () {
		// directory size is zero by convention.
		return 0;
	}
}

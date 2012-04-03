
//******************************************************************
//*   PGMID.        PERMISSIONS FOR NAMESPACE ENTRY.               *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/07.                                      *
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
 * <p>A <b>Permissions</b> object describes the access permissions
 * for a namespace entry.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Permissions {

	//=================================================================
	/*
	 * Constants: Permission flags (Owner,Group,All)
	 * for operations (Read,Write,eXecute).
	 */
	public static final int PERM_OR 	= 0x100;		// Owner Read
	public static final int PERM_OW 	= 0x080;		// Owner Write
	public static final int PERM_OX		= 0x040;		// Owner eXecute
	public static final int PERM_GR 	= 0x020;		// Group Read
	public static final int PERM_GW 	= 0x010;		// Group Write
	public static final int PERM_GX 	= 0x008;		// Group eXecute
	public static final int PERM_AR		= 0x004;		// All Read
	public static final int PERM_AW 	= 0x002;		// All Write
	public static final int PERM_AX 	= 0x001;		// All eXecute
	
	public static final int PERM_400	= PERM_OR;
	public static final int PERM_444	= PERM_OR  | PERM_GR | PERM_AR;
	public static final int PERM_600	= PERM_400 | PERM_OW;
	public static final int PERM_644	= PERM_600 | PERM_GR | PERM_AR;
	public static final int PERM_666	= PERM_644 | PERM_GW | PERM_AW;
	public static final int PERM_700	= PERM_600 | PERM_OX;
	public static final int PERM_755	= PERM_700 | PERM_GR | PERM_GX
												   | PERM_AR | PERM_AX;
	/*
	 * Constants for access mode.
	 */
	public static final int OREAD	= 0;	// read access
	public static final int OWRITE	= 1;	// write access
	public static final int ORDWR	= 2;	// read/write request
	public static final int OEXEC	= 3;	// execute request
	public static final int OTRUNC	= 0x10;	// truncate file
	public static final int OCLOSE	= 0x40;	// remove on close
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private String	owner;		// file owner (user id)
	private String	group;		// associated group (group id)
	private int		perm;		// permission flags
	
	//=================================================================
	/**
	 * <p>Instantiate a new Permissions object with the specified
	 * parameter values.</p> 
	 * @param uid String - user id of owner
	 * @param gid String - group id of owner
	 * @param mode int - permission flags (PERM_??)
	 */
	public Permissions (String uid, String gid, int mode) {
		owner = uid;
		group = gid;
		setMode (mode);
	}
	
	//=================================================================
	/**
	 * <p>Check if credential allows access to file. The mode parameter
	 * specifies the access mode (read, write, delete, create).</p>
	 * @param cr Credential - authenticated user credential
	 * @param mode int - access mode
	 * @return boolean - operation allowed?
	 */
	public boolean canAccess (Credential cr, int mode, Entry parent) {
		
		// assemble permission mask for entry
		int maskEntry = 0;
		switch (mode & 3) {
			case OREAD:		maskEntry = 4;	break;
			case OWRITE:	maskEntry = 2;	break;
			case ORDWR:		maskEntry = 6;	break;
			case OEXEC:		maskEntry = 1;	break;
		}
		// handle OCLOSE: write access to parent directory
		if ((mode & OCLOSE) != 0) {
			// check permission on directory.
			if (!parent.getPermissions().canAccess (cr, OWRITE, null))
				return false;
		}
		// check global access permissions
		if ((perm & maskEntry) == maskEntry)
			return true;
		// check group permissions
		maskEntry <<= 3;
		if (cr.isMember (group) && (perm & maskEntry) == maskEntry)
			return true;
		// check owner permissions
		maskEntry <<= 3;
		if (owner.equals (cr.getUser()) && (perm & maskEntry) == maskEntry)
			return true;
		
		// not authorized
		return false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check if access mode includes read access.</p> 
	 * @param mode int - mode to be checked
	 * @return boolean - includes read access?
	 */
	public static boolean withReadAccess (int mode) {
		mode &= 3;
		return (mode == OREAD || mode == ORDWR || mode == OEXEC);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check if access mode includes write access.</p> 
	 * @param mode int - mode to be checked
	 * @return boolean - includes write access?
	 */
	public static boolean withWriteAccess (int mode) {
		mode &= 3;
		return (mode == OWRITE || mode == ORDWR);
	}
	
	//=================================================================
	//	Getter methods
	//=================================================================
	/**
	 * <p>Get user id of file owner.</p> 
	 * @return String - file owner
	 */
	public String getOwner() {
		return owner;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get group id for file.</p>
	 * @return String - group id
	 */
	public String getGroup() {
		return group;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get permission flags for file.</p>
	 * @return int - permission flags
	 */
	public int getMode () {
		return perm;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Return a copy of this permissions.</p>
	 * @return Permissions - cloned permissions
	 */
	public Permissions clone() {
		return new Permissions (owner, group, perm);
	}

	//=================================================================
	//	Setter methods
	//=================================================================
	/**
	 * <p>Set user id of file owner.</p>
	 * @param uid String - user id of owner
	 */
	public void setOwner (String uid) {
		owner = uid;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set identifier of associated group.</p>
	 * @param gid String - group id
	 */
	public void setGroup (String gid) {
		group = gid;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set permission flags.</p>
	 * @param m int - permission flags
	 */
	public void setMode (int m) {
		perm = m & 0x1FF;
	}
}

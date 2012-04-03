
//******************************************************************
//*   PGMID.        DBFS -- DATABASE SERVER STATUS FILE.           *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/03/27.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
//*                 AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.       *
//******************************************************************
//*                                                                *
//*  dbfs: J9P database filesystem.                                *
//*                                                                *
//*  Copyright (C) 2009-2012, Bernd R. Fix                         *
//*                                                                *
//*  This program is free software; you can redistribute it and/or *
//*  modify it under the terms of the GNU General Public License   *
//*  as published by the Free Software Foundation; either version  *
//*  3 of the License, or (at your option) any later version.      *
//*                                                                *
//*  This program is distributed in the hope that it will be use-  *
//*  ful, but WITHOUT ANY WARRANTY; without even the implied       *
//*  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR       *
//*  PURPOSE. See the GNU General Public License for more details. *
//*                                                                *
//*  You should have received a copy of the GNU General Public     *
//*  License along with this program; if not, see                  *
//*  <http://www.gnu.org/licenses/>.                               *
//*                                                                *
//******************************************************************

package j9p.example.dbfs;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.ns.File;
import j9p.ns.Permissions;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Read-only file that contains the database server status information.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerStatus extends File {
	
	//=================================================================
	/*
	 * Constants:
	 */
	private static final int UPD_PERIOD = 60;	// update every 60 sec
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private DatabaseEngine	db = null;			// reference to engine
	private String			statusMsg = "";		// last status info
	private long			lastCheck = 0;		// time of last check
	
	//=================================================================
	/**
	 * <p>Instantiate a new status file.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param db DatabaseEngine - underlying database engine
	 */
	public ServerStatus (String name, Permissions perm, DatabaseEngine db) {
		super (name, perm, "db");
		this.db = db;
	}

	//=================================================================
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset long - offset into entry content
	 * @param count int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int count, AttributeHandler fmt) {
		
		// get current status message
		updateStatus();
		
		// check bounds.
		byte[] data = statusMsg.getBytes();
		int size = data.length;
		if (offset < 0 || offset > size-1)
			return null;
		
		// assemble result.
		int num = (int) Math.min (size-offset, count);
		byte[] res = new byte [num];
		System.arraycopy (data, (int)offset, res, 0, num);
		return res;
	}
	
	//=================================================================
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes.</p>
	 * <p>This is a read-only file, so no writing happens...</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset long - offset into entry content
	 * @param count int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public int write (Handle hdl, byte[] data, long offset, int count) {
		return 0;
	}
	
	//=================================================================
	/**
	 * <p>Get size of entry (status message).</p>
	 * @return long - entry size
	 */
	public long getSize () {
		
		// get current status message
		updateStatus();
		
		// return our size
		return statusMsg.length();
	}
	
	//=================================================================
	/**
	 * <p>Update expired status information.</p> 
	 */
	private void updateStatus () {
		
		// check time since last update
		long now = System.currentTimeMillis() / 1000;
		if ((now - lastCheck) > UPD_PERIOD) {
			// get an updated status
			statusMsg = db.getStatus();
			lastCheck = now;
		}
	}
}

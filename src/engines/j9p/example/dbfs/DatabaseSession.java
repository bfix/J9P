
//******************************************************************
//*   PGMID.        DBFS -- DATABASE SESSION FILE.                 *
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

import java.sql.Connection;
import j9p.ns.File;
import j9p.ns.Directory;
import j9p.ns.Permissions;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>File used to establish database connections.</p>
 * <p>In order to establish a database connection, additional authentication
 * information is required for the SQL server - a role (user) and the
 * corresponding password. This is supplied by writing the information as
 * a string ("user:password") to this file.</p>
 * <p>Reading the file will return a session identifier (if the session was
 * established successfully) or an error message.</p>
 * <p>If a session id is returned, the handler will automatically allocate a
 * new session directory and populate it with the necessary files.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class DatabaseSession extends File {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private DatabaseEngine	db = null;			// reference to engine
	private String			content = "";		// file content (dynamic)
	private int				nextSessionId = 0;	// next session id
	private String			dbInst = null;		// reference to database
	private String			dbAuth = null;		// authentication data
	
	//=================================================================
	/**
	 * <p>Instantiate a new session file.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param db DatabaseEngine - underlying database engine
	 * @param instance String - name of db instance
	 */
	public DatabaseSession (String name, Permissions perm, DatabaseEngine db, String instance) {
		super (name, perm, "db");
		this.db = db;
		dbInst = instance;
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
		
		// check for auth data.
		if (offset == 0) {
			String dbUser = null, dbPassword = null;
			if (dbAuth != null && dbAuth.length() > 0) {
				int pos = dbAuth.indexOf (':');
				if (pos != -1) {
					dbUser = dbAuth.substring (0, pos);
					dbPassword = dbAuth.substring (pos+1);
				}
				// reset auth string
				dbAuth = null;
			}
		
			// connect to database
			if (dbUser != null) {
				// establish session with database
				String sessionId = "" + nextSessionId;
				Connection conn = db.connect (dbInst, dbUser, dbPassword);
				if (conn != null) {

					// create subdirectory.
					String client = hdl.userCredential.getUser();
					String group = hdl.userCredential.getGroup();
					Permissions perm = new Permissions (client, group, Permissions.PERM_400);
					Directory session = new SessionDirectory (sessionId, perm, conn);
					parent.add (session);
				
					content = sessionId + "\n";
					nextSessionId++;
				} else
					content = "Can't connect to database!\n";
			} else
				content = "No (valid) authentication data!\n";
		}
		
		// check bounds.
		byte[] data = content.getBytes();
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
		if (offset == 0)
			dbAuth = "";
		dbAuth += new String (data, (int)offset, count);
		return 0;
	}
	
	//=================================================================
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public long getSize () {
		
		// check for writes
		if (dbAuth != null)
			// return current file length
			return dbAuth.length();
		
		// return no size otherwise
		return 0;
	}
}

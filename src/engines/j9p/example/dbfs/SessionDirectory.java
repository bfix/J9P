
//******************************************************************
//*   PGMID.        DBFS -- DATABASE SESSION DIRECTORY.            *
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import j9p.ns.File;
import j9p.ns.Permissions;
import j9p.ns.handlers.LogicalDirectory;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class SessionDirectory extends LogicalDirectory {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private Connection session = null;
	private String query = "";
	private String result = "";
	
	//=================================================================
	/**
	 * <p>Session-related query file: Writing a SQL command to this file
	 * will trigger the execution of the command on the associated database
	 * instance. Error/info messages and/or returned result sets are stored
	 * in the session result file in the same directory.</p>
	 */
	private class SessionQuery extends File {
		
		//-------------------------------------------------------------
		/**
		 * <p>Instantiate a new query file.</p>
		 * @param name String - name of entry
		 * @param perm Permissions - access permissions for file
		 */
		public SessionQuery (String name, Permissions perm) {
			super(name, perm, "db");
		}
		//-------------------------------------------------------------
		/**
		 * <p>Read entry content starting at offset for given number
		 * of bytes.</p>
		 * <p>This is a write-only file.</p>
		 * @param hdl Handle - handle to opened file
		 * @param offset long - offset into entry content
		 * @param count int - number of bytes to be read
		 * @param fmt Formatter - protocol-specific entry representation
		 * @return byte[] - read content
		 */
		public byte[] read(Handle hdl, long offset, int size, AttributeHandler fmt) {
			return null;
		}
		//-------------------------------------------------------------
		/**
		 * <p>Write entry content starting at offset for given number
		 * of bytes.</p>
		 * @param hdl Handle - handle to opened file
		 * @param data byte[] - data to be written
		 * @param offset long - offset into entry content
		 * @param count int - number of bytes to be written
		 * @return int - number of bytes written
		 */
		public int write(Handle hdl, byte[] data, long offset, int size) {
			// reset content on initial write
			if (offset == 0)
				query = "";
			String in = new String (data, (int)offset, size);
			query += in;
			
			// try to process query.
			processQuery();
			
			// return number of bytes written successfully.
			return in.length();
		}
		//-------------------------------------------------------------
		/**
		 * <p>Get size of entry (query length).</p>
		 * @return long - entry size
		 */
		public long getSize() {
			if (query == null)
				return 0;
			return query.length();
		}
	}

	//=================================================================
	/**
	 * <p>.</p>
	 */
	private class SessionResult extends File {

		//-------------------------------------------------------------
		/**
		 * <p>Instantiate a new result file.</p>
		 * @param name String - name of entry
		 * @param perm Permissions - access permissions for file
		 */
		public SessionResult (String name, Permissions perm) {
			super (name, perm, "db");
		}
		//-------------------------------------------------------------
		/**
		 * <p>Read entry content starting at offset for given number
		 * of bytes.</p>
		 * @param hdl Handle - handle to opened file
		 * @param offset long - offset into entry content
		 * @param count int - number of bytes to be read
		 * @param fmt Formatter - protocol-specific entry representation
		 * @return byte[] - read content
		 */
		public byte[] read(Handle hdl, long offset, int size, AttributeHandler fmt) {
			
			// check bounds.
			if (offset < 0)
				return null;		
			byte[] data = result.getBytes();
			int count = data.length;
			if (offset > count-1) {
				// we are reading beyond end of content.
				result = "";
				return null;
			}
			
			// assemble result.
			int num = (int) Math.min (count-offset, size);
			byte[] res = new byte [num];
			System.arraycopy (data, (int)offset, res, 0, num);
			return res;
		}
		//-------------------------------------------------------------
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
		public int write(Handle hdl, byte[] data, long offset, int size) {
			return 0;
		}
		//-------------------------------------------------------------
		/**
		 * <p>Get size of entry (result size).</p>
		 * @return long - entry size
		 */
		public long getSize() {
			if (result == null)
				return 0;
			return result.length();
		}
	}

	//=================================================================
	/**
	 * <p>Instantiate new session directory and populate it with
	 * the necessary files.</p>
	 * @param name String - name of session directory (session id)
	 * @param perm Permissions - permissions for entry
	 * @param conn Connection - established database session
	 */
	public SessionDirectory (String name, Permissions perm, Connection conn) {
		super (name, perm, "db");
		session = conn;
		
		// create interaction files.
		Permissions permQ = perm.clone();
		permQ.setMode (Permissions.PERM_OW);
		File query = new SessionQuery ("query", permQ);
		add (query);
	
		Permissions permR = perm.clone();
		permR.setMode (Permissions.PERM_OR);
		File result = new SessionResult ("result", permR);
		add (result);
	}
	
	//=================================================================
	/**
	 * <p>Try to execute a query written to the query file.</p>
	 * <p>Commands are terminated by a ";\n" sequence.</p> 
	 */
	private void processQuery () {
		
		// find command terminator
		int pos = query.indexOf (";\n");
		if (pos < 0)
			return;
		
		// extract query to execute
		String cmd = query.substring (0, pos+1);
		query = query.substring (pos+2);
		System.out.println ("SQL cmd: '" + cmd + "'");
		
		// drop previous result
		result = "# @SQL@: " + cmd + "\n";
		
		try {
			// execute SQL command
			Statement stmt = session.createStatement();
			if (stmt.execute (cmd)) {
				// successful:
				StringBuffer buf = new StringBuffer();
				buf.append ("# @STATUS@: SUCCESSFUL\n");
				
				// get result
				ResultSet r = stmt.getResultSet();
				if (r != null) {
					// assemble column list
					ResultSetMetaData md = r.getMetaData();
					int numCol = md.getColumnCount();
					buf.append ("# @COLS@: ");
					for (int n = 0; n < numCol; n++) {
						buf.append (md.getColumnName (n+1));
						if (n < numCol-1)
							buf.append ("¦");
					}
					buf.append ("\n");
					
					// collect result set data
					StringBuffer recBuf = new StringBuffer();
					int numRec = 0;
					while (r.next()) {
						// assemble record.
						for (int n = 0; n < numCol; n++) {
							String data = r.getString (n+1);
							recBuf.append (convert(data));
							if (n < numCol-1)
								recBuf.append ("¦");
						}
						recBuf.append ("\n");
						numRec++;
					}
					// assemble result
					buf.append ("# @RESULT@: " + numRec + "\n");
					buf.append (recBuf);
				} else
					buf.append ("# @RESULT@: NONE\n");
				result += buf.toString();
			} else
				result += "# @STATUS@: FAILED!\n";
		} catch (SQLException e) {
			result += "# @STATUS@: FAILED! (" + e.getMessage() + ")\n";
		}
	}
	
	//=================================================================
	/**
	 * <p>Convert record field to appropriate string representation.</p> 
	 * @param s String - input string
	 * @return String - converted string
	 */
	private String convert (String s) {
		String res = "";
		while (s.length() > 0) {
			int pos = s.indexOf ('¦');
			if (pos < 0) {
				res += s;
				break;
			}
			res += s.substring (0, pos) + "|";
			s = s.substring (pos+1);
		}
		return res;
	}
}

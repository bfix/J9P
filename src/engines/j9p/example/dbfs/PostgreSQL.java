
//******************************************************************
//*   PGMID.        DBFS -- POSTGRESQL IMPLEMENTATION.             *
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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>PostgreSQL 8.1 server implementation of DatabaseEngine interface.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class PostgreSQL implements DatabaseEngine {

	//=================================================================
	/**
	 * <p>Status flag: database driver loaded and operational.</p>
	 */
	private static boolean loaded = false;
	
	//=================================================================
	// Static initializer
	static {
		try {
			// load the appropriate database driver.
			Class.forName ("org.postgresql.Driver");
			loaded = true;
		} catch (ClassNotFoundException e) {
			loaded = false;
			e.printStackTrace();
		}
	}

	//=================================================================
	/*
	 * Attributes
	 */
	private String dbHost = null;		// Host reference ("addr:port")
	private String errMsg = null;		// last error message
	
	//=================================================================
	/**
	 * <p>Connect to a database with given user credentials.</p>
	 * @param database String - name of database
	 * @param dbUser String - database user name
	 * @param password - user password
	 * @return Connection - established database connection (or null)
	 */
	public Connection connect (String database, String dbUser, String dbPassword) {
		
		// check for loaded database driver.
		if (!loaded || dbHost == null)
			return null;
		
		try {
			// connect to database server and return session instance
			String dbUrl = "jdbc:postgresql://" + dbHost + "/" + database;
			Connection session = DriverManager.getConnection (dbUrl, dbUser, dbPassword);
			return session;
		} catch (SQLException exc) {
			return null;
		}
	}

	//=================================================================
	/**
	 * <p>Enumerate databases on the server that are visible in
	 * the published namespace.</p>
	 * @return Vector<String> - list of database instance names
	 */
	public Vector<String> enumDatabases () {

		Vector<String> instances = new Vector<String>();
		Connection session = null;
		try {
			// connect to management database
			session = connect ("postgres", "postgres", "postgres");
			if (session == null) {
				errMsg = "Can't connect to management database";
				return instances;
			}
			Statement stmt = session.createStatement();
			// select all available database instances
			if (stmt.execute ("select datname from pg_database;")) {
				ResultSet r = stmt.getResultSet();
				if (r != null) {
					while (r.next()) {
						// add instance name to list
						String name = r.getString ("datname");
						instances.add (name);
					}
				}
			}
			errMsg = null;
		} catch (SQLException e) {
			errMsg = e.getMessage();
		}
		finally {
			// close opened session
			if (session != null)
				try { session.close(); } catch (SQLException e) { }
		}
		// return list of visible databases.
		return instances;
	}

	//=================================================================
	/**
	 * <p>Get status description of database server.</p>
	 * @return String - server status information
	 */
	public String getStatus () {
		
		// assemble server status
		String status = "PostgreSQL 8.1 database server\n";
		if (errMsg != null)
			status += "ERROR: " + errMsg + "\n";
		else
			status += "O.K.\n";
		return status;
	}
	
	//=================================================================
	//	Host reference handling
	//=================================================================
	/**
	 * <p>Set host reference ("addr:port").</p>
	 * @param dbHost String - host reference
	 */
	public void setHost (String dbHost) {
		this.dbHost = dbHost;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get host reference ("addr:port").</p>
	 * @return String - host reference
	 */
	public String getHost() {
		return dbHost;
	}
}

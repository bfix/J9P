
//******************************************************************
//*   PGMID.        DBFS -- DATABASE ENGINE INTERFACE.             *
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
import java.util.Vector;

///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Database engine interface: Implemented by derived classes
 * that talk to specific SQL databases.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public interface DatabaseEngine {

	//=================================================================
	/**
	 * <p>Connect to a database with given user credentials.</p>
	 * @param database String - name of database
	 * @param dbUser String - database user name
	 * @param password - user password
	 * @return Connection - established database connection (or null)
	 */
	Connection connect (String database, String dbUser, String password);
	
	//=================================================================
	/**
	 * <p>Enumerate databases on the server that are visible in
	 * the published namespace.</p>
	 * @return Vector<String> - list of database instance names
	 */
	Vector<String> enumDatabases ();
	
	//=================================================================
	/**
	 * <p>Get status description of database server.</p>
	 * @return String - server status information
	 */
	String getStatus ();
	
	//=================================================================
	//	Host reference handling
	//=================================================================
	/**
	 * <p>Set host reference ("addr:port").</p>
	 * @param dbHost String - host reference
	 */
	void setHost (String dbHost);
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get host reference ("addr:port").</p>
	 * @return String - host reference
	 */
	String getHost();
}


//******************************************************************
//*   PGMID.        DBFS -- SERVER DIRECTORY (NAMESPACE ROOT).     *
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

import java.util.Hashtable;
import java.util.Vector;
import j9p.ns.File;
import j9p.ns.Directory;
import j9p.ns.Permissions;
import j9p.ns.handlers.LogicalDirectory;
import j9p.ns.handlers.Process;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Database server directory (usually root of the published namespace).</p>
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerDirectory extends LogicalDirectory {
	
	//=================================================================
	/*
	 * Pre-defined permissions for namespace entries.
	 */
	private static final Permissions P755 = new Permissions ("dbfs", "dbfs", Permissions.PERM_755);
	private static final Permissions P444 = new Permissions ("dbfs", "dbfs", Permissions.PERM_444);
	private static final Permissions P644 = new Permissions ("dbfs", "dbfs", Permissions.PERM_644);
	private static final Permissions P666 = new Permissions ("dbfs", "dbfs", Permissions.PERM_666);

	//=================================================================
	/**
	 * Reference to associated database engine.
	 */
	private DatabaseEngine db = null;

	//=================================================================
	/**
	 * <p>Instantiate a new logical directory in the namespace.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public ServerDirectory (String name, Permissions perm, String device) {
		super (name, perm, device);
		prepare();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	public ServerDirectory () {
		prepare();
	}

	//=================================================================
	/**
	 * <p>Start a (handler) process running in its own thread.
	 * Configuration parameters are passed in as a list
	 * of named parameters with string values.</p>
	 * <p>We return no process instance since no concurrent
	 * handling is required.</p>
	 * @param p Hashtable<String,String> - parameter set
	 * @return Process - started thread (or null)
	 */
	public Process startHandler (Hashtable<String,String> p) {
		
		// instantiate and initialize new database engine.
		String dbEngine = p.get ("dbEngine");
		String dbHost = p.get ("dbHost");
		try {
			Class<?> cl = Class.forName (dbEngine);
			db = (DatabaseEngine) cl.newInstance();
			db.setHost (dbHost);
		} catch (Exception e) {
			db = null;
			e.printStackTrace();
		}
		
		// populate dbfs root directory
		// 1.) Status file
		add (new ServerStatus ("status", P644, db));
		
		// 2.) databases directory
		Directory dbs = new LogicalDirectory ("databases", P755, "db");
		add (dbs);
		
		// populate databases directory
		Vector<String> instances = db.enumDatabases();
		for (String name : instances) {
			
			// create new logical directory.
			Directory base = new LogicalDirectory (name, P444, "db");
			dbs.add (base);
			
			// populate database instance directory.
			File clone = new DatabaseSession ("session", P666, db, name);
			base.add (clone);
		}
		// no concurrent process spawned
		return null;
	}
}

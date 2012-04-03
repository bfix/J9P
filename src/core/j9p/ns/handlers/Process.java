
//******************************************************************
//*   PGMID.        NAMESPACE HANDLER PROCESS.                     *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/31.                                      *
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

import java.util.Hashtable;
import j9p.ns.Entry;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>Process</b> is a concurrent thread that controls the behavior
 * of a namespace entry (usually a file).</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class Process extends Thread {
	
	//=================================================================
	//	Process control:
	//=================================================================
	/**
	 * <p>Initialize a process adaptor with parameters.</p>
	 * @param e Entry - associated namespace entry 
	 * @param p Hashtable<String,String> - list of parameter definitions
	 */
	public void init (Entry e, Hashtable<String,String> p) {
		running = true;
		ent = e;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Terminate running engine request.</p> 
	 */
	public void terminate () {
		running = false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Is engine still running?</p>
	 * @return boolean - engine up and running
	 */
	public boolean isActive() {
		return running;
	}
	//-----------------------------------------------------------------
	/*
	 * Attributes:
	 */
	private boolean running = false;
	protected Entry ent = null;
}

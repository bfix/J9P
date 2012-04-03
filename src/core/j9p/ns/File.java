
//******************************************************************
//*   PGMID.        NAMESPACE FILE.                                *
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
/**
 * <p>A namespace <b>File</b> represents a leaf in the namespace tree
 * structure. The generic class is specifically implemented in derived
 * classes.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class File extends Entry {
	
	//=================================================================
	/**
	 * <p>Instantiate a new File with given name.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public File (String name, Permissions perm, String device) {
		super (name, perm, device);
	}

	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	protected File () {
	}
}


//******************************************************************
//*   PGMID.        NAMESPACE IMPLEMENTATION.                      *
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
 * <p>A <b>Namespace</b> is a (non-cyclic) tree-like structure. Branches
 * in this tree a represented by a <b>Directory</b> class; leafs are
 * represented by the <b>File</b> class.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Namespace {
	
	//=================================================================
	//	Attributes:
	//=================================================================
	/**
	 * <p>Static counter for id generation.</p>
	 */
	private static long lastQidPath = 0x00BF010100000001L;
	/**
	 * <p>Reference to root directory.</p>
	 */
	private Directory root = null;

	//=================================================================
	/**
	 * <p>Instantiate namespace from root directory.</p>
	 * @param root Directory - root directory of namespace
	 */
	public Namespace (Directory root) {
		// register root directory
		this.root = root;
		// initialize tree
		init (root);
	}
	
	//=================================================================
	/**
	 * <p>Get reference to root directory.</p>
	 * @return Directory - root of namespace
	 */
	public Directory getRoot () {
		return root;
	}
	
	//=================================================================
	/**
	 * <p>Helper: initialize directory tree for use as Styx
	 * namespace (recursive invocation on directory entries)</p>
	 * @param dir Directory - directory to be initialized
	 */
	private void init (Directory dir) {
		// initialize directory
		dir.setId (++lastQidPath | Entry.DMDIR);

		// traverse tree and initialize qids
		int count = dir.numEntries();
		for (int n = 0; n < count; n++) {
			Entry e = dir.getEntryAt (n);
			if (e instanceof Directory)
				init ((Directory) e);
			else
				e.setId (++lastQidPath);
		}
	}
	
	//=================================================================
	/**
	 * <p>Return next available unused entry identifier.</p>
	 * @return long - entry identifier
	 */
	public long getNextId () {
		return ++lastQidPath;
	}
}

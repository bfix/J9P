
//******************************************************************
//*   PGMID.        STYX LIBRARY ATTRIBUTES.                       *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/23.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
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

package j9p;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Helper class Library (module description).</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Library {

	//=================================================================
	/**
	 * <p>Return library version (major.minor)
	 * @return String - library version.
	 */
	public static String getVersion () {
		return "1.8";
	}
	
	//=================================================================
	/**
	 * <p>Get library author.</p>
	 * @return String - author of library
	 */
	public static String getAuthor () {
		return "Bernd R. Fix   >Y<";
	}
	
	//=================================================================
	/**
	 * <p>Get license description.</p>
	 * @return String - license text
	 */
	public static String getLicense () {
		return
			"(C) BY BERND R. FIX. ALL RIGHTS RESERVED.\n" +
			"LICENSED MATERIAL - PROGRAM PROPERTY OF THE\n" +
			"AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.\n" + 
			"DISTRIBUTED UNDER LGPL V3.";	
	}

	//=================================================================
	/**
	 * <p>Run default application in library.</p>
	 * @param argv String[] - command line arguments
	 */
	public static void main (String[] argv) {
		
		String delim = "=============================================";
		System.out.println (delim);
		System.out.println ("J9P/StyxLib library -- version " + getVersion());
		System.out.println (delim);
		System.out.println ("Author: " + getAuthor());
		System.out.println (delim);
		System.out.println ("Licence:");
		System.out.println (getLicense());
		System.out.println (delim);
	}
}

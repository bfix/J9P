
//******************************************************************
//*    PGMID.        COMMANDLINE ARGUMENTS/OPTIONS PARSER.         *
//*    AUTHOR.       BERND R. FIX   >Y<                            *
//*    DATE WRITTEN. 07/04/29.                                     *
//*    COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.     *
//*                  LICENSED MATERIAL - PROGRAM PROPERTY OF THE   *
//*                  AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.      *
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


package j9p.util;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.util.Hashtable;
import java.util.Vector;


///////////////////////////////////////////////////////////////////////////////
/**
 * Commandline arguments/option parser:<p>
 * 
 * @author  Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Args {

	//=================================================================
	/*
	 * Attributes:
	 */
	private Vector<String>    			arguments;	// positional arguments
	private Hashtable<String,String>	options;	// options
	
	//=================================================================
	/**
	 * Instantiate commandline parser.<p> 
	 * @param argv String[] - list of commandline fragments
	 * @param pattern String - option pattern
	 */
	public Args (String[] argv, String pattern) {
		
		// allocate lists
		arguments = new Vector<String>();
		options = new Hashtable<String,String>();
		
		// process all arguments
		for (int argc = 0; argc < argv.length; argc++) {
			String s = argv[argc];
			
			// option?
			if (s.charAt(0) == '-') {
				// yes: lookup in pattern
				char optCh = s.charAt(1);
				int idx = pattern.indexOf(optCh);
				// option with own argument?
				if (idx >= 0 && idx < pattern.length()-1) {
					char optFlag = pattern.charAt(idx+1);
					if (optFlag == ':')
						// option with follow-up argument
						options.put (s, argv[++argc]);
					else if (optFlag == '!')
						// option with direct sub-option
						options.put (s.substring(0,2), s.substring(2));
					else
						// simple option (no assigned argument or sub-option)
						options.put (s, "");
				} else
					// simple option (no assigned argument or sub-option)
					options.put (s, "");
			} else
				// not an option: position argument.
				arguments.add (s);
		}
	}
	
	//=================================================================
	//	Option handling
	//=================================================================

	/**
	 * Get option argument as string.<p>
	 * @param optName String - option qualifier
	 * @param defValue String - default argument
	 * @return String - option argument
	 */
	public String getStringOpt (String optName, String defValue) {
		String res = options.get (optName);
		return (res == null ? defValue : res);
	}

	//-----------------------------------------------------------------
	/**
	 * Get option argument as word.<p>
	 * @param optName String - option qualifier
	 * @param defValue int - default value
	 * @return int - option value
	 */
	public int getWordOpt (String optName, int defValue) {
		return getWord (getStringOpt (optName, null), defValue);
	}
	
	//-----------------------------------------------------------------
	/**
	 * Check for existing option.<p> 
	 * @param optName String - option qualifier
	 * @return boolean - option set
	 */
	public boolean getBoolOpt (String optName) {
		String res = options.get (optName);
		return (res != null);
		
	}
	
	//=================================================================
	//	Positional argument handling
	//=================================================================

	/**
	 * Get number of positional arguments.<p>
	 * @return int - number of positional arguments
	 */
	public int getNumArgs() {
		return arguments.size();
	}
	
	//-----------------------------------------------------------------
	/**
	 * Get positional argument as string.<p>
	 * @param pos int - argument position
	 * @param defValue String - default argument
	 * @return String - argument
	 */
	public String getStringArg (int pos, String defValue) {
		String res = arguments.elementAt (pos);
		return (res == null ? defValue : res);
	}
	
	//-----------------------------------------------------------------
	/**
	 * Get positional argument as word.<p>
	 * @param pos int - argument position
	 * @param defValue int - default value
	 * @return int - argument value
	 */
	public int getWordArg (int pos, int defValue) {
		return getWord (getStringArg (pos, null), defValue);
	}

	//=================================================================
	//	internal helper methods
	//=================================================================

	/**
	 * Get word from string representation.<p>
	 * @param s String - number as string
	 * @param defValue int - default value
	 * @return int - parsed number
	 */
	private int getWord (String s, int defValue) {
		if (s == null || s.length() == 0)
			return defValue;
		return Integer.parseInt (s);
	}
}

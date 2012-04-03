
//******************************************************************
//*    PGMID.        HEXIFICATION METHODS.                         *
//*    AUTHOR.       BERND R. FIX   >Y<                            *
//*    DATE WRITTEN. 09/01/03.                                     *
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

import java.util.Vector;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The <b>Hex</b> class provides static (global) methods for conversion
 * of intrinsic types into hex strings and vice versa.</p>
 * 
 * @author  Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Hex {

	//=================================================================
	/**
	 * <p>Ordered sequence of hexadecimal characters</p>
	 */
	private static final String hex = "0123456789ABCDEF"; 

	//=================================================================
	//	Conversion of intrinsic type to hex string
	//=================================================================
	/**
	 * <p>Hexadecimal representation of byte value.</p>
	 * @param b byte - value
	 * @return String - hexadecimal representation
	 */
	public static String fromByte (byte b) {
		return hex.charAt ((b >> 4) & 0xF) + "" + hex.charAt (b & 0xF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of short value.</p>
	 * @param s short - value
	 * @return String - hexadecimal representation
	 */
	public static String fromShort (short s) {
		return    fromByte ((byte)((s >> 8) & 0xFF))
				+ fromByte ((byte)(s & 0xFF));
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of integer value.</p>
	 * @param i int - value
	 * @return String - hexadecimal representation
	 */
	public static String fromInt (int i) {
		return	  fromShort ((short)((i >> 16) & 0xFFFF))
			  	+ fromShort ((short)(i & 0xFFFF));
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of long value.</p>
	 * @param l long - value
	 * @return String - hexadecimal representation
	 */
	public static String fromLong (long l) {
		return	  fromInt ((int)((l >> 32) & 0xFFFFFFFF))
				+ fromInt ((int)(l & 0xFFFFFFFF));
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of section of a byte array.
	 * Byte representations are separated by a delimiter character.</p>
	 * @param array byte[] - data
	 * @param pos int - offset into data 
	 * @param size int - size of section
	 * @param delim char - byte delimiter in hex string
	 * @return String - hexadecimal representation
	 */
	public static String fromArray (byte[] array, int pos, int size, char delim) {
		if (array == null)
			return "<null>";
		StringBuffer buf = new StringBuffer ();
		for (int n = 0; n < size; n++) {
			if (n > 0) buf.append (delim);
			buf.append (fromByte (array[pos+n]));
		}
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of a byte array.
	 * Byte representations are separated by a delimiter character.</p>
	 * @param array byte[] - data
	 * @param delim char - byte delimiter in hex string
	 * @return String - hexadecimal representation
	 */
	public static String fromArray (byte[] array, char delim) {
		if (array == null)
			return "<null>";
		return fromArray (array, 0, array.length, delim);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Hexadecimal representation of a byte array as a sequence of
	 * lines (of given byte count 'count' => line length = 3*count).
	 * Byte representations are separated by a delimiter character.</p>
	 * @param array byte[] - data to be converted
	 * @param delim char - byte delimiter in hex string
	 * @return Vector<String> - list of hexadecimal representation
	 */
	public static Vector<String> fromArraySplit (byte[] array, char delim, int count) {
		Vector<String> res = new Vector<String>();
		int size = array.length;
		for (int pos = 0; pos < size; pos += count) {
			int num = Math.min (count, size-pos);
			String line = fromArray (array, pos, num, delim);
			res.add (line);
		}
		return res;
	}

	//=================================================================
	// Conversion from hex string to intrinsic type
	//=================================================================
	/**
	 * <p>Convert hex string to byte value.</p>
	 * @param s String - hexadecimal representation
	 * @return byte - converted value
	 */
	public static byte toByte (String s) {
		return (byte)(toLong(s) & 0xFF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert hex string to short value.</p>
	 * @param s String - hexadecimal representation
	 * @return short - converted value
	 */
	public static short toShort (String s) {
		return (short)(toLong(s) & 0xFFFF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert hex string to integer value.</p>
	 * @param s String - hexadecimal representation
	 * @return int - converted value
	 */
	public static int toInt (String s) {
		return (int)(toLong(s) & 0xFFFFFFFF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert hex string to long value.</p>
	 * @param s String - hexadecimal representation
	 * @return long - converted value
	 */
	public static long toLong (String s) {
		long val = 0;
		int count = s.length(); 
		for (int n = 0; n < count; n++) {
			int k = hex.indexOf (s.charAt(n));
			if (k > -1)
				val = (val << 4) | k;
		}
		return val;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Convert hex string to byte array. Any non-hexadecimal
	 * characters (byte delimiters) are discarded from the input
	 * string. The input string can be of any length and is
	 * left padded with "0" if necessary (odd length).</p>
	 * @param s String - hexadecimal string
	 * @return byte[] - converted byte array
	 */
	public static byte[] toArray (String s) {
		
		// remove all non-hex characters
		int size = s.length();
		for (int n = 0; n < size; n++) {
			int k = hex.indexOf (s.charAt(n));
			if (k == -1) {
				s = s.substring (0, n) + s.substring (n+1);
				size--;
			}
		}
		// perform padding
		if ((size % 2) == 1) {
			s = "0" + s;
			size++;
		}
		// read bytes into result array
		size /= 2;
		byte[] res = new byte [size];
		for (int n = 0; n < size; n++)
			res[n] = toByte (s.substring (2*n, 2*n+2));
		return res;
	}
}

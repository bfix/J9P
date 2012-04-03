
//******************************************************************
//*   PGMID.        BASE64 ENCODER/DECODER.                        *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/09.                                      *
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

package j9p.util;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Convert between Base64 and binary encodings.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Base64 {

	//=================================================================
	/**
	 * <p>Base64 character set.</p>
	 */
	private static final String charset =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
		"abcdefghijklmnopqrstuvwxyz" +
		"0123456789+/=";
	
	//=================================================================
	/**
	 * <p>Encode array of bytes as Bas64 string.</p>
	 * @param data byte[] - data to be converted
	 * @return String - Base64 representation of data
	 */
	public static String fromArray (byte[] data) {
		
		// allocate result buffer
		StringBuffer buf = new StringBuffer();
		int size = data.length;
		// compute padding
		int padding = (3 - (size % 3)) % 3;

		// process all bytes in array...
		for (int n = 0; n < size; n += 3) {
			// check for final (truncated) block
			boolean eof = size < (n + 3);

			// assemble three byte integer value
			int val = (data[n] & 0xFF);
			val = (val << 8) | (eof && padding == 2 ? 0 : (data[n+1] & 0xFF));
			val = (val << 8) | (eof && padding >  0 ? 0 : (data[n+2] & 0xFF));

			// convert to four-character encoding.
			buf.append (charset.charAt ((val >> 18) & 0x3F));
			buf.append (charset.charAt ((val >> 12) & 0x3F));
			buf.append (eof && padding == 2 ? "=" : charset.charAt ((val >> 6) & 0x3F));
			buf.append (eof && padding >  0 ? "=" : charset.charAt ( val       & 0x3F));
		}
		// return representation
		return buf.toString();
	}
	
	//=================================================================
	/**
	 * <p>Decode Base64 string into a byte array.</p>
	 * @param s String - Base64 representation of data
	 * @return byte[] - data
	 */
	public static byte[] toArray (String s) {
		
		// remove all non-Base64 characters from string.
		int strSize = s.length();
		for (int n = 0; n < strSize; n++) {
			char ch = s.charAt (n);
			if (charset.indexOf (ch) == -1) {
				s = s.substring (0, n) + s.substring (n+1);
				strSize--;
				n--;
			}
		}
		// check length of input string.
		if (strSize == 0 || (strSize % 4) != 0)
			return null;

		// compute padding bytes
		int padding = 0;
		if (s.charAt(strSize-1) == '=') padding++;
		if (s.charAt(strSize-2) == '=') padding++;
		
		// compute size of resulting byte array
		int size = 3 * strSize / 4 - padding;
		byte[] res = new byte [size];
		int pos = 0;
		
		// process Base64 string
		for (int n = 0; n < strSize; n += 4) {
			// reconstruct three-byte integer
			int val = charset.indexOf (s.charAt(n));
			val = (val << 6) |  charset.indexOf (s.charAt(n+1));
			val = (val << 6) | (charset.indexOf (s.charAt(n+2)) & 0x3F);
			val = (val << 6) | (charset.indexOf (s.charAt(n+3)) & 0x3F);
			// copy to array
			res[pos++] = (byte) ((val >> 16) & 0xFF);
			if (pos < size) res[pos++] = (byte) ((val >> 8) & 0xFF);
			if (pos < size) res[pos++] = (byte) ( val       & 0xFF);
		}
		// return byte array
		return res;
	}
}

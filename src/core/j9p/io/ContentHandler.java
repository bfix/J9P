
//******************************************************************
//*   PGMID.        STYX CONTENT HANDLER (KEYRING FILE/AUTH MSG).  *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/10.                                      *
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


package j9p.io;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Vector;
import j9p.Channel;
import j9p.util.Base64;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A ContentHandler manages 9P messages as well as keyring file data.</p>
 * <p>Message content and keyring file data is structured as a list of
 * so-called sections. A section starts with a four byte character field,
 * that specifies the length of the section (not counting the length field
 * itself). The section data itself is a list of '\n' separated strings.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ContentHandler {
	
	//=================================================================
	/**
	 * <p>Read sections from binary data.</p>
	 * @param b Blob - binary data
	 * @return Vector<String[]> - list of sections
 	 * @throws IOException - communication failure
	 */
	public static Vector<String[]> getSections (Blob b) throws IOException {

		int total = b.size();
		int pos = 0;
		
		// parse sections
		Vector<String[]> sections = new Vector<String[]>();
		while (pos < total) {
			// read section size.
			String num = b.getDirectString (pos, 4);
			// check for error message
			boolean isErr = (num.charAt(0) == '!');
			if (isErr)
				num = num.substring(1);
			
			// get size of content
			int sectionSize = 0;
			try {
				sectionSize = Integer.parseInt (num);
			}
			catch (NumberFormatException e) {
				return null;
			}

			// read section
			pos += 5;
			String content = b.getDirectString (pos, sectionSize);
			pos += sectionSize;
		
			// special handling of error messages
			if (isErr)
				content = "*ERROR*\n" + content;
		
			// split into entries
			sections.add (content.split ("\n"));
		}
		return sections;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read next section from input stream.</p>
	 * @param ch Channel - communication channel (stream)
	 * @return String[] - list of section entries
 	 * @throws IOException - communication failure
	 */
	public static String[] getNextSection (Channel ch) throws IOException {
		return getNextSection (null, ch);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read next section from input stream.</p>
	 * @param is InputStream - input stream
	 * @return String[] - list of section entries
 	 * @throws IOException - communication failure
	 */
	public static String[] getNextSection (InputStream is) throws IOException {
		return getNextSection (is, null);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read next section from one of the inputs.</p>
	 * @param is InputStream - input stream
	 * @param ch Channel - communication channel (stream)
	 * @return String[] - list of section entries
 	 * @throws IOException - communication failure
	 */
	private static String[] getNextSection (InputStream is, Channel ch) throws IOException {

		// get size field
		int sectionSize = 0;
		byte[] buffer = new byte[5];
		int numRead = (is != null ? is.read (buffer) : ch.readBytes (buffer));
		if (numRead != 5)
			return null;
		
		// check for error message
		boolean isErr = (buffer[0] == '!');
		if (isErr) buffer[0] = '0';
			
		// get size of content
		try {
			sectionSize = Integer.parseInt (new String (buffer, 0, 4));
		}
		catch (NumberFormatException e) {
			return null;
		}
		
		// read section
		buffer = new byte [sectionSize];
		numRead = (is != null ? is.read (buffer) : ch.readBytes (buffer));
		if (numRead != sectionSize)
			return null;
		String content = new String (buffer);
		
		// special handling of error messages
		if (isErr)
			content = "*ERROR*\n" + content;
		
		// split into entries
		return content.split ("\n");
	}

	//=================================================================
	/**
	 * <p>Assemble new section from list of content strings.</p> 
	 * @param content String[] - list of content strings
	 * @return String - assemble section
	 * @throws IOException - communication failure
	 */
	public static String createSection (String[] content) {
		
		StringBuffer buf = new StringBuffer();
		int num = content.length;
		for (int n = 0; n < num; n++) {
			buf.append (content[n]);
			if (num > 1)
				buf.append ('\n');
		}
		
		int size = buf.length();
		String sizeStr = "000" + size + "\n";
		sizeStr = sizeStr.substring (sizeStr.length() - 5);
		buf.insert (0, sizeStr);
		
		return buf.toString();
	}
	
	//=================================================================
	/**
	 * <p>Assemble new section from single string.</p> 
	 * @param line String - single line section
	 * @return String - assemble section
	 */
	public static String createSection (String line) {
		int size = line.length();
		String sizeStr = "000" + size + "\n";
		sizeStr = sizeStr.substring (sizeStr.length() - 5);
		return sizeStr + line;
	}
	
	//=================================================================
	/**
	 * <p>Create list of content strings from binary data.</p> 
	 * @param data byte[] - binary data
	 * @return String[] - list of content strings
	 */
	public static String[] fromBinary (byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream (data);
		try {
			return getNextSection (bis);
		}
		catch (IOException e) {
			return null;
		}
	}
	
	//=================================================================
	/**
	 * <p>Create an error section (size field starts with "!").</p>
	 * @param err String - error message
	 * @return String - assembled error section
	 */
	public static String createErrorSection (String err) {
		StringBuffer buf = new StringBuffer();
		int length = err.length();
		String size = "000" + length;
		buf.append ('!');
		buf.append (size.substring (size.length() - 3));
		buf.append ('\n');
		buf.append (err);
		return buf.toString();
	}

	//=================================================================
	//	Base64-conversion methods for BigIntegers
	//=================================================================
	
	/**
	 * <p>Get a BigInteger from Base64-encoded representation.</p>
	 * @param s String - value in Base64 encoding
	 * @return BigInteger - decoded value
	 */
	public static BigInteger fromBase64 (String s) {
		return new BigInteger (1, Base64.toArray (s));
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get a Base64-encoded representation for a BigInteger value.</p>
	 * @param n BigInteger - value
	 * @return String - value in Base64 encoding
	 */
	public static String toBase64 (BigInteger n) {
		return Base64.fromArray (n.toByteArray());
	}
	
	//=================================================================
	/**
	 * <p>Convert string to (positive) integer.</p> 
	 * @param s String - number representation
	 * @return int - parsed value (or -1 of failure)
	 */
	public static int getPosNumber (String s) {
		try {
			int v = Integer.parseInt (s);
			return (v < 0 ? -1 : v);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}
}

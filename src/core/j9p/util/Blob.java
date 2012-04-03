
//******************************************************************
//*    PGMID.        BINARY (LARGE) OBJECT [BLOB].                 *
//*    AUTHOR.       BERND R. FIX   >Y<                            *
//*    DATE WRITTEN. 09/01/02.                                     *
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

import java.io.ByteArrayOutputStream;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A binary large object of fixed size with relative and absolute
 * getter and setter methods for intrinsic types. Read and write
 * operations behave gracefully (no exceptions are thrown), even if
 * the read/write positions are out of bounds.</p>
 * 
 * @author  Bernd R. Fix   >Y<
 * @version 1.0
 */

public class Blob {
	
	//=================================================================
	/*
	 * Constants for byte-ordering of intrinsic types.</p> 
	 */
	public static final int LITTLE_ENDIAN	= 1;
	public static final int BIG_ENDIAN		= 2;
	
	//=================================================================
	/*
	 * Attributes: 
	 */
	private byte[] data = null;
	private int pos = 0;
	private ByteArrayOutputStream os = null;
	private int order = LITTLE_ENDIAN;
	
	//=================================================================
	//	Constructors
	//=================================================================
	/**
	 * <p>Allocate a new Blob with given size. If the passed reference
	 * to an initialization buffer is not null, the new Blob will be
	 * initialized with the data of the buffer. The size of the
	 * initialization buffer can differ from the Blob size.</p>
	 * @param buffer byte[] - initialization buffer (or null)
	 * @param length int - size of blob
	 */
	public Blob (byte[] buffer, int length) {
		data = new byte [length];
		pos = 0;
		os = null;
		if (buffer != null) {
			int cpSize = Math.min (length, buffer.length);
			System.arraycopy (buffer, 0, data, 0, cpSize);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Allocate a new Blob and initialize with buffer.</p>
	 * @param buffer byte[] - initialization buffer (or null)
	 */
	public Blob (byte[] buffer) {
		// wrap around existing buffer
		data = buffer;
		pos = 0;
		os = (buffer == null ? new ByteArrayOutputStream() : null);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Allocate a dynamic Blob that grows with write operations.
	 * No read operations are allowed until the Blob is finalized
	 * by a "close()" call.</p> 
	 */
	public Blob () {
		this ((byte[])null);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate a copy of a blob.</p>
	 * @param b Blob - initializing blob
	 */
	public Blob (Blob b) {
		data = b.asByteArray (true);
		pos = 0;
		os = null;
	}
	//=================================================================
	/**
	 * <p>Set the byte-ordering for intrinsic types. The default
	 * is LITTLE_ENDIAN.</p> 
	 * @param mode int - LITTLE_ENDIAN or BIG_ENDIAN
	 * @return int - previous byte order
	 */
	public int setOrder (int mode) {
		int last = order;
		order = mode;
		return last;
	}
	//=================================================================
	/**
	 * <p>Close dynamic Blob after write operations. This method
	 * is implicitly called on read access to the Blob.</p> 
	 * @return boolean - successful operation?
	 */
	public boolean close () {
		// check for correct state
		if (data != null || os == null)
			return false;
		// finalize buffer
		data = os.toByteArray();
		os = null;
		pos = 0;
		return true;
	}
	//=================================================================
	/**
	 * <p>Generate printable representation of Blob.</p> 
	 * @return String - printable blob
	 */
	public String toString () {
		if (data == null)
			close();
		return Hex.fromArray (data, ':');
	}
	//=================================================================
	/**
	 * <p>Return blob content as binary data.</p>
	 * @param clone boolean - return cloned data 
	 * @return byte[] - blob content
	 */
	public byte[] asByteArray (boolean clone) {
		if (data == null)
			close();
		if (clone) {
			int size = data.length;
			byte[] res = new byte [size];
			System.arraycopy (data, 0, res, 0, size);
			return res;
		}
		return data;
	}
	//=================================================================
	/**
	 * <p>Return size of blob.</p>
	 * @return int - size of blob
	 */
	public int size () {
		if (data == null)
			close();
		return data.length;
	}
	//=================================================================
	/**
	 * <p>Rewind position for stream read.</p>
	 */
	public void rewind () {
		if (data == null)
			close();
		pos = 0;
	}
	
	//=================================================================
	//	Getter methods (relative, stream read)
	//=================================================================
	/**
	 * <p>Get next byte from blob (relative).</p>
	 * @return int - next byte from blob (-1 at end)
	 */
	public int getByte () {
		if (data == null)
			close();
		if (pos < data.length)
			return (data[pos++] & 0xFF);
		return -1;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next short from blob (relative).</p>
	 * @return int - next short from blob (-1 at end)
	 */
	public int getShort () {
		int v1 = getByte();
		int v2 = getByte();
		if (v1 < 0 || v2 < 0)
			return -1;
		return (order == LITTLE_ENDIAN ? (v2 << 8) | v1 : (v1 << 8) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next int from blob (relative).</p>
	 * @return int - next int from blob
	 */
	public int getInt () {
		int v1 = getShort();
		int v2 = getShort();
		return (order == LITTLE_ENDIAN ? (v2 << 16) | v1 : (v1 << 16) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next long from blob (relative).</p>
	 * @return long - next long from blob
	 */
	public long getLong () {
		long v1 = getInt();
		long v2 = getInt();
		return (order == LITTLE_ENDIAN ? (v2 << 32) | v1 : (v1 << 32) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next long from blob encoded as radix-10 string (relative).</p>
	 * @param size int - number of representing chars
	 * @return long - converted value
	 */
	public long getLongFromString (int size) {
		String s = getDirectString (size);
		try {
			return Long.parseLong (s);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string from blob ('\0' terminated ASCII) with
	 * maximum length.</p>
	 * @param num int - maximum length of string (incl. terminator)
	 * @return String - string from blob
	 */
	public String getString (int num) {
		StringBuffer buf = new StringBuffer();
		boolean append = true;
		for (int n = 0; n < num; n++) {
			int ch = getByte();
			if (ch != 0) {
				if (append)			
					buf.append ((char)ch);
			} else
				append = false;
		}
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string from blob ('\0' terminated ASCII).</p>
	 * @return String - string from blob
	 */
	public String getDelimString () {
		StringBuffer buf = new StringBuffer();
		int ch;
		while ((ch = getByte()) != 0)
			buf.append ((char)ch);
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string from blob (trailing length value).</p>
	 * @return String - string from blob
	 */
	public String getLenString () {
		int length = getShort();
		return getDirectString (length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get a string of given length from blob. No terminator
	 * or length code is stored in the blob.</p>
	 * @param length int - length of string
	 * @return String - string from blob
	 */
	public String getDirectString (int length) {
		StringBuffer buf = new StringBuffer();
		for (int n = 0; n < length; n++) {
			int ch = getByte();
			buf.append ((char)ch);
		}
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get byte array from blob (relative).</p>
	 * @param size int - size of extracted data
	 * @return  byte[] - extracted data
	 */
	public byte[] getArray (int size) {
		if (data == null)
			close();
		int count = Math.min (size, data.length - pos);
		if (count < 1)
			return null;
		byte[] res = new byte [count];
		System.arraycopy (data, pos, res, 0, count);
		return res;
	}
	
	//=================================================================
	//	Getter methods (absolute, indexed read)
	//=================================================================
	/**
	 * <p>Get byte from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @return int - byte value at offset
	 */
	public int getByte (int ofs) {
		if (data == null)
			close();
		if (ofs < 0 || ofs > data.length-1)
			return -1;
		return (data[ofs] & 0xFF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get short from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @return int - short value at offset
	 */
	public int getShort (int ofs) {
		if (data == null)
			close();
		if (ofs < 0 || ofs > data.length-2)
			return -1;
		int v1 = getByte(ofs);
		int v2 = getByte(ofs+1);
		return (order == LITTLE_ENDIAN ? (v2 << 8) | v1 : (v1 << 8) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get integer from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @return int - value at offset
	 */
	public int getInt (int ofs) {
		if (data == null)
			close();
		if (ofs < 0 || ofs > data.length-4)
			return 0;
		int v1 = getShort(ofs);
		int v2 = getShort(ofs+2);
		return (order == LITTLE_ENDIAN ? (v2 << 16) | v1 : (v1 << 16) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get long from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @return long - value at offset
	 */
	public long getLong (int ofs) {
		if (data == null)
			close();
		if (ofs < 0 || ofs > data.length-8)
			return 0;
		long v1 = getInt(ofs);
		long v2 = getInt(ofs+4);
		return (order == LITTLE_ENDIAN ? (v2 << 16) | v1 : (v1 << 16) | v2);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get next long from blob encoded as radix-10 string (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param size int - number of representing chars
	 * @return long - converted value
	 */
	public long getLongFromString (int ofs, int size) {
		String s = getDirectString (ofs, size);
		try {
			return Long.parseLong (s);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param num int - maximum size (incl. terminator)
	 * @return String - string from blob
	 */
	public String getString (int ofs, int num) {
		if (data == null)
			close();
		StringBuffer buf = new StringBuffer();
		for (int n = 0; n < num; n++) {
			int ch = getByte (ofs+n);
			if (ch > 0)
				buf.append ((char)ch);
		}
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string from blob ('\0' terminated ASCII, indexed).</p>
	 * @param ofs int - offset into blob
	 * @return String - string from blob
	 */
	public String getDelimString (int ofs) {
		StringBuffer buf = new StringBuffer();
		int ch;
		while ((ch = getByte(ofs++)) != 0)
			buf.append ((char)ch);
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string with trailing length from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @return String - string from blob
	 */
	public String getLenString (int ofs) {
		if (data == null)
			close();
		int length = getShort (ofs);
		return getDirectString (ofs+2, length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get string of given length from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param length int - length of string
	 * @return String - string from blob
	 */
	public String getDirectString (int ofs, int length) {
		if (data == null)
			close();
		StringBuffer buf = new StringBuffer();
		for (int n = 0; n < length; n++) {
			int ch = getByte (ofs+n);
			buf.append ((char)ch);
		}
		return buf.toString();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get byte array from blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param size int - size of extracted data
	 * @return  byte[] - extracted data
	 */
	public byte[] getArray (int ofs, int size) {
		if (data == null)
			close();
		int count = Math.min (size, data.length - ofs);
		if (count < 1)
			return null;
		byte[] res = new byte [count];
		System.arraycopy (data, ofs, res, 0, count);
		return res;
	}

	//=================================================================
	// Setter methods (relative, stream write)
	//=================================================================
	/**
	 * <p>Add byte to blob (relative).</p>
	 * @param val int - byte value to be added
	 */
	public void putByte (int val) {
		if (data == null)
			os.write (val & 0xFF);
		else {
			if (pos < data.length)
				data[pos++] = (byte) (val & 0xFF);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add short to blob (relative).</p>
	 * @param val int - short value to be added
	 */
	public void putShort (int val) {
		if (order == LITTLE_ENDIAN) {
			putByte ( val       & 0xFF);
			putByte ((val >> 8) & 0xFF);
		} else {
			putByte ((val >> 8) & 0xFF);
			putByte ( val       & 0xFF);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add integer to blob (relative).</p>
	 * @param val int - value to be added
	 */
	public void putInt (int val) {
		if (order == LITTLE_ENDIAN) {
			putShort ( val        & 0xFFFF);
			putShort ((val >> 16) & 0xFFFF);
		} else {
			putShort ((val >> 16) & 0xFFFF);
			putShort ( val        & 0xFFFF);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add long to blob (relative).</p>
	 * @param val long - value to be added
	 */
	public void putLong (long val) {
		if (order == LITTLE_ENDIAN) {
			putInt ((int)( val        & 0xFFFFFFFF));
			putInt ((int)((val >> 32) & 0xFFFFFFFF));
		} else {
			putInt ((int)((val >> 32) & 0xFFFFFFFF));
			putInt ((int)( val        & 0xFFFFFFFF));
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add string to blob into fixed size field (relative).</p>
	 * @param msg String - string to be added
	 * @param size int - size of blob field
	 */
	public void putString (String msg, int size) {
		int length = Math.min (msg.length(), size-1);
		for (int n = 0; n < length; n++) {
			char ch = msg.charAt(n);
			putByte (ch);
		}
		for (int n = length; n < size; n++)
			putByte (0);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add delimited string to blob. (relative).</p>
	 * @param msg String - string to be added
	 */
	public void putDelimString (String msg) {
		putDirectString (msg);
		// add terminator.
		putByte (0);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add string with trailing length to blob (relative).</p>
	 * @param msg String - string to be added
	 */
	public void putLenString (String msg) {
		int length = msg.length();
		putShort (length);
		putDirectString (msg);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add string of given length to blob (relative).</p>
	 * @param msg String - string to be added
	 */
	public void putDirectString (String msg) {
		int length = msg.length();
		for (int n = 0; n < length; n++) {
			char ch = msg.charAt(n);
			putByte (ch);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add a section of a byte array to blob (relative).</p>
	 * @param arr byte[] - binary data
	 * @param start int - offset into data
	 * @param length int - length of section
	 */
	public void putArray (byte[] arr, int start, int length) {
		for (int n = 0; n < length; n++)
			putByte (arr[start+n]);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add a byte array to blob (relative).</p>
	 * @param arr byte[] - binary data
	 */
	public void putArray (byte[] arr) {
		putArray (arr, 0, arr.length);
	}

	//=================================================================
	// Setter methods (absolute, indexed)
	//=================================================================
	/**
	 * <p>Set byte in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param val int - byte value
	 */
	public void putByte (int ofs, int val) {
		if (data == null || ofs < 0 || ofs > data.length-1)
			return;
		data[ofs] = (byte) (val & 0xFF);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set short in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param val int - short value
	 */
	public void putShort (int ofs, int val) {
		if (data == null || ofs < 0 || ofs > data.length-2)
			return;
		if (order == LITTLE_ENDIAN) {
			putByte (ofs  , val       & 0xFF);
			putByte (ofs+1,(val >> 8) & 0xFF);
		} else {
			putByte (ofs  ,(val >> 8) & 0xFF);
			putByte (ofs+1, val       & 0xFF);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set integer in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param val int - value
	 */
	public void putInt (int ofs, int val) {
		if (data == null || ofs < 0 || ofs > data.length-4)
			return;
		if (order == LITTLE_ENDIAN) {
			putShort (ofs  , val        & 0xFFFF);
			putShort (ofs+2,(val >> 16) & 0xFFFF);
		} else {
			putShort (ofs  ,(val >> 16) & 0xFFFF);
			putShort (ofs+2, val        & 0xFFFF);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set long in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param val long - value
	 */
	public void putLong (int ofs, long val) {
		if (data == null || ofs < 0 || ofs > data.length-8)
			return;
		if (order == LITTLE_ENDIAN) {
			putInt (ofs  ,(int)( val        & 0xFFFFFFFF));
			putInt (ofs+4,(int)((val >> 32) & 0xFFFFFFFF));
		} else {
			putInt (ofs  ,(int)((val >> 32) & 0xFFFFFFFF));
			putInt (ofs+4,(int)( val        & 0xFFFFFFFF));
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set string into fixed-size field in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param msg String - string to be stored
	 * @param size int - size of blob field (-1 of length of string)
	 */
	public void putString (int ofs, String msg, int size) {
		if (size == -1)
			size = msg.length();
		if (data == null || ofs + size > data.length-1)
			return;
		int length = Math.min (msg.length(), size-1);
		for (int n = 0; n < length; n++) {
			char ch = msg.charAt(n);
			putByte (ofs+n, ch);
		}
		for (int n = length; n < size; n++)
			putByte (ofs+n, 0);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Add delimited string to blob. (absolute).</p>
	 * @param ofs int - offset into blob
	 * @param msg String - string to be added
	 */
	public void putDelimString (int ofs, String msg) {
		int length = msg.length();
		if (data == null || ofs + length > data.length-1)
			return;
		putDirectString (ofs, msg);
		// add terminator.
		putByte (ofs+length, 0);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set string with trailing length in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param msg String - string to be stored
	 */
	public void putLenString (int ofs, String msg) {
		int length = msg.length();
		if (data == null || ofs + length + 2 > data.length-1)
			return;
		putShort (ofs, length);
		putDirectString (ofs+2, msg);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set string of given length in blob (indexed).</p>
	 * @param ofs int - offset into blob
	 * @param msg String - string to be stored
	 */
	public void putDirectString (int ofs, String msg) {
		int length = msg.length();
		if (data == null || ofs + length > data.length-1)
			return;
		for (int n = 0; n < length; n++) {
			char ch = msg.charAt(n);
			putByte (ofs+n, ch);
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Copy section of a byte array into blob (indexed).</p> 
	 * @param ofs int - offset into blob
	 * @param arr byte[] - data
	 * @param start int - start of section (offset into data)
	 * @param length int - size of section
	 */
	public void putArray (int ofs, byte[] arr, int start, int length) {
		if (data == null)
			return;
		for (int n = 0; n < length; n++)
			putByte (ofs+n, arr[start+n]);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Copy a byte array into blob (indexed).</p> 
	 * @param ofs int - offset into blob
	 * @param arr byte[] - data
	 */
	public void putArray (int ofs, byte[] arr) {
		if (data == null)
			return;
		putArray (ofs, arr, 0, arr.length);
	}
}

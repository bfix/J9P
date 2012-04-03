
//******************************************************************
//*   PGMID.        LOGICAL NAMESPACE FILE.                        *
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

package j9p.ns.handlers;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import j9p.util.Base64;
import j9p.ns.File;
import j9p.ns.Permissions;
import j9p.ns.handlers.Process;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>LogicalFile</b> is a namespace entry that only exists
 * during a server session and is not persistent across server
 * invocations.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class LogicalFile extends File {
	
	//=================================================================
	/**
	 * <p>Maximum size of file (in memory).</p>
	 */
	protected static final int MAX_SIZE = 4096;
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected byte[]	content;		// file content (limited size)
	protected int		size;			// current file size 

	//=================================================================
	/**
	 * <p>Instantiate a new logical file in the namespace.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public LogicalFile (String name, Permissions perm, String device) {
		super (name, perm, device);
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	public LogicalFile () {
	}
	
	//=================================================================
	/**
	 * <p>Start a (handler) process running in its own thread.
	 * Configuration parameters are passed in as a list
	 * of named parameters with string values.</p> 
	 * @param p Hashtable<String,String> - parameter set
	 * @return Process - started thread (or null)
	 */
	public Process startHandler (Hashtable<String,String> p) {

		// get maximum size of logical file.
		String sizeStr = p.get ("size");
		int size = 4096;
		if (sizeStr != null) {
			try {
				size = Integer.parseInt (sizeStr);
			}
			catch (NumberFormatException e) {
			}
		}
		content = new byte [size];
		size = 0;
		return null;
	}
		
	//=================================================================
	/**
	 * <p>Initialize file (in memory, transient to session).</p>
	 * @param data byte[] - initializing data (or null)
	 */
	public void setContent (byte[] data) {
		// initialize attributes
		if (data != null) {
			int count = Math.min (data.length, content.length);
			System.arraycopy (data, 0, content, 0, count);
			size = count;
		} else
			// file is empty
			size = 0;
	}
	
	//=================================================================
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset long - offset into entry content
	 * @param count int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int count, AttributeHandler fmt) {
		
		// check bounds.
		if (offset < 0 || offset > size-1)
			return null;
		
		// assemble result.
		int num = (int) Math.min (size-offset, count);
		byte[] res = new byte [num];
		System.arraycopy (content, (int)offset, res, 0, num);
		return res;
	}
	
	//=================================================================
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset long - offset into entry content
	 * @param count int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public int write (Handle hdl, byte[] data, long offset, int count) {
		
		// check bounds.
		if (offset < 0 || offset > MAX_SIZE-1)
			return 0;
		
		// write data
		int num = (int) Math.min (MAX_SIZE-offset, count);
		System.arraycopy (data, 0, content, (int)offset, num);
		// we have a new size
		size = (int)offset + num;
		return num;
	}
	
	//=================================================================
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public long getSize () {
		// return our size
		return size;
	}
	
	//=================================================================
	/**
	 * <p>Helper application: Convert file to namespace definition.</p> 
	 * @param argv String[] - list of files to convert
	 */
	public static void main (String[] argv) {
		
		// process all files...
		int count = argv.length;
		for (int n = 0; n < count; n++) {
			// get file name
			String fname = argv[n];
			
			// get file content.
			byte[] content = null;
			try {
				InputStream is = new FileInputStream (fname);
				int size = is.available();
				content = new byte [size];
				is.read (content);				
			} catch (Exception e) {
				e.printStackTrace();
				content = null;
			}
			
			// assemble config entry
			StringBuffer buf = new StringBuffer();
			buf.append ("<File type=\"logFile\"  name=\"");
			buf.append (fname);
			buf.append ("\" mode=\"644\">\n<Content size=\"4096\">\n");
			if (content != null) {
				String cStr = Base64.fromArray(content);
				while (cStr.length() > 80) {
					buf.append (cStr.substring (0, 80));
					buf.append ('\n');
					cStr = cStr.substring(81);
				}
				buf.append(cStr);
				buf.append ('\n');
			}
			buf.append ("</Content>\n</File>");
			System.out.println (buf.toString());
		}
	}
}

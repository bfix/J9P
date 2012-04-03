
//******************************************************************
//*   PGMID.        NAMESPACE ENTRY AS FILE ON DISK.               *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/07.                                      *
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

import j9p.auth.Credential;
import j9p.ns.File;
import j9p.ns.Permissions;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;


///////////////////////////////////////////////////////////////////////////////
/**
* <p>A <b>DiskFile</b> is a namespace entry that is mapped to a file in the
* local filesystem of the server. Access is controlled by the credentials
* and permissions defined in the namespace, not by the permissions of the
* actual disk file. The file should be readable (and possibly writable)
* by the server process; it can't be deleted. </p>
* 
* @author Bernd R. Fix   >Y<
* @version 1.0
*/
public class DiskFile extends File {
	
	//=================================================================
	/*
	 * <p>Disk file handle.</p>
	 */
	private static class DiskHandle extends Handle {

		//-------------------------------------------------------------
		/*
		 * Attributes:
		 */
		RandomAccessFile file = null;			// opened file
		//-------------------------------------------------------------
		/**
		 * <p>Release handle (close file).</p>
		 */
		public void release () {
			// close disk file.
			try {
				if (file != null)
					file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//=================================================================
	/*
	 * Attributes:
	 */
	protected String fileName = null;		// name of disk file
	protected long fileSize = 0;			// size of file

	//=================================================================
	//	Constructors.
	//=================================================================
	/**
	 * <p>Instantiate a new disk file in the namespace.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public DiskFile (String name, Permissions perm, String device) {
		super (name, perm, device);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Constructor for uninitialized entry.</p>
	 */
	public DiskFile () {
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
		fileName = p.get ("ref");
		fileSize = new java.io.File(fileName).length();
		return null;
	}
	
	//=================================================================
	//	abstract methods for read, write and remove
	//=================================================================

	//-----------------------------------------------------------------
	/**
	 * <p>Open file for user with credential for access.</p>
	 * @param cr Credential - authenticated user credential
	 * @param mode int - access mode
	 * @return Handle - handle to opened file
	 */
	public Handle open (Credential cr, int mode) {
		
		// try to open entry.
		Handle hdl = super.open (cr, mode);
		if (hdl.rc != Handle.RC_OK)
			// open failed on basic level
			return hdl;

		// determine access mode
		String fMode = "r";
		if (hdl.forWrite) fMode = "rw";
		
		// check access to open physical file
		java.io.File f = new java.io.File (fileName);
		if ((hdl.forRead && !f.canRead()) || (hdl.forWrite && !f.canWrite())) {
			// overwrite permission if the server
			// can't access the file. 
			inUse--;
			hdl.rc = Handle.RC_NO_PERMISSION;
			return hdl;
		}
		
		// create our own handle.
		DiskHandle dHdl = new DiskHandle ();
		dHdl.forRead = hdl.forRead;
		dHdl.forWrite = hdl.forWrite;
		try {
			dHdl.rc = Handle.RC_OK;
			dHdl.file = new RandomAccessFile (f, fMode);
			// truncate file on request
			if ((mode & Permissions.OTRUNC) != 0)
				dHdl.file.setLength (0);
			fileSize = dHdl.file.length();
		}
		catch (Exception e) {
			hdl.rc = Handle.RC_NO_SUCH_FILE;
			inUse--;
			return hdl;
		}
		return dHdl;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int size, AttributeHandler fmt) {
		
		// convert handle to proper type
		if (!(hdl instanceof DiskHandle))
			return null;
		DiskHandle dHdl = (DiskHandle) hdl;
		
		// check file access
		if (!hdl.forRead || dHdl.file == null)
			return null;
		
		// perform read operation
		try {
			// seek to offset position
			dHdl.file.seek (offset);
			dHdl.lastReadOfs = offset;
			
			// read data
			byte[] res = new byte [size];
			int num = dHdl.file.read (res);
			if (num < 0) {
				dHdl.lastReadNum = 0;
				return null;
			}
			dHdl.lastReadNum = num;
			if (num == size)
				return res;
			
			// truncate result array
			byte [] resTrunc = new byte [num];
			System.arraycopy (res, 0, resTrunc, 0, num);
			return resTrunc;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//=================================================================
	/**
	 * <p>Write entry content starting at offset for given number
	 * of bytes.</p>
	 * @param hdl Handle - handle to opened file
	 * @param data byte[] - data to be written
	 * @param offset long - offset into entry content
	 * @param size int - number of bytes to be written
	 * @return int - number of bytes written
	 */
	public int write (Handle hdl, byte[] data, long offset, int size) {
		
		// convert handle to proper type
		if (!(hdl instanceof DiskHandle))
			return 0;
		DiskHandle dHdl = (DiskHandle) hdl;
		
		// check file access
		if (!hdl.forWrite || dHdl.file == null)
			return 0;
		
		// perform write operation
		try {
			// seek to offset position
			dHdl.file.seek (offset);
			dHdl.lastWriteOfs = offset;
			
			// write data
			dHdl.file.write (data, 0, size);
			fileSize = dHdl.file.length();
			dHdl.lastWriteNum = size;
			return size;
		}
		catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	//=================================================================
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public long getSize () {
		// return size of underlying disk file
		return fileSize;
	}
}

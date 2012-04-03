
//******************************************************************
//*   PGMID.        NAMESPACE ENTRY AS PROCESS PIPE.               *
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

import java.util.Hashtable;
import j9p.ns.File;
import j9p.ns.Permissions;
import j9p.ns.handlers.Process;


///////////////////////////////////////////////////////////////////////////////
/**
* <p>A <b>DiskFile</b> is a namespace entry that is mapped to a file in the
* local filesystem of the server.</p>
* 
* @author Bernd R. Fix   >Y<
* @version 1.0
*/
public class ProcessFile extends File {

	//=================================================================
	/**
	 * <p>Inner class for process file listener implementations.</p>
	 * <p>Implementations listen to file interactions (read/write)
	 * and react accordingly. They receive the binary input (from
	 * a write operation) and return a binary result for a subsequent
	 * read operation. If the client "writes" the file twice without
	 * reading it again, the previous response is lost.</p>
	 */
	public static interface Listener {
		
		//-------------------------------------------------------------
		/**
		 * <p>Data is written to the process.</p>
		 * @param data byte[] - incoming data from a write operation
		 * @param offset long - offset into data
		 */
		void asInput (byte[] data, long offset);
		
		//-------------------------------------------------------------
		/**
		 * <p>Data is read from the process.</p>
		 * @param offset long - offset into output (index)
		 * @param count int - number of expected bytes
		 */
		byte[] getOutput (long offset, int count);
	}
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected Listener listener = null;			// process listener
	protected Hashtable<String,String> params;	// parameter set
	
	//=================================================================
	/**
	 * <p>Instantiate a new process pipe in the namespace.</p>
	 * @param name String - name of entry
	 * @param perm Permissions - access permissions for file
	 * @param device String - mount handler
	 */
	public ProcessFile (String name, Permissions perm, String device) {
		super (name, perm, device);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Protected constructor for uninitialized entry.</p>
	 */
	public ProcessFile () {
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
		// save parameters
		params = p;

		// instantiate a new handler
		String type = p.get ("class");
		if (type != null) {
			// instantiate a new engine
			try {
				Class<?> cl = Class.forName (type);
				Object obj = cl.newInstance();
				
				if (obj instanceof ProcessAdaptor) {
					ProcessAdaptor engine = (ProcessAdaptor) obj;
					
					// pass parameters to engine and start it
					engine.init (this, params);
					engine.start();
					// set engine as listener.
					this.listener = engine;
					return engine;
				}
				else if (obj instanceof Listener) {
					listener = (Listener) obj;
					return null;
				}
			} catch (Exception e) {
				System.err.println (e.getMessage());
			} 
		}
		return null;
	}
		
	//=================================================================
	//	methods for read, write and size
	//=================================================================
	/**
	 * <p>Read entry content starting at offset for given number
	 * of bytes. Returns pending output from listener.</p>
	 * @param hdl Handle - handle to opened file
	 * @param offset long - offset into entry content
	 * @param count int - number of bytes to be read
	 * @param fmt Formatter - protocol-specific entry representation
	 * @return byte[] - read content
	 */
	public byte[] read (Handle hdl, long offset, int count, AttributeHandler fmt) {
		
		// return requested data
		if (listener != null)
			return listener.getOutput (offset, count);
		return null;
	}
	//-----------------------------------------------------------------
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
		
		// prepare data...
		if (data.length > count) {
			byte[] res = new byte [count];
			System.arraycopy (data, 0, res, 0, count);
			data = res;
		}
		// ...and pass it to listener.
		if (listener != null) {
			listener.asInput (data, offset);
			return count;
		}
		// failed to handle
		return 0;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get size of entry.</p>
	 * @return long - entry size
	 */
	public long getSize () {
		// no size by default.
		return 0;
	}
}

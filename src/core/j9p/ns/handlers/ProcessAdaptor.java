
//******************************************************************
//*   PGMID.        LISTENER ADAPTOR FOR PROCESS FILES.            *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/30.                                      *
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


///////////////////////////////////////////////////////////////////////////////
/**
* <p>A <b>ProcessAdaptor</b> is an abstract implementation of the
* Listener interface from the class LogicalFile.</p>
* <p>It provides two methods "read" and "write" to provide
* data exchange with an application in derived classes.</p>
* 
* @author Bernd R. Fix   >Y<
* @version 1.0
*/
public abstract class ProcessAdaptor extends Process implements ProcessFile.Listener {
	
	//=================================================================
	/*
	 * Attributes for input/output buffer handling:
	 */
	private int				bufSize;
	private boolean			alloc;
	private byte[] 			inputBuffer;
	private volatile int    inputPos;
	private byte[]			outputBuffer;
	private volatile int    outputPos;
	private Object 			semaphore;

	//=================================================================
	/**
	 * <p>Instantiate a new ProcessAdaptor with no allocated buffers.
	 * Allocation is deferred until the first write operation on the
	 * associated namespace file is requested.</p> 
	 */
	public ProcessAdaptor () {
		bufSize = 4096;
		alloc = false;
		semaphore = new Object();
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

		// get buffer size
		String sizeStr = p.get ("buf");
		int size = 4096;
		if (sizeStr != null) {
			try {
				size = Integer.parseInt (sizeStr);
			}
			catch (NumberFormatException e) {
			}
		}
		bufSize = size;
		return null;
	}
	
	//=================================================================
	/**
	 * <p>Allocate buffers when needed.</p> 
	 */
	private void alloc () {
		if (!alloc) {
			inputBuffer = new byte [bufSize];
			outputBuffer = new byte [bufSize];
			inputPos = outputPos = 0;
			alloc = true;
		}
	}
	
	//=================================================================
	//	Provided access methods.
	//=================================================================
	/**
	 * <p>Read the next bytes from input. If no bytes
	 * are available, the call can block until input is available.</p> 
	 * @param num int - number of bytes to be read
	 * @param block boolean - allow blocking call?
	 * @return byte[] - read data
	 */
	protected byte[] read (int num, boolean block) {
		alloc();
		// input data available?
		if (inputPos == 0 && block) {
			// no wait for input.
			synchronized (semaphore) {
				try { semaphore.wait(); } catch (InterruptedException e) { }
			}
		}
		synchronized (inputBuffer) {
			int count = Math.min (inputPos, num);
			if (count < 1)
				return null;
			byte[] res = new byte [count];
			System.arraycopy (inputBuffer, 0, res, 0, count);
			System.arraycopy (inputBuffer, count, inputBuffer, 0, bufSize-count);
			inputPos -= count;
			return res;
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Write data to the output queue.</p>
	 * @param data byte[] - data to be written
	 * @return int - number of bytes written
	 */
	protected int write (byte[] data) {
		alloc();
		synchronized (outputBuffer) {
			int count = Math.min (bufSize-outputPos, data.length);
			System.arraycopy (data, 0, outputBuffer, outputPos, count);
			outputPos += count;
			return count;
		}
	}

	//=================================================================
	//	Interface methods:
	//=================================================================
	/**
	 * <p>Data is written to the process.</p>
	 * @param data byte[] - incoming data from a write operation
	 * @param offset long - offset into data
	 */
	public void asInput (byte[] data, long offset) {
		alloc ();
		// we ignore the offset and simply append the data
		// to the input queue.
		synchronized (inputBuffer) {
			int count = Math.min (bufSize-inputPos, data.length);
			System.arraycopy (data, 0, inputBuffer, inputPos, count);
			inputPos += count;
		}
		// notify any waiting reads
		synchronized (semaphore) {
			semaphore.notify();
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Data is read from the process.</p>
	 * @param offset long - offset into output (index)
	 * @param count int - number of expected bytes
	 */
	public byte[] getOutput (long offset, int count) {
		
		// check for deferred allocation.
		if (!alloc)
			// no data available
			return null;
		
		// we ignore the offset and simply return the data
		// from the output queue. If offset is >0,
		// we return no data!
		count = Math.min (outputPos, count);
		if (count == 0 || offset > 0)
			return null;
		synchronized (outputBuffer) {
			byte[] res = new byte [count];
			System.arraycopy (outputBuffer, 0, res, 0, count);
			System.arraycopy (outputBuffer, count, outputBuffer, 0, bufSize-count);
			outputPos -= count;
			return res;
		}
	}
}

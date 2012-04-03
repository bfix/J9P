
//******************************************************************
//*   PGMID.        STYX PROTOCOL MESSAGE (GENERIC).               *
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

package j9p;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p><b>Message</b> is a generic message used in the 9P protocol
 * implementations. Data is represented as a blob (binary data in little
 * endian order) with (relative and absolute) getter and setter methods
 * for intrinsic types.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Message extends Blob {
	
	//=================================================================
	//	Constructors
	//=================================================================
	/**
	 * <p>Instantiate new Message of given length. If the reference
	 * to an initialization buffer is null, no data is preset.</p>
	 * @param buffer byte[] - initialization buffer (or null)
	 * @param length int - total size (in bytes) of message
	 */
	public Message (byte[] buffer, int length) {
		super (buffer, length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Create a Message from a data array.</p> 
	 * @param buffer byte[] - styx message data
	 */
	public Message (byte[] buffer) {
		super (buffer, buffer.length);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Create an empty Message.</p> 
	 */
	protected Message () {
		//super ();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate a copy of a message.</p>
	 * @param msg Message - initializing message 
	 */
	protected Message (Message msg) {
		super (msg);
	}
}

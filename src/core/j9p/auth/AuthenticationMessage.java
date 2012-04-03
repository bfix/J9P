
//******************************************************************
//*   PGMID.        STYX AUTHENTICATION PROTOCOL MESSAGE.          *
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

package j9p.auth;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Message;
import j9p.io.ContentHandler;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A AuthenticationMessage is used in the 9P authentication protocol.
 * It basically is a list of newline-delimited strings and starts with
 * a length field (4 bytes of decimal length) followed by a newline
 * character. The length field is the total size of the message including
 * the length field and the delimiter. If the first character of the length
 * field is a "!", the message is an error message and contains on
 * string (the error message) as data.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AuthenticationMessage extends Message {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	private String[]	content;		// list of strings as content
	private boolean		isValid;		// valid authentication message?
	private boolean		isErr;			// error message?

	//=================================================================
	//	Constructors
	//=================================================================
	/**
	 * <p>Instantiate a new authentication message from given content.</p>
	 */
	protected AuthenticationMessage (String[] content) {
		super();
		this.content = content;
		String msg = ContentHandler.createSection (content);
		putDirectString (msg);
		close();
		isErr = false;
		isValid = true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate an authentication error message.</p> 
	 * @param errMsg String - error message text
	 */
	protected AuthenticationMessage (String errMsg) {
		super();
		content = new String[] { errMsg };
		String msg = ContentHandler.createErrorSection (errMsg);
		putDirectString (msg);
		close();
		isErr = true;
		isValid = true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Copy constructor from a message object. Parse and
	 * check the message for correct format.</p>
	 * @param msg Message - generic message
	 */
	protected AuthenticationMessage (Message msg) {
		super (msg);
		
		// check basic length constraint
		if (size() < 6) {
			isValid = false;
			return;
		}
		// check for 'newline' character
		if (getByte(4) != '\n') {
			isValid = false;
			return;
		}
		// read size of incoming data
		// (check for special error messages)
		int numSize = 4;
		if (getByte(0) == '!') {
			isErr = true;
			numSize = 3;
		}
		String sizeStr = getDirectString (4-numSize, numSize);
		int dataSize = 0;
		try {
			dataSize = Integer.parseInt (sizeStr);
		}
		catch (NumberFormatException e) {
			isValid = false;
			return;
		}
		// validate against length of available data
		if (dataSize + 5 != size()) {
			isValid = false;
			return;
		}
		// split content into 'lines'
		String msgData = getDirectString (5, dataSize);
		content = msgData.split ("\n");
		isValid = true;
	}

	//=================================================================
	//	Getter methods.
	//=================================================================
	/**
	 * <p>Check if message contains valid data.</p>
	 * @return boolean - auth message is valid?
	 */
	public boolean isValid() {
		return isValid;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get content as list of strings.</p>
	 * @return String[] - list of content strings
	 */
	public String[] getContents () {
		return content;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check if message is an error message.</p>
	 * @return boolean - is this an error message?
	 */
	public boolean isErrMsg () {
		return isErr;
	}
}

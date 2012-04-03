
//******************************************************************
//*   PGMID.        PLAN9 PROTOCOL 9P2000 IMPLEMENTATION.          *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/02.                                      *
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

package j9p.proto;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Message;
import j9p.util.Hex;


///////////////////////////////////////////////////////////////////////////////
/**
* <p>Encapsulate constants and methods common for message in the Plan
* protocol version (9P2000).</p>
* 
* @author Bernd R. Fix   >Y<
* @version 1.0
*/
public class V2 {
	
	//=================================================================
	/*
	 * Constants (message types) -- version 2 (9P2000)
	 */
	public static final int Tversion	= 100;
	public static final int Rversion	= 101;
	public static final int Tauth		= 102;
	public static final int Rauth		= 103;
	public static final int Tattach		= 104;
	public static final int Rattach		= 105;
	public static final int Terror		= 106;
	public static final int Rerror		= 107;
	public static final int Tflush		= 108;
	public static final int Rflush		= 109;
	public static final int Twalk		= 110;
	public static final int Rwalk		= 111;
	public static final int Topen		= 112;
	public static final int Ropen		= 113;
	public static final int Tcreate		= 114;
	public static final int Rcreate		= 115;
	public static final int Tread		= 116;
	public static final int Rread		= 117;
	public static final int Twrite		= 118;
	public static final int Rwrite		= 119;
	public static final int Tclunk		= 120;
	public static final int Rclunk		= 121;
	public static final int Tremove		= 122;
	public static final int Rremove		= 123;
	public static final int Tstat		= 124;
	public static final int Rstat		= 125;
	public static final int Twstat		= 126;
	public static final int Rwstat		= 127;

	private static final int MAX_TYPES	= 127;
	/*
	 * Constants (message type names) -- version 2 (Plan 9)
	 */
	public static final String[] typeNames = new String[] {
		"Tversion", "Rversion", "Tauth",   "Rauth",   "Tattach", "Rattach",
		"Terror",   "Rerror",   "Tflush",  "Rflush",  "Twalk",   "Rwalk",
		"Topen",    "Ropen",    "Tcreate", "Rcreate", "Tread",   "Rread",
		"Twrite",   "Rwrite",   "Tclunk",  "Rclunk",  "Tremove", "Rremove",
		"Tstat",    "Rstat",    "Twstat",  "Rwstat"
	};
	
	//=================================================================
	/**
	 * <p>Check if message content is compatible with protocol
	 * version implemented (Version 2: Plan 9).</p> 
	 * @param in Message - message to be checked
	 * @return int - type of message (if compatible, -1 otherwise)
	 */
	public static int isCompatible (Message in) {
		// check for plausible message in protocol version 2 (Plan 9)
		
		// 1.)  check size.
		int length = in.getInt (0);
		if (length != in.size())
			return -1;
		
		// 2.) check type of message
		int type = in.getByte(4);
		if (type < 100 || type > MAX_TYPES)
			// invalid type: not compatible.
			return -1;
		
		// message seems to be compatible
		return type;
	}
	
	//=================================================================
	/**
	 * <p>Create printable representation of this Styx message.</p>
	 * @param in Message - message to be printed
	 * @return String - printable Styx message
	 */
	public static String toString (Message in) {
		if (in == null)
			return "<empty message>";
		StringBuffer buf = new StringBuffer();
		int msgSize = in.size();
		int type = in.getByte(4);
		
		// check for Styx message
		if (type >= 100 && type <= MAX_TYPES) {
			// assemble formatted Styx message
			buf.append ("[type:" + typeNames[type-100] + "]");
			buf.append ("[tag:" + Hex.fromShort ((short)in.getShort(5)) + "]");
			if (msgSize > 7) {
				if (type == Rerror) {
					String err = in.getLenString (7);
					buf.append ("[error:" + err + "]");
				} else {
					byte[] payload = in.getArray (7, msgSize-7);
					buf.append ("[data:" + Hex.fromArray (payload, ':') + "]");
				}
			}
		}
		else {
			// hexadecimal output of message
			buf.append ("{");
			buf.append (Hex.fromArray (in.asByteArray(false), ':'));
			buf.append ("}");
		}
		return buf.toString();
	}
}


//******************************************************************
//*   PGMID.        INFERNO STYX PROTOCOL IMPLEMENTATION.          *
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
* <p>Encapsulate constants and methods common for message in the Inferno/Styx
* protocol version of 9P.</p>
* 
* @author Bernd R. Fix   >Y<
* @version 1.0
*/
public class V1 {

	//=================================================================
	/*
	 * Constants (message types) -- version 1 (inferno)
	 */
	public static final int Tnop		=  0;
	public static final int Rnop		=  1;
	public static final int Terror		=  2;
	public static final int Rerror		=  3;
	public static final int Tflush		=  4;
	public static final int Rflush		=  5;
	public static final int Tclone		=  6;
	public static final int Rclone		=  7;
	public static final int Twalk		=  8;
	public static final int Rwalk		=  9;
	public static final int Topen		= 10;
	public static final int Ropen		= 11;
	public static final int Tcreate		= 12;
	public static final int Rcreate		= 13;
	public static final int Tread		= 14;
	public static final int Rread		= 15;
	public static final int Twrite		= 16;
	public static final int Rwrite		= 17;
	public static final int Tclunk		= 18;
	public static final int Rclunk		= 19;
	public static final int Tremove		= 20;
	public static final int Rremove		= 21;
	public static final int Tstat		= 22;
	public static final int Rstat		= 23;
	public static final int Twstat		= 24;
	public static final int Rwstat		= 25;
	public static final int Tsession	= 26;
	public static final int Rsession	= 27;
	public static final int Tattach		= 28;
	public static final int Rattach		= 29;
	public static final int NUM_TYPES	= 30;
	//-----------------------------------------------------------------
	/*
	 * Constants (message type names) -- version 1 (inferno)
	 */
	public static final String[] typeNames = {
		"Tnop",    "Rnop",    "Terror",   "Rerror",   "Tflush",  "Rflush",
		"Tclone",  "Rclone",  "Twalk",    "Rwalk",    "Topen",   "Ropen",
		"Tcreate", "Rcreate", "Tread",    "Rread",    "Twrite",  "Rwrite",
		"Tclunk",  "Rclunk",  "Tremove",  "Rremove",  "Tstat",   "Rstat",
		"Twstat",  "Rwstat",  "Tsession", "Rsession", "Tattach", "Rattach"
	};
	//-----------------------------------------------------------------
	/*
	 * Constants for length of messages.
	 * (= 0: not supported, < 0: minimum length)
	 */
	private static final int[] msgSizes = {
	 	  3,   3,   0,  67,   5,   3,
		  7,   5,  33,  13,   6,  13,
		 38,  13,  15,  -8, -16,   7,
		  5,   5,   5,   5,   5, 121,
		121,   5,   0,   0,  61,  13
	};
	
	//=================================================================
	/**
	 * <p>Check if message content is compatible with protocol
	 * version implemented (Version 1: Inferno).</p> 
	 * @param in Message - message to be checked
	 * @return int - type of message (if compatible, -1 otherwise)
	 */
	public static int isCompatible (Message in) {
		// check for plausible message in protocol version 1 (Inferno)
		
		// 1.)  check message type.
		int type = in.getByte (0);
		if (type < 0 || type > NUM_TYPES-1)
			// invalid type: not compatible.
			return -1;
		
		// 2.)  get expected message size and compare to actual
		//		size of message
		int len = msgSizes[type];
		if (len == 0)
			return -1;
		if (len > 0 && in.size() != len)
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
		StringBuffer buf = new StringBuffer();
		int msgSize = in.size();
		int type = in.getByte(0);
		
		// check for Styx message
		if (type >= 0 && type < NUM_TYPES) {
			// assemble formatted Styx message
			buf.append ("[type:" + typeNames[type] + "]");
			buf.append ("[tag:" + Hex.fromShort ((short)in.getShort(1)) + "]");
			if (msgSize > 3) {
				if (type == Rerror) {
					String err = in.getString (3, 64);
					buf.append ("[error:" + err + "]");
				} else {
					buf.append ("[fid:" + Hex.fromShort ((short)in.getShort(3)) + "]");
					if (msgSize > 5) {
						byte[] payload = in.getArray (5, msgSize-5);
						buf.append ("[data:" + Hex.fromArray (payload, ':') + "]");
					}
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

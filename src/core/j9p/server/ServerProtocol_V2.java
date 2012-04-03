
//******************************************************************
//*   PGMID.        PLAN9 STYX PROTOCOL IMPLEMENTATION.            *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/09.                                      *
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

package j9p.server;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Message;
import j9p.auth.Credential;
import j9p.ns.Entry;
import j9p.ns.Entry.Attributes;
import j9p.proto.V2;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The <b>ServerProtocol_V2</b> class implements the protocol
 * from Plan9. It can process incoming Tmsg messages and generates
 * outgoing Rmsg messages for the client.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerProtocol_V2 extends ServerProtocol {

	//=================================================================
	/**
	 * <p>Instantiate a new StyxProtocol_V2 handler for Styx messages.</p>
	 * @param nsHdlr NamespaceManager - namespace provider
	 * @param needAuth boolean - authentication required?
	 */
	public ServerProtocol_V2 (NamespaceManager nsHdlr, boolean needAuth) {
		super (nsHdlr, needAuth);
	}

	//=================================================================
	/**
	 * <p>Peek into Message and return the tag of the operation.</p>
	 * @param in Message - message to be inspected
	 * @return int - tag of operation in message
	 */
	public int getTag (Message in) {
		return in.getShort (4);
	}
	
	//=================================================================
	/**
	 * <p>Check if message is a "flush()" operation that
	 * will terminate another pending operation.</p>
	 * @param in Message - message to be inspected
	 * @return int - tag of flushed operation (or 0)
	 */
	public int isFlushing (Message in) {
		if (in.getByte (4) != V2.Tflush)
			return 0;
		return in.getShort (7);
	}
	
	//=================================================================
	/**
	 * <p>Create printable representation of this Styx message.</p>
	 * @param in Message - message to be printed
	 * @return String - printable Styx message
	 */
	public String toString (Message in) {
		return V2.toString (in);
	}
	
	//=================================================================
	/**
	 * <p>Process incoming client request and generate response message.
	 * Comments on message types taken from the book "Inferno Programming
	 * with Limbo" by Phillip Stanley-Marbell.</p>
	 * @param id int - session identifier
	 * @param in Message - incoming client request
	 * @param cr Credential - user credential
	 * @return Message - outgoing server response
	 */
	public Message process (int id, Message in, Credential cr) {
		
		// check message consistency
		int totalSize = in.getInt();
		if (totalSize != in.size())
			return null;

		// read "header" fields of Styx message.
		int type = in.getByte();
		int tag  = in.getShort();
		
		// reply message
		Message reply = null;
		
		switch (type) {
			//---------------------------------------------------------
			// The version request negotiates the protocol version and
	     	// message size to be used on the connection and initializes
	     	// the connection for I/O. Tversion must be the first message
	     	// sent on the Styx connection, and the client cannot issue any
	     	// further requests until it has received the Rversion reply.
	     	// The tag should be NOTAG (value (ushort)~0) for a version
	     	// message.
			//---------------------------------------------------------
			case V2.Tversion: {
				// get arguments
				int msize = in.getInt();
				String version = in.getLenString();

				// check tag value
				if (tag != NOTAG)
					reply = assembleError (tag, "wrong tag value for message");
				else {
					// perform operation.
					Result res = version (msize, version); 
					// return our supported version
					reply = assembleVersion (tag, msize, res.msg);
				}
			} break;
			
			//---------------------------------------------------------
			// The auth message contains afid, a new fid to be established
			// for authentication, and the uname and aname that will be
			// those of the following attach message. If the server does
			// not require authentication, it returns Rerror to the Tauth
			// message.
			// If the server does require authentication, it returns aqid
			// defining a file of type QTAUTH (see intro(5)) that may be
			// read and written (using read and write messages in the usual
			// way) to execute an authentication protocol. That protocol's
			// definition is not part of Styx itself.
			//---------------------------------------------------------
			case V2.Tauth: {
				// get arguments
				int fid = in.getInt();
				String uid = in.getLenString();
				String aname = in.getLenString();
				// perform operation
				Result res = auth (fid, uid, aname);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_FID_IN_USE:		reply = assembleError (tag, "fid in use"); break;
						case Result.RC_NO_AUTH_ENTRY:	reply = assembleError (tag, "no auth file"); break;
						default:						reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					reply = assembleQid (V2.Rauth, tag, res.getEntry());
			} break;
			
			//---------------------------------------------------------
			// Client authenticates with server as user UID and
			// provides a fid point to the filesystem ANAME of server's
			// tree, server provides its unique identifier, QID, for
			// the root of the filesystem to which the client is
			// attached. This is usually the first message pair that
			// transpires in a connection of a client to a server.
			//---------------------------------------------------------
			case V2.Tattach: {
				// get arguments
				int fid = in.getInt();
				int afid = in.getInt();
				String uid = in.getLenString();
				String aname = in.getLenString();
				// perform operation
				Result res = attach (fid, afid, uid, aname, cr);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_NAMESPACE_AVAIL:	reply = assembleError (tag, "no namespace to attach"); break;
						case Result.RC_NOT_AUTHENTICATED:	reply = assembleError (tag, "not authenticated"); break;
						default:							reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleQid (V2.Rattach, tag, res.getEntry());
			} break;

			//---------------------------------------------------------
			// Interrupt pending operation with message tag OLDTAG.
			//---------------------------------------------------------
			case V2.Tflush: {
			} break;

			//---------------------------------------------------------
			// Associate FID with file named NAME in the current
			// directory, server provides its unique QID for the file
			// the FID now points to.
			//---------------------------------------------------------
			case V2.Twalk: {
				// get walk target
				int fid  = in.getInt();
				int newFid  = in.getInt();
				int numNames = in.getShort();
				String[] names = new String[numNames];
				for (int n = 0; n < numNames; n++)
					names[n] = in.getLenString();
				// perform operation
				Result res = walk (fid, newFid, names);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_PARENT_DIRECTORY:	reply = assembleError (tag, "invalid parent directory"); break;
						case Result.RC_FID_IN_USE:			reply = assembleError (tag, "fid already in use"); break;
						case Result.RC_NO_ENTRY:			reply = assembleError (tag, "no such file"); break;
						default:							reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleQids (V2.Rwalk, tag, res.ent);
			} break;
			
			//---------------------------------------------------------
			// Check permissions and open file associated with fid,
			// server returns its unique identifier for the file, the QID.
			//---------------------------------------------------------
			case V2.Topen: {
				// get open mode
				int fid  = in.getInt();
				int mode = in.getByte();
				// perform operation
				Result res = open (fid, mode, cr);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_ENTRY:	reply = assembleError (tag, "no entry found"); break;
						case Result.RC_OPEN_FAILED:	reply = assembleError (tag, "can't open file"); break;
						default:					reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleOpen (tag, res.getEntry(), 0);
			} break;

			//---------------------------------------------------------
			// Create and open a file named NAME in the current directory
			// with permissions PERM, and associate it with fid. If the
			// most significant bit of mode is set, the created file
			// is a directory.
			//---------------------------------------------------------
			case V2.Tcreate: {
				// get parameters
				int fid  = in.getInt();
				String name = in.getLenString ();
				int perm = in.getInt();
				int mode = in.getByte();
				
				// perform operation
				Result res = create (fid, name, perm, mode, cr);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_ENTRY:		reply = assembleError (tag, "no entry found"); break;
						case Result.RC_CREATE_FAILED:	reply = assembleError (tag, "can't create file"); break;
						default:						reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleCreate (tag, res.getEntry(), 0);
			} break;
			
			//---------------------------------------------------------
			// Read COUNT bytes from offset OFFSET from file associated
			// with fid, server returns number of bytes read and data.
			//---------------------------------------------------------
			case V2.Tread: {
				// get offset and size
				int fid  = in.getInt();
				long offset = in.getLong();
				int count = in.getInt();
				
				// perform operation.
				byte[] data = read (fid, offset, count);
				int size = (data != null ? data.length : 0);
				// assemble response
				reply = assembleRead (tag, size, data);
				
			} break;
			
			//---------------------------------------------------------
			// Write COUNT bytes to file associated with fid at offset
			// OFFSET, server returns number of bytes successfully
			// written.
			//---------------------------------------------------------
			case V2.Twrite: {
				// get offset, size and data
				int fid  = in.getInt();
				long offset = in.getLong();
				int size = in.getInt();
				byte[] data = in.getArray (size);
				
				// perform operation
				int count = write (fid, offset, size, data);
				// assemble response
				reply = assembleWrite (tag, count);
			} break;

			//---------------------------------------------------------
			// Remove the association between an entry in the namespace
			// of the server and a fid.
			//---------------------------------------------------------
			case V2.Tclunk: {
				// perform operation
				int fid  = in.getInt();
				clunk (fid);
				// assemble response
				reply = assembleTag (V2.Rclunk, tag);
			} break;
			
			//---------------------------------------------------------
			// Remove file associated with fid.
			//---------------------------------------------------------
			case V2.Tremove: {
				// perform operation
				int fid  = in.getInt();
				remove (fid);
				// assemble response
				reply = assembleTag (V2.Rremove, tag);
			} break;

			//---------------------------------------------------------
			// Retrieve file attributes.
			//---------------------------------------------------------
			case V2.Tstat: {
				// get parameters
				int fid  = in.getInt();
				// perform operation
				byte[] statData = stat (fid);
				if (statData == null)
					return assembleError (tag, "no such file");
				// assemble response
				reply = assembleStat (tag, statData);
			} break;

			//---------------------------------------------------------
			// Set file attributes.
			//---------------------------------------------------------
			case V2.Twstat: {
			} break;
			
			//---------------------------------------------------------
			// Unknown message type
			//---------------------------------------------------------
			default:
				reply = assembleError (tag, "Unknown message type");
				break;
		}
		
		// pass back reply message.
		return reply;
	}

	//=================================================================
	// Implementation of abstract methods from Entry.AttributeHandler
	//=================================================================
	/**
	 * <p>Get entry statistics.</p>
	 * @param e Entry - namespace entry (file)
	 * @return byte[] - file attributes
	 */
	public byte[] getStat (Entry e) {
		Attributes attr = getAttributes (e);

		// compute size of data
		int size = 47;
		size += attr.name.length();
		size += attr.perm.getOwner().length();
		size += attr.perm.getGroup().length();
		size += attr.perm.getOwner().length();
		
		// assemble entry attributes
		Blob b = new Blob();
		b.putShort		(size);
		b.putShort		(attr.device.charAt(0));
		b.putInt		(attr.device.charAt(1));
		b.putByte		(attr.flags >> 24);
		b.putInt		(attr.mtime);
		b.putLong		(attr.qidPath & 0xFFFFFFFFL);
		b.putInt		(attr.perm.getMode() | attr.flags);
		b.putInt		(attr.atime);
		b.putInt		(attr.mtime);
		b.putLong  		(e.getSize());
		b.putLenString	(attr.name);
		b.putLenString	(attr.perm.getOwner());
		b.putLenString	(attr.perm.getGroup());
		b.putLenString	(attr.perm.getOwner());
		byte[] res = b.asByteArray (false);
		return res;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get entry statistics.</p>
	 * @param e Entry - namespace entry (file)
	 * @param stat byte[] - file attributes
	 * @param cr Credential - user credential
	 * @return boolean - attributes modified?
	 */
	public boolean setStat (Entry e, byte[] stat, Credential cr) {
		return false;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get unique identifier of entry.</p>
	 * @param e Entry - namespace entry
	 * @return byte[] - binary representation of qid.
	 */
	public byte[] getQid (Entry e) {
		Blob b = new Blob();
		if (e != null) {
			Attributes attr = getAttributes(e);
			b.putByte (attr.flags >> 24);
			b.putInt  (attr.mtime);
			b.putLong (attr.qidPath);
		} else {
			// empty qid
			b.putByte (0);
			b.putInt  (0);
			b.putLong (0);
		}
		return b.asByteArray (false);
	}
	
	//=================================================================
	//	Message assembler methods
	//=================================================================
	/**
	 * <p>Generate error message.</p>
	 * @param tag int - message identifier
	 * @param msg String - error message
	 * @return Message - assembled error message
	 */
	private Message assembleError (int tag, String msg) {
		int size = msg.length() + 9;
		Message err = new Message (null, size);
		err.putInt   (size);
		err.putByte  (V2.Rerror);
		err.putShort (tag);
		err.putLenString (msg);
		return err;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - version.</p>
	 * @param tag int - transaction identifier
	 * @param msize int - buffer size
	 * @param version String - version identifier
	 * @return Message - assembled reply message
	 */
	private Message assembleVersion (int tag, int msize, String version) {
		int size = version.length() + 13;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Rversion);
		msg.putShort (tag);
		msg.putInt   (msize);
		msg.putLenString (version);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - tag only.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @return Message - assembled reply message
	 */
	private Message assembleTag (int type, int tag) {
		Message msg = new Message (null, 7);
		msg.putInt  (7);
		msg.putByte (type);
		msg.putShort (tag);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - QId.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param e Entry - namespace entry
	 * @return Message - assembled reply message
	 */
	private Message assembleQid (int type, int tag, Entry e) {
		Message msg = new Message (null, 20);
		msg.putInt   (20);
		msg.putByte  (type);
		msg.putShort (tag);
		msg.putArray (getQid(e));
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - Open.</p>
	 * @param tag int - transaction identifier
	 * @param e Entry - namespace entry
	 * @param msize int - available I/O buffer size
	 * @return Message - assembled reply message
	 */
	private Message assembleOpen (int tag, Entry e, int msize) {
		Message msg = new Message (null, 24);
		msg.putInt   (24);
		msg.putByte  (V2.Ropen);
		msg.putShort (tag);
		msg.putArray (getQid(e));
		msg.putInt   (msize);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - Create.</p>
	 * @param tag int - transaction identifier
	 * @param e Entry - namespace entry
	 * @param msize int - available I/O buffer size
	 * @return Message - assembled reply message
	 */
	private Message assembleCreate (int tag, Entry e, int msize) {
		Message msg = new Message (null, 24);
		msg.putInt   (24);
		msg.putByte  (V2.Rcreate);
		msg.putShort (tag);
		msg.putArray (getQid(e));
		msg.putInt   (msize);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - QIdList.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param list Entry[] - list of namespace entries
	 * @return Message - assembled reply message
	 */
	private Message assembleQids (int type, int tag, Entry[] list) {
		int count = list.length;
		int size = 13 * count + 9;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (type);
		msg.putShort (tag);
		msg.putShort (count);
		for (int n = 0; n < count; n++)
			msg.putArray (getQid(list[n]));
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - .</p>
	 * @param tag int - transaction identifier
	 * @param stat byte[] - stat buffer (116 bytes)
	 * @return Message - assembled reply message
	 */
	private Message assembleStat (int tag, byte[] stat) {
		int size = 9 + stat.length;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Rstat);
		msg.putShort (tag);
		msg.putShort (stat.length);
		msg.putArray (stat);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - Read.</p>
	 * @param tag int - transaction identifier
	 * @param count int - counter value
	 * @param data byte[] - transaction data
	 * @return Message - assembled reply message
	 */
	private Message assembleRead (int tag, int count, byte[] data) {
		int size = count+11;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Rread);
		msg.putShort (tag);
		msg.putInt   (count);
		if (data != null)
			msg.putArray (data, 0, count);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - Write.</p>
	 * @param tag int - transaction identifier
	 * @param count int - counter value
	 * @return Message - assembled reply message
	 */
	private Message assembleWrite (int tag, int count) {
		int size = 11;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Rwrite);
		msg.putShort (tag);
		msg.putInt   (count);
		return msg;
	}
}

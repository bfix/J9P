
//******************************************************************
//*   PGMID.        INFERNO STYX PROTOCOL IMPLEMENTATION.          *
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

package j9p.server;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Message;
import j9p.auth.Credential;
import j9p.ns.Entry;
import j9p.ns.Permissions;
import j9p.ns.Entry.Attributes;
import j9p.proto.V1;
import j9p.util.Blob;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The <b>ServerProtocol_V1</b> class implements the protocol
 * from Inferno OS. It can process incoming Tmsg messages and generates
 * outgoing Rmsg messages for the client.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ServerProtocol_V1 extends ServerProtocol {

	//=================================================================
	/**
	 * <p>Instantiate a new ServerProtocol_V1 handler for messages.</p>
	 * @param nsHdlr NamespaceManager - namespace provider
	 * @param needAuth boolean - authentication required?
	 */
	public ServerProtocol_V1 (NamespaceManager nsHdlr, boolean needAuth) {
		super (nsHdlr, needAuth);
	}

	//=================================================================
	/**
	 * <p>Peek into message and return the tag of the operation.</p>
	 * @param in Message - message to be inspected
	 * @return int - tag of operation in message
	 */
	public int getTag (Message in) {
		return in.getShort (1);
	}
	
	//=================================================================
	/**
	 * <p>Check if message is a "flush()" operation that
	 * will terminate another pending operation.</p>
	 * @param in Message - message to be inspected
	 * @return int - tag of flushed operation (or 0)
	 */
	public int isFlushing (Message in) {
		if (in.getByte (0) != V1.Tflush)
			return 0;
		return in.getShort (3);
	}
	
	//=================================================================
	/**
	 * <p>Create printable representation of this message.</p>
	 * @param in Message - message to be printed
	 * @return String - printable Styx message
	 */
	public String toString (Message in) {
		return V1.toString (in);
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
		
		// read "header" fields of Styx message.
		int type = in.getByte();
		int tag  = in.getShort();
		
		// reply message
		Message reply = null;
		
		switch (type) {
			//---------------------------------------------------------
			// Client authenticates with server as user UID and
			// provides a fid point to the filesystem ANAME of server's
			// tree, server provides its unique identifier, QID, for
			// the root of the filesystem to which the client is
			// attached. This is usually the first message pair that
			// transpires in a connection of a client to a server.
			//---------------------------------------------------------
			case V1.Tattach: {
				// get user name and associated namespace
				int fid  = in.getShort();
				String uid = in.getString (28);
				String aname = in.getString (28);
				// perform operation
				Result res = attach (fid, -1, uid, aname, cr);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_NAMESPACE_AVAIL:	reply = assembleError (tag, "no namespace to attach"); break;
						case Result.RC_NOT_AUTHENTICATED:	reply = assembleError (tag, "not authenticated"); break;
						default:							reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleQid (V1.Rattach, tag, fid, res.getEntry());
			} break;
			
			//---------------------------------------------------------
			// Have new fid, NEWFID, associate with the file associated
			// with an existing fid. This usually occurs right before
			// a fid is moved to point to another entry in the current
			// directory with a Walk transaction.
			//---------------------------------------------------------
			case V1.Tclone: {
				// get fids
				int fid  = in.getShort();
				int newFid = in.getShort();
				// perform operation
				Result res = clone (fid, newFid);
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_ENTRY:	reply = assembleError (tag, "no entry found"); break;
						default:					reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleFid (V1.Rclone, tag, fid);
			} break;
			
			//---------------------------------------------------------
			// Associate FID with file named NAME in the current
			// directory, server provides its unique QID for the file
			// the FID now points to.
			//---------------------------------------------------------
			case V1.Twalk: {
				// get walk target
				int fid  = in.getShort();
				String name = in.getString (28);
				// perform operation
				Result res = walk (fid, -1, new String[]{ name });
				// check for errors
				if (res.rc != Result.RC_OK) {
					switch (res.rc) {
						case Result.RC_NO_PARENT_DIRECTORY:	reply = assembleError (tag, "invalid parent directory"); break;
						case Result.RC_FID_IN_USE:			reply = assembleError (tag, "fid already in use"); break;
						case Result.RC_NO_ENTRY:			reply = assembleError (tag, "no such file"); break;
						default:							reply = assembleError (tag, "error (" + res.rc + ")"); break;
					}
				} else
					// assemble response
					reply = assembleQid (V1.Rwalk, tag, fid, res.getEntry());
			} break;
			
			//---------------------------------------------------------
			// Remove the association between an entry in the namespace
			// of the server and a fid.
			//---------------------------------------------------------
			case V1.Tclunk: {
				int fid  = in.getShort();
				// perform operation
				clunk (fid);
				// assemble response
				reply = assembleFid (V1.Rclunk, tag, fid);
			} break;

			//---------------------------------------------------------
			// Retrieve file attributes.
			//---------------------------------------------------------
			case V1.Tstat: {
				int fid  = in.getShort();
				// perform operation
				byte[] statData = stat (fid);
				if (statData == null)
					return assembleError (tag, "no such file");
				// assemble response
				reply = assembleStat (V1.Rstat, tag, fid, statData);
			} break;

			//---------------------------------------------------------
			// Set file attributes.
			//---------------------------------------------------------
			case V1.Twstat: {
				// read stat data
				int fid  = in.getShort();
				byte[] statData = in.getArray(116);
				// perform operation
				int rc = wstat (fid, statData, cr);
				if (rc == -1)
					return assembleError (tag, "no such file");
				else if (rc == -2)
					return assembleError (tag, "attributes not changed");
				// assemble response
				reply = assembleFid (V1.Rwstat, tag, fid);
			} break;

			//---------------------------------------------------------
			// Check permissions and open file associated with fid,
			// server returns its unique identifier for the file, the QID.
			//---------------------------------------------------------
			case V1.Topen: {
				// get open mode
				int fid  = in.getShort();
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
					reply = assembleQid (V1.Ropen, tag, fid, res.getEntry());
			} break;

			//---------------------------------------------------------
			// Create and open a file named NAME in the current directory
			// with permissions PERM, and associate it with fid. If the
			// most significant bit of mode is set, the created file
			// is a directory.
			//---------------------------------------------------------
			case V1.Tcreate: {
				// get parameters
				int fid  = in.getShort();
				String name = in.getString (28);
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
					reply = assembleQid (V1.Rwalk, tag, fid, res.getEntry());
			} break;
			
			//---------------------------------------------------------
			// Read COUNT bytes from offset OFFSET from file associated
			// with fid, server returns number of bytes read and data.
			//---------------------------------------------------------
			case V1.Tread: {
				// get offset and size
				int fid  = in.getShort();
				long offset = in.getLong();
				int count = in.getShort();
				// perform operation.
				byte[] data = read (fid, offset, count);
				int size = (data != null ? data.length : 0);
				// assemble response
				reply = assembleRead (tag, fid, size, data);
			} break;
			
			//---------------------------------------------------------
			// Write COUNT bytes to file associated with fid at offset
			// OFFSET, server returns number of bytes successfully
			// written.
			//---------------------------------------------------------
			case V1.Twrite: {
				// get offset and size
				int fid  = in.getShort();
				long offset = in.getLong();
				int size = in.getShort();
				// skip padding
				in.getByte();
				// read data
				byte[] data = in.getArray (size);
				// perform operation
				int count = write (fid, offset, size, data);
				// assemble response
				reply = assembleCount(V1.Rwrite, tag, fid, count);
			} break;

			//---------------------------------------------------------
			// Remove file associated with fid.
			//---------------------------------------------------------
			case V1.Tremove: {
				int fid  = in.getShort();
				// perform operation
				remove (fid);
				// assemble response
				reply = assembleTag (V1.Rremove, tag);
			} break;

			//---------------------------------------------------------
			// No operation.
			//---------------------------------------------------------
			case V1.Tnop: {
				// assemble response
				reply = assembleTag (V1.Rnop, tag);
			} break;

			//---------------------------------------------------------
			// Interrupt pending operation with message tag OLDTAG.
			//---------------------------------------------------------
			case V1.Tflush: {
				// get tag of operation to terminate
				int oldTag = in.getShort();
				// perform operation
				flush (oldTag);
				// assemble response
				reply = assembleTag (V1.Rflush, tag);
			} break;

			//---------------------------------------------------------
			// Unknown message type
			//---------------------------------------------------------
			default:
				reply = assembleError (tag, "Unknown message type");
				break;
		}
		
		// pass back reply message.
		System.out.println ("StyxSession " + id + ": > " + toString(reply));
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
		
		// re-code file attributes
		Attributes attr = getAttributes (e);
		Blob stat = new Blob (null, 116);
		stat.putString (attr.name, 28);
		stat.putString (attr.perm.getOwner(),  28);
		stat.putString (attr.perm.getGroup(),  28);
		stat.putInt    ((int)attr.qidPath);
		stat.putInt    (attr.mtime);
		stat.putInt    (attr.flags | attr.perm.getMode());
		stat.putInt    (attr.atime);
		stat.putInt    (attr.mtime);
		stat.putLong   (e.getSize());
		stat.putShort  (attr.device.charAt(0));
		stat.putShort  (attr.device.charAt(1));
		return stat.asByteArray (false);
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

		// get old attribute values
		Attributes oldAttr = getAttributes (e);
		
		// allocate new attributes and preset
		// fixed values.
		Attributes newAttr = new Attributes();
		newAttr.qidPath = oldAttr.qidPath;
		newAttr.device = oldAttr.device;
		
		// decode file attributes
		Blob b = new Blob (stat);
		newAttr.name = b.getString (28);

		// read permissions
		String owner = b.getString (28);
		String group = b.getString (28);
		b.getLong(); // skip QID
		int mode = b.getInt();
		newAttr.perm = new Permissions (owner, group, mode);

		// parse timestamps
		newAttr.atime = b.getInt();
		newAttr.mtime = b.getInt();

		// skip size
		//b.getLong();
		// skip device info
		//b.getInt();
		
		// set new attributes
		return setAttributes (e, newAttr, cr);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get unique identifier of entry.</p>
	 * @param e Entry - namespace entry
	 * @return byte[] - binary representation of qid.
	 */
	public byte[] getQid (Entry e) {
		Blob b = new Blob();
		Attributes attr = getAttributes(e);
		b.putInt ((int)attr.qidPath);
		b.putInt (attr.mtime);
		return b.asByteArray (false);
	}

	//=================================================================
	//	Helpers: Message assembler methods
	//=================================================================
	/**
	 * <p>Generate error message.</p>
	 * @param tag int - message identifier
	 * @param msg String - error message
	 * @return Message - assembled error message
	 */
	private Message assembleError (int tag, String msg) {
		Message err = new Message (null, 67);
		err.putByte  (V1.Rerror);
		err.putShort (tag);
		err.putString (msg, 64);
		return err;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - tag only.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @return Message - assembled reply message
	 */
	private Message assembleTag (int type, int tag) {
		Message msg = new Message (null, 3);
		msg.putByte (type);
		msg.putShort (tag);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - fid.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @return Message - assembled reply message
	 */
	private Message assembleFid (int type, int tag, int fid) {
		Message msg = new Message (null, 5);
		msg.putByte (type);
		msg.putShort (tag);
		msg.putShort (fid);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - QId.</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param e Entry - entry reference
	 * @return Message - assembled reply message
	 */
	private Message assembleQid (int type, int tag, int fid, Entry e) {
		Message msg = new Message (null, 13);
		msg.putByte (type);
		msg.putShort (tag);
		msg.putShort (fid);
		msg.putArray (getQid(e));
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble server response - .</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param stat byte[] - stat buffer (116 bytes)
	 * @return Message - assembled reply message
	 */
	private Message assembleStat (int type, int tag, int fid, byte[] stat) {
		Message msg = new Message (null, 121);
		msg.putByte (type);
		msg.putShort (tag);
		msg.putShort (fid);
		msg.putArray (stat);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble Styx server response - Read.</p>
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param count int - counter value
	 * @param data byte[] - transaction data
	 * @return Message - assembled reply message
	 */
	private Message assembleRead (int tag, int fid, int count, byte[] data) {
		Message msg = new Message (null, count+8);
		msg.putByte  (V1.Rread);
		msg.putShort (tag);
		msg.putShort (fid);
		msg.putShort (count);
		msg.putByte  (0);
		if (data != null)
			msg.putArray (data, 0, count);
		return msg;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Assemble Styx server response - .</p>
	 * @param type int - type of Rmsg
	 * @param tag int - transaction identifier
	 * @param fid int - file identifier
	 * @param count int - counter value
	 * @return Message - assembled reply message
	 */
	private Message assembleCount (int type, int tag, int fid, int count) {
		Message msg = new Message (null, 7);
		msg.putByte (type);
		msg.putShort (tag);
		msg.putShort (fid);
		msg.putShort (count);
		return msg;
	}
}

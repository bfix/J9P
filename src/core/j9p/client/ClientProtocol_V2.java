
package j9p.client;

import java.io.IOException;

import j9p.Message;
import j9p.auth.Credential;
import j9p.proto.V2;
import j9p.util.Hex;


public class ClientProtocol_V2 extends ClientProtocol {

	public Credential authenticate (String uname, String aname) throws IOException {

		int authFid = getNextFid();
		
		int size = 15 + uname.length() + aname.length();
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Tauth);
		msg.putShort (getNextTag());
		msg.putInt   (authFid);
		msg.putLenString (uname);
		msg.putLenString (aname);
		comm.sendMessage (msg);
		
		msg = comm.getNextMessage();
		int type = msg.getByte (4);
		if (type == V2.Rerror) {
			System.out.println (msg.getLenString (7));
			return null;
		}
		else if (type != V2.Rauth) {
			System.out.println ("Invalid message type");
			return null;
		}
		//byte[] qid = msg.getArray (7, 13);

		msg = new Message (null, 23);
		msg.putInt  (23);
		msg.putByte  (V2.Tread);
		msg.putShort (getNextTag());
		msg.putInt   (authFid);
		msg.putLong  (0);
		msg.putInt   (2048);
		comm.sendMessage (msg);

		msg = comm.getNextMessage();
		size = msg.getInt (0);
		type = msg.getByte (4);
		if (type != V2.Rread)
			return null;
		byte[] data = msg.getArray (11, size-11);
		String test = new String (data);
		
		System.out.println (Hex.fromArray(data, ':'));
		System.out.println (test);
		
		return null;
	}
	
	public Type.Version negotiateVersion(String version, int msize) throws IOException {
		
		int size = version.length() + 13;
		Message msg = new Message (null, size);
		msg.putInt   (size);
		msg.putByte  (V2.Tversion);
		msg.putShort (getNextTag());
		msg.putInt   (msize);
		msg.putLenString (version);
		comm.sendMessage (msg);
		
		Type.Version res = new Type.Version();
		msg = comm.getNextMessage();
		int type = msg.getByte (4);
		
		// check for error
		if (type == V2.Rerror) {
			res.version = msg.getLenString (7);
			res.msize = -1;
			return res;
		} else if (type != V2.Rversion) {
			res.version = new String (msg.asByteArray(true));
			res.msize = -1;
			return res;
		}
		
		res.version = msg.getLenString (11);
		res.msize = msg.getInt (7);
		return res;
	}

	public Type.Qid attach (String user, String aname) throws IOException {
		return null;
	}
}

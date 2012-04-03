
package j9p.client;

import java.io.IOException;

import j9p.auth.Credential;


public class ClientProtocol_V1 extends ClientProtocol {

	public Type.Qid attach (String user, String aname) throws IOException {
		return null;
	}

	public Type.Version negotiateVersion(String version, int msize) throws IOException {
		return null;
	}
	
	public Credential authenticate (String uname, String aname) throws IOException {
		return null;
	}

}


//******************************************************************
//*   PGMID.        STYX AUTHENTICATION DEBUGGER.                  *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/28.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
//*                 AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.       *
//******************************************************************

package j9p.debug;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;
import java.util.Vector;
import j9p.auth.AP_Generic;
import j9p.auth.AP_p9any;
import j9p.auth.AP_p9sk1;
import j9p.auth.AP_Inferno;
import j9p.auth.AuthProtocolHandler;
import j9p.Channel;
import j9p.util.Blob;
import j9p.util.Hex;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Debug authentication process.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public aspect AuthenticationDebug {

	//=================================================================
	//	Pointcuts
	//=================================================================
	/**
	 * <p>Authentication process.</p>
	 * @param ch Channel - communication channel
	 */
	private pointcut authenticateOnChannel (Channel ch) :
		call (int AP_Generic+.process (Channel))
		&& args (ch)
	;
	//-----------------------------------------------------------------
	/**
	 * <p>Sending AuthEntry data to peer.</p>
	 * @param hdlr AuthProtocolHandler - protocol handler instance
	 * @param b Blob - data to be sent to peer
	 */
	private pointcut auth_toPeer (AuthProtocolHandler hdlr, Blob b):
		call (int AuthProtocolHandler+.getDataForPeer (Blob))
		&& args (b)
		&& target (hdlr);
	;
	//-----------------------------------------------------------------
	/**
	 * <p>Handle AuthEntry data from peer.</p>
	 * @param hdlr AuthProtocolHandler - protocol handler instance
	 * @param b Blob - data to be sent to peer
	 */
	private pointcut auth_fromPeer (AuthProtocolHandler hdlr, Blob b) :
		call (int AuthProtocolHandler+.handlePeerData (Blob))
		&& args (b)
		&& target (hdlr);
	;
	
	//=================================================================
	//	Advices
	//=================================================================
	/**
	 * <p>Successful call to 'getDataForPeer()'.</p>
	 * @param b Blob - data to be send to peer
	 */
	after(AuthProtocolHandler hdlr, Blob b) returning (int rc) :
		auth_toPeer (hdlr, b)
	{		
		String proto = "<none>";
		boolean asString = false;
		
		if (hdlr instanceof AP_p9sk1) {
			proto = "p9sk1";
			asString = false;
		}
		else if (hdlr instanceof AP_p9any) {
			proto = "p9any";
			asString = true;
		}
		else if (hdlr instanceof AP_Inferno) {
			proto = "inferno";
			asString = true;
		}
		// log message
		//System.out.println (">>>> " + proto + " -- outgoing data");
		byte[] data = b.asByteArray (false);
		if (asString)
			System.out.println ("[" + proto + "] > " + new String(data));
		else {
			Vector<String> out = Hex.fromArraySplit (data, ':', 16);
			for (String line : out) {
				System.out.println ("[" + proto + "] > " + line);
			}
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Failed call to 'getDataForPeer()'.</p>
	 * @param ch StyxChannel - communication channel
	 * @param exc IOException - reason
	 */
	after(AuthProtocolHandler hdlr, Blob b) throwing (IOException exc) :
		auth_toPeer (hdlr, b)
	{
		String proto = "<none>";
		
		if (hdlr instanceof AP_p9sk1) {
			proto = "p9sk1";
		}
		else if (hdlr instanceof AP_p9any) {
			proto = "p9any";
		}
		else if (hdlr instanceof AP_Inferno) {
			proto = "inferno";
		}

		System.out.println (">>>> " + proto + " -- outgoing data EXCEPTION");
		exc.printStackTrace();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Call to 'getDataForPeer()'.</p>
	 * @param b Blob - data from peer
	 */
	before(AuthProtocolHandler hdlr, Blob b) : auth_fromPeer (hdlr, b)
	{		
		String proto = "<none>";
		boolean asString = false;
		
		if (hdlr instanceof AP_p9sk1) {
			proto = "p9sk1";
			asString = false;
		}
		else if (hdlr instanceof AP_p9any) {
			proto = "p9any";
			asString = true;
		}
		else if (hdlr instanceof AP_Inferno) {
			proto = "inferno";
			asString = true;
		}

		//System.out.println ("<<<< " + proto + " -- incoming data");
		if (b != null) {
			byte[] data = b.asByteArray (false);
			if (asString)
				System.out.println ("[" + proto + "] < " + new String(data));
			else {
				Vector<String> out = Hex.fromArraySplit (data, ':', 16);
				for (String line : out) {
					System.out.println ("[" + proto + "] < " + line);
				}
			}
		}
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Authenticate user.</p>
	 * @param ch Channel - communication channel
	 * @return Credential - user credential
	 */
	int around (Channel ch) : authenticateOnChannel (ch) {
		// call authentication method
		int rc = proceed (ch);
		if (rc == AuthProtocolHandler.AUTH_SUCCESS)
			System.out.println ("PEER AUTHENTICATION SUCCESSFUL!");
		else
			System.out.println ("PEER AUTHENTICATION FAILED!");
		return rc;
	}
}

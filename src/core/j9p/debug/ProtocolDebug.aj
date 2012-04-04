
//******************************************************************
//*   PGMID.        STYX PROTOCOL DEBUGGER.                        *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/28.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
//*                 AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.       *
//******************************************************************

package j9p.debug;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Channel;
import j9p.Message;
import j9p.Session.Protocol;
import j9p.auth.Credential;
import j9p.crypto.SecureChannel;
import j9p.io.StyxStreamChannel;
import j9p.util.Hex;
import java.io.IOException;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Debug protocol flow (messages).</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public aspect ProtocolDebug {
	
	public static final boolean SHOW_P9MSG		= true;
	public static final boolean SHOW_RAW		= false;

	//=================================================================
	//	Pointcuts
	//=================================================================
	/**
	 * <p>Message processing (Styx/9P level).</p>
	 * @param inst Protocol - specific protocol implementation
	 * @param id int - session identifier
	 * @param in Message - incoming styx message
	 * @param cr Credential - user credential
	 */
	private pointcut process (Protocol inst, int id, Message in, Credential cr) :
		   call (Message Protocol+.process (int,Message, Credential))
		&& args (id,in, cr)
		&& target (inst)
	;
	//-----------------------------------------------------------------
	/**
	 * <p>Get message.</p>
	 */
	private pointcut getMessage (Channel ch) :
		   call (Message StyxChannel+.getNextMessage ())
		&& target (ch)
	;
	//-----------------------------------------------------------------
	/**
	 * <p>Send (combined) authentication message call.</p>
	 * @param msg String - message content (multi-line)
	 * @param ch Channel - communication channel
	 */
	private pointcut sendMessage (Message msg, Channel ch) :
		   call (boolean Channel+.sendMessage (Message))
		&& args (msg)
		&& target (ch)
	;

	//=================================================================
	//	Advices
	//=================================================================
	/**
	 * <p>Print incoming and outgoing messages.</p>
	 * @param inst Protocol - specific protocol implementation
	 * @param id int - session identifier
	 * @param in Message - incoming styx message
	 * @param cr Credential - user credential
	 * @return Message - outgoing styx message
	 */
	StyxMessage around (Protocol inst, int id, Message in, Credential cr) :
		process (inst, id, in, cr)
	{
		if (SHOW_P9MSG)
			System.out.println ("[StyxSession " + id + "] < " + inst.toString(in));
		StyxMessage out = proceed (inst, id, in, cr);
		if (SHOW_P9MSG) {
			System.out.println ("[StyxSession " + id + "] > " + inst.toString(out));
			System.out.flush();
		}
		return out;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>After successful read of next message.</p> 
	 * @param msg Message - message received (or null)
	 */
	after(Channel ch) returning (Message msg) : getMessage (ch) {
		if (msg == null || !SHOW_RAW)
			return;
		String fmt = getChannelFormat (ch);
		if (fmt == null)
			return;
		System.out.print ("incoming [" + fmt + "]: ");
		System.out.println (Hex.fromArray (msg.asByteArray(false), ':'));
		System.out.flush();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Reading next message failed.</p>
	 * @param exc IOException - reason
	 */
	after(Channel ch) throwing (IOException exc) : getMessage (ch) {
		
		String fmt = getChannelFormat (ch);
		if (fmt == null || !SHOW_RAW)
			return;
		System.out.println ("incoming [" + fmt + "]: FAILED!");
		System.out.flush();
		exc.printStackTrace();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Before send of message.</p> 
	 * @param msg Message - outgoing message
	 */
	before(Message msg, Channel ch) : sendMessage (msg, ch) {
		if (msg == null || !SHOW_RAW)
			return;
		String fmt = getChannelFormat (ch);
		if (fmt == null)
			return;
		System.out.print ("outgoing [" + fmt + "]: ");
		System.out.println (Hex.fromArray (msg.asByteArray(false), ':'));
		System.out.flush();
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Sending message failed.</p>
	 * @param exc IOException - reason
	 */
	after(Message msg, Channel ch) throwing (IOException exc) : sendMessage (msg, ch) {
		String fmt = getChannelFormat (ch);
		if (fmt == null || !SHOW_RAW)
			return;
		System.out.println ("outgoing [" + fmt + "]: FAILED!");
		System.out.flush();
		exc.printStackTrace();
	}

	//=================================================================
	//	Helpers
	//=================================================================
	/**
	 * <p>Get name of protocol used by target instance.</p>
	 * @param jp JoinPoint - point in control flow
	 * @return String - name of handled format 
	 */
	public String getChannelFormat (Channel ch) {
		
		if (ch instanceof SecureChannel)
			return "CRYPTO";
		if (ch instanceof StreamChannel)
			return "RAW";
		return "";
	}
}

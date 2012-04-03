
//******************************************************************
//*   PGMID.        STYX CRYPTO DEBUGGER.                          *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/28.                                      *
//*   COPYRIGHT.    (C) BY BERND R. FIX. ALL RIGHTS RESERVED.      *
//*                 LICENSED MATERIAL - PROGRAM PROPERTY OF THE    *
//*                 AUTHOR. REFER TO COPYRIGHT INSTRUCTIONS.       *
//******************************************************************

package j9p.debug;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.crypto.ChannelCipher;
import javax.crypto.SecretKey;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Debug cryptographic operations.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public aspect CryptoDebug {

	//=================================================================
	//	Pointcuts
	//=================================================================
	/**
	 * <p>Set secret key for channel encryption.</p> 
	 * @param key SecretKey - key to be used for en-/decryption
	 */
	pointcut setKey (SecretKey key) :
		this (ChannelCipher) && set (SecretKey ChannelCipher.key) && args (key);
	
	//=================================================================
	//	Advices
	//=================================================================
	/**
	 * <p>Print key information.</p>
	 * @param key SecretKey - key to be used for en-/decryption
	 */
	before (SecretKey key) : setKey (key) {
		StringBuffer buf = new StringBuffer ();
		buf.append ("Cipher: secret key = ");
		buf.append (key.getAlgorithm());
		buf.append (" (");
		buf.append (key.getFormat());
		buf.append (")");
		System.out.println (buf.toString());
	}
}


//******************************************************************
//*   PGMID.        PLAN9/INFERNO KEYRING FILE HANDLER.            *
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

package j9p.auth;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.InputStream;
import java.io.FileInputStream;
import java.math.BigInteger;

import j9p.io.ContentHandler;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Read and write authentication data to Plan9/Inferno keyring files.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class KeyringFile {

	//=================================================================
	/**
	 * <p>A (Plan9/Inferno) keyring file combines three different
	 * cryptographic entities in one unit: A private RSA key,
	 * a certificate of the public key and parameters used
	 * in the Diffie-Hellman key exchange protocol. For sake of
	 * the argument we differentiate between these entities and
	 * encapsulate them in a new class. The private key argument
	 * can be null to indicate that the private key is secret.</p>
	 */
	protected static class AuthSet {
		protected PrivateKey prvKey = null;
		protected Certificate cert = null;
		protected DH_KeyExchange dhParam = null;
		
		//-------------------------------------------------------------
		/**
		 * <p>Convert to AuthData.xml format</p>
		 * @return String - auth config data
		 */
		protected String toConfigXML () {
			StringBuffer buf = new StringBuffer();
			buf.append ("<Identity name=\"...\" domain=\"...\">\n");
			buf.append (prvKey.toConfigXML());
			buf.append ("\n");
			buf.append (cert.toConfigXML());
			buf.append ("\n<Certificate name=\"${signer}\" ... />\n");
			buf.append ("<DHparam>\n<Alpha>");
			buf.append (ContentHandler.toBase64 (dhParam.alpha));
			buf.append ("</Alpha>\n<P>");
			buf.append (ContentHandler.toBase64 (dhParam.p));
			buf.append ("</P>\n</DHparam>\n</Identity>\n");
			return buf.toString();
		}
	}
	
	//=================================================================
	// Read and write Certificates from and to Plan9/Inferno
	// keyring entries (files).
	//=================================================================

	/**
	 * <p>Read a Plan9/Inferno AuthSet from a keyring file:</p>
	 * <p>The file format is as follows: Each segment starts with
	 * a four-character size on a single line that denotes the total
	 * size of the segment (not including the size field and delimiter).
	 * If a segment contains multiple entries, the lines are separated
	 * with a newline character (also the last one!); a single entry
	 * is not terminated by newline:</p>
	 *  <ul>
	 *  	<li>
	 *  		<p><b>[Public key of signer]</b></p>
	 *  		<ul>
	 *  			<li>Encryption algorithm / key scheme (must be "<b>rsa</b>")</li>
	 *  			<li>fully qualified name of signer</li>
	 *  			<li>RSA modulus</li>
	 *  			<li>RSA public exponent</li>
	 *  		</ul>
	 * 		</li>
	 * 		<li>
	 * 			<p><b>[Signature]</b></p>
	 * 			<ul>
	 *  			<li>Encryption algorithm / key scheme (must be "<b>rsa</b>")</li>
	 *  			<li>Hashing algorithm (one of "<b>md4</b>", "<b>md5</b>" or "<b>sha1</b>")</li>
	 *  			<li>fully qualified name of signer</li>
	 *  			<li>Expiration date of certificate</li>
	 *  			<li>Signature data</li>
	 * 			</ul>
	 * 		</li>
	 * 		<li>
	 * 			<p><b>[Private/public key of holder]</b></p>
	 * 			<ul>
	 *  			<li>Encryption algorithm / key scheme (must be "<b>rsa</b>")</li>
	 *  			<li>fully qualified name of holder</li>
	 *  			<li>RSA modulus</li>
	 *  			<li>RSA public exponent</li>
	 *  			<li>------ start of private part</li>
	 * 				<li>RSA private exponent</li>
	 * 				<li>RSA prime factor p</li>
	 * 				<li>RSA prime factor q</li>
	 * 				<li>RSA prime exponent pE</li>
	 * 				<li>RSA prime exponent qE</li>
	 * 				<li>RSA coefficient</li>
	 * 			</ul>
	 * 		</li>
	 * 		<li><b>[DH alpha]</b></p></li>
	 * 		<li><b>[DH modulus]</b></p></li>
	 * 	</ul>
	 */
	public static AuthSet read (String fName) {
		
		try {
			// allocate result
			AuthSet res = new AuthSet();

			// get an input stream to file
			InputStream is = new FileInputStream (fName);
			
			//---------------------------------------------------------
			// Section 1: public key of signer
			//---------------------------------------------------------
			String[] content = ContentHandler.getNextSection (is);
			PublicKey pubSigner = new PublicKey (content);

			//---------------------------------------------------------
			// Section 2: Certificate
			//---------------------------------------------------------
			content = ContentHandler.getNextSection (is);
			res.cert = new Certificate (content);
			if (!res.cert.signer.equals (pubSigner.name))
				throw new Exception ("Signer names don't match");
			res.cert.pubSigner = pubSigner;
			
			//---------------------------------------------------------
			// Section 3: private key of certificate holder
			//---------------------------------------------------------
			content = ContentHandler.getNextSection (is);
			if (content.length < 5)
				// we only have a public key
				res.cert.pubHolder =new PublicKey (content);
			else {
				// we have a complete private key
				PrivateKey prv = new PrivateKey (content);
				if (!prv.validate())
					throw new Exception ("Invalid private key");
				res.prvKey = prv;
				res.cert.pubHolder = prv.getPublicKey();
			}

			//---------------------------------------------------------
			// Section 4: Diffie-Hellman parameter "alpha"
			//---------------------------------------------------------
			content = ContentHandler.getNextSection (is);
			BigInteger dhAlpha = ContentHandler.fromBase64 (content[0]);
			
			//---------------------------------------------------------
			// Section 5: Diffie-Hellman parameter "p"
			//---------------------------------------------------------
			content = ContentHandler.getNextSection (is);
			BigInteger dhP = ContentHandler.fromBase64 (content[0]);
			res.dhParam = new DH_KeyExchange (dhAlpha, dhP);
			
			// return assembled entities
			return res;
		}
		catch (Exception e) {
			System.err.println ("Error reading keyring entry: " + e.getMessage());
			return null;
		}
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Write AuthSet data to a Plan9/Inferno keyring file.</p>
	 * @param auth AuthSet - authentication data 
	 * @return StringBuffer - content buffer
	 */
	public static StringBuffer toKeyringFile (AuthSet auth) {
		StringBuffer buf = new StringBuffer ();
		
		// Section 1: public key of signer
		buf.append (auth.cert.pubSigner.toContent());
		// Section 2: Signature
		buf.append (auth.cert.toContent());
		// Section 3: private key of certificate holder
		buf.append (auth.prvKey.toContent());
		// Section 4: Diffie-Hellman parameter "alpha"
		buf.append (ContentHandler.createSection (new String[] {
			ContentHandler.toBase64 (auth.dhParam.alpha)
		}));
		// Section 5: Diffie-Hellman parameter "p"
		buf.append (ContentHandler.createSection (new String[] {
			ContentHandler.toBase64 (auth.dhParam.p)
		}));
		return buf;
	}
	
	//=================================================================
	/**
	 * <p>Convert keyring file of server to AuthData.xml format.</p> 
	 * @param argv String[] - command line arguments
	 */
	public static void main (String[] argv) {
		KeyringFile.AuthSet auth = KeyringFile.read (argv[0]);
		System.out.println (auth.toConfigXML());
	}
}

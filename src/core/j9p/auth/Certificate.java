
//******************************************************************
//*   PGMID.        PLAN9/INFERNO CERTIFICATE IMPLEMENTATION.      *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/10.                                      *
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

import java.math.BigInteger;

import j9p.io.ContentHandler;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>Certificate</b> holds the information contained in a Plan9/Inferno
 * certificate.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class Certificate extends Entity {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected String		holder;			// name of certificate holder
	protected String		encryption;		// encryption algorithm ("rsa");
	protected String		digest;			// hashing algorithm ("sha1");
	protected String		signer;			// name of signer
	protected BigInteger	signature;		// signature value
	protected int			expiresAt;		// expiration date of certificate
	/*
	 * References to keys are not available in certificates,
	 * only the name of the signer. We "reconstruct" these
	 * references when reading keyring or XML configuration
	 * files.
	 */
	protected PublicKey		pubSigner;		// public key of signer
	protected PublicKey		pubHolder;		// public key of certificate holder
	
	//=================================================================
	//	Constructors
	//=================================================================

	/**
	 * <p>Instantiate a new certificate for given holder. The certificate
	 * will expire at the given epoch.</p>
	 * @param name String - name of certificate holder
	 * @param expires int - epoch value of expiraton date
	 */
	public Certificate (String name, int expires) {
		// set attributes
		encryption = "rsa";
		holder = name;
		expiresAt = expires;
		pubSigner = pubHolder = null;
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate certificate from content data.</p>
	 * @param content String[] - list of content strings
	 */
	protected Certificate (String[] content) {
		fromContentArray (content);
	}
	
	//=================================================================
	// Conversion to/from content data
	//=================================================================
	/**
	 * <p>Get list of content strings from entity data. This is just
	 * the entity as seen in Plan9/Inferno, which is incomplete (no
	 * keys are directly referenced, only by name).</p>
	 * @return String[] - list of content strings
	 */
	public String[] toContentArray () {
		return new String[] {
			encryption, digest, signer, "" + expiresAt,
			ContentHandler.toBase64 (signature)
		};
	}
	//-----------------------------------------------------------------
	/**
	 * <p> Initialize entity data from message content.</p>
	 * @param content String[] - list of content strings
	 * @return boolean - successful operation?
	 */
	public boolean fromContentArray (String[] content) {
		encryption = content[0];
		digest = content[1];
		signer = content[2];
		expiresAt = ContentHandler.getPosNumber (content[3]);
		signature = ContentHandler.fromBase64 (content[4]);
		return true;
	}

	//=================================================================
	/**
	 * <p>Convert entity data to configuration XML data.</p>
	 * @return String - XML encoded entity
	 */
	public String toConfigXML () {
		StringBuffer buf = new StringBuffer ();
		buf.append ("<Certificate name=\"");
		buf.append (pubHolder.name);
		buf.append ("\" expires=\"");
		buf.append (expiresAt);
		buf.append ("\">\n");
		buf.append (pubHolder.toConfigXML ());
		buf.append ("\n<Signature digest=\"");
		buf.append (digest);
		buf.append ("\">\n<Value>");
		buf.append (ContentHandler.toBase64 (signature));
		buf.append ("</Value>\n");
		buf.append (pubSigner.toConfigXML ());
		buf.append ("\n</Signature>\n</Certificate>");
		return buf.toString();
	}
	
	//=================================================================
	/**
	 * <p>Return printable representation of a certificate.</p>
	 * @return String - printable certificate
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append ("Name: " + holder + "\n");
		buf.append ("Encryption: " + encryption + "\n");
		buf.append ("Holder key: " + pubHolder + "\n");
		buf.append ("Digest: " + digest + "\n");
		buf.append ("Signature: " + signature + "\n");
		buf.append ("Signer key: " + pubSigner + "\n");
		return buf.toString();
	}
	
	//=================================================================
	/**
	 * <p>Test invocation of certificate methods.</p> 
	 * @param argv String[] - command line arguments
	 */
	public static void main (String[] argv) {
		KeyringFile.AuthSet auth = KeyringFile.read (argv[0]);
		System.out.println (auth.cert.toConfigXML());
	}
}

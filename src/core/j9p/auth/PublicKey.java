
//******************************************************************
//*   PGMID.        RSA PUBLIC KEY.                                *
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
 * <p>A RSA public key with modulus and public exponent.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class PublicKey extends Entity {
	
	//=================================================================
	/*
	 * Attributes:
	 */
	protected String		name;	// name of key holder
	protected String		encAlg;	// encryption algorithm (must be "rsa")
	protected BigInteger 	m;		// modulus
	protected BigInteger 	e;		// public exponent 

	//=================================================================
	//	Constructors
	//=================================================================
	/**
	 * <p>Instantiate a new named public key.</p> 
	 * @param name String - name of key holder
	 */
	public PublicKey (String name) {
		this.name = name;
		encAlg = "rsa";
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate a public key form content data.</p>
	 * @param content String[] - list of content strings
	 */
	protected PublicKey (String[] content) {
		fromContentArray (content);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate an empty public key</p> 
	 */
	protected PublicKey () {
	}
	
	//=================================================================
	/**
	 * <p>Get list of content strings from entity data.</p>
	 * @return String[] - list of content strings
	 */
	public String[] toContentArray () {
		return new String[] {
			encAlg, name,
			ContentHandler.toBase64 (m),	
			ContentHandler.toBase64 (e)	
		};
	}
	
	//=================================================================
	/**
	 * <p> Initialize entity data from message content.</p>
	 * @param content String[] - list of content strings
	 * @return boolean - successful operation?
	 */
	public boolean fromContentArray (String[] content) {
		encAlg = content[0];
		name = content[1];
		m = ContentHandler.fromBase64 (content[2]);
		e = ContentHandler.fromBase64 (content[3]);
		return true;
	}

	//=================================================================
	/**
	 * <p>Convert entity data to configuration XML data.</p>
	 * @return String - XML encoded entity
	 */
	public String toConfigXML () {
		StringBuffer buf = new StringBuffer ();
		buf.append ("<PublicKey name=\"");
		buf.append (name);
		buf.append ("\">\n<Modulus>");
		buf.append (ContentHandler.toBase64 (m));
		buf.append ("</Modulus>\n<PublicExp>");
		buf.append (ContentHandler.toBase64 (e));
		buf.append ("</PublicExp>\n</PublicKey>");
		return buf.toString();
	}

	//=================================================================
	/**
	 * <p>Return a printable representation of a private key.</p>
	 * @return String - printable private key
	 */
	public String toString () {
		StringBuffer buf = new StringBuffer();
		buf.append ("[PublicKey for " + name + ":\n");
		buf.append ("{encAlg=" + encAlg + "}\n");
		buf.append ("{modulus=" + m + "}\n");
		buf.append ("{publicExp=" + e + "}\n");
		buf.append ("]");
		return buf.toString();
	}
}

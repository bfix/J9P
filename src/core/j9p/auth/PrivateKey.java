
//******************************************************************
//*   PGMID.        RSA PRIVATE KEY.                               *
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
 * <p>A RSA private key with full set of parameters.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class PrivateKey extends PublicKey {

	//=================================================================
	/*
	 * Attributes:
	 */
	protected BigInteger d;			// private exponent d 
	protected BigInteger p, q;		// prime factors of modulus
	protected BigInteger pE, qE;	// prime exponents
	protected BigInteger crt;		// remainder

	//=================================================================
	//	Constructors:
	//=================================================================
	/**
	 * <p>Instantiate a new named key.</p>
	 * @param name String - name of key holder
	 */
	public PrivateKey (String name) {
		super (name);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate key from message content.</p>
	 * @param content String - list of content strings
	 */
	protected PrivateKey (String[] content) {
		fromContentArray (content);
	}
	
	//=================================================================
	/**
	 * <p>Get public key associated with this private key.</p>
	 * @return PublicKey - associated public key
	 */
	public PublicKey getPublicKey () {
		PublicKey res = new PublicKey (name);
		res.encAlg = encAlg;
		res.m = m;
		res.e = e;
		return res;
	}
	
	//=================================================================
	/**
	 * <p>Validate private key parameters.</p>
	 * @return boolean - key is valid
	 */
	public boolean validate () {
		
		// check primes.
		BigInteger mm = p.multiply(q);
		if (!mm.equals (m))
			return false;
		
		BigInteger pm1 = p.subtract (BigInteger.ONE);
        BigInteger qm1 = q.subtract (BigInteger.ONE);
        BigInteger r = pm1.multiply(qm1).divide(pm1.gcd(qm1));

        // check public exponent.
        if (!r.gcd (e).equals (BigInteger.ONE))
        	return false;

        // check private exponent
        BigInteger dd = e.modInverse (r);
		if (!dd.equals (d)) {
			// probably just a different representation
			BigInteger x = d.mod(r);
			if (!x.equals(dd))
				return false;
		}
		
		// check prime exponents and coefficient
		BigInteger pEE = e.modInverse (pm1);
		if (!pEE.equals (pE))
			return false;
		BigInteger qEE = e.modInverse (qm1);
		if (!qEE.equals (qE))
			return false;
		BigInteger coeff = (q.compareTo(p) < 0 ? q.modInverse(p) : p.modInverse(q));
		return coeff.equals (crt);
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
			ContentHandler.toBase64 (e),	
			ContentHandler.toBase64 (d),	
			ContentHandler.toBase64 (p),	
			ContentHandler.toBase64 (q),	
			ContentHandler.toBase64 (pE),	
			ContentHandler.toBase64 (qE),	
			ContentHandler.toBase64 (crt)	
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
		m   = ContentHandler.fromBase64 (content[2]);
		e   = ContentHandler.fromBase64 (content[3]);
		d   = ContentHandler.fromBase64 (content[4]);
		p   = ContentHandler.fromBase64 (content[5]);
		q   = ContentHandler.fromBase64 (content[6]);
		pE  = ContentHandler.fromBase64 (content[7]);
		qE  = ContentHandler.fromBase64 (content[8]);
		crt = ContentHandler.fromBase64 (content[9]);
		return true;
	}

	//=================================================================
	/**
	 * <p>Convert entity data to configuration XML data.</p>
	 * @return String - XML encoded entity
	 */
	public String toConfigXML () {
		StringBuffer buf = new StringBuffer ();
		buf.append ("<PrivateKey name=\"");
		buf.append (name);
		buf.append ("\">\n<Modulus>");
		buf.append (ContentHandler.toBase64 (m));
		buf.append ("</Modulus>\n<PublicExp>");
		buf.append (ContentHandler.toBase64 (e));
		buf.append ("</PublicExp>\n<PrivateExp>");
		buf.append (ContentHandler.toBase64 (d));
		buf.append ("</PrivateExp>\n<PrimeP>");
		buf.append (ContentHandler.toBase64 (p));
		buf.append ("</PrimeP>\n<PrimeQ>");
		buf.append (ContentHandler.toBase64 (q));
		buf.append ("</PrimeQ>\n<PrimeExpP>");
		buf.append (ContentHandler.toBase64 (pE));
		buf.append ("</PrimeExpP>\n<PrimeExpQ>");
		buf.append (ContentHandler.toBase64 (qE));
		buf.append ("</PrimeExpQ>\n<Coefficient>");
		buf.append (ContentHandler.toBase64 (crt));
		buf.append ("</Coefficient>\n</PrivateKey>");
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
		buf.append ("{modulus=" + m + " (" + m.bitLength() + " bits)}\n");
		buf.append ("{publicExp=" + e + "}\n");
		buf.append ("{privateExp=" + d + "}\n");
		buf.append ("{prime p=" + p + "}\n");
		buf.append ("{prime q=" + q + "}\n");
		buf.append ("{primeExpP=" + pE + "}\n");
		buf.append ("{primeExpQ=" + qE + "}\n");
		buf.append ("{coefficient=" + crt + "}\n");
		buf.append ("]");
		return buf.toString();
	}
}

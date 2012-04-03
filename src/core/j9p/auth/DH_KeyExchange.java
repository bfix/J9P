
//******************************************************************
//*   PGMID.        DIFFIE-HELLMAN KEY EXCHANGE.                   *
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
import java.security.SecureRandom;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>Objects of class <b>DH_KeyExchange</b> hold the parameters ('alpha' and 'p')
 * required for a Diffie-Hellman key exchange process between peers.</p>
 * <p>The key exchange process is identical for both peers:</p>
 * <ol>
 *   <li>Generate a random value 'r' (mod 'p')</li>
 *   <li>Compute 's' = 'alpha'^'r' mod 'p'</li>
 *   <li>Send 's' to peer and receive corresponding value ('s2')</li>
 *   <li>
 *   	Compute 'u' = 's2'^'r'. This value is identical for
 *   	both peers ('u' = 'alpha'^('r'*'r2') mod p) and can be
 *   	used for further (symmetrical) encryptions. 
 *   </li>
 * </ol>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class DH_KeyExchange {

	//=================================================================
	/*
	 * Constants
	 */
	private static final BigInteger ONE = BigInteger.ONE;
	private static final BigInteger THREE = BigInteger.valueOf(3);

	//=================================================================
	/*
	 * Diffie-Hellman key exchange parameters.
	 */
	protected BigInteger alpha;
	protected BigInteger p;
	/**
	 * <p>Our private random value.</p>
	 */
	private BigInteger r = null;
	
	//=================================================================
	/**
	 * <p>Instantiate a new key exchange handler from given
	 * parameter values.</p>
	 * @param alpha BigInteger - Diffie-Hellman parameter "alpha"
	 * @param p BigInteger - Diffie-Hellman parameter "p"
	 */
	public DH_KeyExchange (BigInteger alpha, BigInteger p) {
		this.alpha = alpha;
		this.p = p;
	}
	
	//=================================================================
	/**
	 * <p>Initialize new key exchange. Returns the computed value
	 * for 's' that is send to the peer-</p> 
	 * @return BigInteger - value of s = alpha^r mod p
	 */
	public BigInteger init () {
		// generate a new random value r
		int modSize = p.bitLength();
		do {
			r = new BigInteger (modSize, new SecureRandom());
			r = r.mod(p.subtract(THREE)).add(ONE);
		} while (r.bitLength() < modSize/4);
		// compute s
		return alpha.modPow (r, p);
	}
	
	//=================================================================
	/**
	 * <p>Compute shared secret from peer's value of 's'.</p> 
	 * @param s BigInteger - peer value of s
	 * @return byte[] - shared secret
	 */
	public byte[] getSharedSecret (BigInteger s) {
		// shared secret: alpha^(r*r') mod p
		byte[] data = s.modPow(r, p).toByteArray();
		if (data[0] != 0)
			return data;
		byte[] res = new byte [data.length - 1];
		System.arraycopy (data, 1, res, 0, data.length-1);
		return res;
	}
	
	//=================================================================
	/**
	 * <p>Clone a DH key exchange instance.</p>
	 * @return DH_KeyExchange - cloned instance
	 */
	public DH_KeyExchange clone () {
		return new DH_KeyExchange (alpha, p);
	}
}

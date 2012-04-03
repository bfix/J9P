
//******************************************************************
//*   PGMID.        STYX COMM CHANNEL CIPHER ENGINE.               *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/17.                                      *
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

package j9p.crypto;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A ChannelCipher encrypts and decrypts messages send over a secure
 * Styx communication channel.</p>
 *  
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class ChannelCipher {
	
	//=================================================================
	// static attributes and methods.
	//=================================================================
	/*
	 * List of supported encryption algorithms.
	 */
	private static final String[] algs = {
		"rc4", "rc4_40", "rc4_128", "rc4_256",
		"des_56_cbc", "des_56_ecb",
		"ideacbc", "ideaecb"
	};
	/*
	 * Mapping to Java-specific names.
	 */
	private static final String[] cipherAlgs = {
		"RC4", "RC4", "RC4", "RC4",
		"DES/CBC/NoPadding", "DES/ECB/NoPadding",
		"IDEA/CBC/NoPadding",  "IDEA/EBC/NoPadding"
	};
	private static final String[] keyAlgs = {
		"RC4", "RC4", "RC4", "RC4",
		"DES", "DES",
		"IDEA", "IDEA"
	};
	/**
	 * <p>Key sizes (in bytes).</p>
	 */
	private static final int[] keySize = {
		16, 5, 16, 32,
		8, 8,
		32, 32
	};
	/**
	 * <p>Length of initialization vector IV (in bytes).</p>
	 */
	private static final int[] ivSize = {
		0, 0, 0, 0,
		8, 0,
		32, 0
	};
	
	//-----------------------------------------------------------------
	/**
	 * <p>Check if a given cipher algorithm is supported.</p>
	 * @param alg String - algorithm name
	 * @return boolean - algorithm supported?
	 */
	public static boolean isSupported (String alg) {
		return getAlgId (alg) != -1;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get list index of encryption algorithm (or -1 if the algorithm
	 * is not supported).</p>
	 * @param alg String - algorithm name
	 * @return int - list index of algorithm (or -1)
	 */
	private static int getAlgId (String alg) {
		for (int n = 0; n < algs.length; n++)
			if (algs[n].equals (alg))
				return n;
		return -1;
	}
	
	//=================================================================
	/**
	 * <p>Initialized cipher engine instance.</p>
	 */
	protected Cipher	worker = null;		// worker instance
	protected boolean	forEncryption;		// operating mode
	protected SecretKey	key;				// cipher key
	protected byte[]	iv;					// algorithm specs
	
	//=================================================================
	/**
	 * <p>Instantiate a new channel cipher engine with given algorithm.</p>
	 * @param alg String - algorithm name
	 * @param secret byte[] - shared secret
	 * @param forEncryption boolean - use cipher for encryption?
	 * @throws NoSuchAlgorithmException
	 */
	public ChannelCipher (String alg, byte[] secret, boolean forEncryption)
		throws NoSuchAlgorithmException
	{
		// get algorithm index.
		int idx = getAlgId (alg);
		if (idx < 0)
			throw new NoSuchAlgorithmException (alg);
		
		// get length of secret key
		int keyLength = keySize[idx];
		byte[] keyData = new byte [keyLength];
		System.arraycopy (secret, 0, keyData, 0, keyLength);
		
		// create new cipher instance
		try {
			this.forEncryption = forEncryption;
			worker = Cipher.getInstance (cipherAlgs[idx]);
			key = new SecretKeySpec (keyData, keyAlgs[idx]);
		
			// setup algorithm parameter (IV value) for CBC modes
			AlgorithmParameterSpec spec = null;
			if (cipherAlgs[idx].indexOf("CBC") != -1) {
				int size = ivSize[idx];
				iv = new byte [size];
				for (int n = 0; n < size; n++)
					iv[n] = secret[n + keyLength];
				spec = new IvParameterSpec (iv);
			} else
				iv = null;
			
			// initialize worker
			int mode = forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
			worker.init (mode, key, spec);
		}
		catch (Exception e) {
			// no worker available.
			worker = null;
			iv = null;
		}
	}
	
	//=================================================================
	/**
	 * <p>En-/decrypt a message.</p>
	 * @param data byte[] - message data
	 * @return byte[] - decrypted message 
	 * @throws IllegalBlockSizeException 
	 */
	public byte[] process (byte[] data) {
		int size = data.length;

		try {
			//-------------------------------------------------------------
			// Encryption mode:
			//-------------------------------------------------------------
			if (forEncryption) {
				// check blocking
				int newSize = worker.getOutputSize (size);
				if (newSize > size) {
					// we need some padding.
					byte[] newData = new byte [newSize];
					System.arraycopy (data, 0, newData, 0, size);
					data = newData;
					size = newSize;
				}
			}
			// return ciphered data
			return worker.update (data, 0, size);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

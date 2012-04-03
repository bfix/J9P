
//******************************************************************
//*   PGMID.        P9SK2 AUTHENTICATION PROTOCOL HANDLER.         *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/10.                                      *
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
/**
 *  <p>Implementation of the 'p9sk2' authentication protocol.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class AP_p9sk2 extends AP_p9sk {
	
	//=================================================================
	/**
	 * <p>A (virtual) identity assigned to 'p9any'.</p> 
	 */
	public static class Id extends AP_p9sk.Id {
		//-------------------------------------------------------------
		/**
		 * <p>Get name of authentication protocol.</p>
		 * @return String - name of authentication protocol
		 */
		public String getAuthProtocol()	{ return "p9sk2"; }
	}

	//=================================================================
	//=================================================================
	/**
	 * <p>Hidden constructor: Handler instances are returned by
	 * calling 'AP_Generic.getInstance(...)'.</p>
	 */
	protected AP_p9sk2 () {
		skVersion = 2;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get name of handled authentication protocol.</p>
	 * @return String - name of authentication protocol 
	 */
	public String getProtocolName () {
		return "p9sk2";
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Get associated identity.</p>
	 * <p>When called during an authentication process, the identity
	 * used to initialize the handler is returned. If called during
	 * configuration setup, the identity defined in the configuration
	 * is returned.</p>
	 * @return Identity - assigned/defined identity (or null)
	 */
	public Identity getIdentity () {
		if (id == null && done) {
			// create new identity (configuration)
			id = new Id();
			id.name = user;
			id.password = password;
			id.domain = domain;
		}
		return id;
	}
}

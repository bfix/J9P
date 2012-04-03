
//******************************************************************
//*   PGMID.        AUTHENTICATION ENTITY INTERFACE.               *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/12.                                      *
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

import j9p.io.ContentHandler;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>An <b>Entity</b> abstract base class is implemented by authentication
 * objects like keys and certificates. These objects can be converted to
 * "content" data that is used in authentication messages or Plan9/Inferno
 * keyring files or they can be converted to and from XML configuration data.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class Entity {

	//=================================================================
	//	Convert entity data to message content
	//=================================================================

	/**
	 * <p>Get message content from entity data.</p>
	 * @return String - assemble message content
	 */
	public String toContent () {
		return ContentHandler.createSection (toContentArray());
	}
	
	//-----------------------------------------------------------------
	/**
	 * <p>Get list of content strings from entity data.</p>
	 * @return String[] - list of content strings
	 */
	public abstract String[] toContentArray ();
	
	//=================================================================
	/**
	 * <p> Initialize entity data from message content.</p>
	 * @param content String[] - list of content strings
	 * @return boolean - successful operation?
	 */
	public abstract boolean fromContentArray (String[] content);

	//=================================================================
	/**
	 * <p>Convert entity data to configuration XML data.</p>
	 * @return String - XML encoded entity
	 */
	public abstract String toConfigXML ();
}

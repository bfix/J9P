
//******************************************************************
//*   PGMID.        ABSTRACT STYX CLIENT PROTOCOL.                 *
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


package j9p.client;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.IOException;

import j9p.Channel;
import j9p.auth.Credential;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>The <b>ClientProtocol</b> is rather a high-level interface to
 * namespaces published by a server.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class ClientProtocol {

	public abstract Type.Version negotiateVersion (String version, int msize) throws IOException;
	
	public abstract Credential authenticate (String uname, String aname) throws IOException;
	
	public abstract Type.Qid attach (String user, String aname) throws IOException;
	
	
	protected Channel comm = null;
	protected int nextTag = 1;
	protected int nextFid = 0xBF0001;

	public boolean setChannel (Channel comm) {
		this.comm = comm;
		return true;
	}
	
	protected int getNextTag () {
		return nextTag++;
	}
	
	protected int getNextFid () {
		return nextFid++;
	}
}

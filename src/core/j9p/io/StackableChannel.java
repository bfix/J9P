
//******************************************************************
//*   PGMID.        STACKABLE STYX CHANNEL BASE CLASS.             *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/04/04.                                      *
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

package j9p.io;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import j9p.Channel;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p>A <b>StackableChannel</b> is a cascading wrapper for Styx channels.
 * Used to wrap existing (opened) channels with processing layers (encryption,
 * digests or whatever both peers agree on). Channels are wrapped around
 * existing channels without changing the wrapped instance:</p>
 * <pre>
 * :
 * 	StreamChannel  ch = new StreamChannel (is, os);
 *  Message in = ch.getNextMessage(); // raw read
 *  SecureChannel secure = new SecureChannel (...);
 *  ch.wrapWith (secure);
 *  in = ch.getNextMessage(); // decrypt/check message
 *  ch.peelOff();
 *  in = ch.getNextMessage(); // raw read again
 *  :
 * </pre>
 * <p>A StackableChannel maintains two attributes:</p>
 * <ul>
 * 	<li>'top': For visible channels (like 'ch' in the above example) this
 * 		references the outer-most channel (top-level of stack). For wrapper
 * 		channels this reference is set to 'null'.</li>
 * 	<li>'next': For visible channels the reference is null; for wrapping
 * 		channels the attribute references the next channel inwards.</li>
 * </ul>
 * <p>The effort is worth it, if we are going to implement and use a variety
 * of transportation mediums for 9P.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public abstract class StackableChannel implements Channel {

	//=================================================================
	/*
	 * Constants: nesting mode flags
	 */
	protected static final int	NEST_GET	=  1;
	protected static final int	NEST_PEEK	=  2;
	protected static final int	NEST_SEND	=  4;
	protected static final int	NEST_CLOSE	=  8;

	//=================================================================
	/*
	 * Attributes:
	 */
	protected StackableChannel	top;		// outer instance
	protected StackableChannel	next;		// wrapped instance
	protected int				nestMode;	// nesting mode

	//=================================================================
	/**
	 * <p>Constructor for unlinked StackableChannel. Objects
	 * created with this constructor are not usable directly but 
	 * wrap a functional instances as argument to a 'wrapWith()'
	 * method call.</p> 
	 * <p>Constructors of base handlers (raw data transfer) include
	 * the statement 'top = this;' to initialize the top-of-stack
	 * reference.</p>
	 * <p>If 'top' is not set in an instance, it must be a wrapper
	 * instance around another channel. Calls are delegated down
	 * the stack to the 'next' instance.</p>
	 */
	protected StackableChannel () {
		top = null;
		next = null;
		nestMode = 0;
	}

	//=================================================================
	//  Wrapper methods. 
	//=================================================================
	/**
	 * Wrap this instance of Channel with another instance. The
	 * wrapping instance is exposed to the using side and uses the
	 * wrapped channel as transport mechanism.</p>
	 * @param ch Channel - wrapping channel (new outer layer)
	 * @return boolean - wrapping successful?
	 */
	public boolean wrapWith (Channel ch) {
		
		// chain channels if 'ch' is stackable and no
		// wrapped instance is set.
		if (ch == null || !(ch instanceof StackableChannel))
			return false;
		
		// set references to new outer and inner channels
		StackableChannel outer = (StackableChannel) ch;
		StackableChannel inner = top;
		// link them together
		outer.next = inner;
		top = outer;
		return true;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Peel of Channel layer around this instance.</p> 
	 * @return Channel - former wrapping channel (outer layer).
	 */
	public Channel peelOff () {
		StackableChannel outer = top;
		top = outer.next;
		return outer;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Check if we are a wrapped instance.</p> 
	 * @return boolean - channel is nested
	 */
	public boolean isWrapped () {
		return top == null;
	}

	//=================================================================
	//	Call stack synchronization.
	//=================================================================
	/**
	 * <p>Check if we are traversing the channel stack for given mode.</p>
	 * @param mode int - nesting flag NEST_???
	 * @return boolean - on traversal?
	 */
	protected boolean isNested (int mode) {
		return (nestMode & mode) == mode;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Set nesting flag for given mode.</p> 
	 * @param mode int - nesting flag NEST_???
	 */
	protected void setNesting (int mode) {
		nestMode |= mode;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Clear nesting flag for given mode.</p> 
	 * @param mode int - nesting flag NEST_???
	 */
	protected void clearNesting (int mode) {
		nestMode &= ~mode;
	}
}

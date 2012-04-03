
//******************************************************************
//*   PGMID.        NAMESPACE MANAGEMENT.                          *
//*   AUTHOR.       BERND R. FIX   >Y<                             *
//*   DATE WRITTEN. 09/01/02.                                      *
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

package j9p.server;

///////////////////////////////////////////////////////////////////////////////
//import external declarations.

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import j9p.ns.Permissions;
import j9p.ns.Directory;
import j9p.ns.File;
import j9p.ns.Namespace;
import j9p.ns.Entry;
import j9p.ns.handlers.LogicalDirectory;
import j9p.ns.handlers.LogicalFile;
import j9p.ns.handlers.DiskFile;
import j9p.ns.handlers.Process;
import j9p.ns.handlers.ProcessFile;
import j9p.util.Base64;


///////////////////////////////////////////////////////////////////////////////
/**
 * <p><b>NamespaceManager</b> to handle user-specific namespace definitions.</p>
 * 
 * @author Bernd R. Fix   >Y<
 * @version 1.0
 */
public class NamespaceManager extends DefaultHandler {
	
	//=================================================================
	/*
	 * <p>Default permissions for namespace entries.</p> 
	 */
	public static final int PERM_FILE_DEFAULT =
		  Permissions.PERM_OW | Permissions.PERM_OR
		| Permissions.PERM_GR
		| Permissions.PERM_AR;

	public static final int PERM_DIR_DEFAULT = PERM_FILE_DEFAULT |
		  Permissions.PERM_OX
		| Permissions.PERM_GX
		| Permissions.PERM_AX;

	//=================================================================
	/**
	 * <p>List of user-specific namespaces managed by the handler.</p> 
	 */
	private Hashtable<String,Namespace> userNS = null;
	/**
	 * <p>List of running engines.</p>
	 */
	private Vector<Process> engines = null;

	//=================================================================
	/**
	 * <p>Instantiate new namespace handler with empty default
	 * namespace (empty root directory).</p>
	 */
	public NamespaceManager () {
		// allocate namespace mapping.
		userNS = new Hashtable<String,Namespace>();
		// allocate engine list
		engines = new Vector<Process>();

		// setup empty default namespace
		Permissions perm = new Permissions ("sys", "sys", PERM_DIR_DEFAULT);
		Directory root = new LogicalDirectory ("", perm, "|/");
		userNS.put ("*", new Namespace (root));
	}
	
	//=================================================================
	/**
	 * <p>Add user-specific namespace to handler.</p>
	 * @param user String - name of namespace user
	 * @param ns Namespace - namespace definition
	 * @return boolean - successful operation?
	 */
	public boolean addNamespace (String user, Namespace ns) {
		
		// allow redefinition of default namespace
		if (!user.equals ("*") && userNS.containsKey (user))
			return false;
		// save user-specific namespace
		userNS.put (user, ns);
		return true;
	}
	
	//=================================================================
	/**
	 * <p>Lookup user-specific namespace. If no specific
	 * namespace is found, the (empty) default namespace
	 * is returned.</p>
	 * @param user String - name of namespace user
	 * @return Namespace - user-specific (or default) namespace
	 */
	public Namespace getUserNS (String user) {
		Namespace ns = userNS.get (user);
		if (ns == null)
			return userNS.get ("*");
		return ns;
	}
	
	//=================================================================
	/**
	 * <p>Shutdown manager and terminate all running engines.</p> 
	 */
	public void shutdown () {
		
		// shutdown all engines.
		int count = engines.size();
		for (int n = 0; n < count; n++) {
			Process eng = engines.elementAt (n);
			eng.terminate();
		}
	}
	
	//#################################################################
	// Handling of XML configuration file.
	//#################################################################

	/*
	 * XML parse state attributes:
	 */
	private Hashtable<String,Class<?>> handlers = null;
	private String device = null;			// device (kernel only)
	private String user = null;				// associated user
	private Directory root = null;			// root directory
	private Stack<Directory> tree = null;	// directory stack
	private File file = null;				// current file instance 
	private StringBuffer pcData = null;		// PCDATA between tags
	
	//=================================================================
	/**
	 * <p>Read namespace definitions from XML configuration file.</p> 
	 * @param fname String - name of namespace definition file
	 * @return boolean - successful operation?
	 */
	public boolean readConfig (String fname) {

		// initialize XML state attributes
		handlers = new Hashtable<String,Class<?>>();
		
		// use some predefined handlers.
		handlers.put ("logDir",	  LogicalDirectory.class);
		handlers.put ("logFile",  LogicalFile.class);
		handlers.put ("diskFile", DiskFile.class);
		handlers.put ("procFile", ProcessFile.class);

		// parse configuration file.
		try {
			// create reader on configuration file
			InputSource src = new InputSource (new FileInputStream (fname));
			XMLReader rdr = XMLReaderFactory.createXMLReader();
			rdr.setContentHandler (this);
			rdr.parse (src);
		}
		catch (Exception e) {
			System.out.println ("ReadConfig exception: " + e.getMessage());
			return false;
		}
		// report success.
		return true;
	}
	
	//=================================================================
	/**
	 * <p>Start of new Element in XML.</p>
	 * @param namespaceURI String -
	 * @param localName String - local name
	 * @param qName String - qualified name
	 * @param attrs Attributes - element attributes
	 */
	public void startElement (String namespaceURI, String localName,
            				  String qName, Attributes attrs
	) throws SAXException {
		// get entity name
		String name = ( "".equals( localName ) ) ? qName : localName;
		
		//-------------------------------------------------------------
		// <EntryType ...>: Handler definition
		//-------------------------------------------------------------
		if (name.equals ("EntryType")) {
			// get attribute values.
			String hdlrName = attrs.getValue ("name");
			String hdlrClassName = attrs.getValue ("class");
			try {
				// get access to handler class
				Class<?> hdlrClass = Class.forName (hdlrClassName);
				// store handler.
				handlers.put (hdlrName, hdlrClass);
				System.out.println ("Mapping '" + hdlrName + "' to class '" + hdlrClassName + "'");
			}
			catch (ClassNotFoundException exc) {
				System.err.println ("Can't access class " + hdlrClassName);
			}
			return;
		}
		//-------------------------------------------------------------
		// <Namespace ...>:  namespace definition
		//-------------------------------------------------------------
		if (name.equals ("Namespace")) {
			// save user name for later use.
			user = attrs.getValue ("user");
			// preset device 
			device = attrs.getValue ("device");
			// initialize directory references
			root = null;
			tree = new Stack<Directory>();
			return;
		}
		//-------------------------------------------------------------
		// <Directory ...>:  directory definition
		// <... type="logDir" name="" uid="inferno" gid="sys" mode="755" dev="#/">
		//-------------------------------------------------------------
		if (name.equals ("Directory")) {
			
			// get reference to parent directory
			Directory parent = null;
			boolean isRoot = (tree.size() == 0);
			if (!isRoot)
				parent = tree.peek();
			
			// instantiate a new handler
			String type = attrs.getValue ("type");
			Directory dir = (Directory) instantiateHandler (type);
			if (dir == null)
				throw new SAXException ("Entry handler for type '" + type + "' not found or failed.");

			// try to run handler process in separate thread.
			Hashtable<String,String> params = collectParams (attrs);
			Process proc = dir.startHandler (params);
			if (proc != null)
				// keep in list of started threads.
				engines.add (proc);

			// assemble permissions
			Permissions perm = assemblePermissions (attrs, parent);
			if (perm == null)
				throw new SAXException ("Root directory must specify permissions!");
			
			// initialize namespace entry
			String label = attrs.getValue ("name");
			if (isRoot)
				label = "/";
			else if (label == null)
				throw new SAXException ("Un-named directory!");
			dir.init (label, perm, device);
			
			// handle relationship.
			if (parent == null)
				root = dir;
			else
				parent.add (dir);

			// save reference to current directory
			tree.push (dir);
			return;
		}
		//-------------------------------------------------------------
		// <File ...>:  file definition
		//-------------------------------------------------------------
		if (name.equals ("File")) {
			
			// get reference to parent directory
			if (tree.size() == 0)
				throw new SAXException ("No working directory");
			Directory parent = tree.peek();
			
			// instantiate a new handler
			String type = attrs.getValue ("type");
			file = (File) instantiateHandler (type);
			if (file == null)
				throw new SAXException ("Entry handler for type '" + type + "' not found or failed.");
			
			// try to run handler process in separate thread.
			Hashtable<String,String> params = collectParams (attrs);
			Process proc = file.startHandler (params);
			if (proc != null)
				// keep in list of started threads.
				engines.add (proc);

			// assemble permissions
			Permissions perm = assemblePermissions (attrs, parent);
			if (perm == null)
				throw new SAXException ("Root directory must specify permissions!");
			
			// initialize namespace entry
			String label = attrs.getValue ("name");
			if (label == null)
				throw new SAXException ("Un-named file!");
			file.init (label, perm, device);
			
			// handle relationship.
			parent.add (file);
			
			// start parsing of PCDATA for logical files
			if (file instanceof LogicalFile) {
				pcData = new StringBuffer();
			}
			return;
		}
	}

	//=================================================================
	/**
	 * <p>Start of new Element in XML.</p>
	 * @param namespaceURI String -
	 * @param localName String - local name
	 * @param qName String - qualified name
	 */
	public void endElement (String namespaceURI, String localName, String qName)
	throws SAXException {
		// get entity name
		String name = ( "".equals( localName ) ) ? qName : localName;
		
		//-------------------------------------------------------------
		// </Namespace>
		//-------------------------------------------------------------
		if (name.equals ("Namespace")) {
			// create a new namespace from root directory
			// and associate it with user name.
			userNS.put (user, new Namespace (root));
			System.out.println ("   Created namespace for user '" + user + "'");
			return;
		}
		//-------------------------------------------------------------
		// </Directory>
		//-------------------------------------------------------------
		if (name.equals ("Directory")) {
			// we are done for this directory - pop stack
			tree.pop();
		}
		//-------------------------------------------------------------
		// </File>
		//-------------------------------------------------------------
		if (name.equals ("File")) {
			// if we are a logical file definition...
			if (file instanceof LogicalFile) {
				// convert PCDATA to byte array.
				byte[] content = Base64.toArray (pcData.toString());
				// prepare the entry with provided data
				((LogicalFile) file).setContent (content);
				// close PCDATA buffer
				pcData = null;
			}
			return;
		}
	}
	
	//=================================================================
	/**
	 * <p>Collect PCDATA between tags.</p>
	 * @param ch char[] - character array of data
	 * @param start int - offset into array
	 * @param length int - length of data 
	 */
	public void characters (char[] ch, int start, int length) {
		// are we collecting PCDATA?
		if (pcData == null)
			return;
		// append new data
		pcData.append (ch, start, length);
	}
	
	//=================================================================
	//	XML parser helper methods:
	//=================================================================

	/**
	 * <p>Assemble permissions from element attributes. Inherit
	 * missing values from parent.</p>
	 * @param attrs Attributes - XML element attributes
	 * @param parent Directory - parent directory
	 * @return Permissions - assembled permissions
	 */
	private Permissions assemblePermissions (Attributes attrs, Directory parent) {
		
		// parse permissions
		String uid = attrs.getValue ("uid");
		String gid = attrs.getValue ("gid");
		String modeStr = attrs.getValue ("mode");
		int mode = (modeStr != null ? Integer.parseInt (modeStr, 8) : 0);
		
		// missing values?
		if (uid == null || gid == null || mode == 0) {
			// parent directory should specify values
			if (parent == null)
				return null;

			// inherit values from parent
			Permissions parentPerm = parent.getPermissions();
			if (uid == null) uid = parentPerm.getOwner();
			if (gid == null) gid = parentPerm.getGroup();
			if (mode == 0) mode = parentPerm.getMode();
		}
		return new Permissions (uid, gid, mode);
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Instantiate a new entry handler of given type.</p> 
	 * @param type String - type name
	 * @return Entry - entry handler (or null)
	 */
	private Entry instantiateHandler (String type) {
		if (type == null)
			return null;
		// instantiate a new handler
		Entry hdlr = null;
		Class<?> hdlrClass = handlers.get (type);
		if (hdlrClass == null) {
			System.err.println ("No class found for type '" + type + "'");
			return null;
		}
		try {
			hdlr = (Entry) hdlrClass.newInstance();
		} catch (Exception e) {
			System.err.println (e.getMessage());
			return null;
		} 
		return hdlr;
	}
	//-----------------------------------------------------------------
	/**
	 * <p>Collect attributes as parameters to handler.</p> 
	 * @param attrs Attributes - XML element attributes
	 * @return Hashtable<String,String> - list of parameters
	 */
	private Hashtable<String,String> collectParams (Attributes attrs) {
		// collect attributes.
		Hashtable<String,String> params = new Hashtable<String,String>();
		int count = attrs.getLength();
		for (int n = 0; n < count; n++) {
			String key = attrs.getQName (n);
			String val = attrs.getValue (n);
			params.put (key, val);
		}
		return params;
	}
}

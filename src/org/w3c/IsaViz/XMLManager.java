/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 10/22/2001
 */


package org.w3c.IsaViz;

import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.io.File;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.framework.Version;
import org.w3c.dom.Document;
import org.xml.sax.*;

/*in charge of loading and parsing misc. XML files (for instance SVG and ISV project files)*/

class XMLManager {

    Editor application;

    DOMParser parser;

    XMLManager(Editor e){
	application=e;
    }

    Document parse(String xmlFile,boolean validation) {
	try {
	    parser = new DOMParser();
	    parser.setFeature("http://xml.org/sax/features/validation",validation);
	    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",validation);  //if true, the external DTD will be loaded even if validation was set to false
	    parser.setFeature("http://apache.org/xml/features/dom/include-ignorable-whitespace",false);
	    try {
		parser.parse(xmlFile);
	    } 
	    catch (SAXException se) {
		application.errorMessages.append("XMLManager.parse("+xmlFile+"): "+se+"\n");
		javax.swing.JOptionPane.showMessageDialog(application.cmp,Messages.incompleteParsing+xmlFile);
		application.reportError=true;
		//se.printStackTrace();
	    } 
	    catch (IOException ioe) {
		application.errorMessages.append("XMLManager.parse("+xmlFile+"): "+ioe+"\n");
		javax.swing.JOptionPane.showMessageDialog(application.cmp,Messages.incompleteParsing+xmlFile);
		application.reportError=true;
		//ioe.printStackTrace();
	    }
	    Document document = parser.getDocument();
	    document.normalize();
	    return document;
	}
	catch (Exception e){
	    application.errorMessages.append("XMLManager.parse("+xmlFile+"): "+e+"\n");
	    javax.swing.JOptionPane.showMessageDialog(application.cmp,Messages.incompleteParsing+xmlFile);
	    application.reportError=true;
	    return null;
	}
    }

    void serialize(Document d,File f){
	if (f!=null && d!=null){
	    //serialize a DOM instance 
	    org.apache.xml.serialize.XMLSerializer ser=new org.apache.xml.serialize.XMLSerializer();   
	    try {
		ser.setOutputCharStream(new java.io.FileWriter(f.toString()));
		ser.setOutputFormat(new org.apache.xml.serialize.OutputFormat(d,"UTF-8",true));
		ser.serialize(d);
	    }
	    catch (java.io.IOException ioe){ioe.printStackTrace();}
	}
    }
    
}

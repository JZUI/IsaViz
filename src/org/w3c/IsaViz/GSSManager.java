/*   FILE: GSSManager.java
 *   DATE OF CREATION:   Fri Mar 14 09:37:24 2003
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Tue Apr 01 17:41:14 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */ 

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 


package org.w3c.IsaViz;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.awt.Color;

import com.xerox.VTM.engine.SwingWorker;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.datatypes.RDFDatatype;

class GSSManager {

    static File lastStyleDir=null;

    Editor application;

    Hashtable stylesheetFiles;   //key=java.io.File or java.net.URL where the stylesheet comes from; value=corresponding GraphStylesheet

    /*
      rule hashtable informal mapping
      _gssVisibility -> Integer (GraphStylesheet.SHOW || GraphStylesheet.HIDE)

      _gssLayout -> Integer (GraphStylesheet.TABLE_FORM || GraphStylesheet.NODE_EDGE)

      _gssFill -> Color
      _gssStroke -> Color
      _gssStrokeWidth -> Float (positive)
      _gssFontFamily -> String
      _gssFontSize -> Integer  (positive)
      _gssFontWeight -> Short  (one of Style.CSS_FONT_WEIGHT*)
      _gssFontStyle -> Short  (one of Style.CSS_FONT_STYLE*)
      _gssShape -> Integer (one of Style.{ELLIPSE,RECTANGLE,CIRCLE,DIAMOND,OCTAGON,TRIANGLEN,TRIANGLES,TRIANGLEE,TRIANGLEW})
                       || CustomShape
    */

    /*temporarily maps actual RDF resources to the final style/visibility/layout that should be applied to them*/
    Hashtable resource2styleTable;
    /*temporarily maps actual RDF properties to the final style/visibility/layout that should be applied to them*/
    Hashtable property2styleTable;
    /*temporarily maps actual RDF literals to the final style/visibility/layout that should be applied to them*/
    Hashtable literal2styleTable;
    /*stylesheets in reverse order of application (last one to be applied first in the array)*/
    GraphStylesheet[] stylesheets;


    /*returns the rdf:type of a resource, if it exists, null otherwise*/
    public static String getType(Resource r){
	String res=null;
	StmtIterator it=r.listProperties();
	Statement st;
	while (it.hasNext()){
	    st=it.nextStatement();
	    if (st.getPredicate().getURI().equals(GraphStylesheet._rdfType)){
		Object o=st.getObject();
		if (o!=null){res=o.toString();}
		break;
	    }
	}
	it.close();
	return res;
    }

    /*returns the rdf:type of a resource, if it exists, null otherwise*/
    public static String getType(IResource r){
	String res=null;
	Vector outgoingPredicates=r.getOutgoingPredicates();
	if (outgoingPredicates!=null){
	    IProperty p;
	    for (int i=0;i<outgoingPredicates.size();i++){
		p=(IProperty)outgoingPredicates.elementAt(i);
		if (p.getIdent().equals(GraphStylesheet._rdfType)){
		    INode n=p.getObject();
		    if (n instanceof IResource){res=((IResource)n).getIdent();break;} //break only if everything goes fine
		    else if (n instanceof ILiteral){res=((ILiteral)n).getValue();break;} //otherwise try to find another statement defining an rdf:type
		}//although there is little chance this is going to be the case, try it (for robustness)
	    }
	}
 	return res;
    }

    GSSManager(Editor app){
	application=app;
	stylesheetFiles=new Hashtable();
    }

    void reset(){
	stylesheetFiles.clear();
    }
    
    /*load graph stylesheet (do not apply it)*/
    public void loadStylesheet(final File f){
	lastStyleDir=f.getParentFile();
	final SwingWorker worker=new SwingWorker(){
		public Object construct(){
		    application.tblp.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
		    GraphStylesheet gss=new GraphStylesheet();
		    gss.load(f,application.isvMngr.application);
		    if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
		    stylesheetFiles.put(f,gss);
		    application.tblp.addStylesheet(f);
		    application.tblp.setCursor(java.awt.Cursor.getDefaultCursor());
		    return null; 
		}
	    };
	worker.start();
    }

    /*load graph stylesheet (do not apply it)*/
    public void loadStylesheet(final java.net.URL url){
	final SwingWorker worker=new SwingWorker(){
		public Object construct(){
		    application.tblp.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
		    GraphStylesheet gss=new GraphStylesheet();
		    gss.load(url,application.isvMngr.application);
		    if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
		    stylesheetFiles.put(url,gss);
		    application.tblp.addStylesheet(url);
		    application.tblp.setCursor(java.awt.Cursor.getDefaultCursor());
		    return null; 
		}
	    };
	worker.start();
    }

    void removeSelectedStylesheet(){
	Object o=application.tblp.removeSelectedStylesheet();
	if (o!=null){
	    stylesheetFiles.remove(o);
	}
    }

    void editSelectedStylesheet(){
	System.out.println("Sorry, the Graph Stylesheet Editor is not implemented yet");
    }

    /*returns the list of stylesheets (GraphStylesheet objects) in their order of application (empty vector if none)*/
    Vector getStylesheetList(){
	Vector v=application.tblp.getStylesheetList();
	Vector res=new Vector();
	for (int i=0;i<v.size();i++){
	    res.addElement(stylesheetFiles.get(v.elementAt(i)));
	}
	return res;
    }

    void applyStylesheets(){//to current model
	Vector list=getStylesheetList();
	if (list.size()>0){//this will be checked again later by RDFLoader.loadAndStyle, but we do it here to prevent 
	    final SwingWorker worker=new SwingWorker(){//exporting and then importing the current model if there is no point (i.e. no stylesheet to apply)
		    public Object construct(){
			Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
			//generate Jena model for current model
			application.generateJenaModel();
			//serialize this model in a temporary file (RDF/XML)
			File tmpF=Utils.createTempFile(Editor.m_TmpDir.toString(),"mrg",".rdf");
			boolean wasAbbrevSyntax=Editor.ABBREV_SYNTAX; //serialize using
			Editor.ABBREV_SYNTAX=true;                    //abbreviated syntax
			application.rdfLdr.save(application.rdfModel,tmpF);                   //but restore user prefs after
			if (!wasAbbrevSyntax){Editor.ABBREV_SYNTAX=false;}//we are done
			//import this file
			application.reset(false);   //do not reset NS bindings
			//tmp file is generated as RDF/XML
			application.rdfLdr.loadAndStyle(tmpF,RDFLoader.RDF_XML_READER);
			if (Editor.dltOnExit && tmpF!=null){tmpF.deleteOnExit();}
			application.updatePrefixBindingsInGraph();
			Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
			return null; 
		    }
		};
	    worker.start();
	}
    }

    protected void initStyleTables(){
	Vector list=getStylesheetList();
	stylesheets=new GraphStylesheet[list.size()];
	for (int i=list.size()-1;i>=0;i--){
	    stylesheets[list.size()-1-i]=(GraphStylesheet)list.elementAt(i);
	}
	resource2styleTable=new Hashtable();
	property2styleTable=new Hashtable();
	literal2styleTable=new Hashtable();
    }

    protected void cleanStyleTables(){
	resource2styleTable.clear();
	property2styleTable.clear();
	literal2styleTable.clear();
	resource2styleTable=null;
	property2styleTable=null;
	literal2styleTable=null;
    }

    StyleInfoR getStyle(IResource r){
	return (StyleInfoR)resource2styleTable.get(r);
    }

    StyleInfoR computeAndGetStyle(IResource r){
	StyleInfoR res;
	if (resource2styleTable.containsKey(r)){
	    res=(StyleInfoR)resource2styleTable.get(r);
	}
	else {
	    res=new StyleInfoR();
	    resource2styleTable.put(r,res);
	    Vector matchingRules;
	    Object selector;
	    Vector styleIDs;
	    for (int i=0;i<stylesheets.length;i++){
		matchingRules=stylesheets[i].evaluateRules(r);
		for (int j=0;j<matchingRules.size();j++){
		    selector=matchingRules.elementAt(j);
		    if (stylesheets[i].rStyleRules.containsKey(selector)){
			styleIDs=(Vector)stylesheets[i].rStyleRules.get(selector);
			for (int k=0;k<styleIDs.size();k++){
			    res.applyStyle((Style)stylesheets[i].styles.get(styleIDs.elementAt(k)));
			}
		    }
		    res.applyLayout((Integer)stylesheets[i].rLayoutRules.get(selector));
		    res.applyVisibility((Integer)stylesheets[i].rVisRules.get(selector));
		    /*just not to loose time evaluating rules that won't have any effect
		      test that shape is specified because it can influence the layout even though it is not visible, 
		      and we do not want to stop processing potential rules in case one changes the shape
		    */
		    if (res.isFullySpecified() || res.isDisplayNone() || res.isVisibilityHiddenAndShapeSpecified()){return res;}
		}
	    }
	}
	return res;
    }

    StyleInfoP getStyle(IProperty p){
	return (StyleInfoP)property2styleTable.get(p);
    }

    StyleInfoP computeAndGetStyle(IProperty p){
	StyleInfoP res;
	if (property2styleTable.containsKey(p)){
	    res=(StyleInfoP)property2styleTable.get(p);
	}
	else {
	    res=new StyleInfoP();
	    property2styleTable.put(p,res);
	    Vector matchingRules;
	    Object selector;
	    Vector styleIDs;
	    for (int i=0;i<stylesheets.length;i++){
		matchingRules=stylesheets[i].evaluateRules(p);
		for (int j=0;j<matchingRules.size();j++){
		    selector=matchingRules.elementAt(j);
		    if (stylesheets[i].pStyleRules.containsKey(selector)){
			styleIDs=(Vector)stylesheets[i].pStyleRules.get(selector);
			for (int k=0;k<styleIDs.size();k++){
			    res.applyStyle((Style)stylesheets[i].styles.get(styleIDs.elementAt(k)));
			}
		    }
		    res.applyLayout((Integer)stylesheets[i].pLayoutRules.get(selector));
		    res.applyVisibility((Integer)stylesheets[i].pVisRules.get(selector));
		    /*just not to loose time evaluating rules that won't have any effect
		      test that shape is specified because it can influence the layout even though it is not visible, 
		      and we do not want to stop processing potential rules in case one changes the shape
		    */
		    if (res.isFullySpecified() || res.isDisplayNone() || res.isVisibilityHidden()){return res;}
		}
	    }
	}
	return res;
    }

    StyleInfoL getStyle(ILiteral l){
	return (StyleInfoL)literal2styleTable.get(l);
    }

    StyleInfoL computeAndGetStyle(ILiteral l){
	StyleInfoL res;
	if (literal2styleTable.containsKey(l)){
	    res=(StyleInfoL)literal2styleTable.get(l);
	}
	else {
	    res=new StyleInfoL();
	    literal2styleTable.put(l,res);
	    Vector matchingRules;
	    Object selector;
	    Vector styleIDs;
	    for (int i=0;i<stylesheets.length;i++){
		matchingRules=stylesheets[i].evaluateRules(l);
		for (int j=0;j<matchingRules.size();j++){
		    selector=matchingRules.elementAt(j);
		    if (stylesheets[i].lStyleRules.containsKey(selector)){
			styleIDs=(Vector)stylesheets[i].lStyleRules.get(selector);
			for (int k=0;k<styleIDs.size();k++){
			    res.applyStyle((Style)stylesheets[i].styles.get(styleIDs.elementAt(k)));
			}
		    }
		    res.applyLayout((Integer)stylesheets[i].lLayoutRules.get(selector));
		    res.applyVisibility((Integer)stylesheets[i].lVisRules.get(selector));
		    /*just not to loose time evaluating rules that won't have any effect
		      test that shape is specified because it can influence the layout even though it is not visible, 
		      and we do not want to stop processing potential rules in case one changes the shape
		    */
		    if (res.isFullySpecified() || res.isDisplayNone() || res.isVisibilityHiddenAndShapeSpecified()){return res;}
		}
	    }
	}
	return res;
    }


}

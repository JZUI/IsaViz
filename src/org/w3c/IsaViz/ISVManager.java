/*   FILE: ISVManager.java
 *   DATE OF CREATION:   12/24/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Feb 12 12:02:16 2003 by Emmanuel Pietriga
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
import javax.swing.table.DefaultTableModel;

import com.xerox.VTM.engine.View;
import com.xerox.VTM.engine.VirtualSpace;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.VEllipse;
import com.xerox.VTM.glyphs.VRectangle;
import com.xerox.VTM.glyphs.VTriangleOr;
import com.xerox.VTM.glyphs.VPath;
import com.xerox.VTM.glyphs.VClippedPath;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.DOMImplementationImpl;
import org.w3c.dom.*;

/*methods related to ISV file format*/


class ISVManager {

    Editor application;

    /*Temporary data structures used to modelize the graph structure when parsing/serializing ISV project files*/
    Hashtable uniqueIDs2INodes;  //only used when loading a projet file to 
                                 //modelize the graph structure in the XML file
    Hashtable inodes2UniqueIDs;  //only used when saving a projet file to 
    StringBuffer nextUniqueID;   //modelize the graph structure in the XML file
    
    ISVManager(Editor e){
	this.application=e;
    }

    /*open an ISV project file*/
    public void openProject(File f){
	ProgPanel pp=new ProgPanel("Resetting...","Loading ISV");
	application.reset();
	Editor.lastOpenPrjDir=f.getParentFile();
	pp.setPBValue(10);
	pp.setLabel("Loading file "+f.toString()+" ...");
	Editor.projectFile=f;
	Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Loading project to "+f.toString()+" ...");
	uniqueIDs2INodes=new Hashtable();
	pp.setPBValue(20);
	pp.setLabel("Parsing...");
 	try {
	    Document d=application.xmlMngr.parse(f.toString(),true);
	    d.normalize();
	    Element rt=d.getDocumentElement();
	    NodeList nl;
	    //namespace bindings
	    pp.setPBValue(30);
	    pp.setLabel("Processing namespace bindings...");
	    if (rt.getElementsByTagNameNS(Editor.isavizURI,"nsBindings").getLength()>0){
		nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"nsBindings")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"nsBinding");
		Element tmpEl;
		for (int i=0;i<nl.getLength();i++){
		    tmpEl=(Element)nl.item(i);
		    application.addNamespaceBinding(tmpEl.getAttribute("prefix"),tmpEl.getAttribute("uri"),new Boolean(tmpEl.getAttribute("dispPrefix")),true,true);
		}
	    }
	    //property types
	    pp.setPBValue(40);
	    pp.setLabel("Processing property types...");
	    if (rt.getElementsByTagNameNS(Editor.isavizURI,"propertyTypes").getLength()>0){
		nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"propertyTypes")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"propertyType");
		Element tmpEl;
		for (int i=0;i<nl.getLength();i++){
		    tmpEl=(Element)nl.item(i);
		    application.addPropertyType(tmpEl.getAttribute("ns"),tmpEl.getAttribute("name"),true);
		}
	    }
	    //resources
	    pp.setPBValue(50);
	    pp.setLabel("Processing resources...");
	    if (rt.getElementsByTagNameNS(Editor.isavizURI,"resources").getLength()>0){
		nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"resources")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"iresource");
		for (int i=0;i<nl.getLength();i++){
		    createIResourceFromISV((Element)nl.item(i));
		}
	    }
	    //literals
	    pp.setPBValue(60);
	    pp.setLabel("Processing literals...");
	    if (rt.getElementsByTagNameNS(Editor.isavizURI,"literals").getLength()>0){
		nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"literals")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"iliteral");
		for (int i=0;i<nl.getLength();i++){
		    createILiteralFromISV((Element)nl.item(i));
		}
	    }
	    //properties
	    pp.setPBValue(80);
	    pp.setLabel("Processing properties...");
	    if (rt.getElementsByTagNameNS(Editor.isavizURI,"properties").getLength()>0){
		nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"properties")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"iproperty");
		for (int i=0;i<nl.getLength();i++){//statements are added to the model at this time, since we have a bijection between statements and iproperty(s) and since everything else has been created (meaning also that iproperty should always be created after literals and resources)
		    createIPropertyFromISV((Element)nl.item(i));
		}
	    }
	    pp.setLabel("Building graphical representation...");
	    pp.setPBValue(100);
	    ConfigManager.assignColorsToGraph();
	    application.showAnonIds(Editor.SHOW_ANON_ID);  //show/hide IDs of anonymous resources
	    application.showResourceLabels(Editor.DISP_AS_LABEL);
	    uniqueIDs2INodes=null;
	    Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),ConfigManager.ANIM_DURATION);
	    application.centerRadarView();
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Loading project to "+f.toString()+" ...done");
	    
	}
 	catch (Exception ex){application.errorMessages.append("An error occured while loading file "+f+"\nThis might not be a valid ISV project file.\n"+ex);application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
	pp.destroy();
    }

    /*save an ISV project file*/
    public void saveProject(File f){
	Editor.projectFile=f;
	Editor.lastSavePrjDir=f.getParentFile();
	Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Saving project to "+f.toString()+" ...");
	DOMImplementation di=new DOMImplementationImpl();
	//DocumentType dtd=di.createDocumentType("isv:project",null,"isv.dtd");
	Document prj=di.createDocument(Editor.isavizURI,"isv:project",null);
	//generate the XML document
	Element rt=prj.getDocumentElement();
	rt.setAttribute("xmlns:isv",Editor.isavizURI);
	//namespace bindings
	Element bindings=prj.createElementNS(Editor.isavizURI,"isv:nsBindings");
	rt.appendChild(bindings);
	Element aBinding;
	for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
	    if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0 && ((String)application.tblp.nsTableModel.getValueAt(i,1)).length()>0){
		aBinding=prj.createElementNS(Editor.isavizURI,"isv:nsBinding");
		aBinding.setAttribute("prefix",(String)application.tblp.nsTableModel.getValueAt(i,0));
		aBinding.setAttribute("uri",(String)application.tblp.nsTableModel.getValueAt(i,1));
		aBinding.setAttribute("dispPrefix",((Boolean)application.tblp.nsTableModel.getValueAt(i,2)).toString());
		bindings.appendChild(aBinding);
	    }
	}
	Element propTypes=prj.createElementNS(Editor.isavizURI,"isv:propertyTypes");
	rt.appendChild(propTypes);
	Element aPropType;
	DefaultTableModel tm=(DefaultTableModel)application.tblp.prTable.getModel();
	for (int i=0;i<tm.getRowCount();i++){
	    aPropType=prj.createElementNS(Editor.isavizURI,"isv:propertyType");
	    aPropType.setAttribute("ns",(String)tm.getValueAt(i,0));
	    aPropType.setAttribute("name",(String)tm.getValueAt(i,2));
	    propTypes.appendChild(aPropType);
	}
	//initialize table of unique IDs to save in the XML project file (just for nodes of the graph, not edges)
	inodes2UniqueIDs=new Hashtable();
	nextUniqueID=new StringBuffer("0");
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    inodes2UniqueIDs.put(e.nextElement(),nextUniqueID.toString());
	    incPrjID();
	}
	for (Enumeration e=application.literals.elements();e.hasMoreElements();){
	    inodes2UniqueIDs.put(e.nextElement(),nextUniqueID.toString());
	    incPrjID();
	}
	//resources
	Element ress=prj.createElementNS(Editor.isavizURI,"isv:resources");
	rt.appendChild(ress);
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    ress.appendChild(((IResource)e.nextElement()).toISV(prj,this));
	}
	//literals
	Element lits=prj.createElementNS(Editor.isavizURI,"isv:literals");
	rt.appendChild(lits);
	for (Enumeration e=application.literals.elements();e.hasMoreElements();){
	    lits.appendChild(((ILiteral)e.nextElement()).toISV(prj,this));
	}
	//properties
	Element props=prj.createElementNS(Editor.isavizURI,"isv:properties");
	rt.appendChild(props);
	Vector v;
	for (Enumeration e=application.propertiesByURI.elements();e.hasMoreElements();){
	    v=(Vector)e.nextElement();
	    for (Enumeration e2=v.elements();e2.hasMoreElements();){
		props.appendChild(((IProperty)e2.nextElement()).toISV(prj,this));
	    }
	}
	inodes2UniqueIDs=null; //do not need it any longer
	application.xmlMngr.serialize(prj,f);
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Saving project to "+f.toString()+" ...done");
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	Editor.vsm.getView(Editor.mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
    }

    /*given an INode, get its unique project ID (used when loading ISV projects)*/
    protected String getPrjId(Object n){//should be an INode but we do not cast it for efficiency reasons
	return (String)inodes2UniqueIDs.get(n);
    }

    //generate unique IDs to encode the graph structure in ISV project files (used when saving ISV projects)
    private void incPrjID(){
	boolean done=false;
	for (int i=0;i<nextUniqueID.length();i++){
	    byte b=(byte)nextUniqueID.charAt(i);
	    if (b<0x7a){
		nextUniqueID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextUniqueID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextUniqueID.length();i++){nextUniqueID.setCharAt(i,'0');}
	    nextUniqueID.append('0');
	}
    }


    /*create a new resource from ISV projetc file*/
    IResource createIResourceFromISV(Element e){
	IResource res=null;
	long x=(new Long(e.getAttribute("x"))).longValue();
	long y=(new Long(e.getAttribute("y"))).longValue();
	long w=(new Long(e.getAttribute("w"))).longValue();
	long h=(new Long(e.getAttribute("h"))).longValue();
	VEllipse r=new VEllipse(x,y,0,w,h,ConfigManager.resourceColorF);
	boolean anonRes=false;
	if (e.hasAttribute("isAnon")){anonRes=(new Boolean(e.getAttribute("isAnon"))).booleanValue();}
	Editor.vsm.addGlyph(r,Editor.mainVirtualSpace);
	NodeList nl=e.getElementsByTagNameNS(Editor.isavizURI,"URIorID");
	if (nl.getLength()>0){
	    Element e2=(Element)nl.item(0);
	    long xt=(new Long(e2.getAttribute("x"))).longValue();
	    long yt=(new Long(e2.getAttribute("y"))).longValue();
	    if (anonRes){
		String anID=null;
		if (e2.getElementsByTagNameNS(Editor.isavizURI,"anonID").getLength()>0){
		    anID=Editor.ANON_NODE+e2.getElementsByTagNameNS(Editor.isavizURI,"anonID").item(0).getFirstChild().getNodeValue();
		}
		res=application.addAnonymousResource(anID);
	    }
	    else {
		String ns;
		String ln;
		try {
		    ns=e2.getElementsByTagNameNS(Editor.isavizURI,"namespace").item(0).getFirstChild().getNodeValue();
		}
		catch (NullPointerException ex){ns=Editor.BASE_URI;}
		try {
		    ln=e2.getElementsByTagNameNS(Editor.isavizURI,"localname").item(0).getFirstChild().getNodeValue();
		}
		catch (NullPointerException ex){ln="";}
		res=application.addResource(ns,ln);  //create IResource and add to internal model
	    }
	    VText t=new VText(xt,yt,0,ConfigManager.resourceColorTB,res.getIdent());
	    Editor.vsm.addGlyph(t,Editor.mainVirtualSpace);
	    res.setGlyphText(t);
	}
	else {res=new IResource();}
	res.setGlyph(r);
	uniqueIDs2INodes.put(e.getAttribute("id"),res);
	if (e.hasAttribute("commented")){//is node commented out?
	    boolean b=(new Boolean(e.getAttribute("commented"))).booleanValue();
	    if (b){application.commentNode(res,true);}
	}
	return res;
    }

    /*create a new literal from ISV project file*/
    ILiteral createILiteralFromISV(Element e){
	ILiteral res=null;
	long x=(new Long(e.getAttribute("x"))).longValue();
	long y=(new Long(e.getAttribute("y"))).longValue();
	long w=(new Long(e.getAttribute("w"))).longValue();
	long h=(new Long(e.getAttribute("h"))).longValue();
	VRectangle r=new VRectangle(x,y,0,w,h,ConfigManager.literalColorF);
	Editor.vsm.addGlyph(r,Editor.mainVirtualSpace);
	boolean escapeXML=true;
	if (e.hasAttribute("escapeXML")){
	    escapeXML=(new Boolean(e.getAttribute("escapeXML"))).booleanValue();
	}
	NodeList nl=e.getElementsByTagNameNS(Editor.isavizURI,"value");
	if (nl.getLength()>0){
	    Element e2=(Element)nl.item(0);
	    long xt=(new Long(e2.getAttribute("x"))).longValue();
	    long yt=(new Long(e2.getAttribute("y"))).longValue();
	    String value=e2.getFirstChild().getNodeValue();
	    String displayedValue=((value.length()>=Editor.MAX_LIT_CHAR_COUNT) ? value.substring(0,Editor.MAX_LIT_CHAR_COUNT)+" ..." : value);
	    VText t=new VText(xt,yt,0,ConfigManager.literalColorTB,displayedValue);
	    Editor.vsm.addGlyph(t,Editor.mainVirtualSpace);
	    res=application.addLiteral(value,null,escapeXML);
	    res.setGlyphText(t);	    
	}
	else {res=application.addLiteral("",null,true);}	
	if (e.hasAttribute("xml:lang")){res.setLanguage(e.getAttribute("xml:lang"));}
	else if (e.hasAttribute("lang")){res.setLanguage(e.getAttribute("lang"));} //in theory, we should no accept this one, but ISV until 1.1 has been generating lang attrib for project files without the xml: prefix (mistake) so we allow it for compatibility reasons
	res.setGlyph(r);
	uniqueIDs2INodes.put(e.getAttribute("id"),res);
	if (e.hasAttribute("commented")){//is node commented out?
	    boolean b=(new Boolean(e.getAttribute("commented"))).booleanValue();
	    if (b){application.commentNode(res,true);}
	}
	return res;
    }

    /*create a new property instance from ISV project file*/
    IProperty createIPropertyFromISV(Element e){
	IProperty res=null;
	Element e1=(Element)(e.getElementsByTagNameNS(Editor.isavizURI,"path")).item(0);
	String d=e1.getAttribute("d");
	Element e3=(Element)(e.getElementsByTagNameNS(Editor.isavizURI,"head")).item(0);
	long x=(new Long(e3.getAttribute("x"))).longValue();
	long y=(new Long(e3.getAttribute("y"))).longValue();
	long w=(new Long(e3.getAttribute("w"))).longValue();
	float h=(new Float(e3.getAttribute("or"))).floatValue();
 	VPath p=new VPath(0,ConfigManager.propertyColorB,d);
//  	VPath p=new VClippedPath(0,ConfigManager.propertyColorB,d);
	VTriangleOr tr=new VTriangleOr(x,y,0,w,ConfigManager.propertyColorB,h);
	Editor.vsm.addGlyph(tr,Editor.mainVirtualSpace);
	Editor.vsm.addGlyph(p,Editor.mainVirtualSpace);
// 	tr.setPaintBorder(false);
	NodeList nl=e.getElementsByTagNameNS(Editor.isavizURI,"uri");
	String ns="";  //namespace
	String ln="";  //localname
	if (nl.getLength()>0){
	    Element e2=(Element)nl.item(0);
	    ns=e2.getElementsByTagNameNS(Editor.isavizURI,"namespace").item(0).getFirstChild().getNodeValue();
	    ln=e2.getElementsByTagNameNS(Editor.isavizURI,"localname").item(0).getFirstChild().getNodeValue();
	    long xt=(new Long(e2.getAttribute("x"))).longValue();
	    long yt=(new Long(e2.getAttribute("y"))).longValue();
	    String uri="";
	    boolean bindingDefined=false;
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,1)).equals(ns)){
		    if (((Boolean)application.tblp.nsTableModel.getValueAt(i,2)).booleanValue()){uri=((String)application.tblp.nsTableModel.getValueAt(i,0))+":"+ln;}
		    else {uri=ns+ln;}
		    bindingDefined=true;
		    break;
		}
	    }
	    if (!bindingDefined){uri=ns+ln;}
	    VText t=new VText(xt,yt,0,ConfigManager.propertyColorT,uri);
	    Editor.vsm.addGlyph(t,Editor.mainVirtualSpace);
	    res=application.addProperty(ns,ln);
	    res.setGlyphText(t);
	}
	else {res=new IProperty();}
	res.setGlyph(p,tr);
	//sb and op should always exist, since a predicate cannot exist on its own ; it is always linked to a subject and an object (when one of these is deleted, the predicate is automatically destroyed)
	IResource subject=(IResource)uniqueIDs2INodes.get(e.getAttribute("sb"));
	res.setSubject(subject);
	subject.addOutgoingPredicate(res);
	Object o1=uniqueIDs2INodes.get(e.getAttribute("ob"));	
	if (o1 instanceof IResource){
	    IResource object=(IResource)o1;
	    res.setObject(object);
	    object.addIncomingPredicate(res);
	}
	else {//o1 is an ILiteral (or we have an error)
	    ILiteral object=(ILiteral)o1;
	    res.setObject(object);
	    object.setIncomingPredicate(res);
	    if (res.getIdent().equals(Editor.RDFS_NAMESPACE_URI+"label")){//if property is rdfs:label, set label for the resource
		subject.setLabel(object.getValue());
	    }
	}
	if (e.hasAttribute("commented")){//is edge commented out?
	    boolean b=(new Boolean(e.getAttribute("commented"))).booleanValue();
	    if (b){application.commentPredicate(res,true);}
	}
	return res;
    }


}

/*   FILE: IResource.java
 *   DATE OF CREATION:   10/18/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Feb 12 11:13:17 2003 by Emmanuel Pietriga
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xerox.VTM.glyphs.VEllipse;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpaceManager;
import com.xerox.VTM.engine.VirtualSpace;

import com.hp.hpl.mesa.rdf.jena.model.Resource;
import com.hp.hpl.mesa.rdf.jena.model.AnonId;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;

/*Our internal model class for RDF Resources*/

class IResource extends INode {

    /**returns the substring of a Jena AnonID that is unique for each anonymous resource (i.e. what is after the first stroke)
     * e.g. e184cb:ea21ce7dcf:-7fda  will return 7fda because everything that else is common to all AnonIDs fro a given implementation/execution of the application
     */
    public static String getJenaAnonId(AnonId id){
	StringBuffer sb=new StringBuffer(id.toString());
	int i=0;
	while (i<sb.length()){
	    if (sb.charAt(i)=='-'){sb.delete(0,i+1);break;}
	    i++;
	}
	return sb.toString();
    }

    Vector incomingPredicates;   //list of IProperty - null if empty
    Vector outgoingPredicates;   //list of IProperty - null if empty

    private boolean anonymous=false;   //anonymous resource or not
    private String anonymousID;        //anonymous id (null if not anonymous)
    private String namespace;          //namespace+localname = URI
    private String localname;

    private String label;  //set only if there is an rdfs:label property for this resource

    String mapID;

    VEllipse gl1;
    VText gl2;    //if not text has been entered yet, this glyph is null (use this test to find out if there is text)

    /**
     *@param r Jena resource representing this node
     */
    public IResource(Resource r){
	try {
	    if (r.isAnon()){
		anonymous=true;
		anonymousID=Editor.ANON_NODE+IResource.getJenaAnonId(r.getId());
	    }
	    else {
		anonymous=false;
		if (r.getLocalName().length()>0){//seems to always be the case with Jena 1.3.2
		    namespace=r.getNameSpace();
// 		    if (namespace.equals(Editor.BASE_URI+"/")){
// 			namespace=namespace.substring(0,namespace.length()-1);
// 			//it looks like Jena 1.3.2 appends automatically a '/' after the base URI if there is nothing like / or #
// 		    }
		    localname=r.getLocalName();
		}
		else {
		    // when Jena cannot make the difference between namespace and localname it stores everything in namespace and nothing in localname
		    if (r.getNameSpace().startsWith("http://")){//if there is no localname, but only a namespace, do not prepend the default namespace (right now the test only consists of detecting strings beginning with http - something more elaborate would probably be a good idea)
			namespace=r.getNameSpace();
			localname="";
		    }
		    else {//if on the contrary there is only a localname, append the default namespace
			namespace=Editor.BASE_URI;
			localname=r.getNameSpace();
			if (localname.startsWith(Editor.BASE_URI)){
			    localname=localname.substring(Editor.BASE_URI.length(),localname.length());
			}
		    }
		}
	    }
	}
	catch (RDFException ex){System.err.println("Error: IResouce(Resource - Jena): "+ex);}
    }

    /**Create a new IResource from scratch (information will be added later)*/
    public IResource(){}

    void setNamespace(String n){
	namespace=n;
	try {if (namespace.equals(Editor.BASE_URI) && (!localname.startsWith("#"))){localname="#"+localname;}}
	catch (NullPointerException e){}
    }

    void setLocalname(String l){
	localname=l;
	try {if (namespace.equals(Editor.BASE_URI) && (!localname.startsWith("#"))){localname="#"+localname;}}
	catch (NullPointerException e){}
    }

    //the split between namespace and localname is made automatically by IsaViz/Jena (it is just a guess)
    void setURI(String uri){
        int splitPoint = com.hp.hpl.mesa.rdf.jena.common.Util.splitNamespace(uri); 
        if (splitPoint == 0) {
            namespace = uri;
            localname = "";
        }
	else {
            namespace = uri.substring(0, splitPoint);
            localname = uri.substring(splitPoint);
        }
    }

    //id MUST contain the anon prefix
    void setAnonymousID(String id){anonymousID=id;}

    boolean isAnon(){return anonymous;}

    void setAnon(boolean b){anonymous=b;}
    
    public String getIdent(){
	if (anonymous){return anonymousID;}
	else {
	    try {
		String res=namespace+localname;
		return (res.equals("nullnull")) ? null : res ;
	    }
	    catch (NullPointerException ex){return null;}
	}
    }

    public String getNamespace(){
	return namespace;
    }

    public String getLocalname(){
	return localname;
    }

    //rdfs:label property (set to null if empty)
    public void setLabel(String s){label=(s.length()==0) ? null : s;}

    //rdfs:label property (can be null)
    public String getLabel(){return label;}

    public void setMapID(String s){mapID=s;}

    public String getMapID(){return mapID;}

    public void addIncomingPredicate(IProperty p){
	if (incomingPredicates==null){
	    incomingPredicates=new Vector();
	    incomingPredicates.add(p);
	}
	else {
	    if (!incomingPredicates.contains(p)){incomingPredicates.add(p);}
	}
    }

    public void removeIncomingPredicate(IProperty p){
	if (incomingPredicates!=null && incomingPredicates.contains(p)){
	    incomingPredicates.remove(p);
	    if (incomingPredicates.isEmpty()){incomingPredicates=null;}
	}
    }

    /**returns null if none*/
    public Vector getIncomingPredicates(){
	return incomingPredicates;
    }

    public void addOutgoingPredicate(IProperty p){
	if (outgoingPredicates==null){
	    outgoingPredicates=new Vector();
	    outgoingPredicates.add(p);
	}
	else {
	    if (!outgoingPredicates.contains(p)){outgoingPredicates.add(p);}
	}
    }

    public void removeOutgoingPredicate(IProperty p){
	if (outgoingPredicates!=null && outgoingPredicates.contains(p)){
	    outgoingPredicates.remove(p);
	    if (outgoingPredicates.isEmpty()){outgoingPredicates=null;}
	}
    }

    /**returns null if none*/
    public Vector getOutgoingPredicates(){
	return outgoingPredicates;
    }
    
    /**selects this node (and assigns colors to glyph and text)*/
    public void setSelected(boolean b){
	super.setSelected(b);
	if (selected){
	    gl1.setHSVColor(ConfigManager.selFh,ConfigManager.selFs,ConfigManager.selFv);
	    gl1.setHSVbColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);
	    if (gl2!=null){gl2.setHSVColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);}
	    VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
	    vs.onTop(gl1);vs.onTop(gl2);
	}
	else {
	    if (commented){
		gl1.setHSVColor(ConfigManager.comFh,ConfigManager.comFs,ConfigManager.comFv);
		gl1.setHSVbColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
		if (gl2!=null){gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	    }
	    else {
		gl1.setHSVColor(ConfigManager.resFh,ConfigManager.resFs,ConfigManager.resFv);
		gl1.setHSVbColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);
		if (gl2!=null){gl2.setHSVColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);}
	    }
	}
    }

    public void comment(boolean b,Editor e){
	commented=b;
	if (commented){//comment
	    gl1.setHSVColor(ConfigManager.comFh,ConfigManager.comFs,ConfigManager.comFv);
	    gl1.setHSVbColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
	    if (gl2!=null){gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	    if (incomingPredicates!=null){
		for (int i=0;i<incomingPredicates.size();i++){
		    e.commentPredicate((IProperty)incomingPredicates.elementAt(i),true);
		}
	    }
	    if (outgoingPredicates!=null){
		for (int i=0;i<outgoingPredicates.size();i++){
		    e.commentPredicate((IProperty)outgoingPredicates.elementAt(i),true);
		}
	    }
	}
	else {//uncomment
	    gl1.setHSVColor(ConfigManager.resFh,ConfigManager.resFs,ConfigManager.resFv);
	    gl1.setHSVbColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);
	    if (gl2!=null){gl2.setHSVColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);}
	    if (incomingPredicates!=null){
		for (int i=0;i<incomingPredicates.size();i++){
		    e.commentPredicate((IProperty)incomingPredicates.elementAt(i),false);
		}
	    }
	    if (outgoingPredicates!=null){
		for (int i=0;i<outgoingPredicates.size();i++){
		    e.commentPredicate((IProperty)outgoingPredicates.elementAt(i),false);
		}
	    }
	}
    }

    public void setGlyph(VEllipse e){
	gl1=e;
	gl1.setType(Editor.resEllipseType);   //means resource glyph (glyph type is a quick way to retrieve glyphs from VTM)
	gl1.setOwner(this);
    }

    public void setGlyphText(VText t){
	if (t!=null){
	    gl2=t;
	    gl2.setType(Editor.resTextType);  //means resource text (glyph type is a quick way to retrieve glyphs from VTM)
	    gl2.setOwner(this);
	}
	else {gl2=null;}
    }

    public Glyph getGlyph(){
	return gl1;
    }

    public VText getGlyphText(){
	return gl2;
    }

    public Element toISV(Document d,ISVManager e){
	Element res=d.createElementNS(Editor.isavizURI,"isv:iresource");
	Element identif=d.createElementNS(Editor.isavizURI,"isv:URIorID");
	if (!anonymous){
	    if ((namespace!=null) && (!namespace.equals(Editor.BASE_URI))){//do not store namespace if equal to default namespace
		Element namespaceEL=d.createElementNS(Editor.isavizURI,"isv:namespace");
		namespaceEL.appendChild(d.createTextNode(namespace));
		identif.appendChild(namespaceEL);
	    }
	    if (localname!=null){
		Element localnameEL=d.createElementNS(Editor.isavizURI,"isv:localname");
		localnameEL.appendChild(d.createTextNode(localname));
		identif.appendChild(localnameEL);
	    }
	}
	else {
	    if (anonymousID!=null){
		Element anonIDEL=d.createElementNS(Editor.isavizURI,"isv:anonID");
		//do not save prefix since it is a user preference (read from the config file)
		anonIDEL.appendChild(d.createTextNode(Utils.erasePrefix(anonymousID)));
		identif.appendChild(anonIDEL);
	    }
	}
	if (gl2!=null){
	    identif.setAttribute("x",String.valueOf(gl2.vx));
	    identif.setAttribute("y",String.valueOf(gl2.vy));
	}
	res.appendChild(identif);
	res.setAttribute("x",String.valueOf(gl1.vx));
	res.setAttribute("y",String.valueOf(gl1.vy));
	res.setAttribute("w",String.valueOf(gl1.getWidth()));
	res.setAttribute("h",String.valueOf(gl1.getHeight()));
	if (anonymous){res.setAttribute("isAnon","true");}  //do not put this attr if not anon (although isAnon="false" is supported by the ISV loader)
	if (commented){res.setAttribute("commented","true");}  //do not put this attr if not commented (although commented="false" is supported by the ISV loader)
	res.setAttribute("id",e.getPrjId(this));
	return res;
    }

    public String toString(){return super.toString()+" "+getIdent();}

    //a meaningful string representation of this IResource
    public String getText(){return (getIdent()==null) ? "" : getIdent();}

    public void displayOnTop(){
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl1);
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl2);
    }

}

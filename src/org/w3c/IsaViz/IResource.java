/*   FILE: IResource.java
 *   DATE OF CREATION:   10/18/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Thu Apr 17 11:28:37 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
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

import com.xerox.VTM.glyphs.RectangularShape;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpaceManager;
import com.xerox.VTM.engine.VirtualSpace;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.RDFException;

/*Our internal model class for RDF Resources*/

class IResource extends INode {

    int strokeIndex;
    int fillIndex;

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

    Glyph gl1;
    VText gl2;    //if not text has been entered yet, this glyph is null (use this test to find out if there is text)

    /**
     *@param r Jena resource representing this node
     */
    public IResource(Resource r){
	fillIndex=ConfigManager.defaultRFIndex;
	strokeIndex=ConfigManager.defaultRTBIndex;
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
    public IResource(){
	fillIndex=ConfigManager.defaultRFIndex;
	strokeIndex=ConfigManager.defaultRTBIndex;
    }

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
        int splitPoint = com.hp.hpl.jena.rdf.model.impl.Util.splitNamespace(uri); 
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
// 	    System.err.println(getIdent()+" -a-> "+Utils.vectorOfObjectsAsCSString(outgoingPredicates));
	}
	else {
	    if (!outgoingPredicates.contains(p)){outgoingPredicates.add(p);} //System.err.println(getIdent()+" -b-> "+Utils.vectorOfObjectsAsCSString(outgoingPredicates));}
	    //else {System.err.println(getIdent()+" -c-> "+Utils.vectorOfObjectsAsCSString(outgoingPredicates));}
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
	if (this.isVisuallyRepresented()){
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
		    gl1.setColor(ConfigManager.colors[fillIndex]);
		    gl1.setBorderColor(ConfigManager.colors[strokeIndex]);
		    if (gl2!=null){gl2.setColor(ConfigManager.colors[strokeIndex]);}
		}
	    }
	}
    }

    public void comment(boolean b,Editor e){
	commented=b;
	if (commented){//comment
	    if (this.isVisuallyRepresented()){
		gl1.setHSVColor(ConfigManager.comFh,ConfigManager.comFs,ConfigManager.comFv);
		gl1.setHSVbColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
		if (gl2!=null){gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	    }
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
	    if (this.isVisuallyRepresented()){
		gl1.setColor(ConfigManager.colors[fillIndex]);
		gl1.setBorderColor(ConfigManager.colors[strokeIndex]);
		if (gl2!=null){gl2.setColor(ConfigManager.colors[strokeIndex]);}
	    }
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

    public void setVisible(boolean b){
	if (gl1!=null){gl1.setVisible(b);gl1.setSensitivity(b);}
	if (gl2!=null){gl2.setVisible(b);gl2.setSensitivity(b);}
    }

    public void setGlyph(Glyph e){
	gl1=e;
	gl1.setType(Editor.resShapeType);   //means resource glyph (glyph type is a quick way to retrieve glyphs from VTM)
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
	if (gl1 instanceof RectangularShape){
	    res.setAttribute("w",String.valueOf(((RectangularShape)gl1).getWidth()));
	    res.setAttribute("h",String.valueOf(((RectangularShape)gl1).getHeight()));
	}
	else {
	    res.setAttribute("sz",String.valueOf(gl1.getSize()));
	}
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

    public void setFillColor(int i){//index of color in ConfigManager.colors
	fillIndex=i;
	gl1.setColor(ConfigManager.colors[fillIndex]);
    }
    
    public void setStrokeColor(int i){//index of color in ConfigManager.colors
	strokeIndex=i;
	gl1.setBorderColor(ConfigManager.colors[strokeIndex]);
	if (gl2!=null){gl2.setColor(ConfigManager.colors[strokeIndex]);}
    }

    /*returns true if this resource has a property rdf:type with value type*/
    public boolean hasRDFType(String type){
	if (outgoingPredicates!=null){
	    IProperty p;
	    for (int i=0;i<outgoingPredicates.size();i++){
		p=(IProperty)outgoingPredicates.elementAt(i);
		if (p.getIdent().equals(GraphStylesheet._rdfType) && (p.getObject() instanceof IResource) && ((IResource)p.getObject()).getIdent().equals(type)){return true;}
	    }
	}
	return false;
    }

}

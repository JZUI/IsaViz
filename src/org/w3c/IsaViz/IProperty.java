/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 10/18/2001
 */


package org.w3c.IsaViz;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xerox.VTM.glyphs.VPath;
import com.xerox.VTM.glyphs.VTriangleOr;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpace;

import com.hp.hpl.mesa.rdf.jena.model.Property;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;

/*Our internal model class for RDF Properties. Instances of this class are not property types, but predicates (instances of a property type). So there can be many IProperty with the same URI.*/ 

class IProperty extends INode {

    IResource subject;
    INode object;

    private String namespace="";          //namespace+localname = URI
    private String localname="";

    VPath gl1;
    VTriangleOr gl2;
    VText gl3;    //if not text has been entered yet, this glyph is null (use this test to find out if there is text)

    String mapID;

    /**
     *@param rs Jena property representing this edge
     */
    public IProperty(Property p){
	namespace=p.getNameSpace();
	localname=p.getLocalName();
    }

    /**Create a new IProperty from scratch (information will be added later)*/
    public IProperty(){}

//     /**returns the Jena property*/
//     public Property getJenaProperty(){
// 	return p;
//     }

//     public void setJenaProperty(Property prop){
// 	p=prop;
//     }

    void setNamespace(String n){namespace=n;}

    void setLocalname(String l){localname=l;}

    public String getIdent(){
	try {
	    String res=namespace+localname;
	    return (res.equals("nullnull")) ? null : res ;
	}
	catch (NullPointerException ex){return null;}
    }

    public String getNamespace(){
	return namespace;
    }

    public String getLocalname(){
	return localname;
    }

    public void setMapID(String s){mapID=s;}

    public String getMapID(){return mapID;}

    public void setSubject(IResource r){
	subject=r;
    }
    
    public IResource getSubject(){
	return subject;
    }

    public void setObject(IResource r){
	object=r;
    }
    
    public void setObject(ILiteral l){
	object=l;
    }
    
    /**can be an IResource or an ILiteral*/
    public INode getObject(){
	return object;
    }

    /**selects this node (and assigns colors to glyph and text)*/
    public void setSelected(boolean b){
	super.setSelected(b);
	if (selected){
	    gl1.setHSVColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);
	    gl2.setHSVColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);
	    if (gl3!=null){gl3.setHSVColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);}
	    gl1.setStrokeWidth(2.0f);
	    VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);  //bring element
	    vs.onTop(gl1);vs.onTop(gl2);vs.onTop(gl3);                            //to front
	}
	else {
	    if (commented){
		gl1.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
		gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
		if (gl3!=null){gl3.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	    }
	    else {
		gl1.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
		gl2.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
		if (gl3!=null){gl3.setHSVColor(ConfigManager.prpTh,ConfigManager.prpTs,ConfigManager.prpTv);}
	    }
	    gl1.setStrokeWidth(1.0f);
	}
    }

    public void comment(boolean b,Editor e){
	if (b){//comment
	    commented=b;
	    gl1.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
	    gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
	    if (gl3!=null){gl3.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	}
	else {//uncomment
	    if (subject!=null){//do not uncomment predicate if either subject or object is still null
		if (object!=null){
		    if ((!subject.isCommented()) && (!object.isCommented())){
			commented=b;
			gl1.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			gl2.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			if (gl3!=null){gl3.setHSVColor(ConfigManager.prpTh,ConfigManager.prpTs,ConfigManager.prpTv);}
		    }
		}
		else {
		    if (!subject.isCommented()){
			commented=b;
			gl1.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			gl2.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			if (gl3!=null){gl3.setHSVColor(ConfigManager.prpTh,ConfigManager.prpTs,ConfigManager.prpTv);}
		    }
		}
	    }
	    else {
		if (object!=null){
		    if (!object.isCommented()){
			commented=b;
			gl1.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			gl2.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
			if (gl3!=null){gl3.setHSVColor(ConfigManager.prpTh,ConfigManager.prpTs,ConfigManager.prpTv);}
		    }
		}//else should never happen (a predicate alone cannot exist)
	    }
	}
	


    }

    public void setGlyph(VPath p,VTriangleOr t){
	gl1=p;
	gl1.setType(Editor.propPathType);   //means predicate glyph (glyph type is a quick way to retrieve glyphs from VTM)
	gl1.setOwner(this);  
	gl2=t;
	gl2.setType(Editor.propHeadType);   //means predicate head (glyph type is a quick way to retrieve glyphs from VTM)
	gl2.setOwner(this);
	gl2.setPaintBorder(false);
    }

    //called when initializing/destroying a resizer for this property's path - accepts null as input
    protected void setGlyphHead(VTriangleOr t){
	gl2=t;
	if (gl2!=null){
	    gl2.setPaintBorder(false);
	    gl2.setType(Editor.propHeadType);   //means predicate head (glyph type is a quick way to retrieve glyphs from VTM)
	    gl2.setOwner(this);
	}
    }

    public void setGlyphText(VText t){
	gl3=t;
	gl3.setType(Editor.propTextType);   //means predicate glyph (glyph type is a quick way to retrieve glyphs from VTM)
	gl3.setOwner(this);
    }

    public Glyph getGlyph(){
	return gl1;
    }

    public VTriangleOr getGlyphHead(){
	return gl2;
    }

    public VText getGlyphText(){
	return gl3;
    }

   public Element toISV(Document d,ISVManager e){
	Element res=d.createElementNS(Editor.isavizURI,"isv:iproperty");
	Element path=d.createElementNS(Editor.isavizURI,"isv:path");
	Element head=d.createElementNS(Editor.isavizURI,"isv:head");
	if (gl3!=null){
	    Element uri=d.createElementNS(Editor.isavizURI,"isv:uri");
	    uri.setAttribute("x",String.valueOf(gl3.vx));
	    uri.setAttribute("y",String.valueOf(gl3.vy));
	    Element namespaceEL=d.createElementNS(Editor.isavizURI,"isv:namespace");
	    namespaceEL.appendChild(d.createTextNode(namespace));
	    Element localnameEL=d.createElementNS(Editor.isavizURI,"isv:localname");
	    localnameEL.appendChild(d.createTextNode(localname));
	    uri.appendChild(namespaceEL);uri.appendChild(localnameEL);
	    res.appendChild(uri);
	}
	StringBuffer coords=new StringBuffer();
	java.awt.geom.PathIterator pi=gl1.getJava2DPathIterator();
	float[] seg=new float[6];
	int type;
	char lastOp='Z';  //anything but M, L, Q, C since we want the first command to explicitely appear in any case
	while (!pi.isDone()){//save the path as a sequence of instructions following the SVG model for "d" attributes
	//we save it in SVG coordinates (not VTM) because we already have an SVG path interpreter built in VTM's SVGCreator
	    type=pi.currentSegment(seg);
	    switch (type){
	    case java.awt.geom.PathIterator.SEG_MOVETO:{
		if (lastOp!='M'){coords.append('M');} else {coords.append(' ');}
		lastOp='M';
		coords.append(Utils.abl2c(seg[0])+" "+Utils.abl2c(seg[1]));
		break;
	    }
	    case java.awt.geom.PathIterator.SEG_LINETO:{
		if (lastOp!='L'){coords.append('L');} else {coords.append(' ');}
		lastOp='L';
		coords.append(Utils.abl2c(seg[0])+" "+Utils.abl2c(seg[1]));
		break;
	    }
	    case java.awt.geom.PathIterator.SEG_QUADTO:{
		if (lastOp!='Q'){coords.append('Q');} else {coords.append(' ');}
		lastOp='Q';
		coords.append(Utils.abl2c(seg[0])+" "+Utils.abl2c(seg[1])+" "+Utils.abl2c(seg[2])+" "+Utils.abl2c(seg[3]));
		break;
	    }
	    case java.awt.geom.PathIterator.SEG_CUBICTO:{
		if (lastOp!='C'){coords.append('C');} else {coords.append(' ');}
		lastOp='C';
		coords.append(Utils.abl2c(seg[0])+" "+Utils.abl2c(seg[1])+" "+Utils.abl2c(seg[2])+" "+Utils.abl2c(seg[3])+" "+Utils.abl2c(seg[4])+" "+Utils.abl2c(seg[5]));
		break;
	    }
	    }
	    pi.next();
	}
	path.setAttribute("d",coords.toString());
	res.appendChild(path);
	head.setAttribute("x",String.valueOf(gl2.vx));
	head.setAttribute("y",String.valueOf(gl2.vy));
	head.setAttribute("w",Utils.abl2c(String.valueOf(gl2.getSize())));  //only this one will be saved/read (w=h)
	head.setAttribute("or",String.valueOf(gl2.getOrient()));  
	res.appendChild(head);
	if (subject!=null){res.setAttribute("sb",e.getPrjId(subject));}
	if (object!=null){res.setAttribute("ob",e.getPrjId(object));}
	if (commented){res.setAttribute("commented","true");}  //do not put this attr if not commented (although commented="false" is supported by the ISV loader)
	return res;
    }
    
    public String toString(){return super.toString()+" "+getIdent();}

    //a meaningful string representation of this IProperty
    public String getText(){return getIdent()!=null ? getIdent() : "";}
    
    public void displayOnTop(){
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl1);
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl2);
	if (gl3!=null){Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl3);}
    }

}
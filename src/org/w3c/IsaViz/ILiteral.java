/*   FILE: ILiteral.java
 *   DATE OF CREATION:   10/18/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Thu Apr 17 11:28:27 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xerox.VTM.glyphs.RectangularShape;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpaceManager;
import com.xerox.VTM.engine.VirtualSpace;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.datatypes.RDFDatatype;

/*Our internal model class for RDF Literals*/

class ILiteral extends INode {

    int strokeIndex;
    int fillIndex;
    
    private boolean escapeXML=true;
    private String language;
    private String value;
    private RDFDatatype datatype; //null if plain literal

    IProperty incomingPred;

    Glyph gl1;
    VText gl2;   //if not text has been entered yet, this glyph is null (use this test to find out if there is text)

    String mapID;

    /**
     *@param lt Jena literal representing this node
     */
    public ILiteral(Literal l){
	fillIndex=ConfigManager.defaultLFIndex;
	strokeIndex=ConfigManager.defaultLTBIndex;
	try {
	    escapeXML=l.getWellFormed();  //right now, Jena always say false - do not know why Bug? - anyway does not seem to have an impact on serialization, even if it is indeed well-formed
	    if (l.getLanguage().length()>0){language=l.getLanguage();}
	    datatype=l.getDatatype();
	    value=l.getLexicalForm();
	    //value=l.getString();
	}
	catch (RDFException ex){System.err.println("Error: ILiteral(Literal - Jena): "+ex);}
    }

    /**Create a new ILiteral from scratch (information will be added later)*/
    public ILiteral(){
	fillIndex=ConfigManager.defaultLFIndex;
	strokeIndex=ConfigManager.defaultLTBIndex;
    }

    void setLanguage(String l){language=l;}

    public String getLang(){return language;}
    
    void setEscapeXMLChars(boolean b){escapeXML=b;}

    public boolean escapesXMLChars(){return escapeXML;}

    void setValue(String v){value=v;}


//     /**returns the Jena literal*/
//     public Literal getJenaLiteral(){
// 	return l;
//     }
    
//     public void setJenaLiteral(Literal lit){
// 	l=lit;
//     }
    
    public String getValue(){
	return value;
    }

    //null for plain literal
    public void setDatatype(String uri){
	if (uri==null){datatype=null;}
	else {
	    if (uri.length()!=0 && !Utils.isWhiteSpaceCharsOnly(uri)){datatype=com.hp.hpl.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(uri);}
	    else {datatype=null;}
	}
    }

    //null for plain literal
    public void setDatatype(RDFDatatype dt){
	datatype=dt;
    }

    //null if plain literal
    public RDFDatatype getDatatype(){
	return datatype;
    }

    public void setMapID(String s){mapID=s;}

    public String getMapID(){return mapID;}

    public void setIncomingPredicate(IProperty p){
	incomingPred=p;
    }

    public IProperty getIncomingPredicate(){
	return incomingPred;
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
	    if (incomingPred!=null){
		e.commentPredicate(incomingPred,true);
	    }
	}
	else {//uncomment
	    if (this.isVisuallyRepresented()){
		gl1.setColor(ConfigManager.colors[fillIndex]);
		gl1.setBorderColor(ConfigManager.colors[strokeIndex]);
		if (gl2!=null){gl2.setColor(ConfigManager.colors[strokeIndex]);}
	    }
	    if (incomingPred!=null){
		e.commentPredicate(incomingPred,false);
	    }
	}
    }

    public void setVisible(boolean b){
	if (gl1!=null){gl1.setVisible(b);gl1.setSensitivity(b);}
	if (gl2!=null){gl2.setVisible(b);gl2.setSensitivity(b);}
    }

    public void setGlyph(Glyph r){
	gl1=r;
	gl1.setType(Editor.litShapeType);  //means literal glyph (glyph type is a quick way to retrieve glyphs from VTM)
	gl1.setOwner(this);
    }

    public void setGlyphText(VText t){
	gl2=t;
	if (gl2!=null){
	    gl2.setType(Editor.litTextType);  //means literal text (glyph type is a quick way to retrieve glyphs from VTM)
	    gl2.setOwner(this);
	}
    }

    public Glyph getGlyph(){
	return gl1;
    }

    public VText getGlyphText(){
	return gl2;
    }

   public Element toISV(Document d,ISVManager e){
	Element res=d.createElementNS(Editor.isavizURI,"isv:iliteral");
	if (gl2!=null){
	    Element val=d.createElementNS(Editor.isavizURI,"isv:value");
	    val.appendChild(d.createTextNode(getValue()));
	    val.setAttribute("x",String.valueOf(gl2.vx));
	    val.setAttribute("y",String.valueOf(gl2.vy));
	    res.appendChild(val);
	    if (!escapeXML){res.setAttribute("escapeXML","false");} //if value is not well-formed XML, signal it
	}
	if (language!=null){
	    res.setAttribute("xml:lang",language);
	}
	if (datatype!=null){
	    res.setAttribute("dtURI",datatype.getURI());
	}
	res.setAttribute("x",String.valueOf(gl1.vx));
	res.setAttribute("y",String.valueOf(gl1.vy));
	if (gl1 instanceof RectangularShape){
	    res.setAttribute("w",String.valueOf(((RectangularShape)gl1).getWidth()));
	    res.setAttribute("h",String.valueOf(((RectangularShape)gl1).getHeight()));
	}
	else {
	    res.setAttribute("sz",String.valueOf(gl1.getSize()));
	}
	if (commented){res.setAttribute("commented","true");}  //do not put this attr if not commented (although commented="false" is supported by the ISV loader)
	res.setAttribute("id",e.getPrjId(this));
	return res;
    }

    public String toString(){
	String res=super.toString();
	if (getValue()!=null){res+=" "+getValue();}
	if (getDatatype()!=null){res+=" ["+getDatatype().getURI()+"]";}
	return res;
    }

    //a meaningful string representation of this ILiteral
    public String getText(){
	if (value!=null){return (value.length()>=Editor.MAX_LIT_CHAR_COUNT) ? value.substring(0,Editor.MAX_LIT_CHAR_COUNT) : value;}
	else return "";
    }

    public void displayOnTop(){
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl1);
	if (gl2!=null){Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl2);}
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

    
}

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

import com.xerox.VTM.glyphs.VRectangle;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpaceManager;
import com.xerox.VTM.engine.VirtualSpace;

import com.hp.hpl.mesa.rdf.jena.model.Literal;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;

/*Our internal model class for RDF Literals*/

class ILiteral extends INode {
    
    private boolean escapeXML=true;
    private String language;
    private String value;

    IProperty incomingPred;

    VRectangle gl1;
    VText gl2;   //if not text has been entered yet, this glyph is null (use this test to find out if there is text)

    String mapID;

    /**
     *@param lt Jena literal representing this node
     */
    public ILiteral(Literal l){
	try {
	    escapeXML=l.getWellFormed();  //right now, Jena always say false - do not know why Bug? - anyway does not seem to have an impact on serialization, even if it is indeed well-formed
	    if (l.getLanguage().length()>0){language=l.getLanguage();}
	    value=l.getString();
	}
	catch (RDFException ex){System.err.println("Error: ILiteral(Literal - Jena): "+ex);}
    }

    /**Create a new ILiteral from scratch (information will be added later)*/
    public ILiteral(){}

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
		gl1.setHSVColor(ConfigManager.litFh,ConfigManager.litFs,ConfigManager.litFv);
		gl1.setHSVbColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);
		if (gl2!=null){gl2.setHSVColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);}
	    }
	}
    }

    public void comment(boolean b,Editor e){
	commented=b;
	if (commented){//comment
	    gl1.setHSVColor(ConfigManager.comFh,ConfigManager.comFs,ConfigManager.comFv);
	    gl1.setHSVbColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
	    if (gl2!=null){gl2.setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);}
	    if (incomingPred!=null){
		e.commentPredicate(incomingPred,true);
	    }
	}
	else {//uncomment
	    gl1.setHSVColor(ConfigManager.litFh,ConfigManager.litFs,ConfigManager.litFv);
	    gl1.setHSVbColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);
	    if (gl2!=null){gl2.setHSVColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);}
	    if (incomingPred!=null){
		e.commentPredicate(incomingPred,false);
	    }
	}
    }

    public void setGlyph(VRectangle r){
	gl1=r;
	gl1.setType(Editor.litRectType);  //means literal glyph (glyph type is a quick way to retrieve glyphs from VTM)
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
	    res.setAttribute("lang",language);
	}
	res.setAttribute("x",String.valueOf(gl1.vx));
	res.setAttribute("y",String.valueOf(gl1.vy));
	res.setAttribute("w",String.valueOf(gl1.getWidth()));
	res.setAttribute("h",String.valueOf(gl1.getHeight()));
	if (commented){res.setAttribute("commented","true");}  //do not put this attr if not commented (although commented="false" is supported by the ISV loader)
	res.setAttribute("id",e.getPrjId(this));
	return res;
    }

    public String toString(){return super.toString()+" "+getValue();}

    //a meaningful string representation of this ILiteral
    public String getText(){
	if (value!=null){return (value.length()>=Editor.MAX_LIT_CHAR_COUNT) ? value.substring(0,Editor.MAX_LIT_CHAR_COUNT) : value;}
	else {return "";}
    }

    public void displayOnTop(){
	Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl1);
	if (gl2!=null){Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(gl2);}
    }
    
}

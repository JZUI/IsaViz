/*   FILE: StyleInfoP.java
 *   DATE OF CREATION:   Tue Apr 01 14:25:37 2003
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Apr 02 09:11:31 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */ 

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 


package org.w3c.IsaViz;

import java.awt.Color;

class StyleInfoP extends StyleInfo {

    StyleInfoP(){

    }

    void applyStyle(Style s){
	if (s!=null){
	    if (this.stroke==null && s.getStroke()!=null){this.stroke=s.getStroke();}
	    if (this.strokeWidth==null && s.getStrokeWidth()!=null){this.strokeWidth=s.getStrokeWidth();}
	    if (this.fontFamily==null && s.getFontFamily()!=null){this.fontFamily=s.getFontFamily();}
	    if (this.fontSize==null && s.getFontSize()!=null){this.fontSize=s.getFontSize();}
	    if (this.fontWeight==null && s.getFontWeight()!=null){this.fontWeight=s.getFontWeight();}
	    if (this.fontStyle==null && s.getFontStyle()!=null){this.fontStyle=s.getFontStyle();}
	}
    }

    void applyLayout(Integer l){
	if (this.layout==null && l!=null && (l.equals(GraphStylesheet.TABLE_FORM) || l.equals(GraphStylesheet.NODE_EDGE))){
	    this.layout=l;//set it only if it hasn't been set yet by an higher priority rule
	}
    }

    void applyVisibility(Integer v){
	if (this.visibility==null && v!=null && (v.equals(GraphStylesheet.DISPLAY_NONE) || v.equals(GraphStylesheet.VISIBILITY_HIDDEN) || v.equals(GraphStylesheet.VISIBILITY_VISIBLE))){
	    this.visibility=v;//set it only if it hasn't been set yet by an higher priority rule
	}
    }

    boolean isFullySpecified(){
	if (fontFamily==null || fontSize==null || fontWeight==null || fontStyle==null || visibility==null || layout==null || strokeWidth==null || stroke==null){return false;}
	else return true;
    }

    boolean isDisplayNone(){
	if (visibility!=null && visibility.equals(GraphStylesheet.DISPLAY_NONE)){return true;}
	else {return false;}
    }

    boolean isVisibilityHidden(){
	if (visibility!=null && visibility.equals(GraphStylesheet.VISIBILITY_HIDDEN)){return true;}
	else {return false;}
    }

    Color getStrokeColor(){
	if (stroke==null){return GraphStylesheet.DEFAULT_PROPERTY_STROKE;}
	else {return stroke;}
    }

    Float getStrokeWidth(){
	if (strokeWidth==null){return GraphStylesheet.DEFAULT_PROPERTY_STROKE_WIDTH;}
	else {return strokeWidth;}
    }

    String getFontFamily(){
	if (fontFamily==null){return GraphStylesheet.DEFAULT_PROPERTY_FONT_FAMILY;}
	else {return fontFamily;}
    }

    Integer getFontSize(){
	if (fontSize==null){return GraphStylesheet.DEFAULT_PROPERTY_FONT_SIZE;}
	else {return fontSize;}
    }

    Short getFontWeight(){
	if (fontWeight==null){return GraphStylesheet.DEFAULT_PROPERTY_FONT_WEIGHT;}
	else {return fontWeight;}
    }

    Short getFontStyle(){
	if (fontStyle==null){return GraphStylesheet.DEFAULT_PROPERTY_FONT_STYLE;}
	else {return fontStyle;}
    }

    Integer getVisibility(){
	if (visibility==null){return GraphStylesheet.DEFAULT_PROPERTY_VISIBILITY;}
	else {return visibility;}
    }

    Integer getLayout(){
	if (layout==null){return GraphStylesheet.DEFAULT_PROPERTY_LAYOUT;}
	else {return layout;}
    }

}

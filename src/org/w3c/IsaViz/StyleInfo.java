/*   FILE: StyleInfo.java
 *   DATE OF CREATION:   Tue Apr 01 14:25:37 2003
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Tue Apr 01 15:57:09 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */ 

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 


package org.w3c.IsaViz;

import java.awt.Color;

abstract class StyleInfo {

    Color stroke;

    Float strokeWidth;
    
    String fontFamily;
    Integer fontSize;
    Short fontWeight;
    Short fontStyle;

    Integer visibility;

    Integer layout;

}

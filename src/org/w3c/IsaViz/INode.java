/*   FILE: INode.java
 *   DATE OF CREATION:   10/18/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Mon Mar 24 16:49:38 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.glyphs.VText;

/*Parent of IResource, IProperty, ILiteral*/

abstract class INode {

    boolean selected=false;
    boolean commented=false;

    public void setSelected(boolean b){selected=b;}

    public boolean isSelected(){return selected;}

    public abstract Glyph getGlyph();

    public abstract VText getGlyphText();

    public abstract String getText(); //a meaningful string depending on the node's type (uri, etc)

    public abstract org.w3c.dom.Element toISV(org.w3c.dom.Document d,ISVManager e);

    public abstract void comment(boolean b,Editor e);

    public abstract void displayOnTop();

    public boolean isCommented(){return commented;}

    public boolean isVisuallyRepresented(){//the entity might node be present in the graph (visual) if its has a visibility attribute set to GraphStylesheet._gssHide
	if (this.getGlyph()!=null){return true;}
	else return false;
    }

    public abstract void setVisible(boolean b);

}

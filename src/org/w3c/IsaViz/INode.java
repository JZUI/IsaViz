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

}

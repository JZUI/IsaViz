/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/05/2001
 */


package org.w3c.IsaViz;

import com.xerox.VTM.glyphs.Glyph;

/*Parent of LitResizer, ResResizer, PropResizer. Contains resizing handles (small black boxes) that are used to modify the geometry of a glyph*/

abstract class Resizer {

    abstract void updateMainGlyph(Glyph g);

    abstract void updateHandles();

    abstract void destroy();

    abstract Glyph getMainGlyph();
    
}

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

import java.awt.Color;

import com.xerox.VTM.glyphs.VRectangle;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.engine.VirtualSpace;

/*Class that contains resizing handles (small black boxes) that are used to modify the geometry of a literal's rectangular glyph + methods to update*/

class LitResizer extends Resizer {

    VRectangle g0;
    VRectangle r1;  //East handle
    VRectangle r2;  //North handle
    VRectangle r3;  //West handle
    VRectangle r4;  //South handle

    LitResizer(ILiteral l){
	g0=(VRectangle)l.getGlyph();
	r1=new VRectangle(g0.vx+g0.getWidth(),g0.vy,0,4,4,Color.black);
	r2=new VRectangle(g0.vx,g0.vy+g0.getHeight(),0,4,4,Color.black);
	r3=new VRectangle(g0.vx-g0.getWidth(),g0.vy,0,4,4,Color.black);
	r4=new VRectangle(g0.vx,g0.vy-g0.getHeight(),0,4,4,Color.black);
	Editor.vsm.addGlyph(r1,Editor.mainVirtualSpace);r1.setType("rszr");  //ReSiZe Rectangle
	Editor.vsm.addGlyph(r2,Editor.mainVirtualSpace);r2.setType("rszr");
	Editor.vsm.addGlyph(r3,Editor.mainVirtualSpace);r3.setType("rszr");
	Editor.vsm.addGlyph(r4,Editor.mainVirtualSpace);r4.setType("rszr");
    }

    void updateMainGlyph(Glyph g){//g should be a handle (small black box)
	if (g==r1){long newWidth=g.vx-g0.vx;if (newWidth>0){g0.setWidth(newWidth);r3.vx=g0.vx-g0.getWidth();}}
	else if (g==r2){long newHeight=g.vy-g0.vy;if (newHeight>0){g0.setHeight(newHeight);r4.vy=g0.vy-g0.getHeight();}}
	else if (g==r3){long newWidth=g0.vx-g.vx;if (newWidth>0){g0.setWidth(newWidth);r1.vx=g0.vx+g0.getWidth();}}
	else if (g==r4){long newHeight=g0.vy-g.vy;if (newHeight>0){g0.setHeight(newHeight);r2.vy=g0.vy+g0.getHeight();}}
    }

    void updateHandles(){
	r1.vx=g0.vx+g0.getWidth();r1.vy=g0.vy;
	r2.vx=g0.vx;r2.vy=g0.vy+g0.getHeight();
	r3.vx=g0.vx-g0.getWidth();r3.vy=g0.vy;
	r4.vx=g0.vx;r4.vy=g0.vy-g0.getHeight();
    }

    void destroy(){
	VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
	vs.destroyGlyph(r1);vs.destroyGlyph(r2);vs.destroyGlyph(r3);vs.destroyGlyph(r4);
    }

    Glyph getMainGlyph(){return g0;}

}

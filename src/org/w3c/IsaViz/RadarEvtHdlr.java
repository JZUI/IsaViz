/*   FILE: RadarEvtHdlr.java
 *   DATE OF CREATION:   11/05/2002
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Mon Feb 10 09:23:24 2003 by Emmanuel Pietriga
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

// import java.util.Vector;
// import java.awt.event.KeyEvent;
import com.xerox.VTM.engine.*;
import com.xerox.VTM.glyphs.*;

public class RadarEvtHdlr extends AppEventHandler {

    Editor application;

    private boolean draggingRegionRect=false;

    RadarEvtHdlr(Editor app){
	this.application=app;
    }

    public void press1(ViewPanel v,int mod,int jpx,int jpy){
// 	if (v.lastGlyphEntered()!=null){
	    Editor.vsm.stickToMouse(application.observedRegion);  //necessarily observedRegion glyph (there is no other glyph)
	    Editor.vsm.activeView.mouse.setSensitivity(false);
	    draggingRegionRect=true;
// 	}
    }

    public void release1(ViewPanel v,int mod,int jpx,int jpy){
	if (draggingRegionRect){
	    Editor.vsm.activeView.mouse.setSensitivity(true);
	    Editor.vsm.unstickFromMouse();
	    draggingRegionRect=false;
	}
    }

    public void click1(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){}

    public void press2(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(1),500);
	application.cameraMoved();
    }
    public void release2(ViewPanel v,int mod,int jpx,int jpy){}
    public void click2(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){}

    public void press3(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.stickToMouse(application.observedRegion);  //necessarily observedRegion glyph (there is no other glyph)
	Editor.vsm.activeView.mouse.setSensitivity(false);
	draggingRegionRect=true;
    }

    public void release3(ViewPanel v,int mod,int jpx,int jpy){
	if (draggingRegionRect){
	    Editor.vsm.activeView.mouse.setSensitivity(true);
	    Editor.vsm.unstickFromMouse();
	    draggingRegionRect=false;
	}
    }

    public void click3(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){}

    public void mouseMoved(ViewPanel v,int jpx,int jpy){}

    public void mouseDragged(ViewPanel v,int mod,int buttonNumber,int jpx,int jpy){
	if (draggingRegionRect){
	    application.updateMainViewFromRadar();
	}
    }

    public void enterGlyph(Glyph g){
	//super.enterGlyph(g);
    }

    public void exitGlyph(Glyph g){
	//super.exitGlyph(g);
    }

    public void Ktype(ViewPanel v,char c,int code,int mod){}

    public void Kpress(ViewPanel v,char c,int code,int mod){
	application.centerRadarView();
    }

    public void Krelease(ViewPanel v,char c,int code,int mod){}

    public void viewActivated(View v){}

    public void viewDeactivated(View v){}

    public void viewIconified(View v){}

    public void viewDeiconified(View v){}

    public void viewClosing(View v){
	Editor.vsm.getView(Editor.radarView).destroyView();
	Editor.rView=null;
    }

}

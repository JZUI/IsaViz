/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/20/2001
 */

package org.w3c.IsaViz;

import java.util.Vector;
import java.awt.geom.Rectangle2D;

import com.xerox.VTM.engine.VirtualSpace;
import com.xerox.VTM.engine.LongPoint;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.glyphs.VRectangle;
import com.xerox.VTM.glyphs.VEllipse;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.VTriangleOr;
import com.xerox.VTM.glyphs.VPath;


/*ISV command: paste*/

class ISVPaste extends ISVCommand {

    Editor application;
    IResource[] srl;  //list of IResource
    ILiteral[] sll;   //list of ILiteral
    IProperty[] spl;  //list of IProperty

    IResource[] rl;  //list of IResource
    ILiteral[] ll;   //list of ILiteral
    IProperty[] pl;  //list of IProperty

    long mx,my;  //coords where new selection should be placed

    ISVPaste(Editor e,Vector props,Vector ress,Vector lits,long x,long y){
	application=e;
	srl=(IResource[])ress.toArray(new IResource[ress.size()]);
	sll=(ILiteral[])lits.toArray(new ILiteral[lits.size()]);
	spl=(IProperty[])props.toArray(new IProperty[props.size()]);
	rl=new IResource[ress.size()];
	ll=new ILiteral[lits.size()];
	pl=new IProperty[props.size()];
	this.mx=x;
	this.my=y;
    }

    void _do(){
	//compute the geom center of the set of elements in clipboard
	Glyph[] gList=new Glyph[srl.length+sll.length];
	int i=0;
	int j=0;
	while (i<srl.length){gList[j]=srl[i].getGlyph();i++;j++;}
	i=0;
	while (i<sll.length){gList[j]=sll[i].getGlyph();i++;j++;}
	LongPoint gc=VirtualSpace.getGlyphSetGeometricalCenter(gList);
	//duplicate and paste nodes
	pasteResources(gc);
	pasteLiterals(gc);
	//duplicate and paste edges
	pasteProperties(gc);
	//don't need these any longer
	srl=null;
	sll=null;
	spl=null;
    }

    void pasteResources(LongPoint lp){//have to generate new unqiue URIs or IDs for resources
	for (int i=0;i<srl.length;i++){
	    IResource r=new IResource();
	    VEllipse sel=(VEllipse)srl[i].getGlyph();
	    VEllipse el=new VEllipse(mx+sel.vx-lp.x,my+sel.vy-lp.y,0,sel.getWidth(),sel.getHeight(),ConfigManager.resourceColorF);
	    r.setGlyph(el);
	    Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);
	    el.setHSVbColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);
	    if (srl[i].isAnon()){//if source resource was anonymous, create a new anon resource
		r.setAnon(true);
		r.setAnonymousID(application.nextAnonymousID());
		application.resourcesByURI.put(r.getIdent(),r);
	    }
	    else {//append '--copy-X' to the end of the URI to prevent conflict with existing resources
		//but first test if the source still exist (this will not be the case if we did a CUT or
		//if the source was deleted prior to paste after a COPY). If it does ntot, there will
		//not be any conflict, in which case we do not append --copy-X
		r.setNamespace(srl[i].getNamespace());//(X is the lowest integer possible, beginning at 1)
		r.setLocalname(srl[i].getLocalname());
		int j=1;
		while (application.resourcesByURI.containsKey(r.getIdent())){
		    r.setLocalname(srl[i].getLocalname()+"--copy-"+String.valueOf(j));
		    j++;
		}
		application.resourcesByURI.put(r.getIdent(),r);
	    }
	    VText g=new VText(el.vx,el.vy,0,ConfigManager.resourceColorTB,r.getIdent());
	    Editor.vsm.addGlyph(g,Editor.mainVirtualSpace);
	    r.setGlyphText(g);
	    //here we use an ugly hack to compute the position of text and size of ellipse because VText.getBounds() is not yet available (computed in another thread at an unknown time) - so we access the VTM view's Graphics object to manually compute the bounds of the text. Very ugly. Shame on me. But right now there is no other way.
	    Rectangle2D r2d=Editor.vsm.getView(Editor.mainView).getGraphicsContext().getFontMetrics().getStringBounds(r.getIdent(),Editor.vsm.getView(Editor.mainView).getGraphicsContext());
	    el.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*Math.round(r2d.getWidth())/2
	    //ellipse should always have width > height  (just for aesthetics)
	    if (el.getWidth()<(1.5*el.getHeight())){el.setWidth(Math.round(1.5*el.getHeight()));}
	    //center VText in ellipse
	    g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
	    if (r.isAnon() && !Editor.SHOW_ANON_ID){Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).hide(g);}
	    if (srl[i].isCommented()){application.commentNode(r,true);}
	    rl[i]=r;
	}
    }

    void pasteLiterals(LongPoint lp){//pretty straightforward since there cannot be any conflict
	for (int i=0;i<sll.length;i++){
	    ILiteral l=new ILiteral();
	    VRectangle sg=(VRectangle)sll[i].getGlyph();
	    VRectangle g=new VRectangle(mx+sg.vx-lp.x,my+sg.vy-lp.y,0,sg.getWidth(),sg.getHeight(),ConfigManager.literalColorF);
	    Editor.vsm.addGlyph(g,Editor.mainVirtualSpace);
	    g.setHSVbColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);
	    l.setGlyph(g);
	    l.setLanguage(sll[i].getLang());
	    l.setEscapeXMLChars(sll[i].escapesXMLChars());
	    application.setLiteralValue(l,sll[i].getValue());
	    application.literals.add(l);
	    if (sll[i].isCommented()){application.commentNode(l,true);}
	    ll[i]=l;  //replace object to be copied by its copy
	}
    }

    void pasteProperties(LongPoint lp){//builds the property and creates all dependencies w.r.t subject and object
	for (int i=0;i<spl.length;i++){
	    IProperty p=application.addProperty(spl[i].getNamespace(),spl[i].getLocalname());
	    //identify subject and object linked to the copied predicate and assign dependencies
	    IResource subject=this.getCopy(spl[i].getSubject());
	    INode object=this.getCopy(spl[i].getObject());
	    p.setSubject(subject);
	    subject.addOutgoingPredicate(p);
	    if (object instanceof IResource){
		p.setObject((IResource)object);
		((IResource)object).addIncomingPredicate(p);
	    }
	    else {//instanceof ILiteral (or we have an error)
		p.setObject((ILiteral)object);
		((ILiteral)object).setIncomingPredicate(p);
		if (p.getIdent().equals(Editor.RDFS_NAMESPACE_URI+"label")){
		    //if property is rdfs:label, set label for the resource
		    subject.setLabel(((ILiteral)object).getValue());
		    subject.getGlyphText().setText(subject.getLabel());
		}
	    }
	    //clone glyphs
	    //path
	    VPath pt=VPath.duplicateVPath((VPath)spl[i].getGlyph(),mx-lp.x,my-lp.y);
	    pt.setHSVColor(ConfigManager.prpBh,ConfigManager.prpBs,ConfigManager.prpBv);
	    Editor.vsm.addGlyph(pt,Editor.mainVirtualSpace);
	    //arrow
	    VTriangleOr str=spl[i].getGlyphHead();
	    VTriangleOr tr=new VTriangleOr(mx+str.vx-lp.x,my+str.vy-lp.y,0,Editor.ARROW_HEAD_SIZE,ConfigManager.propertyColorB,str.getOrient());
	    Editor.vsm.addGlyph(tr,Editor.mainVirtualSpace);
	    //text
	    VText stx=spl[i].getGlyphText();
	    VText tx=new VText(mx+stx.vx-lp.x,my+stx.vy-lp.y,0,ConfigManager.propertyColorT,stx.getText());
	    Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
	    p.setGlyph(pt,tr);
	    p.setGlyphText(tx);
	    application.geomMngr.adjustPaths(p.getSubject());
	    application.geomMngr.adjustPaths(p.getObject());
	    if (spl[i].isCommented()){application.commentPredicate(p,true);}
	    pl[i]=p;
	}
    }

    //given a source resource, returns its copy 
    IResource getCopy(IResource r){
	for (int i=0;i<srl.length;i++){
	    if (r==srl[i]){return rl[i];}
	}
	return null;
    }

    //given a source node (resource or literal), returns its copy (not for IProperty)
    INode getCopy(INode n){
	for (int i=0;i<srl.length;i++){
	    if (n==srl[i]){return rl[i];}
	}
	for (int i=0;i<sll.length;i++){
	    if (n==sll[i]){return ll[i];}
	}
	return null;
    }

    void _undo(){//when calling undo, rl,ll and pl contain the clones
	for (int i=pl.length-1;i>=0;i--){
	    application.deleteProperty(pl[i]);
	}
	for (int i=ll.length-1;i>=0;i--){
	    application.deleteLiteral(ll[i]);
	}
	for (int i=rl.length-1;i>=0;i--){
	    application.deleteResource(rl[i]);
	}
    }

}

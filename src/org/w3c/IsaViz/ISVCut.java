/*   FILE: ISVCut.java
 *   DATE OF CREATION:   12/20/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Jan 22 17:49:49 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import java.util.Vector;

/*ISV command: cut*/

class ISVCut extends ISVCommand {

    Editor application;
    IResource[] rl;  //list of IResource
    ILiteral[]  ll;  //list of ILiteral
    IProperty[] pl;  //list of IProperty

    ISVCut(Editor e,Vector props,Vector ress,Vector lits){
	application=e;
	rl=(IResource[])ress.toArray(new IResource[ress.size()]);
	ll=(ILiteral[])lits.toArray(new ILiteral[lits.size()]);
	Vector v=new Vector();
	IProperty p;
	for (int i=0;i<props.size();i++){//copy only predicates for which both subject and
	    p=(IProperty)props.elementAt(i);// object have been selected for the copy, even
	    if (p.getSubject().isSelected() && p.getObject().isSelected()){v.add(p);} //if
	    //the edge itself is selected. Copying the others would create pending edges
	}
	pl=(IProperty[])v.toArray(new IProperty[v.size()]);
    }

    void _do(){
	application.copiedResources=new Vector(java.util.Arrays.asList(rl));
	application.copiedLiterals=new Vector(java.util.Arrays.asList(ll));
	application.copiedPredicates=new Vector(java.util.Arrays.asList(pl));
	for (int i=0;i<pl.length;i++){
	    application.deleteProperty(pl[i]);
	}
	for (int i=0;i<rl.length;i++){
	    application.deleteResource(rl[i]);
	}
	for (int i=0;i<ll.length;i++){
	    application.deleteLiteral(ll[i]);
	}
    }

    void _undo(){
	String vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getName();
	for (int i=0;i<rl.length;i++){//put back IResources
	    Editor.vsm.addGlyph(rl[i].getGlyph(),vs);
	    Editor.vsm.addGlyph(rl[i].getGlyphText(),vs);
	    if (!application.resourcesByURI.containsKey(rl[i].getIdent())){
		application.resourcesByURI.put(rl[i].getIdent(),rl[i]);
	    }
	    else {
		application.errorMessages.append("Undo: A conflict occured when trying to restore resource '"+rl[i].getIdent()+"'.\nThe model probably contains two nodes with this URI.\n");application.reportError=true;
	    }
	}
	for (int i=0;i<ll.length;i++){//put back ILiterals
	    Editor.vsm.addGlyph(ll[i].getGlyph(),vs);
	    Editor.vsm.addGlyph(ll[i].getGlyphText(),vs);
	    application.literals.add(ll[i]);
	}
	INode n;
	for (int i=0;i<pl.length;i++){//put back IProperties and link them back to resources and literals
	    Editor.vsm.addGlyph(pl[i].getGlyph(),vs);
	    Editor.vsm.addGlyph(pl[i].getGlyphHead(),vs);
	    Editor.vsm.addGlyph(pl[i].getGlyphText(),vs);
	    if (application.propertiesByURI.containsKey(pl[i].getIdent())){
		Vector v=(Vector)application.propertiesByURI.get(pl[i].getIdent());
		v.add(pl[i]);
	    }
	    else {
		Vector v=new Vector();
		v.add(pl[i]);
		application.propertiesByURI.put(pl[i].getIdent(),v);
	    }
	    pl[i].getSubject().addOutgoingPredicate(pl[i]);
	    n=pl[i].getObject();
	    if (n instanceof IResource){((IResource)n).addIncomingPredicate(pl[i]);}
	    else {((ILiteral)n).setIncomingPredicate(pl[i]);}
	}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }


}

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

import java.util.Vector;
import java.awt.event.KeyEvent;
import java.awt.Point;
import com.xerox.VTM.engine.*;
import com.xerox.VTM.glyphs.*;

/*class that receives the events sent from VTM views (include mouse click, entering object,...)*/
/*It inherits VTM's AppEventHandler*/

public class EditorEvtHdlr extends AppEventHandler{

    Editor application;

    long lastX,lastY,lastJPX,lastJPY;    //remember last mouse coords to compute translation  (dragging)
    long x1,y1,x2,y2;                     //remember last mouse coords to display selection rectangle (dragginf)

    int mode=SINGLE_SELECTION_MODE;  //identifies the current interaction mode (selecting, creating,...) value is in MODE below
    static final int SINGLE_SELECTION_MODE=0;
    static final int REGION_SELECTION_MODE=1;
    static final int CREATE_RESOURCE_MODE=2;
    static final int CREATE_PREDICATE_MODE=3;
    static final int CREATE_LITERAL_MODE=4;
    static final int EDIT_PROPS_MODE=5;
    static final int REGION_ZOOM_MODE=6;
    static final int COMMENT_SINGLE_MODE=7;
    static final int COMMENT_REGION_MODE=8;
    static final int UNCOMMENT_SINGLE_MODE=9;
    static final int UNCOMMENT_REGION_MODE=10;
    static final int MOVE_RESIZE_MODE=11;
    static final int PASTE_MODE=12;

    boolean CREATE_PREDICATE_STARTED=false;
    Vector pathForNewProperty; //list of LongPoints to create the VPath
    Vector tempSegments; //store segments temporarily representing the path (before we compute the VPath)
    IResource subjectForNewProperty; //remember first object we clicked on when we began creating the property (will be the subject)
    
    boolean resizing=false;  //true when resizing an object in the graph (so that release1 knows it has to do something)
    boolean moving=false; //true when moving a resource or literal in the graph (so that release1 knows it has to do something)
    boolean movingText=false; //true when moving a property's, resource's or literal's text in the graph (so that release1 knows it has to do something)
    boolean editingPath=false; //true when editing a path (by a moving a handle) (so that release1 knows it has to do something)
    ControlPoint whichHandle=null;  //set when clicking in a handle so that we do not have to retrieve it each time mouseDragged is called
    
    int selectWhat=NODES_ONLY;  //when selecting entities in a region, select just NODES (resources+literals) or EDGES (properties)
    static final int NODES_ONLY=0;
    static final int EDGES_ONLY=1;

    EditorEvtHdlr(Editor appli){
	application=appli;
    }

    public void press1(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.getActiveView().setStatusBarText("");
	Glyph g=v.lastGlyphEntered();
	switch (mode){
	case SINGLE_SELECTION_MODE:{
	    if (g!=null){
		if (mod<2){application.unselectLastSelection();}//CTRL not pressed
		if (mod==0 || mod==2){select(g,false);}//SHIFT not pressed
		else {select(g,true);}
	    }
	    else {
		Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
		if (vc!=null){//there is a text under the mouse
		    if (mod<2){application.unselectLastSelection();}
		    //we might accidentally have selected several texts - just take the first one in the list
		    if (mod==0 || mod==2){select((Glyph)vc.firstElement(),false);}//SHIFT not pressed
		    else {select((Glyph)vc.firstElement(),true);}
		    
		}
		else if ((vc=v.getMouse().getIntersectingPaths(Editor.vsm.getActiveCamera()))!=null){//no text under mouse, but there might be a path
		    if (mod<2){application.unselectLastSelection();}
		    if (mod==0 || mod==2){select((Glyph)vc.firstElement(),false);}//SHIFT not pressed
		    else {select((Glyph)vc.firstElement(),true);}
		}
		else {//unselect everything if user clicks in empty region without Ctrl
		    if (mod<2){application.unselectLastSelection();}
		}
	    }
	    break;
	}
	case REGION_SELECTION_MODE:{
	    if (mod<2){application.unselectLastSelection();}
	    x1=v.getMouse().vx;
	    y1=v.getMouse().vy;
	    v.setDrawRect(true);
	    break;
	}
	case REGION_ZOOM_MODE:{
	    x1=v.getMouse().vx;
	    y1=v.getMouse().vy;
	    v.setDrawRect(true);
	    break;
	}
	case CREATE_RESOURCE_MODE:{
	    if (g==null){//we do not want to create a new resource when the user clicks inside something else
		application.createNewResource(v.getMouse().vx,v.getMouse().vy);
	    }
	    else {
		if (g.getType()!=null && g.getType().equals(Editor.resEllipseType)){
		    application.propsp.updateDisplay((INode)g.getOwner());
		}
	    }
	    break;
	}
	case CREATE_PREDICATE_MODE:{
	    if ((application.selectedPropertyConstructorNS!=null) && (application.selectedPropertyConstructorLN!=null)){
		if (g!=null){//we are either beginning or finishing the creation of a property
		    Object o=g.getOwner();
		    if (CREATE_PREDICATE_STARTED){//already started - means we are clicking on the object for this property
			if ((o instanceof IResource) || (o instanceof ILiteral)){//object can be a literal or a resource
			    CREATE_PREDICATE_STARTED=false;
			    INode n=(INode)o;    //object of the statement
			    pathForNewProperty.add(new LongPoint(n.getGlyph().vx,n.getGlyph().vy));
			    application.createNewProperty(subjectForNewProperty,n,pathForNewProperty);
			    subjectForNewProperty=null;
			    VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
			    for (int i=0;i<tempSegments.size();i++){//get rid of temporary segments representing the path
				vs.destroyGlyph((Glyph)tempSegments.elementAt(i));
			    }
			    tempSegments=null;
			}
			else {Editor.vsm.getActiveView().setStatusBarText("Object must be a resource or a literal");}
		    }
		    else {//not started yet - means we are clicking on the subject for this property
			if (o instanceof IResource){//subject must be a resource
			    Editor.vsm.getActiveView().setStatusBarText("Specify intermediate path points (click in empty regions) or select this statement's object (click in a node)");
			    CREATE_PREDICATE_STARTED=true;
			    subjectForNewProperty=(IResource)o;
			    pathForNewProperty=new Vector();
			    tempSegments=new Vector();
			    pathForNewProperty.add(new LongPoint(subjectForNewProperty.getGlyph().vx,subjectForNewProperty.getGlyph().vy));
			}
			else {Editor.vsm.getActiveView().setStatusBarText("Subject must be a resource");}
		    }
		}
		else {//we are drawing the property's edge using a broken line 
		    //that will be converted in a VPath when it is finished
		    if (CREATE_PREDICATE_STARTED){//if we did not select a subject for this property, don't do anything
			Editor.vsm.getActiveView().setStatusBarText("Specify intermediate path points (click in empty regions) or select this statement's object (click in a node)");
			LongPoint lp=(LongPoint)pathForNewProperty.lastElement();
			LongPoint mlp=v.getMouse().getLocation();
			pathForNewProperty.add(mlp);
			long x=(lp.x+mlp.x)/2;
			long y=(lp.y+mlp.y)/2;
			long w=(mlp.x-lp.x)/2;
			long h=(-mlp.y+lp.y)/2;
			VSegment s=new VSegment(x,y,0,w,h,ConfigManager.propertyColorB);
			Editor.vsm.addGlyph(s,Editor.mainVirtualSpace);
			tempSegments.add(s);
		    }
		    else {Editor.vsm.getActiveView().setStatusBarText("You must select a subject first (press right mouse button to cancel)");}
		}
	    }
	    else {Editor.vsm.getActiveView().setStatusBarText("Select a property from the list in the Property tab");}
	    break;
	}
	case CREATE_LITERAL_MODE:{
	    if (g==null){//we do not want to create a new resource when the user clicks inside something else
		application.createNewLiteral(v.getMouse().vx,v.getMouse().vy);
	    }
	    else {
		if (g.getType()!=null && g.getType().equals(Editor.litRectType)){
		    application.propsp.updateDisplay((INode)g.getOwner());
		}
	    }
	    break;
	}
	case MOVE_RESIZE_MODE:{
	    Vector vt=v.getGlyphsUnderMouse(); //give priority to resizing handles
	    if (vt.size()>1){                  //in case the mouse is inside more than one glyph 
		Glyph g3;                      //if mouse inside several resizing handles, take
		for (int i=vt.size()-1;i>=0;i--){//last one entered
		    g3=(Glyph)vt.elementAt(i);
		    if (g3.getType().startsWith("rsz")){
			g=g3;
			break;
		    }
		}
	    }
	    if (g!=null){
		String type=g.getType();
		if (type.startsWith("rsz")){//resizing an object (ellipse, rectangle or path)
		    Editor.vsm.stickToMouse(g);
		    if (type.equals("rszp")){//path
			whichHandle=(ControlPoint)g.getOwner();editingPath=true;
			if (mod==2){whichHandle.dragSiblings(true);}
			//hide the VPath (only display the broken line)
			Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).hide(whichHandle.getPath());
		    }//ellipse or rectangle
		    else {
			v.getMouse().setSensitivity(false);
			resizing=true;
		    }
		}
		else if (type.charAt(3)=='G'){//editing an INode's main glyph (display little black rectangles that will allow the actual resizing operation)
		    if (type.equals(Editor.resEllipseType)){
			if (mod>=2){
			    Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
			    if (vc!=null){//there is a text under the mouse
				Glyph g2=(Glyph)vc.firstElement();
				if (g2.getType().equals(Editor.resTextType)){Editor.vsm.stickToMouse(g2);movingText=true;} //move VText if Ctrl is down
			    }
			}
			else {
			    application.geomMngr.initResourceResizer((IResource)g.getOwner());
			    moving=true;
			    Editor.vsm.stickToMouse(g);  //will be unsticked from mouse if we click (do not drag, meaning we want to resize, not move)
			}
		    }
		    else if (type.equals(Editor.litRectType)){
			if (mod>=2){
			    Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
			    if (vc!=null){//there is a text under the mouse
				Glyph g2=(Glyph)vc.firstElement();
				if (g2.getType().equals(Editor.litTextType)){Editor.vsm.stickToMouse(g2);movingText=true;} //move VText if Ctrl is down
			    }
			}
			else {
			    application.geomMngr.initLiteralResizer((ILiteral)g.getOwner());
			    moving=true;
			    Editor.vsm.stickToMouse(g);  //will be unsticked from mouse if we click (do not drag, meaning we want to resize, not move)
			}
		    }
		}
		else if (type.equals(Editor.propHeadType)){//move/resize the corresponding VPath if clicking on the path's head
		    application.geomMngr.initPropertyResizer((IProperty)g.getOwner());
		}
	    }
	    else {
		Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
		if (vc!=null){//there is a text under the mouse
		    Glyph g2=(Glyph)vc.firstElement();
		    if (g2.getType().equals(Editor.propTextType)){Editor.vsm.stickToMouse(g2);movingText=true;Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).onTop(g2);} //move only if it is a Property's VText
		    else if (mod>=2 && (g2.getType().equals(Editor.resTextType) || g2.getType().equals(Editor.litTextType))){Editor.vsm.stickToMouse(g2);movingText=true;} //or if it is a resouce's or literal's text and Ctrl is down
		}
		else if ((vc=v.getMouse().getIntersectingPaths(Editor.vsm.getActiveCamera()))!=null){
		    Glyph g2=(Glyph)vc.firstElement();
		    if (g2.getType().equals(Editor.propPathType)){application.geomMngr.initPropertyResizer((IProperty)g2.getOwner());}
		}
		else {application.geomMngr.destroyLastResizer();}
	    }
	    break;
	}
	case PASTE_MODE:{
	    application.pasteSelection(v.getMouse().vx,v.getMouse().vy);
	    break;
	}
	case COMMENT_REGION_MODE:{
	    x1=v.getMouse().vx;
	    y1=v.getMouse().vy;
	    v.setDrawRect(true);
	    break;
	}
	case UNCOMMENT_REGION_MODE:{
	    x1=v.getMouse().vx;
	    y1=v.getMouse().vy;
	    v.setDrawRect(true);
	    break;
	}
	case COMMENT_SINGLE_MODE:{
	    if (g!=null){
		if (g.getType().equals(Editor.litRectType) || g.getType().equals(Editor.resEllipseType)){
		    application.commentNode((INode)g.getOwner(),true);
		}
		else if (g.getType().equals(Editor.propHeadType)){application.commentPredicate((IProperty)g.getOwner(),true);}
	    }
	    else {
		Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
		if (vc!=null){//there is a text under the mouse
		    g=(Glyph)vc.firstElement();
		    if (g.getType().startsWith("prd")){application.commentPredicate((IProperty)g.getOwner(),true);}
		}
		else if ((vc=v.getMouse().getIntersectingPaths(Editor.vsm.getActiveCamera()))!=null){//no text under mouse, but there might be a path
		    g=(Glyph)vc.firstElement();
		    if (g.getType().startsWith("prd")){application.commentPredicate((IProperty)g.getOwner(),true);}
		}
	    }
	    break;
	}
	case UNCOMMENT_SINGLE_MODE:{
	    if (g!=null){
		if (g.getType().equals(Editor.litRectType) || g.getType().equals(Editor.resEllipseType)){
		    application.commentNode((INode)g.getOwner(),false);
		}
		else if (g.getType().equals(Editor.propHeadType)){application.commentPredicate((IProperty)g.getOwner(),false);}
	    }
	    else {
		Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
		if (vc!=null){//there is a text under the mouse
		    g=(Glyph)vc.firstElement();
		    if (g.getType().startsWith("prd")){application.commentPredicate((IProperty)g.getOwner(),false);}
		}
		else if ((vc=v.getMouse().getIntersectingPaths(Editor.vsm.getActiveCamera()))!=null){//no text under mouse, but there might be a path
		    g=(Glyph)vc.firstElement();
		    if (g.getType().startsWith("prd")){application.commentPredicate((IProperty)g.getOwner(),false);}
		}
	    }
	    break;
	}
	}
    }

    public void release1(ViewPanel v,int mod,int jpx,int jpy){
	switch (mode){
	case REGION_SELECTION_MODE:{
	    v.setDrawRect(false);
	    x2=v.getMouse().vx;
	    y2=v.getMouse().vy;
	    Vector selectedGlyphs=Editor.vsm.getGlyphsInRegion(x1,y1,x2,y2,Editor.mainVirtualSpace,VirtualSpaceManager.VISIBLE_GLYPHS);
	    if (selectedGlyphs!=null){
		Glyph g; 
		INode n;
		for (int i=0;i<selectedGlyphs.size();i++){
		    g=(Glyph)selectedGlyphs.elementAt(i);
		    //we are only selecting based on the main glyph for each kind of graph entity (identified by G as its 4th char)
		    if (g.getType().charAt(3)=='G'){
			n=(INode)g.getOwner();
			//select only appropriate entities
			if (selectWhat==NODES_ONLY){
			    if (g.getType().equals(Editor.litRectType)){
				application.selectLiteral((ILiteral)n,true);
				if (mod==1 || mod==3){//SHIFT is down - select nodes/edges associated with the actual selection
				    application.selectPropertiesOfLiteral((ILiteral)n);
				}
			    }
			    else if (g.getType().equals(Editor.resEllipseType)){
				application.selectResource((IResource)n,true);
				if (mod==1 || mod==3){//SHIFT is down - select nodes/edges associated with the actual selection
				    application.selectPropertiesOfResource((IResource)n);
				}
			    }
			}
			else if (selectWhat==EDGES_ONLY && g.getType().equals(Editor.propPathType)){
			    application.selectPredicate((IProperty)n,true);
			    if (mod==1 || mod==3){//SHIFT is down - select nodes/edges associated with the actual selection
				    application.selectNodesOfProperty((IProperty)n);
			    }
			}
		    }
		}
	    }
	    break;
	}
	case REGION_ZOOM_MODE:{
	    v.setDrawRect(false);
	    x2=v.getMouse().vx;
	    y2=v.getMouse().vy;
	    Editor.vsm.centerOnRegion(Editor.vsm.getActiveCamera(),500,x1,y1,x2,y2);
	    break;
	}
	case COMMENT_REGION_MODE:{
	    v.setDrawRect(false);
	    x2=v.getMouse().vx;
	    y2=v.getMouse().vy;
	    Vector selectedGlyphs=Editor.vsm.getGlyphsInRegion(x1,y1,x2,y2,Editor.mainVirtualSpace,VirtualSpaceManager.VISIBLE_GLYPHS);
	    if (selectedGlyphs!=null){
		Glyph g; 
		for (int i=0;i<selectedGlyphs.size();i++){
		    g=(Glyph)selectedGlyphs.elementAt(i);
		    //we are only selecting based on the main glyph for each kind of graph entity (identified by G as its 4th char)
		    if (g.getType().charAt(3)=='G'){
			//select only resources and literals  (properties will be commented out by these if necessary)
			if (g.getType().equals(Editor.litRectType) || g.getType().equals(Editor.resEllipseType)){application.commentNode((INode)g.getOwner(),true);}
		    }
		}
	    }
	    break;
	}
	case UNCOMMENT_REGION_MODE:{
	    v.setDrawRect(false);
	    x2=v.getMouse().vx;
	    y2=v.getMouse().vy;
	    Vector selectedGlyphs=Editor.vsm.getGlyphsInRegion(x1,y1,x2,y2,Editor.mainVirtualSpace,VirtualSpaceManager.VISIBLE_GLYPHS);
	    if (selectedGlyphs!=null){
		Glyph g; 
		for (int i=0;i<selectedGlyphs.size();i++){
		    g=(Glyph)selectedGlyphs.elementAt(i);
		    //we are only selecting based on the main glyph for each kind of graph entity (identified by G as its 4th char)
		    if (g.getType().charAt(3)=='G'){
			//select only resources and literals  (properties will be commented out by these if necessary)
			if (g.getType().equals(Editor.litRectType) || g.getType().equals(Editor.resEllipseType)){application.commentNode((INode)g.getOwner(),false);}
		    }
		}
	    }
	    break;
	}
	case MOVE_RESIZE_MODE:{
	    if (resizing){
		resizing=false;
		v.getMouse().setSensitivity(true);
		application.geomMngr.endResize();
	    }
	    else if (moving){
		moving=false;
		application.geomMngr.endMove();
	    }
	    else if (movingText){Editor.vsm.unstickFromMouse();movingText=false;}
	    else if (editingPath){
		//first check that we have not changed the subject/object of the statement
		Glyph g3=(Glyph)v.getMouse().getStickedGlyphs().firstElement();
		if (g3!=null && g3.getType().equals("rszp")){
		    ControlPoint cp=(ControlPoint)g3.getOwner();
		    if (cp.type==ControlPoint.START_POINT){
			Vector gum=v.getMouse().getGlyphsUnderMouse();
			Glyph subj=cp.owner.prop.getSubject().getGlyph();
			IResource r;
			if (!gum.contains(subj) && ((r=insideAnIResource(gum))!=null)){//mouse is being released in a node that is not the original subject for this predicate
			    Editor.changePropertySubject(cp.owner.prop,r);
			}
		    }
		    else if (cp.type==ControlPoint.END_POINT){
			Vector gum=v.getMouse().getGlyphsUnderMouse();
			Glyph obj=cp.owner.prop.getObject().getGlyph();
			INode n;
			if (!gum.contains(obj) && ((n=insideAnINode(gum))!=null)){//mouse is being released in a node that is not the original subject for this predicate
			    Editor.changePropertyObject(cp.owner.prop,n);
			}
		    }
		}
		//then get rid of the resizer (must do it after so that start and end points get adjusted w.r.t the new subject/object if changed)
		Editor.vsm.unstickFromMouse();editingPath=false;
		application.geomMngr.updatePathAfterResize();
		Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).show(whichHandle.getPath());
		whichHandle.dragSiblings(false);
		whichHandle=null;
	    }
	    break;
	}
	}

    }

    public void click1(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){
	switch (mode){
	case SINGLE_SELECTION_MODE:{//if double clicking on a resource, try to display its content in a web browser
	    if (clickNumber==2){
		Glyph g=v.lastGlyphEntered();
		if (g!=null && g.getType().equals(Editor.resEllipseType)){
		    application.displayURLinBrowser((IResource)g.getOwner());
		}
	    }
	    break;
	}
	}
    }

    public void press2(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.getActiveView().setStatusBarText("");
	Glyph g=v.lastGlyphEntered();
	try {
	    if (g==null){
		application.ctmnMngr.displayMiscMenu(v,jpx,jpy);
	    }
// 	    else {
// 		Object o=g.getOwner();
// 		if (o instanceof IResource){
// 		    application.ctmnMngr.displayResourceMenu((IResource)o,v,jpx,jpy);
// 		}
// 		else if (o instanceof ILiteral){
// 		    application.ctmnMngr.displayLiteralMenu((ILiteral)o,v,jpx,jpy);
// 		}
// 	    }
	}
	catch (NullPointerException ex){System.err.println("Error: eventHandler.press2(): "+ex);}
    }
    public void release2(ViewPanel v,int mod,int jpx,int jpy){}
    public void click2(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){}

    public void press3(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.getActiveView().setStatusBarText("");
	lastJPX=jpx;
	lastJPY=jpy;
	v.setDrawDrag(true);
	Editor.vsm.activeView.mouse.setSensitivity(false);  //because we would not be consistent  (when dragging the mouse, we computeMouseOverList, but if there is an anim triggered by {X,Y,A}speed, and if the mouse is not moving, this list is not computed - so here we choose to disable this computation when dragging the mouse with button 3 pressed)
    }

    public void release3(ViewPanel v,int mod,int jpx,int jpy){
	Editor.vsm.animator.Xspeed=0;
	Editor.vsm.animator.Yspeed=0;
	Editor.vsm.animator.Aspeed=0;
	v.setDrawDrag(false);
	Editor.vsm.activeView.mouse.setSensitivity(true);
    }

    public void click3(ViewPanel v,int mod,int jpx,int jpy,int clickNumber){
	Glyph g=v.lastGlyphEntered();
	if (mode==CREATE_PREDICATE_MODE && CREATE_PREDICATE_STARTED){
	    cancelStartedPredicate();
	}
	else {
	    if (g!=null){Editor.vsm.centerOnGlyph(g,v.cams[0],500);}
	    else {//we might be clicking on a predicate (no enter/exit event is fired when the mouse overlaps a VPath or a VText, test has to be done manually)
		Vector vc=v.getMouse().getIntersectingTexts(Editor.vsm.getActiveCamera());
		if (vc!=null){//there is a text under the mouse
		    Editor.vsm.centerOnGlyph((Glyph)vc.firstElement(),Editor.vsm.getActiveCamera(),500);
		}
		else if ((vc=v.getMouse().getIntersectingPaths(Editor.vsm.getActiveCamera()))!=null){
		    //no text under mouse, but there might be a path
		    Editor.vsm.centerOnGlyph((Glyph)vc.firstElement(),Editor.vsm.getActiveCamera(),500);
		}
	    }
	}
    }

    public void mouseMoved(ViewPanel v,int jpx,int jpy){

    }

    public void mouseDragged(ViewPanel v,int mod,int buttonNumber,int jpx,int jpy){
	if (buttonNumber==3){
	    Camera c=Editor.vsm.getActiveCamera();
	    if (mod==1) {
		Editor.vsm.animator.Xspeed=0;
		Editor.vsm.animator.Yspeed=0;
		Editor.vsm.animator.Aspeed=(lastJPY-jpy)*(long)((c.focal+c.altitude)/c.focal)/50;  //50 is just a speed factor (too fast otherwise)
	    }
	    else {
		Editor.vsm.animator.Aspeed=0;
		Editor.vsm.animator.Xspeed=(jpx-lastJPX)*(long)((c.focal+c.altitude)/c.focal)/50;
		Editor.vsm.animator.Yspeed=(lastJPY-jpy)*(long)((c.focal+c.altitude)/c.focal)/50;	
	    }
	}
	else if (buttonNumber==1){//dragging a resizer handle
	    if (resizing){application.geomMngr.resize(v.lastGlyphEntered());}  //for both we could store lastGlyphEntered.getowner()
	    else if (moving){application.geomMngr.move(v.lastGlyphEntered());} //instead of accessing it each time
	    else if (editingPath){whichHandle.update();}
	}
// 	else if (buttonNumber==2){System.err.println(v.getMouse().glyphsUnderMouse[0]);}
    }

    public void enterGlyph(Glyph g){
	super.enterGlyph(g);  //for border color
	//if entering a resource or literal, display its value in the status bar text
	try {if (g.getType().charAt(3)=='G' && g.getOwner()!=null){
	    Editor.vsm.getActiveView().setStatusBarText(((INode)g.getOwner()).getText());}
	}
	catch (StringIndexOutOfBoundsException ex){}
    }

    public void exitGlyph(Glyph g){
	super.exitGlyph(g);  //for border color
// 	Editor.vsm.getActiveView().setStatusBarText("");
    }

    public void Ktype(ViewPanel v,char c,int code,int mod){}

    public void Kpress(ViewPanel v,char c,int code,int mod){
	if (mod==0){//pressing no modifier
	    if (code==java.awt.event.KeyEvent.VK_DELETE){application.deleteSelectedEntities();}
	    else if (c=='+'){
		Glyph gl;
		if (mode==MOVE_RESIZE_MODE && v.lastGlyphEntered()!=null && (gl=mouseInsideAPathCP(v.getGlyphsUnderMouse()))!=null){
		    application.geomMngr.insertSegmentInPath(gl);
		}
	    }
	    else if (c=='-'){
		Glyph gl;
		if (mode==MOVE_RESIZE_MODE && v.lastGlyphEntered()!=null && (gl=mouseInsideAPathCP(v.getGlyphsUnderMouse()))!=null){
		    application.geomMngr.deleteSegmentInPath(gl);
		}
	    }
	}
	else if (mod==2){
	    if (code==KeyEvent.VK_Z){application.undo();}
	    else if (code==KeyEvent.VK_X){application.cutSelection();}
	    else if (code==KeyEvent.VK_C){application.copySelection();}
	    else if (code==KeyEvent.VK_V){application.pasteSelection(v.getMouse().vx,v.getMouse().vy);}
	    else if (code==KeyEvent.VK_A){application.selectAllNodes();}
	    else if (code==KeyEvent.VK_G){Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),500);}
	    else if (code==KeyEvent.VK_E){application.showErrorMessages();}
	    else if (code==KeyEvent.VK_N){application.promptReset();}
	    else if (code==KeyEvent.VK_O){application.openProject();}
	    else if (code==KeyEvent.VK_S){application.saveProject();}
// 	    else if (code==KeyEvent.VK_Q){application.exit();} //BETTER LEAVE IT COMMENTED - WE DO NOT HAVE ANY WARNING
	}
	else if (mod==1){
	    if (c=='+'){
		Glyph gl;
		if (mode==MOVE_RESIZE_MODE && v.lastGlyphEntered()!=null && (gl=mouseInsideAPathCP(v.getGlyphsUnderMouse()))!=null){
		    application.geomMngr.insertSegmentInPath(gl);
		}
	    }
	    else if (c=='-'){
		Glyph gl;
		if (mode==MOVE_RESIZE_MODE && v.lastGlyphEntered()!=null && (gl=mouseInsideAPathCP(v.getGlyphsUnderMouse()))!=null){
		    application.geomMngr.deleteSegmentInPath(gl);
		}
	    }
	}
    }

    public void Krelease(ViewPanel v,char c,int code,int mod){}

    public void viewActivated(View v){}
    
    public void viewDeactivated(View v){}

    public void viewIconified(View v){}

    public void viewDeiconified(View v){}

    public void viewClosing(View v){application.exit();}

    //doing a single select/unselect operation on a node/edge
    //if shift is down, means we also want to select:
    //     -all edges attached to the node we are actually selecting
    //     -both nodes attached to the edge we are actually selecting
    void select(Glyph g,boolean isShiftDown){
	INode n=(INode)g.getOwner();
	//we only want to select resources and literals when clicking the glyph, not the text
	if (g.getType().startsWith(Editor.resEllipseType)){
	    IResource r=(IResource)n;
	    application.selectResource(r,!r.isSelected());
	    if (isShiftDown && r.isSelected()){//select associated properties only if selecting (not unselecting)
		application.selectPropertiesOfResource(r);//and if SHIFT is pressed
	    }
	    if (r.isSelected()){//show node attributes in PropsPanel
		application.propsp.updateDisplay(r);
		application.updatePropertyBrowser(r);
	    }
	}
	else if (g.getType().startsWith(Editor.litRectType)){
	    ILiteral l=(ILiteral)n;
	    application.selectLiteral(l,!l.isSelected());
	    if (isShiftDown && l.isSelected()){//select associated property only if selecting (not unselecting)
		application.selectPropertiesOfLiteral(l);//and if SHIFT is pressed
	    }
	    if (l.isSelected()){application.propsp.updateDisplay(l);}//show node attributes in PropsPanel
	}
	//on the contrary, predicates can be selected by clicking on text, path or arrow head
	else if (g.getType().startsWith("prd")){
	    IProperty p=(IProperty)n;
	    application.selectPredicate(p,!n.isSelected());
	    if (isShiftDown && p.isSelected()){//select associated nodes only if selecting (not unselecting)
		application.selectNodesOfProperty(p);//and if SHIFT is pressed
	    }
	    if (p.isSelected()){application.propsp.updateDisplay(p);}//show edge attributes in PropsPanel
	}
    }

    void cancelStartedPredicate(){
	pathForNewProperty=null;
	VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
	for (int i=0;i<tempSegments.size();i++){//get rid of temporary segments representing the path
	    vs.destroyGlyph((Glyph)tempSegments.elementAt(i));
	}
	tempSegments=null;
	subjectForNewProperty=null;
	CREATE_PREDICATE_STARTED=false;
    }

    private IResource insideAnIResource(Vector glyphs){
	Object o;
	for (int i=glyphs.size()-1;i>=0;i--){
	    if ((o=((Glyph)glyphs.elementAt(i)).getOwner())!=null && o instanceof IResource){return (IResource)o;}
	}
	return null;
    }
    
    private INode insideAnINode(Vector glyphs){
	Object o;
	for (int i=glyphs.size()-1;i>=0;i--){
	    if ((o=((Glyph)glyphs.elementAt(i)).getOwner())!=null && (o instanceof IResource || o instanceof ILiteral)){return (INode)o;}
	}
	return null;
    }

    private Glyph mouseInsideAPathCP(Vector v){
	for (int i=0;i<v.size();i++){
	    if (((Glyph)v.elementAt(i)).getType().equals("rszp")){return (Glyph)v.elementAt(i);}
	}
	return null;
    }

}
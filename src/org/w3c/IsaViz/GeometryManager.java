/*   FILE: GeometryManager.java
 *   DATE OF CREATION:   12/17/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Fri Apr 18 17:07:20 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import java.util.Vector;
import java.util.Enumeration;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.Shape;

import com.xerox.VTM.engine.*;
import com.xerox.VTM.glyphs.*;
import net.claribole.zvtm.glyphs.GlyphUtils;

/*methods to compute new geometry (ellipse width, text position, paths, etc...)*/

class GeometryManager {

    Editor application;

    GeometryManager(Editor e){
	application=e;
    }

    /*object created to edit the geometric attributes of a node/edge - remember it to be able to quickly destroy it*/
    Resizer lastResizer;

    void resetLastResizer(){
	lastResizer=null;
    }

    //adjust the start and end points of a path when resizing one of the nodes it is attached to (passed as parameter)
    /*this could be greatly enhanced - we are just modifying the coordinates of the start/end point whereas we could 
      translate all points of the path so that it keeps the same shape, but with a different aspect ratio - this should
      not be too hard ('amount' of translation is proportional to the delta from old to new position of the node)
      -actually, not sure this would be a good idea: would that be the expected behavior form the user point of view?
    */
    void adjustPaths(INode n){
	if (n.isVisuallyRepresented()){
	    Vector v;
	    Point2D delta;
	    Point2D newPoint;
	    VPath p;
	    Vector segs=new Vector();
	    double[] cds=new double[6];
	    int type;
	    IProperty ip;
	    if (n instanceof IResource){
		IResource r=(IResource)n;
		Glyph el1=n.getGlyph();
		Shape el2=GlyphUtils.getJava2DShape(el1);
		if ((v=r.getOutgoingPredicates())!=null){
		    for (int i=0;i<v.size();i++){//for all outgoing edges
			ip=(IProperty)v.elementAt(i);
			if (ip.isVisuallyRepresented()){
			    p=(VPath)ip.getGlyph(); //get the path
			    PathIterator pi=p.getJava2DPathIterator();
			    segs.removeAllElements();
			    while (!pi.isDone()){ //store all its segments
				type=pi.currentSegment(cds);
				segs.add(new PathSegment(cds,type));
				pi.next();
			    }
			    newPoint=((PathSegment)segs.firstElement()).getMainPoint();
			    if (el2.contains(newPoint)){//path start point is inside the ellipse
				delta=Utils.computeStepValue(el1.vx,el1.vy,newPoint.getX(),newPoint.getY());
				while (el2.contains(newPoint)){
				    newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
				}
			    }
			    else {//path start point is outside the ellipse
				delta=Utils.computeStepValue(newPoint.getX(),newPoint.getY(),el1.vx,el1.vy);
				while (!el2.contains(newPoint)){
				    newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
				}
			    }
			    ((PathSegment)segs.firstElement()).setMainPoint(newPoint);
			    //then update the VPath
			    reconstructVPathFromPathSegments(p,segs);
			}
		    }
		}
		if ((v=r.getIncomingPredicates())!=null){
		    for (int i=0;i<v.size();i++){//for all incoming edges
			ip=(IProperty)v.elementAt(i);
			if (ip.isVisuallyRepresented()){
			    p=(VPath)ip.getGlyph(); //get the path
			    PathIterator pi=p.getJava2DPathIterator();
			    segs.removeAllElements();
			    while (!pi.isDone()){ //store all its segments
				type=pi.currentSegment(cds);
				segs.add(new PathSegment(cds,type));
				pi.next();
			    }
			    newPoint=((PathSegment)segs.lastElement()).getMainPoint();
			    if (el2.contains(newPoint)){//path start point is inside the ellipse
				delta=Utils.computeStepValue(el1.vx,el1.vy,newPoint.getX(),newPoint.getY());
				while (el2.contains(newPoint)){
				    newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
				}
			    }
			    else {//path start point is outside the ellipse
				delta=Utils.computeStepValue(newPoint.getX(),newPoint.getY(),el1.vx,el1.vy);
				while (!el2.contains(newPoint)){
				    newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
				}
			    }
			    ((PathSegment)segs.lastElement()).setMainPoint(newPoint);
			    //then update the VPath
			    reconstructVPathFromPathSegments(p,segs);
			    //and the VTriangle (arrow head)
			    VTriangleOr t=(VTriangleOr)((IProperty)v.elementAt(i)).getGlyphHead(); //get the arrow head
			    double[] last2points=getLastTwoVPathPoints(segs);
			    Utils.createPathArrowHead(last2points[0],last2points[1],last2points[2],last2points[3],t);
			}
		    }
		}
	    }
	    else {//n instanceof ILiteral
		ILiteral l=(ILiteral)n;
		Glyph el1=n.getGlyph();
		Shape el2=GlyphUtils.getJava2DShape(el1);
		if (l.getIncomingPredicate()!=null){
		    ip=(IProperty)l.getIncomingPredicate();
		    if (ip.isVisuallyRepresented()){//i theory this is not necessary, as a visible literal has necessarily a visible incoming property
			//but you never now... :-)
			p=(VPath)ip.getGlyph(); //get the path
			PathIterator pi=p.getJava2DPathIterator();
			segs.removeAllElements();
			while (!pi.isDone()){ //store all its segments
			    type=pi.currentSegment(cds);
			    segs.add(new PathSegment(cds,type));
			    pi.next();
			}
			newPoint=((PathSegment)segs.lastElement()).getMainPoint();
			if (el2.contains(newPoint)){//path start point is inside the ellipse
			    delta=Utils.computeStepValue(el1.vx,el1.vy,newPoint.getX(),newPoint.getY());
			    while (el2.contains(newPoint)){
				newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
			    }
			}
			else {//path start point is outside the ellipse
			    delta=Utils.computeStepValue(newPoint.getX(),newPoint.getY(),el1.vx,el1.vy);
			    while (!el2.contains(newPoint)){
				newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
			    }
			}
			((PathSegment)segs.lastElement()).setMainPoint(newPoint);
			//then update the VPath
			reconstructVPathFromPathSegments(p,segs);
			//and the VTriangle (arrow head)
			VTriangleOr t=(VTriangleOr)l.getIncomingPredicate().getGlyphHead(); //get the arrow head
			double[] last2points=getLastTwoVPathPoints(segs);
			Utils.createPathArrowHead(last2points[0],last2points[1],last2points[2],last2points[3],t);
		    }
		}
	    }
	Editor.vsm.repaintNow();
	}
    }

    //adjust a resource's ellipse width and center text in it - also adjust paths since the ellipse might change
    void adjustResourceTextAndShape(IResource r,String newText){//newText==null if text is left unchanged
	if (r.isVisuallyRepresented()){
	    VText g=r.getGlyphText();
	    if (newText!=null){g.setText(newText);}
	    Rectangle2D r2d=Editor.vsm.getView(Editor.mainView).getGraphicsContext().getFontMetrics().getStringBounds(g.getText(),Editor.vsm.getView(Editor.mainView).getGraphicsContext());//have to do it this way because the paint thread might not yet have (and probably has not yet) computed the new String's bounds
	    Glyph el=r.getGlyph();
	    if (el instanceof RectangularShape){
		RectangularShape rs=(RectangularShape)el;
		rs.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*Math.round(r2d.getWidth())/2
		//shape should always have width > height  (just for aesthetics)
		if (rs.getWidth()<(1.5*rs.getHeight())){rs.setWidth(Math.round(1.5*rs.getHeight()));}
		//center VText in rectangle
		g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
		adjustPaths(r);
	    }
	    else {/*else we don't want to adjust the width of non-rectangular shapes*/
		//center VText in rectangle
		g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
		adjustPaths(r);
	    }
	}
    }

    //called after RDF import (graphviz positioning can be bad, e.g. under Linux)
    void correctResourceTextAndShape(IResource r){
	if (r.isVisuallyRepresented()){
	    VText g=r.getGlyphText();
	    Glyph el=r.getGlyph();
	    Rectangle2D r2d=Editor.vsm.getView(Editor.mainView).getGraphicsContext().getFontMetrics().getStringBounds(g.getText(),Editor.vsm.getView(Editor.mainView).getGraphicsContext());//have to do it this way because the paint thread might not yet have (and probably has not yet) computed the new String's bounds 
	    if (el instanceof RectangularShape){
		RectangularShape rs=(RectangularShape)el;
		rs.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*bounds/2
		//ellipse should always have width > height  (just for aesthetics)
		if (rs.getWidth()<(1.5*rs.getHeight())){rs.setWidth(Math.round(1.5*rs.getHeight()));}
		//center VText in rectangle
		g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
		adjustPaths(r);
	    }
	    else {
		//center VText in rectangle
		g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
		adjustPaths(r);
	    }
	}
    }

    //called after RDF import (graphviz positioning can be bad, e.g. under Linux)
    void correctLiteralTextAndShape(ILiteral l){
	if (l.isVisuallyRepresented()){
	    VText g=l.getGlyphText();
	    if (g!=null && g.getText().length()>0){
		Glyph rl=l.getGlyph();
		Rectangle2D r2d=Editor.vsm.getView(Editor.mainView).getGraphicsContext().getFontMetrics().getStringBounds(g.getText(),Editor.vsm.getView(Editor.mainView).getGraphicsContext());//have to do it this way because the paint thread might not yet have (and probably has not yet) computed the new String's bounds 
		if (rl instanceof RectangularShape){
		    RectangularShape rs=(RectangularShape)rl;
		    rs.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*bounds/2
		    //rectanglee should always have width > height  (just for aesthetics)
		    if (rs.getWidth()<(1.5*rs.getHeight())){rs.setWidth(Math.round(1.5*rs.getHeight()));}
		    //center VText in rectangle
		    g.moveTo(rl.vx-(long)r2d.getWidth()/2,rl.vy-(long)r2d.getHeight()/4);
		    adjustPaths(l);
		}
		else {
		    //center VText in rectangle
		    g.moveTo(rl.vx-(long)r2d.getWidth()/2,rl.vy-(long)r2d.getHeight()/4);
		    adjustPaths(l);
		}
	    }
	}
    }

    //just centers the text inside the ellipse
    void adjustResourceText(IResource r){
	if (r.isVisuallyRepresented()){
	    VText g=r.getGlyphText();
	    Glyph el=r.getGlyph();
	    LongPoint bounds=g.getBounds(Editor.vsm.getActiveCamera().getIndex());
	    //center VText in rectangle
	    g.moveTo(el.vx-bounds.x/2,el.vy-bounds.y/4);
	}
    }

    //just centers the text inside the rectangle
    void adjustLiteralText(ILiteral l){
	if (l.isVisuallyRepresented()){
	    VText g=l.getGlyphText();
	    if (g!=null){
		Glyph rl=l.getGlyph();
		LongPoint bounds=g.getBounds(Editor.vsm.getActiveCamera().getIndex());
		//center VText in rectangle
		g.moveTo(rl.vx-bounds.x/2,rl.vy-bounds.y/4);
	    }
	}
    }

    /*graphical objects to resize a resource's ellipse in the graph*/
    void initResourceResizer(IResource r){
	destroyLastResizer();
	Vector v=new Vector();v.add(r);
	Vector dependencies=new Vector();
	if (r.getIncomingPredicates()!=null){//also remember geometry of properties attached to this resource
	    for (Enumeration e=r.getIncomingPredicates().elements();e.hasMoreElements();){//so that it can be
		dependencies.add(e.nextElement());//restored if user undoes the operation
	    }
	}
	if (r.getOutgoingPredicates()!=null){
	    for (Enumeration e=r.getOutgoingPredicates().elements();e.hasMoreElements();){
		dependencies.add(e.nextElement());
	    }
	}
	ISVGeom cmd=new ISVGeom(application,dependencies,v,new Vector());
	application.addCmdToUndoStack(cmd);
	r.displayOnTop();
	lastResizer=new ResResizer(r);
	if (r.getGlyphText()!=null){
	    Editor.vsm.stickToGlyph(r.getGlyphText(),r.getGlyph());  //also update the resource text's position
	}
    }

    /*graphical objects to resize a literal's rectangle in the graph*/
    void initLiteralResizer(ILiteral l){
	destroyLastResizer();
	Vector v=new Vector();v.add(l);
	Vector dependencies=new Vector();
	//also remember geometry of properties attached to this resource so that it can be restored if 
	if (l.getIncomingPredicate()!=null){dependencies.add(l.getIncomingPredicate());}//user undoes the operation
	ISVGeom cmd=new ISVGeom(application,dependencies,new Vector(),v);
	application.addCmdToUndoStack(cmd);
	l.displayOnTop();
	lastResizer=new LitResizer(l);
	if (l.getGlyphText()!=null){
	    Editor.vsm.stickToGlyph(l.getGlyphText(),l.getGlyph());  //also update the literal text's position
	}
    }

    /*graphical objects to edit a predicate's path in the graph*/
    void initPropertyResizer(IProperty p){
	destroyLastResizer();
	Vector v=new Vector();v.add(p);
	ISVGeom cmd=new ISVGeom(application,v,new Vector(),new Vector());
	application.addCmdToUndoStack(cmd);
	p.displayOnTop();
	lastResizer=new PropResizer(p);
    }

    /*destroy graphical objects (handles) used to resize/move the last node/edge edited*/
    void destroyLastResizer(){
	if (lastResizer!=null){lastResizer.destroy();lastResizer=null;}
    }

    /*resize a resource/literal*/
    void resize(Glyph handle){
	try {lastResizer.updateMainGlyph(handle);}
	catch (NullPointerException e){}
    }

    /*end resizing a resource/literal*/
    void endResize(){
	Editor.vsm.unstickFromMouse();
	//then have to adjust edges start and end points attached to this resource/literal
	Object o=lastResizer.getMainGlyph().getOwner();
	if (o instanceof IResource){adjustResourceText((IResource)o);adjustPaths((INode)o);}
	else if (o instanceof ILiteral){adjustLiteralText((ILiteral)o);adjustPaths((INode)o);}
    }

    /*move a resource/literal*/
    void move(Glyph mainGlyph){
	try {lastResizer.updateHandles();}
	catch (NullPointerException e){}
    }

    /*end moving a resource/literal*/
    void endMove(){
	try {Editor.vsm.unstickFromGlyph(((INode)lastResizer.getMainGlyph().getOwner()).getGlyphText(),lastResizer.getMainGlyph());}  //also update the node text's position
	catch (NullPointerException ex){}
	Editor.vsm.unstickFromMouse();
	//then have to adjust edges start and end points attached to this resource/literal
	adjustPaths((INode)lastResizer.getMainGlyph().getOwner());
	application.centerRadarView();
    }

    /*draw the VPath matching a broken line*/
    void updatePathAfterResize(){
	try {
	    lastResizer.updateMainGlyph(null);
	}
	catch (NullPointerException e){}
    }

    /*given a list of segments describing a broken line, reconstruct the VPath matching it*/
    void reconstructVPathFromPathSegments(VPath p,Vector segs){//segs is a vector of PathSegment
	double[] cds;
	PathSegment ps;
	p.resetPath();
	for (int j=0;j<segs.size();j++){
	    ps=(PathSegment)segs.elementAt(j);
	    cds=ps.getCoords();
	    switch (ps.getType()){
	    case PathIterator.SEG_CUBICTO:{
		p.addCbCurve((long)cds[4],(long)cds[5],(long)cds[0],(long)cds[1],(long)cds[2],(long)cds[3],true);
		break;
	    }
	    case PathIterator.SEG_QUADTO:{
		p.addQdCurve((long)cds[2],(long)cds[3],(long)cds[0],(long)cds[1],true);
		break;
	    }
	    case PathIterator.SEG_LINETO:{
		p.addSegment((long)cds[0],(long)cds[1],true);
		break;
	    }
	    case PathIterator.SEG_MOVETO:{
		p.jump((long)cds[0],(long)cds[1],true);
		break;
	    }
	    }
	}
    }

    /*returns the last two points of a vpath (no matter their type (start point, control point curve point,etc))*/
    double[] getLastTwoVPathPoints(Vector segs){//segs is a vector of PathSegment
	double[] res=new double[4];
	double[] cds=((PathSegment)segs.lastElement()).getCoords();
	int type=((PathSegment)segs.lastElement()).getType();
	if (type==PathIterator.SEG_LINETO){
	    Point2D oneButLast=((PathSegment)segs.elementAt(segs.size()-2)).getMainPoint();
	    res[0]=oneButLast.getX();
	    res[1]=oneButLast.getY();
	    res[2]=cds[0];
	    res[3]=cds[1];
	}
	else if (type==PathIterator.SEG_CUBICTO){
	    res[0]=cds[2];
	    res[1]=cds[3];
	    res[2]=cds[4];
	    res[3]=cds[5];
	}
	else if (type==PathIterator.SEG_QUADTO){
	    res[0]=cds[0];
	    res[1]=cds[1];
	    res[2]=cds[2];
	    res[3]=cds[3];
	}
	else {System.err.println("Error: Editor.getLastTwoVPathPoints: bad segment type "+type);}
	return res;
    }

    void insertSegmentInPath(Glyph g){//should only receive "rszp" VRectangles  (resizing path handles)
	ControlPoint cpA=(ControlPoint)g.getOwner();
	if (cpA.type<ControlPoint.END_POINT){//if cp is a START_POINT or CURVE_POINT  (i.e. a black handle except the last one)
	    PropResizer pr=cpA.owner;
	    //retrieve next handle
	    ControlPoint cpB=(ControlPoint)cpA.nextHandle.getOwner();
	    //destroy old segment linking cpA to cpB
	    Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).destroyGlyph(cpA.s2);
	    cpA.s2=null;
	    cpB.s1=null;
	    //compute coordinates of two new intermediate points between cpA and cpB -  we are creating a Quadratic curve
	    LongPoint p1=new LongPoint(Math.round(cpA.handle.vx+(cpB.handle.vx-cpA.handle.vx)/3),Math.round(cpA.handle.vy+(cpB.handle.vy-cpA.handle.vy)/3));
	    LongPoint p2=new LongPoint(Math.round(cpA.handle.vx+(cpB.handle.vx-cpA.handle.vx)*2/3),Math.round(cpA.handle.vy+(cpB.handle.vy-cpA.handle.vy)*2/3));
	    RectangleNR r1=new RectangleNR(p1.x,p1.y,0,4,4,java.awt.Color.red);
	    RectangleNR r2=new RectangleNR(p2.x,p2.y,0,4,4,java.awt.Color.black);
	    Editor.vsm.addGlyph(r1,Editor.mainVirtualSpace);
	    Editor.vsm.addGlyph(r2,Editor.mainVirtualSpace);
	    LongPoint pA=new LongPoint(cpA.handle.vx,cpA.handle.vy);
	    LongPoint pB=new LongPoint(cpB.handle.vx,cpB.handle.vy);
	    VSegment s1=new VSegment((pA.x+p1.x)/2,(pA.y+p1.y)/2,0,(p1.x-pA.x)/2,(pA.y-p1.y)/2,java.awt.Color.red);
	    VSegment s2=new VSegment((p1.x+p2.x)/2,(p1.y+p2.y)/2,0,(p2.x-p1.x)/2,(p1.y-p2.y)/2,java.awt.Color.red);
	    VSegment s3=new VSegment((p2.x+pB.x)/2,(p2.y+pB.y)/2,0,(pB.x-p2.x)/2,(p2.y-pB.y)/2,java.awt.Color.red);
	    Editor.vsm.addGlyph(s1,Editor.mainVirtualSpace);
	    Editor.vsm.addGlyph(s2,Editor.mainVirtualSpace);
	    Editor.vsm.addGlyph(s3,Editor.mainVirtualSpace);
	    cpA.setSecondSegment(s1,r1);
	    ControlPoint cp1=new ControlPoint(r1,cpA.handle,s1,ControlPoint.QUAD_CURVE_CP,pr);
	    cp1.setSecondSegment(s2,r2);
	    ControlPoint cp2=new ControlPoint(r2,r1,s2,ControlPoint.CURVE_POINT,pr);
	    cp2.setSecondSegment(s3,cpB.handle);
	    cpB.prevHandle=r2;
	    cpB.s1=s3;
	    //new points have been inserted and linked. Reconstruct the array of CPs
	    //for the resizer based on the new linked list
	    ControlPoint[] res=new ControlPoint[pr.cps.length+2];
	    int i=0;
	    ControlPoint iterator=pr.cps[i];
	    while (i<res.length-1){
		res[i]=iterator;
		iterator=(ControlPoint)iterator.nextHandle.getOwner();
		i++;
	    }
	    res[i]=iterator;  //last point is not inside the loop because its nextHandle is null
	    pr.cps=res;
	    pr.updateMainGlyph(null);
	}
    }

    void deleteSegmentInPath(Glyph g){//should only receive "rszp" VRectangles  (resizing path handles)
	ControlPoint cp1=(ControlPoint)g.getOwner();
	if (cp1.type==ControlPoint.CURVE_POINT){//if cp is a CURVE_POINT  (i.e. a black handle except the first and last one)
	    PropResizer pr=cp1.owner;
	    ControlPoint cpA=(ControlPoint)cp1.prevHandle.getOwner(); //first point before the ones to destroy
	    ControlPoint cpB; //first point after the ones to destroy
	    ControlPoint cpIt=(ControlPoint)cp1.nextHandle.getOwner();
	    VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
	    vs.destroyGlyph(cp1.handle);
	    vs.destroyGlyph(cp1.s1);
	    vs.destroyGlyph(cp1.s2);
	    while (cpIt.type>ControlPoint.END_POINT){//seek next curve point and destroy all intermediate quad or cub control points
		vs.destroyGlyph(cpIt.handle);
		vs.destroyGlyph(cpIt.s2);
		cpIt=(ControlPoint)cpIt.nextHandle.getOwner();
	    }
	    cpB=cpIt;
	    LongPoint pA=new LongPoint(cpA.handle.vx,cpA.handle.vy);
	    LongPoint pB=new LongPoint(cpB.handle.vx,cpB.handle.vy);
	    VSegment s=new VSegment((pA.x+pB.x)/2,(pA.y+pB.y)/2,0,(pB.x-pA.x)/2,(pA.y-pB.y)/2,java.awt.Color.red);
	    Editor.vsm.addGlyph(s,Editor.mainVirtualSpace);
	    cpA.setSecondSegment(s,cpB.handle);
	    cpB.prevHandle=cpA.handle;
	    cpB.s1=s;
	    Vector v=new Vector();
	    cpIt=pr.cps[0];
	    v.add(cpIt);
	    while (cpIt.nextHandle!=null){
		cpIt=(ControlPoint)cpIt.nextHandle.getOwner();
		v.add(cpIt);
	    }
	    pr.cps=(ControlPoint[])v.toArray(new ControlPoint[v.size()]);
	    pr.updateMainGlyph(null);
	}
    }

    /*returns the same path but reversed (start point becomes end point) if point tx,ty is closer to first point on path cds than last point on the same path */
    static VPath invertPath(long tx,long ty,VPath pt){
	PathIterator pi=pt.getJava2DPathIterator();
	double[] cds=new double[6];
	//retrieve first point on path
	int type=pi.currentSegment(cds);
	pi.next();
	double fpx,fpy,lpx,lpy;  //first and last points on path
	if (type==PathIterator.SEG_MOVETO){//first instruction in a jump so the path begins at the coords specified by this jump
	    fpx=cds[0];
	    fpy=cds[1];
	}
	else {//first instructions is not a jump so the path begins at the current coordinates, i.e. 0,0 (should not happen)
	    fpx=0;
	    fpy=0;
	}
	while (!pi.isDone()){type=pi.currentSegment(cds);pi.next();}//go to last point (ignore intermediate points)
	if (type==PathIterator.SEG_CUBICTO){//last instruction is a cubic curve (in theory, it should always be this one, unless graphviz changes its SVG output format)
	    lpx=cds[4];
	    lpy=cds[5];
	}
	else if (type==PathIterator.SEG_QUADTO){//last instruction is a quadratic curve
	    lpx=cds[2];
	    lpy=cds[3];
	}
	else if (type==PathIterator.SEG_LINETO){//last instruction is a segment
	    lpx=cds[0];
	    lpy=cds[1];
	}
	else if (type==PathIterator.SEG_CLOSE){//last instruction closes the path
	    lpx=fpx;
	    lpy=fpy;
	}
	else {//last instruction is a jump (makes no sense)
	    lpx=0;
	    lpy=0;
	}
	double d1=Math.sqrt(Math.pow(tx-fpx,2)+Math.pow(ty-fpy,2));
	double d2=Math.sqrt(Math.pow(tx-lpx,2)+Math.pow(ty-lpy,2));
	if (d1<d2){//if point tx,ty is closer to start point than end point, invert path
	    pi=pt.getJava2DPathIterator();
	    Vector segs=new Vector();
	    while (!pi.isDone()){
		type=pi.currentSegment(cds);
		segs.add(new PathSegment(cds,type));
		pi.next();
	    }
	    VPath newPt=new VPath();
	    PathSegment seg1,seg2;
	    //first, move to last point (which becomes first point)
	    seg1=(PathSegment)segs.elementAt(segs.size()-1);
	    if (seg1.getType()==PathIterator.SEG_CUBICTO){
		newPt.jump((long)seg1.cds[4],(long)seg1.cds[5],true);
	    }
	    else if (seg1.getType()==PathIterator.SEG_MOVETO){
		newPt.jump((long)seg1.cds[0],(long)seg1.cds[1],true);
	    }
	    else if (seg1.getType()==PathIterator.SEG_QUADTO){
		newPt.jump((long)seg1.cds[2],(long)seg1.cds[3],true);
	    }
	    else if (seg1.getType()==PathIterator.SEG_LINETO){
		newPt.jump((long)seg1.cds[0],(long)seg1.cds[1],true);
	    }
	    //then process the points in reverse order
	    for (int j=segs.size()-1;j>0;j--){
		seg1=(PathSegment)segs.elementAt(j);
		seg2=(PathSegment)segs.elementAt(j-1);
		if (seg1.getType()==PathIterator.SEG_CUBICTO){
		    if (seg2.getType()==PathIterator.SEG_CUBICTO){newPt.addCbCurve((long)seg2.cds[4],(long)seg2.cds[5],(long)seg1.cds[2],(long)seg1.cds[3],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_MOVETO){newPt.addCbCurve((long)seg2.cds[0],(long)seg2.cds[1],(long)seg1.cds[2],(long)seg1.cds[3],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_QUADTO){newPt.addCbCurve((long)seg2.cds[2],(long)seg2.cds[3],(long)seg1.cds[2],(long)seg1.cds[3],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_LINETO){newPt.addCbCurve((long)seg2.cds[0],(long)seg2.cds[1],(long)seg1.cds[2],(long)seg1.cds[3],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		}
		else if (seg1.getType()==PathIterator.SEG_MOVETO){
		    if (seg2.getType()==PathIterator.SEG_CUBICTO){newPt.jump((long)seg2.cds[4],(long)seg2.cds[5],true);}
		    else if (seg2.getType()==PathIterator.SEG_MOVETO){newPt.jump((long)seg2.cds[0],(long)seg2.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_QUADTO){newPt.jump((long)seg2.cds[2],(long)seg2.cds[3],true);}
		    else if (seg2.getType()==PathIterator.SEG_LINETO){newPt.jump((long)seg2.cds[0],(long)seg2.cds[1],true);}
		}
		else if (seg1.getType()==PathIterator.SEG_QUADTO){
		    if (seg2.getType()==PathIterator.SEG_CUBICTO){newPt.addQdCurve((long)seg2.cds[4],(long)seg2.cds[5],(long)seg1.cds[4],(long)seg1.cds[5],true);}
		    else if (seg2.getType()==PathIterator.SEG_MOVETO){newPt.addQdCurve((long)seg2.cds[0],(long)seg2.cds[1],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_QUADTO){newPt.addQdCurve((long)seg2.cds[2],(long)seg2.cds[3],(long)seg1.cds[2],(long)seg1.cds[3],true);}
		    else if (seg2.getType()==PathIterator.SEG_LINETO){newPt.addQdCurve((long)seg2.cds[0],(long)seg2.cds[1],(long)seg1.cds[0],(long)seg1.cds[1],true);}
		}
		else if (seg1.getType()==PathIterator.SEG_LINETO){
		    if (seg2.getType()==PathIterator.SEG_CUBICTO){newPt.addSegment((long)seg2.cds[4],(long)seg2.cds[5],true);}
		    else if (seg2.getType()==PathIterator.SEG_MOVETO){newPt.addSegment((long)seg2.cds[0],(long)seg2.cds[1],true);}
		    else if (seg2.getType()==PathIterator.SEG_QUADTO){newPt.addSegment((long)seg2.cds[2],(long)seg2.cds[3],true);}
		    else if (seg2.getType()==PathIterator.SEG_LINETO){newPt.addSegment((long)seg2.cds[0],(long)seg2.cds[1],true);}
		}
	    }
	    return newPt;
	}
	else {return pt;}
    }

}

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 10/19/2001
 */



package org.w3c.IsaViz;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.Point;
import java.awt.Color;
import java.awt.geom.PathIterator;

import org.w3c.dom.*;

import com.xerox.VTM.svg.SVGReader;
import com.xerox.VTM.glyphs.*;
import com.xerox.VTM.engine.LongPoint;

import com.hp.hpl.jena.rdf.arp.JenaReader;
import com.hp.hpl.mesa.rdf.jena.mem.ModelMem;
import com.hp.hpl.mesa.rdf.jena.model.Model;
import com.hp.hpl.mesa.rdf.jena.model.Statement;
import com.hp.hpl.mesa.rdf.jena.model.RDFNode;
import com.hp.hpl.mesa.rdf.jena.model.Resource;
import com.hp.hpl.mesa.rdf.jena.model.Property;
import com.hp.hpl.mesa.rdf.jena.model.Literal;
import com.hp.hpl.mesa.rdf.jena.model.StmtIterator;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.RDFErrorHandler;
import com.hp.hpl.mesa.rdf.jena.model.RDFWriter;
import com.hp.hpl.mesa.rdf.jena.model.RDFReader;
import com.hp.hpl.mesa.rdf.jena.model.NsIterator;
import com.hp.hpl.mesa.rdf.jena.common.RDFWriterFImpl;
import com.hp.hpl.mesa.rdf.jena.common.RDFReaderFImpl;
import com.hp.hpl.mesa.rdf.jena.common.NTripleWriter;
import com.hp.hpl.mesa.rdf.jena.common.NTripleReader;

/*in charge of loading, parsing  and serializing RDF files (using Jena/ARP)*/

class RDFLoader implements RDFErrorHandler {

    // Name for the DOT file title
    private static final String DOT_TITLE = "dotfile";

    static int RDF_XML_READER=0;
    static int NTRIPLE_READER=1;

    Editor application;

    //RDF parser
    RDFReader parser;

    File rdfF;
    java.net.URL rdfU;
    File dotF;
    File svgF;
    boolean dltOnExit;

    StringBuffer nextResID;
    StringBuffer nextPredID;
    StringBuffer nextLitID;

    RDFLoader(Editor e){
	application=e;
	nextResID=new StringBuffer("0");
	nextPredID=new StringBuffer("0");
	nextLitID=new StringBuffer("0");
    }

    void reset(){
	rdfF=null;
	rdfU=null;
	dotF=null;
	svgF=null;
	nextResID=new StringBuffer("0");
	nextPredID=new StringBuffer("0");
	nextLitID=new StringBuffer("0");
    }

    void initParser(int i){//i==0 means we are reading RDF/XML, i==1 means we are reading NTriples
	if (i==0){
	    parser=new JenaReader();
	    parser.setErrorHandler(this);
	}
	else if (i==1){
	    try {
		parser=(new RDFReaderFImpl()).getReader("N-TRIPLE");
		parser.setErrorHandler(this);
	    }
	    catch (RDFException ex){System.err.println("Error: RDFLoader.initParser(): "+ex);}
	}
    }

    void load(Object o,int whichReader){//o can be a java.net.URL or a java.io.File
	//whichReader=0 if RDF/XML, 1 if NTriples
	ProgPanel pp=new ProgPanel("Resetting...","Loading RDF");
	PrintWriter pw=null;
	try {
	    pp.setPBValue(5);
	    application.rdfModel=new ModelMem();
	    if (o instanceof File){
		rdfF=(File)o;
		pp.setLabel("Loading local file "+rdfF.toString()+" ...");
		FileReader fr=new FileReader(rdfF);
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,fr,Editor.DEFAULT_NAMESPACE);
	    }
	    else if (o instanceof java.net.URL){
		rdfU=(java.net.URL)o;
		pp.setLabel("Loading remote file "+rdfU.toString()+" ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,rdfU.toString());
	    }
	    NsIterator nsit=application.rdfModel.listNameSpaces();
	    while (nsit.hasNext()){
		application.addNamespaceBinding("",nsit.next(),new Boolean(false),true,false);//do it silently, don't override display state and prefix for existing bindings
	    }
	    SH sh=new SH(pw,application);
	    StmtIterator it=application.rdfModel.listStatements();
	    Statement st;
	    while (it.hasNext()){
		st=it.next();
		if (st.getObject() instanceof Resource){sh.statement(st.getSubject(),st.getPredicate(),(Resource)st.getObject());}
		else if (st.getObject() instanceof Literal){sh.statement(st.getSubject(),st.getPredicate(),(Literal)st.getObject());}
		else {System.err.println("Error: RDFLoader.load(): unknown kind of object: "+st.getObject());}
	    }
	    it.close();
	    pp.setPBValue(50);
	    pp.setLabel("Creating temporary SVG file ...");
	    svgF=Utils.createTempFile(Editor.m_TmpDir.toString(),"isv",".svg");
	    pp.setPBValue(60);
	    pp.setLabel("Calling GraphViz ...");
	    callDOT(pw);
	    pp.setPBValue(80);
	    pp.setLabel("Parsing SVG ...");
	    displaySVG(application.xmlMngr.parse(svgF.toString(),false));
	    cleanMapIDs();//the mapping between SVG and RDF has been done -  we do not need these any longer
	    ConfigManager.assignColorsToGraph();
	    application.showAnonIds(application.SHOW_ANON_ID);  //show/hide IDs of anonymous resources
	    application.showResourceLabels(Editor.DISP_AS_LABEL);
	    pp.setPBValue(100);
	    pp.setLabel("Deleting temporary files ...");
	    if (Editor.dltOnExit){deleteFiles();}
	    Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),500);
	    application.rdfModel=null; //get rid of it at this point - we will generate it only when appropriate (for instance when exporting)
	}
	catch (IOException ex){application.errorMessages.append("RDFLoader.load() "+ex+"\n");application.reportError=true;}
	catch (RDFException ex2){application.errorMessages.append("RDFLoader.load() "+ex2+"\n");application.reportError=true;}
	//catch (Exception ex3){application.errorMessages.append("RDFLoader.load() "+ex3+"\nPlease verify your directory preferences (GraphViz/DOT might not be configured properly), your default namespace and anonymous node prefix declarations");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	pp.destroy();
    }

    Model merge(Object o,int whichReader){//o can be a java.net.URL or a java.io.File
	//whichReader=0 if RDF/XML, 1 if NTriples	
	ModelMem res=new ModelMem();
	initParser(whichReader);
	if (o instanceof File){
	    try {
		FileReader fr=new FileReader((File)o);
		parser.read(res,fr,Editor.DEFAULT_NAMESPACE);
	    }
	    catch (IOException ex){application.errorMessages.append("RDFLoader.merge() "+ex+"\n");application.reportError=true;}
	    catch (RDFException ex2){application.errorMessages.append("RDFLoader.merge() "+ex2+"\n");application.reportError=true;}
	}
	else if (o instanceof java.net.URL){
	    java.net.URL tmpURL=(java.net.URL)o;
	    try {
		parser.read(res,tmpURL.toString());
	    }
	    catch (RDFException ex){application.errorMessages.append("RDFLoader.merge() "+ex+"\n");application.reportError=true;}
	}
	return res;
    }

    void loadProperties(File f){
	try {
	    FileReader fr=new FileReader(f);
	    initParser(RDF_XML_READER);
	    Model tmpModel=new ModelMem();
	    parser.read(tmpModel,fr,Editor.DEFAULT_NAMESPACE);
	    StmtIterator it=tmpModel.listStatements();
	    Property prd;
	    while (it.hasNext()){
		prd=it.next().getPredicate();
		application.addPropertyType(prd.getNameSpace(),prd.getLocalName(),true);
	    }
	    tmpModel=null;
	}
	catch (IOException ex){application.errorMessages.append("RDFLoader.loadProperties() "+ex+"\n");application.reportError=true;}
	catch (RDFException ex){application.errorMessages.append("RDFLoader.loadProperties() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    void deleteFiles(){
	if (dotF!=null){dotF.delete();}
	if (svgF!=null){svgF.delete();}	
    }

    PrintWriter createDOTFile(){
	dotF=initGraphFile();
	PrintWriter pw=null;
	if (dotF==null){return null;} // Assume error has been reported
	// Create a PrintWriter for the DOT handler
	try {
	    FileWriter fw=new FileWriter(dotF);
	    if (fw!=null){pw=new PrintWriter(fw);}
	    if (pw!=null){processDOTParameters(pw);}  // Add the graph header
	}
	catch (IOException ex){application.errorMessages.append("RDFLoader.createDOTFile() "+ex+"\n");application.reportError=true;}
	return pw;
    }

    //create a temporary file for DOT input
    File initGraphFile(){
        try {
            // Stop if any of the parameters are missing
            //if (Editor.m_TmpDir==null || Editor.m_GraphVizPath==null || Editor.m_GraphVizFontDir==null){
	    if (Editor.m_TmpDir==null || Editor.m_GraphVizPath==null){
                // Put the paths in a comment in the returned content
		application.errorMessages.append("Temporary DOT file initialization failed\n");
                application.errorMessages.append("TMP_DIR = " + Editor.m_TmpDir+"\n");
                application.errorMessages.append("GRAPH_VIZ_PATH = " + Editor.m_GraphVizPath+"\n");
		application.reportError=true;
                //System.err.println("GRAPH_FONT_DIR  = " + Editor.m_GraphVizFontDir);
                return null;
            }
	}
	catch (Exception e) {application.errorMessages.append("Unable to create a temporary graph file. A graph cannot be generated.\n");application.reportError=true;return null;}
        File f=null;
	// Must generate a unique file name that the DOT handler will use 
	f=Utils.createTempFile(Editor.m_TmpDir.toString(),"isv",".dot");
	if (f == null){
	    application.errorMessages.append("Failed to create a temporary graph file. A graph cannot be generated.\n");
	    application.reportError=true;
	    return null;
	}
	return f;
    }

    void processDOTParameters(PrintWriter pw){
        pw.println("digraph "+DOT_TITLE+"{ ");  // Print the graph header
        String nodeColor="black";
        String nodeTextColor="black";
        String edgeColor="black";
        String edgeTextColor="black";
        // Orientation must be either TB or LR
        String orientation=Editor.GRAPH_ORIENTATION;
        // Add an attribute for all of the graph's nodes
        pw.println("node [fontname="+Editor.vtmFontName+",fontsize=" +Editor.vtmFontSize+",color="+nodeColor+",fontcolor="+nodeTextColor+"];");
        // Add an attribute for all of the graph's edges
        pw.println("edge [fontname="+Editor.vtmFontName+",fontsize=" +Editor.vtmFontSize+",color="+edgeColor+",fontcolor="+edgeTextColor+"];");
        // Add an attribute for the orientation
        pw.println("rankdir="+orientation+";");
    }

    /*
     * Generate a graph of the RDF data model
     *
     *@param out the servlet's output stream
     *@param pw the graph file's PrintWriter
     *@param dotFile the File handle for the graph file
     *@param rdf the RDF text
     */
    private void callDOT(PrintWriter pw){
        try {
            pw.println( " }"); // Add the graph footer
            pw.close();  // Close the DOT input file so the GraphViz can open and read it
            // Pass the DOT data file to the GraphViz dot program
            // so it can create a graph image of the data model
            if (!generateSVGFile(dotF.getAbsolutePath(), svgF.getAbsolutePath())) {
                application.errorMessages.append("An attempt to create a graph failed.\n");
		deleteFiles();
                return;
            }
        }
	catch (Exception e){application.errorMessages.append("Exception generating graph: " + e.getMessage()+"\n");application.reportError=true;}
    }


    /*
     * Invokes the GraphViz program to create a graph image from the
     * the given DOT data file
     *
     *@param dotFileName the name of the DOT data file
     *@param outputFileName the name of the output data file 
     *@return true if success; false if any failure occurs
     */
    private boolean generateSVGFile(String dotFileName,String outputFileName){
        //String environment[]={DOTFONTPATH+"="+Editor.m_GraphVizFontDir};
        String cmdArray[]={Editor.m_GraphVizPath.toString(),"-Tsvg","-o",outputFileName,dotFileName};
        Runtime rt=Runtime.getRuntime();
        try {
            //Process p = rt.exec(cmdArray, environment);
            Process p = rt.exec(cmdArray);
            p.waitFor();
        } 
	catch (Exception e) {application.errorMessages.append("Error: generating OutputFile.\n");application.reportError=true;return false;}
        return true;
    }



    void displaySVG(Document d){
	Element svgRoot=d.getDocumentElement();
	//get the space width and height and set an offset for the SVG interpreter so that all objects
	//do not get created in the same quadrant (south-east)
	if (svgRoot.hasAttribute("width") && svgRoot.hasAttribute("height")){
	    String width=svgRoot.getAttribute("width");
	    String height=svgRoot.getAttribute("height");
	    try {
		long Xoffset= -(new Long(width.substring(0,width.length()-2))).longValue()/2;
		long Yoffset= -(new Long(height.substring(0,height.length()-2))).longValue()/2;
		SVGReader.setPositionOffset(Xoffset,Yoffset);
	    }
	    catch (IndexOutOfBoundsException ex){} //if we run into any problem, just forget this
	}
	if (Editor.GRAPHVIZ_VERSION==0){//dealing with SVG output by GraphViz 1.7.6
	    NodeList objects=svgRoot.getChildNodes();
	    for (int i=0;i<objects.getLength();i++){
		Node obj=objects.item(i);
		if (obj.getNodeType()==Node.ELEMENT_NODE){processSVG176Node((Element)obj);}
	    }
	}
	else {//dealing with SVG output by GraphViz 1.7.11 or later
	    //javax.swing.JOptionPane.showMessageDialog(application.cmp,"GraphViz 1.7.11 SVG output processing is not yet available in IsaViz. Coming soon.");
	    NodeList objects=svgRoot.getElementsByTagName("g").item(0).getChildNodes();
	    for (int i=0;i<objects.getLength();i++){
		Node obj=(Node)objects.item(i);
		if (obj.getNodeType()==Node.ELEMENT_NODE){processSVG1711Node((Element)obj);}
	    }
	}
	/*when running isaviz under Linux (and perhaps other posix systems), nodes are too small 
	  (width) w.r.t the text (don't know if it is a graphviz or java problem - anyway,
	  we correct it by adjusting widths a posteriori*/
	/*also do it under windows as some literals, sometimes have the same problem*/
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    application.geomMngr.correctResourceTextAndShape((IResource)e.nextElement());
	}
	for (Enumeration e=application.literals.elements();e.hasMoreElements();){
	    application.geomMngr.correctLiteralTextAndShape((ILiteral)e.nextElement());
	}
 	SVGReader.setPositionOffset(0,0);  //reset position offset (might affect anything that uses SVGReader methods, like constructors in VPath)
    }
    
    void processSVG176Node(Element e){
	String name=e.getLocalName();
	if (name.equals("a")){
	    NodeList content;
	    if ((content=e.getElementsByTagName("ellipse")).getLength()>0){//dealing with a resource
		VEllipse el=SVGReader.createEllipse((Element)content.item(0));
		VText tx=SVGReader.createText((Element)e.getElementsByTagName("text").item(0));
		Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);
		Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
		// IResource r=application.getResource(((Element)e.getElementsByTagName("text").item(0)).getFirstChild().getNodeValue());
		IResource r=getResourceByMapID(e.getAttribute("xlink:href"));  //do not use getAttributeNS because
		r.setGlyph(el);                                                //GraphViz 176
		r.setGlyphText(tx);                                            //does not define the XLink namespace
	    }
	    else if ((content=e.getElementsByTagName("g")).getLength()>0){//dealing with a property
		//PATH
		VPath pt=SVGReader.createPath((Element)((Element)content.item(0)).getElementsByTagName("path").item(0),new VPath());
		Editor.vsm.addGlyph(pt,Editor.mainVirtualSpace);
		//ARROW - not part of the std SVG generator
		Element e2=(Element)e.getElementsByTagName("polygon").item(0);
		Vector coords=new Vector();
		//get the polygon's vertices and translate them in the VTM's coord syst
		SVGReader.translateSVGPolygon(e2.getAttribute("points"),coords);
		//find {left,up,right,down}-most coordinates
		LongPoint p=(LongPoint)coords.firstElement();
		long minx=p.x;
		long maxx=p.x;
		long miny=p.y;
		long maxy=p.y;
		for (int i=1;i<coords.size();i++){
		    p=(LongPoint)coords.elementAt(i);
		    if (p.x<minx){minx=p.x;}
		    if (p.x>maxx){maxx=p.x;}
		    if (p.y<miny){miny=p.y;}
		    if (p.y>maxy){maxy=p.y;}
		}
		//retrieve last two points defining this path (2nd control point + end point) (GraphViz/DOT generates paths made only of cubic curves)
		PathIterator pi=pt.getJava2DPathIterator();
		double[] cds=new double[6];
		while (!pi.isDone()){pi.currentSegment(cds);pi.next();}
		//compute slope of segment linking the two points and deduce the triangle's orientation from it
		double angle=0;
		java.awt.geom.Point2D delta=Utils.computeStepValue(cds[2],-cds[3],cds[4],-cds[5]);
		if (delta.getX()==0){
		    angle=0;
		    if (delta.getY()<0){angle=Math.PI;}
		}
		else {
		    angle=Math.atan(delta.getY()/delta.getX());
		    //align with VTM's system coordinates (a VTriangle's "head" points to the north when orient=0, not to the east)
		    if (delta.getX()<0){angle+=Math.PI/2;}   //comes from angle+PI-PI/2 (first PI due to the fact that ddx is <0 and the use of the arctan function - otherwise, head points in the opposite direction)
		    else {angle-=Math.PI/2;}
		}
		VTriangleOr c=new VTriangleOr((maxx+minx)/2,-(maxy+miny)/2,0,Math.max(maxx-minx,maxy-miny)/2,Color.black,(float)angle);
		Editor.vsm.addGlyph(c,Editor.mainVirtualSpace);
		//TEXT
		VText tx=SVGReader.createText((Element)e.getElementsByTagName("text").item(0));
		Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
		Vector props=application.getProperties(((Element)e.getElementsByTagName("text").item(0)).getFirstChild().getNodeValue());
		IProperty pr=null;
		String mapID=e.getAttribute("xlink:href");  //do not use getAttributeNS because
		for (int i=0;i<props.size();i++){           //GraphViz 176
		    pr=(IProperty)props.elementAt(i);       //does not define the XLink namespace
		    if (pr.getMapID().equals(mapID)){
			pr.setGlyph(pt,c);
			pr.setGlyphText(tx);
			break;
		    }
		}
	    }
	    else if ((content=e.getElementsByTagName("polygon")).getLength()>0){//dealing with a literal
		VRectangle r=SVGReader.createRectangleFromPolygon((Element)content.item(0));
		VText tg=null;
		if (r!=null){
		    Editor.vsm.addGlyph(r,Editor.mainVirtualSpace);
		}
		Element txt=(Element)e.getElementsByTagName("text").item(0);
		if (txt!=null){
		    tg=SVGReader.createText(txt);
		    Editor.vsm.addGlyph(tg,Editor.mainVirtualSpace);
		}
		ILiteral lt;
		String mapID=e.getAttribute("xlink:href");            //do not use getAttributeNS because
		for (int i=0;i<application.literals.size();i++){      //GraphViz 176
		    lt=(ILiteral)application.literals.elementAt(i);   //does not define the XLink namespace
		    if (lt.getMapID().equals(mapID)){
			lt.setGlyph(r);
			if (tg!=null){
			    lt.setGlyphText(tg);
			}
			break;
		    }
		}		
	    }
	    else {javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured when parsing the SVG file.\nThis might be caused by an incompatibility with the selected version of GraphViz.\nTry selecting another version of GraphViz in the Preferences window.");}
	}
	//the next commented alternative does not happen anymore (since we changed the mapping strategy for resources)
// 	else if (name.equals("ellipse")){//dealing with an anonymous resource
// 	    Element nsb=null;
// 	    Node nd=e.getNextSibling();
// 	    while (nsb==null){
// 		if (nd instanceof Element && ((Element)nd).getLocalName().equals("text")){nsb=(Element)nd;}
// 		else {nd=nd.getNextSibling();}
// 	    }
// 	    VEllipse el=SVGReader.createEllipse(e);
// 	    Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);	    
// 	    IResource r=application.getResource(nsb.getFirstChild().getNodeValue());
// 	    r.setGlyph(el);
// 	    VText tx=SVGReader.createText(nsb);
// 	    Editor.vsm.addGlyph(tx,application.mainVirtualSpace);
// 	    r.setGlyphText(tx);
// 	}
	else if (name.equals("text")){//dealing with an anonymous resource's text (handled in previous branch - ignore)
	}
	//does not happen anymore
// 	else if (name.equals("g")){
// 	    Editor.vsm.addGlyph(SVGReader.createPath((Element)e.getElementsByTagName("path").item(0),new VPath()),application.mainVirtualSpace);
// 	}
	else {javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured when parsing the SVG file.\nThis might be caused by an incompatibility with the selected version of GraphViz.\nTry selecting another version of GraphViz in the Preferences window.");}
    }

    void processSVG1711Node(Element e){//receiving g elements
	NodeList content;
	if (e.getAttribute("class").equals("node")){//dealing with resource or literal
// 	    if ((content=e.getElementsByTagName("ellipse")).getLength()>0){//dealing with an anonymous resource
// 		VEllipse el=SVGReader.createEllipse((Element)content.item(0));
// 		Element text=(Element)e.getElementsByTagName("text").item(0);
// 		VText tx=SVGReader.createText(text);
// 		Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);
// 		Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
//  		IResource r=application.getResource(text.getFirstChild().getNodeValue());
// 		r.setGlyph(el);
// 		r.setGlyphText(tx);
// 	    }
	    if ((content=e.getElementsByTagName("a")).getLength()>0){//dealing with a resource or a literal
		Element a=(Element)content.item(0);
		if ((content=a.getElementsByTagName("ellipse")).getLength()>0){//dealing with a resource
		    VEllipse el=SVGReader.createEllipse((Element)content.item(0));
		    Element text=(Element)a.getElementsByTagName("text").item(0);
		    VText tx=SVGReader.createText(text);
		    Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);
		    Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
// 		    IResource r=application.getResource(text.getFirstChild().getNodeValue());
		    IResource r=getResourceByMapID(a.getAttributeNS("http://www.w3.org/1999/xlink","href"));
		    r.setGlyph(el);
		    r.setGlyphText(tx);
		}
		else if ((content=a.getElementsByTagName("polygon")).getLength()>0){//dealing with a literal
		    VRectangle r=SVGReader.createRectangleFromPolygon((Element)content.item(0));
		    VText tg=null;
		    if (r!=null){
			Editor.vsm.addGlyph(r,Editor.mainVirtualSpace);
		    }
		    Element txt=(Element)a.getElementsByTagName("text").item(0);
		    if (txt!=null){
			tg=SVGReader.createText(txt);
			Editor.vsm.addGlyph(tg,Editor.mainVirtualSpace);
		    }
		    ILiteral lt;
		    String mapID=a.getAttributeNS("http://www.w3.org/1999/xlink","href");
		    for (int i=0;i<application.literals.size();i++){
			lt=(ILiteral)application.literals.elementAt(i);
			if (lt.getMapID().equals(mapID)){
			    lt.setGlyph(r);
			    if (tg!=null){
				lt.setGlyphText(tg);
			    }
			    break;
			}
		    }
		}
	    }
	    else {System.err.println("Error: processSVG1711Node: unknown tag in "+e);}
	}
	else if (e.getAttribute("class").equals("edge")){//dealing with property
	    Element a=(Element)e.getElementsByTagName("a").item(0);
	    String pathCoords=((Element)a.getElementsByTagName("path").item(0)).getAttribute("d");
	    //partially deal with the arrow because we need to know if we have to invert the path or not (if the arrow head coincides
	    //with the path start point instead of the end point
	    Element e2=(Element)a.getElementsByTagName("polygon").item(0);
	    Vector coords=new Vector();
	    //get the polygon's vertices and translate them in the VTM's coord syst
	    SVGReader.translateSVGPolygon(e2.getAttribute("points"),coords);
	    //find {left,up,right,down}-most coordinates
	    LongPoint p=(LongPoint)coords.firstElement();
	    long minx=p.x;
	    long maxx=p.x;
	    long miny=p.y;
	    long maxy=p.y;
	    for (int i=1;i<coords.size();i++){
		p=(LongPoint)coords.elementAt(i);
		if (p.x<minx){minx=p.x;}
		if (p.x>maxx){maxx=p.x;}
		if (p.y<miny){miny=p.y;}
		if (p.y>maxy){maxy=p.y;}
	    }//note that max and min are used again later in this block
	    //PATH
	    VPath pt=SVGReader.createPath(pathCoords,new VPath());
	    //invert path if necessary (happens when there are paths going from right to left - graphviz encodes them as going from left to right, so start and end points of the spline in isaviz are inversed and automatically/wrongly reassigned to the corresponding node - this causes truely weird splines as start point is moved to the position of end point and inversely) - the method below tests whether the arrow head is closer to the spline start point or end point (in the graphviz representation) ; if it is closer to the start point, it means that the path has to be inversed
	    pt=GeometryManager.invertPath((minx+maxx)/2,(miny+maxy)/2,pt);
	    Editor.vsm.addGlyph(pt,Editor.mainVirtualSpace);
	    //ARROW - not part of the std SVG generator
	    //retrieve last two points defining this path (2nd control point + end point) (GraphViz/DOT generates paths made only of cubic curves)
	    PathIterator pi=pt.getJava2DPathIterator();
	    float[] cds=new float[6];
	    while (!pi.isDone()){pi.currentSegment(cds);pi.next();}
	    //compute steep of segment linking the two points and deduce the triangle's orientation from it
	    double angle=0;
	    java.awt.geom.Point2D delta=Utils.computeStepValue(cds[2],-cds[3],cds[4],-cds[5]);
	    if (delta.getX()==0){
		angle=0;
		if (delta.getY()<0){angle=Math.PI;}
	    }
	    else {
		angle=Math.atan(delta.getY()/delta.getX());
		//align with VTM's system coordinates (a VTriangle's "head" points to the north when orient=0, not to the east)
		if (delta.getX()<0){angle+=Math.PI/2;}   //comes from angle+PI-PI/2 (first PI due to the fact that ddx is <0 and the use of the arctan function - otherwise, head points in the opposite direction)
		else {angle-=Math.PI/2;}
	    }
	    VTriangleOr c=new VTriangleOr((maxx+minx)/2,-(maxy+miny)/2,0,Math.max(maxx-minx,maxy-miny)/2,Color.black,(float)angle);
	    Editor.vsm.addGlyph(c,Editor.mainVirtualSpace);
	    //TEXT
	    VText tx=SVGReader.createText((Element)a.getElementsByTagName("text").item(0));
	    Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
	    Vector props=application.getProperties(((Element)a.getElementsByTagName("text").item(0)).getFirstChild().getNodeValue());
	    IProperty pr;
	    String mapID=a.getAttributeNS("http://www.w3.org/1999/xlink","href");
	    for (int i=0;i<props.size();i++){
		pr=(IProperty)props.elementAt(i);
		if (pr.getMapID().equals(mapID)){
		    pr.setGlyph(pt,c);
		    pr.setGlyphText(tx);
		    break;
		}
	    }
	}
    }

    void cleanMapIDs(){//get rid of the mapID attribute used in properties and literals
	for (Enumeration e=application.literals.elements();e.hasMoreElements();){
	    ((ILiteral)e.nextElement()).setMapID(null);
	}
	Vector v;
	for (Enumeration e=application.propertiesByURI.elements();e.hasMoreElements();){
	    v=(Vector)e.nextElement();
	    for (Enumeration e2=v.elements();e2.hasMoreElements();){
		((IProperty)e2.nextElement()).setMapID(null);
	    }
	}
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    ((IResource)e.nextElement()).setMapID(null);
	}
    }

    IResource getResourceByMapID(String id){
	IResource res=null;
	IResource tmp;
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    tmp=(IResource)e.nextElement();
// 	    System.err.println(tmp.getMapID());
	    if (tmp.getMapID().equals(id)){res=tmp;break;}
	}
	return res;
    }

    void incResID(){
	boolean done=false;
	for (int i=0;i<nextResID.length();i++){
	    byte b=(byte)nextResID.charAt(i);
	    if (b<0x7a){
		nextResID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextResID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextResID.length();i++){nextResID.setCharAt(i,'0');}
	    nextResID.append('0');
	}
    }

    void incPredID(){
	boolean done=false;
	for (int i=0;i<nextPredID.length();i++){
	    byte b=(byte)nextPredID.charAt(i);
	    if (b<0x7a){
		nextPredID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextPredID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextPredID.length();i++){nextPredID.setCharAt(i,'0');}
	    nextPredID.append('0');
	}
    }

    void incLitID(){
	boolean done=false;
	for (int i=0;i<nextLitID.length();i++){
	    byte b=(byte)nextLitID.charAt(i);
	    if (b<0x7a){
		nextLitID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextLitID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextLitID.length();i++){nextLitID.setCharAt(i,'0');}
	    nextLitID.append('0');
	}
    }

    //given a statement, adds missing entitites to the internal model (called by RDFLoader when importing RDF/XML)
    Vector processStatement(Resource subj, Property pred, Resource obj){
	//subject
	IResource r1=addResource(subj);
	//predicate
	IProperty p=addProperty(pred);
	//object
	IResource r2=addResource(obj);
	r1.addOutgoingPredicate(p);
	p.setSubject(r1);
	p.setObject(r2);
	r2.addIncomingPredicate(p);
	Vector res=new Vector();
	res.add(r1);res.add(p);res.add(r2);
	return res;
    }

    //given a statement, adds missing entitites to the internal model (called by RDFLoader when importing RDF/XML)
    Vector processStatement(Resource subj, Property pred, Literal lit){
	//subject
	IResource r=addResource(subj);
	//predicate
	IProperty p=addProperty(pred);
	//object
	ILiteral l=addLiteral(lit);
	r.addOutgoingPredicate(p);
	p.setSubject(r);
	p.setObject(l);
	l.setIncomingPredicate(p);
	Vector res=new Vector();
	res.add(r);res.add(p);res.add(l);
	if (p.getIdent().equals(Editor.RDFS_NAMESPACE_URI+"label")){//if property is rdfs:label, set label for the resource
	    r.setLabel(l.getValue());
	}
	return res;
    }

    /*create a new IResource from a Jena resource and add it to the internal model*/
    IResource addResource(Resource r){
	IResource res=new IResource(r);
	if (!application.resourcesByURI.containsKey(res.getIdent())){
	    application.resourcesByURI.put(res.getIdent(),res);
	    return res;
	}
	else {return (IResource)application.resourcesByURI.get(res.getIdent());}
    }

    //create a new IProperty and add it to the internal model (from a Jena property)
    IProperty addProperty(Property p){
	IProperty res=new IProperty(p);
	if (application.propertiesByURI.containsKey(res.getIdent())){
	    Vector v=(Vector)application.propertiesByURI.get(res.getIdent());
	    v.add(res);
	}
	else {
	    Vector v=new Vector();
	    v.add(res);
	    application.propertiesByURI.put(res.getIdent(),v);
	}
	application.addPropertyType(res.getNamespace(),res.getLocalname(),true);  //add to the table of property constructors silently (a property might be used multiple times in existing graphs)
	return res;
    }

    //create a new ILiteral and add it to the internal model (from Jena literal)
    ILiteral addLiteral(Literal l){
	ILiteral res=new ILiteral(l);
	application.literals.add(res);
	return res;
    }

    void generateJenaModel(){
	application.rdfModel=new ModelMem();
	Hashtable addedResources=new Hashtable();
	Hashtable addedProperties=new Hashtable();
	IProperty p;
	IResource s;  //subject
	INode o;      //object
	for (Enumeration e1=application.propertiesByURI.elements();e1.hasMoreElements();){
	    for (Enumeration e2=((Vector)e1.nextElement()).elements();e2.hasMoreElements();){
		p=(IProperty)e2.nextElement();
		s=p.getSubject();
		o=p.getObject();
		if ((s!=null) && (o!=null) && (!p.isCommented())){//if the subject or the object is commented, the predicate will be commented
		    Resource jenaSubject=null;
		    if (addedResources.containsKey(s)){//keep track of resources already added to the model
			jenaSubject=(Resource)addedResources.get(s); // (can appear in several statements)
		    }
		    else {
			if (s.isAnon()){
			    try {
				jenaSubject=application.rdfModel.createResource();
				addedResources.put(s,jenaSubject);
			    }
			    catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating anonymous resource\n"+s.toString()+"\n"+ex);}
			    
			}
			else {
			    try {
				jenaSubject=application.rdfModel.createResource(s.getIdent());
				addedResources.put(s,jenaSubject);
			    }
			    catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating resource\n"+s.toString()+"\n"+ex);}
			}			
		    }
		    Property jenaPredicate=null;
		    try {
			if (addedProperties.containsKey(p.getIdent())){//keep track of properties already added to the
			    jenaPredicate=(Property)addedProperties.get(p.getIdent()); //model (can appear in several statements)
			}
			else {
			    jenaPredicate=application.rdfModel.createProperty(p.getNamespace(),p.getLocalname());
			    addedResources.put(p.getIdent(),jenaPredicate);
			}
		    }
		    catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating property\n"+p.toString()+"\n"+ex);}
		    RDFNode jenaObject=null;
		    if (o instanceof IResource){
			IResource o2=(IResource)o;
			if (addedResources.containsKey(o2)){//keep track of resources already added to the model
			    jenaObject=(Resource)addedResources.get(o2); // (can appear in several statements)
			}
			else {
			    if (o2.isAnon()){
				try {
				    jenaObject=application.rdfModel.createResource();
				    addedResources.put(o2,jenaObject);
				}
				catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating anonymous resource\n"+s.toString()+"\n"+ex);}
			    }
			    else {
				try {
				    jenaObject=application.rdfModel.createResource(o2.getIdent());
				    addedResources.put(o2,jenaObject);
				}
				catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating resource\n"+s.toString()+"\n"+ex);}
			    }
			    
			}
		    }
		    else {//o instanceof ILiteral
			ILiteral l=(ILiteral)o;
			try {
			    if (!l.escapesXMLChars()){
				if (l.getLang()!=null){
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),l.getLang(),false);
				}
				else {
				    String lang=Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS ? Editor.DEFAULT_LANGUAGE_IN_LITERALS : "" ;
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),lang,false);
				}
			    }
			    else {//well formed XML, don't specify it
				if (l.getLang()!=null){
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),l.getLang());
				}
				else {
				    String lang=Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS ? Editor.DEFAULT_LANGUAGE_IN_LITERALS : "" ;
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),lang);
				}
			    }
			}
			catch(RDFException ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating literal\n"+o.toString()+"\n"+ex);}
		    }
		    try {
			Statement st=application.rdfModel.createStatement(jenaSubject,jenaPredicate,jenaObject);
			application.rdfModel.add(st);
		    }
		    catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating the Jena model:\nadding statement "+p.toString()+"("+s.toString()+","+o.toString()+")\n"+ex);}
		}
	    }
	}
    }
    
    public void save(Model m,File f){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to RDF/XML "+f.toString()+" ...");
	try {//should choose between abbrev and std syntax
	    RDFWriter rdfw;
	    if (Editor.ABBREV_SYNTAX){rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML-ABBREV");}
	    else {rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML");}
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0){//only add complete bindings
		    rdfw.setNsPrefix((String)application.tblp.nsTableModel.getValueAt(i,0),(String)application.tblp.nsTableModel.getValueAt(i,1));
		}
	    }
	    FileWriter fw=new FileWriter(f);
	    rdfw.write(m,fw,Editor.DEFAULT_NAMESPACE);
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to RDF/XML "+f.toString()+" ...done");
	}
	catch (RDFException ex){application.errorMessages.append("RDF exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	catch (IOException ex){application.errorMessages.append("I/O exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    /*Same as save(), but the target is a StringBuffer instead of a file*/
    public StringBuffer serialize(Model m){
	try {//should choose between abbrev and std syntax
	    RDFWriter rdfw;
	    if (Editor.ABBREV_SYNTAX){rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML-ABBREV");}
	    else {rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML");}
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0){//only add complete bindings
		    rdfw.setNsPrefix((String)application.tblp.nsTableModel.getValueAt(i,0),(String)application.tblp.nsTableModel.getValueAt(i,1));
		}
	    }
	    java.io.StringWriter sw=new java.io.StringWriter();
	    rdfw.write(m,sw,Editor.DEFAULT_NAMESPACE);
	    return sw.getBuffer();
	}
	catch (RDFException ex){application.errorMessages.append("RDF exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	return new StringBuffer();
    }

    public void saveAsTriples(Model m,File f){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to N-Triples "+f.toString()+" ...");
	try {
	    FileWriter fw=new FileWriter(f);
	    (new NTripleWriter()).write(m,fw,Editor.DEFAULT_NAMESPACE);
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to N-Triples "+f.toString()+" ...done");
	}
	catch (Exception ex){application.errorMessages.append("RDF exception in RDFLoader.saveAsTriples() "+ex+"\n");application.reportError=true;}	
    }

    public void error(Exception e){
	application.errorMessages.append("RDFErrorHandler.error: "+e+"\n");
	application.reportError=true;
    }
    
    public void fatalError(Exception e){
	application.errorMessages.append("RDFErrorHandler.fatalError: "+e+"\n");
	application.reportError=true;
    }

    public void warning(Exception e){
	application.errorMessages.append("RDFErrorHandler.warning: "+e+"\n");
	application.reportError=true;
    }

    //Statement handler inner class--------------------------------------------------
    private class SH {
	PrintWriter pw;
	Editor application;
	int numLiterals=0;

	public SH(PrintWriter p,Editor app){
	    this.pw=p;
	    this.application=app;
	}

        /*
         * Generic handler for a Resource/Resource/Resource triple (S/P/O).
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statement(Resource subj, Property pred, Resource obj){
	    Vector v=processStatement(subj,pred,obj);
	    statementDotResource((IResource)v.elementAt(0),subj,(IProperty)v.elementAt(1),pred,(IResource)v.elementAt(2),obj);
        }

        /*
         * Generic handler for a Resource/Resource/Resource triple (S/P/O).
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statement(Resource subj, Property pred, Literal lit){
	    Vector v=processStatement(subj,pred,lit);
            numLiterals++;
	    statementDotLiteral((IResource)v.elementAt(0),subj,(IProperty)v.elementAt(1),pred,(ILiteral)v.elementAt(2),lit);
        }


        /*
         * Handler for a Resource/Resource/Resource triple (S/P/O).
	 * Outputs the given triple using Dot syntax.
	 *
	 * Each triple will be output in three lines of DOT code as
	 * follows (not including the complication of anon nodes 
	 * and the possiblity that the anon nodes may be named 
	 * with an empty string):
	 *
	 *   1. "<subject>" [URL="<subject">];
	 *   2. "<subject>" -> "<object>" [label="<predicate>",URL="<predicate>"];
	 *   3. "<object>"  [URL="<object>"];
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statementDotResource(IResource subj,Resource s,IProperty pred,Property p,IResource obj,Resource o){
	    if (this.pw == null) return;
	    printFirstPart(s,subj);
            this.pw.print("\" -> ");
	    String aUniqueID=application.rdfLdr.nextPredID.toString();
	    pred.setMapID(aUniqueID);
	    application.rdfLdr.incPredID();
	    boolean nodeAlreadyInDOTFile=true;
	    if (obj.getMapID()==null){
		String aUniqueRID=application.rdfLdr.nextResID.toString();
		obj.setMapID(aUniqueRID);
		application.rdfLdr.incResID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (o.isAnon()){
		this.pw.println("\""+obj.getIdent()+"\" [label=\""+p.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");// "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because since graphviz 1.8 text objects are centered around the coordinates provided with the text element
		if (!nodeAlreadyInDOTFile){this.pw.println("\""+obj.getIdent()+"\" [URL=\""+obj.getMapID()+"\"];");}
	    }
	    else {
// 		this.pw.println("\""+o.getURI()+"\" [label=\""+p.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");
// 		if (!nodeAlreadyInDOTFile){this.pw.println("\""+o.getURI()+"\" [URL=\""+obj.getMapID()+"\"];");}
		this.pw.println("\""+obj.getIdent()+"\" [label=\""+p.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");
		if (!nodeAlreadyInDOTFile){this.pw.println("\""+obj.getIdent()+"\" [URL=\""+obj.getMapID()+"\"];");}
	    }
        }

        /*
         * Handler for a Resource/Resource/Literal triple (S/P/O).
	 * Outputs the given triple using Dot syntax.
	 *
	 * Each triple will be output in three lines of DOT code as
	 * follows (not including the complication of anon nodes 
	 * and the possiblity that the anon nodes may be named 
	 * with an empty string):
         *
	 *   1. "<subject>" [URL="<subject">];
	 *   2. "<subject>" -> "<literal>" [label="<predicate>",URL="mapID"];
	 *   3. "aLiteralUniqueID" [shape="box",label="<1st 80 characters of the literal's value>",URL="mapID"];
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statementDotLiteral(IResource subj,Resource s,IProperty pred,Property p,ILiteral lit,Literal l){
	    if (this.pw == null) return;
	    printFirstPart(s,subj);  // Same as Res/Res/Res
            /*
             * Before outputing the object (Literal) do the following:
             *
             * o GraphViz/DOT cannot handle embedded line terminators characters
             *   so they must be replaced with spaces
             * o Limit the number of chars to make the graph legible
             * o Escape double quotes
             */
	    try {
		String s1 = new String(l.getString());
		s1 = s1.replace('\n', ' ');
		s1 = s1.replace('\f', ' ');
		s1 = s1.replace('\r', ' ');
		if (s1.indexOf('"')!= -1){s1=Utils.replaceString(s1,"\"","\\\"");}
		// Anything beyond 80 chars makes the graph too large
		String tmpObject=((s1.length()>=Editor.MAX_LIT_CHAR_COUNT) ? s1.substring(0,Editor.MAX_LIT_CHAR_COUNT)+" ..." : s1);
		// Create a temporary label for the literal so that if
		// it is duplicated it will be unique in the graph and
		// thus have its own node.
		String aUniquePID=application.rdfLdr.nextPredID.toString();
		pred.setMapID(aUniquePID);
		application.rdfLdr.incPredID();
		String aUniqueLID=application.rdfLdr.nextLitID.toString();
		lit.setMapID(aUniqueLID);
		application.rdfLdr.incLitID();
		String tmpName = "Literal_"+Integer.toString(this.numLiterals);
		this.pw.print("\" -> \""+tmpName);
		this.pw.println("\" [label=\""+p.getURI()+"\\l\",URL=\""+aUniquePID+"\"];");// "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because since graphviz 1.8 text objects are centered around the coordinates provided with the text element
		this.pw.println("\""+tmpName+"\" [shape=box,label=\""+tmpObject+"\\l\",URL=\""+aUniqueLID+"\"];");
	    }
	    catch (RDFException ex){application.errorMessages.append("Error: SH.statementDotLiteral: "+ex+"\n");application.reportError=true;}
        }
	
	/* 
	 * Print the first part of a triple's Dot file.  See below for
	 * more info.  This is the same regardless if the triple's
	 * object is a Resource or a Literal
	 *
	 *@param subj the subject
	 */
        public void printFirstPart(Resource subj,IResource ir){
	    try {
		boolean nodeAlreadyInDOTFile=true;
		if (ir.getMapID()==null){
		    String aUniqueRID=application.rdfLdr.nextResID.toString();
		    ir.setMapID(aUniqueRID);
		    application.rdfLdr.incResID();
		    nodeAlreadyInDOTFile=false;
		}
		if (subj.isAnon()) {
		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(subj.getId())+"\" [URL=\"" +ir.getMapID()+"\"];");}
		    this.pw.print("\""+Editor.ANON_NODE+IResource.getJenaAnonId(subj.getId()));
		} 
		else {
// 		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+subj.getURI()+"\" [URL=\"" +ir.getMapID()+"\"];");}
// 		    this.pw.print("\""+subj.getURI());
		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+ir.getIdent()+"\" [URL=\"" +ir.getMapID()+"\"];");}
		    this.pw.print("\""+ir.getIdent());
		}
	    }
	    catch (RDFException ex){application.errorMessages.append("Error: SH.printFirstPart(): "+ex+"\n");application.reportError=true;}
	}

    }

}

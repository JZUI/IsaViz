/*   FILE: RDFLoader.java
 *   DATE OF CREATION:   10/19/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Fri Apr 18 14:28:50 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import java.io.File;
//import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.Point;
import java.awt.Font;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.PathIterator;

import org.w3c.dom.*;

import com.xerox.VTM.svg.SVGReader;
import com.xerox.VTM.glyphs.*;
import com.xerox.VTM.engine.LongPoint;

import com.hp.hpl.jena.mem.ModelMem;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.NsIterator;
import com.hp.hpl.jena.datatypes.RDFDatatype;

/*in charge of loading, parsing  and serializing RDF files (using Jena/ARP)*/

class RDFLoader implements RDFErrorHandler {

    // Name for the DOT file title
    private static final String DOT_TITLE = "dotfile";

    static int RDF_XML_READER=0;
    static int NTRIPLE_READER=1;
    static int N3_READER=2;

    static String RDFXML="RDF/XML";
    static String RDFXMLAB="RDF/XML-ABBREV";
    static String NTRIPLE="N-TRIPLE";
    static String N3="N3";

    static String errorModePropertyName="http://jena.hpl.hp.com/arp/properties/error-mode";
    static String errorModePropertyValue="default";

    Editor application;

    //RDF parser
    RDFReader parser;

    File rdfF;
    java.net.URL rdfU;
    File dotF;
    File svgF;
    boolean dltOnExit;

    private static String RESOURCE_MAPID_PREFIX="R_";
    private static String LITERAL_MAPID_PREFIX="L_";

    StringBuffer nextNodeID;
    StringBuffer nextEdgeID;
//     StringBuffer nextLitID;

    Integer literalLbNum;   //used to count literals and generate tmp literal labels in the DOT file (only in styled import for now)

    RDFLoader(Editor e){
	application=e;
	nextNodeID=new StringBuffer("0");
	nextEdgeID=new StringBuffer("0");
    }

    void reset(){
	rdfF=null;
	rdfU=null;
	dotF=null;
	svgF=null;
	nextNodeID=new StringBuffer("0");
	nextEdgeID=new StringBuffer("0");
    }

    void initParser(int i,Model model){//i==0 means we are reading RDF/XML, i==1 means we are reading NTriples, 2==Notation3
	try {
	    //property name/value pairs are defined in 
	    //www.hpl.hp.com/semweb/javadoc/com/hp/hpl/jena/rdf/arp/JenaReader.html#setProperty(java.lang.String, java.lang.Object)
	    if (ConfigManager.PARSING_MODE==ConfigManager.STRICT_PARSING){errorModePropertyValue="strict";}
	    else if (ConfigManager.PARSING_MODE==ConfigManager.LAX_PARSING){errorModePropertyValue="lax";}
	    else {errorModePropertyValue="default";}
	    if (i==RDF_XML_READER){
		//parser=new JenaReader();
		parser=model.getReader(RDFXMLAB);
		parser.setErrorHandler(this);
		parser.setProperty(errorModePropertyName,errorModePropertyValue);
	    }
	    else if (i==NTRIPLE_READER){
		//parser=(new RDFReaderFImpl()).getReader(NTRIPLE);
		parser=model.getReader(NTRIPLE);
		parser.setErrorHandler(this);
		parser.setProperty(errorModePropertyName,errorModePropertyValue);
	    }
	    else if (i==N3_READER){
		//parser=(new RDFReaderFImpl()).getReader(N3);
		parser=model.getReader(N3);
		parser.setErrorHandler(this);
		parser.setProperty(errorModePropertyName,errorModePropertyValue);
	    }
	}
	catch (RDFException ex){System.err.println("Error: RDFLoader.initParser(): ");ex.printStackTrace();}
    }

    void load(Object o,int whichReader){//o can be a java.net.URL, a java.io.File or a java.io.InputStream (plug-ins)
	//whichReader=0 if RDF/XML, 1 if NTriples, 2 if N3
	ProgPanel pp=new ProgPanel("Resetting...","Loading RDF");
	PrintWriter pw=null;
	try {
	    pp.setPBValue(5);
	    application.rdfModel=new ModelMem();
	    if (o instanceof File){
		rdfF=(File)o;
		//FileReader fr=new FileReader(rdfF);
		FileInputStream fis=new FileInputStream(rdfF);
		pp.setLabel("Loading local file "+rdfF.toString()+" ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,fis,Editor.BASE_URI);
	    }
	    else if (o instanceof java.net.URL){
		rdfU=(java.net.URL)o;
		pp.setLabel("Loading remote file "+rdfU.toString()+" ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,rdfU.toString());
	    }
	    else if (o instanceof java.io.InputStream){
		pp.setLabel("Reading stream ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,(InputStream)o,Editor.BASE_URI);
	    }
	    NsIterator nsit=application.rdfModel.listNameSpaces();
	    while (nsit.hasNext()){
		application.addNamespaceBinding("",nsit.nextNs(),new Boolean(false),true,false);//do it silently, don't override display state and prefix for existing bindings
	    }
	    SH sh=new SH(pw,application);
	    StmtIterator it=application.rdfModel.listStatements();
	    Statement st;
	    while (it.hasNext()){
		st=it.nextStatement();
		if (st.getObject() instanceof Resource){sh.statement(st.getSubject(),st.getPredicate(),(Resource)st.getObject());}
		else if (st.getObject() instanceof Literal){sh.statement(st.getSubject(),st.getPredicate(),(Literal)st.getObject());}
		else {System.err.println("Error: RDFLoader.load(): unknown kind of object: "+st.getObject());}
	    }
	    it.close();
	    pp.setPBValue(50);
	    pp.setLabel("Creating temporary SVG file ...");
	    svgF=Utils.createTempFile(Editor.m_TmpDir.toString(),"isv",".svg");
	    pp.setPBValue(60);
	    pp.setLabel("Calling GraphViz (this can take several minutes) ...");
	    callDOT(pw);
	    pp.setPBValue(80);
	    pp.setLabel("Parsing SVG ...");
	    displaySVG(application.xmlMngr.parse(svgF,false));
	    cleanMapIDs();//the mapping between SVG and RDF has been done -  we do not need these any longer
	    application.cfgMngr.assignColorsToGraph();
	    application.showAnonIds(application.SHOW_ANON_ID);  //show/hide IDs of anonymous resources
	    application.showResourceLabels(Editor.DISP_AS_LABEL);
	    pp.setPBValue(100);
	    pp.setLabel("Deleting temporary files ...");
	    if (Editor.dltOnExit){deleteFiles();}
	    Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),ConfigManager.ANIM_DURATION);
	    application.centerRadarView();
	    application.rdfModel=null; //get rid of it at this point - we will generate it only when appropriate (for instance when exporting)
	}
	catch (IOException ex){application.errorMessages.append("RDFLoader.load() "+ex+"\n");application.reportError=true;}
	catch (RDFException ex2){application.errorMessages.append("RDFLoader.load() "+ex2+"\n");application.reportError=true;}
	//catch (Exception ex3){application.errorMessages.append("RDFLoader.load() "+ex3+"\nPlease verify your directory preferences (GraphViz/DOT might not be configured properly), your default namespace and anonymous node prefix declarations");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	pp.destroy();
    }

    Model merge(Object o,int whichReader){//o can be a java.net.URL, a java.io.File or a java.io.InputStream (plug-ins)
	//whichReader=0 if RDF/XML, 1 if NTriples, 2 if N3
	ModelMem res=new ModelMem();
	if (o instanceof File){
	    try {
		//FileReader fr=new FileReader((File)o);
		//InputStreamReader isr=new InputStreamReader(new FileInputStream((File)o),ConfigManager.ENCODING);
		FileInputStream fis=new FileInputStream((File)o);
		initParser(whichReader,application.rdfModel);
		parser.read(res,fis,Editor.BASE_URI);
	    }
	    catch (IOException ex){application.errorMessages.append("RDFLoader.merge() (File) "+ex+"\n");application.reportError=true;}
	    catch (RDFException ex2){application.errorMessages.append("RDFLoader.merge() (File) "+ex2+"\n");application.reportError=true;}
	}
	else if (o instanceof java.net.URL){
	    java.net.URL tmpURL=(java.net.URL)o;
	    try {
		parser.read(res,tmpURL.toString());
	    }
	    catch (RDFException ex){application.errorMessages.append("RDFLoader.merge() (URL) "+ex+"\n");application.reportError=true;}
	}
	else if (o instanceof InputStream){
	    try {
		initParser(whichReader,application.rdfModel);
		parser.read(res,(InputStream)o,Editor.BASE_URI);
	    }
	    catch (RDFException ex){application.errorMessages.append("RDFLoader.merge() (InputStream) "+ex+"\n");application.reportError=true;}
	}
	return res;
    }

    void loadProperties(File f){
	try {
	    //FileReader fr=new FileReader(f);
	    //InputStreamReader isr=new InputStreamReader(new FileInputStream(f),ConfigManager.ENCODING);
	    FileInputStream fis=new FileInputStream(f);
	    Model tmpModel=new ModelMem();
	    initParser(RDF_XML_READER,tmpModel);
	    parser.read(tmpModel,fis,Editor.BASE_URI);
	    StmtIterator it=tmpModel.listStatements();
	    Property prd;
	    while (it.hasNext()){
		prd=it.nextStatement().getPredicate();
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
	    //FileWriter fw=new FileWriter(dotF);
	    OutputStreamWriter osw=new OutputStreamWriter(new FileOutputStream(dotF),ConfigManager.ENCODING);
	    if (osw!=null){pw=new PrintWriter(osw);}
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
        pw.println("digraph "+DOT_TITLE+" {");  // Print the graph header
        String nodeColor="black";
        String nodeTextColor="black";
	String nodeFillColor="white";
        String edgeColor="black";
        String edgeTextColor="black";
        // Orientation must be either TB or LR
        String orientation=Editor.GRAPH_ORIENTATION;
        // Add an attribute for all of the graph's nodes
        pw.println("node [fontname=\""+Editor.vtmFontName+"\",fontsize=" +Editor.vtmFontSize+",color="+nodeColor+",fillcolor="+nodeFillColor+",fontcolor="+nodeTextColor+"];");
        // Add an attribute for all of the graph's edges
        pw.println("edge [fontname=\""+Editor.vtmFontName+"\",fontsize=" +Editor.vtmFontSize+",color="+edgeColor+",fontcolor="+edgeTextColor+"];");
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
            pw.println("}"); // Add the graph footer
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
// 	if (Editor.GRAPHVIZ_VERSION==0){//dealing with SVG output by GraphViz 1.7.6
// 	    NodeList objects=svgRoot.getChildNodes();
// 	    for (int i=0;i<objects.getLength();i++){
// 		Node obj=objects.item(i);
// 		if (obj.getNodeType()==Node.ELEMENT_NODE){processSVG176Node((Element)obj);}
// 	    }
// 	}
// 	else {//dealing with SVG output by GraphViz 1.7.11 or later
	    //javax.swing.JOptionPane.showMessageDialog(application.cmp,"GraphViz 1.7.11 SVG output processing is not yet available in IsaViz. Coming soon.");
	NodeList objects=svgRoot.getElementsByTagName("g").item(0).getChildNodes();
	for (int i=0;i<objects.getLength();i++){
	    Node obj=(Node)objects.item(i);
	    if (obj.getNodeType()==Node.ELEMENT_NODE){processSVGNode((Element)obj,false);}
	}
// 	}
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

    void processSVGNode(Element e,boolean styling){
	NodeList content;
	if (e.getAttribute("class").equals("node")){//dealing with resource or literal
	    if ((content=e.getElementsByTagName("a")).getLength()>0){//dealing with a resource or a literal
		Element a=(Element)content.item(0);
		String mapID=a.getAttributeNS("http://www.w3.org/1999/xlink","href");
		if (mapID.startsWith(RESOURCE_MAPID_PREFIX)){
		    IResource r=getResourceByMapID(mapID);
		    Glyph el=getResourceShape(r,a,styling);
		    el.setFill(true);
		    Editor.vsm.addGlyph(el,Editor.mainVirtualSpace);
		    Element text=(Element)a.getElementsByTagName("text").item(0);
		    r.setGlyph(el);
		    VText tx;
		    if (text!=null){
			tx=SVGReader.createText(text,Editor.vsm);
		    }
		    else {//looks like a resource can be blank (rdf:about="") - even if it is not the case,
			tx=new VText("");//just be robust here, it is up to the RDF parser to report an error
		    }
		    tx.setTextAnchor(VText.TEXT_ANCHOR_START); //latest ZVTM/SVG takes MIDDLE into account, and this disturbs our previous hacks to center text (a future version should use MIDDLE and get rid of the hacks)
		    Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
		    r.setGlyphText(tx);
		}
		else if (mapID.startsWith(LITERAL_MAPID_PREFIX)){
		    ILiteral lt=null;
		    ILiteral tmpLt;
		    for (int i=0;i<application.literals.size();i++){
			tmpLt=(ILiteral)application.literals.elementAt(i);
			if (tmpLt.getMapID()!=null && tmpLt.getMapID().equals(mapID)){
			    lt=tmpLt;
			    break;
			}
		    }
		    if (lt!=null){
			Glyph r=getLiteralShape(lt,a,styling);
			r.setFill(true);
			if (r!=null){
			    Editor.vsm.addGlyph(r,Editor.mainVirtualSpace);
			    lt.setGlyph(r);
			}
			Element txt=(Element)a.getElementsByTagName("text").item(0);
			VText tg=null;
			if (txt!=null){
			    tg=SVGReader.createText(txt,Editor.vsm);
			    tg.setTextAnchor(VText.TEXT_ANCHOR_START); //latest ZVTM/SVG takes MIDDLE into account, and this disturbs our previous hacks to center text (a future version should use MIDDLE and get rid of the hacks)
			    Editor.vsm.addGlyph(tg,Editor.mainVirtualSpace);
			}
			if (tg!=null){
			    lt.setGlyphText(tg);
			}
		    }
		}
		else {System.err.println("Error: processSVGNode: unable to identify mapID "+mapID);}
	    }
	    else {System.err.println("Error: processSVGNode: unknown tag in "+e+" (expected <a ...>)");}
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
	    VText tx=SVGReader.createText((Element)a.getElementsByTagName("text").item(0),Editor.vsm);
	    tx.setTextAnchor(VText.TEXT_ANCHOR_START); //latest ZVTM/SVG takes MIDDLE into account, and this disturbs our previous hacks to center text (a future version should use MIDDLE and get rid of the hacks)
	    Editor.vsm.addGlyph(tx,Editor.mainVirtualSpace);
	    Vector props=application.getProperties(((Element)a.getElementsByTagName("text").item(0)).getFirstChild().getNodeValue());
	    IProperty pr;
	    String mapID=a.getAttributeNS("http://www.w3.org/1999/xlink","href");
	    for (int i=0;i<props.size();i++){
		pr=(IProperty)props.elementAt(i);
		if (pr.getMapID()!=null && pr.getMapID().equals(mapID)){
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
	    if (tmp.getMapID()!=null && tmp.getMapID().equals(id)){//mapID can be null if the resource is not visible
		res=tmp;break;
	    }
	}
	return res;
    }

    void incNodeID(){
	boolean done=false;
	for (int i=0;i<nextNodeID.length();i++){
	    byte b=(byte)nextNodeID.charAt(i);
	    if (b<0x7a){
		nextNodeID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextNodeID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextNodeID.length();i++){nextNodeID.setCharAt(i,'0');}
	    nextNodeID.append('0');
	}
    }

    void incEdgeID(){
	boolean done=false;
	for (int i=0;i<nextEdgeID.length();i++){
	    byte b=(byte)nextEdgeID.charAt(i);
	    if (b<0x7a){
		nextEdgeID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextEdgeID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextEdgeID.length();i++){nextEdgeID.setCharAt(i,'0');}
	    nextEdgeID.append('0');
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
    Vector processStyledStatement(Resource subj, Property pred, Resource obj){
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
	//also remember Jena entities
	res.add(subj);res.add(pred);res.add(obj);
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

    //given a statement, adds missing entitites to the internal model (called by RDFLoader when importing RDF/XML)
    Vector processStyledStatement(Resource subj, Property pred, Literal lit){
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
	res.add(subj);res.add(pred);res.add(lit);
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
			    if (l.getDatatype()!=null){
				if (l.getLang()!=null){
				    try {
					jenaObject=application.rdfModel.createTypedLiteral(l.getValue(),l.getLang(),l.getDatatype());
				    }
				    catch (com.hp.hpl.jena.datatypes.DatatypeFormatException dfe){
					application.errorMessages.append("A datatype format error occured while creating the following typed literal:\n");
					application.errorMessages.append("Lexical form: "+l.getValue()+"\n");
					application.errorMessages.append("Datatype: "+l.getDatatype()+"\n");
					application.errorMessages.append(dfe.getMessage()+"\n");
					application.reportError=true;
				    }
				}
				else {
				    String lang=Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS ? Editor.DEFAULT_LANGUAGE_IN_LITERALS : "" ;
				    jenaObject=application.rdfModel.createTypedLiteral(l.getValue(),lang,l.getDatatype());
				}
			    }
			    else {
				if (l.getLang()!=null){
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),l.getLang());
				}
				else {
				    String lang=Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS ? Editor.DEFAULT_LANGUAGE_IN_LITERALS : "" ;
				    jenaObject=application.rdfModel.createLiteral(l.getValue(),lang);
				}
			    }
			}
			catch(RDFException ex){
			    javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating literal\n"+o.toString()+"\n"+ex);
			    application.errorMessages.append(ex.getMessage()+"\n");
			    application.reportError=true;
			}
		    }
		    try {
			Statement st=application.rdfModel.createStatement(jenaSubject,jenaPredicate,jenaObject);
			application.rdfModel.add(st);
		    }
		    catch(Exception ex){
			javax.swing.JOptionPane.showMessageDialog(application.cmp,"An error occured while creating the Jena model:\nadding statement "+p.toString()+"("+s.toString()+","+o.toString()+")\n"+ex);
			application.errorMessages.append(ex.getMessage()+"\n");
			application.reportError=true;
		    }
		}
	    }
	}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }
    
    public void save(Model m,File f){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to RDF/XML "+f.toString()+" ...");
	try {//should choose between abbrev and std syntax
	    RDFWriter rdfw;
	    if (Editor.ABBREV_SYNTAX){
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML-ABBREV");
		rdfw=m.getWriter(RDFXMLAB);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }
	    else {
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML");
		rdfw=m.getWriter(RDFXML);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }
	    rdfw.setErrorHandler(new SerializeErrorHandler(application));
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0){//only add complete bindings
		    rdfw.setNsPrefix((String)application.tblp.nsTableModel.getValueAt(i,0),(String)application.tblp.nsTableModel.getValueAt(i,1));
		}
	    }
	    //OutputStreamWriter fw=new OutputStreamWriter(new FileOutputStream(f),ConfigManager.ENCODING);
	    FileOutputStream fos=new FileOutputStream(f);
	    rdfw.write(m,fos,Editor.BASE_URI);
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
	    if (Editor.ABBREV_SYNTAX){
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML-ABBREV");
		rdfw=m.getWriter(RDFXMLAB);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }
	    else {
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML");
		rdfw=m.getWriter(RDFXML);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }	    
	    rdfw.setErrorHandler(new SerializeErrorHandler(application));
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0){//only add complete bindings
		    rdfw.setNsPrefix((String)application.tblp.nsTableModel.getValueAt(i,0),(String)application.tblp.nsTableModel.getValueAt(i,1));
		}
	    }
	    java.io.StringWriter sw=new java.io.StringWriter();
	    rdfw.write(m,sw,Editor.BASE_URI);
	    return sw.getBuffer();
	}
	catch (RDFException ex){application.errorMessages.append("RDF exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	return new StringBuffer();
    }

    public void save(Model m,OutputStream os){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Writing RDF/XML to stream ...");
	try {//should choose between abbrev and std syntax
	    RDFWriter rdfw;
	    if (Editor.ABBREV_SYNTAX){
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML-ABBREV");
		rdfw=m.getWriter(RDFXMLAB);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }
	    else {
		//rdfw=(new RDFWriterFImpl()).getWriter("RDF/XML");
		rdfw=m.getWriter(RDFXML);
		rdfw.setProperty("allowBadURIs","true");  //because Jena2p2 does not allow null (blank) base URIs when checking for bad URIs
		rdfw.setProperty("showXmlDeclaration","true");
		if (Editor.BASE_URI.length()>0 && !Utils.isWhiteSpaceCharsOnly(Editor.BASE_URI)){rdfw.setProperty("xmlbase",Editor.BASE_URI);}
	    }
	    rdfw.setErrorHandler(new SerializeErrorHandler(application));
	    for (int i=0;i<application.tblp.nsTableModel.getRowCount();i++){
		if (((String)application.tblp.nsTableModel.getValueAt(i,0)).length()>0){//only add complete bindings
		    rdfw.setNsPrefix((String)application.tblp.nsTableModel.getValueAt(i,0),(String)application.tblp.nsTableModel.getValueAt(i,1));
		}
	    }
	    //OutputStreamWriter fw=new OutputStreamWriter(new FileOutputStream(f),ConfigManager.ENCODING);
	    rdfw.write(m,os,Editor.BASE_URI);
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Writing RDF/XML to stream ...done");
	}
	catch (RDFException ex){application.errorMessages.append("RDF exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	//catch (IOException ex){application.errorMessages.append("I/O exception in RDFLoader.save() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    public void saveAsN3(Model m,File f){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to Notation 3 "+f.toString()+" ...");
	try {
	    //OutputStreamWriter fw=new OutputStreamWriter(new FileOutputStream(f),ConfigManager.ENCODING);
	    FileOutputStream fos=new FileOutputStream(f);
	    //RDFWriter rdfw=new N3JenaWriter();
	    RDFWriter rdfw=m.getWriter(N3);
	    rdfw.setErrorHandler(new SerializeErrorHandler(application));
	    rdfw.write(m,fos,Editor.BASE_URI);
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to Notation 3 "+f.toString()+" ...done");
	}
	catch (Exception ex){application.errorMessages.append("RDF exception in RDFLoader.saveAsN3() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    public void saveAsTriples(Model m,File f){
	Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to N-Triples "+f.toString()+" ...");
	try {
	    //OutputStreamWriter fw=new OutputStreamWriter(new FileOutputStream(f),ConfigManager.ENCODING);
	    FileOutputStream fos=new FileOutputStream(f);
	    //RDFWriter rdfw=new NTripleWriter();
	    RDFWriter rdfw=m.getWriter(NTRIPLE);
	    rdfw.setErrorHandler(new SerializeErrorHandler(application));
	    rdfw.write(m,fos,Editor.BASE_URI);
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("Exporting to N-Triples "+f.toString()+" ...done");
	}
	catch (Exception ex){application.errorMessages.append("RDF exception in RDFLoader.saveAsTriples() "+ex+"\n");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    void loadAndStyle(Object o,int whichReader){//o can be a java.net.URL, a java.io.File or a java.io.InputStream (plug-ins)
	//whichReader=0 if RDF/XML, 1 if NTriples, 2 if N3
	ProgPanel pp=new ProgPanel("Resetting...","Loading RDF and applying stylesheets");
	PrintWriter pw=null;
	try {
	    pp.setPBValue(5);
	    application.rdfModel=new ModelMem();
	    if (o instanceof File){
		rdfF=(File)o;
		//FileReader fr=new FileReader(rdfF);
		//InputStreamReader isr=new InputStreamReader(new FileInputStream(rdfF),ConfigManager.ENCODING);
		FileInputStream fis=new FileInputStream(rdfF);
		pp.setLabel("Loading local file "+rdfF.toString()+" ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,fis,Editor.BASE_URI);
	    }
	    else if (o instanceof java.net.URL){
		rdfU=(java.net.URL)o;
		pp.setLabel("Loading remote file "+rdfU.toString()+" ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,rdfU.toString());
	    }
	    else if (o instanceof InputStream){
		pp.setLabel("Reading stream ...");
		pw=createDOTFile();
		pp.setPBValue(10);
		pp.setLabel("Initializing ARP(Jena) ...");
		initParser(whichReader,application.rdfModel);
		pp.setPBValue(20);
		pp.setLabel("Parsing RDF ...");
		parser.read(application.rdfModel,(InputStream)o,Editor.BASE_URI);
	    }
	    NsIterator nsit=application.rdfModel.listNameSpaces();
	    while (nsit.hasNext()){
		application.addNamespaceBinding("",nsit.nextNs(),new Boolean(false),true,false);//do it silently, don't override display state and prefix for existing bindings
	    }
	    //build the temp data structures containing the styling rules
	    pp.setPBValue(40);
	    pp.setLabel("Building styling rules ...");
	    application.gssMngr.initStyleTables();
	    //process statements
	    StyledSH sh=new StyledSH(application,application.rdfModel);
	    StmtIterator it=application.rdfModel.listStatements();
	    Statement st;
	    Vector ijStatements=new Vector();
	    while (it.hasNext()){
		st=it.nextStatement();
		if (st.getObject() instanceof Resource){sh.statement(st.getSubject(),st.getPredicate(),(Resource)st.getObject(),ijStatements);}
		else if (st.getObject() instanceof Literal){sh.statement(st.getSubject(),st.getPredicate(),(Literal)st.getObject(),ijStatements);}
		else {System.err.println("Error: RDFLoader.load(): unknown kind of object: "+st.getObject());}
	    }
	    it.close();
	    Vector ijStatement;
	    for (int i=0;i<ijStatements.size();i++){
		ijStatement=(Vector)ijStatements.elementAt(i);
		if (ijStatement.elementAt(2) instanceof IResource){
		    sh.statementDotResource((IResource)ijStatement.elementAt(0),(Resource)ijStatement.elementAt(3),(IProperty)ijStatement.elementAt(1),(Property)ijStatement.elementAt(4),(IResource)ijStatement.elementAt(2),(Resource)ijStatement.elementAt(5));
		    ijStatement.removeAllElements();
		}
		else {//ijStatement.elementAt(2) instanceof ILiteral
		    sh.statementDotLiteral((IResource)ijStatement.elementAt(0),(Resource)ijStatement.elementAt(3),(IProperty)ijStatement.elementAt(1),(Property)ijStatement.elementAt(4),(ILiteral)ijStatement.elementAt(2),(Literal)ijStatement.elementAt(5));
		    ijStatement.removeAllElements();
		}
	    }
	    ijStatements.removeAllElements();
	    ijStatements=null;
	    pp.setPBValue(50);
	    pp.setLabel("Creating temporary SVG file ...");
	    literalLbNum=new Integer(0);  //used to count literals and generate tmp literal labels in the DOT file
  	    generateStyledDOTFile(sh.getVisibleStatements(),sh.getVisibleResources(),pw);
	    sh.clean();
	    svgF=Utils.createTempFile(Editor.m_TmpDir.toString(),"isv",".svg");
	    pp.setPBValue(60);
	    pp.setLabel("Calling GraphViz (this can take several minutes) ...");
	    callDOT(pw);
	    pp.setPBValue(80);
	    pp.setLabel("Parsing SVG ...");
 	    displaySVGAndStyle(application.xmlMngr.parse(svgF,false));
	    cleanMapIDs();//the mapping between SVG and RDF has been done -  we do not need these any longer
	    pp.setPBValue(90);
	    pp.setLabel("Applying styling rules ...");
 	    assignStyleToGraph();
	    application.gssMngr.cleanStyleTables();
	    application.showAnonIds(application.SHOW_ANON_ID);  //show/hide IDs of anonymous resources
	    application.showResourceLabels(Editor.DISP_AS_LABEL);
	    pp.setPBValue(100);
	    pp.setLabel("Deleting temporary files and data structures...");
	    if (Editor.dltOnExit){deleteFiles();}
	    Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),ConfigManager.ANIM_DURATION);
	    application.centerRadarView();
	    application.rdfModel=null; //get rid of it at this point - we will generate it only when appropriate (for instance when exporting)
	}
	catch (IOException ex){application.errorMessages.append("RDFLoader.loadAndStyle() "+ex+"\n");application.reportError=true;}
	catch (RDFException ex2){application.errorMessages.append("RDFLoader.loadAndStyle() "+ex2+"\n");application.reportError=true;}
	//catch (Exception ex3){application.errorMessages.append("RDFLoader.load() "+ex3+"\nPlease verify your directory preferences (GraphViz/DOT might not be configured properly), your default namespace and anonymous node prefix declarations");application.reportError=true;}
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were error/warning messages ('Ctrl+E' to display error log)");application.reportError=false;}
	pp.destroy();
    }

    void generateStyledDOTFile(Hashtable visibleStatements,Hashtable visibleResources,PrintWriter pw){
	int numLiterals=0;
	String key;
	Vector[] netfStatements;  //node-edge and table-form statements for a given subject
	ISVJenaStatement aStatement;
	ISVJenaStatement[] tfStatements;
	for (Enumeration e=visibleStatements.keys();e.hasMoreElements();){
	    key=(String)e.nextElement();
	    netfStatements=(Vector[])visibleStatements.get(key);
	    for (int i=0;i<netfStatements[0].size();i++){//statements to be laid out as node-edge
		aStatement=(ISVJenaStatement)netfStatements[0].elementAt(i);
		if (aStatement.objectIsResource()){printDOTStatementNERO(aStatement,pw);}
		else {printDOTStatementNELO(aStatement,pw);}
	    }
	    tfStatements=new ISVJenaStatement[netfStatements[1].size()];
	    for (int i=0;i<netfStatements[1].size();i++){//statements to be laid out in a table form
		tfStatements[i]=(ISVJenaStatement)netfStatements[1].elementAt(i);
	    }
	    printDOTStatementsTF(tfStatements,pw);
	}
	Vector v;
	for (Enumeration e=visibleResources.elements();e.hasMoreElements();){
	    v=(Vector)e.nextElement();
	    printDOTResource((IResource)v.elementAt(0),(Resource)v.elementAt(1),v.elementAt(2),pw);
	}
    }

    void printDOTStatementNERO(ISVJenaStatement ijs,PrintWriter pw){//statement to be laidout as a node-edge with Resource Object
	if (pw == null) return;
	try {
	    //find out if subject is already in the DOT file by checking if its mapID il null or not
	    boolean nodeAlreadyInDOTFile=true;
	    if (ijs.isubject.getMapID()==null){
		String aUniqueRID=RESOURCE_MAPID_PREFIX+nextNodeID.toString();
		ijs.isubject.setMapID(aUniqueRID);
		incNodeID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (ijs.jsubject.isAnon()) {//b-node (subject)
		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getSubjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId())+"\" [shape=\""+shape+"\",fixedsize=true,URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId())+"\" [shape=\""+shape+"\",URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		}
		//then print the left hand side of the DOT statement
		pw.print("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId()));
	    } 
	    else {//named resources (URI) (subject)
 		String rident=ijs.isubject.getIdent();
 		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getSubjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+rident+"\" [shape=\""+shape+"\",fixedsize=true,URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+rident+"\" [shape=\""+shape+"\",URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		}
		//then print the left hand side of the DOT statement
 		pw.print("\""+rident);
	    }
	    //print the -> statement symbol
	    pw.print("\" -> ");
	    //prepare a new unique ID for the edge representing the property
	    String aUniqueID=nextEdgeID.toString();
	    ijs.ipredicate.setMapID(aUniqueID);
	    incEdgeID();
	    //find out if subject is already in the DOT file by checking if its mapID il null or not
	    nodeAlreadyInDOTFile=true;
	    if (ijs.iobjectr.getMapID()==null){
		String aUniqueRID=RESOURCE_MAPID_PREFIX+nextNodeID.toString();
		ijs.iobjectr.setMapID(aUniqueRID);
		incNodeID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (ijs.jobjectr.isAnon()){//b-node (object)
		/* "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because 
		   since graphviz 1.8 text objects are centered around the coordinates provided with the text element*/
		//print the right-hand side of the statement
		pw.println("\""+ijs.iobjectr.getIdent()+"\" [label=\""+ijs.jpredicate.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");
		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (object)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getObjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+ijs.iobjectr.getIdent()+"\" [shape=\""+shape+"\",fixedsize=true,URL=\""+ijs.iobjectr.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+ijs.iobjectr.getIdent()+"\" [shape=\""+shape+"\",URL=\""+ijs.iobjectr.getMapID()+"\",style=filled];");
		    }
		}
	    }
	    else {//named resources (URI) (object)
		/* "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because 
		   since graphviz 1.8 text objects are centered around the coordinates provided with the text element*/
		//print the right-hand side of the statement
		pw.println("\""+ijs.iobjectr.getIdent()+"\" [label=\""+ijs.jpredicate.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");
		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (object)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getObjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+ijs.iobjectr.getIdent()+"\" [shape=\""+shape+"\",fixedsize=true,URL=\""+ijs.iobjectr.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+ijs.iobjectr.getIdent()+"\" [shape=\""+shape+"\",URL=\""+ijs.iobjectr.getMapID()+"\",style=filled];");
		    }
		}
	    }
	}
	catch (RDFException ex){application.errorMessages.append("Error: printDOTStatementNERO(): "+ex+"\n");application.reportError=true;}	
    }

    void printDOTStatementNELO(ISVJenaStatement ijs,PrintWriter pw){//statement to be laidout as a node-edge with Literal Object
	if (pw == null) return;
	try {
	    //find out if subject is already in the DOT file by checking if its mapID il null or not
	    boolean nodeAlreadyInDOTFile=true;
	    if (ijs.isubject.getMapID()==null){
		String aUniqueRID=RESOURCE_MAPID_PREFIX+nextNodeID.toString();
		ijs.isubject.setMapID(aUniqueRID);
		incNodeID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (ijs.jsubject.isAnon()) {//b-node (subject)
		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getSubjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId())+"\" [shape=\""+shape+"\",fixedsize=true,URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId())+"\" [shape=\""+shape+"\",URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		}
		//then print the left hand side of the DOT statement
		pw.print("\""+Editor.ANON_NODE+IResource.getJenaAnonId(ijs.jsubject.getId()));
	    } 
	    else {//named resources (URI) (subject)
 		String rident=ijs.isubject.getIdent();
 		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    String shape=GraphStylesheet.gss2dotShape(ijs.getSubjectShapeType());
		    if (shape.equals(GraphStylesheet._dotCircle)){
			pw.println("\""+rident+"\" [shape=\""+shape+"\",fixedsize=true,URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		    else {
			pw.println("\""+rident+"\" [shape=\""+shape+"\",URL=\"" +ijs.isubject.getMapID()+"\",style=filled];");
		    }
		}
		//then print the left hand side of the DOT statement
 		pw.print("\""+rident);
	    }
	    //prepare a new unique ID for the edge representing the property
	    String aUniquePID=nextEdgeID.toString();
	    ijs.ipredicate.setMapID(aUniquePID);
	    incEdgeID();
	    //prepare the literal label (truncate and escape some chars from the initial value)
	    String s1 = new String(ijs.jobjectl.getString());
	    s1 = s1.replace('\n', ' ');
	    s1 = s1.replace('\f', ' ');
	    s1 = s1.replace('\r', ' ');
	    if (s1.indexOf('"')!= -1){s1=Utils.replaceString(s1,"\"","\\\"");}
	    // Anything beyond MAX_LIT_CHAR_COUNT chars makes the graph too large
	    String tmpObject=((s1.length()>=Editor.MAX_LIT_CHAR_COUNT) ? s1.substring(0,Editor.MAX_LIT_CHAR_COUNT)+" ..." : s1);
	    //prepare a new unique ID for the literal
	    String aUniqueLID=LITERAL_MAPID_PREFIX+nextNodeID.toString();
	    ijs.iobjectl.setMapID(aUniqueLID);
	    incNodeID();
	    String tmpName = "Literal_"+literalLbNum.toString();
	    literalLbNum=new Integer(literalLbNum.intValue()+1);
	    pw.print("\" -> \""+tmpName);
	    pw.println("\" [label=\""+ijs.jpredicate.getURI()+"\\l\",URL=\""+aUniquePID+"\"];");// "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because since graphviz 1.8 text objects are centered around the coordinates provided with the text element
	    String shape=GraphStylesheet.gss2dotShape(ijs.getObjectShapeType());
	    if (shape.equals(GraphStylesheet._dotCircle)){
		pw.print("\""+tmpName+"\" [shape=\""+shape+"\",fixedsize=true,label=\""+tmpObject+"\\l\",URL=\""+aUniqueLID+"\",style=filled];");
	    }
	    else {
		pw.print("\""+tmpName+"\" [shape=\""+shape+"\",label=\""+tmpObject+"\\l\",URL=\""+aUniqueLID+"\",style=filled];");
	    }
	}
	catch (RDFException ex){application.errorMessages.append("Error: printDOTStatementNELO(): "+ex+"\n");application.reportError=true;}
    }


    //NOT WRITTEN YET
    void printDOTStatementsTF(ISVJenaStatement[] ijss,PrintWriter pw){//statement to be laid out in a table form (both Resource and Literal Objects)
	if (pw == null) return;
	if (ijss.length>0){
	    System.err.println("sorry, table form layout not supported yet");
	}
    }

    void printDOTResource(IResource ir,Resource jr,Object shapeType,PrintWriter pw){//resource without any visible statement
	if (pw == null) return;
	try {
	    /*find out if resource is already in the DOT file by checking if its mapID il null or not
	      this might be the case if a visible statement declaring this resource as its object was
	      processed by the StatementHandler (StyleSH) before an invisible statement involving this
	      resource as subject or object (to be shown) ; to find out if it is already declared in
	      the DOT file, we check whether the mapID is null or not (this is safe as all visible 
	      statements have already been processed)*/
	    boolean nodeAlreadyInDOTFile=true;
	    if (ir.getMapID()==null){
		String aUniqueRID=RESOURCE_MAPID_PREFIX+nextNodeID.toString();
		ir.setMapID(aUniqueRID);
		incNodeID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (ir.isAnon()) {/*b-node (subject) - this is a little weird, showing an unlinked AND anonymous resource... should we really output it?
				there is no mean to identify it (except for its anonID, which has no real value)*/
		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(jr.getId())+"\" [shape=\""+GraphStylesheet.gss2dotShape(shapeType)+"\",URL=\"" +ir.getMapID()+"\",style=filled];");
		}
	    } 
	    else {//named resources (URI) (subject)
 		String rident=ir.getIdent();
 		if (!nodeAlreadyInDOTFile){//declare the node itself if this hasn't been done yet (subject)
		    pw.println("\""+rident+"\" [shape=\""+GraphStylesheet.gss2dotShape(shapeType)+"\",URL=\"" +ir.getMapID()+"\",style=filled];");
		}
	    }
	}
	catch (RDFException ex){application.errorMessages.append("Error: printDOTResource(): "+ex+"\n");application.reportError=true;}
    }

    void displaySVGAndStyle(Document d){
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
	//dealing with SVG output by GraphViz 1.7.11 or later  (but tested only with graphviz 1.9.0)
	NodeList objects=svgRoot.getElementsByTagName("g").item(0).getChildNodes();
	for (int i=0;i<objects.getLength();i++){
	    Node obj=(Node)objects.item(i);
	    if (obj.getNodeType()==Node.ELEMENT_NODE){processSVGNode((Element)obj,true);}
	}
	/*when running isaviz under Linux (and perhaps other posix systems), nodes are too small 
	  (width) w.r.t the text (don't know if it is a graphviz or java problem - anyway,
	  we correct it by adjusting widths a posteriori*/
	/*also do it under windows as some literals, sometimes have the same problem*/
	IResource r;
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    application.geomMngr.correctResourceTextAndShape((IResource)e.nextElement());
	}
	ILiteral l;
	for (Enumeration e=application.literals.elements();e.hasMoreElements();){
	    application.geomMngr.correctLiteralTextAndShape((ILiteral)e.nextElement());
	}
 	SVGReader.setPositionOffset(0,0);  //reset position offset (might affect anything that uses SVGReader methods, like constructors in VPath)
    }

    /*fonts hashtable: key=font family as String; value=hashtable
                                 value=Vector (as many as variants) of Vector of 3 elements:
                                   -1st is an Integer representing the size
                                   -2nd is either Integer(0) or Integer(1) for the weight (normal, bold)
                                   -3rd is either Integer(0) or Integer(1) for the style (normal, italic)
				   -4th is the corresponding java.awt.Font*/
    Font rememberFont(Hashtable fonts,String family,int size,short weight,short style){
	if (family.equals(Editor.vtmFontName) && (size==Editor.vtmFontSize) && Utils.sameFontStyleAs(Editor.vtmFont,weight,style)){
	    return null;
	}
	else {
	    boolean isBold=Utils.isBold(weight);
	    boolean isItalic=Utils.isItalic(style);
	    Font res=null;
	    if (fonts.containsKey(family)){
		Vector v=(Vector)fonts.get(family);
		Vector v2;
		int sz,wt,st;
		for (int i=0;i<v.size();i++){
		    v2=(Vector)v.elementAt(i);
		    sz=((Integer)v2.elementAt(0)).intValue();
		    wt=((Integer)v2.elementAt(1)).intValue();
		    st=((Integer)v2.elementAt(2)).intValue();
		    if (isBold){
			if (isItalic){
			    if (sz==size && wt==1 && st==1){res=(Font)v2.elementAt(3);break;}
			}
			else {
			    if (sz==size && wt==1 && st==0){res=(Font)v2.elementAt(3);break;}
			}
		    }
		    else {
			if (isItalic){
			    if (sz==size && wt==0 && st==1){res=(Font)v2.elementAt(3);break;}
			}
			else {
			    if (sz==size && wt==0 && st==0){res=(Font)v2.elementAt(3);break;}
			}
		    }
		}
		if (res==null){//could not find a font matching what we need, have to create a new one
		    v2=new Vector();
		    v2.add(new Integer(size));
		    v2.add(new Integer(((isBold) ? 1 : 0)));
		    v2.add(new Integer(((isItalic) ? 1 : 0)));
		    int nst=Font.PLAIN;
		    if (isBold){
			if (isItalic){nst=Font.BOLD+Font.ITALIC;}
			else {nst=Font.BOLD;}
		    }
		    else if (isItalic){nst=Font.ITALIC;}
		    res=new Font(family,nst,size);
		    v2.add(res);
		    v.add(v2);
		    //System.err.println("Members in the family, but not this variant "+res.toString());
		}
		//else {System.err.println("Found an existing font for "+res.toString());}
	    }
	    else {
		Vector v=new Vector();
		Vector v2=new Vector();
		v2.add(new Integer(size));
		v2.add(new Integer(((isBold) ? 1 : 0)));
		v2.add(new Integer(((isItalic) ? 1 : 0)));
		int nst=Font.PLAIN;
		if (isBold){
		    if (isItalic){nst=Font.BOLD+Font.ITALIC;}
		    else {nst=Font.BOLD;}
		}
		else if (isItalic){nst=Font.ITALIC;}
		res=new Font(family,nst,size);
		v2.add(res);
		v.add(v2);
		fonts.put(family,v);
		//System.err.println("Had to create a whole new thing for "+res.toString());
	    }
	    return res;
	}
    }

    void assignStyleToGraph(){
	Hashtable strokeWidths=new Hashtable();  //key=stroke width as a Float, value a=corresponding java.awt.BasicStroke
	Hashtable fonts=new Hashtable();  //temporarily store all fonts needed to style the graph
	//key=font family as String; value=hashtable
	//                             key=font size as Integer
	//                             value=Vector of 3 elements:
	//                               -1st is an Integer representing the size
	//                               -2nd is either Short(0) or Short(1) for the weight (normal, bold)
	//                               -3rd is either Short(0) or Short(1) for the style (normal, italic)
	int fillind;
	int strokeind;
	Color fill;
	Color stroke;
	float width;
	String ffamily;
	int fsize;
	short fweight,fstyle;
	boolean hide;
	Glyph g1,g2,g3;
	IResource r;
	Float swKey;
	BasicStroke swValue;
	Font font;
	StyleInfoR sir;
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    r=(IResource)e.nextElement();
	    if (r.isVisuallyRepresented()){
		sir=application.gssMngr.getStyle(r);
		hide=(sir.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		fill=sir.getFillColor();
		stroke=sir.getStrokeColor();
		width=sir.getStrokeWidth().floatValue();
		ffamily=sir.getFontFamily();
		fsize=sir.getFontSize().intValue();
		fweight=sir.getFontWeight().shortValue();
		fstyle=sir.getFontStyle().shortValue();
		g1=r.getGlyph();
		g2=r.getGlyphText();
		if (fill!=null){
		    fillind=ConfigManager.addColor(fill);
		    r.setFillColor(fillind);
		}
		else {
		    fillind=ConfigManager.defaultRFIndex;
		    r.setFillColor(fillind);
		}
		if (stroke!=null){
		    strokeind=ConfigManager.addColor(stroke);
		    r.setStrokeColor(strokeind);
		}
		else {
		    strokeind=ConfigManager.defaultRTBIndex;
		    r.setStrokeColor(strokeind);
		}
		if (width>0.0f && width!=1.0f){
		    swKey=new Float(width);
		    if (strokeWidths.containsKey(swKey)){
			g1.setStroke((BasicStroke)strokeWidths.get(swKey));
		    }
		    else {
			swValue=new BasicStroke(width);
			g1.setStroke(swValue);
			strokeWidths.put(swKey,swValue);
		    }
		}
		if ((font=rememberFont(fonts,ffamily,fsize,fweight,fstyle))!=null){
		    g2.setSpecialFont(font);
		}
// 		if (hide){
// 		    r.setVisible(false);
// 		}
	    }
	}
	ILiteral l;
	StyleInfoL sil;
	for (int i=0;i<application.literals.size();i++){
	    l=(ILiteral)application.literals.elementAt(i);
	    if (l.isVisuallyRepresented()){
		sil=application.gssMngr.getStyle(l);
		hide=(sil.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		fill=sil.getFillColor();
		stroke=sil.getStrokeColor();
		width=sil.getStrokeWidth().floatValue();
		ffamily=sil.getFontFamily();
		fsize=sil.getFontSize().intValue();
		fweight=sil.getFontWeight().shortValue();
		fstyle=sil.getFontStyle().shortValue();
		g1=l.getGlyph();
		g2=l.getGlyphText();
		if (fill!=null){
		    fillind=ConfigManager.addColor(fill);
		    l.setFillColor(fillind);
		}
		else {
		    fillind=ConfigManager.defaultLFIndex;
		    l.setFillColor(fillind);
		}
		if (stroke!=null){
		    strokeind=ConfigManager.addColor(stroke);
		    l.setStrokeColor(strokeind);
		}
		else {
		    strokeind=ConfigManager.defaultLTBIndex;
		    l.setStrokeColor(strokeind);
		}
		if (width>0.0f && width!=1.0f){
		    swKey=new Float(width);
		    if (strokeWidths.containsKey(swKey)){
			g1.setStroke((BasicStroke)strokeWidths.get(swKey));
		    }
		    else {
			swValue=new BasicStroke(width);
			g1.setStroke(swValue);
			strokeWidths.put(swKey,swValue);
		    }
		}
		if ((font=rememberFont(fonts,ffamily,fsize,fweight,fstyle))!=null){
		    g2.setSpecialFont(font);
		}
// 		if (hide){
// 		    l.setVisible(false);
// 		}
	    }
	}
	IProperty p;
	StyleInfoP sip;
	Vector v;
	for (Enumeration e=application.propertiesByURI.elements();e.hasMoreElements();){
	    v=(Vector)e.nextElement();
	    for (int i=0;i<v.size();i++){
		p=(IProperty)v.elementAt(i);
		if (p.isVisuallyRepresented()){
		    sip=application.gssMngr.getStyle(p);
		    hide=(sip.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		    stroke=sip.getStrokeColor();
		    width=sip.getStrokeWidth().floatValue();
		    ffamily=sip.getFontFamily();
		    fsize=sip.getFontSize().intValue();
		    fweight=sip.getFontWeight().shortValue();
		    fstyle=sip.getFontStyle().shortValue();
		    g1=p.getGlyph();
		    g2=p.getGlyphText();
		    g3=p.getGlyphHead();
		    if (stroke!=null){
			strokeind=ConfigManager.addColor(stroke);
			p.setStrokeColor(strokeind);
			p.setTextColor(strokeind);
		    }
		    else {
			p.setStrokeColor(ConfigManager.defaultPBIndex);
			p.setTextColor(ConfigManager.defaultPTIndex);
		    }
		    if (width>0.0f && width!=1.0f){
			swKey=new Float(width);
			if (strokeWidths.containsKey(swKey)){
			    g1.setStroke((BasicStroke)strokeWidths.get(swKey));
			}
			else {
			    swValue=new BasicStroke(width);
			    g1.setStroke(swValue);
			    strokeWidths.put(swKey,swValue);
			}
		    }
		    if ((font=rememberFont(fonts,ffamily,fsize,fweight,fstyle))!=null){
			g2.setSpecialFont(font);
		    }
// 		    if (hide){
// 			p.setVisible(false);
// 		    }
		}
	    }
	}
	strokeWidths.clear();
	fonts.clear();
	strokeWidths=null;
	fonts=null;
	boolean noVisibleAttachedStatement=true;
	boolean inspectNextProperty=true;
	/*the following code hides entities for which visibility=hidden. 
	  Stuff that had display=none has already been removed when generating the DOT file.
	  The code here could be "optimized" by remembering what's already hidden as a side effect
	  of hiding something else so that it does not get hidden twice (or more)
	  But this means constructing new data structures, and the benefits (in terms of speed) are not
	  obvious, as hiding something already hidden does not take much time at the ZVTM level, so
	  constructing the data structures and looking for stuff in them might actually take longer
	*/
	for (Enumeration e=application.resourcesByURI.elements();e.hasMoreElements();){
	    r=(IResource)e.nextElement();
	    if (r.isVisuallyRepresented()){//this checks that the resource has a representation (not the case if display=none)
		sir=application.gssMngr.getStyle(r);
		hide=(sir.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		if (hide){//if the resource has gss:visibility=hidden, hide it
		    r.setVisible(false);
		    v=r.getIncomingPredicates();//also hide all incoming predicates (predicates for which the resource is the object)
		    if (v!=null){
			for (int i=0;i<v.size();i++){
			    p=(IProperty)v.elementAt(i);
			    p.setVisible(false);
			}
		    }
		    v=r.getOutgoingPredicates();//and all outgoing predicates (predicates for which the resource is the subject)
		    if (v!=null){
			for (int i=0;i<v.size();i++){
			    p=(IProperty)v.elementAt(i);
			    p.setVisible(false);
			    if (p.getObject() instanceof ILiteral){p.getObject().setVisible(false);}
			}
		    }
		}
		else if (!TablePanel.SHOW_ISOLATED_NODES){//else if the resource has visibility=visible but if orphan resources should be hidden
		    noVisibleAttachedStatement=true;//check whether there is at least one visible property coming to or from this resource
		    inspectNextProperty=true;
		    v=r.getIncomingPredicates();//if it is the case, the resource should not be hidden, if not it should be hidden as it is considered
		    if (v!=null){               //an orpahn resource from the graphical point of view (although there might be resources attached to it)
			for (int i=0;i<v.size();i++){
			    p=(IProperty)v.elementAt(i);
			    if (p.isVisuallyRepresented()){
				if (application.gssMngr.getStyle(p).getVisibility().equals(GraphStylesheet.VISIBILITY_VISIBLE)){
				    noVisibleAttachedStatement=false;
				    inspectNextProperty=false;break;
				}
			    }
			}
		    }
		    if (inspectNextProperty){
			v=r.getOutgoingPredicates();
			if (v!=null){
			    for (int i=0;i<v.size();i++){
				p=(IProperty)v.elementAt(i);
				if (p.isVisuallyRepresented()){
				    if (application.gssMngr.getStyle(p).getVisibility().equals(GraphStylesheet.VISIBILITY_VISIBLE)){
					noVisibleAttachedStatement=false;
					break;
				    }
				}
			    }
			}
		    }
		    if (noVisibleAttachedStatement){r.setVisible(false);}
		}
	    }
	}
	for (int i=0;i<application.literals.size();i++){
	    l=(ILiteral)application.literals.elementAt(i);
	    if (l.isVisuallyRepresented()){
		sil=application.gssMngr.getStyle(l);
		hide=(sil.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		if (hide){
		    l.setVisible(false);
		    l.getIncomingPredicate().setVisible(false);
		}
	    }
	}
	for (Enumeration e=application.propertiesByURI.elements();e.hasMoreElements();){
	    v=(Vector)e.nextElement();
	    for (int i=0;i<v.size();i++){
		p=(IProperty)v.elementAt(i);
		if (p.isVisuallyRepresented()){
		    sip=application.gssMngr.getStyle(p);
		    hide=(sip.getVisibility().equals(GraphStylesheet.VISIBILITY_HIDDEN)) ? true : false;
		    if (hide){
			p.setVisible(false);
			if (p.getObject() instanceof ILiteral){p.getObject().setVisible(false);}
		    }
		}
	    }
	}
    }

    public void error(Exception e){
	String message="RDFErrorHandler:Error: "+format(e);
	application.errorMessages.append(message+"\n");
	application.reportError=true;
    }
    
    public void fatalError(Exception e){
	String message="RDFErrorHandler:Fatal Error: "+format(e);
	application.errorMessages.append(message+"\n");
	application.reportError=true;
    }

    public void warning(Exception e){
	String message="RDFErrorHandler:Warning: "+format(e);
	application.errorMessages.append(message+"\n");
	application.reportError=true;
    }

    private static String format(Exception e){
	String msg=e.getMessage();
	if (msg==null){msg=e.toString();}
	if (e instanceof org.xml.sax.SAXParseException){
	    org.xml.sax.SAXParseException spe=(org.xml.sax.SAXParseException)e;
	    return msg + "[Line = " + spe.getLineNumber() + ", Column = " + spe.getColumnNumber() + "]";
	}
	else {
	    return e.toString();
	}
    }

    protected Glyph getResourceShape(IResource r,Element a,boolean styling){//we get the surrounding a element (should contain an ellipse/polygon/circle/... and a text)
	if (styling){
	    Object shape=application.gssMngr.getStyle(r).getShape();
	    if (shape instanceof Integer){
		if (shape.equals(Style.ELLIPSE)){//if the stylesheet asks a ellipse, dot generates an SVG ellipse representing the ellipse
		    NodeList content=a.getElementsByTagName("ellipse");
		    return SVGReader.createEllipse((Element)content.item(0));
		}
		else if (shape.equals(Style.CIRCLE)){//if the stylesheet asks a circle, dot generates an SVG ellipse representing the circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VCircle(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.DIAMOND)){//if the stylesheet asks a diamond, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VDiamond(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.OCTAGON)){//if the stylesheet asks a octagon, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VOctagon(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.TRIANGLEN)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangle(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.TRIANGLES)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)Math.PI);
		}
		else if (shape.equals(Style.TRIANGLEE)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)-Math.PI/2.0f);
		}
		else if (shape.equals(Style.TRIANGLEW)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)Math.PI/2.0f);
		}
		else if (shape.equals(Style.RECTANGLE)){//if the stylesheet asks a rectangle, dot generates an SVG polygon representing the rectangle
		    NodeList content=a.getElementsByTagName("polygon");
		    return SVGReader.createRectangleFromPolygon((Element)content.item(0));
		}
		else {//for robustness (should not happen)
		    System.err.println("Error: RDFLoader.getLiteralShape(): requested shape type unknown: "+shape.toString());
		    return new VRectangle(0,0,0,1,1,Color.white);
		}
	    }
	    else if (shape instanceof CustomShape){
		NodeList content=a.getElementsByTagName("ellipse");
		VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		float[] vertices=((CustomShape)shape).getVertices();
		Float orient=((CustomShape)shape).getOrientation();
		return new VShape(e.vx,e.vy,0,e.getHeight(),vertices,e.getColor(),(orient!=null) ? orient.floatValue(): 0.0f);
	    }
	    else {//for robustness (should not happen)
		System.err.println("Error: RDFLoader.getResourceShape(): requested shape type unknown: "+shape.toString());
		return new VEllipse(0,0,0,1,1,Color.white);
	    }
	}
	else {
	    NodeList content=a.getElementsByTagName("ellipse");
	    return SVGReader.createEllipse((Element)content.item(0));
	}
    }

    protected Glyph getLiteralShape(ILiteral l,Element a,boolean styling){//we get the surrounding a element (should contain an ellipse/polygon/circle/... and a text)
	if (styling){
	    Object shape=application.gssMngr.getStyle(l).getShape();
	    if (shape instanceof Integer){
		if (shape.equals(Style.RECTANGLE)){//if the stylesheet asks a rectangle, dot generates an SVG polygon representing the rectangle
		    NodeList content=a.getElementsByTagName("polygon");
		    return SVGReader.createRectangleFromPolygon((Element)content.item(0));
		}
		else if (shape.equals(Style.CIRCLE)){//if the stylesheet asks a circle, dot generates an SVG ellipse representing the circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VCircle(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.DIAMOND)){//if the stylesheet asks a diamond, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VDiamond(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.OCTAGON)){//if the stylesheet asks a octagon, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VOctagon(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.TRIANGLEN)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangle(e.vx,e.vy,0,e.getHeight(),e.getColor());
		}
		else if (shape.equals(Style.TRIANGLES)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)Math.PI);
		}
		else if (shape.equals(Style.TRIANGLEE)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)-Math.PI/2.0f);
		}
		else if (shape.equals(Style.TRIANGLEW)){//if the stylesheet asks a triangle, dot generates an SVG ellipse representing the bounding circle
		    NodeList content=a.getElementsByTagName("ellipse");
		    VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		    return new VTriangleOr(e.vx,e.vy,0,e.getHeight(),e.getColor(),(float)Math.PI/2.0f);
		}
		else if (shape.equals(Style.ELLIPSE)){//if the stylesheet asks a ellipse, dot generates an SVG ellipse representing the ellipse
		    NodeList content=a.getElementsByTagName("ellipse");
		    return SVGReader.createEllipse((Element)content.item(0));
		}
		else {//for robustness (should not happen)
		    System.err.println("Error: RDFLoader.getLiteralShape(): requested shape type unknown: "+shape.toString());
		    return new VRectangle(0,0,0,1,1,Color.white);
		}
	    }
	    else if (shape instanceof CustomShape){
		NodeList content=a.getElementsByTagName("ellipse");
		VEllipse e=SVGReader.createEllipse((Element)content.item(0));
		float[] vertices=((CustomShape)shape).getVertices();
		Float orient=((CustomShape)shape).getOrientation();
		return new VShape(e.vx,e.vy,0,e.getHeight(),vertices,e.getColor(),(orient!=null) ? orient.floatValue(): 0.0f);
	    }
	    else {//for robustness (should not happen)
		System.err.println("Error: RDFLoader.getLiteralShape(): requested shape type unknown: "+shape.toString());
		return new VRectangle(0,0,0,1,1,Color.white);
	    }
	}
	else {
	    NodeList content=a.getElementsByTagName("polygon");
	    return SVGReader.createRectangleFromPolygon((Element)content.item(0));
	}
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
	    String aUniqueID=application.rdfLdr.nextEdgeID.toString();
	    pred.setMapID(aUniqueID);
	    application.rdfLdr.incEdgeID();
	    boolean nodeAlreadyInDOTFile=true;
	    if (obj.getMapID()==null){
		String aUniqueRID=RDFLoader.RESOURCE_MAPID_PREFIX+application.rdfLdr.nextNodeID.toString();
		obj.setMapID(aUniqueRID);
		application.rdfLdr.incNodeID();
		nodeAlreadyInDOTFile=false;
	    }
	    if (o.isAnon()){
		this.pw.println("\""+obj.getIdent()+"\" [label=\""+p.getURI()+"\\l\",URL=\""+aUniqueID+"\"];");
		/* "\l" at the end of the label is here to generate a left-aliggned attribute in the SVG file because 
		   since graphviz 1.8 text objects are centered around the coordinates provided with the text element*/
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
		String aUniquePID=application.rdfLdr.nextEdgeID.toString();
		pred.setMapID(aUniquePID);
		application.rdfLdr.incEdgeID();
		String aUniqueLID=RDFLoader.LITERAL_MAPID_PREFIX+application.rdfLdr.nextNodeID.toString();
		lit.setMapID(aUniqueLID);
		application.rdfLdr.incNodeID();
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
		    String aUniqueRID=RDFLoader.RESOURCE_MAPID_PREFIX+application.rdfLdr.nextNodeID.toString();
		    ir.setMapID(aUniqueRID);
		    application.rdfLdr.incNodeID();
		    nodeAlreadyInDOTFile=false;
		}
		if (subj.isAnon()) {
		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+Editor.ANON_NODE+IResource.getJenaAnonId(subj.getId())+"\" [URL=\"" +ir.getMapID()+"\"];");}
		    this.pw.print("\""+Editor.ANON_NODE+IResource.getJenaAnonId(subj.getId()));
		} 
		else {
// 		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+subj.getURI()+"\" [URL=\"" +ir.getMapID()+"\"];");}
// 		    this.pw.print("\""+subj.getURI());
		    String rident=ir.getIdent();
		    if (!nodeAlreadyInDOTFile){this.pw.println("\""+rident+"\" [URL=\"" +ir.getMapID()+"\"];");}
		    this.pw.print("\""+rident);
		}
	    }
	    catch (RDFException ex){application.errorMessages.append("Error: SH.printFirstPart(): "+ex+"\n");application.reportError=true;}
	}
    }

    //Statement handler inner class  (with support for  stylesheets)---------------------
    private class StyledSH {
	Editor application;
	Model model;
	Hashtable visibleStatements;  /*stores all statements that should be output in the DOT file
					key=subject resource URI
					value=array of 2 vectors
					-1st vector (contains properties to be laid out as node-edge)
					items are instances of ISVJenaStatement
					-2nd vector (contains properties to be laid out in a table form)
					items are instances of ISVJenaStatement*/

	Hashtable visibleResources; /*stores unlinked resources that should nevertheless be output in the DOT file
				      (some may actually be linked to a visible statement, but we will filter them
				      as explained in a comment below)
				      key=resource URI
				      value=vector with 3 elements
				      -1st element is the IResource (IsaViz)
				      -2nd element is the Resource (Jena)
				      -3rd element is the shape type (one of Style.{ELLIPSE,RECTANGLE,CIRCLE,DIAMOND,OCTAGON,TRIANGLEN,TRIANGLES,TRIANGLEE,TRIANGLEW} or a CustomShape)*/

	public StyledSH(Editor app,Model m){
	    this.application=app;
	    this.model=m;
	    visibleStatements=new Hashtable();
	    visibleResources=new Hashtable();
	}

	void addVisibleStatement(IResource subject,IProperty predicate,IResource object,Resource s,Property p,Resource o,boolean inTable,Object sShapeType,Object oShapeType){/*sShapeType and oShapeType are (each) one of Style.{ELLIPSE,RECTANGLE,CIRCLE,DIAMOND,OCTAGON,TRIANGLEN,TRIANGLES,TRIANGLEE,TRIANGLEW} or a CustomShape */
	    ISVJenaStatement pair=new ISVJenaStatement(subject,predicate,object,s,p,o,sShapeType,oShapeType);
	    String sURI=subject.getIdent();
	    if (visibleStatements.containsKey(sURI)){
		Vector[] ar=(Vector[])visibleStatements.get(sURI);
		if (inTable){
		    ar[1].add(pair);
		}
		else {
		    ar[0].add(pair);
		}
	    }
	    else {
		Vector[] ar=new Vector[2];
		Vector v1=new Vector();
		Vector v2=new Vector();
		if (inTable){v2.add(pair);}
		else {v1.add(pair);}
		ar[0]=v1;
		ar[1]=v2;
		visibleStatements.put(sURI,ar);
	    }
	    String oURI=object.getIdent();
	    //if the subject and/or object of this statement was previously put in visibleResources, remove it from there
	    if (visibleResources.containsKey(sURI)){
		visibleResources.remove(sURI);
	    }
	    if (visibleResources.containsKey(oURI)){
		visibleResources.remove(oURI);
	    }
	}

	void addVisibleStatement(IResource subject,IProperty predicate,ILiteral object,Resource s,Property p,Literal o,boolean inTable,Object sShapeType,Object oShapeType){/*sShapeType and oShapeType are (each) one of Style.{ELLIPSE,RECTANGLE,CIRCLE,DIAMOND,OCTAGON,TRIANGLEN,TRIANGLES,TRIANGLEE,TRIANGLEW} or a CustomShape */
	    ISVJenaStatement pair=new ISVJenaStatement(subject,predicate,object,s,p,o,sShapeType,oShapeType);
	    String sURI=subject.getIdent();
	    if (visibleStatements.containsKey(sURI)){
		Vector[] ar=(Vector[])visibleStatements.get(sURI);
		if (inTable){
		    ar[1].add(pair);
		}
		else {
		    ar[0].add(pair);
		}
	    }
	    else {
		Vector[] ar=new Vector[2];
		Vector v1=new Vector();
		Vector v2=new Vector();
		if (inTable){v2.add(pair);}
		else {v1.add(pair);}
		ar[0]=v1;
		ar[1]=v2;
		visibleStatements.put(sURI,ar);
	    }
	    //if the subject of this statement was previously put in visibleResources, remove it from there
	    if (visibleResources.containsKey(sURI)){
		visibleResources.remove(sURI);
	    }
	}

	/*although we remove resources from visibleResources when they are part of a visible statement,
	  there is still the possibility that a resource can be added to visibleResources after it has
	  been processed as an object in a visible statement (since we do not keep an easily accessible
	  record of the objects of visible statements. It does not really matter, as we are going to filter
	  the list of visible resources again when generating the DOT file, including only the ones that do
	  not yet have a mapID (and since we do that after generating all visible statements, there is no
	  ambiguity)
	*/

	void addVisibleResource(IResource ir,Resource jr,Object shapeType){/*shapeType is one of Style.{ELLIPSE,RECTANGLE,CIRCLE,DIAMOND,OCTAGON,TRIANGLEN,TRIANGLES,TRIANGLEE,TRIANGLEW} or a CustomShape */
	    String rURI=ir.getIdent();
	    if (!visibleStatements.containsKey(rURI)){
		if (!visibleResources.containsKey(rURI)){
		    Vector v=new Vector();
		    v.add(ir);
		    v.add(jr);
		    v.add(shapeType);
		    visibleResources.put(rURI,v);
		}
	    }
	}

        /*
         *handler for a Resource/Property/Resource triple (S/P/O).
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statement(Resource subj, Property pred, Resource obj,Vector stmts){
	    stmts.add(processStyledStatement(subj,pred,obj));
// 	    statementDotResource((IResource)v.elementAt(0),subj,(IProperty)v.elementAt(1),pred,(IResource)v.elementAt(2),obj);
        }

        /*
         * Generic handler for a Resource/Property/Literal triple (S/P/O).
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statement(Resource subj, Property pred, Literal lit,Vector stmts){
	    stmts.add(processStyledStatement(subj,pred,lit));
// 	    statementDotLiteral((IResource)v.elementAt(0),subj,(IProperty)v.elementAt(1),pred,(ILiteral)v.elementAt(2),lit);
        }

        public void statementDotResource(IResource subj,Resource s,IProperty pred,Property p,IResource obj,Resource o){
	    StyleInfoR ssi=application.gssMngr.computeAndGetStyle(subj);
	    StyleInfoP psi=application.gssMngr.computeAndGetStyle(pred);
	    StyleInfoR osi=application.gssMngr.computeAndGetStyle(obj);
	    Integer subjectVisibility=ssi.getVisibility();
	    boolean subjectTableForm=(ssi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Integer predicateVisibility=psi.getVisibility();
	    boolean predicateTableForm=(psi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Integer objectVisibility=osi.getVisibility();
	    boolean objectTableForm=(osi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Object sShapeType=ssi.getShape();   //subject shape type
	    Object oShapeType=osi.getShape();   //object shape type
	    if (predicateVisibility.equals(GraphStylesheet.DISPLAY_NONE)){
		//if the statement itself should be hidden, we still want
		//to show the subject and object resources (provided they want to be visible) even if it does not have
		//any visible resource attached
		if (TablePanel.SHOW_ISOLATED_NODES && !(subjectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || subjectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){addVisibleResource(subj,s,sShapeType);}
		if (TablePanel.SHOW_ISOLATED_NODES && !(objectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || objectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){addVisibleResource(obj,o,oShapeType);}
	    }
	    else {
		if (!(subjectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || subjectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){
		    if (objectVisibility.equals(GraphStylesheet.DISPLAY_NONE)){
			if (TablePanel.SHOW_ISOLATED_NODES){addVisibleResource(subj,s,sShapeType);}
			//if the statement itself should be hidden (because the object is hidden),
			//we still want to show the subject resource even if it does not have any visible resource attached
			//(provided it wants to be visible)
		    }
		    else {//everything (subject/predicate/object) is visible, show the entire statement
			addVisibleStatement(subj,pred,obj,s,p,o,predicateTableForm || objectTableForm,sShapeType,oShapeType);
			/*predicateTableForm || objectTableForm means that only one table_form attribute is needed to display a property in a table form (in the property or in the object)*/
		    }
		}
		else {
		    if (!(objectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || objectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){
			if (TablePanel.SHOW_ISOLATED_NODES){addVisibleResource(obj,o,oShapeType);}
			//if the statement itself should be hidden (because the subject is hidden),
			//we still want to show the object resource even if it does not have any visible resource attached
			//(provided it wants to be visible)
		    }
		}
	    }
        }

        public void statementDotLiteral(IResource subj,Resource s,IProperty pred,Property p,ILiteral lit,Literal l){
	    StyleInfoR ssi=application.gssMngr.computeAndGetStyle(subj);
	    StyleInfoP psi=application.gssMngr.computeAndGetStyle(pred);
	    StyleInfoL osi=application.gssMngr.computeAndGetStyle(lit);
	    Integer subjectVisibility=ssi.getVisibility();
	    boolean subjectTableForm=(ssi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Integer predicateVisibility=psi.getVisibility();
	    boolean predicateTableForm=(psi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Integer objectVisibility=osi.getVisibility();
	    boolean objectTableForm=(osi.getVisibility().equals(GraphStylesheet.TABLE_FORM)) ? true : false ;
	    Object sShapeType=ssi.getShape();   //subject shape type
	    Object oShapeType=osi.getShape();   //object shape type
	    if (predicateVisibility.equals(GraphStylesheet.DISPLAY_NONE)){
		if (TablePanel.SHOW_ISOLATED_NODES && !(subjectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || subjectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){addVisibleResource(subj,s,sShapeType);}//if the statement itself should be hidden, we still want
		//to show the subject resource even if it does not have any visible resource attached
		//do not do the same for the literal, as there is no point in showing it if the statement is hidden
		//(since no new or existing statementcan be attached to it)
	    }
	    else {
		if (!(subjectVisibility.equals(GraphStylesheet.DISPLAY_NONE) || subjectVisibility.equals(GraphStylesheet.VISIBILITY_HIDDEN))){
		    if (objectVisibility.equals(GraphStylesheet.DISPLAY_NONE)){
			if (TablePanel.SHOW_ISOLATED_NODES){addVisibleResource(subj,s,sShapeType);}//if the statement itself should be hidden (because the object is hidden),
			//we still want to show the subject resource even if it does not have any visible resource attached
		    }
		    else {//everything (subject/predicate/object) is visible, show the entire statement
			addVisibleStatement(subj,pred,lit,s,p,l,predicateTableForm || objectTableForm,sShapeType,oShapeType);
			/*predicateTableForm || objectTableForm means that only one table_form attribute is needed to display a property in a table form (in the property or in the object)*/
		    }
		}
	    }
        }

	void clean(){
	    visibleStatements.clear();
	    visibleStatements=null;
	    visibleResources.clear();
	    visibleResources=null;
	}

	Hashtable getVisibleStatements(){
	    return visibleStatements;
	}

	Hashtable getVisibleResources(){
	    return visibleResources;
	}
	
    }

}

class SerializeErrorHandler implements RDFErrorHandler {

    Editor application;

    SerializeErrorHandler(Editor app){
	this.application=app;
    }
    
    public void error(java.lang.Exception ex){
	application.errorMessages.append("An error occured while exporting "+ex+"\n");application.reportError=true;
    }

    public void fatalError(java.lang.Exception ex){
	application.errorMessages.append("An fatal error occured while exporting "+ex+"\n");application.reportError=true;
    }

    public void warning(java.lang.Exception ex){
	application.errorMessages.append("Warning "+ex+"\n");application.reportError=true;
    }
    
}

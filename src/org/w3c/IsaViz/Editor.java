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

import java.awt.Font;
import java.awt.Color;
import java.awt.geom.*;
import javax.swing.JOptionPane;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.File;
import java.io.InputStreamReader;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import com.xerox.VTM.glyphs.VRectangle;
import com.xerox.VTM.glyphs.VEllipse;
import com.xerox.VTM.glyphs.VTriangleOr;
import com.xerox.VTM.glyphs.VText;
import com.xerox.VTM.glyphs.VPath;
import com.xerox.VTM.glyphs.VSegment;
import com.xerox.VTM.glyphs.VQdCurve;
import com.xerox.VTM.glyphs.Glyph;
import com.xerox.VTM.svg.SVGWriter;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.DOMImplementationImpl;
import org.w3c.dom.*;

//www.xrce.xerox.com/
import com.xerox.VTM.engine.*;
import com.xerox.VTM.svg.SVGReader;

//www.hpl.hp.co.uk/people/jjc/arp/apidocs/index.html
import com.hp.hpl.jena.rdf.arp.*;
import com.hp.hpl.mesa.rdf.jena.model.Model;
import com.hp.hpl.mesa.rdf.jena.model.Resource;
import com.hp.hpl.mesa.rdf.jena.model.Property;
import com.hp.hpl.mesa.rdf.jena.model.Literal;
import com.hp.hpl.mesa.rdf.jena.mem.ModelMem;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.NsIterator;

/**This is the main IsaViz class - usd to launch the application. You can pass a file as argument. If its extension is .isv, isaViz will attempt to load it as an IsaViz project. Otherwise, it will try to import it through Jena+GraphViz/Dot. <br> It contains the main definitions, references to all managers and GUI components + the internal model and methods to modify it.*/

public class Editor {

    /*namespaces and default prefixes*/
    static String isavizURI="http://www.w3.org/2001/10/IsaViz";     /*isaviz namespace*/
    static String RDFMS_NAMESPACE_PREFIX="rdf";                     /*RDF model and syntax namespace*/
    static String RDFMS_NAMESPACE_URI="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    static String RDFS_NAMESPACE_PREFIX="rdfs"; /*RDF Schema namespace*/
    static String RDFS_NAMESPACE_URI="http://www.w3.org/2000/01/rdf-schema#";
    static String DEFAULT_NAMESPACE = "online:"; /*The string to use for a namespace name when no namespace is available - e.g. for the RDF that is directly entered into the input form*/
    static String ANON_NODE = "genid:";  /*The string to use for to prefix anonymous nodes*/

    /*Misc. constants*/
    /*value displayed in the property type table for the auto-numbering membership property constructor (should begin with a string identifying it uniquely based on its first 3 chars like '_??' since a test depends on this in createNewProperty())*/
    static String MEMBERSHIP_PROP_CONSTRUCTOR="_??   (Membership property auto-numbering: _1, _2, ...)";
    /*default language used in literals*/
    static String DEFAULT_LANGUAGE_IN_LITERALS="en"; 
    /*if true, xml:lang is added for each literal, even when lang is default*/
    static boolean ALWAYS_INCLUDE_LANG_IN_LITERALS=false;
    /*tells whether RDFWriter should output standard or abbreviated syntax*/
    static boolean ABBREV_SYNTAX=false;
    /*tells whether we should show anonymous IDs or not in the graph (they always exist, but are only displayed if true)*/
    static boolean SHOW_ANON_ID=false;
    /*tells whether we should display the URI (false) or the label (true) of a resource in the ellipse*/
    static boolean DISP_AS_LABEL=false;
    /*max number of chars displayed in the graph for literals*/ 
    static int MAX_LIT_CHAR_COUNT=40;
    /*orientation of the graph (when computed by GraphViz) - can be "LR" or "TB"*/
    static String GRAPH_ORIENTATION="LR";
    /*which version of graphviz (changes the way we parse the SVG file) 0=GraphViz 1.7.6 ; 1=GraphViz 1.7.11 or later*/
    static int GRAPHVIZ_VERSION=0;

    /*directories and files*/
    JFileChooser fc;
    /*location of the configuration file - at init time, we look for it in the user's home dir.
     If it is not there, we take the one in IsaViz dir.*/
    static File cfgFile;
    /*rdf/isv file passed as argument from the command line (if any)*/
    static String argFile;
    /*file for the current project - set by openProject(), used by saveProject()*/
    static File projectFile;
    /*temp xml-serialization of the current model used to display model as RDF/XML*/
    static String tmpRdfFile="tmp/serial.rdf";
    /*path to GraphViz/DOT executable*/
    static File m_GraphVizPath=new File("C:\\ATT\\Graphviz\\bin\\dot.exe");
    /*path to Graphviz font dir (did not seem to matter, at least under Win32 and Linux)*/
    static File m_GraphVizFontDir=new File("C:\\ATT\\Graphviz");
    /*temporary directory (temp .rdf, .dot and .svg files)*/
    static File m_TmpDir=new File("tmp");
    /*IsaViz project files (.isv)*/
    static File projectDir=new File("projects");
    static File lastOpenPrjDir=null; //remember these 2 so that next file
    static File lastSavePrjDir=null;  //dialog gets open in the same place
    /*Import/Export directory*/
    static File rdfDir=new File("export");
    static File lastImportRDFDir=null; //remember these 2 so that next file
    static File lastExportRDFDir=null; //dialog gets open in the same place
    /*delete temporary files on exit*/
    static boolean dltOnExit=true;
    /*maximum number of resources remembered (for back button) when navigating in the property browser tab (TablePanel)*/
    static int MAX_BRW_LIST_SIZE=10;
    /*maximum number of operations remembered (for Undo)*/
    static int UNDO_SIZE=5;
    /*should the window positions and sizes be saved and restored next time IsaViz is started*/
    static boolean SAVE_WINDOW_LAYOUT=false;

    /*VTM data*/
    static final String mainVirtualSpace="rdfSpace"; /*name of the main VTM virtual space*/
    static final String mainView="Graph";            /*name of the main VTM view*/
    static final String resEllipseType="resG";       //VTM glyph types associated with 
    static final String resTextType="resT";          //the entities of the graph (resources, 
    static final String propPathType="prdG";         //properties, literals). Actions fired 
    static final String propHeadType="prdH";         //in the VTM event handler (EditorEvtHdlr) 
    static final String propTextType="prdT";         //depend on these (or part of these, like {G,T,H}).
    static final String litRectType="litG";          //Modify at your own risks
    static final String litTextType="litT";

    static int ARROW_HEAD_SIZE=5; //size of the VTriangle used as head of arrows (edges)

    /*L&F data*/
    static Font smallFont=new Font("Dialog",0,10);
    static Font tinyFont=new Font("Dialog",0,9);

    /*Font used in VTM view - info also used when generating the DOT file for GraphViz*/
    static String vtmFontName="Dialog";
    static int vtmFontSize=10;
    static Font vtmFont=new Font(vtmFontName,0,vtmFontSize);

    static boolean ANTIALIASING=false;  //sets antialiasing in VTM views

    /*VTM main class*/
    static VirtualSpaceManager vsm;
    /*class that receives the events sent from VTM views (include mouse click, entering object,...)*/
    EditorEvtHdlr eeh;
    /*in charge of loading, parsing  and serializing RDF files (using Jena/ARP)*/
    RDFLoader rdfLdr;
    /*in charge of loading and parsing misc. XML files (for instance SVG and ISV project files)*/
    XMLManager xmlMngr;
    /*in charge of building and analysing ISV DOM Trees*/
    ISVManager isvMngr;
    /*configuration (user prefs) manager*/
    ConfigManager cfgMngr;
    /*methods to adjust path start/end points, text inside ellipses, etc...*/
    GeometryManager geomMngr;
    /*methods to manage contextual menus associated with nodes and edges*/
    ContMenuManager ctmnMngr;

    /*store last UNDO_SIZE commands so that they can be undone (not all operations are supported)*/
    ISVCommand[] undoStack;
    /*index of the last command in the undo stack*/
    int undoIndex;

    /*Swing panels*/
    MainCmdPanel cmp;   //main swing command panel (menus,...)
    TablePanel tblp;    //swing panel with tables for namespaces, properties, resource types...
    PropsPanel propsp;  //swing panel showing the attributes of the last selected node/edge (can be edited through this panel)

    /*External (platform-dependant) browser*/
    //a class to access a platform-specific web browser (not initialized at startup, but only on demand)
    static WebBrowser webBrowser;
    //try to automatically detect browser (do not take browser path into account)
    static boolean autoDetectBrowser=true;
    //path to the browser's exec file
    static File browserPath=new File("");
    //browser command line options
    static String browserOptions="";

    /*proxy/firewall configuration*/
    static boolean useProxy=false;
    static String proxyHost="";    //proxy hostname
    static String proxyPort="80";    //default value for the JVM proxyPort system property

    /*In memory data structures used to store the model*/

    /*a dictionary containing all resources in the model*/
    Hashtable resourcesByURI; //key is a String whose value is a resource's URI or ID (obtained by IResource.getIdent()) ; value is the corresponding IResource
    /*a dictionary containing all property instances (predicates) in the model*/
    Hashtable propertiesByURI; //key is a string representing a property URI ; value is a vector containing all IProperty whose URI is equal to key
    /*a list of all literals in the model (literals with the same value are different ILiteral objects)*/
    Vector literals; //vector of ILiteral

    /*next unique anonymous ID to be assigned to an anonymous resource - DOES NOT CONTAIN THE ANON_NODE PREFIX*/
    StringBuffer nextAnonID=new StringBuffer("0");

    /*selected graph entities*/
    Vector selectedResources=new Vector();
    Vector selectedLiterals=new Vector();
    Vector selectedPredicates=new Vector();
    static INode lastSelectedItem;   //last node/edge selected by user

    /*copied graph entities (isaviz clipboard, for cut/copy/paste)*/
    Vector copiedResources=new Vector();
    Vector copiedLiterals=new Vector();
    Vector copiedPredicates=new Vector();

    /*Jena rdf model (instantiated when importing/exporting RDF/XML or NTriples)*/
    Model rdfModel;

    /*selected row in the table of Properties (one such row needs to be selected when creating a new property instance in the graph)*/
    String selectedPropertyConstructorNS;
    String selectedPropertyConstructorLN;
    
    /*quick search variables*/
    int searchIndex=0;
    String lastSearchedString="";
    Vector matchingList=new Vector();
    INode lastMatchingEntity=null;  //remember it so that its color can be reset after the search ends

    /*error management*/
    StringBuffer errorMessages;
    boolean reportError;

    /*main constructor - called from main()*/
    public Editor(){
	SplashWindow sp=new SplashWindow(2000,"images/IsavizSplash.gif",false); //displays a splash screen
	File f=new File(System.getProperty("user.home")+"/isaviz.cfg");
	if (f.exists()){cfgFile=f;}
	else {cfgFile=new File("isaviz.cfg");}
// 	System.out.println("Loading config file from "+cfgFile);
	vsm=new VirtualSpaceManager();//VTM main class
	vsm.setMainFont(vtmFont);
// 	vsm.setDebug(true);   //COMMENT OUT IN PUBLIC RELEASES!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	cfgMngr=new ConfigManager(this);
	geomMngr=new GeometryManager(this);
	isvMngr=new ISVManager(this);
	ctmnMngr=new ContMenuManager(this);
	sp.setProgressBarValue(10);
	cfgMngr.initLookAndFeel();    //fonts, Swing colors
	sp.setProgressBarValue(20);
	cfgMngr.initWindows();                //Swing panels and VTM views (default layout)
	sp.setProgressBarValue(30);
	xmlMngr=new XMLManager(this); //must happen before initConfig(), initHistory() and any project opening/file import
	sp.setProgressBarValue(40);
	resourcesByURI=new Hashtable();
	sp.setProgressBarValue(50);
	propertiesByURI=new Hashtable();
	sp.setProgressBarValue(60);
	literals=new Vector();
	errorMessages=new StringBuffer();
	reportError=false;
	undoStack=new ISVCommand[UNDO_SIZE];
	undoIndex=-1;
	sp.setProgressBarValue(70);
	cfgMngr.assignColorsToGraph();
	sp.setProgressBarValue(80);
	cfgMngr.initConfig();
	sp.setProgressBarValue(90);
	cfgMngr.layoutWindows();
	sp.setProgressBarValue(100);
	cfgFile=new File(System.getProperty("user.home")+"/isaviz.cfg"); //the user's prefs will be saved in his home dir, no matter whether there was a cfg file there or not
	if (m_TmpDir.exists()){
	    if (argFile!=null){//load/import file passed as command line argument
		if (argFile.endsWith(".isv")){isvMngr.openProject(new File(argFile));}
		else if (argFile.endsWith(".n3")){loadRDF(new File(argFile),RDFLoader.NTRIPLE_READER);}  //1 is NTriples reader
		else {loadRDF(new File(argFile),RDFLoader.RDF_XML_READER);}  //0 is for RDF/XML reader
	    }
	    else {vsm.getGlobalView(vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),10);}
	}
	else {
	    JOptionPane.showMessageDialog(cmp,"You need to select a temporary directory for IsaViz\nin the Directories tab of the Preferences Panel, or some functions will not work properly.\nThe current directory ("+m_TmpDir+") does not exist.");
	}
    }

    /*GUI warning before reset*/
    void promptReset(){
	Object[] options={"Yes","No"};
	int option=JOptionPane.showOptionDialog(null,Messages.resetWarning,"Warning",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
	if (option==JOptionPane.OK_OPTION){
	    Editor.vsm.getView(Editor.mainView).setStatusBarText("New project");
	    this.reset();
	}
    }

    /*reset project*/
    public void reset(){
	if (rdfLdr!=null){rdfLdr.reset();}
	projectFile=null;
	propsp.reset();
	matchingList=new Vector();
	resourcesByURI.clear();
	propertiesByURI.clear();
	literals.removeAllElements();
	resetNamespaceBindings();
	resetPropertyConstructors();
	resetPropertyBrowser();
	reportError=false;
	rdfModel=null;
	nextAnonID=new StringBuffer("0");
	resetSelected();
	resetCopied();
	lastSelectedItem=null;
	Utils.resetArray(undoStack);  //undo stack 
	undoIndex=-1;
	cmp.enableUndo(false);
	geomMngr.resetLastResizer();
	vsm.destroyGlyphsInSpace(mainVirtualSpace);  //erase source document representation
	SVGReader.setPositionOffset(0,0);  /*the offset might have been changed by RDFLoader - and it might affect
					     project loaded from ISV (which uses SVGReader.createPath())*/
	vsm.repaintNow();
    }

    /*called by reset()*/
    void resetSelected(){
	selectedResources.removeAllElements();   //selected nodes/edges
	selectedLiterals.removeAllElements();
	selectedPredicates.removeAllElements();	
    }

    /*called by reset() and each time we do a new copy*/
    void resetCopied(){
	copiedResources.removeAllElements();   //reset clipboard
	copiedLiterals.removeAllElements();
	copiedPredicates.removeAllElements();
	cmp.enablePaste(false);
    }

    /*called by reset()*/
    void resetNamespaceBindings(){
	tblp.resetNamespaceTable();
	addNamespaceBinding(RDFMS_NAMESPACE_PREFIX,RDFMS_NAMESPACE_URI,new Boolean(false),true,false);
	addNamespaceBinding(RDFS_NAMESPACE_PREFIX,RDFS_NAMESPACE_URI,new Boolean(false),true,false);
    }

    /*called by reset()*/
    void resetPropertyConstructors(){
	tblp.resetPropertyTable();
	initRDFMSProperties();
	initRDFSProperties();
    }

    /*reset the tab in which is displayed the property browser*/
    void resetPropertyBrowser(){
	tblp.resetBrowser();
    }

    void openProject(){
	fc = new JFileChooser(Editor.lastOpenPrjDir!=null ? Editor.lastOpenPrjDir : Editor.projectDir);
	int returnVal= fc.showOpenDialog(cmp);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    errorMessages.append("-----Loading ISV project-----\n");
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			isvMngr.openProject(fc.getSelectedFile());
			return null; 
		    }
		};
	    worker.start();
	}
    }

    void saveProject(){
	final SwingWorker worker=new SwingWorker(){
		public Object construct(){
		    isvMngr.saveProject(Editor.projectFile);
		    return null; 
		}
	    };
	worker.start();
    }

    void saveProjectAs(){
	fc = new JFileChooser(Editor.lastSavePrjDir!=null ? Editor.lastSavePrjDir : Editor.projectDir);
	int returnVal= fc.showSaveDialog(cmp);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			isvMngr.saveProject(fc.getSelectedFile());
			return null;
		    }
		};
	    worker.start();
	}
    }

    //assign a unique anonymous ID to a node (checks for potential conflicts with existing IDs)
    //returns an anon id WITH prefix ANON_NODE
    String nextAnonymousID(){
	incAnonID();
	while (resourcesByURI.containsKey(ANON_NODE+nextAnonID)){//to prevent possible conflicts
	    incAnonID();//with anon IDs generated by Jena or loaded from an ISV file
	}
	return ANON_NODE+nextAnonID;
    }

    //called by nextAnonymousID - do not use it anywhere else
    private void incAnonID(){
	boolean done=false;
	for (int i=0;i<nextAnonID.length();i++){
	    byte b=(byte)nextAnonID.charAt(i);
	    if (b<0x7a){
		nextAnonID.setCharAt(i,(char)Utils.incByte(b));
		done=true;
		for (int j=0;j<i;j++){nextAnonID.setCharAt(j,'0');}
		break;
	    }
	}
	if (!done){
	    for (int i=0;i<nextAnonID.length();i++){nextAnonID.setCharAt(i,'0');}
	    nextAnonID.append('0');
	}
    }

    /*import local RDF file*/
    public void loadRDF(final File f,final int whichReader){
	if (m_GraphVizPath.exists()){
	    reset();
	    errorMessages.append("-----Importing RDF-----\n");
	    lastImportRDFDir=f.getParentFile();
	    if (rdfLdr==null){rdfLdr=new RDFLoader(this);}  //not initialized at launch time since we might not need it
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
			rdfLdr.load(f,whichReader);
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
			return null; 
		    }
		};
	    worker.start();
	}
	else {
	    JOptionPane.showMessageDialog(cmp,"The current location of GraphViz/dot ("+m_GraphVizPath+") is not valid.\nGo to the Directories tab in Preferences and browse\nto the location of your dot executable file (dot or dot.exe)");
	}
    }

    /*import remote RDF file*/
    public void loadRDF(final java.net.URL u,final int whichReader){
	if (m_GraphVizPath.exists()){
	    reset();
	    errorMessages.append("-----Importing RDF-----\n");
	    if (rdfLdr==null){rdfLdr=new RDFLoader(this);}  //not initialized at launch time since we might not need it
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
			rdfLdr.load(u,whichReader);
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
			return null; 
		    }
		};
	    worker.start();
	}
	else {
	    JOptionPane.showMessageDialog(cmp,"The current location of GraphViz/dot ("+m_GraphVizPath+") is not valid.\nGo to the Directories tab in Preferences and browse\nto the location of your dot executable file (dot or dot.exe)");
	}
    }

    /*merge local RDF file with current model*/
    public void mergeRDF(final File f,final int whichReader){
	if (m_GraphVizPath.exists()){
	    lastImportRDFDir=f.getParentFile();
	    errorMessages.append("-----Merging-----\n");
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
			//generate Jena model for current model
			generateJenaModel();
			//load second model only as a Jena model and merge it with first one
			try {rdfModel.add(rdfLdr.merge(f,whichReader));}
			catch (RDFException ex){errorMessages.append("Editor.mergeRDF() "+ex+"\n");reportError=true;}
			//serialize this model in a temporary file (RDF/XML)
			File tmpF=Utils.createTempFile(Editor.m_TmpDir.toString(),"mrg",".rdf");
			boolean wasAbbrevSyntax=Editor.ABBREV_SYNTAX; //serialize using
			Editor.ABBREV_SYNTAX=true;                    //abbreviated syntax
			rdfLdr.save(rdfModel,tmpF);                   //but restore user prefs after
			if (!wasAbbrevSyntax){Editor.ABBREV_SYNTAX=false;}//we are done
			//import this file
			reset();
			rdfLdr.load(tmpF,0);   //tmp file is generated as RDF/XML
			if (Editor.dltOnExit){tmpF.deleteOnExit();}
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
			return null; 
		    }
		};
	    worker.start();
	}
	else {
	    JOptionPane.showMessageDialog(cmp,"The current location of GraphViz/dot ("+m_GraphVizPath+") is not valid.\nGo to the Directories tab in Preferences and browse\nto the location of your dot executable file (dot or dot.exe)");
	}
    }

    /*merge remote RDF file with current model*/
    public void mergeRDF(final java.net.URL u,final int whichReader){
	if (m_GraphVizPath.exists()){
	    errorMessages.append("-----Merging-----\n");
	    final SwingWorker worker=new SwingWorker(){
		    public Object construct(){
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
			//generate Jena model for current model
			generateJenaModel();
			//load second model only as a Jena model and merge it with first one 
			try {rdfModel.add(rdfLdr.merge(u,whichReader));}
			catch (RDFException ex){errorMessages.append("Editor.mergeRDF() "+ex+"\n");reportError=true;}
			//serialize this model in a temporary file (RDF/XML)
			File tmpF=Utils.createTempFile(Editor.m_TmpDir.toString(),"mrg",".rdf");
			boolean wasAbbrevSyntax=Editor.ABBREV_SYNTAX; //serialize using
			Editor.ABBREV_SYNTAX=true;                    //abbreviated syntax
			rdfLdr.save(rdfModel,tmpF);                   //but restore user prefs after
			if (!wasAbbrevSyntax){Editor.ABBREV_SYNTAX=false;}//we are done
			//import this file
			reset();
			rdfLdr.load(tmpF,0);   //tmp file is generated as RDF/XML
			if (Editor.dltOnExit){tmpF.deleteOnExit();}
			vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
			return null; 
		    }
		};
	    worker.start();
	}
	else {
	    JOptionPane.showMessageDialog(cmp,"The current location of GraphViz/dot ("+m_GraphVizPath+") is not valid.\nGo to the Directories tab in Preferences and browse\nto the location of your dot executable file (dot or dot.exe)");
	}
    }

    /*parse local RDF/XML file and add all property types to tblp.prTable (property constructors)*/
    public void loadPropertyTypes(final File f){
	if (rdfLdr==null){rdfLdr=new RDFLoader(this);}  //not initialized at launch time since we might not need it
	final SwingWorker worker=new SwingWorker(){
		public Object construct(){
		    rdfLdr.loadProperties(f);
		    return null; 
		}
	    };
	worker.start();
    }

    /*generate the Jena model equivalent to our internal model prior to export*/
    void generateJenaModel(){
	if (rdfLdr==null){rdfLdr=new RDFLoader(this);}
	rdfLdr.generateJenaModel();
    }

    /*export RDF/XML file locally*/
    public void exportRDF(File f){
	vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	lastExportRDFDir=f.getParentFile();
	if (rdfLdr==null){rdfLdr=new RDFLoader(this);}
	rdfLdr.generateJenaModel(); //this actually builds the Jena model from our internal representation
	rdfLdr.save(rdfModel,f);
	vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
    }

    /*export as N-Triples locally*/
    public void exportNTriples(File f){
	vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	lastExportRDFDir=f.getParentFile();
	if (rdfLdr==null){rdfLdr=new RDFLoader(this);}
	this.generateJenaModel(); //this actually builds the Jena model from our internal representation
	rdfLdr.saveAsTriples(rdfModel,f);
	vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
    }

    /*export as PNG (bitmap image) locally (only the current view displayed by VTM, not the entire virtual space)*/
    public void exportPNG(File f){//should only be called if JVM is 1.4.0-beta or later (uses package javax.imageio)
	//comment out this method if trying to compile using a JDK 1.3.x
	boolean proceed=true;
	if (!Utils.javaVersionIs140OrLater()){
	    Object[] options={"Yes","No"};
	    int option=JOptionPane.showOptionDialog(null,Messages.pngOnlyIn140FirstPart+System.getProperty("java.vm.version")+Messages.pngOnlyIn140SecondPart,"Warning",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
	    if (option!=JOptionPane.OK_OPTION){
		proceed=false;
	    }
	}
	if (proceed){
	    vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	    lastExportRDFDir=f.getParentFile();
	    vsm.getView(mainView).setStatusBarText("Exporting to PNG "+f.toString()+" ... (This operation can take some time)");
	    ImageWriter writer=(ImageWriter)ImageIO.getImageWritersByFormatName("png").next();
	    try {
		writer.setOutput(ImageIO.createImageOutputStream(f));
		java.awt.image.BufferedImage bi=vsm.getView(mainView).getImage();
		if (bi!=null){
		    writer.write(bi);
		    vsm.getView(mainView).setStatusBarText("Exporting to PNG "+f.toString()+" ...done");
		}
		else {JOptionPane.showMessageDialog(cmp,"An error occured when retrieving the image.\n Please try again.");}
	    }
	    catch (java.io.IOException ex){JOptionPane.showMessageDialog(cmp,"Error while exporting to PNG:\n"+ex);}
	    vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
	}
    }

    /*export the entire RDF graph as SVG locally*/
    public void exportSVG(File f){
	if (f!=null){
	    vsm.getView(mainView).setCursorIcon(java.awt.Cursor.WAIT_CURSOR);
	    lastExportRDFDir=f.getParentFile();
	    vsm.getView(mainView).setStatusBarText("Exporting to SVG "+f.toString()+" ...");
	    if (f.exists()){f.delete();}
	    SVGWriter svgw=new SVGWriter();
	    Document d=svgw.exportVirtualSpace(vsm.getVirtualSpace(mainVirtualSpace),new DOMImplementationImpl(),f);
	    xmlMngr.serialize(d,f);
	    vsm.getView(mainView).setStatusBarText("Exporting to SVG "+f.toString()+" ...done");
	    vsm.getView(mainView).setCursorIcon(java.awt.Cursor.CUSTOM_CURSOR);
	}
    }

    /*enable/disable use of abbreviated syntax when exporting to RDF/XML*/
    public void setAbbrevSyntax(boolean b){
	ABBREV_SYNTAX=b;
    }

    public void displayLabels(boolean b){//finish it--------------------------------------------------
	//have to scan all resources, see if they have a rdfs:label property and assign new VText
	DISP_AS_LABEL=b;
	showResourceLabels(DISP_AS_LABEL);
    }

    /*display anonymous IDs in the graph, or just display blank ellipses*/
    public void showAnonIds(boolean b){
	IResource r;
	VirtualSpace vs=vsm.getVirtualSpace(mainVirtualSpace);
	for (Enumeration e=resourcesByURI.elements();e.hasMoreElements();){
	    r=(IResource)e.nextElement();
	    if (r.isAnon()){//for all anonymous resources
		if (b){vs.show(r.getGlyphText());}
		else {vs.hide(r.getGlyphText());}
	    }
	}
	SHOW_ANON_ID=b;
    }

    /*if true, display resource URI or rdfs:label if defined; if false, always display resource URI*/
    void showResourceLabels(boolean b){//in any case, the rdfs:label property is still shown as a statement in
	Vector v=getProperties(RDFS_NAMESPACE_URI+"label");//the graph
	if (b){//display labels
	    try {
		IProperty p;
		for (int i=0;i<v.size();i++){
		    p=(IProperty)v.elementAt(i);
		    geomMngr.adjustResourceTextAndEllipse(p.subject,p.object.getText());
		}
	    }//v might be null if there is no such property in the graph
	    catch (NullPointerException ex){}
	}
	else {//display URIs
	    try {
		IProperty p;
		for (int i=0;i<v.size();i++){
		    p=(IProperty)v.elementAt(i);
		    geomMngr.adjustResourceTextAndEllipse(p.subject,p.subject.getIdent());
		}
	    }//v might be null if there is no such property in the graph
	    catch (NullPointerException ex){}
	}
    }

    /*create a new IResource from ISV or user-entered data, and add it to the internal model*/
    IResource addResource(String namespace,String localname){
	IResource res=new IResource();
	res.setNamespace(namespace);
	res.setLocalname(localname);
	if (!resourcesByURI.containsKey(res.getIdent())){
	    resourcesByURI.put(res.getIdent(),res);
	    return res;
	}
	else {return (IResource)resourcesByURI.get(res.getIdent());}
    }

    //if ID is null, a new ID is generated for this resource
    IResource addAnonymousResource(String id){
	IResource res=new IResource();
	res.setAnon(true);
	if (id!=null){res.setAnonymousID(id);} //if not already defined
	else {res.setAnonymousID(nextAnonymousID());} //generate a new one
	if (!resourcesByURI.containsKey(res.getIdent())){
	    resourcesByURI.put(res.getIdent(),res);
	    return res;
	}
	else {return (IResource)resourcesByURI.get(res.getIdent());}
    }

    //create a new IProperty and add it to the internal model (from ISV or user-entered data)
    IProperty addProperty(String namespace,String localname){
	IProperty res=new IProperty();
	res.setNamespace(namespace);
	res.setLocalname(localname);
	if (propertiesByURI.containsKey(res.getIdent())){
	    Vector v=(Vector)propertiesByURI.get(res.getIdent());
	    v.add(res);
	}
	else {
	    Vector v=new Vector();
	    v.add(res);
	    propertiesByURI.put(res.getIdent(),v);
	}
	addPropertyType(res.getNamespace(),res.getLocalname(),true);  //add to the table of property constructors silently (a property might be used multiple times in existing graphs)
	return res;
    }

    //create a new ILiteral and add it to the internal model (from ISV or user-entered data)
    ILiteral addLiteral(String value,String lang,boolean wellFormed){
	ILiteral res=new ILiteral();
	res.setValue(value);
	if (lang!=null){res.setLanguage(lang);}
	res.setEscapeXMLChars(wellFormed);
	literals.add(res);
	return res;
    }

    //get an IResource knowing its URI
    IResource getResource(String uri){
	IResource res=(IResource)resourcesByURI.get(uri);
	return res;
    }

    //get IPropert(-ies) having this URI (null if none)
    Vector getProperties(String uri){
	Vector res=(Vector)propertiesByURI.get(uri);
	return res;
    }

    /*when the user creates a new resource from scratch in the environment*/
    void createNewResource(long x,long y){
	IResource r=new IResource();
	VEllipse g=new VEllipse(x,y,0,40,18,ConfigManager.resourceColorF);
	r.setGlyph(g);
	vsm.addGlyph(g,mainVirtualSpace);
	g.setHSVbColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);
	new NewResPanel(this,r);
    }

    /*the resource will be stored only after the user gives enough information through NewResPanel (which calls this method) ; uriORid=true if resource defined by a URI, false if defined by an ID*/
    void storeResource(IResource r,String about,boolean uriORid){
	if (about.length()==0){//considered as an anonymous resource - if URI is added later, will change its status
	    r.setAnon(true);
	    r.setAnonymousID(this.nextAnonymousID());
	}
	else {
	    if (uriORid){
		r.setURI(about);
	    }
	    else {
		r.setNamespace(DEFAULT_NAMESPACE);
		r.setLocalname(about);
	    }
	}
	VEllipse el=(VEllipse)r.getGlyph();
	VText g=new VText(el.vx,el.vy,0,ConfigManager.resourceColorTB,r.getIdent());
	vsm.addGlyph(g,mainVirtualSpace);
	r.setGlyphText(g);
	//here we use an ugly hack to compute the position of text and size of ellipse because VText.getBounds() is not yet available (computed in another thread at an unknown time) - so we access the VTM view's Graphics object to manually compute the bounds of the text. Very ugly. Shame on me. But right now there is no other way.
	Rectangle2D r2d=vsm.getView(mainView).getGraphicsContext().getFontMetrics().getStringBounds(r.getIdent(),vsm.getView(mainView).getGraphicsContext());
	
	el.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*Math.round(r2d.getWidth())/2
	//ellipse should always have width > height  (just for aesthetics)
	if (el.getWidth()<(1.5*el.getHeight())){el.setWidth(Math.round(1.5*el.getHeight()));}
	//center VText in ellipse
	g.moveTo(el.vx-(long)r2d.getWidth()/2,el.vy-(long)r2d.getHeight()/4);
	if (r.isAnon() && !SHOW_ANON_ID){vsm.getVirtualSpace(mainVirtualSpace).hide(g);}
	resourcesByURI.put(r.getIdent(),r); //we have already checked through resourceAlreadyExists that there is no conflict
    }

    //have to destroy the recently added node+glyph because user canceled is operation
    void cancelNewNode(INode n){
	//just have to remove it form virtual space ; should then be garbage-collected
	vsm.getVirtualSpace(mainVirtualSpace).destroyGlyph(n.getGlyph());
    }

    //called when editing an existing resource
    void makeAnonymous(IResource r){
	resourcesByURI.remove(r.getIdent());
	r.setAnon(true);
	r.setAnonymousID(this.nextAnonymousID());
	resourcesByURI.put(r.getIdent(),r);
	r.getGlyphText().setText(r.getIdent());
	if (!SHOW_ANON_ID){vsm.getVirtualSpace(mainVirtualSpace).hide(r.getGlyphText());}
    }

    //called when editing an existing resource
    void changeResourceURI(IResource r,String uri,boolean uriORid){
	if (uriORid){//a full URI
	    if (!uri.equals(r.getIdent())){//trying to change the URI to the same value has no effect
		if (!resourceAlreadyExists(uri)){
		    resourcesByURI.remove(r.getIdent());
		    if (r.isAnon()){
			r.setAnon(false);
			if (!SHOW_ANON_ID){vsm.getVirtualSpace(mainVirtualSpace).show(r.getGlyphText());}
		    }
		    r.setURI(uri);
		    resourcesByURI.put(r.getIdent(),r);
		}
		else {JOptionPane.showMessageDialog(propsp,"A resource with URI "+uri+" already exists");}
	    }
	}
	else {//a local ID
	    String id=uri.startsWith(DEFAULT_NAMESPACE) ? uri.substring(DEFAULT_NAMESPACE.length(),uri.length()) : uri ;
	    //2 tests below because at this point with have not yet normalized IDs with '#' (still value entered by user in text field)
	    if (!(r.getIdent().equals(DEFAULT_NAMESPACE+"#"+id) || r.getIdent().equals(DEFAULT_NAMESPACE+id))){
		if (!resourceAlreadyExists(DEFAULT_NAMESPACE+"#"+id)){
		    resourcesByURI.remove(r.getIdent());
		    if (r.isAnon()){
			r.setAnon(false);
			if (!SHOW_ANON_ID){vsm.getVirtualSpace(mainVirtualSpace).show(r.getGlyphText());}
		    }
		    r.setNamespace(DEFAULT_NAMESPACE);
		    r.setLocalname(id);
		    resourcesByURI.put(r.getIdent(),r);
		}
		else {JOptionPane.showMessageDialog(propsp,"A resource with ID "+uri+" already exists");}
	    }
	}
	geomMngr.adjustResourceTextAndEllipse(r,r.getIdent());
	VText g=r.getGlyphText();
	if (!g.isVisible()){vsm.getVirtualSpace(mainVirtualSpace).show(g);}
    }

    /*returns true if a resource with this URI already exists in the internal model*/
    boolean resourceAlreadyExists(String uri){
	if (resourcesByURI.containsKey(uri)){return true;}
	else {return false;}
    }

    /*when the user creates a new resource from scratch in the environment*/
    void createNewLiteral(long x,long y){
	ILiteral l=new ILiteral();
	VRectangle g=new VRectangle(x,y,0,40,18,ConfigManager.literalColorF);
	vsm.addGlyph(g,mainVirtualSpace);
	g.setHSVbColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);
	l.setGlyph(g);
	new NewLitPanel(this,l);
    }

    /*the resource will be stored only after the user gives enough information through NewResPanel (which calls this method)*/
    void storeLiteral(ILiteral l,String value,boolean wellFormed,String lang){
	if (lang.length()>0){l.setLanguage(lang);}
	l.setEscapeXMLChars(wellFormed);
	setLiteralValue(l,value);
	literals.add(l);
    }

    /*set the value of the ILiteral and updates its VText in the graph view*/
    void setLiteralValue(ILiteral l,String value){
	l.setValue(value);
	if (value.length()>0){
	    String truncText=((l.getValue().length()>=Editor.MAX_LIT_CHAR_COUNT) ? l.getValue().substring(0,Editor.MAX_LIT_CHAR_COUNT)+" ..." :l.getValue());
	    if (l.getGlyphText()!=null){l.getGlyphText().setText(truncText);}
	    else {//literal is empty, so it does not have an associated VText - have to create it
		VRectangle rt=(VRectangle)l.getGlyph();
		VText g=new VText(rt.vx,rt.vy,0,l.isSelected() ? ConfigManager.selectionColorTB : ConfigManager.literalColorTB,truncText);
		vsm.addGlyph(g,mainVirtualSpace);
		l.setGlyphText(g);
		//here we use an ugly hack to compute the position of text and size of rectangle because VText.getBounds() is not yet available (computed in another thread at an unknown time) - so we access the VTM view's Graphics object to manually compute the bounds of the text. Very ugly. Shame on me. But right now there is no other way.
		Rectangle2D r2d=vsm.getView(mainView).getGraphicsContext().getFontMetrics().getStringBounds(truncText,vsm.getView(mainView).getGraphicsContext());
		rt.setWidth(Math.round(0.6*r2d.getWidth()));  //0.6 comes from 1.2*Math.round(r2d.getWidth())/2
		//rectangle should always have width > height  (just for aesthetics)
		if (rt.getWidth()<(1.5*rt.getHeight())){rt.setWidth(Math.round(1.5*rt.getHeight()));}
		//center VText in rectangle
		g.moveTo(rt.vx-(long)r2d.getWidth()/2,rt.vy-(long)r2d.getHeight()/4);
	    }
	}
	else {//get rid of the VText if value is set to empty
	    if (l.getGlyphText()!=null){
		vsm.getVirtualSpace(mainVirtualSpace).destroyGlyph(l.getGlyphText());
		l.setGlyphText(null);
	    }
	}
	IProperty p;
	if ((p=l.getIncomingPredicate())!=null && p.getNamespace().equals(RDFS_NAMESPACE_URI) && p.getLocalname().equals("label")){//in case this literal is the object of an rdfs:label statement, update 
	    p.subject.setLabel(l.getValue());
	    if (DISP_AS_LABEL){geomMngr.adjustResourceTextAndEllipse(p.subject,p.subject.getLabel());}
	}
    }

    /*when the user creates a new property from scratch in the environment - points is a Vector of LongPoint*/
    void createNewProperty(IResource subject,INode object,Vector points){
	boolean error=false;
	if ((object instanceof ILiteral) && (((ILiteral)object).getIncomingPredicate()!=null)){error=true;JOptionPane.showMessageDialog(vsm.getActiveView().getFrame(),"This literal is already the object of a statement.");}
	if (!error){
	    IProperty res;
	    if (selectedPropertyConstructorNS.equals(RDFMS_NAMESPACE_URI) && selectedPropertyConstructorLN.startsWith(MEMBERSHIP_PROP_CONSTRUCTOR.substring(0,3))){//user selected the membership property auto-numbering constructor
		res=addProperty(RDFMS_NAMESPACE_URI,IContainer.nextContainerIndex(subject));
	    }
	    else {//any other property type
		res=addProperty(selectedPropertyConstructorNS,selectedPropertyConstructorLN);
	    }
	    res.setSubject(subject);
	    subject.addOutgoingPredicate(res);
	    if (object instanceof IResource){
		IResource object2=(IResource)object;
		res.setObject(object2);
		object2.addIncomingPredicate(res);
	    }
	    else {//object is an ILiteral (or we have an error)
		ILiteral object2=(ILiteral)object;
		res.setObject(object2);
		object2.setIncomingPredicate(res);
		if (res.getIdent().equals(Editor.RDFS_NAMESPACE_URI+"label")){
		    //if property is rdfs:label, set label for the resource
		    subject.setLabel(object2.getValue());
		    subject.getGlyphText().setText(subject.getLabel());
		}
	    }
	    LongPoint lp1=(LongPoint)points.elementAt(0);
	    LongPoint lp2=(LongPoint)points.elementAt(1);
	    //have to modify first and last point so that they begin at the node's boundary, not its center
	    //modification for start point
	    //compute the direction from center of glyph to point 2 on curve
	    Point2D newPoint=new Point2D.Double(lp1.x,lp1.y);
	    Point2D delta=Utils.computeStepValue(lp1,lp2);
	    //we then walk in this direction until we get out of the subject (which is always an ellipse)
	    VEllipse el1=(VEllipse)subject.getGlyph();
	    Ellipse2D el2=new Ellipse2D.Double(el1.vx-el1.getWidth(),el1.vy-el1.getHeight(),el1.getWidth()*2,el1.getHeight()*2);
	    while (el2.contains(newPoint)){
		newPoint.setLocation(newPoint.getX()+delta.getX(),newPoint.getY()+delta.getY());
	    }
	    //when we find the point on the boundary of the ellipse, still in the direction of the second point on the path, we assign its coordinates to the first point on path
	    lp1.setLocation(Math.round(newPoint.getX()),Math.round(newPoint.getY()));
	    VPath pt=new VPath(lp1.x,lp1.y,0,ConfigManager.propertyColorB);
	    //then add following points
// 	    pt.addSegment((lp1.x+lp2.x)/2,(lp1.y+lp2.y)/2,true);
// 	    for (int i=1;i<points.size()-1;i++){
// 		lp1=(LongPoint)points.elementAt(i);
// 		lp2=(LongPoint)points.elementAt(i+1);
// 		pt.addQdCurve((lp2.x+lp1.x)/2,(lp2.y+lp1.y)/2,lp1.x,lp1.y,true);
// 	    }
	    for (int i=1;i<points.size()-2;i++){//old version stared by a segment - was the source of ericP's elbows
		lp1=(LongPoint)points.elementAt(i);//now we begin directly with a Quad curve
		lp2=(LongPoint)points.elementAt(i+1);
		pt.addQdCurve((lp2.x+lp1.x)/2,(lp2.y+lp1.y)/2,lp1.x,lp1.y,true);
	    }
	    //finally, for last point, do something similar to what's been done for first point (place it on the edge of the object's shape)
	    lp2=(LongPoint)points.elementAt(points.size()-2);
	    lp1=(LongPoint)points.lastElement();
	    //modification for end point
	    //compute the direction from center of glyph to one before last point on curve
	    Point2D newPoint2=new Point2D.Double(lp1.x,lp1.y);
	    Point2D delta2=Utils.computeStepValue(lp1,lp2);
	    if (object instanceof IResource){//we have a VEllipse
		//we then walk in this direction until we get out of the object (which is an ellipse in this case)
		VEllipse el3=(VEllipse)object.getGlyph();
		Ellipse2D el4=new Ellipse2D.Double(el3.vx-el3.getWidth(),el3.vy-el3.getHeight(),el3.getWidth()*2,el3.getHeight()*2);
		while (el4.contains(newPoint2)){
		    newPoint2.setLocation(newPoint2.getX()+delta2.getX(),newPoint2.getY()+delta2.getY());
		}
		//when we find the point on the boundary of the ellipse, 
		//we assign its coordinates to the first point on path
		lp1.setLocation(Math.round(newPoint2.getX()),Math.round(newPoint2.getY()));
	    }
	    else {//instanceof ILiteral - we have a VRectangle
		//we then walk in this direction until we get out of the object (which is an ellipse in this case)
		VRectangle rl3=(VRectangle)object.getGlyph();
		Rectangle2D rl4=new Rectangle2D.Double(rl3.vx-rl3.getWidth(),rl3.vy-rl3.getHeight(),rl3.getWidth()*2,rl3.getHeight()*2);
		while (rl4.contains(newPoint2)){
		    newPoint2.setLocation(newPoint2.getX()+delta2.getX(),newPoint2.getY()+delta2.getY());
		}
		//when we find the point on the boundary of the rectangle,
		//we assign its coordinates to the first point on path
		lp1.setLocation(Math.round(newPoint2.getX()),Math.round(newPoint2.getY()));
	    }
	    //then add the last curve/segment to the path using the newly computed point
	    if (points.size()>2){//old version created a segment - now we finish by a quad curve
		pt.addQdCurve(lp1.x,lp1.y,lp2.x,lp2.y,true);
	    }
	    else {//unless the user did not specify any intermediate point, in which case the path is just made of one segment (straight)
		pt.addSegment(lp1.x,lp1.y,true);
	    }
// 	    pt.addSegment(lp1.x,lp1.y,true);
	    vsm.addGlyph(pt,mainVirtualSpace);
	    //ARROW
	    //at this point lp1 holds the coordinates of the path's end point, and lp2 the coordinates of the one point just before lp1 in the path
	    VTriangleOr tr=Utils.createPathArrowHead(lp2,lp1,null);
	    vsm.addGlyph(tr,Editor.mainVirtualSpace);	    
	    //TEXT - display namespace as prefix or URI depending on user's prefs
	    String uri=""; //this will be used later to create the IProperty's VText in the graph
	    boolean bindingDefined=false;
	    for (int i=0;i<tblp.nsTableModel.getRowCount();i++){//retrieve NS binding if defined
		if (((String)tblp.nsTableModel.getValueAt(i,1)).equals(selectedPropertyConstructorNS)){
		    if (((Boolean)tblp.nsTableModel.getValueAt(i,2)).booleanValue()){
			uri=((String)tblp.nsTableModel.getValueAt(i,0))+":"+res.getLocalname();
		    }
		    else {uri=selectedPropertyConstructorNS+res.getLocalname();}
		    bindingDefined=true;
		    break;
		}
	    }
	    if (!bindingDefined){uri=selectedPropertyConstructorNS+res.getLocalname();}
	    long posx,posy;
	    if (points.size()%2!=0){//try to position the text to the best location possible
		posx=((LongPoint)points.elementAt(points.size()/2)).x;
		posy=((LongPoint)points.elementAt(points.size()/2)).y;
	    }
	    else {
		posx=(((LongPoint)points.elementAt(points.size()/2)).x+((LongPoint)points.elementAt(points.size()/2-1)).x)/2;
		posy=(((LongPoint)points.elementAt(points.size()/2)).y+((LongPoint)points.elementAt(points.size()/2-1)).y)/2;
	    }
	    VText tx=new VText(posx,posy,0,ConfigManager.propertyColorT,uri);
	    vsm.addGlyph(tx,mainVirtualSpace);
	    res.setGlyph(pt,tr);
	    res.setGlyphText(tx);
	}
    }

    /*change the type of a statement's predicate*/
    void changePropertyURI(IProperty p,String ns,String ln){
	String uri;
	if ((uri=getNSURIfromPrefix(ns))==null){uri=ns;} //replace prefix by uri if necessary
	if (!p.getIdent().equals(uri+ln)){//if the property has really changed, we have to update propertiesByURI
	    if (p.getNamespace().equals(RDFS_NAMESPACE_URI) && p.getLocalname().equals("label")){
		//property WAS rdfs:label, but we are changing - update subject's label
		p.subject.setLabel("");
		if (Editor.DISP_AS_LABEL){geomMngr.adjustResourceTextAndEllipse(p.subject,p.subject.getIdent());}
	    }
	    if (uri.equals(RDFMS_NAMESPACE_URI) && ln.startsWith(MEMBERSHIP_PROP_CONSTRUCTOR.substring(0,3))){
		ln=IContainer.nextContainerIndex(p.subject); //replace _??.... by the first real available index
	    }
	    else {
		if (uri.equals(RDFS_NAMESPACE_URI) && ln.equals("label")){
		    //property was anything but we are changing to rdfs:label - display the statement's object value
		    p.subject.setLabel(p.object.getText());
		    if (Editor.DISP_AS_LABEL){geomMngr.adjustResourceTextAndEllipse(p.subject,p.subject.getLabel());}
		}
	    }
	    Vector v=(Vector)propertiesByURI.get(p.getIdent());
	    v.remove(p);
	    p.setNamespace(uri);
	    p.setLocalname(ln);
	    if (propertiesByURI.containsKey(p.getIdent())){
		Vector v2=(Vector)propertiesByURI.get(p.getIdent());
		v2.add(p);
	    }
	    else {
		Vector v2=new Vector();
		v2.add(p);
		propertiesByURI.put(p.getIdent(),v2);
	    }
	    if (showThisNSAsPrefix(p.getNamespace(),true)){
		updateAPropertyText(p,ns+":"+p.getLocalname());  //ns still holds the prefix at this time (but not uri)
	    }
	    else {
		updateAPropertyText(p,p.getIdent());  //here should display prefix if appropriate
	    }
	}
    }

    /*change the subject of a statement*/
    static void changePropertySubject(IProperty p,IResource newSubject){
	//first remove the property from the list of outgoing predicates in old subject
	if (p.getSubject()!=null && p.getSubject()!=newSubject){
	    IResource oldSubject=p.getSubject();oldSubject.removeOutgoingPredicate(p);
	}
	//then attach it as an outgoing predicate to the new subject
	p.setSubject(newSubject);
	newSubject.addOutgoingPredicate(p);
    }

    /*change the object of a statement*/
    static void changePropertyObject(IProperty p,INode newObject){
	//first remove the property from the list of incoming predicates in old object
	if (p.getObject()!=null && p.getObject()!=newObject){
	    INode oldObject=p.getObject();
	    if (oldObject instanceof ILiteral){((ILiteral)oldObject).setIncomingPredicate(null);}
	    else {((IResource)oldObject).removeIncomingPredicate(p);}
	}
	//then attach it as an incoming predicate to the new object
	if (newObject instanceof ILiteral){
	    p.setObject((ILiteral)newObject);
	    ((ILiteral)newObject).setIncomingPredicate(p);
	}
	else {//instanceof IResource
	    p.setObject((IResource)newObject);
	    ((IResource)newObject).addIncomingPredicate(p);
	}
    }

    /*selects all resources whose URI contains uriFragment*/
    void selectResourcesMatching(String uriFragment){
	if (uriFragment.length()>0){
	    String key;
	    for (Enumeration e=resourcesByURI.keys();e.hasMoreElements();){
		key=(String)e.nextElement();
	    if (key.indexOf(uriFragment)!=-1){selectResource((IResource)resourcesByURI.get(key),true);}
	    }
	    if (lastSelectedItem!=null){propsp.updateDisplay(lastSelectedItem);}
	}
    }

    /*selects all property instances whose namespace contains nsFragment and local name contains lnFragment
      for both parameters, empty string acts as wildcard ("*")
      nsFragment can be a FULL (no fragment) namespace prefix ending with a colon (":")
    */
    void selectPropertiesMatching(String nsFragment,String lnFragment){
	if (nsFragment.length()>0 || lnFragment.length()>0){
	    String trueNS=nsFragment;
	    if (nsFragment.endsWith(":")){
		trueNS=getNSURIfromPrefix(nsFragment.substring(0,nsFragment.length()-1));
		if (trueNS==null){trueNS="";} //getNSURIformPrefix returns null if no binding uses this prefix
	    }
	    IProperty p;
	    for (Enumeration e=propertiesByURI.elements();e.hasMoreElements();){
		for (Enumeration e2=((Vector)e.nextElement()).elements();e2.hasMoreElements();){
		    p=(IProperty)e2.nextElement();
		    if (trueNS.length()>0){
			if (lnFragment.length()>0){
			    if ((p.getNamespace().indexOf(trueNS)!=-1) && (p.getLocalname().indexOf(lnFragment)!=-1)){selectPredicate(p,true);}
			}
			else {
			    if (p.getNamespace().indexOf(trueNS)!=-1){selectPredicate(p,true);}
			}
		    }
		    else {
			if (lnFragment.length()>0){
			    if (p.getLocalname().indexOf(lnFragment)!=-1){selectPredicate(p,true);}
			}
			//else nether occurs (caught by root test)
		    }
		}
	    }
	}
	if (lastSelectedItem!=null){propsp.updateDisplay(lastSelectedItem);}
    }

    /*selects all literals whose value contains fragment*/
    void selectLiteralsMatching(String fragment){
	if (fragment.length()>0){
	    ILiteral literal;
	    for (Enumeration e=literals.elements();e.hasMoreElements();){
		if ((literal=(ILiteral)e.nextElement()).getValue().indexOf(fragment)!=-1){
		    selectLiteral(literal,true);
		}
	    }
	    if (lastSelectedItem!=null){propsp.updateDisplay(lastSelectedItem);}
	}
    }

    /*(un)select a single resource*/
    void selectResource(IResource r,boolean b){
	if (b!=r.isSelected()){
	    r.setSelected(b);
	    if (b){if (!selectedResources.contains(r)){selectedResources.add(r);lastSelectedItem=r;}}
	    else {selectedResources.remove(r);}
	}
    }

    /*select all properties associated with a resource*/
    void selectPropertiesOfResource(IResource r){
	Vector v;
	IProperty p;
	if ((v=r.getIncomingPredicates())!=null){
	    for (int i=0;i<v.size();i++){
		p=(IProperty)v.elementAt(i);
		selectPredicate(p,true);
	    }
	}
	if ((v=r.getOutgoingPredicates())!=null){
	    for (int i=0;i<v.size();i++){
		p=(IProperty)v.elementAt(i);
		selectPredicate(p,true);
	    }
	}
    }

    /*(un)select a single literal*/
    void selectLiteral(ILiteral l,boolean b){
	if (b!=l.isSelected()){
	    l.setSelected(b);
	    if (b){if (!selectedLiterals.contains(l)){selectedLiterals.add(l);lastSelectedItem=l;}}
	    else {selectedLiterals.remove(l);}
	}
    }

    /*select the property that might be associated with this literal*/
    void selectPropertiesOfLiteral(ILiteral l){
	IProperty p;
	if ((p=l.getIncomingPredicate())!=null){
	    selectPredicate(p,true);
	}
    }

    /*(un)select a single property instance*/
    void selectPredicate(IProperty p,boolean b){
	if (b!=p.isSelected()){
	    p.setSelected(b);
	    if (b){if (!selectedPredicates.contains(p)){selectedPredicates.add(p);lastSelectedItem=p;}}
	    else {selectedPredicates.remove(p);}
	}
    }

    /*select the property that might be associated with this literal*/
    void selectNodesOfProperty(IProperty p){
	selectResource(p.getSubject(),true);  //properties necessarily have a subject and object, they cannot exist on their own
	INode n=p.getObject();
	if (n instanceof IResource){selectResource((IResource)n,true);}
	else {selectLiteral((ILiteral)n,true);}//instanceof ILiteral
    }

    /*select all resources and literals*/
    void selectAllNodes(){
	for (Enumeration en=resourcesByURI.elements();en.hasMoreElements();){
	    selectResource((IResource)en.nextElement(),true);
	}
	for (Enumeration en=literals.elements();en.hasMoreElements();){
	    selectLiteral((ILiteral)en.nextElement(),true);
	}
    }

    /*select all property instances*/
    void selectAllEdges(){
	for (Enumeration en1=propertiesByURI.elements();en1.hasMoreElements();){
	    for (Enumeration en2=((Vector)en1.nextElement()).elements();en2.hasMoreElements();){
		selectPredicate((IProperty)en2.nextElement(),true);
	    }
	}
    }

    //unselect last selected item (does not matter whether it was a property instance, resource or literal)
    void unselectLastSelection(){
	propsp.reset();
	tblp.updatePropertyBrowser(null,false);
	unselectAll();
	lastSelectedItem=null;
    }

    /*unselect all nodes and edges*/
    void unselectAll(){
	propsp.reset();
	for (int i=selectedResources.size()-1;i>=0;i--){
	    selectResource((IResource)selectedResources.elementAt(i),false);
	}
	for (int i=selectedLiterals.size()-1;i>=0;i--){
	    selectLiteral((ILiteral)selectedLiterals.elementAt(i),false);
	}
	for (int i=selectedPredicates.size()-1;i>=0;i--){
	    selectPredicate((IProperty)selectedPredicates.elementAt(i),false);
	}
	if (!selectedResources.isEmpty()){System.err.println("Debug: Editor.unselectAll(): selectedResources should be empty");}
	if (!selectedLiterals.isEmpty()){System.err.println("Debug: Editor.unselectAll(): selectedLiterals should be empty ; size="+selectedLiterals.size());}
	if (!selectedPredicates.isEmpty()){System.err.println("Debug: Editor.unselectAll(): selectedPredicates should be empty");}
	lastSelectedItem=null;
    }

    //remove a resource (called by GUI)
    void deleteResource(IResource r){
	//remove all incoming and outgoing predicates
	Vector v;
	if ((v=r.getIncomingPredicates())!=null){
	    for (int i=v.size()-1;i>=0;i--){deleteProperty((IProperty)v.elementAt(i));}
	}
	if ((v=r.getOutgoingPredicates())!=null){
	    for (int i=v.size()-1;i>=0;i--){deleteProperty((IProperty)v.elementAt(i));}
	}
	//destroy glyphs
	VirtualSpace vs=vsm.getVirtualSpace(mainVirtualSpace);
	vs.destroyGlyph(r.getGlyph());
	if (r.getGlyphText()!=null){vs.destroyGlyph(r.getGlyphText());}
	//remove from resourcesByURI
	removeResource(r);
    }

    //remove a resource from internal model, and from list of selected resources if present
    void removeResource(IResource r){
	if (resourcesByURI.containsKey(r.getIdent())){
	    resourcesByURI.remove(r.getIdent());
	}
	selectResource(r,false);
    }

    //remove a literal (called by GUI)
    void deleteLiteral(ILiteral l){
	//remove incoming property if exists
	if (l.getIncomingPredicate()!=null){deleteProperty(l.getIncomingPredicate());}
	//destroy glyphs
	VirtualSpace vs=vsm.getVirtualSpace(mainVirtualSpace);
	vs.destroyGlyph(l.getGlyph());
	if (l.getGlyphText()!=null){vs.destroyGlyph(l.getGlyphText());}
	//remove from literals
	removeLiteral(l);
    }

    //remove an ILiteral from internal model, and from list of selected literals if present
    void removeLiteral(ILiteral l){
	literals.remove(l);
	selectLiteral(l,false);
    }

    //remove a property, called by GUI or by deleteResource/deleteLiteral (pending edges are not allowed)
    void deleteProperty(IProperty p){
	//remove links to subject and object
	if (p.getSubject()!=null){
	    IResource subj=p.getSubject();subj.removeOutgoingPredicate(p);
	    if (p.getNamespace().equals(RDFS_NAMESPACE_URI) && p.getLocalname().equals("label")){
		//subject's text gets back to resource URI since we are deleting the rdfs:label property
		geomMngr.adjustResourceTextAndEllipse(p.subject,p.subject.getIdent());
	    }
	}
	if (p.getObject()!=null){
	    INode obj=p.getObject();
	    if (obj instanceof IResource){((IResource)obj).removeIncomingPredicate(p);}
	    else {//instanceof ILiteral
		((ILiteral)obj).setIncomingPredicate(null);
	    }
	}
	//destroy glyphs
	VirtualSpace vs=vsm.getVirtualSpace(mainVirtualSpace);
	vs.destroyGlyph(p.getGlyph());
	vs.destroyGlyph(p.getGlyphHead());
	if (p.getGlyphText()!=null){vs.destroyGlyph(p.getGlyphText());}
	//remove from propertiesByURI
	removeProperty(p);
    }

    //remove an IProperty from internal model, remove entry for this URI if empty - erase from list of selected properties if present
    void removeProperty(IProperty p){
	if (propertiesByURI.containsKey(p.getIdent())){
	    Vector v=(Vector)propertiesByURI.get(p.getIdent());
	    v.remove(p);
	    if (v.isEmpty()){propertiesByURI.remove(p.getIdent());}
	}
	selectPredicate(p,false);
    }

    //(un)comment resource or literal - will (un)comment out properties when appropriate
    void commentNode(INode n,boolean b){
	if (b && (!n.isCommented())){n.comment(b,this);}
	else if ((!b) && (n.isCommented())){n.comment(b,this);}
    }

    //(un)comment property - does not modify resource/literal linked to it
    void commentPredicate(IProperty p,boolean b){
	if (b && (!p.isCommented())){p.comment(b,this);}
	else if ((!b) && (p.isCommented())){p.comment(b,this);}
    }

    //update the VText of an IProperty, change its position so that the center of the String does not move
    void updateAPropertyText(IProperty p,String text){
	VText g=p.getGlyphText();
	if (g!=null){//POSITIONING IS NOT WORKING PROPERLY - bounds HAVE NOT YET BEEN VALIDATED WHEN CALLING G.GETBOUNDS THE SECOND TIME - SHOULD CREATE A THREAD THAT WAITS TIL IT IS VALID AND ASSIGNS VALUE
	    int index=vsm.getActiveCamera().getIndex();
	    LongPoint pt=g.getBounds(index);
	    long oldX=g.vx+pt.x/2;
	    long oldY=g.vy+pt.y/2;
	    g.setText(text);
	    pt=g.getBounds(index);
	    g.moveTo(oldX-pt.x/2,oldY-pt.y/2);
	}
    }

    //this method is called both when the user adds manually a binding and when we load/import ISV/RDF. In the second case we do not want dialogs to appear if bindings like rdfms have already been defined, so we set silent to true in that case. When loading an ISV file, bindings from this file override any binding already present (like rdf and rdfs).
    boolean addNamespaceBinding(String prefix,String uri,Boolean display,boolean silent,boolean override){
	boolean prefAlreadyInUse=false;
	boolean uriAlreadyInUse=false;
	for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
	    if (prefix.length()>0 && prefix.equals((String)tblp.nsTableModel.getValueAt(i,0))){//prefix can be "" if no binding is assigned
		prefAlreadyInUse=true;
		if (!silent){JOptionPane.showMessageDialog(tblp,"Prefix "+prefix+" is already assigned to namespace "+(String)tblp.nsTableModel.getValueAt(i,1));}
		if (!override){return false;} //don't return if overrid (occurs in the next test)
	    }
	    if (uri.equals((String)tblp.nsTableModel.getValueAt(i,1))){
		uriAlreadyInUse=true;
		if (!silent){JOptionPane.showMessageDialog(tblp,"Namespace URI "+uri+" is already binded to prefix "+(String)tblp.nsTableModel.getValueAt(i,0));}
		if (!override){return false;}
		else {//conflict detected - override existing prefix and display_as_prefix in the table with new param values
		    tblp.nsTableModel.setValueAt(prefix,i,0);
		    tblp.nsTableModel.setValueAt(display,i,2);
		    return true;
		}
	    }
	}
	if (prefix.equals(ANON_NODE.substring(0,ANON_NODE.length()-1)) || prefix.equals(DEFAULT_NAMESPACE.substring(0,DEFAULT_NAMESPACE.length()-1))){prefAlreadyInUse=true;if (!silent){JOptionPane.showMessageDialog(tblp,"Prefix '"+prefix+"' is either used as the anonymous node prefix or the base prefix");}}
	if (!(prefAlreadyInUse || uriAlreadyInUse)){//no conflict
	    //the update of namespaceBindings will be handled by updateNamespaceBinding
	    //since addRow fires a tableChanged event
	    Vector v=new Vector();v.add(prefix);v.add(uri);v.add(display);
	    String aURI;
	    int i;
	    for (i=0;i<tblp.nsTableModel.getRowCount();i++){//find where to insert the new binding in the table 
		aURI=(String)tblp.nsTableModel.getValueAt(i,1);//(sorted lexicographically)
		if (aURI.compareTo(uri)>0){break;}
	    }
	    tblp.nsTableModel.insertRow(i,v);
	    updatePropertyTabPrefix(uri,prefix);
	    return true;
	}
	else return false;
    }

    //remove namespace definition @ row n ONLY REMOVES the binding - keeping it with a prefix="" would be the same from the internal model point of view
    void removeNamespaceBinding(int n){
	tblp.nsTableModel.removeRow(n);
	String ns=(String)tblp.nsTableModel.getValueAt(n,1);
	String key;//display properties using URI instead of prefix since the binding is being deleted
	for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
	    key=(String)e.nextElement();
	    if (key.startsWith(ns)){
		for (Enumeration e2=((Vector)propertiesByURI.get(key)).elements();e2.hasMoreElements();){
		    IProperty p=(IProperty)e2.nextElement();
		    updateAPropertyText(p,p.getNamespace()+p.getLocalname());
		}
	    }
	}
	updatePropertyTabPrefix(ns,"");
    }

    /*update the prefix or display status of a given namespace binding*/
    /*the URI of an NS binding cannot be edited in the table - users have to remove it
      from the list and add a new one
      addORupd tells whether this is a new entry or the update of an existing one
    */
    void updateNamespaceBinding(int nb,int whatCell,String prefix,String uri,Boolean display,int addORupd){
	if (whatCell==2){//if the display_as_prefix cell has changed w.r.t what is stored, update the graph
	    if (display.booleanValue() && prefix.length()>0){//show namespace as prefix
		IProperty p;
		String key;
		for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
		    key=(String)e.nextElement();
		    if (key.startsWith(uri)){
			for (Enumeration e2=((Vector)propertiesByURI.get(key)).elements();e2.hasMoreElements();){
			    p=(IProperty)e2.nextElement();
			    updateAPropertyText(p,prefix+":"+p.getLocalname());
			}
		    }
		}
	    }
	    else {//show namespace as URI
		String key;
		IProperty p;
		for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
		    key=(String)e.nextElement();
		    if (key.startsWith(uri)){
			for (Enumeration e2=((Vector)propertiesByURI.get(key)).elements();e2.hasMoreElements();){
			    p=(IProperty)e2.nextElement();
			    updateAPropertyText(p,p.getIdent());
			}
		    }
		}
	    }
	}
	else if (whatCell==0){//the prefix field has been edited
	    if (prefix.length()>0){//assign a new prefix for this namespace
		if (display.booleanValue()){//if displaying as prefix and if prefix is not null, update the graph
		    IProperty p;      
		    String key;
		    for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
			key=(String)e.nextElement();
			if (key.startsWith(uri)){
			    for (Enumeration e2=((Vector)propertiesByURI.get(key)).elements();e2.hasMoreElements();){
				p=(IProperty)e2.nextElement();
				updateAPropertyText(p,prefix+":"+p.getLocalname());
			    }
			}
		    }
		}
		updatePropertyTabPrefix(uri,prefix);
	    }
	    else {// if prefix has become null, revert back to namespace URI
		String key;
		IProperty p;
		for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
		    key=(String)e.nextElement();
		    if (key.startsWith(uri)){
			for (Enumeration e2=((Vector)propertiesByURI.get(key)).elements();e2.hasMoreElements();){
			    p=(IProperty)e2.nextElement();
			    updateAPropertyText(p,p.getIdent());
			}
		    }
		}
		updatePropertyTabPrefix(uri,prefix);
	    }
	}
	//whatCell can also be equal to -1 when the entire row has changed - e.g. when adding a new NS - do not need to deal with it here
	//what cell can also be equal to 1 when modifying the namespace URI - this should be prevented by the fact that column 1 is not editable in NSTableModel
    }


    //providingURI=true if s is the URI, false if it is the prefix
    //returns false if no binding is defined
    boolean showThisNSAsPrefix(String s,boolean providingURI){
	for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
	    if ((((String)tblp.nsTableModel.getValueAt(i,1)).equals(s) && (providingURI)) || (((String)tblp.nsTableModel.getValueAt(i,0)).equals(s) && (!providingURI))){
		return ((Boolean)tblp.nsTableModel.getValueAt(i,2)).booleanValue();
	    }
	}
	return false;
    }

    //returns the prefix binded to this uri if defined (null otherwise)
    String getNSBinding(String uri){
	for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
	    if (((String)tblp.nsTableModel.getValueAt(i,1)).equals(uri) && ((String)tblp.nsTableModel.getValueAt(i,0)).length()>0){
		return (String)tblp.nsTableModel.getValueAt(i,0);
	    }
	}
	return null;
    }

    //returns the URI binded to this prefix if defined (null otherwise)
    String getNSURIfromPrefix(String prefix){
	for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
	    if (((String)tblp.nsTableModel.getValueAt(i,0)).equals(prefix) && ((String)tblp.nsTableModel.getValueAt(i,1)).length()>0){
		return (String)tblp.nsTableModel.getValueAt(i,1);
	    }
	}
	return null;
    }

    //returns true if pr is already binded to a namespace
    boolean prefixAlreadyInUse(String pr){
	if (pr.length()>0){
	    for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
		if (((String)tblp.nsTableModel.getValueAt(i,0)).equals(pr)){return true;}
	    }
	    if (pr.equals(ANON_NODE.substring(0,ANON_NODE.length()-1)) || pr.equals(DEFAULT_NAMESPACE.substring(0,DEFAULT_NAMESPACE.length()-1))){return true;}
	    return false;
	}
	else return false;
    }

    /*add a new property type constructor to the table, that can be selected to add a new property instance 
      of this type to the graph
    */
    boolean addPropertyType(String ns,String ln,boolean silent){
	boolean propertyAlreadyExists=false;
	String namespace="";
	if (ns.length()>0 && ln.length()>0){//only add complete properties
	    if (ns.charAt(ns.length()-1)==':'){//if cell ends with ':', the user has entered 
		//the prefix instead of the full URI, check that it is an existing prefix, 
		//get its URI, and then add it to the list
		String prefix=ns.substring(0,ns.length()-1);
		for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
		    if (((String)tblp.nsTableModel.getValueAt(i,0)).equals(prefix)){
			namespace=(String)tblp.nsTableModel.getValueAt(i,1);
			break;
		    }
		}
		if (namespace.length()==0){JOptionPane.showMessageDialog(tblp,"Prefix "+ns+" is not binded to any namespace.");return false;}
	    }
	    else {
		namespace=ns;
	    }
	    DefaultTableModel tm=(DefaultTableModel)tblp.prTable.getModel();
	    for (int i=0;i<tm.getRowCount();i++){//check that this property is not already defined
		if (((String)tm.getValueAt(i,2)).equals(ln) && ((String)tm.getValueAt(i,0)).equals(namespace)){
		    propertyAlreadyExists=true;
		    if (!silent){JOptionPane.showMessageDialog(tblp,"Property "+namespace+ln+" is already defined.");}
		    return false;
		}
	    }
	    if (!propertyAlreadyExists){
		String prefix=getNSBinding(namespace);
		Vector data=new Vector();data.add(namespace);data.add(prefix==null?"":prefix);data.add(ln);//null as 2nd element
		String aProperty;              //is for the prefix column
		int i;
		if (ln.charAt(0)=='_' && Character.isDigit(ln.charAt(1))){//adding a membership property type to the table
		    //(e.g. _1, _2 ,...). Adding it will automatically select it. We do not want that in this specific case
		    //Furthermore, the special properties should be inserted at the end of the table (not very interesting)
		    //but the way isaviz works for now, they need to be added to the table since the table is the main 
		    //and only in-memory internal representation of available property types
		    //We also do that for another reason: so that _X do not appear first in combo boxes of propspanel (when changing the property type of an iproperty). First because they are not very interesting, second because this had a nasty side effect: it automatically assigned _1 to the iproperty, which altered the automatic numbering if then selected as the type of this property (since the property was _1, the generator thought that the next one was _2 whereas there was actually no real _1 for this resource)
		    int lastMembershipPropIndex=tm.getRowCount();
		    String aLN;
		    for (int j=tm.getRowCount()-1;j>0;j--){//find the first table row containing a membership property type
			aLN=(String)tm.getValueAt(j,2);
			if (!(((String)tm.getValueAt(j,0)).equals(RDFMS_NAMESPACE_URI) && aLN.charAt(0)=='_' && Character.isDigit(aLN.charAt(1)))){lastMembershipPropIndex=j+1;break;}
		    }
		    for (i=lastMembershipPropIndex;i<tm.getRowCount();i++){//find where to insert the new entry in the table, beginning sorting at the first row containing a membership property (end of table if none)
			aProperty=((String)tm.getValueAt(i,0)).concat((String)tm.getValueAt(i,2));
			if (aProperty.compareTo(namespace+ln)>0){break;}//(sorted lexicographically)
		    }
		    tblp.prTable.clearSelection();
		    selectedPropertyConstructorNS=RDFMS_NAMESPACE_URI;
		    selectedPropertyConstructorLN=MEMBERSHIP_PROP_CONSTRUCTOR;
		}
		else {
		    for (i=0;i<tm.getRowCount();i++){//find where to insert the new entry in the table 
			aProperty=((String)tm.getValueAt(i,0)).concat((String)tm.getValueAt(i,2));
			if (aProperty.compareTo(namespace+ln)>0){break;}//(sorted lexicographically)
		    }
		}
		tm.insertRow(i,data);
		return true;
	    }
	    else return false;
	}
	else return false;
    }

    //remove property constructor @ row n ONLY REMOVES the constructor from the table - does not delete any predicate pr definition in propertiesByURI
    void removePropertyConstructor(int n){
	boolean used=false;  //if the property type ised used at least once in the model,
	DefaultTableModel tm=(DefaultTableModel)tblp.prTable.getModel();
	String uri=(String)tm.getValueAt(n,0)+(String)tm.getValueAt(n,2);
	for (Enumeration e=propertiesByURI.keys();e.hasMoreElements();){
	    if (((String)e.nextElement()).equals(uri)){used=true;break;}
	}
	if (used){//prompt a warning dialog before removing it
	    Object[] options={"Yes","No"};
	    int option=JOptionPane.showOptionDialog(null,Messages.removePropType,"Warning",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
	    if (option==JOptionPane.OK_OPTION){
		tm.removeRow(n);
	    }
	}
	else {tm.removeRow(n);}
    }

    //called when something changes in the NS binding table - updates the property type table's prefix column
    void updatePropertyTabPrefix(String ns,String prefix){
	DefaultTableModel tm=(DefaultTableModel)tblp.prTable.getModel();
	for (int i=0;i<tm.getRowCount();i++){
	    if (((String)tm.getValueAt(i,0)).equals(ns)){tm.setValueAt(prefix,i,1);}
	}
    }

    //add the properties defined in RDF Model and Syntax Spec to the table of property constructors (they might be used often, so we offer them by default)
    void initRDFMSProperties(){
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"li",true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,MEMBERSHIP_PROP_CONSTRUCTOR,true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"object",true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"predicate",true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"subject",true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"type",true);
	addPropertyType(Editor.RDFMS_NAMESPACE_URI,"value",true);
    }

    //add the properties defined in RDF Schema Spec to the table of property constructors (they might be used often, so we offer them by default)
    void initRDFSProperties(){
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"comment",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"domain",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"isDefinedBy",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"label",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"range",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"seeAlso",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"subClassOf",true);
	addPropertyType(Editor.RDFS_NAMESPACE_URI,"subPropertyOf",true);
    }

    //holds the type of property to create when the user creates a new predicate
    void setSelectedPropertyConstructor(String namespace,String localname){
	selectedPropertyConstructorNS=namespace;
	selectedPropertyConstructorLN=localname;
    }

    //returns a list of strings representing all namespaces used in properties (one copy for each)
    Vector getAllPropertyNS(){// (used by the combo boxes in PropsPanel)
	Vector res=new Vector();
	javax.swing.table.TableModel tm=tblp.prTable.getModel();
	String ns;
	String prefix;
	for (int i=0;i<tm.getRowCount();i++){
	    ns=(String)tm.getValueAt(i,0);
	    if ((prefix=getNSBinding(ns))!=null){ns=prefix;} //replace namespace URI by prefix if defined
	    if (!res.contains(ns)){res.add(ns);}
	}
	return res;
    }

    //returns all property names defined in a given namespace (used by the combo boxes in PropsPanel)
    Vector getProperties4NS(String ns){
	Vector res=new Vector();
	javax.swing.table.TableModel tm=tblp.prTable.getModel();
	String prefix;
	for (int i=0;i<tm.getRowCount();i++){
	    if (((String)tm.getValueAt(i,0)).equals(ns) || (((prefix=getNSBinding((String)tm.getValueAt(i,0)))!=null) &&  prefix.equals(ns))){//ns might be the namespace's uri or the prefix binded to this namespace (depending on whether a binding has been defined or not)
		res.add(tm.getValueAt(i,2));
	    }
	}
	return res;
    }

    /**set prefix used in anonymous node (default is "genid:")*/
    public void setAnonymousNodePrefix(String s){ANON_NODE=s;}

    /**get prefix used in anonymous node (default is "genid:")*/
    public String getAnonymousNodePrefix(){return ANON_NODE;}

    /**set default namespace (default is "online:")*/
    public void setDefaultNamespace(String s){DEFAULT_NAMESPACE=s;}

    /**get default namespace (default is "online:")*/
    public String getDefaultNamespace(){return DEFAULT_NAMESPACE;}

    /*show/hide the window containing the NS binding and Property types tables*/
    void showTablePanel(boolean b){
	ConfigManager.showNSWindow=b;
	if (ConfigManager.showNSWindow){tblp.show();}
	else {tblp.hide();}
    }

    /*show/hide the window displaying a node/edge attributes (which can be edited)*/
    void showPropsPanel(boolean b){
	ConfigManager.showEditWindow=b;
	if (ConfigManager.showEditWindow){propsp.show();}
	else {propsp.hide();}
    }

    /*set the maximum number of chars of a literal's value displayed (updates the graph with the new value)*/
    void setMaxLiteralCharCount(int max){
	if (max!=MAX_LIT_CHAR_COUNT){
	    MAX_LIT_CHAR_COUNT=max;
	    ILiteral l;
	    String value;
	    for (int i=0;i<literals.size();i++){
		l=(ILiteral)literals.elementAt(i);
		String displayedValue=((l.getValue().length()>=MAX_LIT_CHAR_COUNT) ? l.getValue().substring(0,MAX_LIT_CHAR_COUNT)+" ..." : l.getValue());
		l.getGlyphText().setText(displayedValue);
	    }
	}
    }

    /*changing tool in icon palette*/
    void setMode(int i){
	if (i!=EditorEvtHdlr.MOVE_RESIZE_MODE){geomMngr.destroyLastResizer();}  //get rid of resizer object when changing mode
	eeh.mode=i;
    }

    void updatePropertyBrowser(INode n){
	if (ConfigManager.showNSWindow && tblp.tabbedPane.getSelectedIndex()==2){//only update if visible (increases performances)
	    tblp.updatePropertyBrowser(n,true);
	}
    }

    //given a string, centers on a VText with this string in it
    void quickSearch(String s){//if firstTime=true, the list of VText is reinitialized ; if false, go to the next one in the list
	if (s.length()>0){
	    if (!s.toLowerCase().equals(lastSearchedString)){//searching a new string - reinitialize everything
		lastSearchedString=s.toLowerCase();
		searchIndex=-1;
		matchingList.removeAllElements();
		Vector v=vsm.getVirtualSpace(mainVirtualSpace).getVisibleGlyphs();
		for (int i=0;i<v.size();i++){
		    if ((((Glyph)v.elementAt(i)).getText()!=null) && ((((Glyph)v.elementAt(i)).getText()).toLowerCase().indexOf(lastSearchedString)!=-1)){matchingList.add(v.elementAt(i));}
		}
	    }
	    if (matchingList.size()>0){
		if (searchIndex<matchingList.size()-1){//get next entry in the list of matching elements
		    searchIndex++;
		}
		else {//go back to first one if necessary (loop)
		    vsm.getActiveView().setStatusBarText("Reached end of list, going back to the beginning");
		    searchIndex=0;
		}
		//center on the entity
		Glyph g=(Glyph)matchingList.elementAt(searchIndex);
		if (lastMatchingEntity!=null){resetINodeColors(lastMatchingEntity);}
		lastMatchingEntity=(INode)g.getOwner();
		g.setHSVColor(ConfigManager.srhTh,ConfigManager.srhTs,ConfigManager.srhTv);
		vsm.centerOnGlyph(g,vsm.getVirtualSpace(mainVirtualSpace).getCamera(0),400);
	    }
	}
    }

    //reset the search variables after it is finished
    void resetQuickSearch(){
	searchIndex=-1;
	lastSearchedString="";
	matchingList.removeAllElements();
	if (lastMatchingEntity!=null){
	    resetINodeColors(lastMatchingEntity);
	    lastMatchingEntity=null;
	}
    }

    //reset the colors of an INode, taking into accout its state (selected, commented, normal)
    void resetINodeColors(INode n){
	if (lastMatchingEntity.isSelected()){
	    lastMatchingEntity.getGlyphText().setHSVColor(ConfigManager.selTh,ConfigManager.selTs,ConfigManager.selTv);
	}
	else if (lastMatchingEntity.isCommented()){
	    lastMatchingEntity.getGlyphText().setHSVColor(ConfigManager.comTh,ConfigManager.comTs,ConfigManager.comTv);
	}
	else {
	    if (lastMatchingEntity instanceof IResource){
		lastMatchingEntity.getGlyphText().setHSVColor(ConfigManager.resTBh,ConfigManager.resTBs,ConfigManager.resTBv);
	    }
	    else if (lastMatchingEntity instanceof IProperty){
		    lastMatchingEntity.getGlyphText().setHSVColor(ConfigManager.prpTh,ConfigManager.prpTs,ConfigManager.prpTv);
	    }
	    else {//necessarily instanceof ILiteral
		lastMatchingEntity.getGlyphText().setHSVColor(ConfigManager.litTBh,ConfigManager.litTBs,ConfigManager.litTBv);
	    }
	}
    }

    /*cut selected node(s)*/
    void cutSelection(){
	if (lastSelectedItem!=null){
	    propsp.reset();
	    resetCopied(); //clean clipboard
	    ISVCut cmd=new ISVCut(this,selectedPredicates,selectedResources,selectedLiterals);
	    cmd._do();
	    addCmdToUndoStack(cmd);
	    cmp.enablePaste(true);
	}
    }

    /*copy selected node(s)*/
    void copySelection(){
	if (lastSelectedItem!=null){
	    resetCopied(); //clean clipboard
	    ISVCopy cmd=new ISVCopy(this,selectedPredicates,selectedResources,selectedLiterals);
	    cmd._do();
// 	    addCmdToUndoStack(cmd); //no undo for copy, so it is not added to the stack
	    cmp.enablePaste(true);
	}
    }

    /*paste selected node(s)*/
    void pasteSelection(long x,long y){//x,y are the coords (of set's geom center) where the copies should be created
	ISVPaste cmd=new ISVPaste(this,copiedPredicates,copiedResources,copiedLiterals,x,y);
	cmd._do();
	addCmdToUndoStack(cmd);
    }

    //delete everything that is selected, beginning by edges and then nodes
    //this is the only gate to deletion from the GUI - (so that UNDO works correctly)
    void deleteSelectedEntities(){
	if (lastSelectedItem!=null){
	    propsp.reset();
	    /*also select predicates that would anyway get deleted
	      by the deletion of their subject/object so that they 
	      get are properly restored if the operation is undone*/
	    Vector v;
	    IProperty p;
	    Enumeration e;
	    for (int i=0;i<selectedResources.size();i++){
		if ((v=((IResource)selectedResources.elementAt(i)).getIncomingPredicates())!=null){
		    for (e=v.elements();e.hasMoreElements();){
			p=(IProperty)e.nextElement();
			if (!p.isSelected()){selectPredicate(p,true);}
		    }
		}
		if ((v=((IResource)selectedResources.elementAt(i)).getOutgoingPredicates())!=null){
		    for (e=v.elements();e.hasMoreElements();){
			p=(IProperty)e.nextElement();
			if (!p.isSelected()){selectPredicate(p,true);}
		    }
		}
	    }
	    for (int i=0;i<selectedLiterals.size();i++){
		if ((p=((ILiteral)selectedLiterals.elementAt(i)).getIncomingPredicate())!=null && (!p.isSelected())){
		    selectPredicate(p,true);
		}
	    }
	    ISVDelete cmd=new ISVDelete(this,selectedPredicates,selectedResources,selectedLiterals);
	    cmd._do();
	    addCmdToUndoStack(cmd);
	}
    }

    /*undo last operation and update stack*/
    void undo(){
	if (undoIndex>=0){
	    undoStack[undoIndex]._undo();
	    undoStack[undoIndex]=null;
	    undoIndex--;
	    if (undoIndex<0){undoIndex=-1;cmp.enableUndo(false);}
	}
    }

    /*add an undoable command to undo stack*/
    void addCmdToUndoStack(ISVCommand c){
	int index=Utils.getFirstEmptyIndex(undoStack);
	if (index==-1){
	    Utils.eraseFirstAddNewElem(undoStack,c);
	    undoIndex=undoStack.length-1;
	}
	else {
	    undoStack[index]=c;
	    undoIndex=index;
	}
	cmp.enableUndo(true);
    }


    /*serializes the model to a stringbuffer and displays it in TextViewer*/
    void displayRawRDFXMLFile(){
	try {
	    if (rdfLdr==null){rdfLdr=new RDFLoader(this);}
	    rdfLdr.generateJenaModel(); //this actually builds the Jena model from our internal representation
	    StringBuffer sb=rdfLdr.serialize(rdfModel);
	    TextViewer v=new TextViewer(sb,"Raw RDF/XML Viewer",0);
	}
	catch (Exception ex){System.err.println("Error: Editor.displayRawFile: "+ex);}
    }

    /*call graphviz to relayout current model*/
    /*right now this is a very dumb version that goes through the whole process of export/import.
      In the future, I will write a version that bypasses the RDF export/import phase. It will 
      generate directly the DOT file with unique IDs, call graphviz and get the SVG, linking
      it back to existing entities (don;t go through the whole process of generating the model bla bla bla...)
    */
    /*since we are performing a reset, this also means that UNDO/PASTE from earlier operations are not available any longer - this will be fixed when we write the above mentioned code*/
    void reLayoutGraph(){
	Object[] options={"Yes","No"};
	int option=JOptionPane.showOptionDialog(null,Messages.reLayoutWarning,"Warning",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
	if (option==JOptionPane.OK_OPTION){
	    File tmpRdf=Utils.createTempFile(m_TmpDir.toString(),"tmp",".rdf");
	    tmpRdf.deleteOnExit();
	    exportRDF(tmpRdf);
	    reset();
	    loadRDF(tmpRdf,RDFLoader.RDF_XML_READER);
	}
    }

    /*open a window and display all properties for which r is the subject*/
    void displayResOutgoingPredicates(IResource r){
	new PropertySummary(r,this);
    }

    /*opens a window and displays error messages*/
    void showErrorMessages(){
	new TextViewer(errorMessages,"Error log",1000);
	vsm.getView(mainView).setStatusBarText("");
    }

    /*opens a window and dislays information about the project (file name, number of resources, etc)*/
    void showPrjSummary(){
	int nbProps=0;
	for (Enumeration e1=propertiesByURI.elements();e1.hasMoreElements();){
	    for (Enumeration e2=((Vector)e1.nextElement()).elements();e2.hasMoreElements();){
		e2.nextElement();
		nbProps++;
	    }
	}
	String prjF=projectFile==null ? "" : projectFile.toString();
	JOptionPane.showMessageDialog(cmp,
						  "Project File: "+prjF+"\n"+
						  "Number of resources: "+resourcesByURI.size()+"\n"+
						  "Number of literals: "+literals.size()+"\n"+
						  "Number of statements: "+nbProps);
    }

    //open up the default or user-specified browser (netscape, ie,...) and try to display the content at the resource's URI
    void displayURLinBrowser(IResource r){
	if (!r.isAnon()){
	    if (webBrowser==null){webBrowser=new WebBrowser(this);}
	    webBrowser.show(r.getIdent());
	}
	else vsm.getActiveView().setStatusBarText("Anonymous resources do not have a URI");
    }

    /*tells whether all views should be repainted, even if not active*/
    void alwaysUpdateViews(boolean b){
	vsm.setRepaintPolicy(b);
    }

    /*antialias ON/OFF for views*/
    void setAntialiasing(boolean b){
	ANTIALIASING=b;
	vsm.getView(mainView).setAntialiasing(ANTIALIASING);
    }

    /*save user preferences*/
    void saveConfig(){cfgMngr.saveConfig();}

    /*exit from IsaViz, save bookmarks*/
    public void exit(){
	cfgMngr.saveURLs();
	System.exit(0);
    }

    //debug
    void summary(){ 
	System.out.println("Resources "+resourcesByURI.size());
	System.out.println("Literals "+literals.size());
	int i=0;
	for (Enumeration e1=propertiesByURI.elements();e1.hasMoreElements();){
	    for (Enumeration e2=((Vector)e1.nextElement()).elements();e2.hasMoreElements();){
		e2.nextElement();
		i++;
	    }
	}
	System.out.println("Properties "+i);
    }

    //debug
    void nsBindings(){
	System.out.println("Namespace bindings");
	for (int i=0;i<tblp.nsTableModel.getRowCount();i++){
	    System.out.println("p="+tblp.nsTableModel.getValueAt(i,0)+" uri="+tblp.nsTableModel.getValueAt(i,1)+" disp="+tblp.nsTableModel.getValueAt(i,2));
	}
    }

    //debug
    void printClipboard(){
	System.out.println("Clipboard");
	System.out.print("[");
	if (!copiedResources.isEmpty()){
	    for (int i=0;i<copiedResources.size()-1;i++){System.out.print(copiedResources.elementAt(i).toString()+",");}
	    System.out.println(copiedResources.lastElement().toString()+"]");
	}
	if (!copiedLiterals.isEmpty()){
	    System.out.print("[");
	    for (int i=0;i<copiedLiterals.size()-1;i++){System.out.print(copiedLiterals.elementAt(i).toString()+",");}
	    System.out.println(copiedLiterals.lastElement().toString()+"]");
	}
	if (!copiedPredicates.isEmpty()){
	    System.out.print("[");
	    for (int i=0;i<copiedPredicates.size()-1;i++){System.out.print(copiedPredicates.elementAt(i).toString()+",");}
	    System.out.println(copiedPredicates.lastElement().toString()+"]");
	}
    }

    public static void commandLineHelp(){
	System.out.println("Usage : ");
	System.out.println("  java org.w3c.IsaView [options] [file_name.isv|file_name.rdf]");
	System.out.println("  Options:");
	System.out.println("          -I  launch IsaViz in Internal Frames mode (your operating system will only manage one IsaViz window)");
	System.exit(0);	
    }

    //MAIN
    public static void main(String[] args){
	if (args.length>2) {
	    commandLineHelp();
	}
	else if (args.length==2){
	    argFile=args[2];
	    if (args[1].equals("-I")){ConfigManager.internalFrames=true;}
	    else {commandLineHelp();}
	}
	else if (args.length==1){
	    argFile=args[0];
	}
	Editor appli=new Editor();
    }

}

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/15/2001
 */

package org.w3c.IsaViz;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;

import com.xerox.VTM.engine.View;
import com.xerox.VTM.engine.VirtualSpace;
import com.xerox.VTM.glyphs.Glyph;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.DOMImplementationImpl;
import org.w3c.dom.*;

/*methods related to user preferences (load and save) + bookmarks + window layout + some look and feel management*/


class ConfigManager {

    static java.awt.Color pastelBlue=new java.awt.Color(156,154,206);
    static java.awt.Color darkerPastelBlue=new java.awt.Color(125,123,165);

    /*color preferences*/
    static Color resourceColorF=new Color(115,191,115);   //fill color of resources
    static Color resourceColorTB=new Color(66,105,66);    //text and border color of resources
    static float resFh,resFs,resFv,resTBh,resTBs,resTBv;         //HSV coords
    static Color propertyColorB=new Color(90,89,206);     //border color of predicates
    static Color propertyColorT=new Color(90,89,206);     //text color of predicates
    static float prpBh,prpBs,prpBv,prpTh,prpTs,prpTv;            //HSV coords
    static Color literalColorF=new Color(255,223,123);    //fill color of literals
    static Color literalColorTB=new Color(132,117,66);    //text and border color of literals
    static float litFh,litFs,litFv,litTBh,litTBs,litTBv;         //HSV coords
    static Color selectionColorF=new Color(255,150,150);  //fill color of selected objects
    static float selFh,selFs,selFv,selTh,selTs,selTv;            //HSV coords
    static Color selectionColorTB=new Color(255,0,0);     //text and border color of selected objects
    static Color commentColorF=new Color(231,231,231);    //fill color of commented objects
    static float comFh,comFs,comFv,comTh,comTs,comTv;            //HSV coords
    static Color commentColorTB=new Color(180,180,180);   //text and border color of commented objects
    static Color mouseInsideColor=Color.white;            //when mouse enter an object
    static Color bckgColor=new Color(231,231,231);        //background color
    static Color cursorColor=Color.black;                 //mouse cursor color
    static Color searchColor=new Color(255,0,0);          //text color of objects matching quick search string
    static float srhTh,srhTs,srhTv;                              //HSV coords
    static String COLOR_SCHEME="default";                 //can be "default" or "b&w"

    /*if true, use JInternalFrames to display IsaViz windows - not implemented yet*/
    static boolean internalFrames=false;
    /*data about the GUI (environment)*/
    /*position and size of windows*/
    static int cmpX,cmpY,cmpW,cmpH;          //main command panel
    static int mainX,mainY,mainW,mainH;      //main VTM view
    static int prpX,prpY,prpW,prpH;          //edit attribs panel (node/edge attributes)
    static int tabX,tabY,tabW,tabH;          //tables panel (NS bindings, properties)
    static boolean showEditWindow=true;
    static boolean showNSWindow=true;
    static boolean showRadarWindow=false;
    static int radarCameraIndex=1;  //0 is for the main camera - this index will be incremented each time the radar view is destroyed - begins at 1 (incremented just after deletion)

    static void initLookAndFeel(){
	String key;
	for (Enumeration e=UIManager.getLookAndFeelDefaults().keys();e.hasMoreElements();){
	    key=(String)e.nextElement();
	    if (key.endsWith(".font") || key.endsWith("Font")){UIManager.put(key,Editor.smallFont);}
	}
	UIManager.put("ProgressBar.foreground",pastelBlue);
	UIManager.put("ProgressBar.background",java.awt.Color.lightGray);
	UIManager.put("Label.foreground",java.awt.Color.black);
    }

    Editor application;

    /*save last (URL_LIMIT) URLs used to retrieve RDF models*/
    static Vector lastURLs;
    /*maximum number of URLs bookmared in remote RDF/XML import*/
    static int URL_LIMIT=5;

    ConfigManager(Editor e){
	this.application=e;
    }

    /*init Swing panels and VTM view*/
    void initWindows(){
	java.awt.Dimension screen=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	//main command panel
	cmpX=0;cmpY=0;
	cmpW=250;cmpH=240;
	application.cmp=new MainCmdPanel(application,cmpX,cmpY,cmpW,cmpH);
	mainX=cmpW;mainY=0;
	mainW=screen.width-cmpW;mainH=600;
	prpX=0;prpY=cmpH;
	prpW=cmpW;prpH=mainH-cmpH;
	tabX=0;tabY=mainH;
	tabW=screen.width;tabH=screen.height-mainH;
	application.propsp=new PropsPanel(application,prpX,prpY,prpW,prpH);
	if (Utils.osIsWindows()){tabH-=28;} //if we have a Windows GUI, take the taskbar into account
	application.tblp=new TablePanel(application,tabX,tabY,tabW,tabH);
	//VTM entities (virtual spaces, cameras, views...)
	Editor.vsm.addVirtualSpace(Editor.mainVirtualSpace);
	Vector cameras=new Vector();cameras.add(Editor.vsm.addCamera(Editor.mainVirtualSpace));
	(Editor.vsm.addView(cameras,Editor.mainView,mainW,mainH,true,false)).setStatusBarFont(Editor.smallFont);
	Editor.vsm.getView(Editor.mainView).setLocation(mainX,mainY);
	application.eeh=new EditorEvtHdlr(application);
	Editor.vsm.getView(Editor.mainView).setEventHandler(application.eeh);
    }

    /*layout the windows according to the default values or values provided in the config file, then make them visible*/
    void layoutWindows(){
	application.cmp.setLocation(cmpX,cmpY);
	application.cmp.setSize(cmpW,cmpH);
	View v=Editor.vsm.getView(Editor.mainView);
	v.setLocation(mainX,mainY);
	v.setSize(mainW,mainH);
	application.propsp.setLocation(prpX,prpY);
	application.propsp.setSize(prpW,prpH);
	application.tblp.setLocation(tabX,tabY);
	application.tblp.setSize(tabW,tabH);
	application.cmp.setVisible(true);
	if (!showEditWindow){application.cmp.showPropsMn.setSelected(false);}
	else {application.propsp.setVisible(true);}
	if (!showNSWindow){application.cmp.showTablesMn.setSelected(false);} 
	else {application.tblp.setVisible(true);}
	Editor.vsm.getView(Editor.mainView).setVisible(true);
    }

    /*update window position and size variables prior to saving them in the config file*/
    void updateWindowVariables(){
	cmpX=application.cmp.getX();cmpY=application.cmp.getY();
	cmpW=application.cmp.getWidth();cmpH=application.cmp.getHeight();
	View v=Editor.vsm.getView(Editor.mainView);
	mainX=v.getFrame().getX();mainY=v.getFrame().getY();
	mainW=v.getFrame().getWidth();mainH=v.getFrame().getHeight();
	prpX=application.propsp.getX();prpY=application.propsp.getY();
	prpW=application.propsp.getWidth();prpH=application.propsp.getHeight();
	tabX=application.tblp.getX();tabY=application.tblp.getY();
	tabW=application.tblp.getWidth();tabH=application.tblp.getHeight();
    }

    /*load user prefs from config file (in theory, if the file cannot be found, 
      every variable should have a default value)*/
    void initConfig(){//this one should be cleaned a little, with better(finer-grain) exception management, to get the most out of the config file in case of error
	if (Editor.cfgFile.exists()){
	    try {
		Document d=application.xmlMngr.parse(Editor.cfgFile.toString(),true);
		d.normalize();
		Element rt=d.getDocumentElement();
		Element e=(Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"directories")).item(0);
		try {
		    Editor.m_TmpDir=new File(e.getElementsByTagNameNS(Editor.isavizURI,"tmpDir").item(0).getFirstChild().getNodeValue());
		    Editor.dltOnExit=(new Boolean(((Element)e.getElementsByTagNameNS(Editor.isavizURI,"tmpDir").item(0)).getAttribute("value"))).booleanValue();
		}
		catch (Exception ex){}
		try {Editor.projectDir=new File(e.getElementsByTagNameNS(Editor.isavizURI,"projDir").item(0).getFirstChild().getNodeValue());}
		catch (Exception ex){}
		try {Editor.rdfDir=new File(e.getElementsByTagNameNS(Editor.isavizURI,"rdfDir").item(0).getFirstChild().getNodeValue());}
		catch (Exception ex){}
		try {Editor.m_GraphVizPath=new File(e.getElementsByTagNameNS(Editor.isavizURI,"dotExec").item(0).getFirstChild().getNodeValue());}
		catch (Exception ex){}
		try {Editor.m_GraphVizFontDir=new File(e.getElementsByTagNameNS(Editor.isavizURI,"graphvizFontDir").item(0).getFirstChild().getNodeValue());}
		catch (Exception ex){}
		try {
		    e=(Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"constants")).item(0);
		    Editor.DEFAULT_NAMESPACE=e.getAttribute("defaultNamespace");
		    Editor.ANON_NODE=e.getAttribute("anonymousNodes");
		    Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS=(new Boolean(e.getAttribute("alwaysIncludeLang"))).booleanValue();
		    Editor.DEFAULT_LANGUAGE_IN_LITERALS=e.getAttribute("defaultLang");
		    application.setAbbrevSyntax((new Boolean(e.getAttribute("abbrevSyntax"))).booleanValue());
		    Editor.SHOW_ANON_ID=(new Boolean(e.getAttribute("showAnonIds"))).booleanValue();
		    Editor.DISP_AS_LABEL=(new Boolean(e.getAttribute("displayLabels"))).booleanValue();
		    Editor.MAX_LIT_CHAR_COUNT=(new Integer(e.getAttribute("maxLitCharCount"))).intValue();
		    Editor.GRAPH_ORIENTATION=e.getAttribute("graphOrient");
		    Editor.GRAPHVIZ_VERSION=(new Integer(e.getAttribute("graphVizVersion"))).intValue();
		    application.setAntialiasing((new Boolean(e.getAttribute("antialiasing"))).booleanValue());
		    Editor.SAVE_WINDOW_LAYOUT=(new Boolean(e.getAttribute("saveWindowLayout"))).booleanValue();;
		    if (Editor.SAVE_WINDOW_LAYOUT){//window layout preferences
			try {
			    e=(Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"windows")).item(0);
			    cmpX=(new Integer(e.getAttribute("cmpX"))).intValue();
			    cmpY=(new Integer(e.getAttribute("cmpY"))).intValue();
			    cmpW=(new Integer(e.getAttribute("cmpW"))).intValue();
			    cmpH=(new Integer(e.getAttribute("cmpH"))).intValue();
			    mainX=(new Integer(e.getAttribute("mainX"))).intValue();
			    mainY=(new Integer(e.getAttribute("mainY"))).intValue();
			    mainW=(new Integer(e.getAttribute("mainW"))).intValue();
			    mainH=(new Integer(e.getAttribute("mainH"))).intValue();
			    prpX=(new Integer(e.getAttribute("prpX"))).intValue();
			    prpY=(new Integer(e.getAttribute("prpY"))).intValue();
			    prpW=(new Integer(e.getAttribute("prpW"))).intValue();
			    prpH=(new Integer(e.getAttribute("prpH"))).intValue();
			    tabX=(new Integer(e.getAttribute("tabX"))).intValue();
			    tabY=(new Integer(e.getAttribute("tabY"))).intValue();
			    tabW=(new Integer(e.getAttribute("tabW"))).intValue();
			    tabH=(new Integer(e.getAttribute("tabH"))).intValue();
			    showNSWindow=(new Boolean(e.getAttribute("showNSWindow"))).booleanValue();
			    showEditWindow=(new Boolean(e.getAttribute("showEditWindow"))).booleanValue();
			}
			catch (Exception ex2){}
		    }
		}
		catch (Exception ex){}
		//web browser preferences
		try {
		    e=(Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"webBrowser")).item(0);
		    Editor.autoDetectBrowser=(new Boolean(e.getAttribute("autoDetect"))).booleanValue();
		    Editor.browserPath=new File(e.getAttribute("path"));
		    Editor.browserOptions=e.getAttribute("options");
		}
		catch (Exception ex){}
		//proxy/firewall settings
		try {
		    e=(Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"proxy")).item(0);
		    updateProxy((new Boolean(e.getAttribute("enable"))).booleanValue(),e.getAttribute("host"),e.getAttribute("port"));
		}
		catch (Exception ex){System.getProperties().put("proxySet","false");}
		//last URLs used when importing remote RDF files
		lastURLs=new Vector();
		try {
		    NodeList nl=((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"urls")).item(0)).getElementsByTagNameNS(Editor.isavizURI,"li");
		    for (int i=0;i<nl.getLength();i++){
			if (i<URL_LIMIT){lastURLs.add(nl.item(i).getFirstChild().getNodeValue());}
		    }
		}
		catch (NullPointerException ex1){}
		try {
		    assignColorsToGraph(((Element)(rt.getElementsByTagNameNS(Editor.isavizURI,"colorScheme")).item(0)).getAttribute("select"));
		}
		catch (NullPointerException ex){}
	    }
	    catch (Exception ex){application.errorMessages.append("Error while loading IsaViz configuration file (isaviz.cfg): "+ex+"\n");application.reportError=true;}
	}
	application.resetNamespaceBindings(); //do it before the 2 following methods so that prefixes get initialized
	application.initRDFMSProperties();    //in property constructors
	application.initRDFSProperties();
	if (application.reportError){Editor.vsm.getView(Editor.mainView).setStatusBarText("There were errors during initialization ('Ctrl+E' to display error log)");application.reportError=false;}
    }

    /*save user prefs to config file*/
    void saveConfig(){
 	DOMImplementation di=new DOMImplementationImpl();
// 	DocumentType dtd=di.createDocumentType("isv:config",null,"isv.dtd");
	Document cfg=di.createDocument(Editor.isavizURI,"isv:config",null);
	//generate the XML document
	Element rt=cfg.getDocumentElement();
	rt.setAttribute("xmlns:isv",Editor.isavizURI);
	//save directory preferences
	Element dirs=cfg.createElementNS(Editor.isavizURI,"isv:directories");
	rt.appendChild(dirs);
	Element aDir=cfg.createElementNS(Editor.isavizURI,"isv:tmpDir");
	aDir.appendChild(cfg.createTextNode(Editor.m_TmpDir.toString()));
	aDir.setAttribute("value",String.valueOf(Editor.dltOnExit));
	dirs.appendChild(aDir);
	aDir=cfg.createElementNS(Editor.isavizURI,"isv:projDir");
	aDir.appendChild(cfg.createTextNode(Editor.projectDir.toString()));
	dirs.appendChild(aDir);
	aDir=cfg.createElementNS(Editor.isavizURI,"isv:rdfDir");
	aDir.appendChild(cfg.createTextNode(Editor.rdfDir.toString()));
	dirs.appendChild(aDir);
	aDir=cfg.createElementNS(Editor.isavizURI,"isv:dotExec");
	aDir.appendChild(cfg.createTextNode(Editor.m_GraphVizPath.toString()));
	dirs.appendChild(aDir);
	aDir=cfg.createElementNS(Editor.isavizURI,"isv:graphvizFontDir");
	aDir.appendChild(cfg.createTextNode(Editor.m_GraphVizFontDir.toString()));
	dirs.appendChild(aDir);
	//save misc. constants
	Element consts=cfg.createElementNS(Editor.isavizURI,"isv:constants");
	rt.appendChild(consts);
	consts.setAttribute("defaultNamespace",Editor.DEFAULT_NAMESPACE);
	consts.setAttribute("anonymousNodes",Editor.ANON_NODE);
	consts.setAttribute("alwaysIncludeLang",String.valueOf(Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS));
	consts.setAttribute("defaultLang",Editor.DEFAULT_LANGUAGE_IN_LITERALS);
	if (Editor.ABBREV_SYNTAX){consts.setAttribute("abbrevSyntax","true");} else {consts.setAttribute("abbrevSyntax","false");}
	if (Editor.SHOW_ANON_ID){consts.setAttribute("showAnonIds","true");} else {consts.setAttribute("showAnonIds","false");}
	if (Editor.DISP_AS_LABEL){consts.setAttribute("displayLabels","true");} else {consts.setAttribute("displayLabels","false");}
	consts.setAttribute("graphOrient",Editor.GRAPH_ORIENTATION);
	consts.setAttribute("graphVizVersion",String.valueOf(Editor.GRAPHVIZ_VERSION));
	consts.setAttribute("maxLitCharCount",String.valueOf(Editor.MAX_LIT_CHAR_COUNT));
	consts.setAttribute("antialiasing",String.valueOf(Editor.ANTIALIASING));
	consts.setAttribute("saveWindowLayout",String.valueOf(Editor.SAVE_WINDOW_LAYOUT));
	//browser settings
	consts=cfg.createElementNS(Editor.isavizURI,"isv:webBrowser");
	consts.setAttribute("autoDetect",String.valueOf(Editor.autoDetectBrowser));
	consts.setAttribute("path",Editor.browserPath.toString());
	consts.setAttribute("options",Editor.browserOptions);
	rt.appendChild(consts);
	//proxy
	consts=cfg.createElementNS(Editor.isavizURI,"isv:proxy");
	consts.setAttribute("enable",String.valueOf(Editor.useProxy));
	consts.setAttribute("host",Editor.proxyHost);
	consts.setAttribute("port",Editor.proxyPort);
	rt.appendChild(consts);
	//window locations and sizes
	//first update the values
	if (Editor.SAVE_WINDOW_LAYOUT){
	    updateWindowVariables();
	    consts=cfg.createElementNS(Editor.isavizURI,"isv:windows");
	    consts.setAttribute("cmpX",String.valueOf(cmpX));
	    consts.setAttribute("cmpY",String.valueOf(cmpY));
	    consts.setAttribute("cmpW",String.valueOf(cmpW));
	    consts.setAttribute("cmpH",String.valueOf(cmpH));
	    consts.setAttribute("mainX",String.valueOf(mainX));
	    consts.setAttribute("mainY",String.valueOf(mainY));
	    consts.setAttribute("mainW",String.valueOf(mainW));
	    consts.setAttribute("mainH",String.valueOf(mainH));
	    consts.setAttribute("prpX",String.valueOf(prpX));
	    consts.setAttribute("prpY",String.valueOf(prpY));
	    consts.setAttribute("prpW",String.valueOf(prpW));
	    consts.setAttribute("prpH",String.valueOf(prpH));
	    consts.setAttribute("tabX",String.valueOf(tabX));
	    consts.setAttribute("tabY",String.valueOf(tabY));
	    consts.setAttribute("tabW",String.valueOf(tabW));
	    consts.setAttribute("tabH",String.valueOf(tabH));
	    consts.setAttribute("showNSWindow",String.valueOf(showNSWindow));
	    consts.setAttribute("showEditWindow",String.valueOf(showEditWindow));
	    rt.appendChild(consts);
	}
	//colors
	consts=cfg.createElementNS(Editor.isavizURI,"isv:colorScheme");
	rt.appendChild(consts);
	consts.setAttribute("select",COLOR_SCHEME);
// 	if (currentColScheme!=null && currentColScheme.length()>0){consts.setAttribute("select",currentColScheme);}
	//bookmark URLs
	consts=cfg.createElementNS(Editor.isavizURI,"isv:urls");
	rt.appendChild(consts);
	if (lastURLs!=null){
	    for (int i=0;i<lastURLs.size();i++){
		Element aURL=cfg.createElementNS(Editor.isavizURI,"isv:li");
		aURL.appendChild(cfg.createTextNode((String)lastURLs.elementAt(i)));
		consts.appendChild(aURL);
	    }
	}
	if (Editor.cfgFile.exists()){Editor.cfgFile.delete();}
	application.xmlMngr.serialize(cfg,Editor.cfgFile);
    }

    /*add last entered URL to bookmarks (get rid of an existing one if more than URL_LIMIT URLs are already stored)*/
    void addLastURL(String s){
	boolean exists=false;
	for (int i=0;i<lastURLs.size();i++){
	    if (((String)lastURLs.elementAt(i)).equals(s)){
		if (i>0){
		    String tmp=(String)lastURLs.firstElement();
		    lastURLs.setElementAt(s,0);
		    lastURLs.setElementAt(tmp,i);
		}
		return;
	    }
	}
	lastURLs.insertElementAt(s,0);
	if (lastURLs.size()>URL_LIMIT){lastURLs.removeElementAt(URL_LIMIT);}  //we limit the list to five elements
    }

    /*save URLs on exit, without modifying user settings if he did not ask to do so*/
    void saveURLs(){
	try {
	    Document d;
	    Element rt;
	    Element urls;
	    if (Editor.cfgFile.exists()){
		d=application.xmlMngr.parse(Editor.cfgFile.toString(),true);
		d.normalize();
		rt=d.getDocumentElement();
		if ((rt.getElementsByTagNameNS(Editor.isavizURI,"urls")).getLength()>0){
		    rt.removeChild((rt.getElementsByTagNameNS(Editor.isavizURI,"urls")).item(0));
		}
		urls=d.createElementNS(Editor.isavizURI,"isv:urls");
		if (lastURLs!=null){
		    for (int i=0;i<lastURLs.size();i++){
			Element aURL=d.createElementNS(Editor.isavizURI,"isv:li");
			aURL.appendChild(d.createTextNode((String)lastURLs.elementAt(i)));
			urls.appendChild(aURL);
		    }
		}
	    }
	    else {
		DOMImplementation di=new DOMImplementationImpl();
		d=di.createDocument(Editor.isavizURI,"isv:config",null);
		rt=d.getDocumentElement();
		urls=d.createElementNS(Editor.isavizURI,"isv:urls");
		if (lastURLs!=null){
		    for (int i=0;i<lastURLs.size();i++){
			Element aURL=d.createElementNS(Editor.isavizURI,"isv:li");
			aURL.appendChild(d.createTextNode((String)lastURLs.elementAt(i)));
			urls.appendChild(aURL);
		    }
		}
	    }
	    rt.appendChild(urls);
	    application.xmlMngr.serialize(d,Editor.cfgFile);
	}
	catch (Exception ex){}
    }

    /*choose a color scheme (2 values for now: "b&w" or "default")*/
    static void assignColorsToGraph(String selectedScheme){
	if (!selectedScheme.equals(COLOR_SCHEME)){//do not change anything if selected scheme is current scheme
	    if (selectedScheme.equals("b&w")){
		resourceColorF=Color.white;
		resourceColorTB=Color.black;
		propertyColorB=Color.black;
		propertyColorT=Color.black;
		literalColorF=Color.white;
		literalColorTB=Color.black;
		selectionColorF=Color.white;
		selectionColorTB=Color.red;
		mouseInsideColor=Color.green;
		commentColorF=new Color(231,231,231);
		commentColorTB=new Color(150,150,150);
		bckgColor=Color.lightGray;
		searchColor=Color.red;
	    }
	    else {//has to be "default"
		resourceColorF=new Color(115,191,115);
		resourceColorTB=new Color(66,105,66);
		propertyColorB=new Color(90,89,206);
		propertyColorT=new Color(90,89,206);
		literalColorF=new Color(255,223,123);
		literalColorTB=new Color(132,117,66);
		selectionColorF=new Color(255,150,150);
		selectionColorTB=new Color(255,0,0);
		mouseInsideColor=new Color(0,0,0);
		commentColorF=new Color(231,231,231);
		commentColorTB=new Color(180,180,180);
		bckgColor=new Color(231,231,231);
		searchColor=new Color(255,0,0);
	    }
	    cursorColor=Color.black;
	    COLOR_SCHEME=selectedScheme;
	    assignColorsToGraph();
	}
    }

    /*actually assign colors from chosen color scheme to graph entities*/
    static void assignColorsToGraph(){
	VirtualSpace vs=Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace);
	float[] hsv=Color.RGBtoHSB(resourceColorF.getRed(),resourceColorF.getGreen(),resourceColorF.getBlue(),new float[3]);
	resFh=hsv[0];resFs=hsv[1];resFv=hsv[2];
	hsv=Color.RGBtoHSB(resourceColorTB.getRed(),resourceColorTB.getGreen(),resourceColorTB.getBlue(),new float[3]);
	resTBh=hsv[0];resTBs=hsv[1];resTBv=hsv[2];
	hsv=Color.RGBtoHSB(propertyColorB.getRed(),propertyColorB.getGreen(),propertyColorB.getBlue(),new float[3]);
	prpBh=hsv[0];prpBs=hsv[1];prpBv=hsv[2];
	hsv=Color.RGBtoHSB(propertyColorT.getRed(),propertyColorT.getGreen(),propertyColorT.getBlue(),new float[3]);
	prpTh=hsv[0];prpTs=hsv[1];prpTv=hsv[2];
	hsv=Color.RGBtoHSB(literalColorF.getRed(),literalColorF.getGreen(),literalColorF.getBlue(),new float[3]);
	litFh=hsv[0];litFs=hsv[1];litFv=hsv[2];
	hsv=Color.RGBtoHSB(literalColorTB.getRed(),literalColorTB.getGreen(),literalColorTB.getBlue(),new float[3]);
	litTBh=hsv[0];litTBs=hsv[1];litTBv=hsv[2];
	hsv=Color.RGBtoHSB(selectionColorF.getRed(),selectionColorF.getGreen(),selectionColorF.getBlue(),new float[3]);
	selFh=hsv[0];selFs=hsv[1];selFv=hsv[2];
	hsv=Color.RGBtoHSB(selectionColorTB.getRed(),selectionColorTB.getGreen(),selectionColorTB.getBlue(),new float[3]);
	selTh=hsv[0];selTs=hsv[1];selTv=hsv[2];
	hsv=Color.RGBtoHSB(commentColorF.getRed(),commentColorF.getGreen(),commentColorF.getBlue(),new float[3]);
	comFh=hsv[0];comFs=hsv[1];comFv=hsv[2];
	hsv=Color.RGBtoHSB(commentColorTB.getRed(),commentColorTB.getGreen(),commentColorTB.getBlue(),new float[3]);
	comTh=hsv[0];comTs=hsv[1];comTv=hsv[2];
	hsv=Color.RGBtoHSB(searchColor.getRed(),searchColor.getGreen(),searchColor.getBlue(),new float[3]);
	srhTh=hsv[0];srhTs=hsv[1];srhTv=hsv[2];
	Glyph g;
	//resources
	Vector v=vs.getGlyphsOfType(Editor.resEllipseType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){
		g.setHSVColor(selFh,selFs,selFv);
		g.setHSVbColor(selTh,selTs,selTv);
	    }
	    else if (((INode)g.getOwner()).isCommented()){
		g.setHSVColor(comFh,comFs,comFv);
		g.setHSVbColor(comTh,comTs,comTv);
	    }
	    else {
		g.setHSVColor(resFh,resFs,resFv);
		g.setHSVbColor(resTBh,resTBs,resTBv);
	    }
	    g.setMouseInsideColor(mouseInsideColor);
	}
	v=vs.getGlyphsOfType(Editor.resTextType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){g.setHSVColor(selTh,selTs,selTv);}
	    else if (((INode)g.getOwner()).isCommented()){g.setHSVColor(comTh,comTs,comTv);}
	    else {g.setHSVColor(resTBh,resTBs,resTBv);}
	}
	//predicates
	v=vs.getGlyphsOfType(Editor.propPathType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){g.setHSVColor(selTh,selTs,selTv);}
	    else if (((INode)g.getOwner()).isCommented()){g.setHSVColor(comTh,comTs,comTv);}
	    else {g.setHSVColor(prpBh,prpBs,prpBv);}
	}
	v=vs.getGlyphsOfType(Editor.propHeadType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){g.setHSVColor(selTh,selTs,selTv);}
	    else if (((INode)g.getOwner()).isCommented()){g.setHSVColor(comTh,comTs,comTv);}
	    else {g.setHSVColor(prpBh,prpBs,prpBv);}	    
	}
	v=vs.getGlyphsOfType(Editor.propTextType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){g.setHSVColor(selTh,selTs,selTv);}
	    else if (((INode)g.getOwner()).isCommented()){g.setHSVColor(comTh,comTs,comTv);}
	    else {g.setHSVColor(prpTh,prpTs,prpTv);}	    
	}
	//literals
	v=vs.getGlyphsOfType(Editor.litRectType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){
		g.setHSVColor(selFh,selFs,selFv);
		g.setHSVbColor(selTh,selTs,selTv);
	    }
	    else if (((INode)g.getOwner()).isCommented()){
		g.setHSVColor(comFh,comFs,comFv);
		g.setHSVbColor(comTh,comTs,comTv);
	    }
	    else {
		g.setHSVColor(litFh,litFs,litFv);
		g.setHSVbColor(litTBh,litTBs,litTBv);
	    }
	    g.setMouseInsideColor(mouseInsideColor);
	}
	v=vs.getGlyphsOfType(Editor.litTextType);
	for (int i=0;i<v.size();i++){
	    g=(Glyph)v.elementAt(i);
	    if (((INode)g.getOwner()).isSelected()){g.setHSVColor(selTh,selTs,selTv);}
	    else if (((INode)g.getOwner()).isCommented()){g.setHSVColor(comTh,comTs,comTv);}
	    else {g.setHSVColor(litTBh,litTBs,litTBv);}
	}
	//selection color
	Editor.vsm.setSelectedGlyphColor(selectionColorTB);
	Editor.vsm.setMouseInsideGlyphColor(mouseInsideColor);
	//background color
	Editor.vsm.getView(Editor.mainView).setBackgroundColor(bckgColor);
	Editor.vsm.getView(Editor.mainView).mouse.setColor(cursorColor);
    }

    /*could also be set at runtime from command line
      java -DproxySet=true -DproxyHost=proxy_host -DproxyPort=proxy_port*/
    static void updateProxy(boolean use,String hostname,String port){
	Editor.useProxy=use;
	Editor.proxyHost=hostname;
	Editor.proxyPort=port;
	if (Editor.useProxy){
	    System.getProperties().put("proxySet","true");
	    System.getProperties().put("proxyHost",Editor.proxyHost);
	    System.getProperties().put("proxyPort",Editor.proxyPort);
	}
	else {
	    System.getProperties().put("proxySet","false");
	}
    }

}

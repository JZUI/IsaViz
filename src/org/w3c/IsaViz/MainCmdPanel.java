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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import com.xerox.VTM.engine.SwingWorker;
import com.xerox.VTM.engine.View;

/*The main command panel. Contains the icon palette + menus to load/save/access preferences/etc...*/

class MainCmdPanel extends JFrame implements ActionListener,ItemListener,KeyListener,FocusListener {

    Editor application;

    PrefWindow dp;

    JMenuBar mnb;
    JMenu fileMenu;
    JMenuItem resetMn;
    JMenuItem loadMn,saveMn,saveAsMn;
    JMenuItem ldLocRDFXMLMn,ldRemRDFXMLMn,ldLocNTrMn,ldRemNTrMn;
    JMenuItem mgLocRDFXMLMn,mgRemRDFXMLMn,mgLocNTrMn,mgRemNTrMn;
    JMenuItem summaryMn;
    JMenuItem exportRDFMn,exportNTriMn,exportPNGMn,exportSVGMn;
    JMenuItem exitMn;
 
    JMenu editMenu;
    JMenuItem undoMn,cutMn,copyMn,pasteMn;
    JMenuItem selectAllNMn,selectAllEMn,advSelectMn,unselAllMn;
    JMenuItem deleteMn;
    JMenuItem prefMn;

    JMenu viewMenu;
    JMenuItem rawrdfMn,errorMn,getGlobVMn,layoutMn;
    JCheckBoxMenuItem showTablesMn,showPropsMn;

    JMenu helpMenu;
    JMenuItem aboutMn;

    JFileChooser fc;

    JRadioButton singleSelectBt,regionNSelectBt,regionESelectBt,regionZoomBt;
    JButton undoBt,cutBt,copyBt;
    JRadioButton createResBt,createPropBt,createLitBt;
    JRadioButton resizeBt,pasteBt;
    JRadioButton commSingleBt,commRegionBt,uncommSingleBt,uncommRegionBt;
    JTextField searchTf;
    JButton searchBt;


    MainCmdPanel(Editor appli,int x,int y,int width, int height){
	application=appli;

	//MENUS
	mnb=new JMenuBar();
	this.setJMenuBar(mnb);
	fileMenu=new JMenu("File");
	fileMenu.setMnemonic(KeyEvent.VK_F);
	mnb.add(fileMenu);
	resetMn = new JMenuItem("New Project");
	resetMn.addActionListener(this);
	resetMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
	fileMenu.add(resetMn);
	loadMn = new JMenuItem("Open Project...");
	loadMn.addActionListener(this);
	loadMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
	fileMenu.add(loadMn);
	saveMn = new JMenuItem("Save Project");
	saveMn.addActionListener(this);
	saveMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
	fileMenu.add(saveMn);
	saveAsMn = new JMenuItem("Save Project As...");
	saveAsMn.addActionListener(this);
	fileMenu.add(saveAsMn);
	fileMenu.addSeparator();
	summaryMn=new JMenuItem("Project info...");
	summaryMn.addActionListener(this);
	fileMenu.add(summaryMn);
	fileMenu.addSeparator();
	JMenu loadMenu=new JMenu("Import");
	JMenu replaceMenu=new JMenu("Replace");
	JMenu mergeMenu=new JMenu("Merge");
	loadMenu.add(replaceMenu);
	loadMenu.add(mergeMenu);
	ldLocRDFXMLMn = new JMenuItem("RDF/XML from file...");
	ldLocRDFXMLMn.addActionListener(this);
	replaceMenu.add(ldLocRDFXMLMn);
	ldRemRDFXMLMn = new JMenuItem("RDF/XML from URL...");
	ldRemRDFXMLMn.addActionListener(this);
	replaceMenu.add(ldRemRDFXMLMn);
	replaceMenu.addSeparator();
	ldLocNTrMn = new JMenuItem("N-Triples from file...");
	ldLocNTrMn.addActionListener(this);
	replaceMenu.add(ldLocNTrMn);
	ldRemNTrMn = new JMenuItem("N-Triples from URL...");
	ldRemNTrMn.addActionListener(this);
	replaceMenu.add(ldRemNTrMn);
	mgLocRDFXMLMn = new JMenuItem("RDF/XML from file...");
	mgLocRDFXMLMn.addActionListener(this);
	mergeMenu.add(mgLocRDFXMLMn);
	mgRemRDFXMLMn = new JMenuItem("RDF/XML from URL...");
	mgRemRDFXMLMn.addActionListener(this);
        mergeMenu.add(mgRemRDFXMLMn);
	mergeMenu.addSeparator();
	mgLocNTrMn = new JMenuItem("N-Triples from file...");
	mgLocNTrMn.addActionListener(this);
	mergeMenu.add(mgLocNTrMn);
	mgRemNTrMn = new JMenuItem("N-Triples from URL...");
	mgRemNTrMn.addActionListener(this);
	mergeMenu.add(mgRemNTrMn);
	fileMenu.add(loadMenu);
	JMenu exportMenu=new JMenu("Export");
	exportRDFMn = new JMenuItem("RDF/XML...");
	exportRDFMn.addActionListener(this);
	exportMenu.add(exportRDFMn);
	exportNTriMn = new JMenuItem("N-Triples...");
	exportNTriMn.addActionListener(this);
	exportMenu.add(exportNTriMn);
	exportMenu.addSeparator();
	exportPNGMn = new JMenuItem("PNG (current view)...");
	exportPNGMn.addActionListener(this);
	exportMenu.add(exportPNGMn);
	exportSVGMn = new JMenuItem("SVG...");
	exportSVGMn.addActionListener(this);
	exportMenu.add(exportSVGMn);
	fileMenu.add(exportMenu);
	fileMenu.addSeparator();
	exitMn = new JMenuItem("Exit");
	exitMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
	fileMenu.add(exitMn);
	exitMn.addActionListener(this);

	editMenu=new JMenu("Edit");
	editMenu.setMnemonic(KeyEvent.VK_E);
	mnb.add(editMenu);


	undoMn=new JMenuItem("Undo");
	undoMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
	undoMn.addActionListener(a2);
	editMenu.add(undoMn);
	undoMn.setEnabled(false);
	editMenu.addSeparator();
	cutMn=new JMenuItem("Cut");
	cutMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
	cutMn.addActionListener(a2);
	editMenu.add(cutMn);
	copyMn=new JMenuItem("Copy");
	copyMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
	copyMn.addActionListener(a2);
	editMenu.add(copyMn);
	pasteMn=new JMenuItem("Paste");
	pasteMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
	pasteMn.addActionListener(a2);
	editMenu.add(pasteMn);
	pasteMn.setEnabled(false);
	deleteMn=new JMenuItem("Delete");
	deleteMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));  //0=no mask
	deleteMn.addActionListener(a2);
	editMenu.add(deleteMn);
	editMenu.addSeparator();
	selectAllNMn=new JMenuItem("Select All Nodes");
	selectAllNMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
	selectAllNMn.addActionListener(a2);
	editMenu.add(selectAllNMn);
	selectAllEMn=new JMenuItem("Select All Edges");
	selectAllEMn.addActionListener(a2);
	editMenu.add(selectAllEMn);
	advSelectMn=new JMenuItem("Advanced Selection...");
	advSelectMn.addActionListener(a2);
	editMenu.add(advSelectMn);
	unselAllMn=new JMenuItem("Unselect All");
	unselAllMn.addActionListener(a2);
	editMenu.add(unselAllMn);
	editMenu.addSeparator();
	prefMn = new JMenuItem("Preferences...");
	prefMn.addActionListener(this);
	editMenu.add(prefMn);

	viewMenu=new JMenu("Views");
	viewMenu.setMnemonic(KeyEvent.VK_V);
	mnb.add(viewMenu);
	rawrdfMn=new JMenuItem("Show RDF/XML");
	rawrdfMn.addActionListener(this);
	viewMenu.add(rawrdfMn);
	errorMn=new JMenuItem("Show Error log");
	errorMn.addActionListener(this);
	errorMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
	viewMenu.add(errorMn);
	getGlobVMn=new JMenuItem("Global View");
	getGlobVMn.addActionListener(this);
	getGlobVMn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
	viewMenu.add(getGlobVMn);
	layoutMn=new JMenuItem("Suggest Layout");
	layoutMn.addActionListener(this);
	viewMenu.add(layoutMn);
	viewMenu.addSeparator();
	showTablesMn=new JCheckBoxMenuItem("Show NS/Property Window");
	showTablesMn.setSelected(ConfigManager.showNSWindow);
	showTablesMn.addItemListener(this);
	viewMenu.add(showTablesMn);
	showPropsMn=new JCheckBoxMenuItem("Show Edit Window");
	showPropsMn.setSelected(ConfigManager.showEditWindow);
	showPropsMn.addItemListener(this);
	viewMenu.add(showPropsMn);

	helpMenu=new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
	mnb.add(helpMenu);
	aboutMn = new JMenuItem("About");
	aboutMn.addActionListener(this);
	helpMenu.add(aboutMn);

	//ICON PALETTE
	Container cpane=this.getContentPane();
	GridBagLayout gridBag=new GridBagLayout();
	GridBagConstraints constraints=new GridBagConstraints();
	cpane.setLayout(gridBag);
	ButtonGroup bg=new ButtonGroup();

	//1st row title
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.CENTER;
	GridBagLayout gridBag1=new GridBagLayout();
	GridBagConstraints constraints1=new GridBagConstraints();
	JPanel p1=new JPanel();
	JLabel lgd1=new JLabel("Selection/Creation");
	HSepPanel hp1=new HSepPanel(1,true,null);
	p1.setLayout(gridBag1);
	constraints1.fill=GridBagConstraints.NONE;
	constraints1.anchor=GridBagConstraints.WEST;
	buildConstraints(constraints1,0,0,1,1,10,100);
	gridBag1.setConstraints(lgd1,constraints1);
	p1.add(lgd1);
	constraints1.fill=GridBagConstraints.HORIZONTAL;
	constraints1.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints1,1,0,1,1,90,0);
	gridBag1.setConstraints(hp1,constraints1);
	p1.add(hp1);
	buildConstraints(constraints,0,0,6,1,100,8);
	gridBag.setConstraints(p1,constraints);
	cpane.add(p1);
	//1st row of icons
	constraints.fill=GridBagConstraints.NONE;
	constraints.anchor=GridBagConstraints.CENTER;
	singleSelectBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Select24b.gif")));
	singleSelectBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Select24g.gif")));
	singleSelectBt.addActionListener(a1);
	singleSelectBt.setToolTipText("Select");
	bg.add(singleSelectBt);
	buildConstraints(constraints,0,1,1,1,16,12);
	gridBag.setConstraints(singleSelectBt,constraints);
	cpane.add(singleSelectBt);
	regionNSelectBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/RegionNode24b.gif")));
	regionNSelectBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/RegionNode24g.gif")));
	regionNSelectBt.addActionListener(a1);
	regionNSelectBt.setToolTipText("Select Nodes in Region");
	bg.add(regionNSelectBt);
	buildConstraints(constraints,1,1,1,1,16,0);
	gridBag.setConstraints(regionNSelectBt,constraints);
	cpane.add(regionNSelectBt);
	regionESelectBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/RegionEdge24b.gif")));
	regionESelectBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/RegionEdge24g.gif")));
	regionESelectBt.addActionListener(a1);
	bg.add(regionESelectBt);
	regionESelectBt.setToolTipText("Select Edges in Region");
	buildConstraints(constraints,2,1,1,1,16,0);
	gridBag.setConstraints(regionESelectBt,constraints);
	cpane.add(regionESelectBt);
	createResBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Resource24b.gif")));
	createResBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Resource24g.gif")));
	createResBt.addActionListener(a1);
	createResBt.setToolTipText("Create New Resource");
	bg.add(createResBt);
	buildConstraints(constraints,3,1,1,1,16,0);
	gridBag.setConstraints(createResBt,constraints);
	cpane.add(createResBt);
	createLitBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Literal24b.gif")));
	createLitBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Literal24g.gif")));
	createLitBt.addActionListener(a1);
	createLitBt.setToolTipText("Create New Literal");
	bg.add(createLitBt);
	buildConstraints(constraints,4,1,1,1,16,0);
	gridBag.setConstraints(createLitBt,constraints);
	cpane.add(createLitBt);
	createPropBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Property24b.gif")));
	createPropBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Property24g.gif")));
	createPropBt.addActionListener(a1);
	createPropBt.setToolTipText("Create New Property");
	bg.add(createPropBt);
	buildConstraints(constraints,5,1,1,1,16,0);
	gridBag.setConstraints(createPropBt,constraints);
	cpane.add(createPropBt);

	//2nd row title
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.CENTER;
	GridBagLayout gridBag4=new GridBagLayout();
	GridBagConstraints constraints4=new GridBagConstraints();
	JPanel p4=new JPanel();
	JLabel lgd4=new JLabel("Zoom/(De)activation/Resize");
	HSepPanel hp4=new HSepPanel(1,true,null);
	p4.setLayout(gridBag4);
	constraints4.fill=GridBagConstraints.NONE;
	constraints4.anchor=GridBagConstraints.WEST;
	buildConstraints(constraints4,0,0,1,1,10,100);
	gridBag4.setConstraints(lgd4,constraints4);
	p4.add(lgd4);
	constraints4.fill=GridBagConstraints.HORIZONTAL;
	constraints4.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints4,1,0,1,1,90,0);
	gridBag4.setConstraints(hp4,constraints4);
	p4.add(hp4);
	buildConstraints(constraints,0,2,6,1,100,8);
	gridBag.setConstraints(p4,constraints);
	cpane.add(p4);
	//2nd row of icons
	constraints.fill=GridBagConstraints.NONE;
	constraints.anchor=GridBagConstraints.CENTER;
	regionZoomBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/RegionZoom24b.gif")));
	regionZoomBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/RegionZoom24g.gif")));
	regionZoomBt.addActionListener(a1);
	regionZoomBt.setToolTipText("Zoom in Region");
	bg.add(regionZoomBt);
	buildConstraints(constraints,0,3,1,1,16,0);
	gridBag.setConstraints(regionZoomBt,constraints);
	cpane.add(regionZoomBt);
	commSingleBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Comment24b.gif")));
	commSingleBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Comment24g.gif")));
	commSingleBt.addActionListener(a1);
	commSingleBt.setToolTipText("Deactivate Node/Edge");
	bg.add(commSingleBt);
	buildConstraints(constraints,1,3,1,1,16,12);
	gridBag.setConstraints(commSingleBt,constraints);
	cpane.add(commSingleBt);
 	uncommSingleBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/UnComment24b.gif")));
 	uncommSingleBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/UnComment24g.gif")));
	uncommSingleBt.addActionListener(a1);
	uncommSingleBt.setToolTipText("Reactivate Node/Edge");
	bg.add(uncommSingleBt);
	buildConstraints(constraints,2,3,1,1,16,0);
	gridBag.setConstraints(uncommSingleBt,constraints);
	cpane.add(uncommSingleBt);
	commRegionBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/CommentRegion24b.gif")));
	commRegionBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/CommentRegion24g.gif")));
	commRegionBt.addActionListener(a1);
	commRegionBt.setToolTipText("Deactivate Region");
	bg.add(commRegionBt);
	buildConstraints(constraints,3,3,1,1,16,0);
	gridBag.setConstraints(commRegionBt,constraints);
	cpane.add(commRegionBt);
 	uncommRegionBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/UnCommentRegion24b.gif")));
 	uncommRegionBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/UnCommentRegion24g.gif")));
	uncommRegionBt.addActionListener(a1);
	uncommRegionBt.setToolTipText("Reactivate Region");
	bg.add(uncommRegionBt);
	buildConstraints(constraints,4,3,1,1,16,0);
	gridBag.setConstraints(uncommRegionBt,constraints);
	cpane.add(uncommRegionBt);
	resizeBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Resize24b.gif")));
	resizeBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Resize24g.gif")));
	resizeBt.addActionListener(a1);
	resizeBt.setToolTipText("Move and Resize Node/Edge");
	bg.add(resizeBt);
	buildConstraints(constraints,5,3,1,1,16,0);
	gridBag.setConstraints(resizeBt,constraints);
	cpane.add(resizeBt);

	//3rd row title
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.CENTER;
	GridBagLayout gridBag2=new GridBagLayout();
	GridBagConstraints constraints2=new GridBagConstraints();
	JPanel p2=new JPanel();
	JLabel lgd2=new JLabel("Editing");
	HSepPanel hp2=new HSepPanel(1,true,null);
	p2.setLayout(gridBag2);
	constraints2.fill=GridBagConstraints.NONE;
	constraints2.anchor=GridBagConstraints.WEST;
	buildConstraints(constraints2,0,0,1,1,10,100);
	gridBag2.setConstraints(lgd2,constraints2);
	p2.add(lgd2);
	constraints2.fill=GridBagConstraints.HORIZONTAL;
	constraints2.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints2,1,0,1,1,90,0);
	gridBag2.setConstraints(hp2,constraints2);
	p2.add(hp2);
	buildConstraints(constraints,0,4,6,1,100,8);
	gridBag.setConstraints(p2,constraints);
	cpane.add(p2);
	//3rd row if icons
	constraints.fill=GridBagConstraints.NONE;
	constraints.anchor=GridBagConstraints.CENTER;

	undoBt=new JButton(new ImageIcon(this.getClass().getResource("/images/Undo24b.gif")));
	undoBt.setContentAreaFilled(false);
	undoBt.setBorderPainted(false);
	undoBt.setFocusPainted(false);
	undoBt.setMargin(singleSelectBt.getMargin());
	undoBt.setRolloverIcon(new ImageIcon(this.getClass().getResource("/images/Undo24g.gif")));
	undoBt.addActionListener(a2);
	undoBt.setToolTipText("Undo");
	buildConstraints(constraints,0,5,1,1,16,0);
	gridBag.setConstraints(undoBt,constraints);
	cpane.add(undoBt);
 	undoBt.setEnabled(false);
	cutBt=new JButton(new ImageIcon(this.getClass().getResource("/images/Cut16b.gif")));
	cutBt.setContentAreaFilled(false);
	cutBt.setBorderPainted(false);
	cutBt.setFocusPainted(false);
	cutBt.setMargin(singleSelectBt.getMargin());
	cutBt.setRolloverIcon(new ImageIcon(this.getClass().getResource("/images/Cut16g.gif")));
	cutBt.addActionListener(a2);
	cutBt.setToolTipText("Cut");
	buildConstraints(constraints,1,5,1,1,16,0);
	gridBag.setConstraints(cutBt,constraints);
	cpane.add(cutBt);
	copyBt=new JButton(new ImageIcon(this.getClass().getResource("/images/Copy24b.gif")));
	copyBt.setContentAreaFilled(false);
	copyBt.setBorderPainted(false);
	copyBt.setFocusPainted(false);
	copyBt.setMargin(singleSelectBt.getMargin());
	copyBt.setRolloverIcon(new ImageIcon(this.getClass().getResource("/images/Copy24g.gif")));
	copyBt.addActionListener(a2);
	copyBt.setToolTipText("Copy");
	buildConstraints(constraints,2,5,1,1,16,0);
	gridBag.setConstraints(copyBt,constraints);
	cpane.add(copyBt);
	pasteBt=new JRadioButton(new ImageIcon(this.getClass().getResource("/images/Paste24b.gif")));
// 	pasteBt.setContentAreaFilled(false);
// 	pasteBt.setBorderPainted(false);
// 	pasteBt.setFocusPainted(false);
// 	pasteBt.setMargin(singleSelectBt.getMargin());
	pasteBt.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/Paste24g.gif")));
	bg.add(pasteBt);
	pasteBt.addActionListener(a1);
	pasteBt.setToolTipText("Paste");
	buildConstraints(constraints,3,5,1,1,16,0);
	gridBag.setConstraints(pasteBt,constraints);
	cpane.add(pasteBt);
	pasteBt.setEnabled(false);

	//4th row title
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.CENTER;
	GridBagLayout gridBag5=new GridBagLayout();
	GridBagConstraints constraints5=new GridBagConstraints();
	JPanel p5=new JPanel();
	JLabel lgd5=new JLabel("Quick Search");
	HSepPanel hp5=new HSepPanel(1,true,null);
	p5.setLayout(gridBag5);
	constraints5.fill=GridBagConstraints.NONE;
	constraints5.anchor=GridBagConstraints.WEST;
	buildConstraints(constraints5,0,0,1,1,10,100);
	gridBag5.setConstraints(lgd5,constraints5);
	p5.add(lgd5);
	constraints5.fill=GridBagConstraints.HORIZONTAL;
	constraints5.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints5,1,0,1,1,90,0);
	gridBag5.setConstraints(hp5,constraints5);
	p5.add(hp5);
	buildConstraints(constraints,0,6,6,1,100,8);
	gridBag.setConstraints(p5,constraints);
	cpane.add(p5);
	//5rd row (search components)
	searchTf=new JTextField("");
	searchTf.addKeyListener(this);
	searchTf.addFocusListener(this);
	buildConstraints(constraints,0,7,4,1,60,12);
	gridBag.setConstraints(searchTf,constraints);
	cpane.add(searchTf);
	searchBt=new JButton("Search");
	searchBt.addKeyListener(this);
	searchBt.addActionListener(a1);
	searchBt.addFocusListener(this);
	searchBt.setPreferredSize(new Dimension(20,18));
	buildConstraints(constraints,4,7,2,1,20,0);
	gridBag.setConstraints(searchBt,constraints);
	cpane.add(searchBt);
	singleSelectBt.setSelected(true);
	this.setTitle("IsaViz RDF Editor");
	WindowListener w0=new WindowAdapter(){
		public void windowClosing(WindowEvent e){application.exit();}
		//public void windowActivated(WindowEvent e){application.alwaysUpdateViews(true);}
	    };
	this.addWindowListener(w0);
	this.pack();
	this.setLocation(x,y);
	this.setSize(width,height);
// 	this.setVisible(true);
// 	this.setResizable(false);
    }

    public void actionPerformed(ActionEvent e){
	int returnVal;
	Object o=e.getSource();
	if (o==exitMn){application.exit();}
	else if (o==resetMn){
	    application.promptReset();
	}
	else if (o==loadMn){
	    application.openProject();
	}
	else if (o==saveMn && Editor.projectFile!=null){
	    application.saveProject();
	}
	else if ((o==saveAsMn) || ((o==saveMn) && (Editor.projectFile==null))){
	    application.saveProjectAs();
	}
	else if (o==summaryMn){application.showPrjSummary();}
	else if (o==ldLocRDFXMLMn){
	    fc=new JFileChooser(Editor.lastImportRDFDir!=null ? Editor.lastImportRDFDir : Editor.rdfDir);
	    returnVal=fc.showOpenDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		application.loadRDF(fc.getSelectedFile(),RDFLoader.RDF_XML_READER);
	    }
	}
	else if (o==ldRemRDFXMLMn){
	    new URLPanel(this.application,"Specify URL:",RDFLoader.RDF_XML_READER,false);
	}
	else if (o==ldLocNTrMn){
	    fc=new JFileChooser(Editor.lastImportRDFDir!=null ? Editor.lastImportRDFDir : Editor.rdfDir);
	    returnVal=fc.showOpenDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		application.loadRDF(fc.getSelectedFile(),RDFLoader.NTRIPLE_READER);
	    }
	}
	else if (o==ldRemNTrMn){
	    new URLPanel(this.application,"Specify URL:",RDFLoader.NTRIPLE_READER,false);
	}
	else if (o==mgLocRDFXMLMn){
	    fc=new JFileChooser(Editor.lastImportRDFDir!=null ? Editor.lastImportRDFDir : Editor.rdfDir);
	    returnVal=fc.showOpenDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		application.mergeRDF(fc.getSelectedFile(),RDFLoader.RDF_XML_READER);
	    }
	}
	else if (o==mgRemRDFXMLMn){
	    new URLPanel(this.application,"Specify URL:",RDFLoader.RDF_XML_READER,true);
	}
	else if (o==mgLocNTrMn){
	    fc=new JFileChooser(Editor.lastImportRDFDir!=null ? Editor.lastImportRDFDir : Editor.rdfDir);
	    returnVal=fc.showOpenDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		application.mergeRDF(fc.getSelectedFile(),RDFLoader.NTRIPLE_READER);
	    }
	}
	else if (o==mgRemNTrMn){
	    new URLPanel(this.application,"Specify URL:",RDFLoader.NTRIPLE_READER,true);
	}
	else if (o==exportRDFMn){
	    fc=new JFileChooser(Editor.lastExportRDFDir!=null ? Editor.lastExportRDFDir : Editor.rdfDir);
	    returnVal=fc.showSaveDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		    final SwingWorker worker=new SwingWorker(){
			    public Object construct(){
				application.exportRDF(fc.getSelectedFile());
				return null;
			    }
			};
		    worker.start();
	    }
	}
	else if (o==exportNTriMn){
	    fc=new JFileChooser(Editor.lastExportRDFDir!=null ? Editor.lastExportRDFDir : Editor.rdfDir);
	    returnVal=fc.showSaveDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		    final SwingWorker worker=new SwingWorker(){
			    public Object construct(){
				application.exportNTriples(fc.getSelectedFile());
				return null;
			    }
			};
		    worker.start();
	    }
	}
	else if (o==exportPNGMn){
	    fc=new JFileChooser(Editor.lastExportRDFDir!=null ? Editor.lastExportRDFDir : Editor.rdfDir);
	    returnVal=fc.showSaveDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		    final SwingWorker worker=new SwingWorker(){
			    public Object construct(){
				application.exportPNG(fc.getSelectedFile());
				return null;
			    }
			};
		    worker.start();
	    }
	}
	else if (o==exportSVGMn){
	    fc=new JFileChooser(Editor.lastExportRDFDir!=null ? Editor.lastExportRDFDir : Editor.rdfDir);
	    returnVal=fc.showSaveDialog(this);
	    if (returnVal==JFileChooser.APPROVE_OPTION) {
		    final SwingWorker worker=new SwingWorker(){
			    public Object construct(){
				application.exportSVG(fc.getSelectedFile());
				return null;
			    }
			};
		    worker.start();
	    }
	}
	else if (o==prefMn){
	    dp=new PrefWindow(application);
	}
	else if (o==rawrdfMn){
// 		if (application.rdfLdr.rdfF!=null){displayRawFile(application.rdfLdr.rdfF,"Raw RDF Viewer");}
// 		else if (application.rdfLdr.rdfU!=null){displayRawFile(application.rdfLdr.rdfU,"Raw RDF Viewer");}
	    application.displayRawRDFXMLFile();
	}
	else if (o==errorMn){application.showErrorMessages();}
	else if (o==getGlobVMn){Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(application.mainVirtualSpace).getCamera(0),500);}
	else if (o==layoutMn){application.reLayoutGraph();}
	else if (o==aboutMn){SplashWindow sp=new SplashWindow(0,"images/IsavizSplash.gif",false);}}

    public void itemStateChanged(ItemEvent e){
	Object source=e.getItemSelectable();
        if (source==showTablesMn){
	    if (e.getStateChange()==ItemEvent.DESELECTED){
		application.showTablePanel(false);
	    }
	    else if (e.getStateChange()==ItemEvent.SELECTED){
		application.showTablePanel(true);
	    }
	}
	else if (source==showPropsMn){
	    if (e.getStateChange()==ItemEvent.DESELECTED){
		application.showPropsPanel(false);
	    }
	    else if (e.getStateChange()==ItemEvent.SELECTED){
		application.showPropsPanel(true);
	    }
	}
    }

    public void keyPressed(KeyEvent e){
	if (e.getKeyCode()==KeyEvent.VK_ENTER){
	    Object src=e.getSource();
	    if (src==searchTf || src==searchBt){application.quickSearch(searchTf.getText());}
	}
    }

    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}

    public void focusGained(FocusEvent e){}
    public void focusLost(FocusEvent e){
	application.resetQuickSearch();
    }

    ActionListener a1=new ActionListener(){
	    public void actionPerformed(ActionEvent e){
		Object src=e.getSource();
		if (src==singleSelectBt){
		    application.setMode(EditorEvtHdlr.SINGLE_SELECTION_MODE);
		    Editor.vsm.getActiveView().setStatusBarText("Hold Ctrl down for multiple selection");
		}
		else if (src==regionNSelectBt){
		    application.setMode(EditorEvtHdlr.REGION_SELECTION_MODE);
		    application.eeh.selectWhat=EditorEvtHdlr.NODES_ONLY;
		}
		else if (src==regionESelectBt){
		    application.setMode(EditorEvtHdlr.REGION_SELECTION_MODE);
		    application.eeh.selectWhat=EditorEvtHdlr.EDGES_ONLY;
		}
		else if (src==regionZoomBt){
		    application.setMode(EditorEvtHdlr.REGION_ZOOM_MODE);
		}
		else if (src==createResBt){
		    application.setMode(EditorEvtHdlr.CREATE_RESOURCE_MODE);
		}
		else if (src==createPropBt){
		    application.setMode(EditorEvtHdlr.CREATE_PREDICATE_MODE);
		    application.tblp.tabbedPane.setSelectedIndex(1);  //display the Property constructor Panel in table panel
		}
		else if (src==createLitBt){
		    application.setMode(EditorEvtHdlr.CREATE_LITERAL_MODE);
		}
		else if (src==resizeBt){
		    application.setMode(EditorEvtHdlr.MOVE_RESIZE_MODE);
		}
		else if (src==pasteBt){
		    application.setMode(EditorEvtHdlr.PASTE_MODE);
		}
		else if (src==commSingleBt){
		    application.setMode(EditorEvtHdlr.COMMENT_SINGLE_MODE);
		}
		else if (src==commRegionBt){
		    application.setMode(EditorEvtHdlr.COMMENT_REGION_MODE);
		}
		else if (src==uncommSingleBt){
		    application.setMode(EditorEvtHdlr.UNCOMMENT_SINGLE_MODE);
		}
		else if (src==uncommRegionBt){
		    application.setMode(EditorEvtHdlr.UNCOMMENT_REGION_MODE);
		}
		else if (src==searchBt){
		    application.quickSearch(searchTf.getText());
		}
	    }
	};

    ActionListener a2=new ActionListener(){
	    public void actionPerformed(ActionEvent e){
		Object src=e.getSource();
		if (src==undoMn || src==undoBt){
		    application.undo();
		}
		else if (src==selectAllNMn){
		    application.selectAllNodes();
		}
		else if (src==selectAllEMn){
		    application.selectAllEdges();
		}
		else if (src==advSelectMn){
		    new SelectionPanel(application);
		}
		else if (src==unselAllMn){
		    application.unselectAll();
		}
		else if (src==deleteMn){
		    application.deleteSelectedEntities();
		}
		else if (src==cutMn || src==cutBt){
		    application.cutSelection();
		}
		else if (src==copyMn || src==copyBt){
		    application.copySelection();
		}
		else if (src==pasteMn){
		    View v=Editor.vsm.getView(Editor.mainView);
		    application.pasteSelection(v.mouse.vx,v.mouse.vy);
		}
	    }
	};

//     void displayRawFile(Object o,String frameTitle){
// 	try {
// 	    if (o instanceof File){
// 		File f=(File)o;
// 		FileReader fr=new FileReader(f);
// 		char[] buf=new char[(int)f.length()];
// 		fr.read(buf);
// 		fr.close();
// 		StringBuffer sb=new StringBuffer();
// 		sb.append(buf);
// 		TextViewer v=new TextViewer(sb,frameTitle,0);
// 	    }
// 	    else if (o instanceof java.net.URL){
// 		java.net.URL u=(java.net.URL)o;
// 		BufferedReader in=new BufferedReader(new InputStreamReader(u.openStream()));
// 		StringBuffer sb=new StringBuffer();
// 		String s1;
// 		while ((s1=in.readLine())!=null){
// 		    sb.append(s1+"\n");
// 		}
// 		in.close();
// 		TextViewer v=new TextViewer(sb,frameTitle,0);
// 	    }
// 	}
// 	catch (Exception ex){System.err.println("Error: commandPanel.displayRawFile: "+ex);}
//     }

    /*enable/disable the icon/menu item associated to undo*/
    void enableUndo(boolean b){
	if (b!=undoBt.isEnabled()){
	    undoMn.setEnabled(b);
	    undoBt.setEnabled(b);
	}
    }

    /*enable/disable the icon/menu item associated to undo*/
    void enablePaste(boolean b){
	if (b!=pasteBt.isEnabled()){
	    pasteMn.setEnabled(b);
	    pasteBt.setEnabled(b);
	}
    }

    void buildConstraints(GridBagConstraints gbc, int gx,int gy,int gw,int gh,int wx,int wy){
	gbc.gridx=gx;
	gbc.gridy=gy;
	gbc.gridwidth=gw;
	gbc.gridheight=gh;
	gbc.weightx=wx;
	gbc.weighty=wy;
    }

}

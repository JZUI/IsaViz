/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 10/22/2001
 */


package org.w3c.IsaViz;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.File;

class PrefWindow extends JFrame implements ActionListener,KeyListener {

    JTabbedPane tabbedPane;

    JButton okPrefs,savePrefs;

    //directory panel
    JButton brw1,brw2,brw3,brw4,brw5;
    JTextField tf1,tf2,tf3,tf4,tf5;
    JCheckBox cb1;
    JRadioButton gr1,gr2; //graphviz version 1.7.6 or 1.7.11 and later

    //web browser panel
    JRadioButton detectBrowserBt,specifyBrowserBt;
    JTextField browserPathTf,browserOptsTf;
    JButton brw6,webHelpBt;
    JLabel pathLb,optLb;

    //proxy/firewall
    JCheckBox useProxyCb;
    JLabel proxyHostLb,proxyPortLb;
    JTextField proxyHostTf,proxyPortTf;
    JButton proxyHelpBt;
    
    //Misc prefs
    JTextField tf1a,tf1c;
    JTextField tf2a;
    JCheckBox cb1a,cb1b,cb1c;
    JRadioButton b1a,b2a;
    //JSpinner spinner;  //for number of characters displayed in literals
    JTextField spinner;  //use this instead (much more primitive) since JSpinner is only available since jdk 1.4 (and we want to be compatible with 1.3.x for now)
    JCheckBox saveWindowLayoutCb;
    JCheckBox dispAsLabelCb;
    
    //rendering panel
    JComboBox cbb;     //color scheme selector
    JCheckBox antialiascb; //set antialias rendering

    Editor application;

    PrefWindow(Editor e){
	application=e;

	tabbedPane = new JTabbedPane();

	//misc panel
	JPanel miscPane=new JPanel();
	GridBagLayout gridBag0=new GridBagLayout();
	GridBagConstraints constraints0=new GridBagConstraints();
	constraints0.fill=GridBagConstraints.HORIZONTAL;
	constraints0.anchor=GridBagConstraints.WEST;
	miscPane.setLayout(gridBag0);

	JLabel lb2=new JLabel("Graph Orientation");
	buildConstraints(constraints0,0,0,1,1,34,10);
	gridBag0.setConstraints(lb2,constraints0);
	miscPane.add(lb2);
	ButtonGroup bg1=new ButtonGroup();
	b1a=new JRadioButton("Horizontal");
	b2a=new JRadioButton("Vertical");
	bg1.add(b1a);
	bg1.add(b2a);
	if (Editor.GRAPH_ORIENTATION.equals("LR")){b1a.setSelected(true);} else {b2a.setSelected(true);}
	buildConstraints(constraints0,1,0,1,1,33,0);
	gridBag0.setConstraints(b1a,constraints0);
	miscPane.add(b1a);
	buildConstraints(constraints0,2,0,1,1,33,0);
	gridBag0.setConstraints(b2a,constraints0);
	miscPane.add(b2a);
	JLabel lb0=new JLabel("Default Namespace (without ':')");
	buildConstraints(constraints0,0,1,1,1,34,10);
	gridBag0.setConstraints(lb0,constraints0);
	miscPane.add(lb0);
	tf1a=new JTextField(Editor.DEFAULT_NAMESPACE.substring(0,Editor.DEFAULT_NAMESPACE.length()-1));
	buildConstraints(constraints0,1,1,2,1,66,0);//do not display ':' in the textfield (appended automatically)
	gridBag0.setConstraints(tf1a,constraints0);
	miscPane.add(tf1a);
	JLabel lb1=new JLabel("Anonymous Node Prefix (without ':')");
	buildConstraints(constraints0,0,2,1,1,34,10);
	gridBag0.setConstraints(lb1,constraints0);
	miscPane.add(lb1);
	tf2a=new JTextField(Editor.ANON_NODE.substring(0,Editor.ANON_NODE.length()-1));
	buildConstraints(constraints0,1,2,2,1,66,0);//do not display ':' in the textfield (appended automatically)
	gridBag0.setConstraints(tf2a,constraints0);
	miscPane.add(tf2a);
	cb1c=new JCheckBox("Always Include xml:lang in Literals - Default:");
	cb1c.setSelected(Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS);
	buildConstraints(constraints0,0,3,2,1,67,10);
	gridBag0.setConstraints(cb1c,constraints0);
	miscPane.add(cb1c);
	tf1c=new JTextField(Editor.DEFAULT_LANGUAGE_IN_LITERALS);
	buildConstraints(constraints0,2,3,1,1,33,0);//do not display ':' in the textfield (appended automatically)
	gridBag0.setConstraints(tf1c,constraints0);
	miscPane.add(tf1c);
	cb1a=new JCheckBox("Use Abbreviated RDF Syntax");
	cb1a.setSelected(Editor.ABBREV_SYNTAX);
	buildConstraints(constraints0,0,4,3,1,100,10);
	gridBag0.setConstraints(cb1a,constraints0);
	miscPane.add(cb1a);
	cb1b=new JCheckBox("Show Anonymous IDs");
	cb1b.setSelected(Editor.SHOW_ANON_ID);
	buildConstraints(constraints0,0,5,3,1,100,10);
	gridBag0.setConstraints(cb1b,constraints0);
	miscPane.add(cb1b);
	dispAsLabelCb=new JCheckBox("Display Label as Resource Text When Available",Editor.DISP_AS_LABEL);
	buildConstraints(constraints0,0,6,3,1,100,10);
	gridBag0.setConstraints(dispAsLabelCb,constraints0);
	miscPane.add(dispAsLabelCb);
	JLabel l47=new JLabel("Max. Nb. of Chars. Displayed in Literals");
	buildConstraints(constraints0,0,7,1,1,33,10);
	gridBag0.setConstraints(l47,constraints0);
	miscPane.add(l47);
// 	spinner=new JSpinner(new SpinnerNumberModel(Editor.MAX_LIT_CHAR_COUNT,0,80,1));
	spinner=new JTextField(String.valueOf(Editor.MAX_LIT_CHAR_COUNT)); //use this instead (much more primitive) since JSpinner is only available since jdk 1.4 (and we want to be compatible with 1.3.x for now)
	spinner.addKeyListener(this);
	buildConstraints(constraints0,1,7,2,1,66,0);
	gridBag0.setConstraints(spinner,constraints0);
	miscPane.add(spinner);
	saveWindowLayoutCb=new JCheckBox("Save/Restore Window Layout at Startup",Editor.SAVE_WINDOW_LAYOUT);
	buildConstraints(constraints0,0,8,3,1,100,10);
	gridBag0.setConstraints(saveWindowLayoutCb,constraints0);
	miscPane.add(saveWindowLayoutCb);
	tabbedPane.addTab("Misc.",miscPane);

	//directories panel
	JPanel dirPane=new JPanel();
	GridBagLayout gridBag=new GridBagLayout();
	GridBagConstraints constraints=new GridBagConstraints();
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.WEST;
	dirPane.setLayout(gridBag);
	JLabel l1=new JLabel("Temporary directory");
	buildConstraints(constraints,0,0,1,1,60,10);
	gridBag.setConstraints(l1,constraints);
	dirPane.add(l1);
	cb1=new JCheckBox("Delete temp files on exit");
	buildConstraints(constraints,1,0,1,1,30,0);
	gridBag.setConstraints(cb1,constraints);
	if (Editor.dltOnExit){cb1.setSelected(true);} else {cb1.setSelected(false);}
	cb1.addActionListener(this);
	dirPane.add(cb1);
	brw1=new JButton("Browse...");
	buildConstraints(constraints,2,0,1,1,10,0);
	gridBag.setConstraints(brw1,constraints);
	brw1.addActionListener(this);
	dirPane.add(brw1);
	tf1=new JTextField(Editor.m_TmpDir.toString());tf1.setEnabled(false);
	buildConstraints(constraints,0,1,3,1,100,10);
	gridBag.setConstraints(tf1,constraints);
	dirPane.add(tf1);
	JLabel l2=new JLabel("Project directory");
	buildConstraints(constraints,0,2,2,1,90,10);
	gridBag.setConstraints(l2,constraints);
	dirPane.add(l2);
	brw2=new JButton("Browse...");
	buildConstraints(constraints,2,2,1,1,10,0);
	gridBag.setConstraints(brw2,constraints);
	brw2.addActionListener(this);
	dirPane.add(brw2);
	tf2=new JTextField(Editor.projectDir.toString());tf2.setEnabled(false);
	buildConstraints(constraints,0,3,3,1,100,10);
	gridBag.setConstraints(tf2,constraints);
	dirPane.add(tf2);
	JLabel l3=new JLabel("RDF directory");
	buildConstraints(constraints,0,4,2,1,90,10);
	gridBag.setConstraints(l3,constraints);
	dirPane.add(l3);
	brw3=new JButton("Browse...");
	buildConstraints(constraints,2,4,1,1,10,0);
	gridBag.setConstraints(brw3,constraints);
	brw3.addActionListener(this);
	dirPane.add(brw3);
	tf3=new JTextField(Editor.rdfDir.toString());tf3.setEnabled(false);
	buildConstraints(constraints,0,5,3,1,100,10);
	gridBag.setConstraints(tf3,constraints);
	dirPane.add(tf3);
	JLabel l4=new JLabel("GraphViz DOT executable");
	buildConstraints(constraints,0,6,1,1,60,10);
	gridBag.setConstraints(l4,constraints);
	dirPane.add(l4);
	JPanel p1=new JPanel();
	p1.setLayout(new FlowLayout());
	ButtonGroup bg=new ButtonGroup();
	gr1=new JRadioButton("1.7.6");
	gr2=new JRadioButton("1.7.11 or later");
	bg.add(gr1);bg.add(gr2);
	p1.add(gr1);p1.add(gr2);
	if (Editor.GRAPHVIZ_VERSION==0){gr1.setSelected(true);}else {gr2.setSelected(true);}
	buildConstraints(constraints,1,6,1,1,30,0);
	gridBag.setConstraints(p1,constraints);
	dirPane.add(p1);
	brw4=new JButton("Browse...");
	buildConstraints(constraints,2,6,1,1,10,0);
	gridBag.setConstraints(brw4,constraints);
	brw4.addActionListener(this);
	dirPane.add(brw4);
	tf4=new JTextField(Editor.m_GraphVizPath.toString());tf4.setEnabled(false);
	buildConstraints(constraints,0,7,3,1,100,10);
	gridBag.setConstraints(tf4,constraints);
	dirPane.add(tf4);
	JLabel l5=new JLabel("GraphViz font directory");
	buildConstraints(constraints,0,8,2,1,90,10);
	gridBag.setConstraints(l5,constraints);
	dirPane.add(l5);
	brw5=new JButton("Browse...");
	buildConstraints(constraints,2,8,1,1,10,0);
	gridBag.setConstraints(brw5,constraints);
	brw5.addActionListener(this);
	dirPane.add(brw5);
	tf5=new JTextField(Editor.m_GraphVizFontDir.toString());tf5.setEnabled(false);
	buildConstraints(constraints,0,9,3,1,100,10);
	gridBag.setConstraints(tf5,constraints);
	dirPane.add(tf5);
	tabbedPane.addTab("Directories",dirPane);

	//web browser panel
	JPanel webPane=new JPanel();
	GridBagLayout gridBag2=new GridBagLayout();
	GridBagConstraints constraints2=new GridBagConstraints();
	constraints2.fill=GridBagConstraints.HORIZONTAL;
	constraints2.anchor=GridBagConstraints.WEST;
	webPane.setLayout(gridBag2);
	ButtonGroup bg2=new ButtonGroup();
	detectBrowserBt=new JRadioButton("Automatically Detect Default Browser");
	buildConstraints(constraints2,0,0,3,1,100,1);
	gridBag2.setConstraints(detectBrowserBt,constraints2);
	detectBrowserBt.addActionListener(this);
	bg2.add(detectBrowserBt);
	webPane.add(detectBrowserBt);
	specifyBrowserBt=new JRadioButton("Specify Browser");
	buildConstraints(constraints2,0,1,3,1,100,1);
	gridBag2.setConstraints(specifyBrowserBt,constraints2);
	specifyBrowserBt.addActionListener(this);
	bg2.add(specifyBrowserBt);
	webPane.add(specifyBrowserBt);
	JPanel p7=new JPanel();
	buildConstraints(constraints2,0,2,1,1,10,1);
	gridBag2.setConstraints(p7,constraints2);
	webPane.add(p7);
	pathLb=new JLabel("Path");
	buildConstraints(constraints2,1,2,1,1,80,0);
	gridBag2.setConstraints(pathLb,constraints2);
	webPane.add(pathLb);
	brw6=new JButton("Browse...");
	buildConstraints(constraints2,2,2,1,1,10,0);
	gridBag2.setConstraints(brw6,constraints2);
	brw6.addActionListener(this);
	webPane.add(brw6);
	browserPathTf=new JTextField(Editor.browserPath.toString());
	buildConstraints(constraints2,1,3,2,1,90,1);
	gridBag2.setConstraints(browserPathTf,constraints2);
	webPane.add(browserPathTf);
	optLb=new JLabel("Command Line Options");
	buildConstraints(constraints2,1,4,2,1,90,1);
	gridBag2.setConstraints(optLb,constraints2);
	webPane.add(optLb);
	browserOptsTf=new JTextField(Editor.browserOptions);
	buildConstraints(constraints2,1,5,2,1,90,1);
	gridBag2.setConstraints(browserOptsTf,constraints2);
	webPane.add(browserOptsTf);
	//fill out empty space
	JPanel p8=new JPanel();
	buildConstraints(constraints2,0,6,3,1,100,92);
	gridBag2.setConstraints(p8,constraints2);
	webPane.add(p8);
	webHelpBt=new JButton("Help");
	buildConstraints(constraints2,2,7,1,1,10,1);
	gridBag2.setConstraints(webHelpBt,constraints2);
	webHelpBt.addActionListener(this);
	webPane.add(webHelpBt);
	if (Editor.autoDetectBrowser){detectBrowserBt.doClick();} //select and fire event
	else {specifyBrowserBt.doClick();} //so that fields get enabled/disabled as is approriate
	tabbedPane.addTab("Web Browser",webPane);

	//proxy panel
	JPanel proxyPane=new JPanel();
	GridBagLayout gridBag5=new GridBagLayout();
	GridBagConstraints constraints5=new GridBagConstraints();
	constraints5.fill=GridBagConstraints.HORIZONTAL;
	constraints5.anchor=GridBagConstraints.WEST;
	proxyPane.setLayout(gridBag5);
	useProxyCb=new JCheckBox("Use Proxy Server");
	buildConstraints(constraints5,0,0,2,1,100,1);
	gridBag5.setConstraints(useProxyCb,constraints5);
	useProxyCb.setSelected(Editor.useProxy);
	useProxyCb.addActionListener(this);
	proxyPane.add(useProxyCb);
	proxyHostLb=new JLabel("Hostname:");
	proxyHostLb.setEnabled(Editor.useProxy);
	buildConstraints(constraints5,0,1,1,1,80,1);
	gridBag5.setConstraints(proxyHostLb,constraints5);
	proxyPane.add(proxyHostLb);
	proxyPortLb=new JLabel("Port:");
	proxyPortLb.setEnabled(Editor.useProxy);
	buildConstraints(constraints5,1,1,1,1,20,1);
	gridBag5.setConstraints(proxyPortLb,constraints5);
	proxyPane.add(proxyPortLb);
	proxyHostTf=new JTextField(Editor.proxyHost);
	proxyHostTf.setEnabled(Editor.useProxy);
	buildConstraints(constraints5,0,2,1,1,80,1);
	gridBag5.setConstraints(proxyHostTf,constraints5);
	proxyPane.add(proxyHostTf);
	proxyPortTf=new JTextField(Editor.proxyPort);
	proxyPortTf.setEnabled(Editor.useProxy);
	buildConstraints(constraints5,1,2,1,1,20,1);
	gridBag5.setConstraints(proxyPortTf,constraints5);
	proxyPane.add(proxyPortTf);

	constraints5.fill=GridBagConstraints.BOTH;
	constraints5.anchor=GridBagConstraints.CENTER;
	//fill out empty space
	JPanel p1000=new JPanel();
	buildConstraints(constraints5,0,3,2,1,100,95);
	gridBag5.setConstraints(p1000,constraints5);
	proxyPane.add(p1000);
	constraints5.fill=GridBagConstraints.NONE;
	constraints5.anchor=GridBagConstraints.EAST;
	proxyHelpBt=new JButton("Help");
	buildConstraints(constraints5,1,4,1,1,20,1);
	gridBag5.setConstraints(proxyHelpBt,constraints5);
	proxyHelpBt.addActionListener(this);
	proxyPane.add(proxyHelpBt);
	tabbedPane.addTab("Proxy",proxyPane);

	//rendering panel
	JPanel renderPane=new JPanel();
	GridBagLayout gridBag4=new GridBagLayout();
	GridBagConstraints constraints4=new GridBagConstraints();
	constraints4.fill=GridBagConstraints.HORIZONTAL;
	constraints4.anchor=GridBagConstraints.WEST;
	renderPane.setLayout(gridBag4);
	JLabel lb3=new JLabel("Color scheme");
	buildConstraints(constraints4,0,0,1,1,33,10);
	gridBag4.setConstraints(lb3,constraints4);
	renderPane.add(lb3);
	java.util.Vector colorSchemes=new java.util.Vector();
	colorSchemes.add("default");colorSchemes.add("b&w");
	cbb=new JComboBox(colorSchemes);
	cbb.setMaximumRowCount(2);
	cbb.setSelectedItem(ConfigManager.COLOR_SCHEME);
	buildConstraints(constraints4,1,0,1,1,66,0);
	gridBag4.setConstraints(cbb,constraints4);
	renderPane.add(cbb);
	antialiascb=new JCheckBox("Antialiasing",Editor.ANTIALIASING);
	antialiascb.addActionListener(this);
	buildConstraints(constraints4,0,1,2,1,100,10);
	gridBag4.setConstraints(antialiascb,constraints4);
	renderPane.add(antialiascb);
	JPanel p51=new JPanel();
	buildConstraints(constraints4,0,2,2,1,100,80);
	gridBag4.setConstraints(p51,constraints4);
	renderPane.add(p51);
	tabbedPane.addTab("Rendering",renderPane);

	//main panel (tabbed panes + OK/Save buttons)
	Container cpane=this.getContentPane();
	GridBagLayout gridBag3=new GridBagLayout();
	GridBagConstraints constraints3=new GridBagConstraints();
	constraints3.fill=GridBagConstraints.BOTH;
	constraints3.anchor=GridBagConstraints.WEST;
	cpane.setLayout(gridBag3);
	buildConstraints(constraints3,0,0,3,1,100,90);
	gridBag3.setConstraints(tabbedPane,constraints3);
	cpane.add(tabbedPane);
	JPanel tmp=new JPanel();
	buildConstraints(constraints3,0,1,1,1,70,10);
	gridBag3.setConstraints(tmp,constraints3);
	cpane.add(tmp);
	constraints3.fill=GridBagConstraints.HORIZONTAL;
	constraints3.anchor=GridBagConstraints.CENTER;
	okPrefs=new JButton("Apply & Close");
	//okPrefs.setPreferredSize(new Dimension(60,25));
	buildConstraints(constraints3,1,1,1,1,15,10);
	gridBag3.setConstraints(okPrefs,constraints3);
	okPrefs.addActionListener(this);
	cpane.add(okPrefs);
	constraints3.fill=GridBagConstraints.HORIZONTAL;
	constraints3.anchor=GridBagConstraints.CENTER;
	savePrefs=new JButton("Save");
	//savePrefs.setPreferredSize(new Dimension(60,35));
	buildConstraints(constraints3,2,1,1,1,15,10);
	gridBag3.setConstraints(savePrefs,constraints3);
	savePrefs.addActionListener(this);
	cpane.add(savePrefs);

	tabbedPane.setSelectedIndex(0);
	//window
	WindowListener w0=new WindowAdapter(){
		public void windowClosing(WindowEvent e){}
	    };
	this.addWindowListener(w0);
	this.setTitle("Preferences");
	this.pack();
	this.setLocation(0,0);
	this.setSize(400,300);
	this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
	JFileChooser fc;
	int returnVal;
	Object o=e.getSource();
	if (o==brw1){//tmp directory browse button
	    fc=new JFileChooser(Editor.m_TmpDir);
 	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //does not work well with JVM 1.3.x (works fine in 1.4)
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.m_TmpDir=fc.getSelectedFile();
		tf1.setText(Editor.m_TmpDir.toString());
	    }
	}
	else if (o==brw2){
	    fc=new JFileChooser(Editor.projectDir);
 	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //does not work well with JVM 1.3.x (works fine in 1.4)
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.projectDir=fc.getSelectedFile();
		tf2.setText(Editor.projectDir.toString());
	    }
	}
	else if (o==brw3){
	    fc=new JFileChooser(Editor.rdfDir);
 	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //does not work well with JVM 1.3.x (works fine in 1.4)
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.rdfDir=fc.getSelectedFile();
		tf3.setText(Editor.rdfDir.toString());
	    }
	}
	else if (o==brw4){
	    fc=new JFileChooser(Editor.m_GraphVizPath);
	    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.m_GraphVizPath=fc.getSelectedFile();
		tf4.setText(Editor.m_GraphVizPath.toString());
	    }
	}
	else if (o==brw5){
	    fc=new JFileChooser(Editor.m_GraphVizFontDir);
 	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //does not work well with JVM 1.3.x (works fine in 1.4)
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.m_GraphVizFontDir=fc.getSelectedFile();
		tf5.setText(Editor.m_GraphVizFontDir.toString());
	    }
	}
	else if (o==cb1){
	    if (cb1.isSelected()){Editor.dltOnExit=true;}
	    else {Editor.dltOnExit=false;}
	}
	else if (o==detectBrowserBt){
	    if (detectBrowserBt.isSelected()){//automatically detect browser
		Editor.autoDetectBrowser=true;
		browserPathTf.setEnabled(false);
		brw6.setEnabled(false);
		browserOptsTf.setEnabled(false);
		pathLb.setEnabled(false);
		optLb.setEnabled(false);
	    }
	}
	else if (o==specifyBrowserBt){
	    if (specifyBrowserBt.isSelected()){//specify browser
		Editor.autoDetectBrowser=false;
		browserPathTf.setEnabled(true);
		brw6.setEnabled(true);
		browserOptsTf.setEnabled(true);
		pathLb.setEnabled(true);
		optLb.setEnabled(true);
	    }
	}
	else if (o==brw6){
	    fc=new JFileChooser(Editor.browserPath);
	    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    returnVal= fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION){
		Editor.browserPath=fc.getSelectedFile();
		browserPathTf.setText(Editor.browserPath.toString());
	    }
	}
	else if (o==webHelpBt){
	    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	    TextViewer help=new TextViewer(new StringBuffer(Messages.webBrowserHelpText),"Web Browser Configuration",0,(screenSize.width-400)/2,(screenSize.height-300)/2,400,300);
	}
	else if (o==useProxyCb){
	    proxyHostLb.setEnabled(useProxyCb.isSelected());
	    proxyPortLb.setEnabled(useProxyCb.isSelected());
	    proxyHostTf.setEnabled(useProxyCb.isSelected());
	    proxyPortTf.setEnabled(useProxyCb.isSelected());
	}
	else if (o==proxyHelpBt){
	    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	    TextViewer help=new TextViewer(new StringBuffer(Messages.proxyHelpText),"Proxy Configuration",0,(screenSize.width-400)/2,(screenSize.height-300)/2,400,300);
	}
	else if (o==okPrefs){updateVars();this.dispose();}
	else if (o==savePrefs){updateVars();application.saveConfig();}
	else if (o==antialiascb){
	    if (antialiascb.isSelected()){javax.swing.JOptionPane.showMessageDialog(this,Messages.antialiasingWarning);}
	    application.setAntialiasing(antialiascb.isSelected());
	}
    }

    public void keyPressed(KeyEvent e){//only need this because we could not implement JSpinner ()
	if (e.getKeyCode()==KeyEvent.VK_ENTER && e.getSource()==spinner){
	    if (!Utils.isPositiveInteger(spinner.getText())){javax.swing.JOptionPane.showMessageDialog(this,spinner.getText()+" is not a valid number.");spinner.setText(String.valueOf(Editor.MAX_LIT_CHAR_COUNT));}
	}
    }
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}

    void updateVars(){
	if (gr2.isSelected()){Editor.GRAPHVIZ_VERSION=1;} //means GraphViz 1.7.11 or later is used
	else {Editor.GRAPHVIZ_VERSION=0;} //means GraphViz 1.7.6 is used
	Editor.DEFAULT_NAMESPACE=tf1a.getText()+":"; //':' is automatically appended to the user's choice
	Editor.ANON_NODE=tf2a.getText()+":";//since it is the separator between prefix and ID
	Editor.ALWAYS_INCLUDE_LANG_IN_LITERALS=cb1c.isSelected();
	Editor.DEFAULT_LANGUAGE_IN_LITERALS=tf1c.getText();
	application.setAbbrevSyntax(cb1a.isSelected());
	Editor.SAVE_WINDOW_LAYOUT=saveWindowLayoutCb.isSelected();
	if (Editor.SHOW_ANON_ID!=cb1b.isSelected()){application.showAnonIds(cb1b.isSelected());}
	if (Editor.DISP_AS_LABEL!=dispAsLabelCb.isSelected()){application.displayLabels(dispAsLabelCb.isSelected());}
// 	application.setMaxLiteralCharCount(((Integer)spinner.getValue()).intValue());
	try {
	    application.setMaxLiteralCharCount((new Integer(spinner.getText())).intValue());//use this instead (much more primitive) since JSpinner is only available since jdk 1.4 (and we want to be compatible with 1.3.x for now)
	}
	catch (NumberFormatException ex){javax.swing.JOptionPane.showMessageDialog(this,spinner.getText()+" is not a valid number.");}//if there is an error, signal it and keep old value (should have been signaled to user anyway)
	if (b1a.isSelected()){Editor.GRAPH_ORIENTATION="LR";} else {Editor.GRAPH_ORIENTATION="TB";}
	ConfigManager.assignColorsToGraph((String)cbb.getSelectedItem());
	Editor.browserPath=new File(browserPathTf.getText());
	Editor.browserOptions=browserOptsTf.getText();
	ConfigManager.updateProxy(useProxyCb.isSelected(),proxyHostTf.getText(),proxyPortTf.getText());
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

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 11/27/2001
 */



package org.w3c.IsaViz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.Vector;

/*a panel displaying namespace bindings and property constructors (property types that can be selected to instantiate a property), in 2 different tabbed panes.*/

class TablePanel extends JFrame implements ActionListener,KeyListener,MouseListener,ChangeListener {

    Editor application;

    JTabbedPane tabbedPane;
    JScrollPane sp1,sp2;  //scrollpanes for nsTable and prTable

    //namespace bindings table
    JTable nsTable;
    DefaultTableModel nsTableModel;
    JButton addNSBt,remNSBt;
    JTextField nsPrefTf,nsURITf;

    //property constructors table
    JTable prTable;
    JButton addPRBt,remPRBt,loadPRBt;
    JTextField nsPrpTf,lnPrpTf;
    
    //resource outgoing predicates browser
    JPanel rsPane,outerRsPane;
    JLabel resourceLb,bckBt;
    
    IResource[] browserList=new IResource[Editor.MAX_BRW_LIST_SIZE];
    int brwIndex=0;


    TablePanel(Editor e,int x,int y,int width,int height){
	application=e;

	tabbedPane = new JTabbedPane();
	tabbedPane.addChangeListener(this);

	//panel for namespace definitions
	JPanel nsPane=new JPanel();
	GridBagLayout gridBag1=new GridBagLayout();
	GridBagConstraints constraints1=new GridBagConstraints();
	nsPane.setLayout(gridBag1);
	nsTableModel=new NSTableModel(0,3);
	nsTableModel.addTableModelListener(l1);
	nsTable=new JTable(nsTableModel);
	nsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
// 	nsTable.setCellSelectionEnabled(true);
	TableColumn tc=nsTable.getColumnModel().getColumn(0);
	tc.setPreferredWidth(width/100*20);tc.setHeaderValue("Prefix");
	tc=nsTable.getColumnModel().getColumn(1);
	tc.setPreferredWidth(width/100*70);tc.setHeaderValue("URI");
	tc=nsTable.getColumnModel().getColumn(2);
	tc.setPreferredWidth(width/100*10);tc.setHeaderValue("Display Prefix");
	//display 3rd column as checkbox (it is a boolean)
	TableCellRenderer tcr=nsTable.getDefaultRenderer(Boolean.class);
	tc.setCellRenderer(tcr);
	TableCellEditor tce=nsTable.getDefaultEditor(Boolean.class);
	tc.setCellEditor(tce);
	sp1=new JScrollPane(nsTable);
	sp1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	sp1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	constraints1.fill=GridBagConstraints.BOTH;
	constraints1.anchor=GridBagConstraints.CENTER;
	buildConstraints(constraints1,0,0,5,1,100,99);
	gridBag1.setConstraints(sp1,constraints1);
	nsPane.add(sp1);
	constraints1.fill=GridBagConstraints.HORIZONTAL;
	constraints1.anchor=GridBagConstraints.WEST;
	nsPrefTf=new JTextField();
	nsPrefTf.addKeyListener(this);
	buildConstraints(constraints1,0,1,1,1,25,1);
	gridBag1.setConstraints(nsPrefTf,constraints1);
	nsPane.add(nsPrefTf);
	nsURITf=new JTextField();
	nsURITf.addKeyListener(this);
	buildConstraints(constraints1,1,1,1,1,75,0);
	gridBag1.setConstraints(nsURITf,constraints1);
	nsPane.add(nsURITf);
	constraints1.fill=GridBagConstraints.NONE;
	addNSBt=new JButton("Add NS Binding");addNSBt.setFont(Editor.tinyFont);
	addNSBt.addActionListener(this);addNSBt.addKeyListener(this);
	buildConstraints(constraints1,2,1,1,1,7,0);
	gridBag1.setConstraints(addNSBt,constraints1);
	nsPane.add(addNSBt);
	constraints1.anchor=GridBagConstraints.CENTER;
	remNSBt=new JButton("Remove Selected");remNSBt.setFont(Editor.tinyFont);
	remNSBt.addActionListener(this);remNSBt.addKeyListener(this);
	buildConstraints(constraints1,3,1,1,1,6,0);
	gridBag1.setConstraints(remNSBt,constraints1);
	nsPane.add(remNSBt);
	tabbedPane.addTab("Namespaces",nsPane);


	//panel for properties
	JPanel prPane=new JPanel();
	GridBagLayout gridBag2=new GridBagLayout();
	GridBagConstraints constraints2=new GridBagConstraints();
	prPane.setLayout(gridBag2);
	DefaultTableModel prTbModel=new PRTableModel(0,3);
	prTbModel.addTableModelListener(l2);
	prTable=new JTable(prTbModel);
	prTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
// 	prTable.setCellSelectionEnabled(true);
	TableColumn tc2=prTable.getColumnModel().getColumn(0);
	tc2.setPreferredWidth(width/100*50);tc2.setHeaderValue("Namespace");
	tc2=prTable.getColumnModel().getColumn(1);
	tc2.setPreferredWidth(width/100*15);tc2.setHeaderValue("Prefix");
	tc2=prTable.getColumnModel().getColumn(2);
	tc2.setPreferredWidth(width/100*35);tc2.setHeaderValue("Property name");
	sp2=new JScrollPane(prTable);
	sp2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	sp2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	constraints2.fill=GridBagConstraints.BOTH;
	constraints2.anchor=GridBagConstraints.CENTER;
	buildConstraints(constraints2,0,0,5,1,100,99);
	gridBag2.setConstraints(sp2,constraints2);
	prPane.add(sp2);
	constraints2.fill=GridBagConstraints.HORIZONTAL;
	constraints2.anchor=GridBagConstraints.WEST;
	nsPrpTf=new JTextField();
	nsPrpTf.addKeyListener(this);
	buildConstraints(constraints2,0,1,1,1,40,1);
	gridBag2.setConstraints(nsPrpTf,constraints2);
	prPane.add(nsPrpTf);
	lnPrpTf=new JTextField();
	lnPrpTf.addKeyListener(this);
	buildConstraints(constraints2,1,1,1,1,40,0);
	gridBag2.setConstraints(lnPrpTf,constraints2);
	prPane.add(lnPrpTf);
	constraints2.fill=GridBagConstraints.NONE;
	addPRBt=new JButton("Add Property");addPRBt.setFont(Editor.tinyFont);
	addPRBt.addActionListener(this);addPRBt.addKeyListener(this);
	buildConstraints(constraints2,2,1,1,1,20,0);
	gridBag2.setConstraints(addPRBt,constraints2);
	prPane.add(addPRBt);
	constraints2.anchor=GridBagConstraints.WEST;
	remPRBt=new JButton("Remove Selected");remPRBt.setFont(Editor.tinyFont);
	remPRBt.addActionListener(this);remPRBt.addKeyListener(this);
	buildConstraints(constraints2,3,1,1,1,10,0);
	gridBag2.setConstraints(remPRBt,constraints2);
	prPane.add(remPRBt);
	constraints2.anchor=GridBagConstraints.EAST;
	loadPRBt=new JButton("Load Properties...");loadPRBt.setFont(Editor.tinyFont);
	loadPRBt.addActionListener(this);loadPRBt.addKeyListener(this);
	buildConstraints(constraints2,4,1,1,1,10,0);
	gridBag2.setConstraints(loadPRBt,constraints2);
	prPane.add(loadPRBt);
	tabbedPane.addTab("Property Types",prPane);

	ListSelectionModel rowSM=prTable.getSelectionModel();
	rowSM.addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    if (e.getValueIsAdjusting()) return;
		    ListSelectionModel lsm=(ListSelectionModel)e.getSource();
		    if (!lsm.isSelectionEmpty()){
			int selectedRow = lsm.getMinSelectionIndex();
			application.setSelectedPropertyConstructor((String)prTable.getModel().getValueAt(selectedRow,0),(String)prTable.getModel().getValueAt(selectedRow,2));
		    }
		}
	    });

	//property browser panel
	outerRsPane=new JPanel();
	GridBagLayout gridBag3=new GridBagLayout();
	GridBagConstraints constraints3=new GridBagConstraints();
	outerRsPane.setLayout(gridBag3);

	constraints3.fill=GridBagConstraints.HORIZONTAL;
	constraints3.anchor=GridBagConstraints.WEST;
	resourceLb=new JLabel();
	buildConstraints(constraints3,0,0,1,1,50,1);
	gridBag3.setConstraints(resourceLb,constraints3);
	outerRsPane.add(resourceLb);
	constraints3.fill=GridBagConstraints.NONE;
	constraints3.anchor=GridBagConstraints.EAST;
	bckBt=new JLabel("Back");
	buildConstraints(constraints3,1,0,1,1,50,1);
	gridBag3.setConstraints(bckBt,constraints3);
	bckBt.addMouseListener(this);
	outerRsPane.add(bckBt);
	rsPane=new JPanel();
	JScrollPane sp3=new JScrollPane(rsPane);
	sp3.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	sp3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	sp3.getVerticalScrollBar().setUnitIncrement(5);
	constraints3.fill=GridBagConstraints.BOTH;
	constraints3.anchor=GridBagConstraints.CENTER;
	buildConstraints(constraints3,0,1,2,1,100,99);
	gridBag3.setConstraints(sp3,constraints3);
	outerRsPane.add(sp3);
	tabbedPane.addTab("Property Browser",outerRsPane);

	Container cpane=this.getContentPane();
	cpane.add(tabbedPane);	
	//window
	WindowListener w0=new WindowAdapter(){
		public void windowClosing(WindowEvent e){application.cmp.showTablesMn.setSelected(false);}
	    };
	this.addWindowListener(w0);
	this.setTitle("Definitions");
	this.pack();
	this.setLocation(x,y);
	this.setSize(width,height);
// 	this.setVisible(true);
    }

    void updatePropertyBrowser(INode n,boolean insert){
	rsPane.removeAll();
	if (n!=null && n instanceof IResource){
	    IResource r=(IResource)n;
	    if (insert){
		int index=Utils.getFirstEmptyIndex(browserList);
		if (index==-1){
		    Utils.eraseFirstAddNewElem(browserList,r);
		    brwIndex=browserList.length-1;
		}
		else {
		    browserList[index]=r;
		    brwIndex=index;
		}
	    }
	    String subjectLabel=r.getIdent();
	    if (r.getLabel()!=null){subjectLabel+=" ("+r.getLabel()+")";}
	    resourceLb.setText(subjectLabel);
	    resourceLb.setForeground(ConfigManager.darkerPastelBlue);
	    Vector v;
	    if ((v=r.getOutgoingPredicates())!=null){
		GridBagLayout gridBag=new GridBagLayout();
		GridBagConstraints constraints=new GridBagConstraints();
		constraints.fill=GridBagConstraints.HORIZONTAL;
		constraints.anchor=GridBagConstraints.WEST;
		rsPane.setLayout(gridBag);
		int gridIndexH=0;
		int gridIndexV=0;
		int spanH=1;
		int spanV=1;
		int ratioH=50;
		int ratioV=100/v.size();
		IProperty p;
		JLabel propertyLabel;
		Component objectComp;
		String prefix;
		for (int j=0;j<v.size();j++){
		    p=(IProperty)v.elementAt(j);
		    prefix=application.getNSBinding(p.getNamespace());
		    propertyLabel=new JLabel(prefix!=null ? prefix+":"+p.getLocalname() : p.getIdent());
		    buildConstraints(constraints,gridIndexH,gridIndexV,spanH,spanV,ratioH,1);
		    gridBag.setConstraints(propertyLabel,constraints);
		    rsPane.add(propertyLabel);
		    gridIndexH++;
		    objectComp=this.getSwingRepresentation(p.object);
		    buildConstraints(constraints,gridIndexH,gridIndexV,spanH,spanV,ratioH,0);
		    gridBag.setConstraints(objectComp,constraints);
		    rsPane.add(objectComp);
		    gridIndexH=0;
		    gridIndexV++;
		}
		JLabel lb=new JLabel();//add a blank label to push all entries upward in the window (this label is not displayed
		buildConstraints(constraints,gridIndexH,gridIndexV,2,1,100,99);//even if we use a vertical scrollbar)
		gridBag.setConstraints(lb,constraints);
		rsPane.add(lb);
	    }
	    else {
		rsPane.add(new JLabel("No property is associated to this resource."));
	    }
	}
	else {resourceLb.setText("");}
	outerRsPane.paintAll(outerRsPane.getGraphics());
    }

    public void actionPerformed(ActionEvent e){
	Object src=e.getSource();
	if (src==addNSBt){
	    checkAndAddNS(nsPrefTf.getText(),nsURITf.getText());
	}
	else if (src==addPRBt){
	    checkAndAddProperty(nsPrpTf.getText(),lnPrpTf.getText());
	}
	else if (src==remNSBt){
	    int row;
	    if ((row=nsTable.getSelectedRow())!=-1){application.removeNamespaceBinding(row);}
	}
	else if (src==remPRBt){
	    int row;
	    if ((row=prTable.getSelectedRow())!=-1){application.removePropertyConstructor(row);}
	}
	else if (src==loadPRBt){
	    JFileChooser fc = new JFileChooser(Editor.lastImportRDFDir!=null ? Editor.lastImportRDFDir : Editor.rdfDir);
	    int returnVal=fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		application.loadPropertyTypes(fc.getSelectedFile());
	    }
	}
    }

    public void keyPressed(KeyEvent e){
	if (e.getKeyCode()==KeyEvent.VK_ENTER){
	    Object src=e.getSource();
	    if ((src==addNSBt) || (src==nsPrefTf) || (src==nsURITf)){
		checkAndAddNS(nsPrefTf.getText(),nsURITf.getText());
	    }
	    else if ((src==addPRBt) || (src==nsPrpTf) || (src==lnPrpTf)){
		checkAndAddProperty(nsPrpTf.getText(),lnPrpTf.getText());
	    }
	    else if (src==remNSBt){
		int row;
		if ((row=nsTable.getSelectedRow())!=-1){application.removeNamespaceBinding(row);}
	    }
	    else if (src==remPRBt){
		int row;
		if ((row=prTable.getSelectedRow())!=-1){application.removePropertyConstructor(row);}
	    }
	}
    }

    public Component getSwingRepresentation(INode n){
	if (n instanceof IResource){
	    final IResource r=(IResource)n;
	    String s;
	    if (r.isAnon()){
		s="(AR) ";
		if (Editor.SHOW_ANON_ID){s+=r.getIdent();}
	    }
	    else {s="(R) "+r.getIdent();}
	    final JLabel res=new JLabel(s);
	    MouseListener m1=new MouseAdapter(){
		    public void mousePressed(MouseEvent e){
			int whichBt=e.getModifiers();
			if ((whichBt & InputEvent.BUTTON1_MASK)==InputEvent.BUTTON1_MASK){
			    updatePropertyBrowser(r,true);
			}
			else if ((whichBt & InputEvent.BUTTON3_MASK)==InputEvent.BUTTON3_MASK){
			    Editor.vsm.centerOnGlyph(r.getGlyph(),Editor.vsm.getActiveCamera(),500);
			}
		    }
		    public void mouseReleased(MouseEvent e){}
		    public void mouseClicked(MouseEvent e){}
		    public void mouseEntered(MouseEvent e){res.setForeground(ConfigManager.darkerPastelBlue);}
		    public void mouseExited(MouseEvent e){res.setForeground(Color.black);}
		};
	    res.addMouseListener(m1);
	    return res;
	}
	else if (n instanceof ILiteral){
	    final ILiteral l=(ILiteral)n;
	    final JLabel res=new JLabel("(L) "+l.getValue());
	    MouseListener m2=new MouseAdapter(){
		    public void mousePressed(MouseEvent e){
			int whichBt=e.getModifiers();
			if ((whichBt & InputEvent.BUTTON3_MASK)==InputEvent.BUTTON3_MASK){
			    Editor.vsm.centerOnGlyph(l.getGlyph(),Editor.vsm.getActiveCamera(),500);
			}
		    }
		    public void mouseReleased(MouseEvent e){}
		    public void mouseClicked(MouseEvent e){}
		    public void mouseEntered(MouseEvent e){res.setForeground(ConfigManager.darkerPastelBlue);}
		    public void mouseExited(MouseEvent e){res.setForeground(Color.black);}
		};
	    res.addMouseListener(m2);
	    return res;
	}
	else {
	    return new JLabel("Unknown kind of object - unable to display "+n.toString());
	}
    }

    public void mousePressed(MouseEvent e){
	int whichBt=e.getModifiers();
	if (e.getSource()==bckBt){//pressing Back button in property browser
	    if ((whichBt & InputEvent.BUTTON1_MASK)==InputEvent.BUTTON1_MASK){
		if (brwIndex>0){browserList[brwIndex]=null;brwIndex--;updatePropertyBrowser(browserList[brwIndex],false);}
	    }
	}
    }
    public void mouseReleased(MouseEvent e){}
    public void mouseClicked(MouseEvent e){}
    //only JLabel serving as buttons are registered to this mouse listener - if this changes in the future, the automatic casting will not work
    public void mouseEntered(MouseEvent e){((JLabel)e.getSource()).setForeground(ConfigManager.darkerPastelBlue);}
    public void mouseExited(MouseEvent e){((JLabel)e.getSource()).setForeground(Color.black);}

    public void stateChanged(ChangeEvent e) {
	if (tabbedPane.getSelectedIndex()==2){
	    updatePropertyBrowser(Editor.lastSelectedItem,true);
	}
    }
    
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}

    void resetNamespaceTable(){
	for (int i=nsTableModel.getRowCount()-1;i>=0;i--){
	    nsTableModel.removeRow(i);
	}
    }

    void resetPropertyTable(){
	for (int i=((DefaultTableModel)prTable.getModel()).getRowCount()-1;i>=0;i--){
	    ((DefaultTableModel)prTable.getModel()).removeRow(i);
	}
    }
    
    void resetBrowser(){
	Utils.resetArray(browserList);
	updatePropertyBrowser(null,false);
    }

    void checkAndAddNS(String prefix,String uri){//prefix can be "" (no binding assigned), but URI has to be non-null
	if (uri.length()>0){
	    String prefix2="";
	    if (prefix.length()>0){//get rid of ':' if entered by user in the text field
		prefix2=prefix.endsWith(":") ? prefix.substring(0,prefix.length()-1) : prefix;
	    }
	    boolean success=application.addNamespaceBinding(prefix2,uri,new Boolean(false),false,false);
	    if (success){//empty fields only if the addition succeeded
		nsPrefTf.setText("");nsURITf.setText("");
		nsPrefTf.requestFocus();
	    }
	}
    }

    void checkAndAddProperty(String ns,String ln){
	if (application.addPropertyType(ns,ln,false)){//empty fields only if the addition succeeded
	    nsPrpTf.setText("");lnPrpTf.setText("");
	    nsPrpTf.requestFocus();
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


    TableModelListener l1=new TableModelListener(){//listener for the namespace table
	    public void tableChanged(TableModelEvent e){
		if (e.getType()!=TableModelEvent.DELETE){//can be INSERT or UPDATE
		    int row=e.getFirstRow();
		    int column=e.getColumn();
		    application.updateNamespaceBinding(row,column,(String)nsTableModel.getValueAt(row,0),(String)nsTableModel.getValueAt(row,1),(Boolean)nsTableModel.getValueAt(row,2),e.getType());
		}
		//do not do anything if DELETE (there's nothing to update - we are taking care of it in resetNamespaceDefinition() or removeNamespaceDefinition())
	    }
	};

    TableModelListener l2=new TableModelListener(){//listener for the property table
	    public void tableChanged(TableModelEvent e){
		if (e.getType()!=TableModelEvent.DELETE){//could also be INSERT or UPDATE
// 		    int row=e.getFirstRow();
// 		    int column=e.getColumn();
		    
		}
		//do not do anything is DELETE (there's nothing to update - we are taking care of it in resetNamespaceDefinition() or removeNamespaceDefinition())
	    }
	};

}

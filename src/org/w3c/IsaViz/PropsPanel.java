/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/05/2001
 */


package org.w3c.IsaViz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.Vector;

/*Display (and edit) an INode's attributes (URI, anonymous, value...) + appropriate commands, like delete*/

class PropsPanel extends JFrame {

    Editor application;
    JPanel mainPanel;

    PropsPanel(Editor e,int x,int y,int width,int height){
	this.application=e;
	Container cpane=this.getContentPane();
	mainPanel=new JPanel();
	cpane.add(mainPanel);
	//window
	WindowListener w0=new WindowAdapter(){
		public void windowClosing(WindowEvent e){application.cmp.showPropsMn.setSelected(false);}
		public void windowActivated(WindowEvent e){application.alwaysUpdateViews(true);}
	    };
	this.addWindowListener(w0);
	this.setTitle("Attributes");
	this.pack();
	this.setLocation(x,y);
	this.setSize(width,height);
// 	this.setVisible(true);
    }

    void updateDisplay(INode o){
	if (o instanceof IResource){this.showResourceProps((IResource)o);}
	else if (o instanceof ILiteral){this.showLiteralProps((ILiteral)o);}
	else if (o instanceof IProperty){this.showPropertyProps((IProperty)o);}
    }

    void reset(){
	mainPanel.removeAll();
	this.paintAll(this.getGraphics());
    }

    void showResourceProps(final IResource r){
	if (r!=null){
	    final JRadioButton uriBt=new JRadioButton("URI");
	    final JRadioButton idBt=new JRadioButton("ID");
	    final JCheckBox anonBt=new JCheckBox("Anonymous",r.isAnon());
	    String id=r.getIdent();
	    final JTextField tf=new JTextField(id.startsWith(Editor.DEFAULT_NAMESPACE) ? id.substring(Editor.DEFAULT_NAMESPACE.length(),id.length()) : id);
	    final JButton delete=new JButton("Delete");
	    final JButton showProperties=new JButton("Show Properties");
	    final JLabel caption=new JLabel(" ");

	    ActionListener a1=new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			if (e.getSource()==delete){
			    application.deleteResource(r);
			    reset();
			}
			else if (e.getSource()==uriBt){
			    caption.setText(" ");
			    this.checkAndUpdateResource();
			}
			else if (e.getSource()==idBt){
			    caption.setText("Base URI='"+Editor.DEFAULT_NAMESPACE+" '");
			    this.checkAndUpdateResource();
			}
			else if (e.getSource()==showProperties){
			    application.displayResOutgoingPredicates(r);
			}
			else if (e.getSource()==anonBt){
			    if (anonBt.isSelected()){
				caption.setText(" ");
				tf.setEnabled(false);
				uriBt.setEnabled(false);
				idBt.setEnabled(false);
				application.makeAnonymous(r);
				String id=r.getIdent();
				tf.setText(r.getIdent());
			    }
			    else {
				tf.setText("");
				tf.setEnabled(true);
				uriBt.setEnabled(true);
				uriBt.setSelected(true);
				idBt.setEnabled(true);
			    }
			}
		    }

		    void checkAndUpdateResource(){
			if (tf.getText().length()>0){
			    if (!tf.getText().startsWith(Editor.ANON_NODE)){
				application.changeResourceURI(r,tf.getText(),uriBt.isSelected()); //if uriBt not selected, we have an ID
				String id=r.getIdent();//update since it may have been changed by changeResourceURI
				tf.setText(id.startsWith(Editor.DEFAULT_NAMESPACE) ? id.substring(Editor.DEFAULT_NAMESPACE.length(),id.length()) : id); //don't display default namespace if we have an ID 
			    }//(appended automatically in the resource itself)
			    else {javax.swing.JOptionPane.showMessageDialog(application.propsp,Editor.ANON_NODE+" is reserved for anonymous nodes.");}
			}
			else {javax.swing.JOptionPane.showMessageDialog(application.propsp,Messages.provideURI);}
		    }
		};
	    
	    KeyListener k1=new KeyListener(){
		    public void keyPressed(KeyEvent e){
			if (e.getKeyCode()==KeyEvent.VK_ENTER){
			    if (e.getSource()==tf){
				if (tf.getText().length()>0){
				    if (!tf.getText().startsWith(Editor.ANON_NODE)){
					application.changeResourceURI(r,tf.getText(),uriBt.isSelected()); //if uriBt not selected, we have an ID
					String id=r.getIdent();//update since it may have been changed by changeResourceURI
					tf.setText(id.startsWith(Editor.DEFAULT_NAMESPACE) ? id.substring(Editor.DEFAULT_NAMESPACE.length(),id.length()) : id); //don't display default namespace if we have an ID 
				    }//(appended automatically in the resource itself)
				    else {javax.swing.JOptionPane.showMessageDialog(application.propsp,Editor.ANON_NODE+" is reserved for anonymous nodes.");}
				}
				else {javax.swing.JOptionPane.showMessageDialog(application.propsp,Messages.provideURI);}
			    }
			}
		    }
		    
		    public void keyReleased(KeyEvent e){}
		    public void keyTyped(KeyEvent e){}
		};

	    mainPanel.removeAll();
	    GridBagLayout gridBag=new GridBagLayout();
	    GridBagConstraints constraints=new GridBagConstraints();
	    mainPanel.setLayout(gridBag);
	    constraints.fill=GridBagConstraints.NONE;
	    constraints.anchor=GridBagConstraints.WEST;

	    ButtonGroup bg=new ButtonGroup();
	    buildConstraints(constraints,0,0,1,1,34,3);
	    gridBag.setConstraints(uriBt,constraints);
	    mainPanel.add(uriBt);bg.add(uriBt);
	    buildConstraints(constraints,1,0,1,1,33,0);
	    gridBag.setConstraints(idBt,constraints);
	    mainPanel.add(idBt);bg.add(idBt);
	    if (r.getIdent().startsWith(Editor.DEFAULT_NAMESPACE)){
		idBt.setSelected(true);
		caption.setText("Base URI='"+Editor.DEFAULT_NAMESPACE+" '");
	    }
	    else {uriBt.setSelected(true);}
	    uriBt.addActionListener(a1);
	    idBt.addActionListener(a1);

	    constraints.anchor=GridBagConstraints.EAST;
	    anonBt.addActionListener(a1);
	    buildConstraints(constraints,2,0,1,1,33,0);
	    gridBag.setConstraints(anonBt,constraints);
	    mainPanel.add(anonBt);

	    constraints.anchor=GridBagConstraints.CENTER;
	    constraints.fill=GridBagConstraints.BOTH;
	    buildConstraints(constraints,0,1,3,1,100,3);
	    gridBag.setConstraints(caption,constraints);
	    mainPanel.add(caption);

	    constraints.fill=GridBagConstraints.HORIZONTAL;
	    tf.addKeyListener(k1);
	    buildConstraints(constraints,0,2,3,1,100,3);
	    gridBag.setConstraints(tf,constraints);
	    mainPanel.add(tf);

	    if (r.isAnon()){//make input fields not editable if anonymous resource
		tf.setEnabled(false);
		uriBt.setEnabled(false);
		idBt.setEnabled(false);
	    }

	    delete.addActionListener(a1);
	    buildConstraints(constraints,0,3,3,1,100,3);
	    gridBag.setConstraints(delete,constraints);
	    mainPanel.add(delete);

	    showProperties.addActionListener(a1);
	    buildConstraints(constraints,0,4,3,1,100,3);
	    gridBag.setConstraints(showProperties,constraints);
	    mainPanel.add(showProperties);

	    //fill remaining space
	    constraints.fill=GridBagConstraints.BOTH;
	    JPanel p0=new JPanel();
	    buildConstraints(constraints,0,5,3,1,100,88);
	    gridBag.setConstraints(p0,constraints);
	    mainPanel.add(p0);

	    this.paintAll(this.getGraphics());
	}
    }

    void showLiteralProps(final ILiteral l){
	if (l!=null){

	    final JCheckBox wellFormedBt=new JCheckBox("Escape special XML chars",l.escapesXMLChars());
	    final JTextField tf=new JTextField((l.getLang()!=null) ? l.getLang() : "");
	    final JTextArea ta=new JTextArea((l.getValue()!=null) ? l.getValue() : "");
	    final JButton delete=new JButton("Delete");

	    ActionListener a2=new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			if (e.getSource()==delete){
			    application.deleteLiteral(l);
			    reset();
			}
			else if (e.getSource()==wellFormedBt){
			    l.setEscapeXMLChars(wellFormedBt.isSelected());
			}
		    }
		};

	    KeyListener k2=new KeyListener(){
		    public void keyPressed(KeyEvent e){
			if (e.getKeyCode()==KeyEvent.VK_ENTER){
			    if (e.getSource()==tf){
				l.setLanguage((tf.getText().length()>0) ? tf.getText() : null);
			    }
			}
		    }
		    
		    public void keyReleased(KeyEvent e){}
		    public void keyTyped(KeyEvent e){}
		};

	    FocusListener f2=new FocusListener(){
		    public void focusGained(FocusEvent e){}
		    public void focusLost(FocusEvent e){
			if (e.getSource()==tf){
			    l.setLanguage((tf.getText().length()>0) ? tf.getText() : null);
			}
			else if (e.getSource()==ta){
			    application.setLiteralValue(l,ta.getText());
			}
		    }
		};


	    mainPanel.removeAll();
	    GridBagLayout gridBag=new GridBagLayout();
	    GridBagConstraints constraints=new GridBagConstraints();
	    mainPanel.setLayout(gridBag);
	    constraints.fill=GridBagConstraints.HORIZONTAL;
	    constraints.anchor=GridBagConstraints.WEST;

	    JLabel lang=new JLabel("lang:");
	    buildConstraints(constraints,0,0,1,1,33,3);
	    gridBag.setConstraints(lang,constraints);
	    mainPanel.add(lang);
	    tf.addKeyListener(k2);
	    tf.addFocusListener(f2);
	    buildConstraints(constraints,1,0,1,1,33,0);
	    gridBag.setConstraints(tf,constraints);
	    mainPanel.add(tf);

	    constraints.fill=GridBagConstraints.NONE;
	    constraints.anchor=GridBagConstraints.EAST;
	    wellFormedBt.addActionListener(a2);
	    buildConstraints(constraints,2,0,1,1,34,0);
	    gridBag.setConstraints(wellFormedBt,constraints);
	    mainPanel.add(wellFormedBt);

	    constraints.fill=GridBagConstraints.HORIZONTAL;
	    constraints.anchor=GridBagConstraints.CENTER;
	    delete.addActionListener(a2);
	    buildConstraints(constraints,0,1,3,1,100,3);
	    gridBag.setConstraints(delete,constraints);
	    mainPanel.add(delete);

	    constraints.fill=GridBagConstraints.BOTH;
 	    ta.addFocusListener(f2);
	    JScrollPane sp=new JScrollPane(ta);
	    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    buildConstraints(constraints,0,2,3,1,100,94);
	    gridBag.setConstraints(sp,constraints);
	    mainPanel.add(sp);

	    this.paintAll(this.getGraphics());
	}
    }

    void showPropertyProps(final IProperty p){
	if (p!=null){
	    final JButton delete=new JButton("Delete");

	    Vector v=application.getAllPropertyNS(); //add an additional element to
	    final JComboBox nscbb=new JComboBox(v);
	    String prefix=application.getNSBinding(p.getNamespace());
	    nscbb.setSelectedItem((prefix!=null) ? prefix : p.getNamespace());
	    final JComboBox lncbb=new JComboBox(application.getProperties4NS(p.getNamespace()));
	    lncbb.setSelectedItem(p.getLocalname());
	    nscbb.setMaximumRowCount(10);
	    lncbb.setMaximumRowCount(10);

	    ActionListener a3=new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			if (e.getSource()==delete){
			    application.deleteProperty(p);
			    reset();
			}
		    }
		};

	    ItemListener i3=new ItemListener(){
		    public void itemStateChanged(ItemEvent e){
			if (e.getSource()==nscbb){//update name list to display only props
			    if (e.getStateChange()==ItemEvent.SELECTED){//in this namespace
				lncbb.removeAllItems();
				Vector v=application.getProperties4NS((String)e.getItem());
				for (int i=0;i<v.size();i++){
				    lncbb.addItem(v.elementAt(i));
				}
			    }
			}
			else {//e.getSource()==lncbb
			    if (e.getStateChange()==ItemEvent.SELECTED){
				application.changePropertyURI(p,(String)nscbb.getSelectedItem(),(String)lncbb.getSelectedItem());
			    }
			}
		    }
		};

	    mainPanel.removeAll();
	    GridBagLayout gridBag=new GridBagLayout();
	    GridBagConstraints constraints=new GridBagConstraints();
	    mainPanel.setLayout(gridBag);
	    constraints.fill=GridBagConstraints.HORIZONTAL;
	    constraints.anchor=GridBagConstraints.CENTER;

	    delete.addActionListener(a3);
	    buildConstraints(constraints,0,0,1,1,100,5);
	    gridBag.setConstraints(delete,constraints);
	    mainPanel.add(delete);

	    JLabel l0=new JLabel("Namespace");
	    buildConstraints(constraints,0,1,1,1,100,5);
	    gridBag.setConstraints(l0,constraints);
	    mainPanel.add(l0);

	    nscbb.addItemListener(i3);
	    buildConstraints(constraints,0,2,1,1,100,5);
	    gridBag.setConstraints(nscbb,constraints);
	    mainPanel.add(nscbb);

	    JLabel l1=new JLabel("Property");
	    buildConstraints(constraints,0,3,1,1,100,5);
	    gridBag.setConstraints(l1,constraints);
	    mainPanel.add(l1);

	    lncbb.addItemListener(i3);
	    buildConstraints(constraints,0,4,1,1,100,5);
	    gridBag.setConstraints(lncbb,constraints);
	    mainPanel.add(lncbb);

	    //fill remaining space
	    constraints.fill=GridBagConstraints.BOTH;
	    JPanel p0=new JPanel();
	    buildConstraints(constraints,0,5,1,1,100,75);
	    gridBag.setConstraints(p0,constraints);
	    mainPanel.add(p0);

	    this.paintAll(this.getGraphics());
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




























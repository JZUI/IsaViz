/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 11/26/2001
 */

package org.w3c.IsaViz;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;

/*This dialog is made modal so that users cannot crate several resources/literals "in parallel"*/

class NewLitPanel extends JDialog implements KeyListener,ActionListener {

    Editor application;

    ILiteral node;

    JCheckBox wfBt;
    JTextArea ta;
    JTextField tf;
    JButton ok,cancel;
    
    NewLitPanel(Editor app,ILiteral n){
	super((JFrame)Editor.vsm.getActiveView().getFrame(),"New Literal...",true); //getFrame() sends a Container
	application=app;                     //because it is the first common swing ancestor of EView and IView
	node=n;                              //if in the future we switch to internal views, will have to cast
	Container cpane=this.getContentPane();//as JInternalFrame
	GridBagLayout gridBag=new GridBagLayout();
	GridBagConstraints constraints=new GridBagConstraints();
	constraints.fill=GridBagConstraints.HORIZONTAL;
	constraints.anchor=GridBagConstraints.EAST;
	cpane.setLayout(gridBag);
	JPanel p0=new JPanel();
	GridBagLayout gridBag1=new GridBagLayout();
	GridBagConstraints constraints1=new GridBagConstraints();
	constraints1.fill=GridBagConstraints.NONE;
	p0.setLayout(gridBag1);
	JLabel l2=new JLabel("Lang:");
	constraints1.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints1,0,0,1,1,20,0);
	gridBag1.setConstraints(l2,constraints1);
	p0.add(l2);
	tf=new JTextField();
	tf.addKeyListener(this);
	constraints1.fill=GridBagConstraints.HORIZONTAL;
	constraints1.anchor=GridBagConstraints.WEST;
	buildConstraints(constraints1,1,0,1,1,40,0);
	gridBag1.setConstraints(tf,constraints1);
	p0.add(tf);
	wfBt=new JCheckBox("Escape special XML characters");
	constraints1.fill=GridBagConstraints.NONE;
	constraints1.anchor=GridBagConstraints.EAST;
	buildConstraints(constraints1,2,0,1,1,40,0);
	gridBag1.setConstraints(wfBt,constraints1);
	p0.add(wfBt);
	wfBt.setSelected(true);
	buildConstraints(constraints,0,0,1,1,100,5);
	gridBag.setConstraints(p0,constraints);
	cpane.add(p0);
	ta=new JTextArea("");
	JScrollPane sp=new JScrollPane(ta);
	sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	constraints.fill=GridBagConstraints.BOTH;
	constraints.anchor=GridBagConstraints.CENTER;
	buildConstraints(constraints,0,1,1,1,100,90);
	gridBag.setConstraints(sp,constraints);
	cpane.add(sp);
	JPanel p1=new JPanel();
	p1.setLayout(new GridLayout(1,2));
	ok=new JButton("OK");
	ok.addActionListener(this);
	ok.addKeyListener(this);
	p1.add(ok);
	cancel=new JButton("Cancel");
	cancel.addActionListener(this);
	cancel.addKeyListener(this);
	p1.add(cancel);
	constraints.fill=GridBagConstraints.HORIZONTAL;
	buildConstraints(constraints,0,2,1,1,20,5);
	gridBag.setConstraints(p1,constraints);
	cpane.add(p1);
	WindowListener w0=new WindowAdapter(){
		public void windowClosing(WindowEvent e){}
		public void windowActivated(WindowEvent e){ta.requestFocus();}
	    };
	this.addWindowListener(w0);
	this.pack();
	Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	this.setLocation((screenSize.width-125)/2,(screenSize.height-40)/2);
	this.setSize(300,200);
	this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource()==ok){
	    application.storeLiteral(node,ta.getText(),wfBt.isSelected(),tf.getText());
	    this.dispose();
	}
	else if (e.getSource()==cancel){application.cancelNewNode(node);this.dispose();}
    }

    public void keyPressed(KeyEvent e){
	if (e.getKeyCode()==KeyEvent.VK_ENTER){
	    if (e.getSource()==tf || e.getSource()==ok){
		application.storeLiteral(node,ta.getText(),wfBt.isSelected(),tf.getText());
		this.dispose();
	    }
	    else if (e.getSource()==cancel){application.cancelNewNode(node);this.dispose();}
	}
    }

    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}

    void buildConstraints(GridBagConstraints gbc, int gx,int gy,int gw,int gh,int wx,int wy){
	gbc.gridx=gx;
	gbc.gridy=gy;
	gbc.gridwidth=gw;
	gbc.gridheight=gh;
	gbc.weightx=wx;
	gbc.weighty=wy;
    }

}

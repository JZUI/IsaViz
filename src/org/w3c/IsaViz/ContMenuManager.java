/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/26/2001
 */


package org.w3c.IsaViz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.xerox.VTM.engine.ViewPanel;

/*Manages contextual menus for nodes and edges in the graph*/

class ContMenuManager {

    Editor application;

    static JPopupMenu resMn;
    static JPopupMenu litMn;
    static JPopupMenu miscMn;

    ContMenuManager(Editor e){
	this.application=e;
	initResMenu();
	initLitMenu();
	initMiscMenu();
    }

    //init as much as we can so that we don;t have to do it each time we show the menu
    void initResMenu(){//resource menu
	resMn=new JPopupMenu();
	JLabel l1=new JLabel("Resource");
	l1.setFont(Editor.smallFont); //does not care about my changes in initLookAndFell() - don't know why
	resMn.add(l1);
	resMn.addSeparator();
    }

    //init as much as we can so that we don;t have to do it each time we show the menu
    void initLitMenu(){//literal menu
	litMn=new JPopupMenu();
	JLabel l1=new JLabel("Literal");
	l1.setFont(Editor.smallFont); //does not care about my changes in initLookAndFell() - don't know why
	litMn.add(l1);
	litMn.addSeparator();
    }

    void initMiscMenu(){//miscellaneous menu
	miscMn=new JPopupMenu();
	JMenuItem centerViewMn=new JMenuItem("Global View");
	centerViewMn.setFont(Editor.smallFont); //does not care about my changes in initLookAndFell() - don't know why
	miscMn.add(centerViewMn);
	ActionListener a1=new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    Editor.vsm.getGlobalView(Editor.vsm.getVirtualSpace(Editor.mainVirtualSpace).getCamera(0),500);
		}
	    };
	centerViewMn.addActionListener(a1);
    }

    
    void displayResourceMenu(IResource r,ViewPanel v,int x,int y){//x,y are the coordinates of the mouse in java2D coord syst (so as to place the popup menu at an appropriate location)
	resMn.show(v,x,y);
    }

    void displayLiteralMenu(ILiteral l,ViewPanel v,int x,int y){//x,y are the coordinates of the mouse in java2D coord syst (so as to place the popup menu at an appropriate location)
	litMn.show(v,x,y);
    }

    void displayMiscMenu(ViewPanel v,int x,int y){//x,y are the coordinates of the mouse in java2D coord syst (so as to place the popup menu at an appropriate location)
	miscMn.show(v,x,y);
    }

}

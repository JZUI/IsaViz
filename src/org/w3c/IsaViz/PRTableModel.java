/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/10/2001
 */


package org.w3c.IsaViz;

import javax.swing.table.*;

/*a custom table model in which no cell is editable*/

public class PRTableModel extends DefaultTableModel {

    public PRTableModel(int nbRow,int nbCol){
	super(nbRow,nbCol);
    }

    public boolean isCellEditable(int row,int column){
	return false;
    }

}

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/20/2001
 */


package org.w3c.IsaViz;


/*Parent of all ISV commands (delete, copy, cut, paste, create, comment)*/

abstract class ISVCommand {

    abstract void _undo();

    abstract void _do();

}

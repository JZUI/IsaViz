/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 01/09/2002
 */

package org.w3c.IsaViz;

import org.w3c.dom.Element;

import com.xerox.VTM.glyphs.VPath;
import com.xerox.VTM.svg.SVGWriter;

/*remember parameters of a node (position and size)*/

class SplineGeom {

    String svgCoords;
    long tvx,tvy,hvx,hvy;
    float hor;

    SplineGeom(VPath p,long tx,long ty,long hx,long hy,float hr){
	svgCoords=(new SVGWriter()).getSVGPathCoordinates(p);  //spline
	tvx=tx;    //text's x coord
	tvy=ty;    //text's y coord
	hvx=hx;    //arrow head's x coord
	hvy=hy;    //arrow head's y coord
	hor=hr;    //arrow head's orientation
    }

}

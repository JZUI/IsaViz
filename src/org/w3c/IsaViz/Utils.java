/*   FILE: Utils.java
 *   DATE OF CREATION:   10/27/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Feb 12 11:12:55 2003 by Emmanuel Pietriga
 */

/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */ 



package org.w3c.IsaViz;

import com.xerox.VTM.engine.LongPoint;
import com.xerox.VTM.glyphs.VTriangleOr;
import java.awt.geom.Point2D;
import java.io.File;

public class Utils {

    /**various misc utility methods*/

    /**increment a byte representing a char value with the following values (in order) 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0*/
    public static byte incByte(byte b){
	byte res;
	if (b<0x7a){
	    if (b==0x39){res=0x41;}
	    else if (b==0x5a){res=0x61;}
	    else {res=++b;}
	}
	else {res=0x30;}
	return res;
    }

    /**check whether this string represents a positive integer or not*/
    public static boolean isPositiveInteger(String s){
	for (int i=0;i<s.length();i++){
	    if (!Character.isDigit(s.charAt(i))){return false;}
	}
	return true;
    }
    

    /**
     * returns all but last 2 characters of a string (All But Last 2 Chars)
     */
    public static String abl2c(String s){
	return s.substring(0,s.length()-2);
    }

    /**
     * returns all but last 2 characters of a string (All But Last 2 Chars) representing a float
     */
    public static String abl2c(float f){
	String s=String.valueOf(f);
	return s.substring(0,s.length()-2);
    }

    /**
     * tells whether the underlying OS is Windows(Win32) or not
     */
    public static boolean osIsWindows(){
	String osName=System.getProperty("os.name");
	if (osName.startsWith("Windows") || osName.startsWith("windows")){
	    //I've always seen it as Windows, not windows - but just in case...
	    return true;
	}
	else return false;
    }
    

    /**
     * tells wheter the current JVM is version 1.4.0 and later (or not)
     */
    public static boolean javaVersionIs140OrLater(){
	String version=System.getProperty("java.vm.version");
	float numVer=(new Float(version.substring(0,3))).floatValue();
	if (numVer>=1.4f){return true;}
	else {return false;}
    }

    /**erases prefix if exists (e.g. genid:6574 -> 6574)*/
    public static String erasePrefix(String s){
	StringBuffer sb=new StringBuffer(s);
	int i=0;
	while (i<sb.length()){
	    if (sb.charAt(i)==':'){sb.delete(0,i+1);break;}
	    i++;
	}
	return sb.toString();
    }

    /**
     *Replaces all occurences of key in input by replacement
     *RDFValidator was using Jakarta's regexp package to do the same thing - rewrote a similar function so that we do not need users to install Jakarta
     */
    public static String replaceString(String input, String key, String replacement) {
        String res="";
        int keyLength=key.length();
        int index=input.indexOf(key);
        int lastIndex=0;
        while (index>=0) {
            res=res+input.substring(lastIndex,index)+replacement;
            lastIndex=index+keyLength;
            index=input.indexOf(key,lastIndex);
        }
	res+=input.substring(lastIndex,input.length());
        return res;
    }

    /** Given a URI, determine the split point between the namespace part - taken from Jena 1.6.1
     * and the localname part.
     * If there is no valid localname part then the length of the
     * string is returned.
     *
     * @param uri
     * @return the index of the first character of the localname
     */
    public static int splitNamespace(String uri)
    {
        char ch;
        int lg = uri.length();
        if (lg == 0) return 0;
        int j;
        int i;
        for (i=lg-1; i>=0; i--) {
            ch = uri.charAt(i);
            if ( (com.hp.hpl.mesa.rdf.jena.common.XMLChar.CHARS[ch] & com.hp.hpl.mesa.rdf.jena.common.XMLChar.MASK_NCNAME) == 0 )
               break;
        }
        for (j=i+1;j<lg;j++) {
          ch = uri.charAt(j);
          if ((com.hp.hpl.mesa.rdf.jena.common.XMLChar.CHARS[ch] & com.hp.hpl.mesa.rdf.jena.common.XMLChar.MASK_NCNAME_START) != 0 )
             break;
        }
        return j;
    }

    public static Point2D computeStepValue(LongPoint p1,LongPoint p2){
	int signOfX=(p2.x>=p1.x) ? 1 : -1 ;
	int signOfY=(p2.y>=p1.y) ? 1 : -1 ;
	double ddx,ddy;
	if (p2.x==p1.x){//vertical direction (ar is infinite) - to prevent division by 0
	    ddx=0;
	    ddy=signOfY;
	}
	else {
	    double ar=(p2.y-p1.y)/((double)(p2.x-p1.x));
	    if (Math.abs(ar)>1.0f){
		ddx=signOfX/Math.abs(ar);
		ddy=signOfY;
	    }
	    else {
		ddx=signOfX;
		ddy=signOfY*Math.abs(ar);
	    }
	}
	return new Point2D.Double(ddx,ddy);
    }

    public static Point2D computeStepValue(double p1x,double p1y,double p2x,double p2y){
	int signOfX=(p2x>=p1x) ? 1 : -1 ;
	int signOfY=(p2y>=p1y) ? 1 : -1 ;
	double ddx,ddy;
	if (p2x==p1x){//vertical direction (ar is infinite) - to prevent division by 0
	    ddx=0;
	    ddy=signOfY;
	}
	else {
	    double ar=(p2y-p1y)/((double)(p2x-p1x));
	    if (Math.abs(ar)>1.0f){
		ddx=signOfX/Math.abs(ar);
		ddy=signOfY;
	    }
	    else {
		ddx=signOfX;
		ddy=signOfY*Math.abs(ar);
	    }
	}
	return new Point2D.Double(ddx,ddy);
    }

    /**creates a VTriangle whose orientation matches the direction of the line passing by both argument points (direction is from p1 to p2) - triangle is created at coordinates of p2*/
    public static VTriangleOr createPathArrowHead(LongPoint p1,LongPoint p2,VTriangleOr t){
	return createPathArrowHead(p1.x,p1.y,p2.x,p2.y,t);
    }
    
    /**creates a VTriangle whose orientation matches the direction of the line passing by both argument points (direction is from p2 to p1) - triangle is created at coordinates of p2*/
    public static VTriangleOr createPathArrowHead(double p1x,double p1y,double p2x,double p2y,VTriangleOr t){
	Point2D deltaor=Utils.computeStepValue(p1x,p1y,p2x,p2y);
	double angle=0;
	if (deltaor.getX()==0){
	    angle=0;
	    if (deltaor.getY()<0){angle=Math.PI;}
	}
	else {
	    angle=Math.atan(deltaor.getY()/deltaor.getX());
	    //align with VTM's system coordinates (a VTriangle's "head" points to the north when orient=0, not to the east)
	    if (deltaor.getX()<0){angle+=Math.PI/2;}   //comes from angle+PI-PI/2 (first PI due to the fact that ddx is <0 and the use of the arctan function - otherwise, head points in the opposite direction)
	    else {angle-=Math.PI/2;}
	}
	if (t!=null){
	    t.moveTo((long)p2x,(long)p2y);
	    t.orientTo((float)angle);
	    return t;
	}
	else return new VTriangleOr((long)p2x,(long)p2y,0,Editor.ARROW_HEAD_SIZE,ConfigManager.propertyColorB,(float)angle);
    }

    /**
     * Create a File object from the given directory and file names
     *
     *@param directory the file's directory
     *@param prefix the file's prefix name (not its directory)
     *@param suffix the file's suffix or extension name
     *@return a File object if a temporary file is created; null otherwise
     */
    public static File createTempFile (String directory, String prefix, String suffix){
        File f;
        try {
            File d=new File(directory);
            f=File.createTempFile(prefix,suffix,d);
        }
	catch (Exception e){System.err.println(e);return null;}
        return f;
    }

    /**
     *@param sb a StringBuffer from which leading whitespaces should be removed
     */
    static void delLeadingSpaces(StringBuffer sb){
	while ((sb.length()>0) && (Character.isWhitespace(sb.charAt(0)))){
	    sb.deleteCharAt(0);
	}
    }

    public static boolean isWhiteSpaceCharsOnly(String s){
	for (int i=0;i<s.length();i++){
	    if (!Character.isWhitespace(s.charAt(i))){return false;}
	}
	return true;
    }

    /**
     *returns the index of the first empty (null) element in an array - returns -1 if the array is full
     *@param ar the array in which 
     */
    public static int getFirstEmptyIndex(Object[] ar){
	int i;
	for (i=0;i<ar.length;i++){
	    if (ar[i]==null){break;}
	}
	if (i==ar.length){return -1;}
	else {return i;}
    }

    /**
     *returns the index of the first empty (null) element in an array - returns -1 if the array is full
     *@param ar the array in which 
     */
    public static void eraseFirstAddNewElem(Object[] ar,Object o){
	for (int i=1;i<ar.length;i++){
	    ar[i-1]=ar[i];
	}
	ar[ar.length-1]=o;
    }

    /**sets all elements of array ar to null*/
    public static void resetArray(Object[] ar){
	java.util.Arrays.fill(ar,null);
    }

    /**
     *computes the Euclidian distance between points (x1,y1) and (x2,y2)
     */
    public static double euclidianDistance(double x1,double y1,double x2,double y2){
	return Math.sqrt(Math.pow(x2-x1,2)+Math.pow(y2-y1,2));
    }

    /**
     *returns a representation of a Font (family, style, size) that can be parsed by java.awt.Font.decode()
     */
    public static String encodeFont(java.awt.Font f){
	return f.getFamily()+" "+net.claribole.zvtm.fonts.FontDialog.getFontStyleName(f.getStyle())+" "+(new Integer(f.getSize())).toString();
    }
}

/*   FILE: Utils.java
 *   DATE OF CREATION:   10/27/2001
 *   AUTHOR :            Emmanuel Pietriga (emmanuel@w3.org)
 *   MODIF:              Wed Apr 16 10:26:31 2003 by Emmanuel Pietriga (emmanuel@w3.org, emmanuel@claribole.net)
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
import java.util.Vector;
import java.awt.Font;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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

//     /** Given a URI, determine the split point between the namespace part - taken from Jena 1.6.1
//      * and the localname part.
//      * If there is no valid localname part then the length of the
//      * string is returned.
//      *
//      * @param uri
//      * @return the index of the first character of the localname
//      */
//     public static int splitNamespace(String uri)
//     {
//         char ch;
//         int lg = uri.length();
//         if (lg == 0) return 0;
//         int j;
//         int i;
//         for (i=lg-1; i>=0; i--) {
//             ch = uri.charAt(i);
//             if ( (com.hp.hpl.jena.rdf.common.XMLChar.CHARS[ch] & com.hp.hpl.jena.rdf.common.XMLChar.MASK_NCNAME) == 0 )
//                break;
//         }
//         for (j=i+1;j<lg;j++) {
//           ch = uri.charAt(j);
//           if ((com.hp.hpl.jena.rdf.common.XMLChar.CHARS[ch] & com.hp.hpl.jena.rdf.common.XMLChar.MASK_NCNAME_START) != 0 )
//              break;
//         }
//         return j;
//     }

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

//     /**
//      *dump an input stream into a temporary file 
//      *@param is the input stream whose content should be dumped into the file
//      *@param directory the target file's directory
//      *@param prefix the target file's prefix name (not its directory)
//      *@param suffix the target file's suffix or extension name
//      *@return a File object if a temporary file is created; null otherwise
//      */
//     public static File inputStreamToTempFile(InputStream is,String directory, String prefix, String suffix){
// 	File f=createTempFile(directory,prefix,suffix);
// 	if (f!=null && is!=null){
// 	    // Dump input to tmp file
// 	    f.deleteOnExit();
// 	    try {
// 		FileOutputStream out = new FileOutputStream(f);
// 		byte[] buf = new byte[4096];
// 		int count;
// 		while ( (count = is.read(buf)) != -1 ) {
// 		    out.write(buf, 0, count);
// 		}
// 		is.close();
// 		out.flush();
// 		out.close();
// 	    }
// 	    catch (java.io.FileNotFoundException ex){System.err.println("Could not open file "+f);}
// 	    catch (java.io.IOException ex){System.err.println("Utils.inputStreamToTempFile: An error occured while creating temp file "+f);ex.printStackTrace();}
// 	}
// 	return f;
//     }

//     public static void writeInputStreamToOutputStream(InputStream is,OutputStream os){
// 	if (is!=null && os!=null){
// 	    try {
// 		byte[] buf = new byte[4096];
// 		int count;
// 		while ( (count = is.read(buf)) != -1 ) {
// 		    os.write(buf, 0, count);
// 		}
// 		is.close();
// 		os.flush();
// 		os.close();
// 	    }
// 	    catch (java.io.IOException ex){System.err.println("Utils.writeInputStreamToOutputStream: An error occured while reading from "+is+" or writing to "+os);ex.printStackTrace();}
// 	}
//     }

    static String delLeadingAndTrailingSpaces(String s){
	StringBuffer sb=new StringBuffer(s);
	Utils.delLeadingSpaces(sb);
	Utils.delTrailingSpaces(sb);
	return sb.toString();
    }

    static void delLeadingAndTrailingSpaces(StringBuffer sb){
	Utils.delLeadingSpaces(sb);
	Utils.delTrailingSpaces(sb);
    }

    static String delLeadingSpaces(String s){
	StringBuffer sb=new StringBuffer(s);
	Utils.delLeadingSpaces(sb);
	return sb.toString();
    }

    static String delTrailingSpaces(String s){
	StringBuffer sb=new StringBuffer(s);
	Utils.delTrailingSpaces(sb);
	return sb.toString();
    }

    /**
     *@param sb a StringBuffer from which leading whitespaces should be removed
     */
    static void delLeadingSpaces(StringBuffer sb){
	while ((sb.length()>0) && (Character.isWhitespace(sb.charAt(0)))){
	    sb.deleteCharAt(0);
	}
    }

    /**
     *@param sb a StringBuffer from which trailing whitespaces should be removed
     */
    static void delTrailingSpaces(StringBuffer sb){
	while ((sb.length()>0) && (Character.isWhitespace(sb.charAt(sb.length()-1)))){
	    sb.deleteCharAt(sb.length()-1);
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

    /**
     *tells whether a vector of String v contains a given string s or not by using String.equals()
     */
    public static boolean containsString(Vector v,String s){
	if (v!=null && s!=null){
	    for (int i=0;i<v.size();i++){
		if (((String)v.elementAt(i)).equals(s)){return true;}
	    }
	}
	return false;
    }

    /*takes a vector of strings and returns a single string containing all values separated by commas*/
    public static String vectorOfStringAsCSStrings(Vector v){
	String res="";
	for (int i=0;i<v.size()-1;i++){
	    res+=(String)v.elementAt(i)+",";
	}
	res+=(String)v.lastElement();
	return res;
    }

    /*takes a vector of strings and returns a single string containing all values separated by commas*/
    public static String vectorOfObjectsAsCSString(Vector v){
	String res="";
	for (int i=0;i<v.size()-1;i++){
	    res+=v.elementAt(i).toString()+",";
	}
	res+=v.lastElement().toString();
	return res;
    }

    /*tells whether f has a weight and style equal to w and s (take value resp. in Style.CSS_FONT_WEIGHT_* and Style.CSS_FONT_STYLE_*)*/
    public static boolean sameFontStyleAs(Font f,short w,short s){
	int style=f.getStyle();
	if (style==Font.PLAIN && (w!=Style.CSS_FONT_WEIGHT_NORMAL || s!=Style.CSS_FONT_STYLE_NORMAL)){return false;}
	if (style==Font.BOLD && ((!(w==Style.CSS_FONT_WEIGHT_BOLD || w==Style.CSS_FONT_WEIGHT_BOLDER || w==Style.CSS_FONT_WEIGHT_500 || w==Style.CSS_FONT_WEIGHT_600 || w==Style.CSS_FONT_WEIGHT_700 || w==Style.CSS_FONT_WEIGHT_800 || w==Style.CSS_FONT_WEIGHT_900)) || (s!=Style.CSS_FONT_STYLE_NORMAL))){return false;}
	if (style==Font.ITALIC && ((s==Style.CSS_FONT_STYLE_NORMAL) || ((w!=Style.CSS_FONT_WEIGHT_NORMAL) && (w!=Style.CSS_FONT_WEIGHT_400)))){return false;}
	if ((style==(Font.BOLD+Font.ITALIC)) && (((!(w==Style.CSS_FONT_WEIGHT_BOLD || w==Style.CSS_FONT_WEIGHT_BOLDER || w==Style.CSS_FONT_WEIGHT_500 || w==Style.CSS_FONT_WEIGHT_600 || w==Style.CSS_FONT_WEIGHT_700 || w==Style.CSS_FONT_WEIGHT_800 || w==Style.CSS_FONT_WEIGHT_900)) || (!((s==Style.CSS_FONT_STYLE_ITALIC) || (s==Style.CSS_FONT_STYLE_OBLIQUE)))))){return false;}
	return true;
    }

    public static boolean isBold(short weight){
	if (weight==Style.CSS_FONT_WEIGHT_BOLD || weight==Style.CSS_FONT_WEIGHT_BOLDER || weight==Style.CSS_FONT_WEIGHT_500 || weight==Style.CSS_FONT_WEIGHT_600 || weight==Style.CSS_FONT_WEIGHT_700 || weight==Style.CSS_FONT_WEIGHT_800 || weight==Style.CSS_FONT_WEIGHT_900){return true;}
	else return false;
    }

    public static boolean isItalic(short style){
	if (style==Style.CSS_FONT_STYLE_ITALIC || style==Style.CSS_FONT_STYLE_OBLIQUE){return true;}
	else return false;
    }

}

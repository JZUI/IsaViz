/*
 *
 *  (c) COPYRIGHT World Wide Web Consortium, 1994-2001.
 *  Please first read the full copyright statement in file copyright.html
 *
 */

/*
 *Author: Emmanuel Pietriga (emmanuel.pietriga@xrce.xerox.com,epietrig@w3.org)
 *Created: 12/16/2001
 */

package org.w3c.IsaViz;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.File;

public class Messages {
    
    /*warning, error, help and other messages*/
    static final String reLayoutWarning="This will call GraphViz/DOT to compute a new layout for the model.\nThe new layout might be completely different from the current one, and you will not be able to undo this operation.\nDo you want to proceed?";

    static final String antialiasingWarning="Antialiasing requires additional computing resources.\nSetting it ON will noticeably reduce the refresh rate.\nIt is primarily aimed at producing higher quality images when exporting to PNG.";

    static final String webBrowserHelpText="--------------------------------------\nAUTOMATIC DETECTION\n--------------------------------------\nIsaViz can try to automatically detect your default web browser.\nThis feature is currently supported under Windows and some POSIX environments.\n\n--------------------------------------\nMANUAL CONFIGURATION\n--------------------------------------\nThe Path value should be the full command line path to your browser's main executable file. It can also be just this file's name if its parent directory is in your PATH environment variable.\n\nExamples:\nnetscape\n/usr/bin/netscape\nC:\\Program Files\\Internet Explorer\\IEXPLORE.EXE\n\nThe Command Line Options value is an optional field where you can put command line switches, like -remote for the UNIX version of Netscape that will open URLs in an already existing Netscape process (if it exists).";

    static final String proxyHelpText="If you are behind a firewall, you can manually set the proxy server to access remote resources.\n\nHostname should be the full name of the proxy server.\n\nPort should be the port number used to access external resources. This is a number (default value is 80).";

    static final String pngOnlyIn140FirstPart="This functionality is only available when running IsaViz using a JVM version 1.4.0 or later (it requires the ImageIO API).\nIsaViz detected JVM version ";

    static final String pngOnlyIn140SecondPart="\nDo you want to proceed anyway (this will probably cause an error)?";

    static final String incompleteParsing="The parsing might not be complete (The file is probably not well-formed XML).\n Some nodes and edges might be missing from the graph because of an error (check error log) in file ";

    static final String resetWarning="You are about to reset your project.\nAre you sure you want to continue?";

    static final String provideURI="You must provide an identifier.\nIf you want to make the resource anonymous, use the above checkbox.";

    static final String removePropType="At least one property in the model is of this type.\n Are you sure you want to remove the type from the list of types?\n(This will not remove the properties from the current model, but just the entry in this list).";

}

# Makefile for IsaViz
# Designed for GNUmake   - CYGWIN (Win32) version

# Compiles Java sources, builds JAR archives, generates docs.

# Author: Emmanuel PIETRIGA  (emmanuel@w3.org, emmanuel@claribole.net)
# Version: 10/02/2001

# SHELL USED TO EXECUTE COMMANDS
SHELL		:= /usr/bin/tcsh

# DIRECTORIES
ISV_DIR		:= /circus/WWW/2001/10/IsaViz
SRC_DIR        	:= $(ISV_DIR)/src
OBJ_DIR		:= $(ISV_DIR)/classes
DOC_DIR		:= $(ISV_DIR)/apidocs
LIB_DIR		:= $(ISV_DIR)/lib
XML_UTILS	:= $(LIB_DIR)/xercesImpl.jar;$(LIB_DIR)/xmlParserAPIs.jar
RDF_UTILS	:= $(LIB_DIR)/jena.jar
VTM             := $(LIB_DIR)/zvtm.jar
SESAME		:= $(LIB_DIR)/sesame-client.jar

# FILES
SRCS 		:= $(shell find $(SRC_DIR) -name '*.java')
OBJS 		:= $(SRCS:$(SRC_DIR)/%.java=$(OBJ_DIR)/%.class)

# Java Development Kit
JDK	    	:= /tools/jdk1.4.1
JAVAC          	:= $(JDK)/bin/javac.exe 
JAVAC_OPT	:= -O -classpath "$(OBJ_DIR);$(SRC_DIR);$(VTM);$(XML_UTILS);$(RDF_UTILS);$(SESAME)" -d $(OBJ_DIR)
COMPILE		:= $(JAVAC) $(JAVAC_OPT)
JAR 		:= $(JDK)/bin/jar
JAR_OPT		:= -cvf
ARCHIVE		:= $(JAR) $(JAR_OPT)

# RULES

# Default: makes all object targets
all : $(OBJS)

# Phony targets
.PHONY : all archive doc clean realclean test

isvjar : 
	cd $(ISV_DIR) ; $(JDK)/bin/jar cvf lib/isaviz.jar -C classes org/w3c/IsaViz images

sesamejar : 
	cd $(ISV_DIR) ; $(JDK)/bin/jar cvf plugins/isaviz-sesame.jar -C classes nl/aidministrator/sesame/isaviz

isvappjar :
	cd $(ISV_DIR) ; $(JDK)/bin/jar cvf lib/isvapp.jar -C classes org/w3c/IsaViz/applet appimages

# Clean (removes all the emacs autosaves)
clean : 
	rm -rf `find $(SRC_DIR)/org/w3c/IsaViz \( -name "*~" -o -name "#*#" \) -print`

realclean :
	rm -rf `find $(OBJ_DIR)/org/w3c/IsaViz -name "*.class" -print`

# Test (used to display variable values, test commands, etc.)
test :
	echo 'Welcome to the ISV makefile'

# Implicit rule for Java compiling
$(OBJ_DIR)/%.class : $(SRC_DIR)/%.java
	$(COMPILE) $<




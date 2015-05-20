#!/bin/sh

CURRENT_DIR=`pwd`
mkdir -p $CURRENT_DIR/classes
javac -J-agentlib:hprof=cpu=samples -cp . -d $CURRENT_DIR/classes PointGenerator.java

#!/bin/sh

CURRENT_DIR=`pwd`
mkdir -p $CURRENT_DIR/classes
javac -cp . -d $CURRENT_DIR/classes PointGenerator.java

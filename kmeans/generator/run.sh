#!/bin/sh

CURRENT_DIR=`pwd`
java -agentlib:hprof=cpu=times,depth=6 -cp ./classes PointGenerator

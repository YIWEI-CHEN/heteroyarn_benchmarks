#!/bin/sh
CURRENT_DIR=`pwd`
result_dir=/home/$( whoami )/hadoop_input/kmeans
points=$(( 50*1024*1024 ))
parts=10
[ -d $result_dir ] && rm -rf $result_dir 
mkdir -p $result_dir 
java -cp ${CURRENT_DIR}/classes PointGenerator "$result_dir/" ${points} ${parts} 

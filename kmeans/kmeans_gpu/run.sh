#/bin/sh/
CUR_DIR=`pwd`

hadoop jar $CUR_DIR/kmeans.jar KMeans $1

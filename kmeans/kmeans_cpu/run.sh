#/bin/sh/
CUR_DIR=`pwd`
TIMESTAMP=$(date -R)

hadoop jar $CUR_DIR/kmeans.jar KMeans $1 2>&1 | tee "kmeans.${1}.${TIMESTAMP}.log"

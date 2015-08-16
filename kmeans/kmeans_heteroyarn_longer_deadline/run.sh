#/bin/sh/
CUR_DIR=`pwd`
[ -d ${CUR_DIR}/log ] || mkdir ${CUR_DIR}/log
TIMESTAMP=$(date +%-d-%-m-%Y-%T)

hadoop --config deadline_540000 jar $CUR_DIR/kmeans.jar KMeans $1 2>&1 | tee "${CUR_DIR}/log/kmeans.${1}.${TIMESTAMP}.log"
sleep 5 
hadoop --config deadline_480000 jar $CUR_DIR/kmeans.jar KMeans $1 2>&1 | tee "${CUR_DIR}/log/kmeans.${1}.${TIMESTAMP}.log"


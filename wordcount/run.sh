#/bin/sh/
CUR_DIR=`pwd`
[ -d ${CUR_DIR}/log ] || mkdir ${CUR_DIR}/log
TIMESTAMP=$(date +%-d-%-m-%Y-%T)

hdfs dfs -rm -r /user/yiwei/wordcount/output
hadoop jar $CUR_DIR/wordcount.jar WordCount /user/yiwei/wordcount/input /user/yiwei/wordcount/output 2>&1 | tee "${CUR_DIR}/log/wordcount.${TIMESTAMP}.log"

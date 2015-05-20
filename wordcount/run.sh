#/bin/sh/
CUR_DIR=`pwd`
TIMESTAMP=$(date -R)

hdfs dfs -rm -r /user/yiwei/wordcount/output
hadoop jar $CUR_DIR/wordcount.jar WordCount /user/yiwei/wordcount/input /user/yiwei/wordcount/output 2>&1 | tee "wordcount.${TIMESTAMP}.log"

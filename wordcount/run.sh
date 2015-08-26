#/bin/sh/
CUR_DIR=`pwd`
[ -d ${CUR_DIR}/log ] || mkdir ${CUR_DIR}/log
TIMESTAMP=$(date +%-d-%-m-%Y-%T)

INPUT_DIR="wordcount/input"
OUTPUT_DIR="wordcount/output"
totalReduces=40

./prepare.sh ${INPUT_DIR} "1"

hdfs dfs -rm -r ${OUTPUT_DIR}
hadoop jar $CUR_DIR/wordcount.jar WordCount $INPUT_DIR $OUTPUT_DIR $totalReduces

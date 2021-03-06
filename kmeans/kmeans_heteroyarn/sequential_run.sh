#/bin/sh/
CUR_DIR=`pwd`
[ -d ${CUR_DIR}/log ] || mkdir ${CUR_DIR}/log
TIMESTAMP=$(date +%-d-%-m-%Y-%T)

K=1000
INPUT_DIR="kmeans/input"
OUTPUT_DIR="kmeans/output"
CENTROIDS_DIR="kmeans/centroids"
totalReduces=40

./prepare.sh ${INPUT_DIR} ${CENTROIDS_DIR} "1"

# prepare 12g data before execution
INPUT_DIR="kmeans/input2"
OUTPUT_DIR="kmeans/output2"
./prepare.sh ${INPUT_DIR} ${CENTROIDS_DIR} "2"

INPUT_DIR="kmeans/input"
OUTPUT_DIR="kmeans/output"
hadoop jar $CUR_DIR/kmeans.jar KMeans $K $INPUT_DIR $OUTPUT_DIR $CENTROIDS_DIR $totalReduces \
    2>&1 | tee "${CUR_DIR}/log/kmeans.${1}.${TIMESTAMP}.log" 

INPUT_DIR="kmeans/input2"
OUTPUT_DIR="kmeans/output2"
hadoop jar $CUR_DIR/kmeans.jar KMeans $K $INPUT_DIR $OUTPUT_DIR $CENTROIDS_DIR $totalReduces \
    2>&1 | tee "${CUR_DIR}/log/kmeans.${1}.${TIMESTAMP}.log"

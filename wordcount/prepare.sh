#hdfs dfs -mkdir -p wordcount/centroids
INPUT_DIR=$1
TIMES=$2
hdfs dfs -mkdir -p ${INPUT_DIR}
if [[ $HOSTNAME == "master.mycorp.kom" ]]; then
    hdfs dfs -put /mnt/wordcount/partaa ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partab ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partac ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partad ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partae ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partaf ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partag ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partah ${INPUT_DIR}
    if [[ $TIMES == "2" ]]; then
    hdfs dfs -put /mnt/wordcount/partai ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partaj ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partak ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partal ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partam ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partan ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partao ${INPUT_DIR}
    hdfs dfs -put /mnt/wordcount/partap ${INPUT_DIR}
    fi
fi

#hdfs dfs -mkdir -p kmeans/input
#hdfs dfs -mkdir -p kmeans/centroids
INPUT_DIR=$1
CENTROIDS_DIR=$2
TIMES=$3
hdfs dfs -mkdir -p ${INPUT_DIR}
hdfs dfs -mkdir -p ${CENTROIDS_DIR}
if [[ $HOSTNAME == "pasX" ]]; then
    hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/* ${INPUT_DIR}
#    echo "in pasX"
elif [[ -n `echo $HOSTNAME | grep "pasH[0-9]"` ]]; then
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part0 ${INPUT_DIR}
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part1 ${INPUT_DIR}
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part2 ${INPUT_DIR}
    if [[ $TIMES == "2" ]]; then
        hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part3 ${INPUT_DIR}
        hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part4 ${INPUT_DIR}
        hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part5 ${INPUT_DIR}
    fi
#    echo "in pasX[0-9]"
elif [[ $HOSTNAME == "master.mycorp.kom" ]]; then
    hdfs dfs -put /mnt/kmeans/part0 ${INPUT_DIR}
    hdfs dfs -put /mnt/kmeans/part1 ${INPUT_DIR}
    hdfs dfs -put /mnt/kmeans/part2 ${INPUT_DIR}
    if [[ $TIMES == "2" ]]; then
        hdfs dfs -put /mnt/kmeans/part3 ${INPUT_DIR}
        hdfs dfs -put /mnt/kmeans/part4 ${INPUT_DIR}
        hdfs dfs -put /mnt/kmeans/part5 ${INPUT_DIR}
    fi
fi
hdfs dfs -cp ${INPUT_DIR}/part0 ${CENTROIDS_DIR}
#hdfs dfs -ls kmeans/

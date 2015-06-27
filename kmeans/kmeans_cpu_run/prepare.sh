hdfs dfs -mkdir -p kmeans/input
hdfs dfs -mkdir -p kmeans/centroids
if [[ $HOSTNAME == "pasX" ]]; then
    hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/* kmeans/input
elif [[ $HOSTNAME == "pasH[0-9]" ]]; then
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/* kmeans/input
fi
hdfs dfs -cp kmeans/input/part0 kmeans/centroids
hdfs dfs -ls kmeans/

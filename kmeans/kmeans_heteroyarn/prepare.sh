hdfs dfs -mkdir -p kmeans/input
hdfs dfs -mkdir -p kmeans/centroids
if [[ $HOSTNAME == "pasX" ]]; then
    hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/* kmeans/input
#    echo "in pasX"
elif [[ -n `echo $HOSTNAME | grep "pasH[0-9]"` ]]; then
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part0 kmeans/input
#    echo "in pasX[0-9]"
elif [[ $HOSTNAME == "master.mycorp.kom" ]]; then
    hdfs dfs -put /mnt/kmeans/* kmeans/input
fi
hdfs dfs -cp kmeans/input/part0 kmeans/centroids
hdfs dfs -ls kmeans/

hdfs dfs -mkdir -p kmeans2/input
hdfs dfs -mkdir -p kmeans2/centroids
if [[ $HOSTNAME == "pasX" ]]; then
    hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/* kmeans2/input
#    echo "in pasX"
elif [[ -n `echo $HOSTNAME | grep "pasH[0-9]"` ]]; then
    hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/part[0-2] kmeans2/input
#    echo "in pasX[0-9]"
elif [[ $HOSTNAME == "master.mycorp.kom" ]]; then
    hdfs dfs -put /mnt/kmeans/* kmeans2/input
fi
hdfs dfs -cp kmeans2/input/part0 kmeans2/centroids
hdfs dfs -ls kmeans2/

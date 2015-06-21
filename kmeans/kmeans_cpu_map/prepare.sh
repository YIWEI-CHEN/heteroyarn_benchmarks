hdfs dfs -mkdir -p /user/yiwei/kmeans/input
hdfs dfs -mkdir -p /user/yiwei/kmeans/centroids
hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/part0 /user/yiwei/kmeans/input
hdfs dfs -cp /user/yiwei/kmeans/input/part0 /user/yiwei/kmeans/centroids

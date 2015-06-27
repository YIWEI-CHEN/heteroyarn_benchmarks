hdfs dfs -mkdir -p kmeans/input
hdfs dfs -mkdir -p kmeans/centroids
hdfs dfs -put /mnt/data/project/hadoop-input/kmeans/part0 kmeans/input
hdfs dfs -cp kmeans/input/part0 centroids

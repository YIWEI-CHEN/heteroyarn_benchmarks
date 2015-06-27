hdfs dfs -mkdir -p kmeans/input
hdfs dfs -mkdir -p kmeans/centroids
hdfs dfs -put /home/`whoami`/hadoop_input/kmeans/* kmeans/input
hdfs dfs -cp kmeans/input/part0 kmeans/centroids
hdfs dfs -ls kmeans/

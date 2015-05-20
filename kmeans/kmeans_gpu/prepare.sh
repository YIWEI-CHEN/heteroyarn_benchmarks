#hmkdir -p /user/yiwei/kmeans/input
#hmkdir -p /user/yiwei/kmeans/centroids
#hput /mnt/data/project/hadoop-input/kmeans/part0 /user/yiwei/kmeans/input
hdfs dfs -cp /user/yiwei/kmeans/input/part0 /user/yiwei/kmeans/centroids

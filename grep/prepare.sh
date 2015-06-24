hdfs dfs -mkdir -p /user/`whoami`
hdfs dfs -put ${HADOOP_CONF_DIR} input

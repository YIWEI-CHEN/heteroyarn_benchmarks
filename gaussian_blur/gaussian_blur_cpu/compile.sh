#/bin/sh/
CUR_DIR=`pwd`

MY_HADOOP_HOME=/home/heterohadoop/hadoop-2.2.0
MY_CLASSPATH=$MY_HADOOP_HOME/share/hadoop/common/hadoop-common-2.2.0.jar:$MY_HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.2.0.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/commons-cli-1.2.jar

mkdir -p $CUR_DIR/classes
javac -classpath $MY_CLASSPATH -d $CUR_DIR/classes $CUR_DIR/GaussianBlur.java
jar -cvf gaussian_blur.jar -C $CUR_DIR/classes . 

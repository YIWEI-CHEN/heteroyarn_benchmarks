#hdfs dfs -mkdir -p gaussian_blur/input
INPUT_DIR=$1
TIMES=$2
hdfs dfs -mkdir -p ${INPUT_DIR}
if [[ $HOSTNAME == "master.mycorp.kom" ]]; then
    hdfs dfs -put /mnt/gaussian_blur/rgb_part1 ${INPUT_DIR}
    #hdfs dfs -put /mnt/gaussian_blur/rgb_part2 ${INPUT_DIR}
    #hdfs dfs -put /mnt/gaussian_blur/rgb_part3 ${INPUT_DIR}
    if [[ $TIMES == "2" ]]; then
        hdfs dfs -put /mnt/gaussian_blur/rgb_part2 ${INPUT_DIR}
        #hdfs dfs -put /mnt/gaussian_blur/rgb_part4 ${INPUT_DIR}
        #hdfs dfs -put /mnt/gaussian_blur/rgb_part5 ${INPUT_DIR}
    fi
fi

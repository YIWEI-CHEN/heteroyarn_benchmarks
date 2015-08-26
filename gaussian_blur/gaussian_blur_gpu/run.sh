#/bin/sh/
CUR_DIR=`pwd`

sigma=4.0
INPUT_DIR="gaussian_blur/input"
OUTPUT_DIR="gaussian_blur/output"

./prepare.sh ${INPUT_DIR} "1"

hadoop jar $CUR_DIR/gaussian_blur.jar GaussianBlur $sigma $INPUT_DIR $OUTPUT_DIR

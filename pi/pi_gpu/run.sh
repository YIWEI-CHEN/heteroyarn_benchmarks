#/bin/sh/
CUR_DIR=`pwd`

hadoop jar $CUR_DIR/pi.jar QuasiMonteCarlo $1 $2 2>&1 | tee "pi.${1}_${2}.log"

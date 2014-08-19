#/bin/sh/
CUR_DIR=`pwd`

hadoop jar $CUR_DIR/pi.jar QuasiMonteCarlo $1 $2

#/bin/sh/
CUR_DIR=`pwd`

TIMESTAMP=$(date -R)

hadoop jar $CUR_DIR/black_scholes.jar BlackScholes 2>&1 | tee "blackschole.${TIMESTAMP}.log"

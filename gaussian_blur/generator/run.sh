#!/bin/bash
CURRENT_DIR=`pwd`
if [[ $HOSTNAME == "pasX" ]]; then
    result_dir=/mnt/data/project/hadoop-input/gaussian_blur
    #    echo "in pasX"
elif [[ -n `echo $HOSTNAME | grep "pasH[0-9]"` ]]; then
    result_dir=/home/$( whoami )/hadoop_input/gaussian_blur
fi
#files=`ls ./images/`
#[ -d $result_dir ] || mkdir -p $result_dir
#mkdir images
#string=""
#for (( i = 0; i < ${#files[@]}; i++ )); do
#    if [[ $(( ${i}%2 )) -eq 0 ]]; then
#        string=${files[$i]}
#    else
#        mv "../images/$string ${files[$i]}" images/a${i}.jpg
#    fi
#done
files=`ls ./images/a2[6-9]*`
for f in $files; do
    #name=`echo $f | cut -d'.' -f1`
    #ls -alh $f
    echo $f
    java -cp ${CURRENT_DIR}/classes PixelGenerator $f "$result_dir/rgb_part7.txt"
done
files=`ls ./images/a1[6-9]*`
for f in $files; do
    #name=`echo $f | cut -d'.' -f1`
    #ls -alh $f
    echo $f
    java -cp ${CURRENT_DIR}/classes PixelGenerator $f "$result_dir/rgb_part8.txt"
done

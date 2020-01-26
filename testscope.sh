#! /bin/bash

#internal script, no users checks

config="mode : 1 - algo1, 2 - algo2, 3 - algo1_stat, 4 - algo2_stat"
usage="$0 -p path_to_peersim_lib -m mode_config"

if [ $# -ne 4 ]
then
        echo $usage
	echo $config 

        exit 1
fi

while getopts p:m:h option
do
case "${option}"
in
p) lib=${OPTARG};;
m) mode=${OPTARG};;
h) echo $usage; echo $config; exit 1
esac
done

#test good lib directory
if  [ -d "$lib" ]
then
        echo "path to peersim is "$lib
else
        echo "Err : bad path to peersim lib ! Exit."
        echo $usage
	echo $config 
        exit 1

fi
#test directory
if  [ -d "test" ]
then
        mv test old_test
fi

mkdir test

echo "Pre-condition : initial scope value in config schould be 0."

#val="$(cat src/ara/config | grep scope)"

#init value in config should be 0
val=0
val0=$val

val2=10

step=20
maxscope=1000

#set and test
while [ $val2 -le $maxscope ]
do
	./launch.sh -p $lib -m $mode
	sed 's/protocol.emit.scope '$val'/protocol.emit.scope '$val2'/g' -i ./src/ara/config 
	val2=$(($val2 + $step))
	exit 1
done

#set init value
val2=$(($val2 - $step))
sed 's/protocol.emit.scope '$val2'/protocol.emit.scope '$val0'/g' -i ./src/ara/config 


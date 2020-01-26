#! /bin/bash

#This script will compile and launch the prog N times with different scopes
#You can vary the scope and N in param with option -N and -s
#You also need to mention the lib path and the type of config
#-h for more info
#Take a reasonable simulation time on your config
#The initial value of scope schould be 0

val=0
val0=$val

val2=40

step=10
n=6

config="mode : 3 - algo1_stat, 4 - algo2_stat"
usage="$0 -p path_to_peersim_lib -m mode_config [-s step] [-N times]"

if [ $# -le 3 ]
then
        echo $usage
	echo $config 

        exit 1
fi

while getopts p:m:s:N:h option
do
case "${option}"
in
p) lib=${OPTARG};;
m) mode=${OPTARG};;
s) step=${OPTARG};;
N) n=${OPTARG};;
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
        echo  "test directiry found"
else
	mkdir test
fi

echo "Pre-condition : initial scope value in config schould be 0."

#define config file
if [ $mode -eq 3 ]
then
	fconf="./src/ara/configVKT04Printer"
else

	if [ $mode -eq 4 ]
	then
		fconf="./src/ara/configPrinter"
	else
		
        	echo "Err : bad mode to test ! Exit."
        	echo $usage
		echo $config 
        	exit 1
	fi

fi

#set and test
cpt=1
while [ $cpt -le $n ]
do


	#compile & run
	#java -cp $p:./bin $class $mode
	./launch.sh -p $lib -m $mode	
	sed 's/protocol.emit.scope '$val'/protocol.emit.scope '$val2'/g' -i $fconf
	
	val=$val2
	val2=$(($val2 + $step))
	cpt=$(($cpt + 1))
	
	#cat src/ara/config | grep scope
	
done

#set init value
val2=$(($val2 - $step))
sed 's/protocol.emit.scope '$val2'/protocol.emit.scope '$val0'/g' -i ./src/ara/config 


#!/bin/bash

#input
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
h) echo $usage;  echo $config; exit 1
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

#read -p 'Path to directory where peersim is (format: /home/user/ara): ' lib

#get all java classes together
find -name "*.java" > allclass

#create bin directory
if [ -d "bin" ] 
then
    rm -r "bin"
fi

mkdir "bin"

#get peersim jars
p=$lib"/peersim-1.0.5/peersim-1.0.5.jar:"$lib"/peersim-1.0.5/djep-1.0.0.jar:"$lib"/peersim-1.0.5/jep-2.3.0.jar:"$lib"/peersim-1.0.5/peersim-doclet.jar"

#compile
javac -encoding ISO-8859-1 -classpath $p @allclass -d bin

#get Main
class=ara.util.LaunchMain 

#run
java -cp $p:./bin $class $mode

#remove
rm -r bin
rm allclass

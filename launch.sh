#!/bin/bash

#input
#lib="/home/n-ame/master2/ara"
read -p 'Path to directory where peersim is:' lib

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
java -cp $p:./bin $class


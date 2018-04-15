#!/bin/bash

if [ $# -lt 2 ]; then
    echo "$0 <base node number>, $1 number of groups"
    exit 1
fi

baseNode=$1
groupsize=4
ngroups=$2

for (( i = 0; i < $ngroups; i++ )); do
	mkdir group-$i
	cp system.config group-$i/
	touch group-$i/hosts.config
	echo "#group $i" > group-$i/hosts.config
	for (( j = 0; j < $groupsize; j++ )); do
		echo "$j node$baseNode 1${j}0${i}${j}" >> group-$i/hosts.config
		baseNode=$(( baseNode + 1 ))
	done
	
done

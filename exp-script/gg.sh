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
		num=$((baseNode + ( i * ( ngroups +1)) + j))
		echo "$j node$num 1${j}0${i}${j}" >> group-$i/hosts.config
	done
	echo $i
done
echo $baseNode
echo $ngroups
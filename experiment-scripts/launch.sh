#! /bin/bash

CMD="${@:2}"
for i in `cat $1`; do
	echo "ssh $i $CMD"
	ssh $i "PATH=$PATH:/sbin ; screen -d -m $CMD"
done
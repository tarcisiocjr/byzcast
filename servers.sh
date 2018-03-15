#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-ID>"
    exit 1
fi

G=$1 
N=4
ARGS="${@:2}"

tmux new-session -d -s bftamcast$G

for (( i = 1; i < $N; i++ )); do
    tmux split -h -t bftamcast$G
done

tmux select-layout -t bftamcast$G tiled

JAVA="java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast"

for (( j = 1; j <=$N; j++ )); do

        tmux send-keys -t bftamcast$G.$j "$JAVA.server.Server -t config/tree.conf -i $(( j-1 )) -g $G -G config/local$G $ARGS" C-m  
done

tmux attach-session -t bftamcast$G


# java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.server.Server -t config/tree.conf -i 0 -g 0 -G config/local0 $ARGS C-m
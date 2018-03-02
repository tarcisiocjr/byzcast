#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-NUMBER>"
    exit 1
fi

G=$1
N=4
ARGS="${@:2}"

tmux new-session -d -s bftamcast

for (( i = 1; i <= $N; i++ )); do
    for (( j = 1; j <=$(( G )); j++ )); do
            tmux split -h -t bftamcast
    done
    tmux select-layout -t bftamcast tiled
done

LCS="-lcs"
for (( i = 0; i < $(( G )); i++ )); do
    LCS="$LCS config/local$i"
done

JAVA="java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast"

for (( j = 1; j <=$(( G )); j++ )); do
    for (( i = 1; i <= $N; i++ )); do
        PANE=$(( (j-1)*N + i ))
        
        tmux send-keys -t bftamcast.$PANE "$JAVA.server.UniversalServer -t config/tree.conf -i $(( i-1 )) -g $(( j-1 )) -G config/local$(( j-1 )) $ARGS" C-m
        
    done
done

tmux attach-session -t bftamcast
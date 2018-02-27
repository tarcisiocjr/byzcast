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
    for (( j = 1; j <=$(( G+1 )); j++ )); do
            tmux split -h -t bftamcast
    done
    tmux select-layout -t bftamcast tiled
done

LCS="-lcs"
for (( i = 0; i < $(( G )); i++ )); do
    LCS="$LCS ../config/local$i"
done

JAVA="java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast"

for (( j = 1; j <=$(( G+1 )); j++ )); do
    for (( i = 1; i <= $N; i++ )); do
        PANE=$(( (j-1)*N + i ))
        if (( j == G+1 )); then
            # tmux send-keys -t bftamcast.$PANE echo global  $i 
            tmux send-keys -t bftamcast.$PANE "$JAVA.server.BatchServerGlobal -i $(( i-1 )) -gc ../config/global $LCS $ARGS" C-m
            # tmux send-keys -t bftamcast.$PANE "$JAVA.server.ServerGlobal -i $(( i-1 )) -gc ../config/global $LCS $ARGS" C-m
        else
        	# tmux send-keys -t bftamcast.$PANE echo $i $j 
            tmux send-keys -t bftamcast.$PANE "$JAVA.server.Server -i $(( i-1 )) -g $(( j-1 )) -lc ../config/local$(( j-1 )) $ARGS" C-m
        fi
    done
done

tmux attach-session -t bftamcast
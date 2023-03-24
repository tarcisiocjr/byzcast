#!/usr/bin/env bash

G=4
N=4
ARGS="${@:2}"

tmux kill-session -t bftamcast | true
find ../ -name "currentView" -exec rm -rf {} \;
tmux new-session -d -s bftamcast

for (( i = 1; i <= $N; i++ )); do
    for (( j = 1; j <=$(( G+1+G/2 )); j++ )); do
        tmux split -h -t bftamcast
        tmux select-layout -t bftamcast tiled
    done
done

LCS="-lcs"
for (( i = 0; i < $(( G )); i++ )); do
    LCS="$LCS ../config/original/local$i"
done

GCS="-gc ../config/original/global0"
for (( i = 1; i < $(( G/2 + 1 )) && G >= 4; i++ )); do
    GCS="$GCS ../config/original/global$i"
done


JAVA="java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast"

for (( j = 1; j <=$(( G+1+G/2 )); j++ )); do
    for (( i = 1; i <= $N; i++ )); do
        PANE=$(( (j-1)*N + i ))
        if (( j > G )); then
            tmux send-keys -t bftamcast.$PANE "$JAVA.server.BatchServerGlobal -g $(( j-G-1 )) -i $(( i-1 )) $GCS $LCS $ARGS " C-m #| tee log-global-$(( i-1 ))-$(( j-G-1 )).txt" C-m
        else
            tmux send-keys -t bftamcast.$PANE "$JAVA.server.Server -i $(( i-1 )) -g $(( j-1 )) -lc ../config/original/local$(( j-1 )) $ARGS " C-m #  | tee log-local-$(( i-1 ))-$(( j-1 )).txt" C-m
        fi
    done
done

# tmux attach-session -t bftamcast
tmux switch -t bftamcast
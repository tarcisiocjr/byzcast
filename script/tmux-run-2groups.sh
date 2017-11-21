#!/usr/bin/env bash


tmux new-session -d -s bftamcast
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux select-layout -t bftamcast tiled
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux split -h -t bftamcast
tmux select-layout -t bftamcast tiled

G=2
N=4

for i in `seq 1 $N`; do
   echo $i
done





tmux attach-session -t bftamcast
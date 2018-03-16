#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-ID>"
    exit 1
fi

G=$1 
N=4
ARGS="${@:2}"

JAVA="java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast"

for (( j = 0; j <$G; j++ )); do
	tmux new-session -d -s bftamcast$j
	tmux new-window 
	tmux select-layout tiled
	for (( i = 1; i < $N; i++ )); do
    	tmux split -h 
	done
	for (( i = 1; i < $N; i++ )); do
        tmux send-keys -t $i "$JAVA.server.Server -t config/tree.conf -i $(( i-1 )) -g $j -G config/local$j $ARGS" C-m  
    done
done




# for (( i = 0; i <$G; i++ )); do
# 	for (( j = 1; j <=$N; j++ )); do

#         tmux send-keys -t bftamcast$i.$j "$JAVA.server.Server -t config/tree.conf -i $(( j-1 )) -g $i -G config/local$i $ARGS" C-m  
#     done

# done

# for (( i = 0; i <$G; i++ )); do
# 	tmux attach-session -t bftamcast$i
#     tmux new-window
# done







# java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.server.Server -t config/tree.conf -i 0 -g 0 -G config/local0 $ARGS C-m
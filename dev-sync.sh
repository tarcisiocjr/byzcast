#!/usr/bin/env bash

G=4
N=4
ARGS="${@:2}"
RECOMPILE=true
CLIENT_TIMEOUT=30

byzcast_path="byzcast-sync"
JAVA="java -cp '$byzcast_path/lib/*:$byzcast_path/target/*' ch.usi.inf.dslab.bftamcast"

tmux kill-session -t bftamcast | true

if [ "$RECOMPILE" = true ] ; then
    make build-sync
fi

tmux new-session -d -s bftamcast

build_grid(){
    for (( i = 1; i <= $N; i++ )); do
        for (( j = 1; j <=$(( G+1+G/2 )); j++ )); do
            tmux split -h -t bftamcast
            tmux select-layout -t bftamcast tiled
        done
    done
}

start_replicas(){
    tmux send-keys -t bftamcast.17 "$JAVA.server.BatchServerGlobal -g 0 -i 0 -gc $byzcast_path/config/dev/g0 -lcs $byzcast_path/config/dev/g1" C-m
    tmux send-keys -t bftamcast.18 "$JAVA.server.BatchServerGlobal -g 0 -i 1 -gc $byzcast_path/config/dev/g0 -lcs $byzcast_path/config/dev/g1" C-m
    tmux send-keys -t bftamcast.19 "$JAVA.server.BatchServerGlobal -g 0 -i 2 -gc $byzcast_path/config/dev/g0 -lcs $byzcast_path/config/dev/g1" C-m
    tmux send-keys -t bftamcast.20 "$JAVA.server.BatchServerGlobal -g 0 -i 3 -gc $byzcast_path/config/dev/g0 -lcs $byzcast_path/config/dev/g1" C-m

    tmux send-keys -t bftamcast.21 "$JAVA.server.BatchServerGlobal -g 1 -i 0 -gc $byzcast_path/config/dev/g1 -lcs $byzcast_path/config/dev/g2" C-m
    tmux send-keys -t bftamcast.22 "$JAVA.server.BatchServerGlobal -g 1 -i 1 -gc $byzcast_path/config/dev/g1 -lcs $byzcast_path/config/dev/g2" C-m
    tmux send-keys -t bftamcast.23 "$JAVA.server.BatchServerGlobal -g 1 -i 2 -gc $byzcast_path/config/dev/g1 -lcs $byzcast_path/config/dev/g2" C-m
    tmux send-keys -t bftamcast.24 "$JAVA.server.BatchServerGlobal -g 1 -i 3 -gc $byzcast_path/config/dev/g1 -lcs $byzcast_path/config/dev/g2" C-m

    tmux send-keys -t bftamcast.13 "$JAVA.server.BatchServerGlobal -g 2 -i 0 -gc $byzcast_path/config/dev/g2 -lcs $byzcast_path/config/dev/g3" C-m
    tmux send-keys -t bftamcast.14 "$JAVA.server.BatchServerGlobal -g 2 -i 1 -gc $byzcast_path/config/dev/g2 -lcs $byzcast_path/config/dev/g3" C-m
    tmux send-keys -t bftamcast.15 "$JAVA.server.BatchServerGlobal -g 2 -i 2 -gc $byzcast_path/config/dev/g2 -lcs $byzcast_path/config/dev/g3" C-m
    tmux send-keys -t bftamcast.16 "$JAVA.server.BatchServerGlobal -g 2 -i 3 -gc $byzcast_path/config/dev/g2 -lcs $byzcast_path/config/dev/g3" C-m

    tmux send-keys -t bftamcast.25 "$JAVA.server.BatchServerGlobal -g 3 -i 0 -gc $byzcast_path/config/dev/g3 -lcs $byzcast_path/config/dev/g4" C-m
    tmux send-keys -t bftamcast.26 "$JAVA.server.BatchServerGlobal -g 3 -i 1 -gc $byzcast_path/config/dev/g3 -lcs $byzcast_path/config/dev/g4" C-m
    tmux send-keys -t bftamcast.27 "$JAVA.server.BatchServerGlobal -g 3 -i 2 -gc $byzcast_path/config/dev/g3 -lcs $byzcast_path/config/dev/g4" C-m
    tmux send-keys -t bftamcast.28 "$JAVA.server.BatchServerGlobal -g 3 -i 3 -gc $byzcast_path/config/dev/g3 -lcs $byzcast_path/config/dev/g4" C-m

    tmux send-keys -t bftamcast.9 "$JAVA.server.BatchServerGlobal -g 4 -i 0 -gc $byzcast_path/config/dev/g4 -lcs $byzcast_path/config/dev/g5" C-m
    tmux send-keys -t bftamcast.10 "$JAVA.server.BatchServerGlobal -g 4 -i 1 -gc $byzcast_path/config/dev/g4 -lcs $byzcast_path/config/dev/g5" C-m
    tmux send-keys -t bftamcast.11 "$JAVA.server.BatchServerGlobal -g 4 -i 2 -gc $byzcast_path/config/dev/g4 -lcs $byzcast_path/config/dev/g5" C-m
    tmux send-keys -t bftamcast.12 "$JAVA.server.BatchServerGlobal -g 4 -i 3 -gc $byzcast_path/config/dev/g4 -lcs $byzcast_path/config/dev/g5" C-m

    tmux send-keys -t bftamcast.1 "$JAVA.server.BatchServerGlobal -g 5 -i 0 -gc $byzcast_path/config/dev/g5 -lcs $byzcast_path/config/dev/g6" C-m
    tmux send-keys -t bftamcast.2 "$JAVA.server.BatchServerGlobal -g 5 -i 1 -gc $byzcast_path/config/dev/g5 -lcs $byzcast_path/config/dev/g6" C-m
    tmux send-keys -t bftamcast.3 "$JAVA.server.BatchServerGlobal -g 5 -i 2 -gc $byzcast_path/config/dev/g5 -lcs $byzcast_path/config/dev/g6" C-m
    tmux send-keys -t bftamcast.4 "$JAVA.server.BatchServerGlobal -g 5 -i 3 -gc $byzcast_path/config/dev/g5 -lcs $byzcast_path/config/dev/g6" C-m

    tmux send-keys -t bftamcast.5 "$JAVA.server.Server -i 0 -g 6 -lc $byzcast_path/config/dev/g6 -ng" C-m
    tmux send-keys -t bftamcast.6 "$JAVA.server.Server -i 1 -g 6 -lc $byzcast_path/config/dev/g6 -ng" C-m
    tmux send-keys -t bftamcast.7 "$JAVA.server.Server -i 2 -g 6 -lc $byzcast_path/config/dev/g6 -ng" C-m
    tmux send-keys -t bftamcast.8 "$JAVA.server.Server -i 3 -g 6 -lc $byzcast_path/config/dev/g6 -ng" C-m

}

start_clients(){
    tmux switch -t bftamcast
    tmux send-keys -t bftamcast.29 "$JAVA.client.Client -i $RANDOM -g 0 -gc $byzcast_path/config/dev/g0 -d $CLIENT_TIMEOUT -md 0"
}

stop_all(){
	echo "--- Stoping all"

	tmux kill-session -t bftamcast | true
	find . -name "currentView" -exec rm -rf {} \;
}

trap "stop_all; exit 255" SIGINT SIGTERM

build_grid
start_replicas
start_clients
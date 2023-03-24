#!/usr/bin/env bash

NUMBER_OF_GROUPS=3
NUMBER_OF_REPLICAS_PER_GROUPS=4
RECOMPILE=true
DEPLOY_CONFIG=true
CLIENT_TIMEOUT=120
ARGS="${@:2}"
G=$NUMBER_OF_GROUPS-2
N=$NUMBER_OF_REPLICAS_PER_GROUPS
CLASSNAME="Replica"

# exit when any command fails
set -e

# shell debug
set -x

if [ "$RECOMPILE" = true ]; then
  sleep 1s
  make build-async
  sleep 1s
fi



BYZCAST_PATH="byzcast-async"
BIN_REPLICA="java -Djava.security.properties=$BYZCAST_PATH/config/java.security -Dlogback.configurationFile=$BYZCAST_PATH/config/dev/logback.xml -classpath $BYZCAST_PATH/byzcast-async/target/byzcast-async-1.1-SNAPSHOT.jar ch.usi.inf.dslab.byzcast"
BIN_CLIENT="java -Djava.security.properties=$BYZCAST_PATH/config/java.security -Dlogback.configurationFile=$BYZCAST_PATH/config/dev/logback.xml -classpath $BYZCAST_PATH/byzcast-async/target/byzcast-async-1.1-SNAPSHOT.jar ch.usi.inf.dslab.byzcast"

if [ "$DEPLOY_CONFIG" = true ]; then
  for d in $BYZCAST_PATH/config/dev/*; do
    if [ -d "$d" ]; then
      cp $BYZCAST_PATH/config/system.config $d
    fi
  done
fi


build_grid() {
  for ((j = 0; j <= $((G + 1)); j++)); do
    for ((i = 1; i <= $N; i++)); do
      tmux split -h -t byzcast
      tmux select-layout -t byzcast tiled
    done
  done
}

start_experiment() {
  stop_all
  tmux new-session -d -s byzcast
  build_grid
}

stop_all() {
  sleep 1
  echo "--- Stoping all"
  tmux kill-session -t byzcast | true
  find . -name "currentView" -exec rm -rf {} \;
  sleep 1
}

start_replicas() {
  for ((j = 0; j <= $((G + 1)); j++)); do
    for ((i = 1; i <= $N; i++)); do
      PANE=$(((j) * N + (i - 1) +1))
      tmux send-keys -t byzcast.$PANE "$BIN_REPLICA.async.$CLASSNAME -i $((i - 1)) -g $((j)) -rp $BYZCAST_PATH/config/dev/ $ARGS" C-m
    done
  done
}

start_clients() {
  tmux switch -t byzcast
  tmux send-keys -t byzcast.$((PANE+1)) "$BIN_CLIENT.client.Client -lcaId 0 -lca $BYZCAST_PATH/config/dev/g0 -i $RANDOM -c1 -d $CLIENT_TIMEOUT -md 0 -sr"
}

start_experiment
start_replicas
start_clients

#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-NUMBER> <client-additional-arguments>"
    echo "For a tree A -> B; A -> C: $0 2 -g 1"
    exit 1
fi

G=$1
ARGS="${@:2}"

LCS="-lcs"
for (( i = 0; i < $(( G )); i++ )); do
    LCS="$LCS ../config/localhost/local$i"
done

java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client -gc ../config/localhost/global2 $LCS -i $RANDOM $ARGS
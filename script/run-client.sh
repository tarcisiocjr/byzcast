#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-NUMBER> <client-additional-arguments>"
    exit 1
fi

G=$1
ARGS="${@:2}"

LCS="-lcs"
for (( i = 0; i < $(( G )); i++ )); do
    LCS="$LCS ../config/local$i"
done

java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client -gc ../config/global $LCS -i $RANDOM $ARGS
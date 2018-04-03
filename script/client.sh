#!/usr/bin/env bash
if [ $# -lt 1 ]; then
    echo "$0 <Clients-#>"
    exit 1
fi


ARGS="${@:2}"

java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client -c $1 -i 0  -g 0 -t ../config/tree.conf $ARGS

#!/usr/bin/env bash

if [ "$GROUP_ID" = "0" ]; then
    echo "Starting as global replica."
    cd /root/bftswarmGlobal
    java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.ServerGlobal $LOCAL_NODE_ID $GROUP_ID "config" "../bftswarmLocal/config_1" "../bftswarmLocal/config_2" "../bftswarmLocal/config_3"
fi

if [ "$GROUP_ID" != "0" ]; then
    echo "Starting as local replica."
    cd /root/bftswarmLocal
    java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.Server $LOCAL_NODE_ID $GROUP_ID "./config"
fi


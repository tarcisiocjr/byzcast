#!/usr/bin/env bash

java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.server.Server -t config/tree.conf -i 1 -g 0 -lc config/local0
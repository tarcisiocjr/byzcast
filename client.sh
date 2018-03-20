#!/usr/bin/env bash

java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.client.Client -c $1 -i 0  -g 0 -t config/tree.conf

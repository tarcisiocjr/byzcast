#!/usr/bin/env bash

java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.client.Client -i 0  -g 0 -t config/tree.conf

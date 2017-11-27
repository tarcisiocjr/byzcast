#!/usr/bin/env bash

java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client \
   -i $RANDOM -g 0 -gc ../config/global0 ../config/global1 ../config/global2 \
   -lcs ../config/local0 ../config/local1 ../config/local2 ../config/local3 $@

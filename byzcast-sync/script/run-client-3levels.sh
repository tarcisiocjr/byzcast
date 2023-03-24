#!/usr/bin/env bash

 java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client \
    -i $RANDOM -g 0 -gc ../config/original/global0 ../config/original/global1 ../config/original/global2 \
    -lcs ../config/original/local0 ../config/original/local1 ../config/original/local2 ../config/original/local3 $@
#
#java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client \
#   -i $RANDOM -c 4 -g 4 -gc ../config/original/global4 $@
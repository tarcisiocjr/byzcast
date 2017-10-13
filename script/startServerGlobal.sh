#!/bin/bash
cd /root/bftswarmGlobal
java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.ServerGlobal $GLOBAL_NODE_ID $GROUP_ID "../bftswarmLocal/config" "./config"
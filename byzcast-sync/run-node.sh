#!/bin/bash

# exit when any command fails
set -e

# shell debug
set -x

IP=`ifconfig enp4s0 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`
IP="192.168.3.11"

if [ "$IP" = "" ]; then
  echo "No IP found, exiting!"
  exit
fi

echo "Using IP '$IP'" 

LOCALGROUPS=""
GLOBALGROUPS=""
cd config
for i in `ls -d group*`; do
  echo $i
  if [ "${i:0:7}" != "group-g" ]; then
    LOCALGROUPS=$LOCALGROUPS"$i "
  else
    GLOBALGROUPS=$GLOBALGROUPS"$i "
  fi

  TEMP_N_ID=`cat config/$i/hosts.config |grep -w $IP| awk '{print $1}'`
  if [ "$TEMP_N_ID" != "" ]; then
    N_ID=$TEMP_N_ID
    G_ID=`cat $i/hosts.config |grep "#group"| awk '{print $2}'`
    echo "Folder $i, node id ${N_ID}, group id ${G_ID}"
    #break
  fi
done

if [ "$G_ID" = "" ] || [ "$N_ID" = "" ]; then
  echo "Group/Node id mismatch, exiting!"
  exit
fi

if [ "${G_ID:0:1}" = "g" ]; then 
    echo "GLOBAL SERVER"
    java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.server.BatchServerGlobal -i $N_ID -g ${G_ID:1:1} -gc $GLOBALGROUPS -lcs $LOCALGROUPS
else
    echo "LOCAL SERVER"
    java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.server.Server -i $N_ID -g $G_ID -lc group-$G_ID
fi

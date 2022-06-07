#!/bin/bash

IP=`ifconfig enp8s0d1 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`

if [ "$IP" = "" ]; then
  IP=`ifconfig enp4s0 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`
fi

if [ "$IP" = "" ]; then
  IP=`ifconfig em1 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`
fi

if [ "$IP" = "" ]; then
  echo "No IP found, exiting!"
  exit 1
fi

echo "Using IP '$IP'"

G_ID=`cat zones.txt | grep $IP |cut -f 5`

LOCALGROUPS="-lcs "
GLOBALGROUPS=""
for i in `ls -d group*`; do
  if [ "${i:0:7}" != "group-g" ]; then
    LOCALGROUPS=$LOCALGROUPS"$i "
  else
    GLOBALGROUPS=$GLOBALGROUPS"$i "
  fi
done

if [ "$GLOBALGROUPS" = "" ]; then
  echo "Single group configuration"
  G_ID=0
  GLOBALGROUPS="${LOCALGROUPS:5}"
  LOCALGROUPS=""
fi

if [ "$G_ID" = "" ]; then
  echo "Group id mismatch, exiting!"
  exit
fi

echo "Running $QTY clients in group $G_ID..."
java -cp '../target/*:../lib/*' ch.usi.inf.dslab.bftamcast.client.Client -i $RANDOM -g $G_ID -gc $GLOBALGROUPS $LOCALGROUPS $@

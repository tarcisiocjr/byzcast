#!/bin/bash

IP=`ifconfig enp1s0 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`
# IP="192.168.3.32"

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
for i in `ls -d group*`; do
  if [ "$i" != "group-g" ]; then
    LOCALGROUPS=$LOCALGROUPS"$i "
  fi
done

if [ "$LOCALGROUPS" = "-lcs " ]; then
  echo "Single group configuration"
  G_ID=0
  LOCALGROUPS=""
fi

if [ "$G_ID" = "" ]; then
  echo "Group id mismatch, exiting!"
  exit
fi

echo "Running $QTY clients in group $G_ID..."
java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.client.Client -i $RANDOM -g $G_ID -gc group-g $LOCALGROUPS $@


#java -cp '../lib/*:../target/*' ch.usi.inf.dslab.bftamcast.client.Client -c $1 -i 0  -g 0 -t ../config/tree.conf $ARGS


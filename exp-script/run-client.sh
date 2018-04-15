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

G_ID=0

echo "Running $QTY clients in group $G_ID..."
java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.client.Client -i 0 -g $G_ID -t tree.conf $@



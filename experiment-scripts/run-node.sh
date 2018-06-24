#!/bin/bash

# IP=`ifconfig enp4s0 | grep "inet " | awk  '{print $2}'| awk '{print $1}'`
# #IP="192.168.3.11"

# if [ "$IP" = "" ]; then
#   echo "No IP found, exiting!"
#   exit
# fi

# echo "Using IP '$IP'" 
name=`hostname`
echo "Using name '$name'" 

 for i in `ls -d group*`; do
   N_ID=`cat $i/hosts.config |grep -w $name| awk '{print $1}'`
   if [ "$N_ID" != "" ]; then
     G_ID=`cat $i/hosts.config |grep "#group"| awk '{print $2}'`
     echo "Folder $i, node id ${N_ID}, group id ${G_ID}"
     break
   fi
 done

if [ "$G_ID" = "" ] || [ "$N_ID" = "" ]; then
  echo "Group/Node id mismatch, exiting!"
  exit
fi


java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.server.Server -i $N_ID -g $G_ID -t tree.conf $@

# if [ "$LOCALGROUPS" = "" ]; then 
#     G_ID=0
# fi

# if [ "$G_ID" = "g" ]; then 
#     echo "GLOBAL SERVER"
#     # java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.server.BatchServerGlobal -i $N_ID -gc group-g -lcs $LOCALGROUPS $@
# else
#     echo "LOCAL SERVER"
#     # java -cp 'target/*:lib/*' ch.usi.inf.dslab.bftamcast.server.Server -i $N_ID -g $G_ID -lc group-$G_ID $@
# fi

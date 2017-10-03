#!/usr/bin/env bash
cd /root/

LOCAL_CONFIG='./bftswarmLocal/config/hosts.config'
GLOBAL_CONFIG='./bftswarmGlobal/config/hosts.config'
LOCALHOST='127.0.0.1'

while IFS=: read group_id local_node_id global_node_id ip_address local_node_port global_node_port; do

	if [ "$global_node_id" = "$GLOBAL_NODE_ID" ]; then
		echo $global_node_id $LOCALHOST $global_node_port >> $GLOBAL_CONFIG
	else
		echo $global_node_id $ip_address $global_node_port >> $GLOBAL_CONFIG
	fi
		
	if [ "$group_id" = "$GROUP_ID" ]; then
		if [ "$local_node_id" = "$LOCAL_NODE_ID" ]; then
			echo $local_node_id $LOCALHOST $local_node_port >> $LOCAL_CONFIG
		else
			echo $local_node_id $ip_address $local_node_port >> $LOCAL_CONFIG
		fi
	fi
done < config
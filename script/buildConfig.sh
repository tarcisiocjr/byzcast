#!/usr/bin/env bash
cd /root/

LOCAL_CONFIG='./bftswarmLocal/config/hosts.config'
GLOBAL_CONFIG='./bftswarmGlobal/config/hosts.config'

rm -f LOCAL_CONFIG
rm -f GLOBAL_CONFIG

while IFS=$'\t' read -r group_id local_node_id global_node_id ip_address local_node_port hostname_deploy hostname_dev; do

    case $group_id in
        ''|\#*) continue ;; # skip blank lines and lines starting with #
    esac

    if [ "$DEV" = "1" ]; then

        if [ "$group_id" = "$GROUP_ID" ]; then
            echo $local_node_id $hostname_dev $local_node_port >> $LOCAL_CONFIG
        fi

        echo $global_node_id $hostname_dev $global_node_port >> $GLOBAL_CONFIG

    else

        if [ "$group_id" = "$GROUP_ID" ]; then
            echo $local_node_id $ip_address $local_node_port >> $LOCAL_CONFIG
        fi

        echo $global_node_id $ip_address $global_node_port >> $GLOBAL_CONFIG
    fi

done < config
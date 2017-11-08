#!/usr/bin/env bash
cd /root/

LOCAL_CONFIG='./bftswarmLocal/config/hosts.config'
GLOBAL_CONFIG='./bftswarmGlobal/config/hosts.config'
PWD_CONFIG='./bftswarmLocal/config'
SYSTEM_CONFIG='./bftswarmLocal/config/system.config'

#rm -f LOCAL_CONFIG
#rm -f GLOBAL_CONFIG

while IFS=$'\t' read -r group_id local_node_id global_node_id ip_address local_node_port hostname_deploy hostname_dev; do

    case ${group_id} in
        ''|\#*) continue ;; # skip blank lines and comments
    esac

    if [ "$DEV" = "1" ] || [ "$HOSTNAME" = "moby" ]; then

        if [ "$group_id" = "0" ]; then
            echo ${local_node_id} ${hostname_dev} ${local_node_port} >> ${GLOBAL_CONFIG}
        elif [ "$group_id" != "0" ]; then
            mkdir -p ${PWD_CONFIG}"_"${group_id}
            echo ${local_node_id} ${hostname_dev} ${local_node_port} >> ${PWD_CONFIG}"_"${group_id}/hosts.config
            cp ${SYSTEM_CONFIG} ${PWD_CONFIG}"_"${group_id}/
        fi


        if [ "${GROUP_ID}" = "${group_id}" ] && [ "${GROUP_ID}" != "0" ]; then
            if [ "$group_id" = "${GROUP_ID}" ]; then
                echo ${local_node_id} ${hostname_dev} ${local_node_port} >> ${LOCAL_CONFIG}
            fi
        fi

    else

        if [ "$group_id" = "0" ]; then
            echo ${local_node_id} ${ip_address} ${local_node_port} >> ${GLOBAL_CONFIG}
        elif [ "$group_id" != "0" ]; then
            mkdir -p ${PWD_CONFIG}"_"${group_id}
            echo ${local_node_id} ${ip_address} ${local_node_port} >> ${PWD_CONFIG}"_"${group_id}/hosts.config
            cp ${SYSTEM_CONFIG} ${PWD_CONFIG}"_"${group_id}/
        fi

        if [ "${GROUP_ID}" = "${group_id}" ] && [ "${GROUP_ID}" != "0" ]; then
            if [ "$group_id" = "${GROUP_ID}" ]; then
                echo ${local_node_id} ${ip_address} ${local_node_port} >> ${LOCAL_CONFIG}
            fi
        fi
    fi

done < config

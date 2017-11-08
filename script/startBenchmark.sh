#!/usr/bin/env bash
cd /root/bftswarmProxy

cp -r /root/bftswarmLocal/config config-local
cp -r /root/bftswarmGlobal/config config-global

while IFS=$'\t' read hostname client_id local_group_id num_of_clients num_of_groups; do

    case ${hostname} in
        ''|\#*) continue ;; # skip blank lines and lines starting with #
    esac

	if [ "$hostname" = "$HOSTNAME" ]; then
	    echo "Starting benchmark"
        java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.ClientThroughput $client_id $local_group_id $num_of_clients $num_of_groups "config-local" "config-global" false 20000
	fi

done < /root/configBenchmark

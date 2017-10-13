#!/usr/bin/env bash
cd /root/bftswarmProxy

cp -r /root/bftswarmLocal/config config-local
cp -r /root/bftswarmGlobal/config config-global

while IFS=: read hostname start_client stop_client groups messages; do

	if [ "$hostname" = "$HOSTNAME" ]; then
        for i in $(eval echo "{$start_client..$stop_client}"); do
            output=$i$
            java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.Client $i $groups "config-local" "config-global" > /tmp/$i'_'$groups.txt &
        done
	fi

done < /root/configClient
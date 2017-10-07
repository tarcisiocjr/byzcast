#!/usr/bin/env bash
cd /root/bftswarmLocal

while IFS=: read hostname start_client stop_client groups messages; do

	if [ "$hostname" = "$HOSTNAME" ]; then
        for i in $(eval echo "{$start_client..$stop_client}"); do
            output=$i$
            java -cp '../lib/*:bin/*' ch.usi.dslab.ceolin.bftswarm.Client $i $groups $messages "./config" "../bftswarmGlobal/config" >> /tmp/$i'_'$groups'_'$messages.txt &
        done
	fi

done < /root/configClient
#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-#>"
    exit 1
fi

for (( i = 0; i < $1; i++ )); do
	# osascript -e 'tell app "Terminal"
	# 	do script "echo " 
	# end tell' $i

	osascript -e 'tell application "Terminal" to do script "t;cd script/ ;./groupC.sh '$i'"'
	sleep 1
done

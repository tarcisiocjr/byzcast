#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "$0 <GROUPS-#>"
    exit 1
fi
# rm tmpscrit*

for (( i = 0; i < $1; i++ )); do
	touch tmpscrit$i.sh
	echo "">tmpscrit$i.sh
	echo "cd /Users/chris/Documents/byzcast/" >> tmpscrit$i.sh
	echo "G=$i " >> tmpscrit$i.sh
	echo "N=4 " >> tmpscrit$i.sh
	echo "JAVA=\"java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast\" " >> tmpscrit$i.sh
	echo "tmux new-session -d -s bftamcast\$G " >> tmpscrit$i.sh
	echo "for (( i = 1; i < \$N; i++ )); do " >> tmpscrit$i.sh
	echo "tmux split -h " >> tmpscrit$i.sh
	echo "done " >> tmpscrit$i.sh
	echo "tmux select-layout tiled " >> tmpscrit$i.sh
	echo "for (( j = 0; j <\$N; j++ )); do" >> tmpscrit$i.sh
	echo "tmux send-keys -t bftamcast\$G.\$j \"\$JAVA.server.Server -t config/tree.conf -i \$j -g \$G -G config/local\$G \" C-m " >> tmpscrit$i.sh
	echo "done " >> tmpscrit$i.sh
	echo "tmux attach-session -t bftamcast\$G " >> tmpscrit$i.sh
	chmod u+x tmpscrit$i.sh
	sleep 2
	open -a /Applications/Utilities/Terminal.app/ tmpscrit$i.sh

done
# G=$1 
# N=4
# ARGS="${@:2}"

# JAVA="java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast"

# tmux new-session -d -s bftamcast$G

	
# for (( i = 1; i < $N; i++ )); do
#     tmux split -h 
# done


# tmux select-layout tiled




# for (( j = 0; j <$N; j++ )); do

#     tmux send-keys -t bftamcast$G.$j "$JAVA.server.Server -t config/tree.conf -i $j -g $G -G config/local$G $ARGS" C-m  
 
# done

# tmux attach-session -t bftamcast$G
sleep 4
rm tmpscrit*








# java -cp 'lib/*:target/*' ch.usi.inf.dslab.bftamcast.server.Server -t config/tree.conf -i 0 -g 0 -G config/local0 $ARGS C-m
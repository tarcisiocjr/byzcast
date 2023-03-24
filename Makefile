build-async: kill
	cd byzcast-async && mvn clean && mvn package -DskipTests

build-sync: kill
	cd byzcast-sync && mvn clean && mvn package -DskipTests

clean:
	find . -name "currentView" -exec rm -rf {} \;
	sleep 1s

kill: clean
	tmux list-sessions | grep -v attached | cut -d: -f1 |  xargs -t -n1 tmux kill-session -t | true

dev-sync: clean
	./dev-sync.sh

dev-async: clean
	./dev-async.sh

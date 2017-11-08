# Yes, a Makefile. Because I am old.

all: build down up

build:
	ant

up:
	docker-compose up -d --build --no-deps

down:
	docker-compose down --remove-orphans

log:
	docker-compose logs -f

proxy:
	docker-compose -f docker-compose.proxy.yml up -d --build --no-dep group_0_proxy_0

console:
	docker exec -it bftswarm_group_0_proxy_0_1 bash

dist:
	rm -rf dist ; ant dist

deploy:
	rm -rf dist ; ant dist ; rsync -av --progress --exclude=".*" --exclude="/src/" --exclude="/tmp/" . dslab.inf.usi.ch:~/bftswarm/ --delete

count:
	@find src -name \*.java | xargs wc -l | sort -n

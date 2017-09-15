# Yes, a Makefile. Because I am old.

all: build down up

build:
	ant

clean:
	ant clean

up: 
	docker-compose up -d --build --no-deps

log:
	docker-compose logs -f

down:
	docker-compose down --remove-orphans

proxy:
	docker-compose -f docker-compose.proxy.yml up -d --build --no-dep

console:
	docker exec -it g0_proxy bash

count:
	@find src -name \*.java | xargs wc -l | sort -n

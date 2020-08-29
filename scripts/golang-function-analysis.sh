#!/usr/bin/env bash

go_container=`docker ps --format "table {{.Names}}"|awk "{print $2}"|grep golang`
echo $1
cmd="/root/golang-longfuncs.sh"
docker cp golang-longfuncs.sh $go_container:/root/
docker exec $go_container $cmd $1| sed 's/\s\{2,\}/,/g' | sed 's/^,//g'



#!/usr/bin/env bash

go_container=`docker ps --format 'table {{.ID}} {{.ID}} {{.Names}}'|awk '{print $3}'|grep golang`
cmd="sh /root/"$2
docker cp golang-longfuncs.sh $go_container:/root/
docker cp /tmp/$2 $go_container:/root/
docker exec $go_container $cmd $1| sed 's/\s\{2,\}/,/g' | sed 's/^,//g'
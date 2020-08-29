#!/usr/bin/env bash

go_container=`docker ps --format "table {{.Names}}"|awk "{print $2}"|grep golang`
cmd="golongfuncs -top 300 +lines +in_params +complexity +complexity/lines"
docker exec $go_container $cmd | sed 's/\s\{2,\}/,/g' | sed 's/^,//g'


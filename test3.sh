#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg 2>./err-log.txt >/dev/null
time2=$(date +%s)
# wc -l log-${time1}
singleconnection=`awk "BEGIN { print ${time2} - ${time1} }"`
rps=`awk "BEGIN { print 500000.0/(${time2} - ${time1}) }"`
spr=`awk "BEGIN { print (${time2} - ${time1})/500000.0 }"`

echo "Time to perform 5kk request on a single connection: ${singleconnection}. Requests per second: ${rps}. Seconds per request: ${spr}."

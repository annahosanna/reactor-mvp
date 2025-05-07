#!/bin/bash

reps=5000
repsdec=${reps}".0"
echo 'time1=$(date +%s)' > ./test3.sh
echo 'curl -K ./test3.cfg 2>./err-log.txt >/dev/null' >> ./test3.sh
echo 'time2=$(date +%s)' >> ./test3.sh
echo 'singleconnection=`awk "BEGIN { print ${time2} - ${time1} }"`' >> ./test3.sh
echo 'rps=`awk "BEGIN { print '${repsdec}'/(${time2} - ${time1}) }"`' >> ./test3.sh
echo 'spr=`awk "BEGIN { print (${time2} - ${time1})/'${repsdec}' }"`' >> ./test3.sh
echo 'echo "Time to perform '${reps}' request on a single connection: ${singleconnection}. Requests per second: ${rps}. Seconds per request: ${spr}."' >> ./test3.sh

cat << EOF > ./test3.cfg
cacert = ./certs.pem
data = "k1=v1&k2=\"\"&k3=&k4=%00"
request = "POST"
header = "Content-Type:application/x-www-form-urlencoded"
parallel
EOF

hostname=$(hostname)
for i in {1..500000}
do
echo "url = http://${hostname}/fortune" >> ./test3.cfg
done

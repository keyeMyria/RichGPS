#!/bin/bash

for i in {1..20}
do
  let "lat = $RANDOM % 1000"
  lat=$(echo "($lat / 1000) + 57" | bc -l)
  let "lng = $RANDOM % 1000"
  lng=$(echo "($lng / 1000) - 3" | bc -l)

  (sleep 1; echo "geo fix $lng $lat"; sleep 1; echo "exit") | telnet localhost 5554 > /dev/null
  echo "Iteration $i"
  sleep 15
done

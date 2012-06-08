#!/bin/bash

CWD=$(pwd)
NODES=$1

if [ -z $NODES ];
then
  echo "Using default number of nodes (4) to run the demo. You can pass it as an argument"
  NODES=4
fi

echo "Running "$NODES" nodes"
for i in $(seq 1 $NODES); do xterm -e "sh "$CWD"/run.sh 127.0.0.1 555"$i" 127.0.0.1:5551" & disown && sleep 30; done

#!/bin/bash

ADDRESS="127.0.0.1"
PORT="5678"
BOOTSTRAP="127.0.0.1:5678"

if [ $1 ]; then
	ADDRESS=$1
else 
	echo "Typical usage run <bind address> <bind port> <bootstrap address:port>. Using Default: run $ADDRESS $PORT $BOOTSTRAP"
fi 

if [ $2 ]; then
	PORT=$2
fi

if [ $3 ]; then
	BOOTSTRAP=$3
fi
	
CLASSPATH=lib/*:build/classes: 

java -cp $CLASSPATH com.bt.pi.core.Main -p $PORT -b $BOOTSTRAP -a $ADDRESS -s true

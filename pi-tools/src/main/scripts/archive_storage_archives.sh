#!/bin/bash

nodeid=`cat /opt/pi/var/run/nodeId.txt`
mkdir -p /pifs/storage_archive/${nodeid}

find /state/partition1/pi/storage_archive/${nodeid} -type d -mtime +2 |
while read line
do
    base=`basename $line`
    if [ ${#base} == 8 ]; then
        tar czf ${line}.tgz ${line}
        mv -v ${line}.tgz /pifs/storage_archive/${nodeid}
	    rm -rf $line
    fi
done


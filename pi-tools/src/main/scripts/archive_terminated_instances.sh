#!/bin/bash

PI_HOME=/opt/pi

find -L /opt/pi/var/instances -name "*crashed*" -or -name "*terminated*" -type d -mtime +14 | xargs rm -rf
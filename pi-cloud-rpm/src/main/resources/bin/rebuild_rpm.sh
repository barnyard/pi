#!/bin/bash

if [ $# -lt 3 ];
then
  echo ">>> <new-rpm-version> <old-rpm-version> <hostname>"
  exit 1
fi

NEWRPMVER=$1
OLDRPMVER=$2
HOSTNAME=$3

TIMESTAMP=$(date +"%d%m%Y%H%M%S")
PWD=$(pwd)
RPMDIR=$PWD/rpmdir
NEWRPMDIR=$PWD/new_rpm
OLDRPMDIR=$PWD/old_rpm

echo ">>> Cleanup old directories and creating required directories"
rm -rvf $RPMDIR 
rm -rvf $NEWRPMDIR
rm -rvf $OLDRPMDIR

mkdir -vp $RPMDIR
mkdir -vp $RPMDIR/BUILD
mkdir -vp $RPMDIR/SOURCES
mkdir -vp $RPMDIR/SRPMS
mkdir -vp $RPMDIR/RPMS/noarch

mkdir -vp $NEWRPMDIR
mkdir -vp $OLDRPMDIR

echo ">>> Extracting New RPM"
rpm2cpio pi-cloud-*-$NEWRPMVER.noarch.rpm| cpio -idm "*$HOSTNAME*" && mv opt $NEWRPMDIR

echo ">>> Extracting Old RPM"
rpm2cpio pi-cloud-*-$OLDRPMVER.noarch.rpm| cpio -idm && mv opt $OLDRPMDIR

echo ">>> Copying the files from new RPM to old RPM"
for fi in $(find new_rpm/* -type f); do OLD_DIR=$(dirname "$fi" | sed 's/new/old/'); echo "cp -v $fi $OLD_DIR" | sh; done

echo ">>> Updating the version for old RPM"
sed -i "/Release:/s/$/_$TIMESTAMP/g" $OLDRPMDIR/opt/pi/current/pi-cloud.spec

echo ">>> Rebuilding Rpm"
rpmbuild --define="_topdir $RPMDIR" --target noarch --buildroot=$OLDRPMDIR -bb $OLDRPMDIR/opt/pi/current/pi-cloud.spec

echo ">>>> The updated rpm is in $RPMDIR/RPMS/noarch directory <<<<"


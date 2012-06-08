#!/bin/bash

d=$(date +"%Y%m%d")

usage()
{
cat << EOF
usage $0 options

This script saves an instance to the gluster filesystem
 
OPTIONS:

  -h    Show this message
  -f    (Optional) Do not copy the state of the instance when migrating from the source vm-container
  -i    Instance ID
  -m    (Optional) Only use this if you are migrating the instance. This will set the instance directory to be terminated
EOF
}

INSTANCE=
INSTANCE_DIR=
SHARE_DIR=pifs/migrations
INSTANCES_DIR=/opt/pi/var/instances
SKIP_STATE=false
MIGRATION=false

while getopts "hmfdi:" OPTION
do
    case $OPTION in
        h)  
            usage
            exit 1
            ;;
        f)  
            SKIP_STATE=true
            ;;
        i)  
            INSTANCE=$OPTARG
            ;;
        d)  
            INSTANCE_DIR=$OPTARG
            ;;
        m)
            MIGRATION=true
            ;;
        ?)
            usage
            exit
            ;;
    esac
done

if [[ -z $INSTANCE ]]
then
    usage
    exit 1
fi

run_source()
{
        if $SKIP_STATE;
        then
            echo "2. Forcing instance to stop"
            xm shutdown $INSTANCE || ( echo "Unable to shutdown instance $INSTANCE"; exit 1; )
        else
            echo "2. Saving the $INSTANCE state"
            echo "Saving the instance state in Instance directory:" $INSTANCE_DIR
            virsh dumpxml $INSTANCE > $INSTANCE_DIR/libvirt.xml.$d
            echo "Copying any attached volumes to remote directory"
            awk -F\' '/source.*volumes/ {print $2}' $INSTANCE_DIR/libvirt.xml.$d > $INSTANCE_DIR/vols
            cat $INSTANCE_DIR/vols | while read line; do cp -v $line /opt/pi/var/volumes/remote; done
            echo "Saving instance $INSTANCE state to $INSTANCE_DIR/checkpoint-$d"
            xm save $INSTANCE $INSTANCE_DIR/checkpoint-$d || ( echo "Unable to save instance $INSTANCE state"; exit 1; )
        fi

        echo "Moving files to $SHARE_DIR"
        USER_DIR=`dirname $INSTANCE_DIR`
        echo "User directory is:" $USER_DIR

        echo "Tar instance directory to $SHARE_DIR"
        mkdir -vp /$SHARE_DIR
        cd $USER_DIR
        tar zcvf /$SHARE_DIR/$INSTANCE-$d.tar.gz $INSTANCE || ( echo "Unable to tar $INSTANCE_DIR"; exit 1; )

        if $MIGRATION;
        then
            echo "Renaming instance directory to .terminated"
            mv -v $INSTANCE_DIR $INSTANCE_DIR"-terminated-"$d
        fi
}

INSTANCE_DIR=`find $INSTANCES_DIR/* -name $INSTANCE`
run_source
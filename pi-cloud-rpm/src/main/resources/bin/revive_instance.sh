#!/bin/bash

d=$(date +"%Y%m%d")

usage()
{
cat << EOF
usage $0 options

This script retrieves an instance from a directory
 
OPTIONS:

  -h    Show this message
  -f    (Optional) Do not copy the state of the instance when migrating from the source vm-container
  -i    Instance ID
  -d    User Directory
  -r    Restore existing instance. Just use this switch with instance id if recovering from a vm-container reboot
EOF
}

INSTANCE=
USER_DIR=
INSTANCE_DIR=
INSTANCES_DIR=/opt/pi/var/instances
SHARE_DIR=pifs/migrations
REMOTE_VOLUMES_DIR=/opt/pi/var/volumes/remote
SKIP_STATE=false
RESTORE=false

while getopts "hrfd:i:" OPTION
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
            USER_DIR=$OPTARG
            ;;
        r)
            RESTORE=true
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

if [[ -z $USER_DIR ]]
then
    INSTANCE_DIR=`find $INSTANCES_DIR/* -name $INSTANCE`
    USER_DIR=`dirname $INSTANCE_DIR`
fi

if [[ -z $INSTANCE_DIR ]]
then
    INSTANCE_DIR="$USER_DIR/$INSTANCE"
fi

retrieve_vlan()
{
        echo "Getting vlan id for $INSTANCE"
        VLANID=`/opt/pi/current/etc/bin/pi-find-instance $INSTANCE | grep vlanId | awk '{print $3}' | sed 's/,//g'`

        if [ -z $VLANID ];
        then
            echo "Unable to get VLAN ID for the Instance."
            exit 1
        fi
}

copy_volumes()
{
        echo "Copying volumes from $REMOTE_VOLUMES_DIR"
        cat $INSTANCE_DIR/vols | while read line; do VOLNAME=`basename $line`;cp -v $REMOTE_VOLUMES_DIR/$VOLNAME $line ; done
}

run_target()
{
        echo "Setting up Pi bridge for $VLANID"
        brctl show | grep pibr$VLANID || ( echo "Adding bridge pibr$VLANID";brctl addbr pibr$VLANID )
        ip link set dev pibr$VLANID up

        if $RESTORE;
        then
            copy_volumes
            if [[ -e $INSTANCE_DIR/checkpoint-$d ]]
            then
                echo "Restoring instance $INSTANCE from checkpoint-$d"
                xm restore $INSTANCE_DIR/checkpoint-$d || ( echo "Unable to restore instance checkpoint $INSTANCE_DIR/checkpoint-$d"; exit 1; )
            else
                read -p "Checkpoint file doesn't exist therefore not able to restore the instance. Do you want to start a fresh copy of the instance using virsh create now? [yn]" -n 1 -s ; echo
                if [ "$REPLY" != "y" ] ; then
                    echo "Ok. Not starting up the instance but you are on your own now :)."
                    exit 0
                fi

                echo "Starting up instance from $INSTANCE_DIR/libvirt.xml"
                virsh create $INSTANCE_DIR/libvirt.xml || ( echo "Unable to start instance $INSTANCE"; exit 1; )
            fi
            exit 0
        fi

        echo "User directory is:" $USER_DIR
        echo "Untar in $USER_DIR"
        mkdir -p $USER_DIR
    	tar zxvf /$SHARE_DIR/$INSTANCE-$d.tar.gz -C $USER_DIR || ( echo "Unable to untar /$SHARE_DIR/$INSTANCE-$d.tar.gz"; exit 1; )

        copy_volumes

        if $SKIP_STATE;
        then
            echo "As we skipped saving state for instance $INSTANCE. We are going to run use virsh to bring up that instance."
            virsh create $INSTANCE_DIR/libvirt.xml || ( echo "Unable to start instance $INSTANCE"; exit 1; )
        else
            echo "Restoring instance $INSTANCE"
            xm restore $INSTANCE_DIR/checkpoint-$d || ( echo "Unable to restore instance checkpoint $INSTANCE_DIR/checkpoint-$d"; exit 1; )
        fi

        echo "Checking if the instance is running on $TARGET >>>>>"
        xm list $INSTANCE || ( echo "Unable to start instance $INSTANCE"; exit 1; )
        echo "<<<<< The Instance $INSTANCE is successfully migrated >>>>>"
}

retrieve_vlan
run_target
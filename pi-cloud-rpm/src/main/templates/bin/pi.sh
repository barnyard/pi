#!/bin/sh
# chkconfig: 2345 99 00
# description: Start script for the p2p core
# This script starts and stops the koala pastry node process.
# processname: pi
APPNAME=pi
LOCKFILE=/var/run/$APPNAME.lock
case "$1" in
  start)
        # Start daemon.
        if [ -e $LOCKFILE ]; then
                echo Apparently $APPNAME is already running - if not, please delete $LOCKFILE
                exit 1
        fi

        echo -n "Starting Pi Node: "
        chmod 0400 @installDir@/conf/management.params
        export MKE2FS_SYNC=10;
        export CLUSTER=`cat /etc/hosts | grep -v $HOSTNAME | grep -v -- "#" | grep -v localhost | awk '{print $2}'`
        cd @installDir@
        ##
        ## we need to ensure that the pi-sss jar is in front of the Grizzly jar as we have overridden a class
        ## to stop it complaining about duplicate Content-Length headers
        ##
        pisssjar=`ls -b @installDir@/current/lib/pi-sss*.jar`
        CLASSPATH=@installDir@/conf:${pisssjar}:@installDir@/current/lib/*: 
        nice -n-7 /usr/java/latest/bin/java -Xmx1024m -Dcom.sun.management.jmxremote.port=@jmxPort@ -Djavax.net.ssl.keyStore=@installDir@/conf/koalaJmx.keystore -Djavax.net.ssl.keyStorePassword=password -Djavax.net.ssl.trustStore=@installDir@/conf/koalaJmx.truststore -Djavax.net.ssl.trustStorePassword=password -Dcom.sun.management.jmxremote.password.file=@installDir@/conf/management.params -cp $CLASSPATH com.bt.pi.core.Main >> @installDir@/var/log/output.log 2>&1 &
        RETVAL=$?
        echo OK        
        [ $RETVAL = 0 ] && touch $LOCKFILE
        ;;
  stop)
        # Stop daemon.
		RETVAL=0
		RETVAL=`ps -ef | grep com.bt.pi.core.Main | grep -v grep`
		if [ $? -eq 0 ]; then
			PID=`ps -ef | grep com.bt.pi.core.Main | grep -v grep | awk '{ print $2}'`
			kill $PID
			RETVAL=$?
			echo "OK"
			rm -rfv $LOCKFILE 2>&1
		else
			echo "$APPNAME not running"
			rm -rfv $LOCKFILE 2>&1
		fi
        ;;
  restart)
        $0 stop
	sleep 2
        $0 start
        ;;
  condrestart)
       [ -e $LOCKFILE ] && $0 restart
       ;;
  *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
esac

exit 0

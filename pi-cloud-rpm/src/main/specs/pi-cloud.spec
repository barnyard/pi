Summary:      Pi Cloud Platform P2P Node
Name:         pi-cloud
Version:      @piCloudVersion@
Release:      @releaseNumber@
License:      Restricted
Group:        Applications/System
Vendor:       BT plc
URL:          http://www.bt.com
Packager:     Baker Men
Requires:     vconfig, iptables, dhcp, bridge-utils
Prefix:       /opt

%description
Pi Cloud is a p2p node network built on top of pastry.

%files
@installDir@/current/*

%pre
rm -f @installDir@/current
rm -rf @installDir@/release/@piCloudVersion@-@releaseNumber@

%post
CLUSTER=`cat /etc/piconf | sed 's,^fe=,,'`
echo ${CLUSTER}
echo ${CLUSTER} | sed "s/^$/localhost/g" | awk '{print "ln -vsf @installDir@/current/bin/pi." $1 ".sh /etc/init.d/pi"}'| sh
echo ${CLUSTER} | sed "s/^$/localhost/g" | awk '{print "chmod 755 @installDir@/current/bin/pi." $1 ".sh"}'| sh
echo ${CLUSTER} | sed "s/^$/localhost/g" | awk '{print "cp -vf @installDir@/current/conf/p2p." $1 ".properties @installDir@/current/conf/p2p.properties"}'| sh
find @installDir@/current/bin/ -name "*.sh" | xargs chmod +x
mkdir -vp @installDir@/release
mkdir -vp @installDir@/var/log
mkdir -vp @installDir@/var/run
mkdir -vp @installDir@/var/run/net
mkdir -vp @installDir@/conf
mkdir -vp @installDir@/var/volumes

mkdir -vp /state/partition1/pi/local_volumes
mkdir -vp /state/partition1/pi/image_processing
mkdir -vp /state/partition1/pi/instances
mkdir -vp /state/partition1/pi/storage_archive

mv @installDir@/current/ @installDir@/release/%{version}-%{release}
ln -vsf @installDir@/release/%{version}-%{release} @installDir@/current

for f in @installDir@/current/conf/*
do
	name=$(basename $f)
	if [ ! -e @installDir@/conf/${name} ]; 
	then
		cp --preserve=all @installDir@/current/conf/${name} @installDir@/conf/${name}
	fi
done

chkconfig --add pi

ln -vsf /pifs/buckets @installDir@/var
ln -vsf /pifs/buckets_archive @installDir@/var
ln -vsf /pifs/images @installDir@/var
ln -vsf /pifs/volumes/remote @installDir@/var/volumes
ln -vsf /pifs/snapshots @installDir@/var

ln -vsf /state/partition1/pi/instances @installDir@/var
ln -vsf /state/partition1/pi/image_processing @installDir@/var
ln -vsf /state/partition1/pi/local_volumes @installDir@/var/volumes/local
ln -vsf /state/partition1/pi/storage_archive @installDir@/var

cd @installDir@/current/etc
tar xfz *.tar.gz

crontab -l | grep "logrotate /opt/pi/conf/logrotate.cfg"
if [ $? -ne 0 ]; then
	crontab -l > /tmp/pi-crontab.tmp
	echo "1 1 * * * /usr/sbin/logrotate /opt/pi/conf/logrotate.cfg" >> /tmp/pi-crontab.tmp
	crontab /tmp/pi-crontab.tmp
	rm -f /tmp/pi-crontab.tmp
fi

crontab -l | grep "archive_storage_archives.sh"
if [ $? -ne 0 ]; then
	crontab -l > /tmp/pi-crontab.tmp
	echo "30 2 * * * /opt/pi/current/etc/bin/archive_storage_archives.sh" >> /tmp/pi-crontab.tmp
	crontab /tmp/pi-crontab.tmp
	rm -f /tmp/pi-crontab.tmp
fi

crontab -l | grep "archive_terminated_instances.sh"
if [ $? -ne 0 ]; then
	crontab -l > /tmp/pi-crontab.tmp
	echo "0 */2 * * * /opt/pi/current/etc/bin/archive_terminated_instances.sh 2>&1 >> /opt/pi/var/log/archive_terminated_instances.log" >> /tmp/pi-crontab.tmp
	crontab /tmp/pi-crontab.tmp
	rm -f /tmp/pi-crontab.tmp
fi

%preun
if [ -x /etc/init.d/pi ]; 
then
	/etc/init.d/pi stop 
	chkconfig --del pi 
	rm -rf /etc/init.d/pi
fi
mv @installDir@/release/%{version}-%{release} /tmp/koalap2p-%{version}-%{release}

%postun
rm -rf @installDir@/current
rm -rf @installDir@/release/%{version}-%{release}
mv /tmp/koalap2p-%{version}-%{release} @installDir@/release/%{version}-%{release}

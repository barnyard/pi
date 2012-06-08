#1 Collect a list of all users: t 'egrep -rh "^Uri:.user:" /opt/pi/var/run/storage*'|grep "Uri"|sort|uniq|awk '{print $2}' > list
#2 dos2unix list
#3 scp list vm-container-0-0:
#4 ssh to vm-container-0-0 and move the file 'list' to a new folder
#5 create a file called script in the same folder with the following text:
#!/bin/bash

echo "builder = appCtx['piIdBuilder']"
echo "factory = appCtx['dhtClientFactory']"

cat list |
while read line
do
        echo "uid = builder.getPId('${line}')"
        echo "user = factory.createBlockingReader().get(uid)"
        echo "instIds = user.getInstanceIds().clone()"
        echo "instIds.each {inst = factory.createBlockingReader().get(builder.getPId('inst:'+it).forLocalAvailabilityZone()); if (inst == null || inst.getState().getCode() == 48)  user.getInstanceIds().remove(it); };"
        echo "print 'Running instances: '"
        echo "user.getInstanceIds()"
        echo "factory.createBlockingWriter().put(uid, user)"
done
#6 sh script > remove_terminated_instances_from_users
#7 /opt/pi/current/etc/bin/groovytelnet.sh remove_terminated_instances_from_users

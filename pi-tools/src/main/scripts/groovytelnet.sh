#!/bin/sh

########################################################
# Wrapper script for groovy shell telnet interface.
# Given a file, it'll feed it to the groovy shell line by line.
# It won't (yet) explicitly check for errors so caution advised!
#
# It uses the expect linux command + shell, producing an .expect
# file in /tmp that you can look at if you need to debug anything
# (make sure you comment out the rm line at the end of this file,
# otherwise the expect script gets destroyed)
#########################################################


if [ $# -ne 1 ]; then
        echo "Usage: $0 <groovy file>"
        exit 1
fi

filename=$1
expectfile=/tmp/`basename $filename`.expect

echo "#!/usr/bin/expect" > $expectfile

echo "spawn telnet localhost 6789;" >> $expectfile
echo "expect \"groovy:\";" >> $expectfile

cat $filename | \
                sed "s/\[/\\\\\\\[/g" | \
                sed "s/\]/\\\\\\\]/g" | \
                sed "s/\"/\\\\\\\\\"/g" | \
while read line; do
	echo $line
	echo $line | egrep "^\#" > /dev/null
	rc=$?
	if [ $rc == 1 ]; then
        	echo "send \"$line\n\"" >> $expectfile
        	echo "expect \"groovy:\";" >> $expectfile
        	echo "expect \"groovy:\";" >> $expectfile
	fi
done
echo "" >> $expectfile

chmod u+x $expectfile
$expectfile

echo ""
rm $expectfile


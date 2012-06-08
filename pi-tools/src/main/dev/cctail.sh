
# Black       0;30     Dark Gray     1;30
# Blue        0;34     Light Blue    1;34
# Green       0;32     Light Green   1;32
# Cyan        0;36     Light Cyan    1;36
# Red         0;31     Light Red     1;31
# Purple      0;35     Light Purple  1;35
# Brown       0;33     Yellow        1;33
# Light Gray  0;37     White         1;37

codes=(0\;34 0\;32 0\;36 0\;35 0\;33 0\;37 1\;30 1\;34 1\;32 1\;36 1\;31 1\;35 1\;33 1\;37)

colourstuff=""
code=0

if test $# -eq 0; then
  echo "No nodes passed. Using all pi nodes."
  PI_NODES=$(rocks list host | grep pi- | awk -F: '{print $1" "}' | tr -d '\n')
else
  PI_NODES=$*
fi

for i in $PI_NODES
do
 color=${codes[code]}
 colourstuff="${colourstuff}s/^${i}.*$/\e[${color}m$&\e[0m/g;"
 let "code = code + 1"
done

sh -c "ctail -m \"$PI_NODES\" -f /opt/pi/var/log/p2p.log -p | perl -pe 's/.*\b(WARN|ERROR|EXCEPTION|Exception)\b.*$/\e[1;31m$&\e[0m/g;${colourstuff}'"

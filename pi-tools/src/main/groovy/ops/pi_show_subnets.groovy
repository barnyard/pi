import static PiScriptUtils.*

def data = jmx().lookup2('pId',"idx:subnet-allocations","REGION")
def json = new org.json.JSONObject(data)

println "\nAvailable ranges:"
iterArray(json.resourceRanges) { println "    ${longToIp(it.min)} - ${longToIp(it.max)} / ${it.allocationStepSize} addrs per subnet" }

println "\nSubnet allocations:"
sm = sortMap(json.allocationMap) { longToIp(it) }
sm.each() { key, value -> println "    ${key}/${longToSlashnet(value.subnetMask)} : ${value.securityGroupId} (${(System.currentTimeMillis() - value.lastHeartbeatTimestamp) / 1000})" }

println sm.size() + " subnet allocation(s)\n"
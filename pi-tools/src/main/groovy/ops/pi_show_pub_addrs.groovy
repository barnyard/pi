import static PiScriptUtils.*

def data = jmx().lookup2('pId','idx:public-ip-allocations','REGION')
def json = new org.json.JSONObject(data)

println "\nAvailable ranges:"
iterArray(json.resourceRanges) { println "    ${longToIp((long)it.min)} - ${longToIp((long)it.max)} " }

println "\nAddress allocations:"
sm = sortMap(json.allocationMap) { longToIp(it) }
sm.each() { key, value -> println "    ${key} : ${value.has('instanceId')? value.instanceId : ''} Owner: ${value.has('ownerId') ? value.ownerId : ''} Last Heart Beat: (${(System.currentTimeMillis() - value.lastHeartbeatTimestamp) / 1000})" }

println "\nLast Most Recently Allocated Resource:"
println " ${longToIp((long)json.mostRecentlyAllocatedResource)}"

println sm.size() + " address assignment(s)\n"

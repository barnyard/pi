import static PiScriptUtils.*

def data = jmx().lookup2("pId","idx:vlan-allocations","REGION")
def json = new org.json.JSONObject(data)

println "\nAvailable vlan ranges:"
iterArray(json.resourceRanges) { println "    ${it.min} - ${it.max} " }

println "\nVlan allocations:"
sm = sortMap(json.allocationMap) { it }
sm.each() { key, value -> println "    ${key} : ${value.securityGroupId} (${(System.currentTimeMillis() - value.lastHeartbeatTimestamp) / 1000})" }

println sm.size() + " vlans assignment(s)\n"

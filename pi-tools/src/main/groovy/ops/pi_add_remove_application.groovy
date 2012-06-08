import static PiScriptUtils.*
import com.bt.pi.core.parser.KoalaJsonParser
import com.bt.pi.core.application.activation.ApplicationRecord

if (args.length != 4) {
	println "pi-add-remove-application <add|remove> <app name> <scope> <resource_ip>"
	println "Where scope is one of: AVAILABILITY_ZONE, REGION, GLOBAL"
	println "EXAMPLE 1: pi-add-remove-app add pi-network-manager AVAILABILITY_ZONE 109.144.11.7/21"
	return 1
}
def app = args[1]
def isAdd = args[0].equals("add")
def scope = args[2]
def resourceIp = args[3]


def data = jmx().lookup2('applicationIdForLocalScopeName', 'app:' + app , scope)
def parser = new KoalaJsonParser()
def appRecord = parser.getObject(data, ApplicationRecord)

if (isAdd) {
	println "Adding resource "+resourceIp+" to application "+app
	appRecord.addResources([resourceIp])
} else {
	println "Removing resource "+resourceIp+" from application "+app
	appRecord.removeResources([resourceIp])
}
/*
 * UPDATE DHT
 */
def newJson = parser.getJson(appRecord)
println newJson
def recId = jmx('piIdLookupService').lookup('applicationIdForLocalScopeName', 'app:' + args[0], args[1])
def updateResult = jmx('piEntityUpdateService').update(recId, "com.bt.pi.core.application.activation.ApplicationRecord", newJson)
println updateResult


import static PiScriptUtils.*

def getAppRecord(String appName, String scope) {
		def data = null
		
		if (scope.equals("AVAILABILITY_ZONE")) { 
		    data = jmx().lookup2('pId', 'avzapp:' + appName, scope)
		}
		else if (scope.equals("REGION")) {
		    data = jmx().lookup2('pId', 'regionapp:' + appName, scope)		    
		}
		
		return data
}

def getAppRecords(appNames) {
        def records = []

        for(item in appNames){
                records.add(getAppRecord(item[0], item[1]))
        }
        return records
}

def getAllNetworkAppRecords(){
        def apps = []
        apps.add(["pi-ops-website", "REGION"])
        apps.add(["pi-api-manager", "REGION"])
        apps.add(["pi-network-manager", "AVAILABILITY_ZONE"])
        apps.add(["pi-sss-manager", "REGION"])

        return getAppRecords(apps)
}

def printAll(array){
        for(item in array){
                println item
        }
}


if(args.size()!=0){
        def items = getAppRecords(args)
        printAll(items)
} else {
        def records = []

        for(item in getAllNetworkAppRecords()){
                def json = new org.json.JSONObject(item)
                def activeNodes = json.get("activeNodeMap")
                for(nodeList in activeNodes.names()){
                        def i=nodeList.length()
                        while(i-- > 0){
                                def node = nodeList.get(i)
                                def hostname = activeNodes.get(node).get("hostname")
                                if (hostname==null){
                                        hostname =""
                                }
                                records.add(json.applicationName + "  " + node + " " + hostname)
                        }
                }
        }

        printAll(records)
}


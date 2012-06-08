import static PiScriptUtils.*

def regionList = jmx().lookup1('pId',"rgn:all")

println "Current region list: "
println regionList

System.in.withReader {
         print  'Enter name of new region (must be unique): '
         def regionName = it.readLine()
         print  'Enter region code for new region (must be unique):'
         def regionCode = it.readLine()
         print  'Enter region endpoint: '
         def regionEndpoint = it.readLine()
         print  'Writen region: '
         print  jmx("piCloudManagementService").addRegion(regionName,regionCode,regionEndpoint)
         println 'Current regions list: '
         println jmx().lookup1('pId',"rgn:all")
} 
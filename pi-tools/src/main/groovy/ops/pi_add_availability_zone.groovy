import static PiScriptUtils.*

def regionList = jmx().lookup0('RegionsId')

println "Current region list: "
println regionList

def availabilityZoneList = jmx().lookup1('pId',"avz:all")
println "Current availability zone list: "
println availabilityZoneList

System.in.withReader {
         print  'Enter name of new availability zone (must be unique): '
         def azName = it.readLine()
         print  'Enter availability zone code for new availability zone (must be unique):'
         def azCode = it.readLine()
         print  'Enter numeric code of region availability zone belongs to: '
         def regionCode = it.readLine()
         print  'Enter availability zone status (Valid values: available): '
         def status = it.readLine()
         print  'Writen availability zone: '
         print  jmx("piCloudManagementService").addAvailabilityZone(azName,azCode,regionCode,status)
         println 'Current availability zones list: '
         println jmx().lookup1('pId',"avz:all")
} 

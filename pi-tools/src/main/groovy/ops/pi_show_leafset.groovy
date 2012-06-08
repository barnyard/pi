import static PiScriptUtils.*


def leafSet = jmx('nodeManagement').getLeafSet()
def currentNodeHandle = jmx('nodeManagement').getLocalNodeHandle()
println 'LeafSet for '+ currentNodeHandle +' :'
for(item in leafSet){
        println item
}

println 'Unique leafSet node count: '+ leafSet.size()


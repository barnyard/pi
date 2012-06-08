import static PiScriptUtils.*

if (args.length == 0) {
    println "Should pass instance id."
    return 1
}

def data = jmx().lookup1('pIdForEc2AvailabilityZone', "inst:"+args[0])

println "\nInstance details:\n"
println data
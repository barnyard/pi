import static PiScriptUtils.*

def volId = args[0]
def data = jmx().lookup1('pIdForEc2AvailabilityZone', "vol:"+volId)
println data
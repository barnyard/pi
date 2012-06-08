import static PiScriptUtils.*

println args[0] + ":-"
def data = jmx().lookup2('pId', 'sg:' + args[0],"REGION")
println data
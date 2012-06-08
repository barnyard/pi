import static PiScriptUtils.*

def data = jmx().lookup1('pId', "img:"+args[0])
println data

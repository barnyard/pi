import static PiScriptUtils.*

println args[0] + ":-"
def data = jmx().lookup1('pId', "bucketMetaData:"+args[0])
println data
import static PiScriptUtils.*

if (args.length != 2) {
    println "pi_show_queue.groovy <queue name> <queue scope>"
    return 1
}

println args[0] + ":-"
def data = jmx().lookup2('pId', "queue:"+args[0],args[1])
println data
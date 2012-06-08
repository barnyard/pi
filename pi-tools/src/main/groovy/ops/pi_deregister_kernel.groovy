import static PiScriptUtils.*

def result = false
if(args.length < 2){
    println 'usage: pi-deregister-kernel <userId> <imageId>'
}
else result = jmx('jmxImageService').deregisterKernel(args[0],args[1])

println 'Kernel deregistered: '+ result


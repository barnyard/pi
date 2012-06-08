import static PiScriptUtils.*

def result = false
if(args.length < 2){
    println 'useage: pi-register-kernel <userId> <manifest location>'
}
else result = jmx('jmxImageService').registerKernel(args[0],args[1])

println 'Kernel registered: '+ result


import static PiScriptUtils.*

def result = false
if(args.length < 2){
    println 'usage: pi-deregister-ramdisk <userId> <imageId>'
}
else result = jmx('jmxImageService').deregisterRamdisk(args[0],args[1])

println 'Ramdisk deregistered: '+ result


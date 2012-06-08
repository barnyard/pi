import static PiScriptUtils.*

def result = false
if(args.length < 2){
    println 'useage: pi-register-ramdisk <userId> <manifest location>'
}
else result = jmx('jmxImageService').registerRamdisk(args[0],args[1])

println 'Ramdisk registered: '+ result


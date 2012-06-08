import static PiScriptUtils.*

def result = false
if(args.length < 2){
    println 'usage: pi-update-image-platform <imageId> <platform>'
}
else result = jmx('piSeeder').updateImagePlatform(args[0],args[1])

println 'Image platform updated to: '+ result
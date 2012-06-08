import static PiScriptUtils.*
import java.io.File
import org.apache.commons.io.FileUtils

if (args.length != 4) {
    println "pi-create-user <user id> <full name> <email address> <directory where cert should be saved>"
    return 1
}

def data = jmx('userService').createUser(args[0], args[1], args[2])
def f = new File(args[3] + '/pi-' + args[0] + '-certs.zip')
FileUtils.writeByteArrayToFile(f, data)

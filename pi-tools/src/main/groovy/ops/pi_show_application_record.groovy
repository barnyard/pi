import static PiScriptUtils.*

if (args.length != 2) {
    println "pi_show_application_record.groovy <app name> <record scope>"
    return 1
}


def show_application_record(String appUrl, String scopeName) {
    def data = jmx().lookup2('pId', appUrl, scopeName)
    println data
}

def scope = args[1]
def app = args[0]

if (scope.equals("AVAILABILITY_ZONE")) 
    show_application_record('avzapp:' + app, scope)
else if (scope.equals("REGION"))
    show_application_record('regionapp:' + app, scope)

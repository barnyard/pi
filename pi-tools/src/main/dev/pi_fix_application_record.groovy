/*

JAVA_CMD=`which java`
if [ -z $JAVA_CMD ]; then
    echo "java not found"
    exit 1
fi

[ -e ~/.pirc ] && export `grep "^PI_TOOLS_HOME" ~/.pirc`

if [ -z $PI_TOOLS_HOME ]; then
    echo "Variable PI_TOOLS_HOME not set - Setting it to /opt/pi/current/etc. Otherwise please set it explicitly, or in ~/.pirc"
    export PI_TOOLS_HOME=/opt/pi/current/etc
fi

BIN_DIR=$PI_TOOLS_HOME/bin
SRC_DIR=$PI_TOOLS_HOME/groovy
LIB_DIR=$PI_TOOLS_HOME/lib
PI_LIB_DIR=$PI_TOOLS_HOME/../lib/*
CLASSPATH=$SRC_DIR:$LIB_DIR/groovy-all-1.7.0.jar:$LIB_DIR/json-20090605.jar:$PI_LIB_DIR:$CLASSPATH

cd $SRC_DIR
$JAVA_CMD -cp $CLASSPATH groovy.lang.GroovyShell pi_fix_application_record.groovy $*
cd $BIN_DIR

 * 
 */

import static PiScriptUtils.*
import java.io.File
import org.apache.commons.io.FileUtils
import com.bt.pi.core.parser.KoalaJsonParser
import com.bt.pi.core.application.activation.ApplicationRecord
import java.util.ArrayList

if (args.length != 2) {
    println "pi_fix_application_record.groovy <app name> <record scope>"
    return 1
}

def data = jmx().lookup2('applicationIdForLocalScopeName', 'app:' + args[0], args[1])
def parser = new KoalaJsonParser()
def appRecord = parser.getObject(data, ApplicationRecord)

def list = new ArrayList()
def map = appRecord.getActiveNodeMap()
println map

//list.each() {value -> map.put(value, null) }

//map.clear()
//map.put("10.19.1.201/24", null)

println appRecord

def newJson = parser.getJson(appRecord)
println newJson

def recId = jmx('piIdLookupService').lookup('applicationIdForLocalScopeName', 'app:' + args[0], args[1])
def updateResult = jmx('piEntityUpdateService').update(recId, "com.bt.pi.core.application.activation.ApplicationRecord", newJson)
println updateResult
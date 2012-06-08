import javax.management.remote.JMXConnector
import groovy.jmx.builder.JmxBuilder
import org.json.*

class PiScriptUtils {
        static final MAX_IP_ADDRESS = 0xFFFFFFFF;

        static longToIp(String longAddr) {
                long i = Long.parseLong(longAddr)
                longToIp(i)
        }

        static longToIp(long i) {
                StringBuffer result = new StringBuffer()
                result.append((i >> 24) & 0xFF);
                result.append('.')
                result.append((i >> 16) & 0xFF)
                result.append('.')
                result.append((i >> 8) & 0xFF)
                result.append('.')
                result.append(i & 0xFF)
                return result.toString()
        }

        static log2(d) {
                Math.log(d) / Math.log(2.0)
        }

        static longToSlashnet(longSlashnet) {
                32 - ((int) log2((double) (MAX_IP_ADDRESS - longSlashnet)) + 1);
        }
		
		static jmx() {
			jmx('piEntityLookupService')
		}
		
        static jmx(beanName) {
                System.properties["javax.net.ssl.trustStore"] = "/opt/pi/conf/koalaJmx.truststore"
                System.properties["javax.net.ssl.trustStorePassword"] = "jmx1jmx2"

                def jmx = new JmxBuilder()
                def client = jmx.connectorClient(port: 45243, properties : [(JMXConnector.CREDENTIALS): (String[]) ["controlRole", "k04l4834R"]])
                client.connect()

                def server = client.getMBeanServerConnection()
                new GroovyMBean(server, 'bean:name=' + beanName)
        }

        static sortMap(map, clos) {
                def res = new TreeMap()
                map.keys().each() { key -> res.put(clos(key), map[key]) }
                res
        }

        static iterArray(arr, clos) {
                for (int i = 0; i < arr.length(); i++ ) {
                        def current  = arr.get(i)
                        clos(current)
                }
        }
}
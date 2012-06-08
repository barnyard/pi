package com.bt.pi.app.networkmanager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jmx.export.MBeanExporter;

import com.bt.pi.core.console.GroovyShellServiceFactory;
import com.bt.pi.core.environment.KoalaParameters;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.node.KoalaNode;

public class BeanPropertiesMunger implements BeanPostProcessor {
    private static boolean doMunging = false;
    private static boolean isFirstEverNode = true;

    private static int port;
    private static int bootstrapPort;
    private static int region;
    private static int availabilityZone;

    private static int groovyPort;

    public static void setDoMunging(boolean value) {
        doMunging = value;
    }

    public static void setRegionAndAvailabilityZone(int reg, int avz) {
        region = reg;
        availabilityZone = avz;
    }

    public static void resetRegionAndAvailabilityZone() {
        region = -1;
        availabilityZone = -1;
    }

    public static void setPortAndBootstrapPort(int p, int bp) {
        port = p;
        bootstrapPort = bp;
    }

    public static void setApplicationPorts(int groovy) {
        groovyPort = groovy;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!doMunging)
            return bean;

        if (beanName.equals("koalaNode")) {
            ((KoalaNode) bean).setPort(port);
            ((KoalaNode) bean).setPreferredBootstraps(String.format("%s:%d", "127.0.0.1", bootstrapPort));
            ((KoalaNode) bean).setNodeIdFile(String.format("nodeIdFile%d", ((KoalaNode) bean).getPort()));
            ((KoalaNode) bean).setAddressPattern(String.format("^%s", "127.0.0.1"));
        } else if (beanName.equals("koalaParameters")) {
            ((KoalaParameters) bean).setString("can_start_new_ring", String.format("%s", isFirstEverNode));
            if (isFirstEverNode)
                isFirstEverNode = false;
        } else if (beanName.equals("koalaIdFactory")) {
            ((KoalaIdFactory) bean).setRegion(region);
            ((KoalaIdFactory) bean).setAvailabilityZone(availabilityZone);
        } else if (beanName.equals("exporter"))
            ((MBeanExporter) bean).setAutodetect(false);
        else if (beanName.equals("groovyShellServiceFactory"))
            ((GroovyShellServiceFactory) bean).setPort(groovyPort);

        return bean;
    }
}

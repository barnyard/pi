package com.bt.pi.integration.util;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;

import com.bt.pi.api.http.SimpleHttpsServerFactoryBean;
import com.bt.pi.api.service.ApiApplicationManager;
import com.bt.pi.core.console.GroovyShellServiceFactory;
import com.bt.pi.core.environment.KoalaParameters;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.integration.IntegrationTestBase;
import com.bt.pi.ops.website.ClasspathSslSelectChannelConnector;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.bt.pi.sss.HttpServer;
import com.bt.pi.sss.PisssApplicationManager;
import com.bt.pi.sss.PisssHttpsServer;

public class BeanPropertiesMunger implements BeanPostProcessor {
    private static boolean doMunging = false;
    private static boolean isFirstEverNode = true;

    private static String nodeId;
    private static int port;
    private static int bootstrapPort;
    private static int region;
    private static int availabilityZone;

    private static int apiPort;
    private static int apiHttpsPort;
    private static int pisssHttpPort;
    private static int pisssHttpsPort;
    private static int opsPort;
    private static int groovyPort;

    public static void setDoMunging(boolean value) {
        doMunging = value;
    }

    public static void setNodeId(String aNodeId) {
        nodeId = aNodeId;
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

    public static void setApplicationPorts(int api, int apiHttps, int pisss, int pisssHttps, int ops, int groovy) {
        apiPort = api;
        apiHttpsPort = apiHttps;
        pisssHttpPort = pisss;
        pisssHttpsPort = pisssHttps;
        opsPort = ops;
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

        if (bean instanceof SimpleHttpServerFactoryBean)
            ((SimpleHttpServerFactoryBean) bean).setPort(apiPort);
        else if (bean instanceof SimpleHttpsServerFactoryBean)
            ((SimpleHttpsServerFactoryBean) bean).setPort(apiHttpsPort);
        else if (beanName.equals("apiApplicationManager"))
            ((ApiApplicationManager) bean).setPort(apiPort);
        else if (beanName.equals("pisssHttpServer"))
            ((HttpServer) bean).setPort(String.format("%d", pisssHttpPort));
        else if (beanName.equals("pisssHttpsServer"))
            ((PisssHttpsServer) bean).setPort(pisssHttpsPort);
        else if (beanName.equals("pisssApplicationManager"))
            ((PisssApplicationManager) bean).setPisssPort(pisssHttpPort);
        else if (bean instanceof ClasspathSslSelectChannelConnector)
            ((ClasspathSslSelectChannelConnector) bean).setPort(opsPort);
        else if (beanName.equals("opsWebsiteApplicationManager"))
            ((OpsWebsiteApplicationManager) bean).setWebsitePort(opsPort);
        else if (beanName.equals("koalaNode")) {
            ((KoalaNode) bean).setPort(port);
            ((KoalaNode) bean).setPreferredBootstraps(String.format("%s:%d", IntegrationTestBase.localhostStr, bootstrapPort));
            ((KoalaNode) bean).setNodeIdFile(String.format("nodeIdFile%d", ((KoalaNode) bean).getPort()));
            ((KoalaNode) bean).setAddressPattern(String.format("^%s", "127.0.0.1"));
        } else if (beanName.equals("koalaParameters")) {
            ((KoalaParameters) bean).setString("can_start_new_ring", String.format("%s", isFirstEverNode));
            if (isFirstEverNode)
                isFirstEverNode = false;
        } else if (beanName.equals("koalaIdFactory")) {
            ((KoalaIdFactory) bean).setNodeId(nodeId);
            ((KoalaIdFactory) bean).setRegion(region);
            ((KoalaIdFactory) bean).setAvailabilityZone(availabilityZone);
        } else if (beanName.equals("exporter"))
            ((MBeanExporter) bean).setAutodetect(false);
        else if (beanName.equals("groovyShellServiceFactory"))
            ((GroovyShellServiceFactory) bean).setPort(groovyPort);
        else if (beanName.equals("resourceHandler"))
            ((ResourceHandler) bean).setResourceBase("build/www");
        else if (bean instanceof com.bt.pi.integration.applications.NodePhysicalHealthChecker)
            ((com.bt.pi.integration.applications.NodePhysicalHealthChecker) bean).setPort(port);

        return bean;
    }
}

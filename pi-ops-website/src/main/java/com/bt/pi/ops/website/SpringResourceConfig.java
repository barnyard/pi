/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import java.util.Collection;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.sun.jersey.api.core.DefaultResourceConfig;

public class SpringResourceConfig extends DefaultResourceConfig implements ApplicationContextAware {
    private static final Log LOG = LogFactory.getLog(SpringResourceConfig.class);
    private final String[] packages;
    private ApplicationContext applicationContext;

    public SpringResourceConfig(String... thePackages) {
        super();
        packages = thePackages;
        applicationContext = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext theApplicationContext) {
        applicationContext = theApplicationContext;
        loadObjects();
    }

    private void loadObjects() {
        loadObjects(applicationContext.getBeansWithAnnotation(Provider.class).values());
        loadObjects(applicationContext.getBeansWithAnnotation(Path.class).values());
    }

    private void loadObjects(Collection<Object> beans) {
        for (Object object : beans) {
            if (isBeanInPackages(object)) {
                LOG.info("Adding jersey singleton " + object.getClass().getName());
                getSingletons().add(object);
            }
        }
    }

    private boolean isBeanInPackages(Object object) {
        String name = object.getClass().getName();
        for (String pckage : packages) {
            if (name.startsWith(pckage))
                return true;
        }
        return false;
    }
}

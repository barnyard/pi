package com.bt.pi.app.common.id;

import java.lang.reflect.Method;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@Component
@ManagedResource(description = "Helper service for automating pi id lookups", objectName = "bean:name=piIdLookupService")
public class PiIdLookupService {
    private static final Log LOG = LogFactory.getLog(PiIdLookupService.class);

    private PiIdBuilder piIdBuilder;

    public PiIdLookupService() {
        piIdBuilder = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    public String lookup(String idDescription) {
        return lookup(idDescription, null, null);
    }

    @ManagedOperation(description = "Look up a Pi ID with id arg")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "idDescription", description = "Description of id to look up"),
            @ManagedOperationParameter(name = "arg0", description = "First Argument to use for the lookup (such as an instance identifer when looking up a Pi ID for an instance DHT record), or blank if none") })
    public String lookup(String idDescription, String arg0, String scope) {
        LOG.debug(String.format("lookup(%s, %s)", idDescription, arg0));
        if (idDescription == null || idDescription.trim().length() < 1) {
            return String.format("ERROR: No id description specified");
        }

        try {
            return invokeMethod(idDescription, arg0, scope);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            return "ERROR: id lookup unexpectedly failed";
        }
    }

    public String invokeMethod(String idDescription, String arg0, String scope) throws Exception {
        String methodName = String.format("get%s%s", idDescription.substring(0, 1).toUpperCase(Locale.getDefault()), idDescription.substring(1));
        Method method;
        PId id;
        try {
            if (arg0 == null || arg0.trim().length() < 1) {
                method = piIdBuilder.getClass().getMethod(methodName);
                id = (PId) method.invoke(piIdBuilder);
            } else {
                method = piIdBuilder.getClass().getMethod(methodName, String.class);
                id = (PId) method.invoke(piIdBuilder, arg0);
            }
        } catch (NoSuchMethodException e) {
            LOG.warn(String.format("ERROR: Bad method name: '%s'", idDescription));
            throw e;
        }

        if (id == null)
            return String.format("ERROR: No id found for '%s'", idDescription);

        if (scope != null) {
            id = id.forLocalScope(NodeScope.valueOf(scope));
        }
        return id.toStringFull();
    }
}

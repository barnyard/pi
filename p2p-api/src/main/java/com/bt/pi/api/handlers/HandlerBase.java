/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpExchangeConnection;

import com.bt.pi.api.ApiException;
import com.bt.pi.api.ApiServerException;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.api.utils.XmlMappingException;

/**
 * Base class for Web service handlers
 */
public abstract class HandlerBase {
    protected static final String SERVICE_ERROR = "Service error";
    protected static final String CLIENT_ERROR = "Client error";
    protected static final String NAMESPACE_20081201 = "http://ec2.amazonaws.com/doc/2008-12-01/";
    protected static final String NAMESPACE_20090404 = "http://ec2.amazonaws.com/doc/2009-04-04/";
    private static final String MAPPING_ERROR_MESSAGE = "error mapping request/response to/from 2009-04-04/2008-12-01 version";
    private static final Log LOG = LogFactory.getLog(HandlerBase.class);
    private ConversionUtils conversionUtils;

    public HandlerBase() {
    }

    /*
     * get the userid attribute from the http exchange 
     */
    protected String getUserId() {
        TransportContext context = getTransportContext();
        HttpExchangeConnection connection = (HttpExchangeConnection) context.getConnection();
        String userid = (String) connection.getHttpExchange().getAttribute("koala.api.userid");
        LOG.debug(String.format("userid: %s", userid));
        return userid;
    }

    // protected to allow override in unittest
    protected TransportContext getTransportContext() {
        return TransportContextHolder.getTransportContext();
    }

    /*
     * derive the method name from the request type
     */
    private String getMethodName(XmlObject xmlObject) {
        String shortRequestTypeName = xmlObject.schemaType().getShortJavaName();
        String methodName = shortRequestTypeName.substring(0, shortRequestTypeName.length() - "Document".length());
        methodName = methodName.substring(0, 1).toLowerCase(Locale.getDefault()) + methodName.substring(1);
        return methodName;
    }

    /*
     * translate request into latest supported version and call the method before translating the response back to
     * the original format.
     */
    protected XmlObject callLatest(XmlObject request) {
        try {
            String payload = request.toString();
            payload = payload.replaceAll(NAMESPACE_20081201, NAMESPACE_20090404);
            XmlObject xmlObject = XmlObject.Factory.parse(payload);
            String requestTypeName = xmlObject.schemaType().getFullJavaName();
            String methodName = getMethodName(xmlObject);
            Method method = this.getClass().getMethod(methodName, Class.forName(requestTypeName));
            XmlObject result = (XmlObject) method.invoke(this, xmlObject);
            updateResult(result);
            String resultString = result.toString();
            resultString = resultString.replaceAll(NAMESPACE_20090404, NAMESPACE_20081201);
            return XmlObject.Factory.parse(resultString);
        } catch (XmlException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (SecurityException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (NoSuchMethodException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (ClassNotFoundException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (IllegalArgumentException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (IllegalAccessException e) {
            LOG.error(formatErrorMessage(e), e);
            throw new XmlMappingException(formatErrorMessage(e), e);
        } catch (InvocationTargetException e) {
            LOG.error(formatErrorMessage(e.getCause()), e);
            throw (RuntimeException) e.getCause();
        }
    }

    private String formatErrorMessage(Throwable e) {
        return String.format("%s - %s - %s", MAPPING_ERROR_MESSAGE, e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * this method can be overridden by concrete classes to update 20090404 responses before they are converted to
     * 20081201
     */
    protected void updateResult(XmlObject xmlObject) {
    }

    protected RuntimeException handleThrowable(Throwable t) {
        if (t instanceof ApiException) {
            LOG.warn(CLIENT_ERROR, t);
            return (RuntimeException) t;
        }
        if (t instanceof IllegalArgumentException) {
            LOG.warn(CLIENT_ERROR, t);
            return new ApiException(t.getMessage()) {
                private static final long serialVersionUID = 5832216898828234994L;
            };
        }
        LOG.error(SERVICE_ERROR, t);
        return new ApiServerException();
    }

    // this method uses serilisation/deserialization to detach from any vestiges of SOAP so that the response is
    // nicely formatted for RightScale client that is a bit pants
    protected XmlObject sanitiseXml(XmlObject input) throws XmlException {
        return XmlObject.Factory.parse(input.toString());
    }

    @Resource
    protected void setConversionUtils(ConversionUtils aConversionUtils) {
        this.conversionUtils = aConversionUtils;
    }

    protected ConversionUtils getConversionUtils() {
        return conversionUtils;
    }
}

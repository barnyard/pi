/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlObject;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;

import com.bt.pi.api.utils.HttpUtils;
import com.bt.pi.api.utils.QueryParameterUtils;
import com.bt.pi.api.utils.SoapEnvelopeUtils;
import com.bt.pi.api.utils.SoapRequestFactory;
import com.bt.pi.api.utils.XmlFormattingException;
import com.bt.pi.api.utils.XmlUtils;
import com.bt.pi.core.util.MDCHelper;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

/**
 * This filter allows convertion of REST api request into SOAP before they hit the Spring WS framework. WS-security and
 * REST hashes are also validated.
 */
public class RestQueryFilter extends Filter {
    protected static final String[] SUPPORTED_VERSIONS = { "2008-12-01", "2009-04-04" };
    private static final String RESPONSE_CODE = "response code: ";
    private static final String SERVER_ERROR = "InternalError";
    private static final Log LOG = LogFactory.getLog(RestQueryFilter.class);
    private static final String AUTH_FAILURE = "AuthFailure";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String S = "%s";
    private static final String KOALA_API_USERID = "koala.api.userid";
    private SoapEnvelopeUtils soapEnvelopeUtils;
    private WSSecurityHandler wsSecurityHandler;
    private SoapRequestFactory soapRequestFactory;
    private XmlUtils xmlUtils;
    private QueryParameterUtils queryParameterUtils;
    private AwsQuerySecurityHandler awsQuerySecurityHandler;
    private HttpUtils httpUtils;

    public RestQueryFilter() {
        super();
        this.soapEnvelopeUtils = null;
        this.soapRequestFactory = null;
        this.xmlUtils = null;
        this.awsQuerySecurityHandler = null;
        this.wsSecurityHandler = null;
        this.queryParameterUtils = null;
        this.httpUtils = null;
    }

    @Override
    public String description() {
        return getClass().getName();
    }

    protected String generateNewTransactionUID() {
        String res = UUID.randomUUID().toString();
        LOG.debug(String.format("Generated transaction UID %s", res));
        return res;
    }

    @Override
    public void doFilter(HttpExchange httpExchange, Chain filterChain) throws IOException {
        MDCHelper.putTransactionUID(generateNewTransactionUID());

        String payload = httpUtils.readPayload(httpExchange);

        Map<String, String> parameters = httpUtils.readRequestParameters(httpExchange, payload);

        if (isRestRequest(parameters)) {
            processRestRequest(httpExchange, filterChain, parameters);
            return;
        }

        try {
            XmlObject envelope;
            try {
                envelope = this.xmlUtils.parseInputString(payload);
            } catch (XmlFormattingException e) {
                LOG.warn("Unable to parse input stream. ", e);
                byte[] soapFault = createSoapFault("Invalid_XML_request", e.getMessage());
                sendResponse(httpExchange, soapFault, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                validateVersion(envelope);
            } catch (IllegalArgumentException e) {
                byte[] soapFault = createSoapFault("SOAP-ENV:Client", e.getMessage());
                sendResponse(httpExchange, soapFault, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            try {
                String userid = this.wsSecurityHandler.processEnvelope((Document) envelope.getDomNode());
                httpExchange.setAttribute(KOALA_API_USERID, userid);
            } catch (WSSecurityHandlerException e) {
                LOG.error("Unable to process envelope. When given this envelope:  " + envelope, e);
                byte[] soapFault = createSoapFault(AUTH_FAILURE, e.getMessage());
                sendResponse(httpExchange, soapFault, e.getHttpCode());
                return;
            }

            httpExchange.setStreams(new ByteArrayInputStream(envelope.toString().getBytes()), null);
            filterChain.doFilter(httpExchange); // invoke Soap
            LOG.debug(RESPONSE_CODE + httpExchange.getResponseCode());
        } catch (Throwable t) {
            LOG.error("Unable to process soap request", t);
            byte[] soapFault = createSoapFault(SERVER_ERROR, t.getMessage());
            sendResponse(httpExchange, soapFault, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void validateVersion(XmlObject envelope) {
        String xml = envelope.toString();
        LOG.debug(String.format("validateVersion(%s)", xml));
        for (String validVersion : SUPPORTED_VERSIONS)
            if (xml.contains("http://ec2.amazonaws.com/doc/" + validVersion))
                return;
        throw new IllegalArgumentException("unsupported AWS version");
    }

    private void processRestRequest(HttpExchange httpExchange, Chain filterChain, Map<String, String> parameters) throws IOException {
        try {
            String method = httpExchange.getRequestMethod();
            String path = httpExchange.getRequestURI().getPath();
            String host = httpExchange.getRequestHeaders().get("host").get(0).split(":")[0];
            String userid;

            try {
                userid = awsQuerySecurityHandler.validate(parameters, host, path, method);
            } catch (WSSecurityHandlerException e) {
                sendQueryErrorResponse(httpExchange, AUTH_FAILURE, e.getMessage(), e.getHttpCode());
                return;
            }

            httpExchange.setAttribute(KOALA_API_USERID, userid);
            Map<String, Object> sanitisedParameters = this.queryParameterUtils.sanitiseParameters(parameters);
            String soapRequest = this.soapRequestFactory.getSoap(sanitisedParameters);
            LOG.debug(String.format("soapRequest: %s", soapRequest));

            HttpExchangeImpl he = new HttpExchangeImpl(httpExchange, soapRequest.getBytes());

            filterChain.doFilter(he); // invoke Soap

            LOG.debug(RESPONSE_CODE + he.getResponseCode());
            LOG.debug(new String(he.getBytes()));

            byte[] soapResponse = he.getBytes();
            byte[] xmlResponse = this.soapEnvelopeUtils.removeEnvelope(soapResponse);
            if (this.soapEnvelopeUtils.isSoapFault(xmlResponse)) {
                String faultCode = this.soapEnvelopeUtils.getFaultCode(xmlResponse);
                String faultString = this.soapEnvelopeUtils.getFaultString(xmlResponse);
                sendQueryErrorResponse(httpExchange, faultCode, faultString, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            sendResponse(httpExchange, xmlResponse, HttpServletResponse.SC_OK);
        } catch (Throwable t) {
            LOG.error("Unable to process rest request", t);
            sendQueryErrorResponse(httpExchange, SERVER_ERROR, String.format("service error - %s", t.getMessage()), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    private boolean isRestRequest(Map<String, String> parameters) {
        LOG.debug(String.format("isRestRequest(%s)", parameters));
        return parameters.size() > 0;
    }

    private void sendQueryErrorResponse(HttpExchange httpExchange, String faultCode, String faultString, int responseCode) throws IOException {
        byte[] errorResponse = createQueryErrorResponse(faultCode, faultString);
        sendResponse(httpExchange, errorResponse, responseCode);
    }

    /*
     * Very poor documentation on Error responses for AWS. Although not for EC2, http://docs.amazonwebservices.com/AmazonFPS/latest/FPSBasicGuide/index.html?Errors.html seems to match what Typica
     * expects in the way of errors.
     * 
     * http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/index.html?api-error-codes.html lists the codes.
     */
    private byte[] createQueryErrorResponse(String code, String message) {
        String xml = String.format("<Response><Errors><Error><Code>%s</Code><Message>%s</Message></Error></Errors></Response>", code, message);
        return xml.getBytes();
    }

    private void sendResponse(HttpExchange httpExchange, byte[] payload, int statusCode) throws IOException {
        LOG.debug(String.format("sendResponse(%s, %s, %d)", httpExchange, new String(payload), statusCode));
        httpExchange.getResponseHeaders().set(CONTENT_LENGTH, String.format(S, payload.length));
        httpExchange.getResponseHeaders().set(CONTENT_TYPE, "text/xml");
        httpExchange.sendResponseHeaders(statusCode, payload.length);
        OutputStream responseBody = httpExchange.getResponseBody();
        FileCopyUtils.copy(payload, responseBody);
        httpExchange.close();
    }

    private byte[] createSoapFault(String faultCode, String faultString) {
        String result = String.format(
                "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><SOAP-ENV:Fault><faultcode>%s</faultcode><faultstring>%s</faultstring></SOAP-ENV:Fault></SOAP-ENV:Body></SOAP-ENV:Envelope>", faultCode,
                faultString);
        return result.getBytes();
    }

    @Resource
    public void setSoapEnvelopeUtils(SoapEnvelopeUtils aSoapEnvelopeUtils) {
        this.soapEnvelopeUtils = aSoapEnvelopeUtils;
    }

    @Resource
    public void setWSSecurityHandler(WSSecurityHandler aWSSecurityHandler) {
        this.wsSecurityHandler = aWSSecurityHandler;
    }

    @Resource
    public void setSoapRequestFactory(SoapRequestFactory aSoapRequestFactory) {
        this.soapRequestFactory = aSoapRequestFactory;
    }

    @Resource
    public void setXmlUtils(XmlUtils aXmlUtils) {
        this.xmlUtils = aXmlUtils;
    }

    @Resource
    public void setQueryParameterUtils(QueryParameterUtils aQueryParameterUtils) {
        this.queryParameterUtils = aQueryParameterUtils;
    }

    @Resource
    public void setAwsQuerySecurityHandler(AwsQuerySecurityHandler aAwsQuerySecurityHandler) {
        this.awsQuerySecurityHandler = aAwsQuerySecurityHandler;
    }

    @Resource
    public void setHttpUtils(HttpUtils aHttpUtils) {
        this.httpUtils = aHttpUtils;
    }
}

/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.Calendar;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.GetConsoleOutputDocument;
import com.amazonaws.ec2.doc.x20090404.GetConsoleOutputResponseDocument;
import com.amazonaws.ec2.doc.x20090404.GetConsoleOutputResponseType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for GetConsoleOutput
 */
@Endpoint
public class GetConsoleOutputHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(RebootInstancesHandler.class);
    private static final String GET_CONSOLE_OUTPUT = "GetConsoleOutput";
    private InstancesService instancesService;

    public GetConsoleOutputHandler() {
        instancesService = null;
    }

    @Resource
    public void setInstancesService(InstancesService anInstancesService) {
        instancesService = anInstancesService;
    }

    @PayloadRoot(localPart = GET_CONSOLE_OUTPUT, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.GetConsoleOutputResponseDocument getConsoleOutput(com.amazonaws.ec2.doc.x20081201.GetConsoleOutputDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.GetConsoleOutputResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = GET_CONSOLE_OUTPUT, namespace = NAMESPACE_20090404)
    public GetConsoleOutputResponseDocument getConsoleOutput(GetConsoleOutputDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String instanceId = requestDocument.getGetConsoleOutput().getInstanceId();
            GetConsoleOutputResponseDocument resultDocument = GetConsoleOutputResponseDocument.Factory.newInstance();
            GetConsoleOutputResponseType type = resultDocument.addNewGetConsoleOutputResponse();

            ConsoleOutput consoleOutput = instancesService.getConsoleOutput(getUserId(), instanceId);
            type.setOutput(new String(Base64.encodeBase64(consoleOutput.getOutput().getBytes())));
            type.setInstanceId(instanceId);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(consoleOutput.getTimestamp());
            type.setTimestamp(calendar);
            type.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (GetConsoleOutputResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}

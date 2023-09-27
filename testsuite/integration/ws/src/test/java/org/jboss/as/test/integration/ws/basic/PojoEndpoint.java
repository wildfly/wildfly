/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.basic;

import jakarta.jws.WebService;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.soap.SOAPFaultException;

/**
 * Simple POJO endpoint
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService(
        serviceName = "POJOService",
        targetNamespace = "http://jbossws.org/basic",
        endpointInterface = "org.jboss.as.test.integration.ws.basic.EndpointIface"
)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class PojoEndpoint implements EndpointIface {

    public String helloString(String input) {
        return "Hello " + input + "!";
    }

    public HelloObject helloBean(HelloObject input) {
        return new HelloObject(helloString(input.getMessage()));
    }

    public HelloObject[] helloArray(HelloObject[] input) {
        HelloObject[] reply = new HelloObject[input.length];
        for (int n = 0; n < input.length; n++) {
            reply[n] = helloBean(input[n]);
        }
        return reply;
    }

    public String helloError(String input) {
        try {
            SOAPFault fault = SOAPFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createFault(input,
                    SOAPConstants.SOAP_VERSIONMISMATCH_FAULT);
            fault.setFaultActor("mr.actor");
            fault.addDetail().addChildElement("test");
            fault.appendFaultSubcode(new QName("http://ws.gss.redhat.com/", "NullPointerException"));
            fault.appendFaultSubcode(new QName("http://ws.gss.redhat.com/", "OperatorNotFound"));
            throw new SOAPFaultException(fault);
        } catch (SOAPException ex) {
            ex.printStackTrace();
        }
        return "Failure!";
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ws.basic;

import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPFaultException;

import org.jboss.ws.api.annotation.WebContext;

/**
 * Simple EJB3 endpoint
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService(
        serviceName = "EJB3Service",
        targetNamespace = "http://jbossws.org/basic",
        endpointInterface = "org.jboss.as.test.integration.ws.basic.EndpointIface"
)
@WebContext(
        urlPattern = "/EJB3Service",
        contextRoot = "/jaxws-basic-ejb3"
)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@Stateless
public class EJBEndpoint implements EndpointIface {

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

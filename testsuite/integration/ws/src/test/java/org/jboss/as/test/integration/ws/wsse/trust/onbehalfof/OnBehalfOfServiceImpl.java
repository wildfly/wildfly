/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust.onbehalfof;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.jboss.as.test.integration.ws.wsse.trust.service.ServiceIface;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * User: rsearls@redhat.com
 * Date: 1/26/14
 */

@WebService
        (
                portName = "OnBehalfOfServicePort",
                serviceName = "OnBehalfOfService",
                wsdlLocation = "WEB-INF/wsdl/OnBehalfOfService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/onbehalfofwssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.trust.onbehalfof.OnBehalfOfServiceIface"
        )

@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.username", value = "myactaskey"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "actasKeystore.properties"),
        @EndpointProperty(key = "ws-security.encryption.properties", value = "actasKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.onbehalfof.OnBehalfOfCallbackHandler")
})

public class OnBehalfOfServiceImpl implements OnBehalfOfServiceIface {
    public String sayHello(String host, String port) {
        Bus bus = BusFactory.newInstance().createBus();
        try {
            BusFactory.setThreadDefaultBus(bus);

            final String serviceURL = "http://" + host + ":" + port + "/jaxws-samples-wsse-policy-trust/SecurityService";
            final QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "SecurityService");
            final URL wsdlURL = new URL(serviceURL + "?wsdl");
            Service service = Service.create(wsdlURL, serviceName);
            ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

            Map<String, Object> ctx = ((BindingProvider) proxy).getRequestContext();
            ctx.put(SecurityConstants.CALLBACK_HANDLER, new OnBehalfOfCallbackHandler());

            ctx.put(SecurityConstants.SIGNATURE_PROPERTIES,
                    Thread.currentThread().getContextClassLoader().getResource("actasKeystore.properties"));
            ctx.put(SecurityConstants.SIGNATURE_USERNAME, "myactaskey");
            ctx.put(SecurityConstants.ENCRYPT_PROPERTIES,
                    Thread.currentThread().getContextClassLoader().getResource("../../META-INF/clientKeystore.properties"));
            ctx.put(SecurityConstants.ENCRYPT_USERNAME, "myservicekey");

            STSClient stsClient = new STSClient(bus);
            Map<String, Object> props = stsClient.getProperties();
            props.put(SecurityConstants.USERNAME, "bob"); //-rls test
            props.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
            props.put(SecurityConstants.STS_TOKEN_USERNAME, "myactaskey");
            props.put(SecurityConstants.STS_TOKEN_PROPERTIES,
                    Thread.currentThread().getContextClassLoader().getResource("actasKeystore.properties"));
            props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");

            ctx.put(SecurityConstants.STS_CLIENT, stsClient);

            return "OnBehalfOf " + proxy.sayHello();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } finally {
            bus.shutdown(true);
        }
    }

}

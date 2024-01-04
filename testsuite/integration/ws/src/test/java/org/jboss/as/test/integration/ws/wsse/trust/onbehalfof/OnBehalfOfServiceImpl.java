/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.onbehalfof;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.jboss.as.test.integration.ws.wsse.trust.service.ServiceIface;

import jakarta.jws.WebService;
import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
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
